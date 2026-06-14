# 职引 AI - Dashboard 模块接口文档

---

## 变更记录

| 日期 | 变更内容 | 影响范围 |
|------|----------|----------|
| 2026-06-09 | 补充 `user_vector_store.content` SQL 层归属读取门禁、Python `resume_profile` 嵌套对象防御性过滤、`query_variants` 脱敏和 Dashboard validation error 收窄规则 | `/internal/dashboard/target-job/match` |
| 2026-06-09 | 明确 Dashboard-AI 三层 summary index 合同、`summary_level`、`record_id`/`parent_id` 关系和 evidence 只引用 chunk 的规则 | `/internal/dashboard/target-job/match` |
| 2026-06-08 | 补充 Dashboard-AI `resume_profile` 出站白名单、敏感字段丢弃规则和 `resume_content` 边界说明 | `/internal/dashboard/target-job/match` |
| 2026-06-08 | 补充 Dashboard-AI 目标岗位 RAG 匹配 Python 边界、Java-Python 错误映射、超时/幂等、前端影响和测试口径 | `/api/dashboard/roadmap`、`/internal/dashboard/target-job/match` |

---

## 数据模型

本模块**不需要新建表**，所有数据从现有的 `users` 表、`resume_analysis_result` 表以及 AI 分析计算结果中获取。

### 数据来源说明

| 数据模块 | 数据来源 | 说明 |
|----------|----------|------|
| match_summary | `resume_analysis_result.parsed_data.match_score` + AI 生成描述 | 匹配度分数及文字描述 |
| market_trends | 外部就业市场数据 API / 内部统计 | 专业/岗位趋势数据 |
| skill_radar | `resume_analysis_result.parsed_data.skills` + AI 评估 | 四维能力雷达图 |
| actions | AI 根据差距分析生成 | 个性化行动建议 |
| roadmap | AI 职业规划算法 | 职业发展路径 |

#### skill_radar 字段结构说明

```json
{
  "technical": 80,        // 技术能力 (0-100)
  "innovation": 90,       // 创新能力 (0-100)
  "resilience": 70,       // 抗压能力 (0-100)
  "communication": 85     // 沟通能力 (0-100)
}
```

#### actions 字段结构说明

```json
[
  {
    "id": "a_001",
    "title": "完成 React 认证",
    "desc": "弥补关键技术差距",
    "icon": "school",
    "link": "/goals"
  }
]
```

#### roadmap steps 字段结构说明

```json
[
  {
    "title": "初级 UI/视觉设计师",
    "time": "目标：第 1-2 年",
    "status": "85% 匹配",
    "icon": "person",
    "active": true
  }
]
```

---

## 3.1 获取仪表盘汇总

### 3.1.1 基本信息

请求路径：/api/dashboard/summary

请求方式：GET

接口描述：该接口用于获取当前登录用户的仪表盘汇总信息，包括匹配度摘要、市场趋势、能力雷达图和行动建议。数据从 `resume_analysis_result` 表的 `parsed_data` 字段、外部就业市场数据及 AI 分析结果中提取。

---

### 3.1.2 请求参数

参数说明：

本接口无需请求参数。

---

### 3.1.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
| :--- | :--- | :--- | :--- |
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据 |
| &#124;- match_summary | object | 非必须 | 匹配度摘要 |
| &#124;- &#124;- score | number | 非必须 | 匹配分数 (0-100) |
| &#124;- &#124;- description | string | 非必须 | AI 生成的匹配描述文字 |
| &#124;- &#124;- tags | string[] | 非必须 | 亮点标签列表 |
| &#124;- market_trends | object[] | 非必须 | 市场趋势列表 |
| &#124;- &#124;- name | string | 非必须 | 专业/岗位名称 |
| &#124;- &#124;- growth | number | 非必须 | 增长率 (小数格式，如 0.14 表示 14%) |
| &#124;- &#124;- value | number | 非必须 | 热度值 (0-100) |
| &#124;- skill_radar | object | 非必须 | 能力雷达图数据 |
| &#124;- &#124;- technical | number | 非必须 | 技术能力得分 (0-100) |
| &#124;- &#124;- innovation | number | 非必须 | 创新能力得分 (0-100) |
| &#124;- &#124;- resilience | number | 非必须 | 抗压能力得分 (0-100) |
| &#124;- &#124;- communication | number | 非必须 | 沟通能力得分 (0-100) |
| &#124;- actions | object[] | 非必须 | 行动建议列表 |
| &#124;- &#124;- id | string | 非必须 | 行动项 ID |
| &#124;- &#124;- title | string | 非必须 | 行动标题 |
| &#124;- &#124;- desc | string | 非必须 | 行动描述 |
| &#124;- &#124;- icon | string | 非必须 | 图标名称 |
| &#124;- &#124;- link | string | 非必须 | 跳转链接 |

