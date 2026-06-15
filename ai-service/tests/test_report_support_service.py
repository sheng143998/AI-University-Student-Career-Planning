from __future__ import annotations

from fastapi.testclient import TestClient

from app.main import AiServiceRuntime, app
from career_ai.report_support_service import Chunk, ReportSupportService


def sample_payload() -> dict:
    return {
        "reportId": 12,
        "userId": 1001,
        "targetJobName": "Java backend engineer",
        "capabilityProfile": {
            "id": 7,
            "overallScore": 85,
            "professionalSkills": ["Java", "Spring Boot", "PostgreSQL"],
            "aiEvaluation": "Backend foundation is solid; project outcomes need metrics.",
        },
        "careerData": {
            "targetJob": "Java backend engineer",
            "jobProfile": {"requirements": ["Spring Boot", "database tuning", "API design"]},
        },
        "resumeAnalysis": {
            "id": 44,
            "highlights": ["Spring Boot project experience"],
            "suggestions": ["Add performance metrics"],
        },
        "matchDetails": {
            "overall": 85,
            "basic_requirements": 90,
            "professional_skills": 82,
            "professional_quality": 80,
            "development_potential": 85,
        },
        "metadataFilters": {
            "userId": 1001,
            "visibility": "private",
            "documentTypes": [
                "resume_analysis",
                "career_data",
                "capability_profile",
                "match_details",
                "action_plan",
                "development_path",
            ],
        },
    }


def test_generate_support_returns_evidence_and_diagnostics():
    result = ReportSupportService().generate_support(sample_payload())

    assert result["status"] == "OK"
    assert "Java backend engineer" in result["aiSuggestions"]
    assert result["evidenceRefs"]
    assert result["evidenceRefs"][0]["id"]
    assert result["ragDiagnostics"]["retrievalMode"] == "deterministic_fallback"
    assert result["ragDiagnostics"]["embeddingMode"] == "hash_embedding_fallback"
    assert result["ragDiagnostics"]["scoreNormalization"] == "min_max"
    assert result["ragDiagnostics"]["fusion"] == "rrf"


def test_generate_support_redacts_sensitive_evidence_snippet():
    service = ReportSupportService()

    phone_ref = service._to_evidence_ref(
        Chunk("resume:1", "x" * 155 + "13812345678 Spring Boot", {"sourceType": "resume_analysis"}),
        0.9,
    )
    email_ref = service._to_evidence_ref(
        Chunk("resume:2", "x" * 150 + "student@example.com Spring Boot", {"sourceType": "resume_analysis"}),
        0.9,
    )
    id_ref = service._to_evidence_ref(
        Chunk("resume:3", "x" * 155 + "110105199003071234 Spring Boot", {"sourceType": "resume_analysis"}),
        0.9,
    )
    snippets = " ".join([phone_ref["snippet"], email_ref["snippet"], id_ref["snippet"]])

    assert "13812345678" not in snippets
    assert "student@example.com" not in snippets
    assert "110105199003071234" not in snippets
    assert "13812" not in snippets
    assert "student@ex" not in snippets
    assert "11010" not in snippets


def test_generate_support_empty_retrieval():
    payload = sample_payload()
    payload["metadataFilters"] = {"userId": 999, "documentTypes": ["resume_analysis"]}

    result = ReportSupportService().generate_support(payload)

    assert result["status"] == "EMPTY_RETRIEVAL"
    assert result["aiSuggestions"] == ""
    assert result["evidenceRefs"] == []
    assert result["ragDiagnostics"]["emptyRetrieval"] is True


def test_generate_support_requires_ids():
    try:
        ReportSupportService().generate_support({"reportId": 1})
    except ValueError as exc:
        assert "reportId and userId" in str(exc)
    else:
        raise AssertionError("expected ValueError")


def _with_client(handler_service=None) -> tuple[TestClient, object]:
    original = AiServiceRuntime.reports_service
    if handler_service is not None:
        AiServiceRuntime.reports_service = handler_service
    client = TestClient(app)
    return client, original


def test_http_contract_200():
    client, original = _with_client()
    try:
        response = client.post("/api/v1/reports/generate-support", json=sample_payload())
        assert response.status_code == 200
        body = response.json()
        assert body["status"] == "OK"
        assert body["evidenceRefs"]
    finally:
        AiServiceRuntime.reports_service = original


def test_http_contract_400_empty_body():
    client, original = _with_client()
    try:
        response = client.post("/api/v1/reports/generate-support", content="")
        body = response.json()
        assert response.status_code == 400
        assert body["error"] == "VALIDATION_ERROR"
    finally:
        AiServiceRuntime.reports_service = original


def test_http_contract_400_non_json():
    client, original = _with_client()
    try:
        response = client.post("/api/v1/reports/generate-support", content="{not-json")
        body = response.json()
        assert response.status_code == 400
        assert body["error"] == "INVALID_JSON"
    finally:
        AiServiceRuntime.reports_service = original


def test_http_contract_500_handler_exception():
    class BrokenService:
        def generate_support(self, payload):
            raise RuntimeError("boom")

    client, original = _with_client(BrokenService())
    try:
        response = client.post("/api/v1/reports/generate-support", json=sample_payload())
        body = response.json()
        assert response.status_code == 500
        assert body["error"] == "INTERNAL_ERROR"
    finally:
        AiServiceRuntime.reports_service = original


def test_http_contract_empty_retrieval_response():
    payload = sample_payload()
    payload["metadataFilters"] = {"userId": 999}
    client, original = _with_client()
    try:
        response = client.post("/api/v1/reports/generate-support", json=payload)
        body = response.json()
        assert response.status_code == 200
        assert body["status"] == "EMPTY_RETRIEVAL"
        assert body["evidenceRefs"] == []
    finally:
        AiServiceRuntime.reports_service = original
