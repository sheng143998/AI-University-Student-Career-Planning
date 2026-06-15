# AI Service

FastAPI AI/RAG boundary service for the career planning project.

This directory (`ai-service`) is the unified Python service layout for newly integrated AI/RAG modules. The Java backend remains the only browser-facing entrypoint and calls this service over local HTTP.

## Ports

- Aggregate AI/RAG service: `127.0.0.1:8090`
- Resume-AI service: `127.0.0.1:8091`
- Chat-AI service: `127.0.0.1:8092`

The same `app.main` entrypoint can be started on different ports. Java routes by base URL:

- `fuchuang.ai.python.base-url` / `FUCHUANG_AI_PYTHON_BASE_URL` for aggregate endpoints.
- `fuchuang.ai.python.resume-base-url` / `FUCHUANG_AI_PYTHON_RESUME_BASE_URL` for Resume-AI.
- `fuchuang.ai.python.chat-base-url` / `FUCHUANG_AI_PYTHON_CHAT_BASE_URL` for Chat-AI.

## Endpoints

- `POST /api/v1/reports/generate-support`
- `POST /api/v1/market/insight`
- `POST /api/v1/market/soft-skills`
- `POST /internal/market/jobs/classify`
- `POST /internal/market/jobs/index`
- `POST /internal/market/jobs/search`
- `POST /internal/goals/advice`
- `POST /internal/dashboard/target-job/match`
- `POST /api/v1/chat/complete`
- `POST /api/v1/chat/daily-suggestions`
- `POST /api/v1/resume/analyze` via `career_ai.resume_analysis_service` standalone handler.
- `POST /internal/resume/ocr`
- `POST /internal/rag/feedback`
- `POST /internal/rag/preferences/validate`

Current implementation status: deterministic fallback RAG with recursive chunking, summary indexing, metadata filtering, Multi-Query expansion, BM25 plus hash embedding retrieval, RRF/RAG-Fusion, deterministic reranking, and sanitized diagnostics. It does not yet claim production pgvector/Dashscope/cross-encoder quality.

## Install

```powershell
python -m pip install -r ai-service/requirements.txt
```

## Run

```powershell
$env:PYTHONPATH='ai-service'
$env:AI_SERVICE_PORT='8090'
python -m uvicorn app.main:app --host 127.0.0.1 --port 8090
```

For Chat:

```powershell
$env:PYTHONPATH='ai-service'
$env:AI_SERVICE_PORT='8092'
python -m uvicorn app.main:app --host 127.0.0.1 --port 8092
```

`python -m app.main` is also supported and starts uvicorn with `AI_SERVICE_HOST` / `AI_SERVICE_PORT`.

For Resume-AI standalone handler:

```powershell
$env:PYTHONPATH='ai-service'
python -m career_ai.resume_analysis_service --host 127.0.0.1 --port 8091
```

## Test

```powershell
$env:PYTHONPATH='ai-service'
python -B -m pytest ai-service/tests -q -p no:cacheprovider
```
