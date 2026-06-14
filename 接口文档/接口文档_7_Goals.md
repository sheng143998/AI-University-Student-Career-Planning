# 职引AI - Goals 模块接口文档

## 模块概述
目标管理相关接口，包括主目标、里程碑、成功准则和并行目标的管理。

---

## 变更记录

| 日期 | 变更 | 影响范围 |
| :--- | :--- | :--- |
| 2026-06-09 | 新增目标 AI 建议生成接口，统一 Java 到 Python Goals-RAG 内部路径为 `/internal/goals/advice`，响应扩展 `evidenceReferences` 与 `retrievalDiagnostics` | `/api/goals/{id}/ai-advice/generate`、`AiAdvice`、`website/src/api/goals.ts` |

---

## 接口列表

| 接口名 | 方法 | 路径 | 说明 |
| :--- | :--- | :--- | :--- |
| 获取目标总览 | GET | `/api/goals/overview` | 含主目标/里程碑/成功准则/并行目标 |
| 创建目标 | POST | `/api/goals` | 新增目标 |
| 获取目标详情 | GET | `/api/goals/{id}` | 含里程碑/成功准则/AI 建议 |
| 生成目标 AI 建议 | POST | `/api/goals/{id}/ai-advice/generate` | 通过 Python Goals-RAG 生成建议 |
| 更新目标 | PUT | `/api/goals/{id}` | 更新字段 |
| 删除目标 | DELETE | `/api/goals/{id}` | 删除 |
| 创建里程碑 | POST | `/api/goals/{id}/milestones` | 里程碑 |
| 更新里程碑 | PATCH | `/api/goals/{id}/milestones/{ms_id}` | 勾选/改名/排序 |

---

## 详细接口定义

### 获取目标总览
- **请求方法**: `GET`
- **请求路径**: `/api/goals/overview`
- **鉴权**: 需要（JWT Token）
- **响应示例**:
```json
{
  "code": 1,
  "msg": null,
  "data": {
    "primaryGoal": {
      "id": "1",
      "title": "成为高级前端工程师",
      "desc": "通过工程化与性能体系建设拿到更高级别岗位",
      "status": "IN_PROGRESS",
      "progress": 65,
      "eta": "2025年12月",
      "isPrimary": true
    },
    "milestones": [
      { "id": "1", "goalId": "1", "title": "完善项目作品集", "desc": "整理3个代表性项目", "status": "DONE", "progress": 100, "order": 1 },
      { "id": "2", "goalId": "1", "title": "系统学习性能优化", "desc": "完成性能优化课程", "status": "IN_PROGRESS", "progress": 50, "order": 2 }
    ],
    "milestonesCompleted": 1,
    "milestonesTotal": 2,
    "successCriteria": {
      "salary": "¥30k - ¥45k / 月",
      "companies": ["腾讯", "字节跳动", "阿里巴巴"],
      "cities": ["北京", "上海", "深圳"]
    },
    "longTermAspirations": [
      { "title": "技术专家", "desc": "成为某领域技术专家" },
      { "title": "团队管理", "desc": "带领10人以上团队" }
    ],
    "aiAdvice": {
      "content": "建议将目标拆成 4 周冲刺：工程化、性能、项目表达、面试题体系。"
    },
    "parallelGoals": [
      { "id": "2", "title": "补齐算法基础", "desc": "系统学习算法", "status": "IN_PROGRESS", "progress": 20, "eta": "2025年6月", "isPrimary": false }
    ]
  }
}
```

---

### 创建目标
- **请求方法**: `POST`
- **请求路径**: `/api/goals`
- **鉴权**: 需要
- **请求体**:
```json
{
  "title": "完成 React 认证",
  "desc": "通过官方认证",
  "status": "TODO",
  "progress": 0,
  "eta": "2025年8月",
  "isPrimary": false
}
```
- **响应示例**:
```json
{ "code": 1, "msg": null, "data": { "id": "3" } }
```

---

