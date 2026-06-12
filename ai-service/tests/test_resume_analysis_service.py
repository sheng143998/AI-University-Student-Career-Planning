from __future__ import annotations

import json
import socket
import threading
import time
import unittest
from http.client import HTTPConnection

from career_ai.resume_analysis_service import (
    ResumeAiHandler,
    ResumeValidationError,
    analyze_resume,
    build_summary_index,
    metadata_filter,
    recursive_chunk,
)
from http.server import ThreadingHTTPServer


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

    def test_http_handler_returns_200_and_400(self) -> None:
        port = free_port()
        server = ThreadingHTTPServer(("127.0.0.1", port), ResumeAiHandler)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        try:
            status, body = post_json(port, "/api/v1/resume/analyze", sample_payload())
            self.assertEqual(200, status)
            self.assertEqual("completed", body["status"])

            status, body = post_json(port, "/api/v1/resume/analyze", {"user_id": 1})
            self.assertEqual(400, status)
            self.assertIn("message", body)
        finally:
            server.shutdown()
            server.server_close()
            thread.join(timeout=3)
            self.assertFalse(thread.is_alive())


def free_port() -> int:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        sock.bind(("127.0.0.1", 0))
        return int(sock.getsockname()[1])


def post_json(port: int, path: str, payload: dict) -> tuple[int, dict]:
    conn = HTTPConnection("127.0.0.1", port, timeout=5)
    try:
        body = json.dumps(payload).encode("utf-8")
        conn.request("POST", path, body=body, headers={"Content-Type": "application/json"})
        response = conn.getresponse()
        raw = response.read().decode("utf-8")
        parsed = json.loads(raw)
        return response.status, parsed
    finally:
        conn.close()
        time.sleep(0.05)


if __name__ == "__main__":
    unittest.main()
