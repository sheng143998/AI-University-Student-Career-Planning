from __future__ import annotations

import unittest

from fastapi.testclient import TestClient

from app.main import app
from career_ai.dashboard_rag_service import build_summary_index, match_target_job, recursive_chunk


def sample_payload() -> dict:
    return {
        "request_id": "dashboard-target-job-1-10",
        "user_id": 1,
        "resume_analysis_id": 10,
        "resume_vector_store_id": "resume_vec_10",
        "resume_profile": {
            "target_role": "AI算法工程师",
            "skills": ["Python", "机器学习", "RAG"],
            "experience_years": 1,
        },
        "resume_content": (
            "项目经历：使用 Python 构建 RAG 检索增强系统，包含 BM25、向量检索、"
            "RRF 重排和模型部署。熟悉机器学习训练和评估。"
        ),
        "job_candidates": [
            {
                "job_id": 101,
                "job_name": "AI算法工程师",
                "job_category_code": "AI_ENGINEER_JUNIOR",
                "job_level": "JUNIOR",
                "job_level_name": "初级",
                "required_skills": ["Python", "机器学习", "模型部署"],
                "job_description": "负责模型训练、评估、RAG 应用和服务部署。",
                "job_profile": {"industrySegment": "人工智能"},
            },
            {
                "job_id": 202,
                "job_name": "UI设计师",
                "job_category_code": "UI_DESIGN_JUNIOR",
                "job_level": "JUNIOR",
                "job_level_name": "初级",
                "required_skills": ["Figma", "视觉设计"],
                "job_description": "负责界面视觉设计和交互规范。",
                "job_profile": {"industrySegment": "设计"},
            },
        ],
        "filters": {
            "document_type": ["resume", "job", "jd"],
            "visibility_scope": "user_or_public",
            "language": "zh-CN",
        },
        "top_k": 5,
    }