### 获取目标详情
- **请求方法**: `GET`
- **请求路径**: `/api/goals/{id}`
- **鉴权**: 需要
- **响应示例**:
```json
{
  "code": 1,
  "msg": null,
  "data": {
    "goal": {
      "id": "1",
      "title": "成为高级前端工程师",
      "desc": "通过工程化与性能体系建设拿到更高级别岗位",
      "status": "IN_PROGRESS",
      "progress": 65,
      "eta": "2025年12月",
      "isPrimary": true
    },
    "milestones": [
      { "id": "1", "goalId": "1", "title": "完善项目作品集", "desc": "整理3个代表性项目", "status": "DONE", "progress": 100, "order": 1 }
    ],
    "successCriteria": {
      "salary": "¥30k - ¥45k / 月",
      "companies": ["腾讯", "字节跳动"],
      "cities": ["北京", "上海"]
    },
    "longTermAspirations": [
      { "title": "技术专家", "desc": "成为某领域技术专家" }
    ],
    "aiAdvice": {
      "content": "建议将目标拆成 4 周冲刺..."
    }
  }
}
```

---

### 生成目标 AI 建议
- **请求方法**: `POST`
- **请求路径**: `/api/goals/{id}/ai-advice/generate`
- **鉴权**: 需要（JWT Token）
- **路径参数**:

| 参数 | 类型 | 必须 | 说明 |
| :--- | :--- | :--- | :--- |
| id | number | 是 | 目标 ID。Java 必须用 `BaseContext.userId` 校验该目标属于当前用户 |

- **请求体**: 无
- **用途**: 为当前用户指定目标生成或刷新 AI 建议。Java 负责鉴权、目标归属校验、上下文脱敏和落库；Python 负责 Goals-RAG 检索、融合排序和建议生成。
- **成功响应示例**:
```json
{
  "code": 1,
  "msg": null,
  "data": {
    "content": "当前目标「成为 AI 应用开发工程师」状态为 IN_PROGRESS，进度约45%。建议优先补齐 RAG 项目证据。",
    "evidenceReferences": [
      {
        "sourceType": "milestone",
        "sourceId": "goal_7:milestone_1:chunk:0",
        "reason": "完成 RAG 项目 实现检索、重排和评估",
        "score": 0.98
      }
    ],
    "retrievalDiagnostics": {
      "expandedQueries": [
        "成为 AI 应用开发工程师 目标拆解 下一步",
        "成为 AI 应用开发工程师 技能差距 里程碑"
      ],
      "metadataFilters": {
        "userId": "10001",
        "goalId": "7",
        "documentTypes": ["goal", "milestone", "successCriteria"],
        "visibilityScope": "USER_PRIVATE"
      },
      "retrieval": "multi_query+bm25+embedding",
      "fusion": "rag_fusion_rrf",
      "reranker": "deterministic_fallback",
      "chunking": "recursive",
      "summaryIndexCount": 4,
      "candidateCount": 4,
      "selectedEvidenceCount": 3,
      "emptyRetrievalFallback": false,
      "scoreNormalization": "minmax_0_1"
    }
  }
}
```

- **失败响应示例**:
```json
{ "code": 0, "msg": "目标不存在", "data": null }
```
```json
{ "code": 0, "msg": "目标AI建议生成失败，请检查目标上下文", "data": null }
```
```json
{ "code": 0, "msg": "目标AI建议服务超时，请稍后重试", "data": null }
```
```json
{ "code": 0, "msg": "目标AI建议服务暂不可用", "data": null }
```

#### Java 到 Python 调用说明

Java 在目标归属校验通过后调用：

`POST /internal/goals/advice`

- **Python 服务地址**: `fuchuang.ai.python.base-url`，默认 `http://127.0.0.1:8090`。
- **超时**: 请求超时默认 20 秒；连接超时 10 秒。实现使用通用 `fuchuang.ai.python.timeout-seconds`，不要求修改 `application*.yml`。
- **重试**: Java 不自动重试，避免重复覆盖 `goal.ai_advice`。
- **幂等/覆盖语义**: 重复调用会重新生成建议，并只按 `goal_id + user_id` 覆盖当前目标的 `ai_advice` 文本内容。
- **持久化边界**: `goal.ai_advice` 只保存 `content`；`evidenceReferences` 和 `retrievalDiagnostics` 只在本次生成响应返回，不改数据库表。`overview/detail` 从数据库读取时只稳定返回 `content`。Goals 普通目标更新、里程碑更新、AI tool 目标更新和里程碑完成也统一走 `id + user_id` SQL 条件，作为本接口 RAG payload 与 AI tool 用户隔离的必要 Java 安全胶水。
- **数据脱敏**: Java 不传完整简历/JD 原文；只传目标、里程碑、成功准则和长期愿景的短摘要，并过滤手机号、邮箱、token-like 字符串和嵌套对象内容。
- **Python RAG 口径**: 当前为可运行 deterministic fallback，包含递归切块、摘要索引、`userId/goalId/documentTypes/visibilityScope` 元数据过滤；其中 `documentTypes` 会真实过滤候选 summary records 与 evidence references，而不只是写入 diagnostics。检索流程包含 Multi-Query、BM25 + hash embedding 混合检索、score normalization、RAG-Fusion/RRF、确定性重排、证据引用和诊断字段。该接口不声明真实 pgvector、Dashscope LLM、cross-encoder 或离线质量评估已完成。

