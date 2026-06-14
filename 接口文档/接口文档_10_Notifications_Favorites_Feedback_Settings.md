# 职引AI - Notifications / Favorites / Feedback / Settings 模块接口文档

## 变更记录

| 日期 | 变更内容 | 影响范围 |
|------|----------|----------|
| 2026-06-08 | 补充 AI/RAG 反馈闭环、AI 推荐偏好设置、`AI_ADVICE` 通知元数据和 Python 服务错误映射 | `/api/feedback/ai-rag`、`/api/settings/ai-rag`、`/api/notifications` |
| 2026-06-08 | 细化 Java 到 Python 内部端点字段归一、超时、重试、幂等和诊断返回；补充前端类型化 API client 影响 | `/api/feedback/ai-rag`、`/api/settings/ai-rag`、`/internal/rag/*` |

## 模块概述
系统通知、收藏、用户反馈和设置相关接口。

本模块普通通知、收藏、反馈和设置仍由 Java Spring Boot 负责。AI/RAG 相关扩展只处理以下场景：

- `AI_ADVICE` 类型通知的来源追踪和反馈入口。
- 用户对 AI 职业建议、岗位匹配、RAG 聊天回答、报告、路线图结果的质量反馈。
- 用户对 AI 推荐个性化范围的偏好设置。
- Java 将 AI/RAG 质量反馈转发给 Python 服务，用于离线评估、检索参数调优和排序质量分析。

不在本模块新增普通通知、收藏、反馈、设置业务能力；如需持久化新增字段，应在单独数据库变更中处理。

### AI/RAG Python 服务边界

| 调用方向 | Java 服务职责 | Python 服务职责 | 说明 |
|----------|---------------|----------------|------|
| 前端提交 AI/RAG 反馈 | 鉴权、校验目标资源属于当前用户、生成 `request_id`、脱敏后调用 Python | 接收反馈事件，归因到 retrieval trace / evidence refs，写入评估队列或日志 | 前端不得直连 Python 服务 |
| 前端读取/更新 AI 偏好 | 鉴权、读取/保存用户 AI 偏好、调用 Python 校验偏好是否可用于检索过滤 | 校验过滤条件、返回可用的 metadata filter 建议 | 不影响普通 `/api/settings` |
| Python 生成 AI 通知 | 返回通知候选和来源元数据 | 基于简历/JD/岗位匹配生成建议 | Java 负责最终通知落库和推送 |

---

## 接口列表

| 接口名 | 方法 | 路径 | 说明 |
| :--- | :--- | :--- | :--- |
| 获取通知列表 | GET | `/api/notifications` | 系统消息 |
| 标记通知已读 | PATCH | `/api/notifications/{id}/read` | 单条 |
| 全部标记已读 | PATCH | `/api/notifications/read-all` | 全部 |
| 收藏岗位 | POST | `/api/favorites/jobs` | 收藏 |
| 取消收藏 | DELETE | `/api/favorites/jobs/{job_id}` | 取消 |
| 收藏列表 | GET | `/api/favorites/jobs` | 列表 |
| 提交反馈 | POST | `/api/feedback` | BUG/建议 |
| 提交 AI/RAG 结果反馈 | POST | `/api/feedback/ai-rag` | AI 答案/推荐/检索结果质量反馈 |
| 获取设置 | GET | `/api/settings` | 主题/通知/语言等 |
| 更新设置 | PUT | `/api/settings` | 保存 |
| 获取 AI/RAG 设置 | GET | `/api/settings/ai-rag` | AI 推荐偏好与检索个性化设置 |
| 更新 AI/RAG 设置 | PUT | `/api/settings/ai-rag` | 保存 AI 推荐偏好 |

---

## 详细接口定义

### 获取通知列表
- **请求方法**: `GET`
- **请求路径**: `/api/notifications`
- **鉴权**: 需要
- **Query 参数**:
  - `only_unread` (boolean, 可选)
  - `cursor` (string, 可选)
  - `limit` (number, 可选, 默认 20)
