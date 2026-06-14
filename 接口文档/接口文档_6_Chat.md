# 职引AI - Chat 模块接口文档

## 变更记录

| 日期 | 变更 |
| :--- | :--- |
| 2026-06-09 | 收紧遗留 `/ai/chat` 调试入口：默认通过 `fuchuang.ai.python.debug-chat-endpoint-enabled=false` 不注册路由，禁用时表现为 404；仅显式启用后允许 `GET /ai/chat` 开发调试，且仍必须经由 `PythonChatClient` 调用 Chat 专属 Python RAG 服务。显式启用时 `HEAD`/`OPTIONS`/`POST`/`PUT`/`PATCH`/`DELETE` 均返回 405 且不得调用 Python。补测 `FUCHUANG_AI_PYTHON_DEBUG_CHAT_ENDPOINT_ENABLED=true` 的 OS-env 风格 relaxed binding 可注册调试入口。正式前端继续使用 `/api/chat/messages`，本变更不接入真实 pgvector、Dashscope embedding/LLM 或完整 runtime smoke。 |
| 2026-06-08 | 按自动化验收反馈修正文档口径：当前 `ai-service` Chat 实现为 Python RAG 边界雏形与 deterministic fallback，已覆盖递归切块、摘要索引、Multi-Query、BM25+词袋向量 fallback、RAG-Fusion 和证据返回；尚未接入真实 pgvector、Dashscope embedding/LLM 或完整简历/JD 知识库，生产完成态需后续单独验收。补充 `parsedData` 透传、幂等性/重试约束、日志脱敏和每日建议空结构降级说明。 |
| 2026-06-08 | 明确 Chat Python 服务入口归属：`ai-service` 为 Chat 专属 Python RAG 服务，默认监听 `http://127.0.0.1:8092`；`ai_service` 继续作为 8090 聚合服务，Resume-AI 继续 8091。Java Chat 新增/使用 `fuchuang.ai.python.chat-base-url`，避免复用 8090 聚合入口。 |
| 2026-06-08 | 明确 Chat AI 边界迁移到 Python RAG 服务；修正发送消息实际路径为 `/api/chat/messages`；补充 Multi-Query、BM25+Embedding/fallback、RAG-Fusion、证据引用、超时和错误映射契约；遗留 `/ai/chat` 调试入口也改为走 Python Chat 契约，不再直连 Java `ChatClient`。 |

## 模块概述

Chat 模块负责 AI 导师对话、会话管理、消息收发、每日建议、附件上传和语音入口。Java Spring Boot 继续负责鉴权、会话归属校验、消息落库、统一接口入口、日志脱敏和前端兼容；Python AI 服务负责 Chat/RAG 边界、查询扩展、递归切块、摘要索引、混合召回、RAG-Fusion 重排序、证据组织和 deterministic fallback。当前实现尚未接入真实 pgvector、Dashscope embedding/LLM 或完整简历/JD 知识库，因此只能作为 Chat Python 化边界雏形验收，不得标记为生产级完整 RAG。

### Python 服务归属与端口

| 服务目录 | 职责 | 默认地址 |
| :--- | :--- | :--- |
| `ai-service` | Chat 专属 Python RAG 边界服务，承载 `/api/v1/chat/complete` 与 `/api/v1/chat/daily-suggestions`；当前为 deterministic fallback 雏形 | `http://127.0.0.1:8092` |
| `ai_service` | 聚合/历史 Python AI 服务，承载 Market、Goals、Reports、Dashboard、Feedback、Roadmap 等非 Chat 能力 | `http://127.0.0.1:8090` |
| `ai_service.resume_ai_service` | Resume-AI 分析服务 | `http://127.0.0.1:8091` |

Java Chat 不复用 `fuchuang.ai.python.base-url` 的 8090 聚合入口，而是通过 `fuchuang.ai.python.chat-base-url` 调用 Chat 专属服务。默认值为 `http://127.0.0.1:8092`，可通过环境变量 `FUCHUANG_AI_PYTHON_CHAT_BASE_URL` 覆盖。`fuchuang.ai.python.base-url` 仍保留给 8090 聚合服务，避免影响 Market、Goals、Reports、Dashboard、Feedback 和 Roadmap。

---

## 接口列表

