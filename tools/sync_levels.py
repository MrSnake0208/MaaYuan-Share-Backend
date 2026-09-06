"""同步飞书中的“关卡数据”到 V2 classpath JSON 及兼容数据文件。"""

import os
import re
from datetime import datetime
from pathlib import Path
from typing import Any
from zoneinfo import ZoneInfo

import feishu_common as fc

TABLE_KEY = os.getenv("FEISHU_LEVELS_TABLE_KEY", "levels")
TABLE_TIMEZONE = ZoneInfo("Asia/Shanghai")
BACKEND_ROOT = Path(__file__).parent.parent
OUTPUT_FILES = (
    BACKEND_ROOT / "src/main/resources/levels/arknights-levels.v2.json",
    BACKEND_ROOT / "resource/levels.json",
)

FIELD_CANDIDATES = {
    "stageId": (
        "stageId",
        "stageId_new",
        "StageId",
        "关卡StageId",
        "stage_id",
    ),
    "levelId": (
        "levelId",
        "levelId_new",
        "LevelId",
        "关卡ID",
        "level_id",
    ),
    "category": ("关卡分类", "catOne", "分类1", "一级分类", "类别1"),
    "displayName": ("显示名称", "name", "关卡名", "名称"),
}
END_TIME_FIELD = "结束时间"
NOTE_FIELDS = ("备注(洞窟用)", "备注", "catThree", "分类3", "三级分类", "类别3")
PROJECTED_FIELDS = sorted(
    {field for fields in FIELD_CANDIDATES.values() for field in fields}
    | set(NOTE_FIELDS)
    | {"文本", END_TIME_FIELD}
)


def _get_text(value: Any) -> str:
    """宽松获取字段文本，兼容 str / list[dict[text]] / list[str] / None。"""
    if isinstance(value, list):
        if value and isinstance(value[0], dict):
            return str(value[0].get("text", "")).strip()
        return ", ".join(str(v) for v in value if v is not None).strip()
    if isinstance(value, dict):
        return str(value.get("text", "")).strip()
    return str(value or "").strip()


def _pick(fields: dict, *names: str) -> str:
    """按候选字段名依次取第一个非空值。"""
    for n in names:
        t = _get_text(fields.get(n))
        if t:
            return t
    return ""


def validate_schema(field_names: set[str]) -> None:
    """确认表结构仍能生成完整的关卡元数据。"""
    missing = [
        name
        for name, candidates in {
            **FIELD_CANDIDATES,
            "endTime": (END_TIME_FIELD,),
        }.items()
        if not any(candidate in field_names for candidate in candidates)
    ]
    if missing:
        raise ValueError(f"飞书关卡表缺少必要字段: {', '.join(missing)}")


def _normalize_end_time(value: Any) -> str | None:
    """将飞书毫秒时间戳或日期文本转换为带时区的 ISO 8601。"""
    if value is None or value == "" or value == []:
        return None

    if isinstance(value, list):
        if len(value) != 1:
            raise ValueError(f"结束时间格式无效: {value!r}")
        value = value[0]

    if isinstance(value, bool):
        raise ValueError(f"结束时间格式无效: {value!r}")

    if isinstance(value, int):
        try:
            parsed = datetime.fromtimestamp(value / 1000, tz=TABLE_TIMEZONE)
        except (OverflowError, OSError, ValueError) as exc:
            raise ValueError(f"结束时间格式无效: {value!r}") from exc
        return parsed.isoformat(timespec="milliseconds")

    if isinstance(value, float):
        if not value.is_integer():
            raise ValueError(f"结束时间格式无效: {value!r}")
        return _normalize_end_time(int(value))

    if not isinstance(value, str):
        raise ValueError(f"结束时间格式无效: {value!r}")

    text = value.strip()
    if not text:
        return None

    if re.fullmatch(r"\d+", text):
        return _normalize_end_time(int(text))

    try:
        parsed = datetime.fromisoformat(text.replace("Z", "+00:00"))
    except ValueError:
        try:
            parsed = datetime.strptime(text, "%Y-%m-%d %H:%M")
        except ValueError as exc:
            raise ValueError(f"结束时间格式无效: {value!r}") from exc

    if parsed.tzinfo is None:
        parsed = parsed.replace(tzinfo=TABLE_TIMEZONE)
    return parsed.isoformat(timespec="milliseconds")