- **响应示例**:
```json
{
  "code": 200,
  "data": {
    "items": [
      { "id": "notif_001", "type": "AI_ADVICE", "title": "AI 导师新建议", "content": "建议关注工程化...", "is_read": false, "created_at": "2026-03-26T09:00:00+08:00" }
    ],
    "next_cursor": null
  }
}
```

#### AI_ADVICE 通知元数据约束

当 `type = "AI_ADVICE"` 时，通知 item 应补充 `metadata` 字段，便于前端跳转和用户反馈闭环。

```json
{
  "id": "notif_ai_001",
  "type": "AI_ADVICE",
  "title": "AI 导师新建议",
  "content": "建议关注工程化项目证据。",
  "is_read": false,
  "created_at": "2026-06-08T09:00:00+08:00",
  "metadata": {
    "source_type": "JOB_MATCH",
    "source_id": "job_30001",
    "retrieval_trace_id": "trace_20260608_10001_001",
    "evidence_ref_ids": ["job_30001:chunk:4", "resume_20001:chunk:12"],
    "feedback_url": "/api/feedback/ai-rag",
    "generated_by": "python-rag-service"
  }
}
```

约束：

- 不返回完整简历正文、完整 JD 正文或 prompt。
- `evidence_ref_ids` 只能是证据引用 ID，用于后续详情页或反馈接口追踪。
- `retrieval_trace_id` 必须由 Python RAG 服务生成，并可用于定位 Multi-Query、BM25、embedding、RAG-Fusion 和 reranker 的检索链路。
- Java 只负责通知落库和权限过滤，不在 Java 中重新实现 RAG 生成逻辑。

---

### 标记单条通知已读
- **请求方法**: `PATCH`
- **请求路径**: `/api/notifications/{id}/read`
- **鉴权**: 需要
- **响应示例**:
```json
{ "code": 200, "data": { "updated": true } }
```

---

### 全部标记已读
- **请求方法**: `PATCH`
- **请求路径**: `/api/notifications/read-all`
- **鉴权**: 需要
- **响应示例**:
```json
{ "code": 200, "data": { "updated": true } }
```

---

### 收藏岗位
- **请求方法**: `POST`
- **请求路径**: `/api/favorites/jobs`
- **鉴权**: 需要
- **请求体**:
```json
{ "job_id": "job_001" }
```
- **响应示例**:
```json
{ "code": 200, "data": { "favorited": true } }
```

---

### 取消收藏岗位
- **请求方法**: `DELETE`
- **请求路径**: `/api/favorites/jobs/{job_id}`
- **鉴权**: 需要
- **响应示例**:
```json
{ "code": 200, "data": { "deleted": true } }
```

---

### 收藏列表
- **请求方法**: `GET`
- **请求路径**: `/api/favorites/jobs`
- **鉴权**: 需要
- **响应示例**:
```json
{ "code": 200, "data": { "items": [{ "job_id": "job_001", "title": "高级前端工程师", "company": "ZZ公司" }] } }
```

---

### 提交用户反馈
- **请求方法**: `POST`
- **请求路径**: `/api/feedback`
- **鉴权**: 需要
- **请求体**:
```json
{
  "type": "BUG",
  "content": "在移动端查看岗位图谱时，连线显示不全。",
  "contact": "alex@example.com"
}
```
- **响应示例**:
```json
{ "code": 200, "data": { "ticket_id": "fb_001" } }
```

---

### 提交 AI/RAG 结果反馈
- **请求方法**: `POST`
- **请求路径**: `/api/feedback/ai-rag`
- **鉴权**: 需要
- **接口说明**: 提交用户对 AI/RAG 输出质量的反馈。适用于聊天回答、简历分析、岗位匹配、市场洞察、职业报告、路线图、目标建议和 `AI_ADVICE` 通知。