| 接口名 | 方法 | 路径 | 说明 |
| :--- | :--- | :--- | :--- |
| 获取会话列表 | GET | `/api/chat/conversations` | 左侧会话栏 |
| 新建会话 | POST | `/api/chat/conversations` | 新建聊天会话 |
| 删除会话 | DELETE | `/api/chat/conversations/{conversationId}` | 删除会话及消息 |
| 获取会话消息 | GET | `/api/chat/conversations/{conversationId}/messages` | 分页拉取会话消息 |
| 发送消息 | POST | `/api/chat/messages` | 保存用户消息，调用 Python RAG，流式返回 AI 回复 |
| 兼容调试聊天 | GET | `/ai/chat` | 遗留开发调试入口，默认关闭并返回 404；显式启用后调用 Python RAG 返回文本流 |
| 获取每日建议 | GET | `/api/chat/daily-suggestions` | 基于简历和用户画像生成今日建议 |
| 上传附件 | POST | `/api/chat/attachments` | 上传聊天附件，返回文件 URL |
| 语音转文字 | POST | `/api/chat/voice` | 上传音频文件，返回转写文本 |

---

## 通用约定

- **鉴权**：所有 `/api/chat/**` 接口均需要登录态 JWT。Java 通过拦截器解析用户 ID，并在调用 Python 服务时传递内部可信的 `userId`，Python 服务不直接信任前端 Token。
- **普通 JSON 响应**：使用项目统一 `Result<T>`，`code=1` 成功，`code=0` 失败。
- **发送消息响应**：`POST /api/chat/messages` 为兼容前端打字机效果，返回 `text/html;charset=utf-8` 文本流，不包裹 `Result<T>`。
- **AI 范围**：Chat 的 AI 生成、RAG 检索、证据筛选、建议问题生成、每日建议生成应由 Python 服务完成；Java 不再直接使用 Spring AI `ChatClient` 承担 Chat 模块生成逻辑。
- **兼容入口**：`/ai/chat` 是早期开发调试入口，不属于正式前端 API；默认不注册路由，禁用时表现为 404。仅当 `fuchuang.ai.python.debug-chat-endpoint-enabled=true` 或环境变量 `FUCHUANG_AI_PYTHON_DEBUG_CHAT_ENDPOINT_ENABLED=true` 显式开启时才允许 GET 调试；开启后也必须经由 `PythonChatClient` 调用 Python 服务，不能绕过 Python RAG 边界直接调用 Java `ChatClient`。

---

## 详细接口定义

### 获取会话列表

- **请求方法**：`GET`
- **请求路径**：`/api/chat/conversations`
- **鉴权**：需要
- **Query 参数**：
  - `cursor`：number，可选，上一页最后一条记录 ID
  - `limit`：number，可选，默认 20
- **成功响应**：

```json
{
  "code": 1,
  "data": [
    {
      "id": 1,
      "title": "简历优化建议",
      "lastMessageAt": "2026-06-08 09:10:00",
      "createdAt": "2026-06-08 09:00:00"
    }
  ]
}
```

---

### 新建会话

- **请求方法**：`POST`
- **请求路径**：`/api/chat/conversations`
- **鉴权**：需要
- **请求体**：

```json
{
  "title": "新对话"
}
```

- **成功响应**：

```json
{
  "code": 1,
  "data": {
    "id": 2,
    "title": "新对话",
    "lastMessageAt": null,
    "createdAt": "2026-06-08 09:12:00"
  }
}
```

---

### 删除会话

- **请求方法**：`DELETE`
- **请求路径**：`/api/chat/conversations/{conversationId}`
- **鉴权**：需要
- **Path 参数**：
  - `conversationId`：number，会话 ID
- **成功响应**：

```json
{
  "code": 1,
  "data": null
}
```

---

### 获取会话消息

- **请求方法**：`GET`
- **请求路径**：`/api/chat/conversations/{conversationId}/messages`
- **鉴权**：需要
- **Path 参数**：
  - `conversationId`：number，会话 ID
- **Query 参数**：
  - `cursor`：number，可选，上一页最后一条消息 ID
  - `limit`：number，可选，默认 20
- **成功响应**：

