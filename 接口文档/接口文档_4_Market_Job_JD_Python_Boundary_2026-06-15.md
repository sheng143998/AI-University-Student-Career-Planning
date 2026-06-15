# Interface Document 4 Supplement - Market Job/JD Python Boundary

## Change Summary

- Date: 2026-06-15
- Scope: Market/JD ingestion, job classification, job indexing, job semantic search, market insight, and soft-skill generation.
- Purpose: Move Java-side AI/model/vector logic to the unified Python `ai-service` boundary. Java keeps auth, business validation, database writes, Redis cache, and existing browser-facing `Result<T>` contracts.

External browser-facing endpoints stay unchanged under Java `/api/market/**` and `/api/jobs/**`. The browser must not call Python directly.

Python aggregate service base URL: `fuchuang.ai.python.base-url`, default `http://127.0.0.1:8090`.

Timeout rule: `FUCHUANG_AI_PYTHON_MARKET_TIMEOUT_SECONDS` > `fuchuang.ai.python.market-timeout-seconds` > `fuchuang.ai.python.timeout-seconds` > `20`.

## Internal Python Endpoints

| Python endpoint | Method | Auth | Purpose |
| --- | --- | --- | --- |
| `/api/v1/market/insight` | POST | Java internal only | Generate `MarketInsightContentVO`-shaped market insight. |
| `/api/v1/market/soft-skills` | POST | Java internal only | Generate `SoftSkillItemVO[]`-shaped soft-skill evidence. |
| `/internal/market/jobs/classify` | POST | Java internal only | Classify one recruitment JD/job sample into `job` category fields. |
| `/internal/market/jobs/index` | POST | Java internal only | Generate Python-side job/JD embedding records and sanitized metadata for `job_vector_store`. |
| `/internal/market/jobs/search` | POST | Java internal only | Run Python-side hybrid retrieval over Java-provided job candidates and return ranked job ids plus diagnostics. |

## Java Failure Behavior For Python-Generated Market Content

Java must not synthesize AI market insight or soft-skill evidence when Python is unavailable.

| Capability | Python success | Python timeout / unavailable / invalid response |
| --- | --- | --- |
| Market insight | Java maps Python `/api/v1/market/insight` response to `MarketInsightContentVO` and may cache it in Redis. | Java returns an empty insight state with title `Market insight unavailable`, empty summary, and empty signal/trend/action arrays. It does not cache the unavailable state. |
| Soft skills | Java maps Python `/api/v1/market/soft-skills` response to `SoftSkillItemVO[]`. | Java returns an empty `softSkills` array and does not generate local soft-skill descriptions or evidence. |

Static Java defaults may still be used for non-AI business display fields such as salary range, required skills, certificate placeholders, benefits, career path labels, and demand counters. They must not be labeled as AI/RAG output.

## POST `/internal/market/jobs/classify`

Request:

```json
{
  "request_id": "job-classify-6001",
  "job_id": 6001,
  "job_content": "sanitized JD text assembled by Java",
  "job": {
    "jobName": "AI Application Engineer",
    "city": "Shenzhen",
    "salaryRange": "15-25K",
    "industry": "AI",
    "companySize": "100-499"
  }
}
```

Success response:

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "category_code": "AI_APP_JUNIOR",
    "category_name": "AI Application Engineer",
    "level": "JUNIOR",
    "level_name": "Junior",
    "min_salary": 15000,
    "max_salary": 25000,
    "salary_unit": "MONTH",
    "required_experience_years": 1,
    "required_skills": ["Python", "RAG"],
    "job_description": "Build AI/RAG applications",
    "confidence": 0.82
  }
}
```

Failure mapping:

| Python/HTTP scene | Java behavior |
| --- | --- |
| `400` / `422` / `code=0` | Skip the single job classification and continue the batch. |
| timeout | Skip the single job classification and continue the batch. |
| connection failure / `5xx` / invalid JSON | Skip the single job classification and continue the batch. |

## POST `/internal/market/jobs/index`

Request:

```json
{
  "request_id": "job-index-batch-1",
  "jobs": [
    {
      "job_id": 6001,
      "content": "sanitized JD text assembled by Java",
      "metadata": {
        "document_type": "jd",
        "source": "recruitment_data",
        "visibility_scope": "public"
      }
    }
  ]
}
```

Success response:

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "records": [
      {
        "id": "job-6001-7a1f...",
        "job_id": 6001,
        "embedding": "[0.01,0.02,...]",
        "metadata": {"document_type":"jd","source":"recruitment_data"},
        "content_hash": "sha256..."
      }
    ],
    "diagnostics": {
      "chunking": "recursive",
      "embedding": "python-deterministic-hash",
      "record_count": 1
    }
  }
}
```

Python must not log raw JD text. Java persists returned records to `ai_career_plan.job_vector_store` with the original content already assembled inside Java.

## POST `/internal/market/jobs/search`

Request:

```json
{
  "request_id": "job-search-uuid",
  "query_text": "Python RAG backend internship",
  "limit": 5,
  "jobs": [
    {
      "job_id": 1001,
      "job_name": "AI Application Engineer",
      "job_category_code": "AI_APP_JUNIOR",
      "job_level": "JUNIOR",
      "required_skills": ["Python", "RAG"],
      "job_description": "Build AI/RAG applications"
    }
  ],
  "filters": {
    "document_type": ["job", "jd"],
    "visibility_scope": "public"
  }
}
```

Success response:

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "job_ids": [1001],
    "scores": [{"job_id":1001,"score":0.032,"source":"rag-fusion"}],
    "retrieval": {
      "expanded_queries": ["Python RAG backend internship", "..."],
      "fusion_method": "rrf",
      "reranker": "deterministic-fallback",
      "candidate_count": 1
    }
  }
}
```

Empty retrieval returns `code=1` with an empty `job_ids` array; Java falls back to keyword search results or an empty list depending on caller context.

## Frontend Impact

No TypeScript API change. Existing frontend calls continue to use Java endpoints only.

## Verification

```powershell
$env:PYTHONPATH='ai-service'; python -B -m pytest ai-service/tests/test_market_service.py -q -p no:cacheprovider
mvn -pl server -am -Dtest=PythonMarketAiClientTest test
mvn -pl server -am -Dtest=JobVectorSearchServiceImplTest test
mvn -pl server -am -Dtest=MarketServiceImplTest test
mvn -pl server -am -DskipTests compile
```