#### 请求体
```json
{
  "target_type": "CHAT_MESSAGE",
  "target_id": "1001",
  "rating": 1,
  "reason_tags": ["HELPFUL", "EVIDENCE_RELEVANT"],
  "comment": "回答引用的岗位要求比较准确，但希望补充学习路径。",
  "retrieval_trace_id": "trace_20260608_10001_001",
  "evidence_ref_ids": ["job_30001:chunk:4", "resume_20001:chunk:12"],
  "page": "chat",
  "user_action": "thumb_up"
}
```

#### 字段说明

| 参数名 | 类型 | 是否必须 | 备注 |
| :--- | :--- | :--- | :--- |
| target_type | string | 必须 | `CHAT_MESSAGE` / `RESUME_ANALYSIS` / `JOB_MATCH` / `MARKET_INSIGHT` / `REPORT` / `ROADMAP` / `GOAL_ADVICE` / `NOTIFICATION_AI_ADVICE` |
| target_id | string | 必须 | 被反馈的业务对象 ID；`CHAT_MESSAGE` 必须使用数字消息 ID 字符串，例如 `"1001"` |
| rating | number | 必须 | `1` 正反馈，`0` 中性，`-1` 负反馈 |
| reason_tags | string[] | 非必须 | 反馈标签，如 `HELPFUL`、`NOT_RELEVANT`、`EVIDENCE_MISSING`、`OUTDATED`、`TOO_GENERIC` |
| comment | string | 非必须 | 用户补充说明，建议限制 500 字以内 |
| retrieval_trace_id | string | 非必须 | Python RAG 服务返回的检索链路 ID |
| evidence_ref_ids | string[] | 非必须 | 用户反馈涉及的证据引用 ID |
| page | string | 非必须 | 触发反馈的前端页面 |
| user_action | string | 非必须 | `thumb_up` / `thumb_down` / `dismiss` / `save` / `click_evidence` |

#### 成功响应
```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "feedback_id": "rag_fb_10001_20260608_001",
    "accepted": true,
    "used_for": ["retrieval_eval", "reranker_eval", "answer_relevancy_eval"]
  }
}
```

#### Java 到 Python 调用说明

Java 校验登录态和资源归属后，调用 Python 服务：

`POST /internal/rag/feedback`

调用约束：
- 超时：默认 10 秒，可通过 `fuchuang.ai.python.rag-feedback-timeout-seconds` 或 `FUCHUANG_PYTHON_AI_RAG_FEEDBACK_TIMEOUT_SECONDS` 调整。
- 重试：Java 不自动重试，避免重复写入反馈事件；Python 应按 `request_id` 做幂等去重或稳定反馈 ID。
- 数据脱敏：Java 只传用户 ID、目标 ID、标签、trace ID、evidence ref ID，不传完整简历、完整 JD 或 prompt。
- 目标归属：Java 必须先校验 `target_type + target_id` 属于当前用户；无法确认归属时返回无权反馈。
- 错误映射：Python 4xx 映射为 `AI 反馈参数错误`，Python 5xx/网络异常映射为 `AI 反馈服务暂不可用`，超时映射为 `AI 反馈服务超时，请稍后重试`。

请求体：
```json
{
  "request_id": "rag-feedback-10001-20260608-001",
  "user_id": 10001,
  "target": {
    "type": "CHAT_MESSAGE",
    "id": "1001",
    "page": "chat"
  },
  "feedback": {
    "rating": 1,
    "reason_tags": ["HELPFUL", "EVIDENCE_RELEVANT"],
    "comment": "回答引用的岗位要求比较准确，但希望补充学习路径。",
    "user_action": "thumb_up"
  },
  "retrieval": {
    "trace_id": "trace_20260608_10001_001",
    "evidence_ref_ids": ["job_30001:chunk:4", "resume_20001:chunk:12"]
  }
}
```

