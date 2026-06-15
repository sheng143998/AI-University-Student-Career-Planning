# Interface Document 5 Supplement - Resume OCR Python Boundary

## Change Summary

- Date: 2026-06-15
- Scope: PDF resume OCR fallback.
- Purpose: Move OCR model invocation from Java to Python. Java keeps PDF rendering, upload, parsing state, database writes, and existing browser-facing resume APIs.

Browser-facing resume endpoints stay unchanged. The browser must not call Python directly.

Python Resume service base URL: `fuchuang.ai.python.resume-base-url`, falling back to `FUCHUANG_AI_PYTHON_RESUME_BASE_URL`, `FUCHUANG_AI_PYTHON_BASE_URL`, and then `http://127.0.0.1:8091`.

Timeout rule: `FUCHUANG_AI_PYTHON_RESUME_OCR_TIMEOUT_SECONDS` > `fuchuang.ai.python.resume-ocr-timeout-seconds` > `fuchuang.ai.python.resume-timeout-seconds` > `fuchuang.ai.python.timeout-seconds` > `60`.

## Internal Python Endpoint

| Python endpoint | Method | Auth | Purpose |
| --- | --- | --- | --- |
| `/internal/resume/ocr` | POST | Java internal only | OCR one rendered PDF page image and return plain text. |

## POST `/internal/resume/ocr`

Request:

```json
{
  "request_id": "resume-ocr-page-1-uuid",
  "page_number": 1,
  "model": "qwen-vl-ocr-2025-11-20",
  "instruction": "Extract all readable resume text...",
  "image_data_url": "data:image/png;base64,..."
}
```

Success response:

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "text": "Name: ...\nSkills: ...",
    "model": "qwen-vl-ocr-2025-11-20",
    "mocked": false
  }
}
```

Failure mapping:

| Python/HTTP scene | Java behavior |
| --- | --- |
| `400` / `422` / `code=0` | Mark OCR fallback as failed and keep existing resume upload error flow. |
| timeout | Mark OCR fallback as failed and keep existing resume upload error flow. |
| connection failure / `5xx` / invalid JSON | Mark OCR fallback as failed and keep existing resume upload error flow. |

## Python Runtime Configuration

Python OCR model calls use:

- `FUCHUANG_RESUME_OCR_API_KEY` > `OPENAI_API_KEY` > `DASHSCOPE_API_KEY`
- `FUCHUANG_RESUME_OCR_BASE_URL` > `OPENAI_BASE_URL` > `https://dashscope.aliyuncs.com/compatible-mode/v1`
- `FUCHUANG_RESUME_OCR_MODEL` > request `model` > `qwen-vl-ocr-2025-11-20`
- `FUCHUANG_RESUME_OCR_MOCK_TEXT` can be set in tests to bypass network calls.

## Frontend Impact

No TypeScript API change. Existing frontend resume upload and analysis polling continue to use Java endpoints only.

## Verification

```powershell
$env:PYTHONPATH='ai-service'; python -B -m pytest ai-service/tests/test_resume_ocr_service.py -q -p no:cacheprovider
mvn -pl server -am -Dtest=PythonResumeAiClientTest test
```