def _to_int(value: Any) -> int:
    try:
        return int(str(value).strip())
    except Exception:
        return 0


def _normalize_title(value: str) -> str:
    """
    复刻 resource/levels.json 的命名规则：
    - YYYY年M月 -> YYYY年MM月（M<10 时补零）
    - M月D日 -> M-D（不补零）
    """
    s = (value or "").strip()
    if not s:
        return ""

    m = re.fullmatch(r"(\d{4})年(\d{1,2})月", s)
    if m:
        year, month = m.groups()
        return f"{year}年{int(month):02d}月"

    m = re.fullmatch(r"(\d{1,2})月(\d{1,2})日", s)
    if m:
        month, day = m.groups()
        return f"{int(month)}-{int(day)}"

    return s


def transform_levels(records: list) -> list[dict[str, str]]:
    """将飞书记录转换为 V2 关卡列表。"""
    if not isinstance(records, list) or not records:
        raise ValueError("飞书关卡表返回空结果")
    if any(not isinstance(record, dict) or not isinstance(record.get("fields"), dict) for record in records):
        raise ValueError("飞书关卡记录格式无效")

    # 先按“文本”(序号)字段排序，确保输出顺序与现有文件一致
    ordered = sorted(
        records,
        key=lambda r: _to_int(_get_text((r.get("fields", {}) or {}).get("文本"))),
    )

    data: list[dict[str, str]] = []
    stage_ids: set[str] = set()
    for r in ordered:
        f = r["fields"]

        cat_one = _pick(f, *FIELD_CANDIDATES["category"])
        raw_name = _pick(f, "关卡名", "name", "名称")
        display_name = _pick(f, *FIELD_CANDIDATES["displayName"]) or raw_name
        note = _pick(f, *NOTE_FIELDS)

        stage_id = _pick(f, *FIELD_CANDIDATES["stageId"])
        level_id = _pick(f, *FIELD_CANDIDATES["levelId"]) or stage_id
        if not stage_id or not level_id or not cat_one or not display_name:
            raise ValueError(f"飞书关卡记录缺少关键信息: {r!r}")
        if stage_id in stage_ids:
            alternate_stage_id = _pick(f, "stageId_new")
            if alternate_stage_id and alternate_stage_id not in stage_ids:
                stage_id = alternate_stage_id
            else:
                raise ValueError(f"飞书关卡记录存在重复 stageId: {stage_id}")
        stage_ids.add(stage_id)

        # 兰台：catTwo 用期数（关卡名），catThree 用备注（阵型）
        if cat_one == "兰台":
            cat_two = _normalize_title(raw_name or display_name)
        else:
            cat_two = _normalize_title(display_name or raw_name)

        name = _normalize_title(display_name or raw_name or stage_id)
        cat_three = note or "无"
        end_time = _normalize_end_time(f.get(END_TIME_FIELD))

        item = {
            "catOne": cat_one,
            "catTwo": cat_two,
            "catThree": cat_three,
            "name": name,
            "levelId": level_id,
            "stageId": stage_id,
        }
        if end_time is not None:
            item["endTime"] = end_time
        data.append(item)

    return data


def main():
    """主执行函数"""
    token = fc.get_tenant_token()
    table = fc.TABLES[TABLE_KEY]
    field_names = fc.list_table_fields(table["app"], table["tbl"], token)
    validate_schema(field_names)
    projected_fields = [field for field in PROJECTED_FIELDS if field in field_names]
    records = fc.fetch_records(
        TABLE_KEY,
        token,
        sort_field="文本" if "文本" in field_names else None,
        field_names=projected_fields,
    )
    levels_data = transform_levels(records)
    for output_file in OUTPUT_FILES:
        fc.write_json_file(levels_data, str(output_file))


if __name__ == "__main__":
    main()
