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