```json
{
  "code": 1,
  "data": [
    {
      "id": 100,
      "conversationId": 1,
      "role": "assistant",
      "content": "你好，我可以结合你的简历和目标岗位给出建议。",
      "createdAt": "2026-06-08 09:10:00"
    }
  ]
}
```

---

### 发送消息

- **请求方法**：`POST`
- **请求路径**：`/api/chat/messages`
- **鉴权**：需要
- **Content-Type**：`application/json`
- **响应 Content-Type**：`text/html;charset=utf-8`
- **请求体**：

```json
{
  "conversationId": 1,
  "content": "我想了解前端开发岗位的未来趋势，并结合我的简历给建议。",
  "resumeId": 123
}
```

| 字段 | 类型 | 必填 | 说明 |
| :--- | :--- | :--- | :--- |
| `conversationId` | number | 是 | 会话 ID，Java 校验必须属于当前用户 |
| `content` | string | 是 | 用户消息内容 |
| `resumeId` | number | 否 | 当前选中的 `resume_analysis_result.id`，用于 Python RAG 元数据过滤 |

- **成功响应示例**：

```text
前端开发正在向工程化、AI 协作和跨端体验融合发展。结合你的简历...
```

- **失败响应示例**：

```text
AI 服务暂不可用，请稍后重试。
```

#### Java 到 Python Chat 契约

Java 在保存用户消息后调用 Python AI 服务：

- **Python 端点**：`POST {pythonBaseUrl}/api/v1/chat/complete`
- **Python 基础地址**：`{pythonBaseUrl}` 使用 Chat 专属 `fuchuang.ai.python.chat-base-url`，默认 `http://127.0.0.1:8092`
- **默认超时**：60 秒，可由 `FUCHUANG_PYTHON_AI_CHAT_TIMEOUT_SECONDS` 配置
- **幂等性**：Java 会先落库用户消息再调用 Python，Python 仅生成回复且不直接写 Java 业务库。调用方、网关和前端不得对 `POST /api/chat/messages` 做自动重试；若后续需要自动重试，必须先新增请求级 idempotency key 并由 Java 在落库前去重。
- **隐私日志**：Java 日志只记录 `conversationId`、`resumeId`、内容长度、异常类型和状态码，不记录完整用户消息或完整简历正文。
- **请求体**：

```json
{
  "userId": 1,
  "conversationId": 1,
  "content": "我想了解前端开发岗位的未来趋势，并结合我的简历给建议。",
  "resumeId": 123,
  "parsedData": {
    "skills": ["Vue", "TypeScript"],
    "targetRole": "前端开发工程师"
  },
  "history": [
    { "role": "user", "content": "我想找前端开发岗位。", "createdAt": "2026-06-08 09:00:00" },
    { "role": "assistant", "content": "可以先明确目标岗位 JD。", "createdAt": "2026-06-08 09:00:03" }
  ],
  "retrievalOptions": {
    "multiQuery": true,
    "hybridSearch": true,
    "ragFusion": true,
    "metadataFilter": {
      "userId": 1,
      "documentTypes": ["resume", "job", "chat_context"],
      "resumeId": 123
    }
  }
}
```

- **Python 成功响应**：

```json
{
  "content": "前端开发正在向工程化、AI 协作和跨端体验融合发展...",
  "suggestionQuestions": [
    "如何把我的项目经历改写成前端岗位更认可的表达？",
    "我的简历和前端 JD 还有哪些技能差距？"
  ],
  "tip": "可以上传目标岗位 JD，我会按招聘要求逐条对齐你的经历。",
  "title": "前端岗位趋势建议",
  "evidence": [
    {
      "sourceType": "resume",
      "sourceId": "123",
      "chunkId": "resume-123-4",
      "summary": "用户具备 Vue、TypeScript 项目经历。",
      "score": 0.87
    }
  ],
  "diagnostics": {
    "expandedQueries": [
      "前端开发未来趋势",
      "前端岗位技能要求",
      "结合简历的前端职业建议"
    ],
    "retrieval": "bm25+embedding",
    "fusion": "rag-fusion",
    "reranker": "deterministic"
  }
}
```

#### Python RAG 处理要求