响应数据样例：
```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "match_summary": {
      "score": 85,
      "description": "您与目标岗位匹配度很高，创意 UI 和用户共情是您的突出优势",
      "tags": ["创意 UI", "用户共情"]
    },
    "market_trends": [
      {
        "name": "计算机科学",
        "growth": 0.14,
        "value": 88
      }
    ],
    "skill_radar": {
      "technical": 80,
      "innovation": 90,
      "resilience": 70,
      "communication": 85
    },
    "actions": [
      {
        "id": "a_001",
        "title": "完成 React 认证",
        "desc": "弥补关键技术差距",
        "icon": "school",
        "link": "/goals"
      }
    ]
  }
}
```

---

### 3.1.4 错误响应

| HTTP 状态码 | 场景 | JSON 响应样例 |
| :--- | :--- | :--- |
| `401` | 未登录或 token 失效 | `{"code": 401, "msg": "未登录"}` |

---

## 3.2 获取仪表盘进化路线

### 3.2.1 基本信息

请求路径：/api/dashboard/roadmap

请求方式：GET

接口描述：该接口用于获取当前登录用户的职业发展路径信息，即首页"职业进化地图"卡片数据。数据由 AI 职业规划算法根据用户简历和目标岗位生成。

---

### 3.2.2 请求参数

参数说明：

本接口无需请求参数。

---

### 3.2.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
| :--- | :--- | :--- | :--- |
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据 |
| &#124;- current_step_index | number | 非必须 | 当前所在阶段索引 (从 0 开始) |
| &#124;- steps | object[] | 非必须 | 职业发展阶段列表 |
| &#124;- &#124;- title | string | 非必须 | 阶段岗位名称 |
| &#124;- &#124;- time | string | 非必须 | 时间目标描述 |
| &#124;- &#124;- status | string | 非必须 | 匹配状态文字 |
| &#124;- &#124;- icon | string | 非必须 | 图标名称 |
| &#124;- &#124;- active | boolean | 非必须 | 是否当前阶段 |

响应数据样例：
```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "current_step_index": 0,
    "steps": [
      {
        "title": "初级 UI/视觉设计师",
        "time": "目标：第 1-2 年",
        "status": "85% 匹配",
        "icon": "person",
        "active": true
      },
      {
        "title": "高级 UI 设计师",
        "time": "目标：第 3-5 年",
        "status": "待解锁",
        "icon": "star",
        "active": false
      }
    ]
  }
}
```

---

### 3.2.4 错误响应

| HTTP 状态码 | 场景 | JSON 响应样例 |
| :--- | :--- | :--- |
| `401` | 未登录或 token 失效 | `{"code": 401, "msg": "未登录"}` |

---

## 数据流转说明

```
用户完成简历解析
    ↓
resume_analysis_result 记录生成
    ↓
AI 异步计算匹配度、雷达图、行动建议、进化路线
    ↓
GET /api/dashboard/summary 或 /api/dashboard/roadmap
    ↓
返回汇总数据供前端仪表盘展示
```

---

## 接口列表汇总

| 接口编号 | 接口名 | 方法 | 路径 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| 3.1 | 获取仪表盘汇总 | GET | /api/dashboard/summary | 匹配摘要、趋势、雷达、行动项 |
| 3.2 | 获取仪表盘进化路线 | GET | /api/dashboard/roadmap | 首页"职业进化地图"卡片数据 |

---

## 2026-06-08 Dashboard-AI 目标岗位匹配合同

### Java-Python 边界

Dashboard 对外仍只暴露 Java Spring Boot `/api/dashboard/**` 接口，前端不直接访问 Python 服务。`GET /api/dashboard/roadmap` 按以下顺序处理：

1. 若 `user_roadmap_steps` 已存在，直接返回已生成路线。
2. 若 `user_career_data.target_job_id` 存在且能在 `job` 表中查到有效岗位，Java 基于该岗位生成垂直晋升路线，不调用 Python。
3. 若目标岗位缺失或无效，Java 读取当前用户最新 `resume_analysis_result`、对应 `user_vector_store.content` 和 `job` 候选快照，调用 Python Dashboard-AI：

`POST {fuchuang.ai.python.base-url}/internal/dashboard/target-job/match`

调用约束：

