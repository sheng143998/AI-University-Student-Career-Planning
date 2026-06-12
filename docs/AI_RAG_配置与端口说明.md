# AI/RAG 配置与端口说明（定稿）

生成时间：2026-06-12
范围：Java backend 与 Python AI/RAG 服务之间的端口分工、配置键、环境变量与超时优先级。
对应清单：`docs/AI_RAG_剩余修改与完善清单.md` P2-12。

所有内容基于 master 分支实际代码核对（`PythonAiProperties`、各 `Python*Client`、`application.yml`、`ai-service/`）。

## 1. 端口分工（定稿）

| 服务 | 端口 | 入口 | 职责 |
| --- | --- | --- | --- |
| Java backend | `8081` | `server`（Spring Boot） | 鉴权、业务、对前端唯一入口 |
| Python 聚合 AI 服务 | `8090` | `ai-service/career_ai/market_service.py`（`--host/--port` 参数） | Market、Goals、Reports、Dashboard、Roadmap、RAG feedback、偏好校验 |
| Python Resume AI 服务 | `8091` | `ai-service/career_ai/resume_analysis_service.py` | 简历分析 |
| Python Chat AI 服务 | `8092` | `ai-service/app/main.py`（`AI_SERVICE_HOST` / `AI_SERVICE_PORT`） | Chat complete、每日建议 |

**决策**：除 Resume（8091）和 Chat（8092）独立端口外，其余 AI/RAG 模块统一聚合在 8090 的聚合服务，不再为每个模块分配独立端口。

聚合服务（8090）当前暴露的路由：

- `POST /api/v1/market/insight`
- `POST /api/v1/market/soft-skills`
- `POST /api/roadmap/recommendations/personalized`
- `POST /api/v1/goals/advice`
- `POST /api/v1/reports/generate-support`
- `POST /internal/dashboard/target-job/match`
- `POST /internal/rag/feedback`
- `POST /internal/rag/preferences/validate`

**安全边界**：所有 Python 端口仅监听 `127.0.0.1`，前端禁止直连，必须经 Java `8081` 转发。

## 2. Java 侧配置键与环境变量

### 2.1 `fuchuang.ai.python.*`（`PythonAiProperties`）

| 配置键 | 环境变量 | 默认值 | 使用方 |
| --- | --- | --- | --- |
| `fuchuang.ai.python.base-url` | `FUCHUANG_AI_PYTHON_BASE_URL` | `http://127.0.0.1:8090` | `PythonRagFeedbackClient`、`PythonDashboardAiClient`、`PythonGoalsAdviceClient` |
| `fuchuang.ai.python.chat-base-url` | `FUCHUANG_AI_PYTHON_CHAT_BASE_URL` | `http://127.0.0.1:8092` | `PythonChatClient` |
| `fuchuang.ai.python.timeout-seconds` | `FUCHUANG_AI_PYTHON_TIMEOUT_SECONDS` | yml 默认 `20`（Dashboard 代码默认 `30`，Goals 代码默认 `20`） | 通用兜底超时 |
| `fuchuang.ai.python.chat-timeout-seconds` | `FUCHUANG_PYTHON_AI_CHAT_TIMEOUT_SECONDS`（legacy） | `60` | Chat complete |
| `fuchuang.ai.python.daily-suggestions-timeout-seconds` | `FUCHUANG_PYTHON_AI_DAILY_SUGGESTIONS_TIMEOUT_SECONDS`（legacy） | `30` | 每日建议 |
| `fuchuang.ai.python.rag-feedback-timeout-seconds` | `FUCHUANG_PYTHON_AI_RAG_FEEDBACK_TIMEOUT_SECONDS`（legacy） | `10` | RAG 反馈与偏好校验 |

**超时优先级**（`PythonAiProperties` 实现）：

```
legacy 环境变量（FUCHUANG_PYTHON_AI_*） > 专用配置键 > fuchuang.ai.python.timeout-seconds > 内置默认值
```

### 2.2 模块独立配置（历史命名，暂保留）

| 配置键 | 环境变量 | 默认值 | 使用方 |
| --- | --- | --- | --- |
| `roadmap.ai.base-url` | `ROADMAP_AI_BASE_URL` | `http://localhost:8090` | Roadmap 调用聚合服务 |
| `roadmap.ai.timeout-seconds` | `ROADMAP_AI_TIMEOUT_SECONDS` | `8` | 同上 |
| `ai.resume-service.base-url` | `RESUME_AI_BASE_URL` | `http://localhost:8091` | `ResumeServiceImpl` |
| `ai.resume-service.timeout-seconds` | `RESUME_AI_TIMEOUT_SECONDS` | `30` | 同上 |

## 3. Python 侧环境变量

| 环境变量 | 默认值 | 作用 |
| --- | --- | --- |
| `AI_SERVICE_HOST` | `127.0.0.1` | Chat 服务监听地址（`app/main.py`） |
| `AI_SERVICE_PORT` | `8092` | Chat 服务端口 |
| `AI_RAG_FEEDBACK_QUEUE_PATH` | `data/rag_feedback_queue.jsonl` | RAG 反馈评估队列落盘路径（`career_ai/feedback_queue.py`） |

聚合服务（8090）通过命令行参数 `--host` / `--port` 配置：

```bash
cd ai-service
python -m career_ai.market_service --host 127.0.0.1 --port 8090
```

## 4. 已知命名债务（暂不改动，列入后续收敛）

1. **环境变量前缀不统一**：超时类 legacy 变量是 `FUCHUANG_PYTHON_AI_*`，base-url 类是 `FUCHUANG_AI_PYTHON_*`，两套前缀并存。新增配置一律使用 `FUCHUANG_AI_PYTHON_*`。
2. **Resume/Roadmap 未并入 `fuchuang.ai.python.*`**：仍使用 `RESUME_AI_BASE_URL`、`ROADMAP_AI_BASE_URL` 历史命名。清单中提到的 `FUCHUANG_AI_PYTHON_RESUME_BASE_URL` 目前**不存在**，迁移需要同时改 `ResumeServiceImpl` / `application.yml`，应作为独立 MR 处理。
3. **通用超时默认值不一致**：yml 默认 20s，但 Dashboard 客户端代码默认 30s、Goals 默认 20s；以 yml 显式配置为准。

## 5. 本地启动速查

```bash
# Java backend (8081)
cd server && mvn spring-boot:run

# Python 聚合服务 (8090)
cd ai-service && python -m career_ai.market_service

# Python Chat 服务 (8092)
cd ai-service && python -m app.main
```

前置依赖：PostgreSQL + pgvector（`ai_career_plan`）、Redis、`OPENAI_API_KEY`（Dashscope）、Aliyun OSS 凭证。详见根目录 `AGENTS.md` 与 `ai-service/README.md`。
