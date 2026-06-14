# 职引AI - Roadmap 模块接口文档

## 模块概述
职业地图相关接口，包括行业分段、地图图谱、节点详情和节点搜索。
本模块基于企业 1w 条就业数据，由 AI 构建岗位关联图谱，支持垂直晋升路径和换岗路径规划。

---

## 数据模型

本模块采用四张表设计：
- **行业分段表（job_segments）**：存储行业/领域分类
- **岗位关联图谱节点表（job_graph_nodes）**：存储岗位图谱节点信息
- **岗位关联关系表（job_graph_edges）**：存储岗位间的晋升/换岗关系
- **岗位换岗路径表（job_transition_paths）**：存储 AI 推荐的换岗路径

**说明**：`user_roadmap_steps` 表（用户职业发展路径表）在接口文档 3（Dashboard 模块）和本接口文档 8 中均有使用，存储用户个人的职业发展阶段路径。

### 3.1 行业分段表（job_segments）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 |
| segment_code | VARCHAR(50) | 分段编码（唯一） |
| segment_name | VARCHAR(100) | 分段名称 |
| sort_order | INT | 排序 |
| create_time | DATETIME | 创建时间 |

### 3.2 岗位关联图谱节点表（job_graph_nodes）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 |
| job_profile_id | BIGINT | 关联岗位 ID |
| segment_id | BIGINT | 所属分段 ID |
| node_type | VARCHAR(20) | 节点类型 (core/secondary/transition) |
| level | INT | 职级等级 (1-10) |
| title | VARCHAR(255) | 岗位标题 |
| subtitle | VARCHAR(100) | 副标题（年限等） |
| label | VARCHAR(255) | 显示标签 |
| sub_label | VARCHAR(100) | 显示副标签 |
| kind | VARCHAR(20) | 种类 (core/secondary/transition) |
| variant | VARCHAR(20) | 变体 (primary/neutral) |
| tags | JSON | 标签列表 |
| x_coord | INT | 图谱 X 坐标 |
| y_coord | INT | 图谱 Y 坐标 |
| summary | TEXT | 岗位描述 |
| requirements | JSON | 技能要求列表 |
| recommended_skills | JSON | 推荐技能列表 |
| create_time | DATETIME | 创建时间 |

### 3.3 岗位关联关系表（job_graph_edges）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 |
| from_node_id | BIGINT | 起始节点 ID |
| to_node_id | BIGINT | 目标节点 ID |
| edge_type | VARCHAR(20) | 关系类型 (vertical/lateral) |
| transition_difficulty | INT | 转换难度 (1-5) |
| avg_transition_time_months | INT | 平均转换时间（月） |
| success_rate | DECIMAL(5,4) | 转换成功率 (0-1) |
| required_skills_gap | JSON | 需要补充的技能差距 |
| description | TEXT | 路径描述 |
| create_time | DATETIME | 创建时间 |

### 3.4 岗位换岗路径表（job_transition_paths）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 |
| from_job_profile_id | BIGINT | 起始岗位 ID |
| to_job_profile_id | BIGINT | 目标岗位 ID |
| path_type | VARCHAR(20) | 路径类型 (direct/stepping_stone) |
| intermediate_nodes | JSON | 中间节点 ID 列表 |
| recommended_actions | JSON | AI 推荐的换岗行动 |
| confidence_score | DECIMAL(5,4) | AI 推荐置信度 (0-1) |
| create_time | DATETIME | 创建时间 |

### 3.5 用户职业发展路径表（user_roadmap_steps）

**说明**：此表在接口文档 3 中已定义，此处为引用说明。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户 ID（逻辑外键） |
| job_profile_id | BIGINT | 关联岗位 ID |
| current_step_index | INTEGER | 当前所在阶段索引 |
| steps | JSON | 职业发展阶段列表 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

---

## AI 服务模块

### Roadmap-AI 服务职责

由专门的 **Roadmap-AI** 服务负责以下分析任务：

1. **岗位关联图谱构建**：基于企业 1w 条就业数据，使用 AI 构建岗位间的关联关系
   - 识别岗位间的晋升路径（垂直关系）
   - 识别相关岗位间的换岗路径（横向关系）
   - 计算转换难度、成功率、所需时间

2. **垂直岗位图谱**：涵盖岗位描述、岗位晋升路径关联信息
   - 每个岗位序列至少包含 3-5 个晋升层级
   - 标注每个层级的技能要求和转换条件

3. **换岗路径图谱**：将相关岗位进行血缘关系关联，规划岗位转换路径
   - 至少提供 5 个岗位的换岗路径
   - 每个岗位的换岗路径不少于 2 条
   - 支持直接换岗和通过中间岗位 stepping-stone 换岗

