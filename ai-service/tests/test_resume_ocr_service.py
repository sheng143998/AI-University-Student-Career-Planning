from __future__ import annotations

import json
import os
import threading
import unittest
from http.client import HTTPConnection
from http.server import ThreadingHTTPServer

from app.main import AiServiceHandler
from career_ai.resume_ocr_service import extract_resume_ocr_text


class ResumeOcrServiceTest(unittest.TestCase):
    def tearDown(self) -> None:
        os.environ.pop("FUCHUANG_RESUME_OCR_MOCK_TEXT", None)

    def test_extract_resume_ocr_text_uses_mock_without_network(self) -> None:
        os.environ["FUCHUANG_RESUME_OCR_MOCK_TEXT"] = "Name: Jay\nSkills: Python RAG"

        result = extract_resume_ocr_text({"image_data_url": "data:image/png;base64,abc", "model": "mock-ocr"})

        self.assertEqual(1, result["code"])
        self.assertEqual("Name: Jay\nSkills: Python RAG", result["data"]["text"])
        self.assertEqual("mock-ocr", result["data"]["model"])
        self.assertTrue(result["data"]["mocked"])

    def test_extract_resume_ocr_text_validates_image_data_url(self) -> None:
        result = extract_resume_ocr_text({"image_data_url": "http://example.test/image.png"})

        self.assertEqual(0, result["code"])
        self.assertEqual("VALIDATION_ERROR", result["msg"])

    def test_http_resume_ocr_endpoint_is_mounted(self) -> None:
        os.environ["FUCHUANG_RESUME_OCR_MOCK_TEXT"] = "OCR text"
        server = ThreadingHTTPServer(("127.0.0.1", 0), AiServiceHandler)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        try:
            status, body = self.post(server.server_port, "/internal/resume/ocr", {"image_data_url": "data:image/png;base64,abc"})
            self.assertEqual(200, status)
            self.assertEqual(1, body["code"])
            self.assertEqual("OCR text", body["data"]["text"])
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
