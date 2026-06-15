from __future__ import annotations

import json
import os
import urllib.error
import urllib.request
from typing import Any


DEFAULT_OCR_MODEL = "qwen-vl-ocr-2025-11-20"
DEFAULT_OCR_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1"
OCR_INSTRUCTION = (
    "Extract all readable resume text from this image. Preserve paragraphs, "
    "lists, and line breaks. Return plain text only, without explanation or markdown."
)


def extract_resume_ocr_text(payload: dict[str, Any]) -> dict[str, Any]:
    image_data_url = _text(payload.get("image_data_url") or payload.get("imageDataUrl"))
    if not image_data_url.startswith("data:image/"):
        return {"code": 0, "msg": "VALIDATION_ERROR", "data": {"error": "image_data_url is required"}}

    mock_text = os.getenv("FUCHUANG_RESUME_OCR_MOCK_TEXT")
    if mock_text is not None:
        return _success(mock_text, _model(payload), mocked=True)

    api_key = _api_key()
    if not api_key:
        return {"code": 0, "msg": "OCR_API_KEY_MISSING", "data": {"error": "OCR API key is not configured"}}

    request_body = {
        "model": _model(payload),
        "temperature": 0,
        "messages": [
            {
                "role": "user",
                "content": [
                    {"type": "text", "text": _text(payload.get("instruction")) or OCR_INSTRUCTION},
                    {"type": "image_url", "image_url": {"url": image_data_url}},
                ],
            }
        ],
    }
    endpoint = _normalize_base_url(_base_url(payload)) + "/chat/completions"
    timeout = max(1, _int(payload.get("timeout_seconds"), _env_int("FUCHUANG_RESUME_OCR_TIMEOUT_SECONDS", 60)))

    request = urllib.request.Request(
        endpoint,
        data=json.dumps(request_body, ensure_ascii=False).encode("utf-8"),
        headers={"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            response_text = response.read().decode("utf-8")
    except urllib.error.HTTPError as exc:
        return {"code": 0, "msg": "OCR_HTTP_ERROR", "data": {"status": exc.code}}
    except Exception as exc:
        return {"code": 0, "msg": "OCR_UNAVAILABLE", "data": {"error": str(exc)[:200]}}

    try:
        text = _extract_text_from_response(response_text)
    except ValueError as exc:
        return {"code": 0, "msg": "OCR_INVALID_RESPONSE", "data": {"error": str(exc)}}
    return _success(text, _model(payload), mocked=False)


def _success(text: str, model: str, mocked: bool) -> dict[str, Any]:
    return {
        "code": 1,
        "msg": "success",
        "data": {
            "text": _cleanup_ocr_text(text),
            "model": model,
            "mocked": mocked,
        },
    }


def _extract_text_from_response(response_text: str) -> str:
    root = json.loads(response_text)
    choices = root.get("choices")
    if not isinstance(choices, list) or not choices:
        raise ValueError("missing choices")
    content = choices[0].get("message", {}).get("content")
    if isinstance(content, str):
        return content
    if isinstance(content, list):
        parts = [item.get("text", "") for item in content if isinstance(item, dict) and item.get("type") == "text"]
        return "\n".join(part for part in parts if part)
    raise ValueError("missing message.content")


def _cleanup_ocr_text(text: str) -> str:
    cleaned = _text(text)
    if cleaned.startswith("```"):
        first_line_break = cleaned.find("\n")
        cleaned = cleaned[first_line_break + 1 :] if first_line_break >= 0 else cleaned
    if cleaned.endswith("```"):
        cleaned = cleaned[:-3].strip()
    return cleaned


def _model(payload: dict[str, Any]) -> str:
    return _text(payload.get("model")) or os.getenv("FUCHUANG_RESUME_OCR_MODEL") or DEFAULT_OCR_MODEL


def _base_url(payload: dict[str, Any]) -> str:
    return (
        _text(payload.get("base_url"))
        or os.getenv("FUCHUANG_RESUME_OCR_BASE_URL")
        or os.getenv("OPENAI_BASE_URL")
        or DEFAULT_OCR_BASE_URL
    )


def _api_key() -> str:
    return os.getenv("FUCHUANG_RESUME_OCR_API_KEY") or os.getenv("OPENAI_API_KEY") or os.getenv("DASHSCOPE_API_KEY") or ""


def _normalize_base_url(base_url: str) -> str:
    normalized = base_url.strip().rstrip("/")
    return normalized if normalized.endswith("/v1") else normalized + "/v1"


def _env_int(name: str, default: int) -> int:
    return _int(os.getenv(name), default)


def _int(value: Any, default: int) -> int:
    try:
        return int(value)
    except (TypeError, ValueError):
        return default


def _text(value: Any) -> str:
    return str(value).strip() if value is not None else ""
