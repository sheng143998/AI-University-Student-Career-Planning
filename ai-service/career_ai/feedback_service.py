from __future__ import annotations

import hashlib
from datetime import datetime, timezone
from typing import Any

from career_ai.feedback_queue import FeedbackEvalQueue


_default_eval_queue: FeedbackEvalQueue | None = None


def set_default_eval_queue(queue: FeedbackEvalQueue | None) -> None:
    global _default_eval_queue
    _default_eval_queue = queue


def _eval_queue() -> FeedbackEvalQueue:
    global _default_eval_queue
    if _default_eval_queue is None:
        _default_eval_queue = FeedbackEvalQueue()
    return _default_eval_queue


ALLOWED_TARGET_TYPES = {
    "CHAT_MESSAGE",
    "RESUME_ANALYSIS",
    "JOB_MATCH",
    "MARKET_INSIGHT",
    "REPORT",
    "ROADMAP",
    "GOAL_ADVICE",
    "NOTIFICATION_AI_ADVICE",
}
ALLOWED_RATINGS = {-1, 0, 1}
SUPPORTED_LANGUAGES = {"zh-CN", "en-US"}
SUPPORTED_JOB_LEVELS = {"INTERN", "JUNIOR", "MID", "SENIOR", "LEAD"}
SUPPORTED_USAGE_SCOPES = {"local_eval_only", "personalization", "disabled"}


def accept_rag_feedback(payload: dict[str, Any], queue: FeedbackEvalQueue | None = None) -> dict[str, Any]:
    request_id = _required_text(payload, "request_id")
    user_id = _required_int(payload, "user_id")
    target = _required_object(payload, "target")
    feedback = _required_object(payload, "feedback")
    retrieval = payload.get("retrieval") if isinstance(payload.get("retrieval"), dict) else {}

    target_type = _required_text(target, "type")
    target_id = _required_text(target, "id")
    if target_type not in ALLOWED_TARGET_TYPES:
        raise ValueError("unsupported target.type")

    rating = _required_int(feedback, "rating")
    if rating not in ALLOWED_RATINGS:
        raise ValueError("feedback.rating must be -1, 0 or 1")

    reason_tags = _text_list(feedback.get("reason_tags"))
    evidence_ref_ids = _text_list(retrieval.get("evidence_ref_ids"))
    trace_id = _optional_text(retrieval.get("trace_id"))

    received_at = datetime.now(timezone.utc).isoformat()
    feedback_id = _stable_id("rag_fb", user_id, request_id, target_type, target_id)
    used_for = _used_for(reason_tags, rating)
    quality_dimensions = _quality_dimensions(reason_tags, rating, evidence_ref_ids, trace_id)
    comment = _optional_text(feedback.get("comment"))

    # Sanitized eval event: only ids, tags and a short user comment.
    # Never include raw resume text, raw JD text or model prompts.
    event = {
        "request_id": request_id,
        "received_at": received_at,
        "user_id": user_id,
        "feedback_id": feedback_id,
        "target": {"type": target_type, "id": target_id, "page": _optional_text(target.get("page"))},
        "feedback": {
            "rating": rating,
            "reason_tags": reason_tags,
            "user_action": _optional_text(feedback.get("user_action")),
            "comment": comment[:500] if comment else None,
        },
        "retrieval": {"trace_id": trace_id, "evidence_ref_ids": evidence_ref_ids},
        "quality_dimensions": quality_dimensions,
        "used_for": used_for,
    }
    queued = (queue or _eval_queue()).enqueue(event)

    return {
        "code": 1,
        "msg": "success",
        "data": {
            "feedback_id": feedback_id,
            "accepted": True,
            "used_for": used_for,
            "quality_dimensions": quality_dimensions,
            "diagnostics": {
                "request_id": request_id,
                "received_at": received_at,
                "target_type": target_type,
                "has_retrieval_trace": bool(trace_id),
                "evidence_ref_count": len(evidence_ref_ids),
                "sanitized": True,
                "queued_for_eval": True,
                "duplicate": not queued,
            },
        },
    }