Python 成功响应：
```json
{
  "code": 1,
  "msg": "success",
    "data": {
    "feedback_id": "rag_fb_10001_20260608_001",
    "accepted": true,
    "used_for": ["retrieval_eval", "reranker_eval", "answer_relevancy_eval"],
    "quality_dimensions": {
      "context_precision": "positive",
      "context_recall": "unknown",
      "faithfulness": "positive",
      "answer_relevancy": "positive"
    },
    "diagnostics": {
      "request_id": "rag-feedback-10001-20260608-001",
      "target_type": "CHAT_MESSAGE",
      "has_retrieval_trace": true,
      "evidence_ref_count": 2,
      "sanitized": true
    }
  }
}
```

#### 错误响应

| HTTP 状态码 | 场景 | JSON 响应样例 |
| :--- | :--- | :--- |
| `200` | 参数校验失败 | `{"code":0,"msg":"AI 反馈参数错误"}` |
| `200` | 目标资源不存在或不属于当前用户 | `{"code":0,"msg":"无权反馈该 AI 结果"}` |
| `200` | Python RAG 服务超时 | `{"code":0,"msg":"AI 反馈服务超时，请稍后重试"}` |
| `200` | Python RAG 服务不可用 | `{"code":0,"msg":"AI 反馈服务暂不可用"}` |
| `401` | 未登录或 token 失效 | `{"code":0,"msg":"未登录"}` |

#### 质量反馈用途

- 正负反馈用于离线评估 BM25、embedding、Multi-Query、RAG-Fusion 和 reranker 的排序质量。
- `EVIDENCE_MISSING`、`NOT_RELEVANT` 等标签用于定位 context recall / context precision 问题。
- `TOO_GENERIC`、`OUTDATED` 等标签用于定位 answer relevancy 和知识更新时间问题。
- 反馈日志不得保存完整简历文本、完整 JD 文本或模型 prompt。

---

### 获取设置
- **请求方法**: `GET`
- **请求路径**: `/api/settings`
- **鉴权**: 需要
- **响应示例**:
```json
{ "code": 200, "data": { "theme": "system", "language": "zh-CN", "notify_email": true } }
```

---

### 更新设置
- **请求方法**: `PUT`
- **请求路径**: `/api/settings`
- **鉴权**: 需要
- **请求体**:
```json
{ "theme": "dark", "language": "zh-CN", "notify_email": true }
```
- **响应示例**:
```json
{ "code": 200, "data": { "updated": true } }
```

---

### 获取 AI/RAG 设置
- **请求方法**: `GET`
- **请求路径**: `/api/settings/ai-rag`
- **鉴权**: 需要
- **接口说明**: 获取当前用户的 AI 推荐偏好和 RAG 个性化过滤设置。该接口不替代普通 `/api/settings`。

#### 响应示例
```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "enable_ai_advice_notifications": true,
    "enable_rag_personalization": true,
    "preferred_city": "深圳",
    "preferred_industries": ["人工智能", "软件开发"],
    "preferred_job_levels": ["JUNIOR", "MID"],
    "career_direction": "技术路线",
    "result_language": "zh-CN",
    "feedback_usage_scope": "local_eval_only"
  }
}
```

### 更新 AI/RAG 设置
- **请求方法**: `PUT`
- **请求路径**: `/api/settings/ai-rag`
- **鉴权**: 需要
- **接口说明**: 保存当前用户的 AI 推荐偏好。Java 后端保存用户设置，并在后续调用 Python RAG 服务时转为 metadata filter 或 query context。

当前最小实现说明：
- Java 对外保持 `Result<T>`；`GET /api/settings/ai-rag` 返回当前用户 AI/RAG 偏好。
- `PUT /api/settings/ai-rag` 保存前先调用 Python `/internal/rag/preferences/validate`，以 Python 返回的 `metadata_filters` 作为 `effective_filters`。
- 在未引入独立设置表前，Java 可使用进程内存保存偏好；后续需要长期保存时再通过独立数据库变更处理，不混入普通 `/api/settings`。

#### 请求体
```json
{
  "enable_ai_advice_notifications": true,
  "enable_rag_personalization": true,
  "preferred_city": "深圳",
  "preferred_industries": ["人工智能", "软件开发"],
  "preferred_job_levels": ["JUNIOR", "MID"],
  "career_direction": "技术路线",
  "result_language": "zh-CN",
  "feedback_usage_scope": "local_eval_only"
}
```

