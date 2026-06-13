# AI/RAG 配置与端口说明（定稿）

生成时间：2026-06-12
范围：Java backend 与 Python AI/RAG 服务之间的端口分工、配置键、环境变量与超时优先级。
对应清单：`docs/AI_RAG_剩余修改与完善清单.md` P2-12。

所有内容基于 `ai-rag-integration-20260613` 集成分支实际代码核对（`PythonAiProperties`、各 `Python*Client`、`ai-service/`）。Dashboard 已迁入聚合服务；Roadmap、Market 尚未纳入本集成分支，保留为后续迁移项。

## 1. 端口分工（定稿）

| 服务 | 端口 | 入口 | 职责 |
| --- | --- | --- | --- |
| Java backend | `8081` | `server`（Spring Boot） | 鉴权、业务、对前端唯一入口 |
| Python 聚合 AI 服务 | `8090` | `ai-service/app/main.py`（`AI_SERVICE_HOST` / `AI_SERVICE_PORT`） | Reports、Goals、Dashboard、RAG feedback、偏好校验 |
| Python Resume AI 服务 | `8091` | `ai-service/career_ai/resume_analysis_service.py` | 简历分析 |
| Python Chat AI 服务 | `8092` | `ai-service/app/main.py`（`AI_SERVICE_HOST` / `AI_SERVICE_PORT`） | Chat complete、每日建议 |

**决策**：除 Resume（8091）和 Chat（8092）独立端口外，已集成的 Reports、Goals、Dashboard、Feedback 统一聚合在 8090。Roadmap、Market 因旧 `ai_service/` 入口冲突暂缓，不在本集成分支声明已暴露。

聚合服务（8090）当前暴露的路由：

- `POST /api/v1/reports/generate-support`
- `POST /internal/goals/advice`
- `POST /internal/dashboard/target-job/match`
- `POST /internal/rag/feedback`
- `POST /internal/rag/preferences/validate`

Chat 服务（8092）当前暴露的路由：

- `GET /health`
- `POST /api/v1/chat/complete`
- `POST /api/v1/chat/daily-suggestions`

Resume 服务（8091）当前暴露的路由：

- `POST /api/v1/resume/analyze`

**安全边界**：所有 Python 端口仅监听 `127.0.0.1`，前端禁止直连，必须经 Java `8081` 转发。

## 2. Java 侧配置键与环境变量

### 2.1 `fuchuang.ai.python.*`（`PythonAiProperties`）

| 配置键 | 环境变量 | 默认值 | 使用方 |
| --- | --- | --- | --- |
| `fuchuang.ai.python.base-url` | `FUCHUANG_AI_PYTHON_BASE_URL` | `http://127.0.0.1:8090` | `PythonRagFeedbackClient`、`PythonGoalsAdviceClient` |
| `fuchuang.ai.python.chat-base-url` | `FUCHUANG_AI_PYTHON_CHAT_BASE_URL` | `http://127.0.0.1:8092` | `PythonChatClient` |
| `fuchuang.ai.python.resume-base-url` | `FUCHUANG_AI_PYTHON_RESUME_BASE_URL` | `http://127.0.0.1:8091` | `PythonResumeAiClient` |
| `fuchuang.ai.python.timeout-seconds` | `FUCHUANG_AI_PYTHON_TIMEOUT_SECONDS` | yml 默认 `20`（Goals 代码默认 `20`） | 通用兜底超时 |
| `fuchuang.ai.python.reports-base-url` | `FUCHUANG_AI_PYTHON_REPORTS_BASE_URL` | 回退到 `base-url` | `PythonReportsAiClient` |
| `fuchuang.ai.python.reports-timeout-seconds` | `FUCHUANG_AI_PYTHON_REPORTS_TIMEOUT_SECONDS` | `8` | Reports-RAG |
| `fuchuang.ai.python.resume-timeout-seconds` | `FUCHUANG_AI_PYTHON_RESUME_TIMEOUT_SECONDS` | `30` | Resume-AI |
| `fuchuang.ai.python.dashboard-timeout-seconds` | `FUCHUANG_AI_PYTHON_DASHBOARD_TIMEOUT_SECONDS` / `FUCHUANG_PYTHON_AI_DASHBOARD_TIMEOUT_SECONDS`（legacy） | `30` | Dashboard-RAG |
| `fuchuang.ai.python.chat-timeout-seconds` | `FUCHUANG_PYTHON_AI_CHAT_TIMEOUT_SECONDS`（legacy） | `60` | Chat complete |
| `fuchuang.ai.python.daily-suggestions-timeout-seconds` | `FUCHUANG_PYTHON_AI_DAILY_SUGGESTIONS_TIMEOUT_SECONDS`（legacy） | `30` | 每日建议 |
| `fuchuang.ai.python.rag-feedback-timeout-seconds` | `FUCHUANG_PYTHON_AI_RAG_FEEDBACK_TIMEOUT_SECONDS`（legacy） | `10` | RAG 反馈与偏好校验 |
| `fuchuang.ai.python.debug-chat-endpoint-enabled` | `FUCHUANG_AI_PYTHON_DEBUG_CHAT_ENDPOINT_ENABLED` | `false` | `/ai/chat` 调试入口开关 |