def validate_rag_preferences(payload: dict[str, Any]) -> dict[str, Any]:
    request_id = _required_text(payload, "request_id")
    user_id = _required_int(payload, "user_id")
    preferences = _required_object(payload, "preferences")

    result_language = _optional_text(preferences.get("result_language")) or "zh-CN"
    if result_language not in SUPPORTED_LANGUAGES:
        raise ValueError("preferences.result_language is unsupported")

    usage_scope = _optional_text(preferences.get("feedback_usage_scope")) or "local_eval_only"
    if usage_scope not in SUPPORTED_USAGE_SCOPES:
        raise ValueError("preferences.feedback_usage_scope is unsupported")

    filters: dict[str, Any] = {
        "user_id": user_id,
        "language": result_language,
        "visibility_scope": "user_or_public",
    }
    city = _optional_text(preferences.get("preferred_city"))
    career_direction = _optional_text(preferences.get("career_direction"))
    industries = _text_list(preferences.get("preferred_industries"))[:10]
    job_levels = [level for level in _text_list(preferences.get("preferred_job_levels")) if level in SUPPORTED_JOB_LEVELS]
    if city:
        filters["city"] = city
    if career_direction:
        filters["career_direction"] = career_direction
    if industries:
        filters["industry"] = industries
    if job_levels:
        filters["job_level"] = job_levels

    return {
        "code": 1,
        "msg": "success",
        "data": {
            "valid": True,
            "metadata_filters": filters,
            "diagnostics": {
                "request_id": request_id,
                "received_at": datetime.now(timezone.utc).isoformat(),
                "filter_strategy": "metadata_filter_before_hybrid_retrieval",
                "retrieval_mode": "multi_query+bm25+embedding+rag_fusion",
                "feedback_usage_scope": usage_scope,
            },
        },
    }


def _quality_dimensions(reason_tags: list[str], rating: int, evidence_ref_ids: list[str], trace_id: str | None) -> dict[str, str]:
    dimensions = {
        "context_precision": "unknown",
        "context_recall": "unknown",
        "faithfulness": "unknown",
        "answer_relevancy": "unknown",
    }
    tags = set(reason_tags)
    if rating > 0:
        dimensions["answer_relevancy"] = "positive"
        dimensions["context_precision"] = "positive" if evidence_ref_ids else "unknown"
        dimensions["faithfulness"] = "positive" if evidence_ref_ids or trace_id else "unknown"
    if rating < 0:
        dimensions["answer_relevancy"] = "negative"
    if "NOT_RELEVANT" in tags:
        dimensions["context_precision"] = "negative"
        dimensions["answer_relevancy"] = "negative"
    if "EVIDENCE_MISSING" in tags:
        dimensions["context_recall"] = "negative"
        dimensions["faithfulness"] = "negative"
    if {"HELPFUL", "EVIDENCE_RELEVANT"} & tags:
        dimensions["context_precision"] = "positive"
        dimensions["answer_relevancy"] = "positive"
    return dimensions


def _used_for(reason_tags: list[str], rating: int) -> list[str]:
    used = ["retrieval_eval", "reranker_eval"]
    tags = set(reason_tags)
    if rating != 0 or {"HELPFUL", "NOT_RELEVANT", "TOO_GENERIC"} & tags:
        used.append("answer_relevancy_eval")
    if {"EVIDENCE_MISSING", "EVIDENCE_RELEVANT"} & tags:
        used.append("faithfulness_eval")
    return used


def _required_object(payload: dict[str, Any], field: str) -> dict[str, Any]:
    value = payload.get(field)
    if not isinstance(value, dict):
        raise ValueError(f"{field} must be an object")
    return value


def _required_text(payload: dict[str, Any], field: str) -> str:
    value = _optional_text(payload.get(field))
    if value is None:
        raise ValueError(f"{field} is required")
    return value


def _optional_text(value: Any) -> str | None:
    if value is None:
        return None
    text = str(value).strip()
    return text or None


def _required_int(payload: dict[str, Any], field: str) -> int:
    try:
        return int(payload.get(field))
    except (TypeError, ValueError) as exc:
        raise ValueError(f"{field} must be an integer") from exc


def _text_list(value: Any) -> list[str]:
    if not isinstance(value, list):
        return []
    return [str(item).strip()[:120] for item in value if str(item).strip()]


def _stable_id(prefix: str, *parts: Any) -> str:
    raw = "|".join(str(part) for part in parts)
    digest = hashlib.sha256(raw.encode("utf-8")).hexdigest()[:16]
    return f"{prefix}_{digest}"
