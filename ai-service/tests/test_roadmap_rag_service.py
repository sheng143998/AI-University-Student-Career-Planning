import unittest
from unittest.mock import Mock

from app.main import AiServiceHandler
from career_ai.roadmap_rag_service import (
    build_summary_index,
    generate_multi_queries,
    generate_roadmap_recommendations,
    reciprocal_rank_fusion,
    recursive_chunk_text,
)


def _payload():
    return {
        "userId": 7,
        "currentJob": "Frontend Engineer",
        "userSkills": ["Vue", "TypeScript", "Python"],
        "resumeData": {
            "target_role": "AI Application Engineer",
            "current_role": "Frontend Engineer",
            "experience_years": 1,
            "skills": ["Vue", "TypeScript", "Python"],
            "match_score": 78,
            "name": "Sensitive Name",
            "education": [{"school": "Sensitive University"}],
            "experience": [{"company": "Sensitive Company", "description": "call 13812345678 or email me@example.com"}],
        },
        "jobs": [
            {
                "id": 1,
                "categoryCode": "FRONTEND_JUNIOR",
                "baseCategoryCode": "FRONTEND",
                "name": "Frontend Engineer",
                "level": "JUNIOR",
                "levelName": "Junior",
                "requiredSkills": ["Vue", "TypeScript"],
                "description": "Build web frontend.",
                "profile": "{}",
                "salaryRange": "10-15 K/month",
            },
            {
                "id": 2,
                "categoryCode": "AI_APP_JUNIOR",
                "baseCategoryCode": "AI_APP",
                "name": "AI Application Engineer",
                "level": "JUNIOR",
                "levelName": "Junior",
                "requiredSkills": ["Python", "RAG", "Prompt Engineering"],
                "description": "Build AI applications and RAG retrieval pipelines.",
                "profile": "{}",
                "salaryRange": "12-18 K/month",
            },
            {
                "id": 3,
                "categoryCode": "DATA_JUNIOR",
                "baseCategoryCode": "DATA",
                "name": "Data Engineer",
                "level": "JUNIOR",
                "levelName": "Junior",
                "requiredSkills": ["Python", "SQL", "ETL"],
                "description": "Build data pipelines for analytics.",
                "profile": "{}",
                "salaryRange": "11-16 K/month",
            },
        ],
        "jdSummaries": [
            {
                "jobId": 2,
                "categoryCode": "AI_APP_JUNIOR",
                "baseCategoryCode": "AI_APP",
                "level": "JUNIOR",
                "title": "AI Application Engineer JD",
                "company": "Sensitive JD Company",
                "requiredSkills": ["Python", "RAG"],
                "requirements": "Need to build RAG pipelines and guard secrets.",
                "description": "Call 13912345678 or email jd@example.com",
            }
        ],
        "retrieval": {
            "topK": 10,
            "filters": {
                "excludeSameCategory": True,
                "documentTypes": ["job", "resume_summary", "jd_summary"],
            },
        },
    }