**超时优先级**（`PythonAiProperties` 实现）：

```
legacy 环境变量（FUCHUANG_PYTHON_AI_*） > 专用配置键 > fuchuang.ai.python.timeout-seconds > 内置默认值
```

Dashboard timeout 兼容顺序为：

```
FUCHUANG_AI_PYTHON_DASHBOARD_TIMEOUT_SECONDS > FUCHUANG_PYTHON_AI_DASHBOARD_TIMEOUT_SECONDS（legacy） > fuchuang.ai.python.dashboard-timeout-seconds > fuchuang.ai.python.timeout-seconds > 30
```

### 2.2 模块独立配置（历史命名，后续迁移项）

| 配置键 | 环境变量 | 默认值 | 使用方 |
| --- | --- | --- | --- |
| `roadmap.ai.base-url` | `ROADMAP_AI_BASE_URL` | `http://localhost:8090` | Roadmap 分支暂缓，待迁移到 `ai-service/` |
| `roadmap.ai.timeout-seconds` | `ROADMAP_AI_TIMEOUT_SECONDS` | `8` | 同上 |

## 3. Python 侧环境变量

| 环境变量 | 默认值 | 作用 |
| --- | --- | --- |
| `AI_SERVICE_HOST` | `127.0.0.1` | `app/main.py` 监听地址 |
| `AI_SERVICE_PORT` | `8090` / `8092` | 聚合服务或 Chat 服务端口 |
| `AI_RAG_FEEDBACK_QUEUE_PATH` | `data/rag_feedback_queue.jsonl` | RAG 反馈评估队列落盘路径（`career_ai/feedback_queue.py`） |

聚合服务（8090）启动：

```bash
cd ai-service
AI_SERVICE_PORT=8090 python -m app.main
```

## 4. 已知命名债务（暂不改动，列入后续收敛）

1. **环境变量前缀不统一**：超时类 legacy 变量是 `FUCHUANG_PYTHON_AI_*`，base-url 类是 `FUCHUANG_AI_PYTHON_*`，两套前缀并存。新增配置一律使用 `FUCHUANG_AI_PYTHON_*`。
2. **Roadmap/Market 尚未迁入本集成分支的 `ai-service/`**：旧分支仍依赖 `ai_service/market_ai_service.py`，直接合并会冲突，需独立迁移。
3. **通用超时默认值不一致**：yml 默认 20s，Goals 默认 20s，Reports 默认 8s；以显式配置为准。

## 5. 本地启动速查

```bash
# Java backend (8081)
cd server && mvn spring-boot:run

# Python 聚合服务 (8090)
cd ai-service && AI_SERVICE_PORT=8090 python -m app.main

# Python Resume 服务 (8091)
cd ai-service && python -m career_ai.resume_analysis_service --host 127.0.0.1 --port 8091

# Python Chat 服务 (8092)
cd ai-service && AI_SERVICE_PORT=8092 python -m app.main
```

前置依赖：PostgreSQL + pgvector（`ai_career_plan`）、Redis、`OPENAI_API_KEY`（Dashscope）、Aliyun OSS 凭证。详见根目录 `AGENTS.md` 与 `ai-service/README.md`。