- 鉴权：仅 Java 内部调用；用户身份来自已通过 JWT 的 `BaseContext.userId`。
- Python 地址：复用 `fuchuang.ai.python.base-url`，默认 `http://127.0.0.1:8090`。
- 超时：Dashboard 专用超时默认 30 秒；优先读取 `FUCHUANG_AI_PYTHON_DASHBOARD_TIMEOUT_SECONDS`，再兼容 legacy `FUCHUANG_PYTHON_AI_DASHBOARD_TIMEOUT_SECONDS`，再读取 `fuchuang.ai.python.dashboard-timeout-seconds`，未配置时回退 `fuchuang.ai.python.timeout-seconds`，最后兜底 30 秒。
- 幂等键：`request_id`，格式 `dashboard-target-job-{user_id}-{resume_analysis_id}`。同一输入重复调用应返回稳定匹配结果，不由 Python 写库。
- 重试：本同步读取链路不自动重试；超时和下游不可用直接映射为业务失败。
- 数据职责：Java 负责鉴权、用户边界、业务表读取和落库；读取 `user_vector_store.content` 必须使用 `id + user_id` 条件在 SQL 层完成归属过滤，不得先用 `SELECT * WHERE id = ?` 读取正文后再在 Java 内存中判断归属；Python 不直接读取业务数据库，不接收完整跨用户数据。
- 前端影响：前端仍调用 `/api/dashboard/roadmap` 和 `/api/dashboard/summary`，TypeScript API 契约不变。

### 内部请求

```json
{
  "request_id": "dashboard-target-job-10001-20001",
  "user_id": 10001,
  "resume_analysis_id": 20001,
  "resume_vector_store_id": "resume_vec_20001",
  "resume_profile": {
    "target_role": "AI 算法工程师",
    "skills": ["Python", "机器学习", "RAG"],
    "experience_years": 1
  },
  "resume_content": "Java 截断后的匹配必要简历文本",
  "job_candidates": [
    {
      "job_id": 30001,
      "job_name": "AI 算法工程师",
      "job_category_code": "AI_ENGINEER_JUNIOR",
      "job_level": "JUNIOR",
      "job_level_name": "初级",
      "required_skills": ["Python", "机器学习", "模型部署"],
      "job_description": "负责模型训练、评估和应用落地",
      "job_profile": {"industrySegment": "人工智能"}
    }
  ],
  "filters": {
    "document_type": ["resume", "job", "jd"],
    "visibility_scope": "user_or_public",
    "language": "zh-CN"
  },
  "top_k": 5
}
```

`resume_profile` 由 Java 从 `resume_analysis_result.parsed_data` 显式白名单构造，只允许以下字段进入 Python 请求：

| 字段 | 类型 | 处理规则 |
| :--- | :--- | :--- |
| `target_role` | string | 可选；仅接收字符串、数字、布尔等标量值，转字符串并 `trim`，空值不发送 |
| `skills` | string[] | 可选；来源可为数组或逗号/中文逗号/顿号分隔字符串；数组元素仅接收字符串、数字、布尔等标量值，转字符串、`trim`、去空、去重，最多 12 项 |
| `experience_years` | number | 可选；仅保留数字类型，字符串或其他类型丢弃；Python 内部入口也必须做同等类型校验，非数字值不得进入 summary records |

以下字段不得出现在 `resume_profile` 中：`phone`、`email`、`name`、`location`、`education`、`experience`、`projects`、`raw_text`、`rawText`、`resume_text`、`resumeContent`、`contact` 以及任何未知键或嵌套扩展对象。这里的 `raw_text` 禁止仅指画像对象内的原文类字段；顶层 `resume_content` 仍是 Python Dashboard-AI 的必需输入，由 Java 继续按既有规则截断到 4000 字符后发送。

允许字段也必须做值级敏感信息过滤。若 `target_role` 或 `skills` 的值包含邮箱、手机号、`token`/`secret`/`api_key` 形式的凭据，或长度较长且同时包含字母和数字的 token-like 字符串，Java 必须丢弃该字段值；Python 也必须在生成 `query_variants` 和 summary records 前做同等防御性过滤。Python 内部入口只能把字符串、数字、布尔值等标量转换为画像文本，嵌套对象、数组对象、未知扩展字段必须丢弃，不得通过 `str()` 化进入查询变体或 summary records。`query_variants` 不得回显邮箱、手机号、token-like 片段或连续原始简历正文。

