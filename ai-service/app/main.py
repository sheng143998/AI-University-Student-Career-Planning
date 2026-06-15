from __future__ import annotations

import json
import os
from json import JSONDecodeError
from typing import Any, Callable

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

from career_ai.dashboard_rag_service import match_target_job
from career_ai.feedback_service import accept_rag_feedback, validate_rag_preferences
from career_ai.goals_advice_service import generate_goal_advice
from career_ai.market_service import (
    classify_job,
    generate_market_insight,
    generate_soft_skills,
    index_jobs,
    search_jobs,
)
from career_ai.resume_ocr_service import extract_resume_ocr_text
from career_ai.roadmap_rag_service import generate_roadmap_recommendations
from career_ai.report_support_service import ReportSupportService
from rag.chat_pipeline import ChatRagPipeline
from schemas.chat import ChatCompleteRequest, DailySuggestionsRequest


REPORTS_SERVICE = ReportSupportService()
CHAT_PIPELINE = ChatRagPipeline()


class AiServiceRuntime:
    reports_service = REPORTS_SERVICE
    chat_pipeline = CHAT_PIPELINE


app = FastAPI(title="Career AI/RAG Service", version="1.0.0")


@app.exception_handler(StarletteHTTPException)
async def http_exception_handler(_request: Request, exc: StarletteHTTPException) -> JSONResponse:
    if exc.status_code == 404:
        return _json({"error": "NOT_FOUND", "message": "unknown path"}, status_code=404)
    return _json({"error": "HTTP_ERROR", "message": str(exc.detail)}, status_code=exc.status_code)


@app.get("/health")
async def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/api/v1/reports/generate-support")
async def reports_generate_support(request: Request) -> JSONResponse:
    return await _handle_json(request, _generate_report_support)


@app.post("/internal/goals/advice")
async def goals_advice(request: Request) -> JSONResponse:
    return await _handle_json(request, generate_goal_advice)


@app.post("/internal/dashboard/target-job/match")
async def dashboard_target_job_match(request: Request) -> JSONResponse:
    return await _handle_json(
        request,
        match_target_job,
        validation_payload=_dashboard_validation_payload,
    )


@app.post("/api/v1/market/insight")
async def market_insight(request: Request) -> JSONResponse:
    return await _handle_json(request, generate_market_insight)


@app.post("/api/v1/market/soft-skills")
async def market_soft_skills(request: Request) -> JSONResponse:
    return await _handle_json(request, generate_soft_skills)


@app.post("/internal/market/jobs/classify")
async def market_jobs_classify(request: Request) -> JSONResponse:
    return await _handle_json(request, classify_job)


@app.post("/internal/market/jobs/index")
async def market_jobs_index(request: Request) -> JSONResponse:
    return await _handle_json(request, index_jobs)


@app.post("/internal/market/jobs/search")
async def market_jobs_search(request: Request) -> JSONResponse:
    return await _handle_json(request, search_jobs)


@app.post("/internal/resume/ocr")
async def resume_ocr(request: Request) -> JSONResponse:
    return await _handle_json(request, extract_resume_ocr_text)


@app.post("/api/roadmap/recommendations/personalized")
async def roadmap_recommendations(request: Request) -> JSONResponse:
    return await _handle_json(request, generate_roadmap_recommendations)


@app.post("/api/v1/chat/complete")
async def chat_complete(request: Request) -> JSONResponse:
    return await _handle_json(request, _complete_chat)


@app.post("/api/v1/chat/daily-suggestions")
async def chat_daily_suggestions(request: Request) -> JSONResponse:
    return await _handle_json(request, _daily_chat_suggestions)


@app.post("/internal/rag/feedback")
async def rag_feedback(request: Request) -> JSONResponse:
    return await _handle_json(request, accept_rag_feedback)


@app.post("/internal/rag/preferences/validate")
async def rag_preferences_validate(request: Request) -> JSONResponse:
    return await _handle_json(request, validate_rag_preferences)


async def _handle_json(
    request: Request,
    handler: Callable[[dict[str, Any]], dict[str, Any] | list[Any]],
    *,
    validation_payload: Callable[[Exception], dict[str, Any]] | None = None,
) -> JSONResponse:
    try:
        payload = await _read_json_object(request)
        status, content = _execute_handler(handler, payload, validation_payload=validation_payload)
        return _json(content, status_code=status)
    except JSONDecodeError:
        return _json({"error": "INVALID_JSON", "message": "request body must be a JSON object"}, status_code=400)
    except KeyError as exc:
        return _json({"error": "VALIDATION_ERROR", "message": f"missing required field: {exc}"}, status_code=400)
    except ValueError as exc:
        content = validation_payload(exc) if validation_payload else {"error": "VALIDATION_ERROR", "message": str(exc)}
        return _json(content, status_code=400)
    except Exception:
        return _json({"error": "INTERNAL_ERROR", "message": "AI/RAG request failed"}, status_code=500)


def _execute_handler(
    handler: Callable[[dict[str, Any]], dict[str, Any] | list[Any]],
    payload: dict[str, Any],
    *,
    validation_payload: Callable[[Exception], dict[str, Any]] | None = None,
) -> tuple[int, dict[str, Any] | list[Any]]:
    try:
        return 200, handler(payload)
    except KeyError as exc:
        return 400, {"error": "VALIDATION_ERROR", "message": f"missing required field: {exc}"}
    except ValueError as exc:
        content = validation_payload(exc) if validation_payload else {"error": "VALIDATION_ERROR", "message": str(exc)}
        return 400, content
    except Exception:
        return 500, {"error": "INTERNAL_ERROR", "message": "AI/RAG request failed"}


def _generate_report_support(payload: dict[str, Any]) -> dict[str, Any]:
    return AiServiceRuntime.reports_service.generate_support(payload)


def _complete_chat(payload: dict[str, Any]) -> dict[str, Any]:
    return AiServiceRuntime.chat_pipeline.complete(ChatCompleteRequest.from_dict(payload)).to_dict()


def _daily_chat_suggestions(payload: dict[str, Any]) -> dict[str, Any]:
    return AiServiceRuntime.chat_pipeline.daily_suggestions(DailySuggestionsRequest.from_dict(payload)).to_dict()


def _dashboard_validation_payload(exc: Exception) -> dict[str, Any]:
    return {"code": 0, "msg": "VALIDATION_ERROR", "data": {"error": str(exc)}}


async def _read_json_object(request: Request) -> dict[str, Any]:
    raw = await request.body()
    if not raw:
        raise ValueError("request body must be a JSON object")
    parsed = json.loads(raw.decode("utf-8"))
    if not isinstance(parsed, dict):
        raise ValueError("request body must be a JSON object")
    return parsed


def _json(payload: dict[str, Any] | list[Any], status_code: int = 200) -> JSONResponse:
    return JSONResponse(content=payload, status_code=status_code)


def main() -> None:
    import uvicorn

    host = os.getenv("AI_SERVICE_HOST", "127.0.0.1")
    port = int(os.getenv("AI_SERVICE_PORT", "8090"))
    print(f"AI/RAG FastAPI service listening on http://{host}:{port}")
    uvicorn.run("app.main:app", host=host, port=port, log_level="info")


if __name__ == "__main__":
    main()
