import tempfile
import unittest
from pathlib import Path
from unittest.mock import call, patch

import feishu_common as fc
from sync_levels import (
    END_TIME_FIELD,
    PROJECTED_FIELDS,
    _normalize_end_time,
    transform_levels,
    validate_schema,
)


class SyncLevelsTest(unittest.TestCase):
    def test_records_search_paginates_with_query_params(self):
        responses = [
            {
                "data": {
                    "items": [{"record_id": "first"}],
                    "has_more": True,
                    "page_token": "next-page",
                }
            },
            {
                "data": {
                    "items": [{"record_id": "second"}],
                    "has_more": False,
                }
            },
        ]

        with patch.object(fc, "_request_json", side_effect=responses) as request_json, patch.object(
            fc.time, "sleep"
        ):
            result = fc.list_records(
                "app-token",
                "table-id",
                "tenant-token",
                sort_field="文本",
                field_names=["stageId", "结束时间"],
            )

        self.assertEqual(
            result,
            [{"record_id": "first"}, {"record_id": "second"}],
        )
        url = f"{fc.BASE_URL}/open-apis/bitable/v1/apps/app-token/tables/table-id/records/search"
        common = {
            "headers": {"Authorization": "Bearer tenant-token"},
            "json": {
                "field_names": ["stageId", "结束时间"],
                "sort": [{"field_name": "文本", "desc": False}],
            },
            "timeout": 30,
        }
        self.assertEqual(
            request_json.call_args_list,
            [
                call("POST", url, params={"page_size": 500}, **common),
                call(
                    "POST",
                    url,
                    params={"page_size": 500, "page_token": "next-page"},
                    **common,
                ),
            ],
        )

    def test_projects_end_time_field(self):
        self.assertIn(END_TIME_FIELD, PROJECTED_FIELDS)

    def test_maps_ids_and_end_time_without_start_time(self):
        records = [
            {
                "fields": {
                    "文本": "2",
                    "stageId": "",
                    "stageId_new": "stage-new",
                    "levelId": "",
                    "levelId_new": "level-new",
                    "关卡分类": "活动",
                    "显示名称": "活动名称",
                    "关卡名": "原始名称",
                    "结束时间": 1788192000000,
                }
            },
            {
                "fields": {
                    "文本": "1",
                    "stageId": "stage-old",
                    "levelId": "level-old",
                    "关卡分类": "主线",
                    "显示名称": "主线名称",
                    "关卡名": "主线名称",
                    "结束时间": "",
                }
            },
        ]

        result = transform_levels(records)

        self.assertEqual(result[0]["stageId"], "stage-old")
        self.assertEqual(result[1]["stageId"], "stage-new")
        self.assertEqual(result[1]["levelId"], "level-new")
        self.assertEqual(result[1]["endTime"], "2026-09-01T00:00:00.000+08:00")
        self.assertNotIn("endTime", result[0])
        self.assertNotIn("startTime", result[1])

    def test_uses_unique_alternate_stage_id_for_duplicate_legacy_id(self):
        records = [
            {
                "fields": {
                    "文本": "1",
                    "stageId": "legacy-stage",
                    "stageId_new": "first-stage",
                    "levelId": "level-1",
                    "关卡分类": "活动",
                    "显示名称": "第一关",
                    "关卡名": "第一关",
                    "结束时间": "",
                }
            },
            {
                "fields": {
                    "文本": "2",
                    "stageId": "legacy-stage",
                    "stageId_new": "second-stage",
                    "levelId": "level-2",
                    "关卡分类": "活动",
                    "显示名称": "第二关",
                    "关卡名": "第二关",
                    "结束时间": "",
                }
            },
        ]

        result = transform_levels(records)

        self.assertEqual([item["stageId"] for item in result], ["legacy-stage", "second-stage"])

    def test_normalizes_iso_and_rejects_invalid_time(self):
        self.assertEqual(
            _normalize_end_time("2026-09-01 00:00"),
            "2026-09-01T00:00:00.000+08:00",
        )
        self.assertEqual(
            _normalize_end_time("2026-09-01T00:00:00.000+08:00"),
            "2026-09-01T00:00:00.000+08:00",
        )
        with self.assertRaises(ValueError):
            _normalize_end_time("not-a-date")
        with self.assertRaises(ValueError):
            _normalize_end_time(1788192000000.5)

    def test_schema_and_malformed_record_fail(self):
        validate_schema({"stageId", "levelId", "关卡分类", "显示名称", "结束时间"})
        with self.assertRaises(ValueError):
            validate_schema({"stageId", "levelId", "关卡分类", "显示名称"})
        with self.assertRaises(ValueError):
            transform_levels([{"fields": {"stageId": "stage"}}])

    def test_atomic_writer_keeps_previous_file_on_serialization_failure(self):
        with tempfile.TemporaryDirectory() as directory:
            target = Path(directory) / "levels.json"
            fc.write_json_file([{"stageId": "old"}], str(target))
            with self.assertRaises(TypeError):
                fc.write_json_file({object()}, str(target))
            self.assertEqual(target.read_text(encoding="utf-8"), '[\n  {\n    "stageId": "old"\n  }\n]\n')


if __name__ == "__main__":
    unittest.main()
