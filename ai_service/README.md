# Python AI Service

This service hosts AI/RAG workflows that should no longer live inside Java.

## Runtime

Run locally:

```bash
python -m ai_service.market_ai_service --host 127.0.0.1 --port 8090
python -m ai_service.resume_ai_service --host 127.0.0.1 --port 8091
```

Endpoints:

- `POST /api/v1/market/insight`
- `POST /api/v1/market/soft-skills`
- `POST /api/v1/resume/analyze`
- `POST /api/roadmap/recommendations/personalized`
- `POST /api/v1/goals/advice`
- `POST /api/v1/reports/generate-support`
- `POST /internal/dashboard/target-job/match`
- `POST /internal/rag/feedback`
- `POST /internal/rag/preferences/validate`

The current implementation is deterministic and dependency-free so Java can call a runnable Python boundary during the migration.

## Resume-AI

`POST /api/v1/resume/analyze` handles resume analysis after Java uploads the file to OSS and extracts base text. It implements recursive chunking, summary index records, user-scoped metadata filtering, Multi-Query retrieval, BM25 plus deterministic embedding matching, RAG-Fusion with reciprocal rank fusion, deterministic reranking fallback, structured resume parsing, capability profile generation, and RAG diagnostics.

## Roadmap-RAG

`POST /api/roadmap/recommendations/personalized` handles AI career path recommendations for Roadmap. It implements recursive chunking, summary index records, metadata filtering, Multi-Query retrieval, BM25 plus lightweight embedding-style matching, RAG-Fusion with reciprocal rank fusion, and a deterministic fallback reranker.

## Goals-RAG

`POST /api/v1/goals/advice` handles AI advice generation for Goals. Java assembles the user-scoped goal context and delegates the AI/RAG reasoning boundary to Python; the response includes advice content, evidence references, metadata filters, Multi-Query diagnostics, BM25 plus embedding retrieval mode, and RAG-Fusion reranking metadata.

## Reports-RAG

`POST /api/v1/reports/generate-support` handles AI support for career reports. Java sends the report, user, capability, career, and resume context; Python builds recursive chunks, summary index records, user-scoped metadata filters, Multi-Query BM25 plus embedding retrieval, RAG-Fusion ranking, deterministic reranking metadata, AI suggestions, and evidence references.

## Dashboard-RAG

`POST /internal/dashboard/target-job/match` handles Dashboard target job matching. Java sends the latest resume analysis, a truncated resume context, and a job candidate snapshot; Python builds recursive chunks, summary index records, metadata filters, Multi-Query BM25 plus deterministic embedding retrieval, RAG-Fusion/RRF ranking, deterministic fallback reranking, diagnostics, and evidence references.

## Feedback / Preferences

`POST /internal/rag/feedback` accepts sanitized AI/RAG feedback events from Java and returns a stable `feedback_id`, acceptance flag, evaluation usage buckets, quality dimensions, and diagnostics.

`POST /internal/rag/preferences/validate` validates AI/RAG personalization preferences and returns metadata filters that Java can use for later retrieval calls. The implementation is deterministic and does not receive full resume text, JD text, or prompts.

Run tests:

```bash
python -m unittest discover -s ai_service -p "test_*.py"
```
