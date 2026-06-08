import json
import unittest
from http import HTTPStatus
from unittest.mock import Mock

from ai_service.market_ai_service import MarketAiHandler
from ai_service.rag_feedback_service import accept_rag_feedback, validate_rag_preferences


class RagFeedbackServiceTest(unittest.TestCase):
    def test_accept_rag_feedback_returns_eval_contract(self):
        result = accept_rag_feedback(
            {
                "request_id": "rag-feedback-10001-20260608-001",
                "user_id": 10001,
                "target": {"type": "CHAT_MESSAGE", "id": "123", "page": "chat"},
                "feedback": {
                    "rating": 1,
                    "reason_tags": ["HELPFUL", "EVIDENCE_RELEVANT"],
                    "comment": "证据有帮助",
                    "user_action": "thumb_up",
                },
                "retrieval": {
                    "trace_id": "trace_20260608_10001_001",
                    "evidence_ref_ids": ["job_30001:chunk:4"],
                },
            }
        )

        self.assertEqual(1, result["code"])
        data = result["data"]
        self.assertTrue(data["accepted"])
        self.assertTrue(data["feedback_id"].startswith("rag_fb_"))
        self.assertIn("retrieval_eval", data["used_for"])
        self.assertEqual("positive", data["quality_dimensions"]["context_precision"])
        self.assertEqual(1, data["diagnostics"]["evidence_ref_count"])

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
                "request_id": "rag-preference-10001-20260608-001",
                "user_id": 10001,
                "preferences": {
                    "preferred_city": "深圳",
                    "preferred_industries": ["人工智能", "软件开发"],
                    "preferred_job_levels": ["JUNIOR", "MID", "UNKNOWN"],
                    "career_direction": "技术路线",
                    "result_language": "zh-CN",
                    "feedback_usage_scope": "local_eval_only",
                },
            }
        )

        data = result["data"]
        self.assertTrue(data["valid"])
        self.assertEqual("深圳", data["metadata_filters"]["city"])
        self.assertEqual(["JUNIOR", "MID"], data["metadata_filters"]["job_level"])
        self.assertEqual("user_or_public", data["metadata_filters"]["visibility_scope"])
        self.assertEqual("multi_query+bm25+embedding+rag_fusion", data["diagnostics"]["retrieval_mode"])

    def test_market_handler_exposes_internal_feedback_endpoint(self):
        handler = object.__new__(MarketAiHandler)
        handler.path = "/internal/rag/feedback"
        handler._read_json = Mock(
            return_value={
                "request_id": "rid",
                "user_id": 1,
                "target": {"type": "CHAT_MESSAGE", "id": "1"},
                "feedback": {"rating": 0},
            }
        )
        handler._write_json = Mock()

        MarketAiHandler.do_POST(handler)

        status, payload = handler._write_json.call_args.args
        self.assertEqual(HTTPStatus.OK, status)
        self.assertEqual(1, payload["code"])

    def test_market_handler_exposes_internal_preferences_endpoint(self):
        handler = object.__new__(MarketAiHandler)
        handler.path = "/internal/rag/preferences/validate"
        handler._read_json = Mock(
            return_value={
                "request_id": "rid",
                "user_id": 1,
                "preferences": {"preferred_city": "深圳", "result_language": "zh-CN"},
            }
        )
        handler._write_json = Mock()

        MarketAiHandler.do_POST(handler)

        status, payload = handler._write_json.call_args.args
        self.assertEqual(HTTPStatus.OK, status)
        self.assertEqual("深圳", payload["data"]["metadata_filters"]["city"])


if __name__ == "__main__":
    unittest.main()