#### 成功响应
```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "updated": true,
    "effective_filters": {
      "city": "深圳",
      "industries": ["人工智能", "软件开发"],
      "job_levels": ["JUNIOR", "MID"],
      "visibility_scope": "user_or_public"
    }
  }
}
```

#### Java 到 Python 调用说明

当需要校验 AI/RAG 偏好是否能转为可用检索过滤条件时，Java 调用：

`POST /internal/rag/preferences/validate`

调用约束：
- 超时：默认 10 秒，与 AI/RAG 反馈共用 `rag-feedback-timeout-seconds`。
- 重试：Java 不自动重试；失败时不保存新偏好。
- 字段归一：Python 负责过滤空字符串、限制列表长度、剔除不支持的岗位级别，并返回标准 `metadata_filters`。
- 错误映射：Python 4xx 映射为 `AI 设置参数错误`，Python 5xx/网络异常映射为 `AI 设置校验服务暂不可用`，超时映射为 `AI 设置校验服务超时，请稍后重试`。

请求体：
```json
{
  "request_id": "rag-preference-10001-20260608-001",
  "user_id": 10001,
  "preferences": {
    "preferred_city": "深圳",
    "preferred_industries": ["人工智能", "软件开发"],
    "preferred_job_levels": ["JUNIOR", "MID"],
    "career_direction": "技术路线",
    "result_language": "zh-CN"
  }
}
```

Python 响应：
```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "valid": true,
    "metadata_filters": {
      "user_id": 10001,
      "city": "深圳",
      "industry": ["人工智能", "软件开发"],
      "job_level": ["JUNIOR", "MID"],
      "language": "zh-CN",
      "visibility_scope": "user_or_public"
    },
    "diagnostics": {
      "request_id": "rag-preference-10001-20260608-001",
      "filter_strategy": "metadata_filter_before_hybrid_retrieval",
      "retrieval_mode": "multi_query+bm25+embedding+rag_fusion",
      "feedback_usage_scope": "local_eval_only"
    }
  }
}
```

#### 错误响应

| HTTP 状态码 | 场景 | JSON 响应样例 |
| :--- | :--- | :--- |
| `200` | 参数校验失败 | `{"code":0,"msg":"AI 设置参数错误"}` |
| `200` | Python RAG 服务超时 | `{"code":0,"msg":"AI 设置校验服务超时，请稍后重试"}` |
| `200` | Python RAG 服务不可用 | `{"code":0,"msg":"AI 设置校验服务暂不可用"}` |
| `401` | 未登录或 token 失效 | `{"code":0,"msg":"未登录"}` |

---

## AI/RAG 契约风险与验收点

- 普通 `/api/feedback` 只处理 BUG/建议；AI/RAG 质量反馈必须走 `/api/feedback/ai-rag`，避免把检索评估数据混入普通工单。
- 普通 `/api/settings` 只处理主题、通知、语言等通用设置；AI 个性化设置必须走 `/api/settings/ai-rag`。
- Python RAG 服务只接收脱敏后的用户 ID、反馈标签、证据引用 ID 和 trace ID，不接收完整私密简历正文。
- 后续实现时必须为 `target_type + target_id` 做用户归属校验，防止跨用户反馈或推断他人 AI 结果。
- 前端影响：新增类型化客户端 `website/src/api/aiRagFeedback.ts`，仅封装 `/api/feedback/ai-rag`、`GET /api/settings/ai-rag`、`PUT /api/settings/ai-rag`，不要求普通页面改造。
- 验证命令建议：后端新增 Java 集成时运行 `mvn test`；Python 新增反馈处理时运行 `$env:PYTHONPATH='ai-service'; python -B -m pytest ai-service/tests/test_feedback_service.py -q -p no:cacheprovider`；前端新增反馈入口时运行 `cd website && npm run build`。