4. **个性化路径推荐**：根据用户简历和目标，推荐最适合的发展路径

**注**：当前企业 1w 条就业数据尚未提供，以上功能待数据提供后完成。

---

## 接口列表

| 接口名 | 方法 | 路径 | 说明 |
| :--- | :--- | :--- | :--- |
| 获取行业分段 | GET | `/api/roadmap/segments` | 左侧行业标签 |
| 获取地图图谱 | GET | `/api/roadmap/graph` | 节点/路径（支持 vertical/lateral） |
| 获取节点详情 | GET | `/api/roadmap/nodes/{id}` | 右侧详情面板 |
| 搜索节点 | GET | `/api/roadmap/search` | query + segment |

---

## 详细接口定义

### 获取行业分段
- **请求方法**: `GET`
- **请求路径**: `/api/roadmap/segments`
- **鉴权**: 需要
- **响应示例**:
```json
{ "code": 200, "data": [{ "id": "seg_01", "name": "互联网" }, { "id": "seg_02", "name": "AI" }] }
```

---

### 获取地图图谱
- **请求方法**: `GET`
- **请求路径**: `/api/roadmap/graph`
- **鉴权**: 需要
- **Query 参数**:
  - `segment` (string, 可选) 行业分段 id
  - `mode` (string, 可选) `vertical` / `lateral` - `vertical` 返回垂直晋升路径，`lateral` 返回换岗路径
  - `q` (string, 可选) 搜索关键字
  - `from_job` (BIGINT, 可选) 起始岗位 ID（用于换岗路径查询）
- **响应示例**:
```json
{
  "code": 200,
  "data": {
    "mode": "vertical",
    "nodes": [
      {
        "id": "n_001",
        "title": "初级前端工程师",
        "label": "初级前端工程师",
        "subtitle": "0-2 年",
        "subLabel": "0-2 年",
        "kind": "core",
        "tags": ["基础"],
        "x": 140,
        "y": 90,
        "variant": "primary"
      },
      {
        "id": "n_002",
        "title": "中级前端工程师",
        "label": "中级前端工程师",
        "subtitle": "2-4 年",
        "subLabel": "2-4 年",
        "kind": "secondary",
        "tags": ["工程化"],
        "x": 240,
        "y": 180,
        "variant": "neutral"
      }
    ],
    "paths": [
      {
        "from": "n_001",
        "to": "n_002",
        "variant": "primary",
        "edgeType": "vertical",
        "difficulty": 2,
        "avgTimeMonths": 24,
        "successRate": 0.75
      }
    ]
  }
}
```

#### 垂直晋升路径示例（mode=vertical）

展示岗位晋升链路，如：初级前端工程师 → 中级前端工程师 → 高级前端工程师 → 技术专家/技术经理

#### 换岗路径示例（mode=lateral）

展示相关岗位换岗路径，如：
- 前端工程师 → 后端工程师（需要补充：数据库、服务器端语言）
- 前端工程师 → 全栈工程师（需要补充：后端技能 + 系统架构）
- 前端工程师 → 产品经理（需要补充：产品思维、需求分析）
- 前端工程师 → UI/UX 设计师（需要补充：设计理论、设计工具）
- 前端工程师 → 技术经理（需要补充：团队管理、项目管理）

**注**：换岗路径至少覆盖 5 个岗位，每个岗位不少于 2 条换岗路径。完整数据待企业 1w 条就业数据提供后由 AI 生成。

---

### 获取节点详情
- **请求方法**: `GET`
- **请求路径**: `/api/roadmap/nodes/{id}`
- **鉴权**: 需要
- **响应示例**:
```json
{
  "code": 200,
  "data": {
    "id": "n_001",
    "title": "初级前端工程师",
    "summary": "掌握基础与常用框架",
    "requirements": ["HTML/CSS/JS", "Vue/React 基础"],
    "recommended_skills": ["TypeScript", "工程化"],
    "next_nodes": ["n_002"],
    "vertical_paths": [
      {
        "target": "n_002",
        "targetTitle": "中级前端工程师",
        "difficulty": 2,
        "avgTimeMonths": 24,
        "successRate": 0.75,
        "requiredSkillsGap": [
          { "skill": "TypeScript", "level": 3 },
          { "skill": "工程化", "level": 3 },
          { "skill": "性能优化", "level": 2 }
        ]
      }
    ],
    "lateral_paths": [
      {
        "target": "n_101",
        "targetTitle": "UI 设计师",
        "difficulty": 3,
        "avgTimeMonths": 12,
        "successRate": 0.60,
        "requiredSkillsGap": [
          { "skill": "Figma/Sketch", "level": 4 },
          { "skill": "设计理论", "level": 3 }
        ]
      },
      {
        "target": "n_102",
        "targetTitle": "产品经理",
        "difficulty": 4,
        "avgTimeMonths": 18,
        "successRate": 0.45,
        "requiredSkillsGap": [
          { "skill": "需求分析", "level": 4 },
          { "skill": "产品思维", "level": 4 },
          { "skill": "数据分析", "level": 2 }
        ]
      }
    ]
  }
}
```

