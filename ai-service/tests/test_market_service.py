from __future__ import annotations

import json
import threading
import unittest
from http.client import HTTPConnection
from http.server import ThreadingHTTPServer

from app.main import AiServiceHandler
from career_ai.market_service import generate_market_insight, generate_soft_skills


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

    def test_http_market_endpoints_are_mounted(self) -> None:
        server = ThreadingHTTPServer(("127.0.0.1", 0), AiServiceHandler)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        try:
            insight = self.post(server.server_port, "/api/v1/market/insight", sample_payload())
            self.assertEqual(200, insight[0])
            self.assertIn("marketSignals", insight[1])

            soft_skills = self.post(server.server_port, "/api/v1/market/soft-skills", sample_payload())
            self.assertEqual(200, soft_skills[0])
            self.assertEqual(5, len(soft_skills[1]))
        finally:
            server.shutdown()
            server.server_close()

    def post(self, port: int, path: str, payload: dict) -> tuple[int, object]:
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        conn = HTTPConnection("127.0.0.1", port, timeout=5)
        conn.request("POST", path, body=body, headers={"Content-Type": "application/json"})
        response = conn.getresponse()
        return response.status, json.loads(response.read().decode("utf-8"))


if __name__ == "__main__":
    unittest.main()
