"""
飞书数据同步共享模块
包含所有通用的、可复用的代码，如 API 配置、token 管理、文件下载、JSON 写入等函数。
"""

import json
import os
import pathlib
import re
import tempfile
import time
from typing import Any
from urllib.parse import urlparse

import requests

# === 0. 基础配置（强烈建议用环境变量读取） ===
APP_ID = os.getenv("FEISHU_APP_ID", "")
APP_SECRET = os.getenv("FEISHU_APP_SECRET", "")
BASE_URL = os.getenv("FEISHU_BASE_URL", "https://open.feishu.cn")
SITE_DATA_DIR = pathlib.Path(__file__).parent.parent / "resource"
FEISHU_IMAGE_DIR = pathlib.Path(__file__).parent.parent / "public" / "images" / "feishu"
RETRY_COUNT = 3
RETRY_BACKOFF_SECONDS = 1

TABLES = {
    "levels": dict(
        cn_name="关卡数据",
        app=os.getenv("FEISHU_LEVELS_APP_TOKEN") or os.getenv("FEISHU_BITABLE_ID") or "IquLbb1sVaV3ljsPhaPcxbmVnbb",
        tbl=os.getenv("FEISHU_LEVELS_TABLE_ID") or os.getenv("FEISHU_TABLE_ID") or "tblywzqIAWTwZstE",
    ),
    "operators": dict(
        cn_name="密探数据",
        app=os.getenv("FEISHU_OPERATORS_APP_TOKEN") or os.getenv("FEISHU_BITABLE_ID") or "IquLbb1sVaV3ljsPhaPcxbmVnbb",
        tbl=os.getenv("FEISHU_OPERATORS_TABLE_ID") or os.getenv("FEISHU_TABLE_ID") or "tblqJZBK1eaz7idg",
    ),
}

# === 1. 飞书 API 封装 ===


class FeishuApiError(RuntimeError):
    """飞书接口返回错误，调用方不得使用不完整的数据继续写文件。"""


def _request_json(method: str, url: str, **kwargs) -> dict:
    for attempt in range(RETRY_COUNT + 1):
        try:
            response = requests.request(method, url, **kwargs)
            status = response.status_code
            if (status == 429 or status >= 500) and attempt < RETRY_COUNT:
                time.sleep(RETRY_BACKOFF_SECONDS * (attempt + 1))
                continue

            response.raise_for_status()
            try:
                payload = response.json()
            except ValueError as exc:
                raise FeishuApiError(f"Feishu returned invalid JSON: {url}") from exc
            if not isinstance(payload, dict):
                raise FeishuApiError(f"Feishu returned an invalid payload: {url}")
            if payload.get("code", 0) not in (0, "0"):
                raise FeishuApiError(
                    f"Feishu API error {payload.get('code')}: {payload.get('msg', 'unknown error')}"
                )
            return payload
        except requests.RequestException as exc:
            status = getattr(exc.response, "status_code", None)
            if attempt < RETRY_COUNT and (status is None or status == 429 or status >= 500):
                time.sleep(RETRY_BACKOFF_SECONDS * (attempt + 1))
                continue
            raise

    raise FeishuApiError(f"Feishu request failed after retries: {url}")

def get_tenant_token() -> str:
    """获取 tenant_access_token"""
    url = f"{BASE_URL}/open-apis/auth/v3/tenant_access_token/internal"
    body = {"app_id": APP_ID, "app_secret": APP_SECRET}
    payload = _request_json("POST", url, json=body, timeout=10)
    tenant_token = payload.get("tenant_access_token")
    if not isinstance(tenant_token, str) or not tenant_token.strip():
        raise FeishuApiError("Feishu token response did not contain tenant_access_token")
    return tenant_token

def list_table_fields(app_token: str, table_id: str, token: str) -> set[str]:
    """读取字段 schema，避免字段变更后生成不完整数据。"""
    field_names: set[str] = set()
    page_token = ""
    while True:
        params = {"page_size": 100}
        if page_token:
            params["page_token"] = page_token

        url = f"{BASE_URL}/open-apis/bitable/v1/apps/{app_token}/tables/{table_id}/fields"
        payload = _request_json(
            "GET",
            url,
            headers={"Authorization": f"Bearer {token}"},
            params=params,
            timeout=30,
        )
        data = payload.get("data")
        if not isinstance(data, dict) or not isinstance(data.get("items"), list):
            raise FeishuApiError("Feishu fields response is missing data.items")
        for item in data["items"]:
            if isinstance(item, dict) and isinstance(item.get("field_name"), str):
                field_names.add(item["field_name"])

        if not data.get("has_more"):
            return field_names
        page_token = data.get("page_token") or ""
        if not page_token:
            raise FeishuApiError("Feishu fields response requested another page without page_token")


