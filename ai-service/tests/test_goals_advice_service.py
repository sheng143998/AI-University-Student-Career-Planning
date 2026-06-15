import json
import unittest

from fastapi.testclient import TestClient

from app.main import app
from career_ai.goals_advice_service import (
    build_summary_index,
    expand_queries,
    generate_goal_advice,
    rag_fusion,
    recursive_chunks,
)


def sample_payload() -> dict:
    return {
        "userId": 10001,
        "goal": {
            "id": 7,
            "title": "成为 AI 应用开发工程师",
            "desc": "补齐 Python RAG 与工程化能力，联系 13812345678，邮箱 test@example.com",
            "status": "IN_PROGRESS",
            "progress": 45,
            "eta": "2026年9月",
            "rawResume": {"nested": "secret-token sk-abcdefghijkl sk-proj-abcdefghijklmnop"},
        },
        "milestones": [
            {"id": 1, "title": "完成 RAG 项目", "desc": "实现检索、重排和评估", "status": "IN_PROGRESS", "progress": 40},
            {"id": 2, "title": "整理面试证据", "desc": "沉淀项目复盘", "status": "TODO", "progress": 0},
        ],
        "successCriteria": {
            "salary": "15k-25k",
            "companies": ["字节跳动"],
            "cities": ["杭州"],
        },
        "retrievalOptions": {
            "metadataFilters": {
                "userId": "10001",
                "goalId": "7",
                "documentTypes": ["goal", "milestone", "successCriteria"],
                "visibilityScope": "USER_PRIVATE",
            }
        },
    }


class GoalsRagServiceTest(unittest.TestCase):

    def test_generate_goal_advice_returns_contract_fields(self):
        result = generate_goal_advice(sample_payload())

        self.assertIn("content", result)
        self.assertIn("evidenceReferences", result)
        self.assertIn("retrievalDiagnostics", result)
        self.assertEqual("10001", result["retrievalDiagnostics"]["metadataFilters"]["userId"])
        self.assertEqual("7", result["retrievalDiagnostics"]["metadataFilters"]["goalId"])
        self.assertEqual("multi_query+bm25+embedding", result["retrievalDiagnostics"]["retrieval"])
        self.assertEqual("rag_fusion_rrf", result["retrievalDiagnostics"]["fusion"])
        self.assertEqual("deterministic_fallback", result["retrievalDiagnostics"]["reranker"])
        self.assertGreaterEqual(len(result["evidenceReferences"]), 1)
        self.assertIn("sourceType", result["evidenceReferences"][0])
        self.assertIn("sourceId", result["evidenceReferences"][0])
        self.assertIn("reason", result["evidenceReferences"][0])

    def test_recursive_chunking_summary_index_and_fusion_are_used(self):
        payload = sample_payload()
        chunks = recursive_chunks("第一段。" * 80, max_chars=80)
        records = build_summary_index(payload)
        queries = expand_queries(payload)
        fused = rag_fusion(queries, records)

        self.assertGreater(len(chunks), 1)
        self.assertGreaterEqual(len(records), 3)
        self.assertEqual(4, len(queries))
        self.assertTrue(all(0.0 <= score <= 1.0 for score in fused.values()))

    def test_empty_milestones_use_fallback_without_error(self):
        payload = sample_payload()
        payload["milestones"] = []

        result = generate_goal_advice(payload)

        self.assertIn("content", result)
        self.assertGreaterEqual(result["retrievalDiagnostics"]["candidateCount"], 1)

    def test_missing_goal_is_validation_error(self):
        with self.assertRaises(ValueError):
            generate_goal_advice({"userId": 1})

    def test_diagnostics_do_not_leak_pii_or_nested_objects(self):
        result = generate_goal_advice(sample_payload())
        serialized = json.dumps(result, ensure_ascii=False)

        self.assertNotIn("13812345678", serialized)
        self.assertNotIn("test@example.com", serialized)
        self.assertNotIn("sk-abcdefghijkl", serialized)
        self.assertNotIn("sk-proj-abcdefghijklmnop", serialized)
        self.assertNotIn("nested", serialized)

    def test_metadata_filters_use_trusted_top_level_identity(self):
        payload = sample_payload()
        payload["retrievalOptions"]["metadataFilters"]["userId"] = "20002"
        payload["retrievalOptions"]["metadataFilters"]["goalId"] = "999"

        result = generate_goal_advice(payload)

        filters = result["retrievalDiagnostics"]["metadataFilters"]
        self.assertEqual("10001", filters["userId"])
        self.assertEqual("7", filters["goalId"])

    def test_document_type_filter_limits_summary_index_and_evidence(self):
        payload = sample_payload()
        payload["retrievalOptions"]["metadataFilters"]["documentTypes"] = ["milestone"]

        records = build_summary_index(payload)
        result = generate_goal_advice(payload)

        self.assertGreaterEqual(len(records), 1)
        self.assertTrue(all(record.metadata["documentType"] == "milestone" for record in records))
        self.assertEqual(["milestone"], result["retrievalDiagnostics"]["metadataFilters"]["documentTypes"])
        self.assertTrue(
            all(item["sourceType"] == "milestone" for item in result["evidenceReferences"])
        )


class GoalsRagHttpHandlerTest(unittest.TestCase):

    def setUp(self):
        self.client = TestClient(app)

    def post(self, path: str, body: object) -> tuple[int, dict]:
        response = self.client.post(path, content=json.dumps(body, ensure_ascii=False))
        return response.status_code, response.json()

    def test_internal_goals_advice_route_returns_camel_case_contract(self):
        status, body = self.post("/internal/goals/advice", sample_payload())

        self.assertEqual(200, status)
        self.assertIn("content", body)
        self.assertIn("evidenceReferences", body)
        self.assertIn("retrievalDiagnostics", body)

    def test_invalid_body_maps_to_400(self):
        status, body = self.post("/internal/goals/advice", [])

        self.assertEqual(400, status)
        self.assertIn("message", body)

    def test_missing_goal_maps_to_400(self):
        status, body = self.post("/internal/goals/advice", {"userId": 1})

        self.assertEqual(400, status)
        self.assertEqual("goal is required", body["message"])

    def test_unknown_route_maps_to_404(self):
        status, body = self.post("/internal/goals/missing", sample_payload())

        self.assertEqual(404, status)
        self.assertIn("message", body)

    def test_legacy_goals_route_is_not_available(self):
        status, body = self.post("/api/v1/goals" + "/advice", sample_payload())

        self.assertEqual(404, status)
        self.assertIn("message", body)


if __name__ == "__main__":
    unittest.main()
