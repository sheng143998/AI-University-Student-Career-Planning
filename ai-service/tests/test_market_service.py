from __future__ import annotations

import json
import unittest

from fastapi.testclient import TestClient

from app.main import app
from career_ai.market_service import (
    classify_job,
    generate_market_insight,
    generate_soft_skills,
    index_jobs,
    search_jobs,
)


def sample_payload() -> dict:
    return {
        "city": "Shenzhen",
        "job": {
            "jobName": "AI Application Engineer",
            "requiredSkills": json.dumps(["Python", "RAG", "Java"]),
            "sourceJobCount": 88,
            "minSalary": 15000,
            "maxSalary": 28000,
            "salaryUnit": "month",
            "capabilityRequirements": {
                "innovation": 72,
                "learning": 80,
                "resilience": 66,
                "communication": 70,
                "internship": 75,
            },
        },
    }


class MarketServiceTest(unittest.TestCase):
    def test_generate_market_insight_returns_java_vo_shape(self) -> None:
        result = generate_market_insight(sample_payload())

        self.assertEqual("AI Application Engineer market insight", result["title"])
        self.assertIn("VERY_HIGH", result["summary"])
        self.assertTrue(result["marketSignals"])
        self.assertTrue(result["industryTrends"])
        self.assertTrue(result["suggestedActions"])
        self.assertEqual("Demand level", result["marketSignals"][0]["label"])
        self.assertEqual("VERY_HIGH", result["marketSignals"][0]["value"])

    def test_generate_market_insight_accepts_comma_separated_skills(self) -> None:
        payload = sample_payload()
        payload["job"]["requiredSkills"] = "Python,RAG,Spring Boot"

        result = generate_market_insight(payload)

        self.assertIn("Python, RAG, Spring Boot", result["suggestedActions"][0]["desc"])

    def test_generate_soft_skills_clamps_scores(self) -> None:
        payload = sample_payload()
        payload["capabilityScores"] = {"innovation": 120, "learning": -5}

        result = generate_soft_skills(payload)

        scores = {item["name"]: item["score"] for item in result}
        self.assertEqual(100, scores["Innovation"])
        self.assertEqual(0, scores["Learning ability"])
        self.assertEqual(60, scores["Communication"])

    def test_classify_job_returns_result_shape(self) -> None:
        result = classify_job(
            {
                "job_id": 6001,
                "job_content": "AI Agent and RAG backend engineer, Python, PyTorch, 15-25K, 1 year experience",
                "job": {"jobName": "AI Application Engineer", "salaryRange": "15-25K"},
            }
        )

        self.assertEqual(1, result["code"])
        data = result["data"]
        self.assertEqual("AI_APP_JUNIOR", data["category_code"])
        self.assertEqual("JUNIOR", data["level"])
        self.assertEqual(15000, data["min_salary"])
        self.assertIn("Python", data["required_skills"])
        self.assertIn("RAG", data["required_skills"])

    def test_index_jobs_returns_deterministic_embedding_records(self) -> None:
        payload = {
            "jobs": [
                {
                    "job_id": 6001,
                    "content": "AI Application Engineer\nPython RAG LLM",
                    "metadata": {"document_type": "jd", "source": "test", "raw_text": "must not pass"},
                }
            ]
        }

        first = index_jobs(payload)
        second = index_jobs(payload)

        record = first["data"]["records"][0]
        self.assertEqual(1, first["code"])
        self.assertEqual(record["embedding"], second["data"]["records"][0]["embedding"])
        self.assertEqual(6001, record["job_id"])
        self.assertNotIn("raw_text", record["metadata"])
        self.assertTrue(record["embedding"].startswith("["))

    def test_search_jobs_returns_ranked_job_ids(self) -> None:
        result = search_jobs(
            {
                "query_text": "Python RAG backend",
                "limit": 2,
                "jobs": [
                    {
                        "job_id": 1,
                        "job_name": "Frontend Engineer",
                        "required_skills": ["Vue", "CSS"],
                    },
                    {
                        "job_id": 2,
                        "job_name": "AI Application Engineer",
                        "required_skills": ["Python", "RAG"],
                        "job_description": "Build backend RAG applications",
                    },
                ],
            }
        )

        self.assertEqual(1, result["code"])
        self.assertEqual(2, result["data"]["job_ids"][0])
        self.assertEqual("rrf", result["data"]["retrieval"]["fusion_method"])

    def test_http_market_endpoints_are_mounted(self) -> None:
        client = TestClient(app)
        insight = self.post(client, "/api/v1/market/insight", sample_payload())
        self.assertEqual(200, insight[0])
        self.assertIn("marketSignals", insight[1])

        soft_skills = self.post(client, "/api/v1/market/soft-skills", sample_payload())
        self.assertEqual(200, soft_skills[0])
        self.assertEqual(5, len(soft_skills[1]))

        classification = self.post(
            client,
            "/internal/market/jobs/classify",
            {"job_content": "Java Spring backend engineer 20-30K", "job": {"jobName": "Java Engineer"}},
        )
        self.assertEqual(200, classification[0])
        self.assertEqual(1, classification[1]["code"])

        indexed = self.post(
            client,
            "/internal/market/jobs/index",
            {"jobs": [{"job_id": 1, "content": "Python RAG engineer", "metadata": {"source": "test"}}]},
        )
        self.assertEqual(200, indexed[0])
        self.assertEqual(1, indexed[1]["code"])

        searched = self.post(
            client,
            "/internal/market/jobs/search",
            {
                "query_text": "Python RAG",
                "jobs": [{"job_id": 1, "job_name": "AI Engineer", "required_skills": ["Python", "RAG"]}],
            },
        )
        self.assertEqual(200, searched[0])
        self.assertEqual([1], searched[1]["data"]["job_ids"])

    def post(self, client: TestClient, path: str, payload: dict) -> tuple[int, object]:
        response = client.post(path, content=json.dumps(payload, ensure_ascii=False))
        return response.status_code, response.json()


if __name__ == "__main__":
    unittest.main()
