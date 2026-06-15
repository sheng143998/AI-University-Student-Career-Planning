from __future__ import annotations

import json
import unittest

from fastapi.testclient import TestClient

from career_ai.resume_analysis_service import (
    ResumeValidationError,
    analyze_resume,
    app,
    build_summary_index,
    metadata_filter,
    recursive_chunk,
)


def sample_payload() -> dict:
    return {
        "vector_store_id": "vs-1001",
        "user_id": 1001,
        "resume_text": """Name: Ada Chen
Target: AI Agent Intern
Location: Shanghai

Skills
Python, RAG, PostgreSQL, Vue, Docker, Git

Education
Future University Bachelor Computer Science 2024-2028

Projects
RAG career assistant project: built recursive chunking, BM25 retrieval, and evidence diagnostics.
Intern company project: used Python and PostgreSQL to build data pipelines.
""",
        "file_type": "txt",
        "original_file_name": "resume.txt",
        "resume_file_path": "https://oss.example/resume.txt",
        "metadata": {"visibility": "user", "source": "unit_test"},
    }


class ResumeAiServiceTest(unittest.TestCase):
    def test_analyze_resume_returns_contract_shape(self) -> None:
        result = analyze_resume(sample_payload())

        self.assertEqual("completed", result["status"])
        self.assertIn("parsed_data", result)
        self.assertIn("scores", result)
        self.assertIn("capability_profile", result)
        self.assertIn("rag_diagnostics", result)
        self.assertIn("Python", result["parsed_data"]["skills"])
        self.assertGreater(result["rag_diagnostics"]["chunk_count"], 0)
        self.assertTrue(result["rag_diagnostics"]["retrieval"]["bm25"])
        self.assertEqual("hash", result["rag_diagnostics"]["retrieval"]["embedding_fallback"])
        self.assertEqual("rrf", result["rag_diagnostics"]["retrieval"]["fusion"])

    def test_validation_rejects_missing_resume_text(self) -> None:
        payload = sample_payload()
        payload["resume_text"] = " "

        with self.assertRaises(ResumeValidationError):
            analyze_resume(payload)

    def test_recursive_chunking_summary_and_metadata_filter(self) -> None:
        payload = sample_payload()
        metadata = {
            "user_id": payload["user_id"],
            "document_type": "resume",
            "vector_store_id": payload["vector_store_id"],
            "visibility": "user",
        }

        chunks = recursive_chunk(payload["resume_text"], metadata, max_chars=80, overlap=10)
        summaries = build_summary_index(chunks, payload["resume_text"])
        filtered = metadata_filter(chunks, user_id=1001, vector_store_id="vs-1001")
        blocked = metadata_filter(chunks, user_id=2002, vector_store_id="vs-1001")
        wrong_type = recursive_chunk(payload["resume_text"], {**metadata, "document_type": "job"}, max_chars=80, overlap=10)
        blocked_type = metadata_filter(wrong_type, user_id=1001, vector_store_id="vs-1001")

        self.assertGreater(len(chunks), 1)
        self.assertGreaterEqual(len(summaries), 2)
        self.assertEqual(len(chunks), len(filtered))
        self.assertEqual([], blocked)
        self.assertEqual([], blocked_type)

    def test_diagnostics_do_not_include_full_resume_or_email(self) -> None:
        payload = sample_payload()
        payload["resume_text"] += "\nContact: ada@example.com 138 0000 0000\n"

        result = analyze_resume(payload)
        diagnostics_text = json.dumps(result["rag_diagnostics"], ensure_ascii=False)

        self.assertNotIn("ada@example.com", diagnostics_text)
        self.assertNotIn("138 0000 0000", diagnostics_text)
        self.assertFalse(result["rag_diagnostics"]["sensitive_text_included"])

    def test_fastapi_endpoint_returns_200_and_400(self) -> None:
        client = TestClient(app)

        response = client.post("/api/v1/resume/analyze", json=sample_payload())
        self.assertEqual(200, response.status_code)
        self.assertEqual("completed", response.json()["status"])

        response = client.post("/api/v1/resume/analyze", json={"user_id": 1})
        self.assertEqual(400, response.status_code)
        self.assertIn("message", response.json())


if __name__ == "__main__":
    unittest.main()