**注**：`vertical_paths` 和 `lateral_paths` 数据待企业 1w 条就业数据提供后，由 Roadmap-AI 服务分析生成。

---

### 搜索节点
- **请求方法**: `GET`
- **请求路径**: `/api/roadmap/search`
- **鉴权**: 需要
- **Query 参数**:
  - `q` (string, 必填) 关键字
  - `segment` (string, 可选) 行业分段 id
  - `limit` (number, 可选, 默认 20)
- **响应示例**:
```json
{
  "code": 200,
  "data": {
    "items": [
      {
        "id": "n_002",
        "title": "中级前端工程师",
        "subtitle": "2-4 年",
        "tags": ["工程化"],
        "variant": "neutral",
        "hasVerticalPaths": true,
        "hasLateralPaths": true
      }
    ]
  }
}
```

---

## 数据流转说明

```
企业 1w 条就业数据导入
    ↓
Roadmap-AI 分析岗位间关系
    ↓
构建 job_graph_nodes（岗位节点）
构建 job_graph_edges（岗位关联关系）
构建 job_transition_paths（换岗路径）
    ↓
GET /api/roadmap/graph 或 /api/roadmap/nodes/{id}
    ↓
返回岗位图谱和路径数据供前端展示
```

**注**：当前企业 1w 条就业数据尚未提供，图谱数据暂为空。待数据提供后，由 Roadmap-AI 服务完成以下任务：
1. 构建不少于 10 个就业岗位画像
2. 建立岗位间的关联图谱
3. 垂直岗位图谱：涵盖岗位描述、岗位晋升路径关联信息
4. 换岗路径图谱：至少提供 5 个岗位的换岗路径，每个岗位的换岗路径不少于 2 条

---

## 2026-06-14 Roadmap-RAG 集成迁移补充

### 变更摘要

Roadmap 个性化职业路径推荐继续保持 Java 对外接口不变：前端调用
`GET /api/roadmap/recommendations/personalized`，Java 负责 JWT 鉴权、`BaseContext.userId`
上下文、岗位和简历摘要组装、`Result<T>` 包装、Redis 缓存和本地相似度降级。
横向换岗推荐中的 RAG 检索、证据选择和诊断输出迁移到统一 Python 聚合服务
`ai-service`，不再提交旧 `ai_service/` Roadmap 新能力。

### 对外接口

- **请求方法**: `GET`
- **请求路径**: `/api/roadmap/recommendations/personalized`
- **鉴权**: 需要登录，Java 从 JWT 中解析当前用户 ID。
- **前端影响**: `website/src/api/roadmapRecommendation.ts` 增加 `evidence` 和
  `ragDiagnostics` 类型；前端仍只调用 Java，不直连 Python。

成功响应继续遵循 `Result<T>`：

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "currentJob": "Frontend Engineer",
    "verticalPath": {},
    "lateralPaths": [
      {
        "targetJobId": 2,
        "targetJobName": "AI Application Engineer",
        "targetCategoryCode": "AI_APP",
        "matchScore": 0.86,
        "transitionDifficulty": 3,
        "estimatedMonths": 15,
        "requiredSkills": ["RAG"],
        "possessedSkills": ["Python"],
        "aiRecommendationReason": "基于 Roadmap-RAG 证据推荐",
        "evidence": [
          {
            "documentType": "job",
            "jobId": 2,
            "chunkId": "job:2:summary",
            "score": 0.03,
            "source": "summary_index"
          }
        ],
        "pathNodes": []
      }
    ],
    "ragDiagnostics": {
      "queries": ["Frontend Engineer transition learning path"],
      "filters": {
        "excludeSameCategory": true,
        "documentTypes": ["job", "resume_summary", "jd_summary"]
      },
      "fusion": "rrf",
      "reranker": "deterministic-fallback",
      "candidateCount": 3
    },
    "generatedAt": "2026-06-14T08:00:00"
  }
}
```

### Java 到 Python 契约

- **Python endpoint**: `POST /api/roadmap/recommendations/personalized`
- **Python 实现位置**: `ai-service/career_ai/roadmap_rag_service.py`
- **Python 聚合入口**: `ai-service/app/main.py`
- **Python base URL**: `fuchuang.ai.python.base-url`，默认 `http://127.0.0.1:8090`
- **超时优先级**:
  `FUCHUANG_AI_PYTHON_ROADMAP_TIMEOUT_SECONDS` >
  `FUCHUANG_PYTHON_AI_ROADMAP_TIMEOUT_SECONDS` >
  `fuchuang.ai.python.roadmap-timeout-seconds` >
  `fuchuang.ai.python.timeout-seconds` > `8`
