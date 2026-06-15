from __future__ import annotations

import os
import unittest

from fastapi.testclient import TestClient

from career_ai.resume_analysis_service import app as resume_app
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
        response = TestClient(resume_app).post("/internal/resume/ocr", json={"image_data_url": "data:image/png;base64,abc"})
        body = response.json()

        self.assertEqual(200, response.status_code)
        self.assertEqual(1, body["code"])
        self.assertEqual("OCR text", body["data"]["text"])


if __name__ == "__main__":
    unittest.main()
