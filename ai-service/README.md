# AI Service

The integrated AI service uses only the Python standard library for the deterministic fallback paths in this branch.

## Run AI/RAG Service

```powershell
$env:PYTHONPATH='ai-service'
python -m app.main
```

Default address: `127.0.0.1:8090`

Environment variables:

- `AI_SERVICE_HOST`: bind host, default `127.0.0.1`
- `AI_SERVICE_PORT`: bind port, default `8090`

Endpoints:

- `POST /api/v1/reports/generate-support`
- `POST /internal/goals/advice`

## Test

```powershell
$env:PYTHONPATH='ai-service'
python -B -m pytest ai-service/tests -q -p no:cacheprovider
```

This service is intentionally separated from the legacy `ai_service/` directory.
