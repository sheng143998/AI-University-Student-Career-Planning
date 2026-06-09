from __future__ import annotations

import json
import os
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from typing import Any

from career_ai.goals_advice_service import generate_goal_advice
from career_ai.report_support_service import ReportSupportService


SERVICE = ReportSupportService()


class ReportsHandler(BaseHTTPRequestHandler):
    service = SERVICE

    def do_POST(self) -> None:
        try:
            payload = self._read_json()
            if self.path == "/api/v1/reports/generate-support":
                result = type(self).service.generate_support(payload)
            elif self.path == "/internal/goals/advice":
                result = generate_goal_advice(payload)
            else:
                self._write_json(404, {"error": "NOT_FOUND", "message": "unknown path"})
                return
            self._write_json(200, result)
        except json.JSONDecodeError:
            self._write_json(400, {"error": "INVALID_JSON", "message": "request body must be a JSON object"})
        except ValueError as exc:
            self._write_json(400, {"error": "VALIDATION_ERROR", "message": str(exc)})
        except Exception:
            self._write_json(500, {"error": "INTERNAL_ERROR", "message": "reports support generation failed"})

    def _read_json(self) -> dict[str, Any]:
        length = int(self.headers.get("Content-Length", "0") or "0")
        raw = self.rfile.read(length)
        if not raw:
            raise ValueError("request body must be a JSON object")
        parsed = json.loads(raw.decode("utf-8"))
        if not isinstance(parsed, dict):
            raise ValueError("request body must be a JSON object")
        return parsed

    def _write_json(self, status: int, payload: dict[str, Any]) -> None:
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, format: str, *args: Any) -> None:
        return


def create_server(host: str | None = None, port: int | None = None) -> ThreadingHTTPServer:
    bind_host = host or os.getenv("AI_SERVICE_HOST", "127.0.0.1")
    bind_port = port or int(os.getenv("AI_SERVICE_PORT", "8090"))
    return ThreadingHTTPServer((bind_host, bind_port), ReportsHandler)


def main() -> None:
    server = create_server()
    host, port = server.server_address
    print(f"Reports AI service listening on http://{host}:{port}")
    server.serve_forever()


if __name__ == "__main__":
    main()
