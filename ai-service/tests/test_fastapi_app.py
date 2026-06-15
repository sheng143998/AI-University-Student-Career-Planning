from __future__ import annotations

from fastapi.testclient import TestClient

from app.main import app


def test_fastapi_health_endpoint() -> None:
    client = TestClient(app)

    response = client.get("/health")

    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_fastapi_reports_endpoint_returns_support_contract() -> None:
    client = TestClient(app)
    payload = {
        "reportId": 12,
        "userId": 1001,
        "targetJobName": "Java backend engineer",
        "capabilityProfile": {"overallScore": 85, "professionalSkills": ["Java", "Spring Boot"]},
        "careerData": {"targetJob": "Java backend engineer"},
        "resumeAnalysis": {"highlights": ["Spring Boot project"]},
        "matchDetails": {"overall": 85},
    }

    response = client.post("/api/v1/reports/generate-support", json=payload)

    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "OK"
    assert "aiSuggestions" in body
    assert "ragDiagnostics" in body


def test_fastapi_dashboard_validation_error_uses_result_shape() -> None:
    client = TestClient(app)

    response = client.post("/internal/dashboard/target-job/match", json={"job_candidates": []})

    assert response.status_code == 400
    body = response.json()
    assert body["code"] == 0
    assert body["msg"] == "VALIDATION_ERROR"
    assert "error" in body["data"]


def test_fastapi_unknown_route_uses_legacy_not_found_shape() -> None:
    client = TestClient(app)

    response = client.post("/internal/missing", json={})

    assert response.status_code == 404
    assert response.json() == {"error": "NOT_FOUND", "message": "unknown path"}