- 当前 `ai-service` 必须先使用 Java 透传的 `parsedData` 和最近对话历史构造 Chat 检索上下文，避免只检索用户问题本身。
- 数据处理使用递归切块，按章节、段落、句子和长度预算逐级切分。
- 当前边界雏形已为 chunk 建立摘要索引，并保留 `rawChunkId`；后续接入真实简历/JD 知识库时，必须将文档级、章节级摘要与原文块绑定落库。
- 检索前必须应用元数据过滤，至少包含 `userId`、`documentType`、`resumeId/jobId`、`visibilityScope`。
- 查询时使用 Multi-Query 生成多个职业意图、技能差距、简历证据和 JD 要求变体。
- 当前边界雏形使用 BM25 + 词袋余弦相似度作为 embedding-like deterministic fallback，并记录候选来源与分数；fallback tokenizer 对中文使用单字和 bigram，对英文/数字使用词项，以覆盖中文自然问题的基本召回；生产完成态必须接入 Dashscope embedding/pgvector 或等价真实向量检索。
- 重排序使用 RAG-Fusion/RRF；可接入排序模型或 cross-encoder，但必须保留确定性降级路径。
- 当前回答由 deterministic 模板基于 evidence 组织，不调用 LLM；生产完成态必须接入 Dashscope/Qwen 或等价 LLM，并补充端到端 smoke 与质量评估。
- 返回内容可包含证据引用，Java 当前只流式返回 `content`，后续若前端需要展示引用，可扩展为 SSE 或 JSON 包装流。

#### 错误映射

| Python 场景 | Java 对前端表现 | 日志要求 |
| :--- | :--- | :--- |
| 连接失败 | 返回文本 `AI 服务暂不可用，请稍后重试。` | `warn`，记录 userId、conversationId、异常类型 |
| 超时 | 返回文本 `AI 服务响应超时，请稍后重试。` | `warn`，记录超时秒数 |
| 4xx 参数错误 | 返回文本 `AI 请求参数无效，请刷新后重试。` | `warn`，记录 Python 响应摘要 |
| 5xx 下游错误 | 返回文本 `AI 服务暂不可用，请稍后重试。` | `error`，记录状态码 |
| 空回复 | 返回文本 `AI 暂时没有生成有效回复，请换个问法再试。` | `warn` |

---

### 兼容调试聊天

- **请求方法**：`GET`
- **请求路径**：`/ai/chat`
- **默认状态**：关闭。未设置 `fuchuang.ai.python.debug-chat-endpoint-enabled=true` 时，Java 不注册该路由，`GET /ai/chat` 返回 404。
- **启用配置**：仅开发兼容场景可设置 `fuchuang.ai.python.debug-chat-endpoint-enabled=true` 或环境变量 `FUCHUANG_AI_PYTHON_DEBUG_CHAT_ENDPOINT_ENABLED=true`。Java 测试已通过 `SystemEnvironmentPropertySource` 模拟 OS 环境变量名称，验证 Spring relaxed binding 能触发 `@ConditionalOnProperty` 注册该入口。
- **鉴权**：不在 `/api/**` JWT 拦截范围内，仅作为开发兼容入口使用；生产侧不应作为正式业务入口暴露，默认关闭即为安全契约，不属于异常降级。
- **Query 参数**：
  - `prompt`：string，必填，用户输入内容
  - `chatId`：string，可选，遗留会话标识，能转为数字时映射为 Python 请求中的 `conversationId`
- **响应 Content-Type**：`text/html;charset=utf-8`
- **Python 调用**：复用 `POST {pythonBaseUrl}/api/v1/chat/complete`，`userId` 使用 `0` 表示调试用户，`resumeId=null`，`history=[]`，并启用 Multi-Query、BM25+Embedding、RAG-Fusion 和元数据过滤默认参数。
- **失败响应**：与 `POST /api/chat/messages` 的 Python 错误映射保持一致。
- **方法限制**：显式启用后也只允许 GET；`HEAD`、`OPTIONS`、`POST`、`PUT`、`PATCH`、`DELETE` 等非 GET 方法不得成为可用入口，应返回 404 或 405，且不得调用 Python Chat 服务。

---

### 获取每日建议

- **请求方法**：`GET`
- **请求路径**：`/api/chat/daily-suggestions`
- **鉴权**：需要
- **Query 参数**：
  - `resumeId`：number，可选，不传则使用最新简历解析数据