def list_records(
    app_token: str,
    table_id: str,
    token: str,
    sort_field: str = None,
    field_names: list[str] | None = None,
):
    """通过 records/search 获取一个表的所有记录（自动处理分页）。"""
    all_records: list[dict[str, Any]] = []
    page_token = ""
    while True:
        params = {"page_size": 500}
        if page_token:
            params["page_token"] = page_token

        body: dict[str, Any] = {}
        if field_names:
            body["field_names"] = field_names
        if sort_field:
            body["sort"] = [{"field_name": sort_field, "desc": False}]

        url = f"{BASE_URL}/open-apis/bitable/v1/apps/{app_token}/tables/{table_id}/records/search"
        payload = _request_json(
            "POST",
            url,
            headers={"Authorization": f"Bearer {token}"},
            params=params,
            json=body,
            timeout=30,
        )
        data = payload.get("data")
        if not isinstance(data, dict) or not isinstance(data.get("items"), list):
            raise FeishuApiError("Feishu records response is missing data.items")
        all_records.extend(data["items"])

        if data.get("has_more"):
            page_token = data.get("page_token") or ""
            if not page_token:
                raise FeishuApiError("Feishu records response requested another page without page_token")
        else:
            break
        time.sleep(0.2) # 避免频率超限
    return all_records

def download_feishu_file(url: str, token: str, table_name: str) -> str | None:
    """
    从飞书下载文件并保存到本地。
    返回可公开访问的 URL 路径。
    """
    if not url:
        return None

    headers = {"Authorization": f"Bearer {token}"}

    try:
        with requests.get(url, headers=headers, stream=True, timeout=30) as r:
            r.raise_for_status()

            parsed_url = urlparse(url)
            file_token = parsed_url.path.split('/')[-2]

            content_disposition = r.headers.get('Content-Disposition', "")
            filename_match = re.search(r'filename="(.+)"', content_disposition)

            original_filename = ""
            if filename_match:
                original_filename = filename_match.group(1)
                # 确保文件名有扩展名
                if not pathlib.Path(original_filename).suffix:
                    content_type = r.headers.get('Content-Type', 'image/png')
                    extension = f".{content_type.split('/')[-1]}"
                    original_filename += extension
            else:
                content_type = r.headers.get('Content-Type', 'image/png')
                extension = f".{content_type.split('/')[-1]}"
                original_filename = f"download{extension}"

            # 清理文件名，防止路径问题
            safe_filename = re.sub(r'[\\/*?:"<>|]', "", original_filename)
            local_filename = f"{file_token}-{safe_filename}"

            table_image_dir = FEISHU_IMAGE_DIR / table_name
            table_image_dir.mkdir(exist_ok=True, parents=True)
            save_path = table_image_dir / local_filename

            with open(save_path, 'wb') as f:
                for chunk in r.iter_content(chunk_size=8192):
                    f.write(chunk)

                print(f"  → Downloaded {url} to {save_path}")

                return f"/images/feishu/{table_name}/{local_filename}"
    except requests.exceptions.HTTPError as e:
        if e.response.status_code == 403:
             print(f"  → Error 403: Forbidden to download file from {url}. "
                   f"Please ensure the app has 'drive:file:readonly' permission and the file is shared correctly.")
        else:
            print(f"  → An HTTP error occurred: {e}")
        return None

# === 2. 辅助函数 ===

def write_json_file(data: Any, filename: str):
    """将数据原子写入指定的 JSON 文件。"""
    outfile = pathlib.Path(filename)
    if not outfile.is_absolute():
        outfile = SITE_DATA_DIR / outfile
    outfile.parent.mkdir(parents=True, exist_ok=True)
    serialized = json.dumps(data, ensure_ascii=False, indent=2) + "\n"
    temp_path = None
    try:
        with tempfile.NamedTemporaryFile(
            mode="w",
            encoding="utf-8",
            dir=outfile.parent,
            prefix=f".{outfile.name}.",
            suffix=".tmp",
            delete=False,
        ) as temp_file:
            temp_file.write(serialized)
            temp_file.flush()
            os.fsync(temp_file.fileno())
            temp_path = pathlib.Path(temp_file.name)
        os.replace(temp_path, outfile)
    finally:
        if temp_path and temp_path.exists():
            temp_path.unlink()

    count_info = ""
    if isinstance(data, list):
        count_info = f" ({len(data)} items)"
    print(f"✔ Generated {outfile}{count_info}")

def fetch_records(
    table_key: str,
    token: str,
    sort_field: str = None,
    field_names: list[str] | None = None,
) -> list:
    """根据 table key 获取一个表的所有记录"""
    try:
        table_info = TABLES[table_key]
    except KeyError as exc:
        raise FeishuApiError(f"Unknown Feishu table key: {table_key}") from exc
    print(f"Fetching {table_info['cn_name']}...")
    records = list_records(
        table_info["app"],
        table_info["tbl"],
        token,
        sort_field,
        field_names,
    )
    if not records:
        raise FeishuApiError(f"Feishu table {table_info['cn_name']} returned no records")
    print(f"  → Fetched {len(records)} records.")
    return records