- **重试规则**: Java 不自动重试。该接口为读取型推荐接口，幂等性由
  `roadmap:recommendations:personalized` 缓存和当前用户上下文控制。
- **错误映射**:
  - Python HTTP 5xx、连接失败、非法 JSON、schema 字段类型错误：Java 记录 warn，
    使用本地相似度 fallback，不向前端暴露 Python 内部异常。
  - Python timeout：Java 使用本地 fallback，并在日志中保留超时类型。
  - Python 返回空推荐：Java 使用本地 fallback。
  - Python 只返回 1 条横向推荐：Java 保留 Python 推荐并用本地 fallback 补足，
    `ragDiagnostics.supplementedBy=local-similarity-fallback`。

### Python 处理要求

Python Roadmap-RAG 使用 deterministic fallback 实现以下能力，便于在无真实
pgvector、Dashscope embedding/LLM 或 cross-encoder 环境下稳定验证：

1. 递归切块，按段落、句子和字符预算拆分岗位、简历摘要和 JD 摘要。
2. 构建 summary index，并保留 `userId`、`documentType`、`jobId`、
   `categoryCode`、`baseCategoryCode`、`level`、`section`、`source`、
   `visibilityScope` 等 metadata。
3. Multi-Query 覆盖当前岗位、技能差距、目标岗位、JD 要求和横向转型路径表达。
4. BM25 与 embedding-style 哈希向量召回融合，使用 RRF 生成可解释排序。
5. 返回 `evidence` 和 `diagnostics` 白名单字段，不返回 raw resume、raw JD、
   prompt、API key、手机号、邮箱或完整敏感文本。

### 测试口径

- Python 在 `ai-service` 目录执行：
  - `python -B -m py_compile app/main.py career_ai/roadmap_rag_service.py tests/test_roadmap_rag_service.py`
  - `python -B -m pytest tests/test_roadmap_rag_service.py -q -p no:cacheprovider`
  - `python -B -m pytest tests -q -p no:cacheprovider`
- Java：
  - `mvn -pl server -am -Dtest=PythonRoadmapRagClientTest,RoadmapServiceImplTest,RoadmapControllerTest test`
  - 若本轮未纳入 `RoadmapControllerTest`，测试日志必须说明原因，且不得声称覆盖 controller hardening。
  - `mvn -pl server -am -DskipTests compile`
- Frontend：
  - `cd website && npm run build`
- 静态门禁：
  - `git diff --check`、`git diff --cached --check`、提交后 `git diff --check origin/master..HEAD` 必须无输出。
  - `git diff --name-only --diff-filter=U` 必须无输出；`MERGE_HEAD`、`CHERRY_PICK_HEAD`、`REVERT_HEAD`、`REBASE_HEAD`、`sequencer`、`rebase-apply`、`rebase-merge` 均不得存在。
  - `RoadmapControllerTest.java` 若被 `.gitignore` 命中，必须使用精确 `git add -f` 纳入，并通过 `git diff --cached --name-status` 或提交后 `git diff --name-status origin/master..HEAD` 证明已提交。
  - `ai-service/tests/test_roadmap_rag_service.py` 不得出现 `from ai_service`、`import ai_service` 或 `MarketAiHandler`。
  - 本轮提交不得包含旧 `ai_service/**` Roadmap 新能力、`website/src/views/Roadmap.vue`、`JwtTokenInterceptor.java` 或构建产物。
  - 本轮提交不得包含 `application*.yml`、`database/**`、`deploy/**`、`target/**`、`website/dist/**`、`website/node_modules/**`、`__pycache__/**`、`*.pyc` 或 `.env*`。
  - staged diff 必须通过 secret scan；测试中如需覆盖 token-like 脱敏，应使用运行时拼接或明确 test-only allowlist，避免静态 `sk-*` 哨兵被误判为真实密钥。
  - 接口文档、测试日志、Obsidian 记录和本轮 staged 文本文件不得出现 mojibake 特征。
