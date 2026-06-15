# Interface Document 8 Supplement - Roadmap Python Boundary Local Fallback Removal

Date: 2026-06-15

## Change Summary

Roadmap browser-facing Java APIs stay unchanged. Java keeps authentication context, database reads, Redis cache, and VO mapping. Roadmap lateral ranking and transition recommendation generation are delegated to Python `ai-service`.

Java no longer supplements empty or single Python Roadmap-RAG recommendation results with local skill-similarity ranking. Java also no longer generates replacement transition recommendations locally when Python fails.

## Browser-Facing Java Endpoints

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| `GET` | `/api/roadmap/recommendations/personalized` | JWT | Return personalized vertical path plus Python Roadmap-RAG lateral paths. |
| `POST` | `/api/roadmap/recommend/transition/by-job` | JWT | Return transition recommendations for a center job, ranked by Python Roadmap-RAG. |

The response wrapper remains the project `Result<T>` contract.

## Java To Python Contract

| Python endpoint | Method | Java caller | Purpose |
| --- | --- | --- | --- |
| `/api/roadmap/recommendations/personalized` | `POST` | `PythonRoadmapRagClient` | Run Multi-Query, BM25 plus embedding-style retrieval, RRF, deterministic rerank, and evidence selection for Roadmap lateral paths. |

Java sends:

- `userId`
- `currentJob`
- `userSkills`
- `resumeData` whitelist only: target role, current role, experience years, skills, match score
- `jobs` candidate list with id, category code, base category code, name, level, level name, required skills, description, profile, salary range
- retrieval filters: `excludeSameCategory=true`, document types `job`, `resume_summary`, `jd_summary`

Python returns raw Roadmap contract:

```json
{
  "lateralPaths": [
    {
      "targetJobId": 2,
      "targetCategoryCode": "AI_APP",
      "targetJobName": "AI Application Engineer",
      "matchScore": 0.86,
      "transitionDifficulty": 3,
      "estimatedMonths": 15,
      "requiredSkills": ["RAG"],
      "possessedSkills": ["Python"],
      "aiRecommendationReason": "Based on RAG-Fusion evidence...",
      "evidence": []
    }
  ],
  "diagnostics": {
    "queries": ["Frontend Engineer transition learning path"],
    "filters": {"excludeSameCategory": true, "documentTypes": ["job"]},
    "fusion": "rrf",
    "reranker": "deterministic-fallback",
    "candidateCount": 3
  }
}
```

## Failure And Empty States

Java behavior for Python timeout, unavailable service, non-2xx, invalid JSON, schema mismatch, or empty retrieval:

- Return an empty Roadmap recommendation list for Python-generated lateral/transition recommendations.
- Include sanitized diagnostics with `status=PYTHON_UNAVAILABLE`, `fusion=none`, `reranker=none`, `candidateCount`, and redacted `fallbackReason`.
- Do not generate Java local skill-similarity replacement recommendations.
- Do not expose raw Python response bodies, prompts, resume text, JD text, tokens, API keys, phone numbers, or emails.

## Frontend Impact

No path or wrapper change. Existing empty-state handling should treat an empty recommendation list as "no Python recommendation is available yet" and must not call Python directly.

## Verification

```powershell
mvn -pl server -am "-Dtest=PythonRoadmapRagClientTest,RoadmapServiceImplTest,RoadmapControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
$env:PYTHONPATH='ai-service'; python -B -m pytest ai-service/tests/test_roadmap_rag_service.py -q -p no:cacheprovider
```