Java 请求示例：
```json
{
  "userId": "10001",
  "goal": {
    "id": "7",
    "title": "成为 AI 应用开发工程师",
    "desc": "补齐 Python RAG 与工程化能力",
    "status": "IN_PROGRESS",
    "progress": 45,
    "eta": "2026年9月",
    "isPrimary": true
  },
  "milestones": [
    { "id": "1", "title": "完成 RAG 项目", "desc": "实现检索、重排和评估", "status": "IN_PROGRESS", "progress": 40, "order": 1 }
  ],
  "successCriteria": {
    "salary": "15k-25k",
    "companies": ["字节跳动"],
    "cities": ["杭州"]
  },
  "longTermAspirations": [
    { "title": "AI 应用工程师", "desc": "能独立落地 RAG 系统" }
  ],
  "retrievalOptions": {
    "chunking": "recursive",
    "summaryIndex": true,
    "metadataFilters": {
      "userId": "10001",
      "goalId": "7",
      "documentTypes": ["goal", "milestone", "successCriteria"],
      "visibilityScope": "USER_PRIVATE"
    },
    "multiQuery": true,
    "hybridSearch": ["bm25", "embedding"],
    "fusion": "rag_fusion_rrf",
    "rankingModel": "deterministic_fallback"
  }
}
```

Python 响应字段必须使用 camelCase：

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| content | string | 生成的 AI 建议文本 |
| evidenceReferences | object[] | 证据引用，不包含完整原文 |
| retrievalDiagnostics | object | 检索诊断，仅包含 query/filter/count/score 等可审计元数据 |

错误映射：

| Python/调用异常 | Java 外部响应 |
| :--- | :--- |
| `400` / `422` | `{"code":0,"msg":"目标AI建议生成失败，请检查目标上下文"}` |
| `504` 或 Java 请求超时 | `{"code":0,"msg":"目标AI建议服务超时，请稍后重试"}` |
| 连接失败、`5xx`、非 JSON | `{"code":0,"msg":"目标AI建议服务暂不可用"}` |
| `content` 缺失或空字符串 | `{"code":0,"msg":"目标AI建议生成结果为空"}` |
| empty retrieval | Python 返回 `emptyRetrievalFallback=true` 的成功响应，Java 仍保存并返回 fallback 建议 |
| 鉴权失败 | 由全局 JWT 拦截器处理，不调用 Python |
| 目标不存在或跨用户目标 | `{"code":0,"msg":"目标不存在"}`，不调用 Python |
| 目标或里程碑更新跨用户 | 先按 `id + user_id` 查询；写入时使用 `updateByIdAndUserId`，SQL 层再次约束 `user_id` |

#### 前端影响

- `website/src/api/goals.ts` 中 `AiAdvice` 扩展 `evidenceReferences?` 和 `retrievalDiagnostics?`。
- 新增 `generateGoalAiAdvice(goalId)`，调用 `POST /api/goals/{id}/ai-advice/generate`。
- 当前最小闭环只增加 API client，不要求改 Goals 页面 UI。

#### 测试口径

- Python: `python -B -m pytest -q -p no:cacheprovider ai_service/test_goals_rag_service.py --tb=short` 与 `python -B -m unittest discover -s ai_service -p "test_goals_rag_service.py"`，覆盖递归切块、摘要索引、metadata filter 防伪、`documentTypes` 真实过滤候选证据、Multi-Query、BM25+embedding、RAG-Fusion/RRF、重排、HTTP handler、PII 过滤和旧 endpoint 清理。
- Java: `mvn -pl server -am -Dtest=PythonGoalsAdviceClientTest,GoalsServiceImplTest,GoalsControllerTest -Dsurefire.failIfNoSpecifiedTests=false test`，并校验 surefire 报告中三个测试类 tests > 0。
- Frontend: `cd website && npm run build`。
- Smoke: 启动 Python 8090 聚合服务后 POST `/internal/goals/advice`，确认三字段、metadata filters 和旧版 Goals advice 公网风格路径不再可用。