### 内部成功响应

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "matched_job": {
      "job_id": 30001,
      "job_name": "AI 算法工程师",
      "job_level": "JUNIOR",
      "score": 0.87
    },
    "retrieval": {
      "query_variants": ["AI 算法工程师 目标岗位 匹配", "Python 机器学习 RAG 技能差距 岗位要求"],
      "filters_applied": {
        "user_id": 10001,
        "document_type": ["resume", "job", "jd"],
        "visibility_scope": "user_or_public",
        "language": "zh-CN"
      },
      "fusion_method": "rrf",
      "reranker": "deterministic-fallback",
      "candidate_count": 6,
      "selected_evidence_ids": ["job_30001:chunk:0"]
    },
    "evidence_refs": [
      {
        "source_type": "job_chunk",
        "source_id": "job_30001:chunk:0",
        "section": "岗位要求",
        "score": 0.0164
      }
    ]
  }
}
```

### 内部空匹配响应

```json
{
  "code": 0,
  "msg": "NO_MATCH",
  "data": {
    "retrieval": {
      "query_variants": [],
      "filters_applied": {"user_id": 10001},
      "fusion_method": "rrf",
      "reranker": "deterministic-fallback",
      "candidate_count": 0,
      "selected_evidence_ids": []
    },
    "evidence_refs": []
  }
}
```

### RAG 处理要求

- Python 对 `resume_content`、`resume_profile` 和 `job_candidates` 构建临时语料，不读取数据库。
- Python 只可依赖 `resume_profile.target_role`、`resume_profile.skills` 和 `resume_profile.experience_years` 作为结构化画像信号；Java 不得透传 `parsed_data` 原始对象或未知字段。
- 进入检索前使用递归切块，优先按标题、段落、句子、标点和字符预算切分。
- 建立三层 summary index，保留 `summary_level=document|section|chunk`：
  - `document` 记录用于简历或岗位候选的整体粗召回，`record_id` 形如 `resume_vec_10:document` 或 `job_101:document`，`parent_id=null`。
  - `section` 记录用于画像、简历正文、岗位要求、岗位描述等章节级粗召回，`record_id` 形如 `resume_vec_10:section:resume_profile` 或 `job_101:section:job_requirement`，`parent_id` 指向对应 document 记录。
  - `chunk` 记录用于原始证据，`record_id` 形如 `resume_vec_10:chunk:0` 或 `job_101:chunk:0`，`parent_id` 指向对应 section 记录。
- metadata 至少包含 `summary_level`、`record_id`、`parent_id`、`user_id`、`document_type`、`document_id`、`job_id`、`section`、`language`、`visibility_scope`。
- 查询侧生成目标岗位、技能差距、项目经历、JD 要求等 Multi-Query 变体。
- 每个查询执行 BM25 和 deterministic embedding 检索，保留候选分数。
- 使用 Reciprocal Rank Fusion 融合多路结果，默认 reranker 为 `deterministic-fallback`。
- 响应必须返回 `retrieval` diagnostics 和 `evidence_refs`；`evidence_refs.source_id` 只能引用 `summary_level=chunk` 的岗位原始证据，不返回 document/section summary id；`query_variants` 不得包含连续原始简历正文片段、邮箱、手机号或 token-like 值。Java/Python 日志只记录脱敏后的融合方法、候选数和 evidence id 等摘要，不记录完整简历正文、请求体或完整 retrieval 对象。
- `ai-service/app/main.py` 是当前 8090 聚合入口，本轮为 `/internal/dashboard/target-job/match` 挂载 Dashboard endpoint，并仅对 Dashboard endpoint 返回统一 `VALIDATION_ERROR`。旧 `ai_service/` 目录已移除，Dashboard 新能力不得继续落在旧目录。

### Java 错误映射

| Python/HTTP 场景 | Java `Result<T>` |
| :--- | :--- |
| `400` / `422` / 参数缺失 | `Result.error("Dashboard-AI 请求参数错误")` |
| `204` 或 `code=0,msg=NO_MATCH` | `Result.error("暂无可用岗位匹配结果")` |
| 请求超时 | `Result.error("Dashboard-AI 服务超时，请稍后重试")` |
| 连接失败 / `5xx` / 非 JSON | `Result.error("Dashboard-AI 服务暂不可用")` |

### 测试口径

```bash
set PYTHONPATH=ai-service
python -B -m pytest ai-service/tests/test_dashboard_rag_service.py -q -p no:cacheprovider
python -B -m pytest ai-service/tests -q -p no:cacheprovider
mvn -pl common,pojo -am install -DskipTests
mvn -pl server -Dtest=PythonDashboardAiClientTest test
mvn -pl server -Dtest=DashboardServiceImplTest test
mvn -pl server -Dtest=DashboardControllerTest test
mvn -pl server -am -DskipTests compile
```

若本地未安装 pytest，则使用 unittest 并在本地验证记录中说明降级原因，不将临时自动化日志提交到 GitHub 运行仓库。Runtime smoke 应优先验证 8090 Python 服务启动和 `/internal/dashboard/target-job/match` sample request；Java 端到端 smoke 依赖 Redis、PostgreSQL/pgvector、Java 8081、Python 8090 和 `OPENAI_API_KEY`，条件不齐时只能记录未执行原因，不能声明端到端通过。