class RoadmapRagServiceTest(unittest.TestCase):
    def test_recursive_chunk_text_splits_long_text(self):
        chunks = recursive_chunk_text("。".join(["RAG recursive chunk"] * 80), max_chars=80, overlap=10)

        self.assertGreater(len(chunks), 1)
        self.assertTrue(all(len(chunk) <= 80 for chunk in chunks))

    def test_build_summary_index_preserves_metadata_and_whitelists_resume(self):
        records = build_summary_index(_payload())

        job_record = next(record for record in records if record["metadata"]["documentType"] == "job")
        jd_record = next(record for record in records if record["metadata"]["documentType"] == "jd_summary")
        self.assertEqual(7, job_record["metadata"]["userId"])
        self.assertEqual("summary_index", job_record["metadata"]["source"])
        self.assertIn("chunkId", job_record)
        self.assertEqual("jd_context", jd_record["metadata"]["visibilityScope"])

        serialized = str(records)
        self.assertNotIn("Sensitive Name", serialized)
        self.assertNotIn("Sensitive University", serialized)
        self.assertNotIn("Sensitive Company", serialized)
        self.assertNotIn("Sensitive JD Company", serialized)
        self.assertNotIn("13812345678", serialized)
        self.assertNotIn("13912345678", serialized)
        self.assertNotIn("me@example.com", serialized)
        self.assertNotIn("jd@example.com", serialized)

    def test_generate_multi_queries_contains_roadmap_intents(self):
        queries = generate_multi_queries(_payload())

        self.assertGreaterEqual(len(queries), 4)
        self.assertTrue(any("skill gap" in query for query in queries))
        self.assertTrue(any("learning path" in query for query in queries))

    def test_reciprocal_rank_fusion_combines_rank_lists(self):
        fused = reciprocal_rank_fusion([[("a", 1.0), ("b", 0.5)], [("b", 0.9), ("c", 0.4)]])

        self.assertGreater(fused["b"], fused["a"] * 0.9)
        self.assertEqual({"a", "b", "c"}, set(fused))

    def test_generate_roadmap_recommendations_returns_raw_contract(self):
        result = generate_roadmap_recommendations(_payload())

        self.assertIn("lateralPaths", result)
        self.assertIn("diagnostics", result)
        self.assertNotIn("code", result)
        self.assertTrue(result["lateralPaths"])
        top = result["lateralPaths"][0]
        self.assertIn(top["targetCategoryCode"], {"AI_APP", "DATA"})
        self.assertIn("evidence", top)
        self.assertEqual("rrf", result["diagnostics"]["fusion"])

    def test_document_type_filter_excludes_resume_summary(self):
        payload = _payload()
        payload["retrieval"]["filters"]["documentTypes"] = ["resume_summary"]

        result = generate_roadmap_recommendations(payload)

        self.assertEqual(["resume_summary"], result["diagnostics"]["filters"]["documentTypes"])
        self.assertEqual(1, result["diagnostics"]["candidateCount"])

    def test_jd_summary_document_type_is_indexed(self):
        payload = _payload()
        payload["retrieval"]["filters"]["documentTypes"] = ["jd_summary"]

        result = generate_roadmap_recommendations(payload)

        self.assertEqual(["jd_summary"], result["diagnostics"]["filters"]["documentTypes"])
        self.assertEqual(1, result["diagnostics"]["candidateCount"])

    def test_exclude_same_category_filter_omits_current_category(self):
        result = generate_roadmap_recommendations(_payload())

        self.assertTrue(all(item["targetCategoryCode"] != "FRONTEND" for item in result["lateralPaths"]))

    def test_empty_jobs_returns_empty_recommendations_with_diagnostics(self):
        payload = _payload()
        payload["jobs"] = []
        payload["jdSummaries"] = []

        result = generate_roadmap_recommendations(payload)

        self.assertEqual([], result["lateralPaths"])
        self.assertEqual(0, result["diagnostics"]["candidateCount"])

    def test_invalid_field_types_are_tolerated(self):
        payload = _payload()
        payload["jobs"][1]["requiredSkills"] = "not-a-list"
        payload["retrieval"]["topK"] = "bad"

        result = generate_roadmap_recommendations(payload)

        self.assertIn("lateralPaths", result)
        self.assertIn("diagnostics", result)

    def test_diagnostics_do_not_leak_sensitive_tokens(self):
        payload = _payload()
        current_marker = "sk-" + "secret-1234567890"
        skill_marker = "sk-" + "skill-1234567890"
        bare_user_marker = "sk-" + "user-1234567890"
        filter_marker = "sk-" + "filter-1234567890"
        doc_marker = "sk-" + "doc-1234567890"
        payload["currentJob"] = "Frontend Engineer " + "api" + "_key=" + current_marker
        payload["userSkills"].append("api" + "_key=" + skill_marker)
        payload["userSkills"].append(bare_user_marker)
        payload["resumeData"]["skills"] = ["Python", "token" + "=resumeToken123456789"]
        payload["retrieval"]["filters"]["credentialMarker"] = filter_marker
        payload["retrieval"]["filters"]["documentTypes"].append("token" + "=docTypeToken123456789")
        payload["retrieval"]["filters"]["documentTypes"].append(doc_marker)

        result = generate_roadmap_recommendations(payload)
        serialized = str(result)

        self.assertNotIn("13812345678", serialized)
        self.assertNotIn("me@example.com", serialized)
        self.assertNotIn(current_marker, serialized)
        self.assertNotIn(skill_marker, serialized)
        self.assertNotIn(bare_user_marker, serialized)
        self.assertNotIn("resumeToken123456789", serialized)
        self.assertNotIn(filter_marker, serialized)
        self.assertNotIn("docTypeToken123456789", serialized)
        self.assertNotIn(doc_marker, serialized)
        self.assertEqual({"excludeSameCategory", "documentTypes"}, set(result["diagnostics"]["filters"]))

    def test_ai_service_handler_exposes_roadmap_endpoint(self):
        handler = object.__new__(AiServiceHandler)
        handler.path = "/api/roadmap/recommendations/personalized"
        handler._read_json = Mock(return_value=_payload())
        handler._write_json = Mock()

        AiServiceHandler.do_POST(handler)

        payload = handler._write_json.call_args.args[0]
        self.assertIn("lateralPaths", payload)
        self.assertIn("diagnostics", payload)


if __name__ == "__main__":
    unittest.main()