---

### 更新目标
- **请求方法**: `PUT`
- **请求路径**: `/api/goals/{id}`
- **鉴权**: 需要
- **请求体**:
```json
{
  "title": "成为高级前端工程师",
  "desc": "通过工程化与性能体系建设拿到更高级别岗位",
  "status": "IN_PROGRESS",
  "progress": 70,
  "eta": "2025年11月",
  "isPrimary": true,
  "successCriteria": {
    "salary": "¥35k - ¥50k / 月",
    "companies": ["腾讯", "字节跳动", "阿里巴巴", "美团"],
    "cities": ["北京", "上海", "深圳", "杭州"]
  },
  "longTermAspirations": [
    { "title": "技术专家", "desc": "成为某领域技术专家" },
    { "title": "创业", "desc": "创办自己的公司" }
  ],
  "aiAdvice": {
    "content": "更新后的AI建议..."
  }
}
```
- **响应示例**:
```json
{ "code": 1, "msg": null, "data": { "updated": true } }
```

---

### 删除目标
- **请求方法**: `DELETE`
- **请求路径**: `/api/goals/{id}`
- **鉴权**: 需要
- **响应示例**:
```json
{ "code": 1, "msg": null, "data": { "deleted": true } }
```

---

### 创建里程碑
- **请求方法**: `POST`
- **请求路径**: `/api/goals/{id}/milestones`
- **鉴权**: 需要
- **请求体**:
```json
{
  "title": "完成 1 次性能优化复盘",
  "desc": "包含数据对比和优化方案",
  "status": "TODO",
  "progress": 0,
  "order": 3
}
```
- **响应示例**:
```json
{ "code": 1, "msg": null, "data": { "id": "10" } }
```

---

### 更新里程碑
- **请求方法**: `PATCH`
- **请求路径**: `/api/goals/{id}/milestones/{ms_id}`
- **鉴权**: 需要
- **请求体（任选字段）**:
```json
{
  "title": "完成 1 次性能优化复盘（含数据对比）",
  "desc": "已完成",
  "status": "DONE",
  "progress": 100,
  "order": 2
}
```
- **响应示例**:
```json
{ "code": 1, "msg": null, "data": { "updated": true } }
```

---

## 数据结构定义

### GoalSummary
| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| id | string | 目标ID |
| title | string | 目标标题 |
| desc | string | 目标描述 |
| status | string | 状态：TODO/IN_PROGRESS/DONE |
| progress | number | 进度 0-100 |
| eta | string | 预计达成时间 |
| isPrimary | boolean | 是否为主目标 |

### Milestone
| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| id | string | 里程碑ID |
| goalId | string | 所属目标ID |
| title | string | 里程碑标题 |
| desc | string | 里程碑描述 |
| status | string | 状态：TODO/IN_PROGRESS/DONE |
| progress | number | 进度 0-100 |
| order | number | 排序顺序 |

### SuccessCriteria
| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| salary | string | 薪资预期 |
| companies | string[] | 目标公司列表 |
| cities | string[] | 目标城市列表 |

### LongTermAspiration
| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| title | string | 愿景标题 |
| desc | string | 愿景描述 |

### AiAdvice
| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| content | string | AI建议内容 |
| evidenceReferences | object[] | 非必须；仅生成接口实时返回的 RAG 证据引用，overview/detail 可为空 |
| retrievalDiagnostics | object | 非必须；仅生成接口实时返回的检索诊断，overview/detail 可为空 |

## 建表语句

