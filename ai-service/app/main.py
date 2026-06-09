from __future__ import annotations

import json
import os
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from typing import Any

from career_ai.goals_advice_service import generate_goal_advice
from career_ai.report_support_service import ReportSupportService
from rag.chat_pipeline import ChatRagPipeline
from schemas.chat import ChatCompleteRequest, DailySuggestionsRequest


REPORTS_SERVICE = ReportSupportService()
CHAT_PIPELINE = ChatRagPipeline()


class AiServiceHandler(BaseHTTPRequestHandler):
    service = REPORTS_SERVICE
    reports_service = REPORTS_SERVICE
    chat_pipeline = CHAT_PIPELINE

    def do_GET(self) -> None:
        if self.path == "/health":
            self._write_json({"status": "ok"})
            return
        self._write_json({"error": "NOT_FOUND", "message": "unknown path"}, status=404)

    def do_POST(self) -> None:
        try:
            payload = self._read_json()
            if self.path == "/api/v1/reports/generate-support":
                result = type(self).service.generate_support(payload)
                self._write_json(result)
                return
            if self.path == "/internal/goals/advice":
                self._write_json(generate_goal_advice(payload))
                return
            if self.path == "/api/v1/chat/complete":
                response = type(self).chat_pipeline.complete(ChatCompleteRequest.from_dict(payload))
                self._write_json(response.to_dict())
                return
            if self.path == "/api/v1/chat/daily-suggestions":
                response = type(self).chat_pipeline.daily_suggestions(DailySuggestionsRequest.from_dict(payload))
                self._write_json(response.to_dict())
                return
            self._write_json({"error": "NOT_FOUND", "message": "unknown path"}, status=404)
        except json.JSONDecodeError:
            self._write_json({"error": "INVALID_JSON", "message": "request body must be a JSON object"}, status=400)
        except KeyError as exc:
            self._write_json({"error": "VALIDATION_ERROR", "message": f"missing required field: {exc}"}, status=400)
        except ValueError as exc:
            self._write_json({"error": "VALIDATION_ERROR", "message": str(exc)}, status=400)
        except Exception:
            self._write_json({"error": "INTERNAL_ERROR", "message": "AI/RAG request failed"}, status=500)

    def log_message(self, format: str, *args: Any) -> None:
        return

    def _read_json(self) -> dict[str, Any]:
        length = int(self.headers.get("Content-Length", "0") or "0")
        raw = self.rfile.read(length)
        if not raw:
            raise ValueError("request body must be a JSON object")
        parsed = json.loads(raw.decode("utf-8"))
        if not isinstance(parsed, dict):
            raise ValueError("request body must be a JSON object")
        return parsed

    def _write_json(self, payload: dict[str, Any], status: int = 200) -> None:
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


ReportsHandler = AiServiceHandler


def create_server(host: str | None = None, port: int | None = None) -> ThreadingHTTPServer:
    bind_host = host or os.getenv("AI_SERVICE_HOST", "127.0.0.1")
    bind_port = port or int(os.getenv("AI_SERVICE_PORT", "8090"))
    return ThreadingHTTPServer((bind_host, bind_port), AiServiceHandler)


def main() -> None:
    server = create_server()
    host, port = server.server_address
    print(f"AI/RAG service listening on http://{host}:{port}")
    server.serve_forever()


if __name__ == "__main__":
    main()
