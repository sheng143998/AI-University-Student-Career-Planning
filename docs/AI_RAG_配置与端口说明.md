# AI/RAG 配置与端口说明

本文档记录当前项目中 Java 后端与 Python AI/RAG 服务的边界、端口、环境变量和启动方式。前端不得直接调用 Python 服务，所有浏览器请求都必须先进入 Java 后端。

## 端口分工

| 服务 | 默认地址 | 入口 | 职责 |
| --- | --- | --- | --- |
| Java 后端 | `127.0.0.1:8081` | `server` Spring Boot | 鉴权、业务编排、数据库、OSS、统一响应 |
| Python 聚合 AI/RAG | `127.0.0.1:8090` | `ai-service/app/main.py` | 报告、目标、Dashboard、Roadmap、Market/JD、RAG 反馈 |
| Python Resume | `127.0.0.1:8091` | `career_ai.resume_analysis_service` | 简历分析、简历 OCR |
| Python Chat | `127.0.0.1:8092` | `ai-service/app/main.py` | 聊天回复、每日建议 |

## Python 聚合服务路由

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

## Java 配置

Java 侧只保存 Python HTTP 边界地址、超时和调试开关，不再承载新的模型调用、向量检索或 RAG 生成逻辑。

| 配置键 | 环境变量 | 默认值 | 使用方 |
| --- | --- | --- | --- |
| `fuchuang.ai.python.base-url` | `FUCHUANG_AI_PYTHON_BASE_URL` | `http://127.0.0.1:8090` | 目标、Roadmap、Dashboard、反馈 |
| `fuchuang.ai.python.reports-base-url` | `FUCHUANG_AI_PYTHON_REPORTS_BASE_URL` | 回退到 base-url | Reports |
| `fuchuang.ai.python.resume-base-url` | `FUCHUANG_AI_PYTHON_RESUME_BASE_URL` | `http://127.0.0.1:8091` | Resume |
| `fuchuang.ai.python.chat-base-url` | `FUCHUANG_AI_PYTHON_CHAT_BASE_URL` | `http://127.0.0.1:8092` | Chat |
| `fuchuang.ai.python.timeout-seconds` | `FUCHUANG_AI_PYTHON_TIMEOUT_SECONDS` | `20` | 通用兜底 |
| `fuchuang.ai.python.reports-timeout-seconds` | `FUCHUANG_AI_PYTHON_REPORTS_TIMEOUT_SECONDS` | `8` | Reports |
| `fuchuang.ai.python.resume-timeout-seconds` | `FUCHUANG_AI_PYTHON_RESUME_TIMEOUT_SECONDS` | `30` | Resume |
| `fuchuang.ai.python.resume-ocr-timeout-seconds` | `FUCHUANG_AI_PYTHON_RESUME_OCR_TIMEOUT_SECONDS` | `60` | Resume OCR |
| `fuchuang.ai.python.dashboard-timeout-seconds` | `FUCHUANG_AI_PYTHON_DASHBOARD_TIMEOUT_SECONDS` | `30` | Dashboard |
| `fuchuang.ai.python.roadmap-timeout-seconds` | `FUCHUANG_AI_PYTHON_ROADMAP_TIMEOUT_SECONDS` | `8` | Roadmap |
| `fuchuang.ai.python.market-timeout-seconds` | `FUCHUANG_AI_PYTHON_MARKET_TIMEOUT_SECONDS` | `20` | Market/JD |
| `fuchuang.ai.python.chat-timeout-seconds` | `FUCHUANG_PYTHON_AI_CHAT_TIMEOUT_SECONDS` | `60` | Chat |
| `fuchuang.ai.python.daily-suggestions-timeout-seconds` | `FUCHUANG_PYTHON_AI_DAILY_SUGGESTIONS_TIMEOUT_SECONDS` | `30` | 每日建议 |
| `fuchuang.ai.python.rag-feedback-timeout-seconds` | `FUCHUANG_PYTHON_AI_RAG_FEEDBACK_TIMEOUT_SECONDS` | `10` | RAG 反馈和偏好校验 |
| `fuchuang.ai.python.debug-chat-endpoint-enabled` | `FUCHUANG_AI_PYTHON_DEBUG_CHAT_ENDPOINT_ENABLED` | `false` | `/ai/chat` 调试入口 |

历史 `FUCHUANG_PYTHON_AI_*` 超时变量仍在部分代码中兼容读取；新增配置统一使用 `FUCHUANG_AI_PYTHON_*`。

## Python 配置

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `AI_SERVICE_HOST` | `127.0.0.1` | FastAPI 监听地址 |
| `AI_SERVICE_PORT` | `8090` | FastAPI 监听端口 |
| `AI_RAG_FEEDBACK_QUEUE_PATH` | `data/rag_feedback_queue.jsonl` | RAG 反馈队列落盘路径 |
| `FUCHUANG_RESUME_OCR_API_KEY` / `OPENAI_API_KEY` / `DASHSCOPE_API_KEY` | 无 | Resume OCR 模型密钥 |
| `FUCHUANG_RESUME_OCR_BASE_URL` / `OPENAI_BASE_URL` | `https://dashscope.aliyuncs.com/compatible-mode/v1` | 兼容 OpenAI 协议的 OCR 地址 |
| `FUCHUANG_RESUME_OCR_MODEL` | `qwen-vl-ocr-2025-11-20` | OCR 模型 |
| `FUCHUANG_RESUME_OCR_MOCK_TEXT` | 无 | 本地测试时跳过真实 OCR 调用 |

## 启动命令

Python 聚合服务：

```bash
cd ai-service
AI_SERVICE_PORT=8090 python -m uvicorn app.main:app --host 127.0.0.1 --port 8090
```

Python Resume 服务：

```bash
cd ai-service
python -m career_ai.resume_analysis_service --host 127.0.0.1 --port 8091
```

Python Chat 服务：

```bash
cd ai-service
AI_SERVICE_PORT=8092 python -m uvicorn app.main:app --host 127.0.0.1 --port 8092
```

Java 后端：

```bash
mvn -pl server -am spring-boot:run
```

## 当前实现说明

当前 Python RAG 是确定性降级实现：递归分块、摘要索引、元数据过滤、多查询扩展、BM25、哈希向量风格召回、RRF/RAG-Fusion、确定性重排和脱敏诊断已经可测试。生产级真实向量模型、pgvector 召回、Dashscope 生成、交叉编码器和离线质量评估属于后续能力，不在当前代码中声明为已完成。

## 安全边界

- Python 服务只应监听 `127.0.0.1`。
- Nginx 只代理 Java `/api/**`，不代理 Python 内部端口。
- Java 调用 Python 时不得向日志写入原始简历、原始 JD、JWT、OSS 临时 URL 或模型密钥。
- Python 诊断信息返回给 Java 前应保持脱敏。
