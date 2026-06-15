# Python AI 服务说明

`ai-service` 是本项目统一的 Python AI/RAG 服务目录。Java 后端通过本机 HTTP 调用这里的服务，前端不得直连 Python 端口。

## 目录结构

| 路径 | 说明 |
| --- | --- |
| `app/` | FastAPI 聚合入口，主要文件为 `app/main.py` |
| `career_ai/` | 简历、报告、市场、目标、Dashboard、Roadmap、反馈等业务 AI 能力 |
| `rag/` | 分块、摘要索引、检索、融合重排和 Chat RAG 流程 |
| `schemas/` | Python 请求/响应模型 |
| `tests/` | Python 单元测试与 FastAPI 路由测试 |

## 端口与入口

| 服务 | 默认端口 | 启动入口 | 用途 |
| --- | --- | --- | --- |
| 聚合 AI/RAG 服务 | `8090` | `app.main:app` | 报告、目标、Dashboard、Roadmap、Market/JD、RAG 反馈 |
| Resume 服务 | `8091` | `career_ai.resume_analysis_service:app` | 简历分析和 OCR |
| Chat 服务 | `8092` | `app.main:app` | AI 聊天与每日建议 |

## 聚合服务接口

`app/main.py` 当前提供以下 FastAPI 路由：

- `GET /health`
- `POST /api/v1/reports/generate-support`
- `POST /internal/goals/advice`
- `POST /internal/dashboard/target-job/match`
- `POST /api/v1/market/insight`
- `POST /api/v1/market/soft-skills`
- `POST /internal/market/jobs/classify`
- `POST /internal/market/jobs/index`
- `POST /internal/market/jobs/search`
- `POST /internal/resume/ocr`
- `POST /api/roadmap/recommendations/personalized`
- `POST /api/v1/chat/complete`
- `POST /api/v1/chat/daily-suggestions`
- `POST /internal/rag/feedback`
- `POST /internal/rag/preferences/validate`

## Resume 服务接口

`career_ai/resume_analysis_service.py` 当前提供以下 FastAPI 路由：

- `GET /health`
- `POST /api/v1/resume/analyze`
- `POST /internal/resume/ocr`

聚合、Resume 和 Chat 的运行入口均为 FastAPI 应用，生产和本地开发推荐使用 uvicorn 启动。

## 安装

```powershell
python -m pip install -r ai-service/requirements.txt
```

## 启动

聚合服务：

```powershell
$env:PYTHONPATH='ai-service'
$env:AI_SERVICE_PORT='8090'
python -m uvicorn app.main:app --host 127.0.0.1 --port 8090
```

Chat 服务：

```powershell
$env:PYTHONPATH='ai-service'
$env:AI_SERVICE_PORT='8092'
python -m uvicorn app.main:app --host 127.0.0.1 --port 8092
```

Resume 服务：

```powershell
$env:PYTHONPATH='ai-service'
python -m uvicorn career_ai.resume_analysis_service:app --host 127.0.0.1 --port 8091
```

也可以运行 `python -m app.main` 或 `python -m career_ai.resume_analysis_service`，它们会读取对应参数后启动 uvicorn。

## 当前实现口径

当前 RAG 能力是可测试的确定性降级实现：递归分块、摘要索引、元数据过滤、多查询扩展、BM25、哈希向量风格召回、RRF/RAG-Fusion、确定性重排和脱敏诊断。该实现用于本项目当前本地闭环，不等同于已经接入真实 pgvector、Dashscope 生成模型、交叉编码器或离线质量评估。

## 测试

```powershell
$env:PYTHONPATH='ai-service'
python -B -m pytest ai-service/tests -q -p no:cacheprovider
```
