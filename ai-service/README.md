# AI Service

Reports-RAG uses only the Python standard library in this branch.

## Run Reports Support Service

```powershell
$env:PYTHONPATH='ai-service'
python -m app.main
```

Default address: `127.0.0.1:8090`

Environment variables:

- `AI_SERVICE_HOST`: bind host, default `127.0.0.1`
- `AI_SERVICE_PORT`: bind port, default `8090`

## Test

```powershell
$env:PYTHONPATH='ai-service'
python -B -m pytest ai-service/tests/test_report_support_service.py -q -p no:cacheprovider
```

This service is intentionally separated from the legacy `ai_service/` directory.
