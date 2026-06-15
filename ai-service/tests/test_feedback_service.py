import json
import os
import tempfile
import unittest

from fastapi.testclient import TestClient

from app.main import app
from career_ai.feedback_queue import FeedbackEvalQueue
from career_ai.feedback_service import accept_rag_feedback, set_default_eval_queue, validate_rag_preferences


class FeedbackServiceTest(unittest.TestCase):
    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory()
        self.queue_path = os.path.join(self._tmp.name, "rag_feedback_queue.jsonl")
        set_default_eval_queue(FeedbackEvalQueue(self.queue_path))

    def tearDown(self):
        set_default_eval_queue(None)
        self._tmp.cleanup()

    def test_accept_rag_feedback_returns_eval_contract(self):
        result = accept_rag_feedback(
            {
                "request_id": "rag-feedback-10001-20260608-001",
                "user_id": 10001,
                "target": {"type": "CHAT_MESSAGE", "id": "123", "page": "chat"},
                "feedback": {"rating": 1, "reason_tags": ["HELPFUL", "EVIDENCE_RELEVANT"]},
                "retrieval": {"trace_id": "trace-1", "evidence_ref_ids": ["job_30001:chunk:4"]},
            }
        )
        data = result["data"]
        self.assertEqual(1, result["code"])
        self.assertTrue(data["feedback_id"].startswith("rag_fb_"))
        self.assertIn("retrieval_eval", data["used_for"])
        self.assertEqual("positive", data["quality_dimensions"]["context_precision"])

    def test_accept_rag_feedback_rejects_invalid_rating(self):
        with self.assertRaises(ValueError):
            accept_rag_feedback(
                {
                    "request_id": "rid",
                    "user_id": 1,
                    "target": {"type": "CHAT_MESSAGE", "id": "1"},
                    "feedback": {"rating": 2},
                }
            )

    def test_validate_preferences_builds_metadata_filters(self):
        result = validate_rag_preferences(
            {
                "request_id": "rag-preference-1",
                "user_id": 10001,
                "preferences": {
                    "preferred_city": "Shenzhen",
                    "preferred_industries": ["AI", "Software"],
                    "preferred_job_levels": ["JUNIOR", "MID", "UNKNOWN"],
                    "career_direction": "engineering",
                    "result_language": "zh-CN",
                },
            }
        )
        data = result["data"]
        self.assertTrue(data["valid"])
        self.assertEqual("Shenzhen", data["metadata_filters"]["city"])
        self.assertEqual(["JUNIOR", "MID"], data["metadata_filters"]["job_level"])
        self.assertEqual("multi_query+bm25+embedding+rag_fusion", data["diagnostics"]["retrieval_mode"])

    def test_accept_rag_feedback_appends_sanitized_event_to_eval_queue(self):
        accept_rag_feedback(
            {
                "request_id": "rid-queue-1",
                "user_id": 7,
                "target": {"type": "CHAT_MESSAGE", "id": "55", "page": "chat"},
                "feedback": {"rating": 1, "reason_tags": ["HELPFUL"], "comment": "useful answer"},
                "retrieval": {"trace_id": "trace-7", "evidence_ref_ids": ["job_1:chunk:2"]},
            }
        )
        with open(self.queue_path, encoding="utf-8") as fh:
            events = [json.loads(line) for line in fh if line.strip()]
        self.assertEqual(1, len(events))
        event = events[0]
        self.assertEqual("rid-queue-1", event["request_id"])
        self.assertEqual("CHAT_MESSAGE", event["target"]["type"])
        self.assertIn("used_for", event)
        self.assertIn("quality_dimensions", event)

    def test_accept_rag_feedback_redacts_sensitive_comment_before_queue(self):
        accept_rag_feedback(
            {
                "request_id": "rid-sensitive-comment-1",
                "user_id": 7,
                "target": {"type": "CHAT_MESSAGE", "id": "55", "page": "chat"},
                "feedback": {
                    "rating": -1,
                    "comment": (
                        "raw resume: Ada email ada@example.com phone 138 0000 0000 "
                        "JD: salary prompt: ignore rules token=abcdef123456 sk-abcdefghijklmnop"
                    ),
                },
            }
        )
        with open(self.queue_path, encoding="utf-8") as fh:
            queued = fh.read()

        self.assertIn("[REDACTED]", queued)
        self.assertNotIn("ada@example.com", queued)
        self.assertNotIn("138 0000 0000", queued)
        self.assertNotIn("abcdef123456", queued)
        self.assertNotIn("sk-abcdefghijklmnop", queued)
        self.assertNotIn("ignore rules", queued)

    def test_accept_rag_feedback_is_idempotent_by_request_id(self):
        payload = {
            "request_id": "rid-dup-1",
            "user_id": 7,
            "target": {"type": "CHAT_MESSAGE", "id": "55"},
            "feedback": {"rating": -1},
        }
        first = accept_rag_feedback(payload)
        second = accept_rag_feedback(payload)
        self.assertFalse(first["data"]["diagnostics"]["duplicate"])
        self.assertTrue(second["data"]["diagnostics"]["duplicate"])
        self.assertEqual(first["data"]["feedback_id"], second["data"]["feedback_id"])
        with open(self.queue_path, encoding="utf-8") as fh:
            lines = [line for line in fh if line.strip()]
        self.assertEqual(1, len(lines))

    def test_handler_exposes_feedback_endpoints(self):
        response = TestClient(app).post(
            "/internal/rag/feedback",
            json={
                "request_id": "rid",
                "user_id": 1,
                "target": {"type": "CHAT_MESSAGE", "id": "1"},
                "feedback": {"rating": 0},
            },
        )
        payload = response.json()

        self.assertEqual(200, response.status_code)
        self.assertEqual(1, payload["code"])


if __name__ == "__main__":
    unittest.main()