class DashboardRagServiceTest(unittest.TestCase):
    def test_recursive_chunk_splits_long_text(self) -> None:
        text = "。".join([f"第{i}段包含 Python 和机器学习项目经验" for i in range(80)])
        chunks = recursive_chunk(text, max_chars=120, overlap=20)
        self.assertGreater(len(chunks), 3)
        self.assertTrue(all(len(chunk) <= 120 for chunk in chunks))

    def test_summary_index_preserves_metadata_filters(self) -> None:
        records = build_summary_index(sample_payload())
        self.assertTrue(any(record.metadata["document_type"] == "resume" for record in records))
        job_records = [record for record in records if record.metadata["document_type"] == "job"]
        self.assertTrue(job_records)
        self.assertTrue(all(record.metadata["visibility_scope"] == "public_job" for record in job_records))
        self.assertTrue(all(record.metadata["language"] == "zh-CN" for record in job_records))
        self.assertTrue(any(record.metadata["job_id"] == 101 for record in job_records))

    def test_summary_index_has_document_section_and_chunk_records(self) -> None:
        records = build_summary_index(sample_payload())
        records_by_id = {record.record_id: record for record in records}
        levels = {record.metadata["summary_level"] for record in records}

        self.assertEqual({"document", "section", "chunk"}, levels)
        for record in records:
            metadata = record.metadata
            self.assertEqual(record.record_id, metadata["record_id"])
            self.assertIn("user_id", metadata)
            self.assertIn("document_type", metadata)
            self.assertIn("document_id", metadata)
            self.assertIn("section", metadata)
            self.assertIn("visibility_scope", metadata)
            self.assertIn("language", metadata)
            if metadata["summary_level"] == "document":
                self.assertIsNone(metadata["parent_id"])
            else:
                self.assertIn(metadata["parent_id"], records_by_id)

        resume_document = records_by_id["resume_vec_10:document"]
        self.assertEqual("document", resume_document.metadata["summary_level"])
        resume_section = records_by_id["resume_vec_10:section:resume_content"]
        self.assertEqual("section", resume_section.metadata["summary_level"])
        self.assertEqual("resume_vec_10:document", resume_section.metadata["parent_id"])
        resume_chunk = next(
            record
            for record in records
            if record.record_id.startswith("resume_vec_10:chunk:")
            and record.metadata["section"] == "resume_content"
        )
        self.assertEqual("chunk", resume_chunk.metadata["summary_level"])
        self.assertEqual("resume_vec_10:section:resume_content", resume_chunk.metadata["parent_id"])

        job_document = records_by_id["job_101:document"]
        self.assertEqual("document", job_document.metadata["summary_level"])
        job_section = records_by_id["job_101:section:job_requirement"]
        self.assertEqual("job_101:document", job_section.metadata["parent_id"])
        job_chunk = next(record for record in records if record.record_id.startswith("job_101:chunk:"))
        self.assertEqual("chunk", job_chunk.metadata["summary_level"])
        self.assertIn(job_chunk.metadata["parent_id"], records_by_id)

    def test_match_target_job_returns_evidence_and_diagnostics(self) -> None:
        response = match_target_job(sample_payload())
        self.assertEqual(1, response["code"])
        data = response["data"]
        self.assertEqual(101, data["matched_job"]["job_id"])
        self.assertEqual("rrf", data["retrieval"]["fusion_method"])
        self.assertEqual("deterministic-fallback", data["retrieval"]["reranker"])
        self.assertGreater(data["retrieval"]["candidate_count"], 0)
        self.assertTrue(data["retrieval"]["query_variants"])
        self.assertTrue(data["retrieval"]["selected_evidence_ids"])
        self.assertTrue(data["evidence_refs"])
        self.assertTrue(
            all(":chunk:" in evidence["source_id"] for evidence in data["evidence_refs"])
        )
        self.assertTrue(
            all(":chunk:" in evidence_id for evidence_id in data["retrieval"]["selected_evidence_ids"])
        )

    def test_match_target_job_top_one_still_returns_chunk_evidence(self) -> None:
        payload = sample_payload()
        payload["top_k"] = 1

        response = match_target_job(payload)

        self.assertEqual(1, response["code"])
        evidence_refs = response["data"]["evidence_refs"]
        selected_evidence_ids = response["data"]["retrieval"]["selected_evidence_ids"]
        self.assertTrue(evidence_refs)
        self.assertTrue(selected_evidence_ids)
        self.assertTrue(all(":chunk:" in item["source_id"] for item in evidence_refs))
        self.assertTrue(all(":chunk:" in item for item in selected_evidence_ids))

    def test_match_target_job_no_match_returns_contract(self) -> None:
        payload = sample_payload()
        payload["resume_profile"]["skills"] = ["水彩", "版式"]
        payload["resume_content"] = "作品集以插画、水彩和品牌视觉为主。"
        payload["job_candidates"] = [
            {
                "job_id": 303,
                "job_name": "后端开发工程师",
                "job_category_code": "BACKEND_JUNIOR",
                "job_level": "JUNIOR",
                "job_level_name": "初级",
                "required_skills": ["Java", "Spring", "PostgreSQL"],
                "job_description": "负责后端服务开发。",
                "job_profile": {"industrySegment": "软件开发"},
            }
        ]

        response = match_target_job(payload)

        self.assertEqual(0, response["code"])
        self.assertEqual("NO_MATCH", response["msg"])
        self.assertEqual([], response["data"]["evidence_refs"])

    def test_scope_filter_blocks_public_job_evidence(self) -> None:
        payload = sample_payload()
        payload["filters"]["visibility_scope"] = "user_private"

        response = match_target_job(payload)

        self.assertEqual(0, response["code"])
        self.assertEqual("NO_MATCH", response["msg"])
        self.assertEqual([], response["data"]["evidence_refs"])
        self.assertEqual([], response["data"]["retrieval"]["selected_evidence_ids"])
        self.assertEqual("user_private", response["data"]["retrieval"]["filters_applied"]["visibility_scope"])

    def test_document_type_filter_blocks_job_evidence(self) -> None:
        payload = sample_payload()
        payload["filters"]["document_type"] = ["resume"]

        response = match_target_job(payload)

        self.assertEqual(0, response["code"])
        self.assertEqual("NO_MATCH", response["msg"])
        self.assertEqual([], response["data"]["evidence_refs"])
        self.assertEqual(["resume"], response["data"]["retrieval"]["filters_applied"]["document_type"])

    def test_match_target_job_empty_candidates_is_validation_error(self) -> None:
        payload = sample_payload()
        payload["job_candidates"] = []
        with self.assertRaises(ValueError):
            match_target_job(payload)

    def test_http_endpoint_is_mounted(self) -> None:
        response = TestClient(app).post("/internal/dashboard/target-job/match", json=sample_payload())
        payload = response.json()

        self.assertEqual(200, response.status_code)
        self.assertEqual(1, payload["code"])
        self.assertEqual(101, payload["data"]["matched_job"]["job_id"])

    def test_http_endpoint_validation_error_uses_result_contract(self) -> None:
        payload = sample_payload()
        payload["job_candidates"] = []

        response = TestClient(app).post("/internal/dashboard/target-job/match", json=payload)
        body = response.json()

        self.assertEqual(400, response.status_code)
        self.assertEqual(0, body["code"])
        self.assertEqual("VALIDATION_ERROR", body["msg"])
        self.assertIn("job_candidates", body["data"]["error"])

    def test_non_dashboard_validation_error_keeps_legacy_shape(self) -> None:
        response = TestClient(app).post("/api/v1/market/insight", content="{invalid-json")
        payload = response.json()

        self.assertEqual(400, response.status_code)
        self.assertEqual("INVALID_JSON", payload["error"])
        self.assertNotIn("code", payload)

    def test_query_variants_do_not_include_raw_resume_content(self) -> None:
        payload = sample_payload()
        raw_fragment = "唯一原文片段ABC123用于检测不应出现在查询变体中"
        payload["resume_content"] = (
            f"{raw_fragment}，后续还有大量简历描述、项目经历、技能证明和实习经历。"
            "该片段用于验证 diagnostics 不回传原始简历文本。"
        )

        response = match_target_job(payload)

        query_variants = response["data"]["retrieval"]["query_variants"]
        joined_queries = "\n".join(query_variants)
        self.assertNotIn(raw_fragment, joined_queries)
        self.assertNotIn(payload["resume_content"][:30], joined_queries)

    def test_query_variants_drop_profile_pii_values(self) -> None:
        payload = sample_payload()
        payload["resume_profile"]["target_role"] = "student@example.com"
        payload["resume_profile"]["skills"] = [
            "Python",
            "13800000000",
            "api_key=abcdef1234567890abcdef123456",
        ]

        response = match_target_job(payload)

        query_variants = response["data"]["retrieval"]["query_variants"]
        joined_queries = "\n".join(query_variants)
        self.assertNotIn("student@example.com", joined_queries)
        self.assertNotIn("13800000000", joined_queries)
        self.assertNotIn("api_key", joined_queries)
        self.assertIn("Python", joined_queries)

    def test_summary_index_drops_non_numeric_experience_years(self) -> None:
        payload = sample_payload()
        payload["resume_profile"]["experience_years"] = "api_key=abcdef1234567890abcdef123456"

        records = build_summary_index(payload)

        summaries = "\n".join(record.summary + "\n" + record.text for record in records)
        self.assertNotIn("api_key", summaries)
        self.assertNotIn("abcdef1234567890abcdef123456", summaries)

    def test_nested_profile_values_do_not_enter_queries_or_summaries(self) -> None:
        payload = sample_payload()
        payload["resume_profile"]["target_role"] = {"raw_text": "nested-secret-role"}
        payload["resume_profile"]["skills"] = [
            "Python",
            {"raw_text": "nested-secret-skill"},
            ["nested-list-skill"],
        ]

        response = match_target_job(payload)
        records = build_summary_index(payload)

        joined_queries = "\n".join(response["data"]["retrieval"]["query_variants"])
        summaries = "\n".join(record.summary + "\n" + record.text for record in records)
        self.assertNotIn("nested-secret-role", joined_queries)
        self.assertNotIn("nested-secret-skill", joined_queries)
        self.assertNotIn("nested-list-skill", joined_queries)
        self.assertNotIn("nested-secret-role", summaries)
        self.assertNotIn("nested-secret-skill", summaries)
        self.assertNotIn("nested-list-skill", summaries)
        self.assertIn("Python", joined_queries)


if __name__ == "__main__":
    unittest.main()
