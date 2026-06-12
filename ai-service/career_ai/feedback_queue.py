from __future__ import annotations

import json
import os
import threading
from pathlib import Path
from typing import Any


DEFAULT_QUEUE_PATH = "data/rag_feedback_queue.jsonl"


class FeedbackEvalQueue:
    """Append-only JSONL queue for offline RAG quality evaluation."""

    def __init__(self, path: str | os.PathLike[str] | None = None) -> None:
        self._path = Path(path or os.getenv("AI_RAG_FEEDBACK_QUEUE_PATH", DEFAULT_QUEUE_PATH))
        self._lock = threading.Lock()
        self._request_ids: set[str] | None = None

    @property
    def path(self) -> Path:
        return self._path

    def enqueue(self, event: dict[str, Any]) -> bool:
        request_id = str(event.get("request_id") or "").strip()
        if not request_id:
            raise ValueError("event.request_id is required")
        with self._lock:
            seen = self._load_request_ids()
            if request_id in seen:
                return False
            self._path.parent.mkdir(parents=True, exist_ok=True)
            with self._path.open("a", encoding="utf-8") as fh:
                fh.write(json.dumps(event, ensure_ascii=False) + "\n")
            seen.add(request_id)
            return True

    def _load_request_ids(self) -> set[str]:
        if self._request_ids is None:
            request_ids: set[str] = set()
            if self._path.exists():
                with self._path.open("r", encoding="utf-8") as fh:
                    for line in fh:
                        stripped = line.strip()
                        if not stripped:
                            continue
                        try:
                            parsed = json.loads(stripped)
                        except json.JSONDecodeError:
                            continue
                        if isinstance(parsed, dict) and parsed.get("request_id"):
                            request_ids.add(str(parsed["request_id"]))
            self._request_ids = request_ids
        return self._request_ids