- **成功响应**：

```json
{
  "code": 1,
  "data": {
    "suggestions": [
      { "title": "今日建议", "text": "用 STAR 法则重写 1 条项目经历。" }
    ],
    "quickQuestions": [
      { "title": "简历优化", "text": "如何让我的简历更贴合前端岗位？" }
    ]
  }
}
```

#### Java 到 Python 每日建议契约

- **Python 端点**：`POST {pythonBaseUrl}/api/v1/chat/daily-suggestions`
- **Python 基础地址**：`{pythonBaseUrl}` 使用 Chat 专属 `fuchuang.ai.python.chat-base-url`，默认 `http://127.0.0.1:8092`
- **默认超时**：30 秒
- **降级策略**：Python 服务不可用、超时、返回非 2xx、JSON 格式异常，或 `suggestions`/`quickQuestions` 任一为空时，Java 返回默认建议，不影响页面加载。
- **请求体**：

```json
{
  "userId": 1,
  "resumeId": 123,
  "parsedData": {
    "skills": ["Vue", "TypeScript"],
    "targetRole": "前端开发工程师"
  }
}
```

- **降级策略**：Python 服务不可用或返回格式异常时，Java 返回默认建议，不影响页面加载。

---

### 上传附件

- **请求方法**：`POST`
- **请求路径**：`/api/chat/attachments`
- **鉴权**：需要
- **Content-Type**：`multipart/form-data`
- **请求字段**：
  - `file`：文件
- **成功响应**：

```json
{
  "code": 1,
  "data": "https://example.com/chat/attachment.pdf"
}
```

---

### 语音转文字

- **请求方法**：`POST`
- **请求路径**：`/api/chat/voice`
- **鉴权**：需要
- **Content-Type**：`multipart/form-data`
- **请求字段**：
  - `file`：音频文件
- **成功响应**：

```json
{
  "code": 1,
  "data": "语音转写文本"
}
```

---

## 前端影响

- `website/src/api/chat.ts` 的发送消息路径应保持 `/api/chat/messages`。
- `sendMessage` 当前接收 `Response` 流对象，前端继续按文本流读取。
- 若后续需要展示 `evidence`、`suggestionQuestions` 或 `diagnostics`，建议新增 SSE 事件或 JSON 流协议，不要直接破坏当前文本流兼容性。

## 验证建议

- 端口一致性：`rg -n "8090|8091|8092|FUCHUANG_AI_PYTHON_CHAT_BASE_URL|AI_SERVICE_PORT|chat-base-url|chatBaseUrl" ai-service ai_service server/src/main/resources server/src/main/java 接口文档`
- 调试入口门禁：`mvn -pl server -am -Dtest=PythonChatClientTest,ChatControllerDebugEndpointDisabledTest,ChatControllerDebugEndpointEnabledTest,ChatRestControllerMessagesTest "-Dsurefire.failIfNoSpecifiedTests=false" test`，覆盖默认关闭、属性显式开启、OS-env 风格 `FUCHUANG_AI_PYTHON_DEBUG_CHAT_ENDPOINT_ENABLED=true` 开启、非 GET 405、正式 `/api/chat/messages` 文本流兼容。
- 调试入口配置反查：`rg -n "debug-chat-endpoint-enabled" server/src/main/resources` 应无结果，避免通过 `application.yml` 或 `application-dev.yml` 默认开启。
- 后端：`mvn -pl server -am -DskipTests compile`，具备 Redis、PostgreSQL/pgvector、Dashscope Key 与 Python Chat 服务时再执行 Chat runtime smoke test
- Python：`python ai-service\tests\test_chat_pipeline.py`
- 前端：本轮未改变前端 API 形态，仍建议运行 `cd website && npm run build` 验证兼容性
- Java client 窄测：`mvn -pl server -am -Dtest=PythonChatClientTest "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Smoke Test：启动 Python AI 服务和 Java 服务后，创建会话并调用 `POST /api/chat/messages`，确认用户消息与 AI 回复均落库，Python 服务异常时返回降级文本。只有 Redis、PostgreSQL/pgvector、Java 8081、Python Chat 8092、`OPENAI_API_KEY` 同时具备时才能声明端到端通过。