```sql
DROP TABLE IF EXISTS ai_career_plan.goal_milestone CASCADE;
DROP TABLE IF EXISTS ai_career_plan.goal CASCADE;
-- =============================================
-- goal 表（目标表）
-- =============================================
CREATE TABLE ai_career_plan.goal (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    goal_desc TEXT,
    status VARCHAR(50) DEFAULT 'TODO',              -- TODO / IN_PROGRESS / DONE
    progress INTEGER DEFAULT 0,                      -- 0-100
    eta VARCHAR(100),                                -- 预计达成时间
    is_primary BOOLEAN DEFAULT FALSE,                -- 是否为主目标
    success_salary VARCHAR(100),                     -- 成功准则-薪资
    success_companies TEXT,                          -- 成功准则-目标公司(JSON数组)
    success_cities TEXT,                             -- 成功准则-目标城市(JSON数组)
    long_term_aspirations TEXT,                      -- 长期愿景(JSON数组)
    ai_advice TEXT,                                  -- AI建议
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- goal_milestone 表（里程碑表）
-- =============================================
CREATE TABLE ai_career_plan.goal_milestone (
    id BIGSERIAL PRIMARY KEY,
    goal_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    milestone_desc TEXT,
    status VARCHAR(50) DEFAULT 'TODO',              -- TODO / IN_PROGRESS / DONE
    progress INTEGER DEFAULT 0,                      -- 0-100
    sort_order INTEGER DEFAULT 1,                    -- 排序顺序
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_goal_user_id ON ai_career_plan.goal(user_id);
CREATE INDEX idx_goal_is_primary ON ai_career_plan.goal(is_primary);
CREATE INDEX idx_goal_milestone_goal_id ON ai_career_plan.goal_milestone(goal_id);
CREATE INDEX idx_goal_milestone_user_id ON ai_career_plan.goal_milestone(user_id);
-- goal 表注释
COMMENT ON TABLE ai_career_plan.goal IS '目标表';
COMMENT ON COLUMN ai_career_plan.goal.id IS '主键';
COMMENT ON COLUMN ai_career_plan.goal.user_id IS '用户ID';
COMMENT ON COLUMN ai_career_plan.goal.title IS '目标标题';
COMMENT ON COLUMN ai_career_plan.goal.goal_desc IS '目标描述';
COMMENT ON COLUMN ai_career_plan.goal.status IS '状态：TODO/IN_PROGRESS/DONE';
COMMENT ON COLUMN ai_career_plan.goal.progress IS '进度：0-100';
COMMENT ON COLUMN ai_career_plan.goal.eta IS '预计达成时间';
COMMENT ON COLUMN ai_career_plan.goal.is_primary IS '是否为主目标';
COMMENT ON COLUMN ai_career_plan.goal.success_salary IS '成功准则-薪资预期';
COMMENT ON COLUMN ai_career_plan.goal.success_companies IS '成功准则-目标公司(JSON数组)';
COMMENT ON COLUMN ai_career_plan.goal.success_cities IS '成功准则-目标城市(JSON数组)';
COMMENT ON COLUMN ai_career_plan.goal.long_term_aspirations IS '长期愿景(JSON数组)';
COMMENT ON COLUMN ai_career_plan.goal.ai_advice IS 'AI建议';

-- goal_milestone 表注释
COMMENT ON TABLE ai_career_plan.goal_milestone IS '里程碑表';
COMMENT ON COLUMN ai_career_plan.goal_milestone.id IS '主键';
COMMENT ON COLUMN ai_career_plan.goal_milestone.goal_id IS '关联目标ID';
COMMENT ON COLUMN ai_career_plan.goal_milestone.user_id IS '用户ID';
COMMENT ON COLUMN ai_career_plan.goal_milestone.title IS '里程碑标题';
COMMENT ON COLUMN ai_career_plan.goal_milestone.milestone_desc IS '里程碑描述';
COMMENT ON COLUMN ai_career_plan.goal_milestone.status IS '状态：TODO/IN_PROGRESS/DONE';
COMMENT ON COLUMN ai_career_plan.goal_milestone.progress IS '进度：0-100';
COMMENT ON COLUMN ai_career_plan.goal_milestone.sort_order IS '排序顺序';
```

## 2026-06-09 里程碑父目标一致性补充

- `PATCH /api/goals/{id}/milestones/{ms_id}` 必须同时校验 `goal_id = id`、`id = ms_id`、`user_id = BaseContext.userId`。
- 当 `ms_id` 属于当前用户但不属于路径中的 `id` 时，不应更新该里程碑；该约束用于保持 REST 资源层级、AI tool 用户隔离胶水与 Goals-RAG payload 来源一致。
