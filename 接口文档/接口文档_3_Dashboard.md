# 职引 AI - Dashboard 模块接口文档

---

## 数据模型

本模块采用两张表设计：
- **用户职业数据表（user_career_data）**：存储 Dashboard 相关的所有用户数据（岗位画像、匹配度、市场趋势、能力雷达、行动建议）
- **用户职业发展路径表（user_roadmap_steps）**：存储用户个人的职业发展阶段路径（roadmap steps）

两张表通过 `user_id` 与用户表关联。

### 2.1 用户职业数据表（user_career_data）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户 ID（逻辑外键） |
| job_profile | JSON | 岗位画像信息（岗位名称、行业、城市等） |
| match_summary | JSON | 匹配度摘要（分数、描述、标签、各维度得分） |
| market_trends | JSON | 市场趋势数据（岗位需求、薪资、热门技能） |
| skill_radar | JSON | 能力雷达图数据（技术/创新/抗压/沟通/学习/实习） |
| actions | JSON | 行动建议列表 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### 2.2 用户职业发展路径表（user_roadmap_steps）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户 ID（逻辑外键） |
| job_profile_id | BIGINT | 关联岗位 ID |
| current_step_index | INTEGER | 当前所在阶段索引 |
| steps | JSON | 职业发展阶段列表 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

**说明**：`user_roadmap_steps` 表在接口文档 3（Dashboard 模块）和接口文档 8（Roadmap 模块）中均有使用。

---

## 数据来源说明

### Dashboard 模块数据来源

| user_career_data 字段 | 数据来源 | 说明 |
|--------|------|------|
| job_profile | 预置岗位画像库 / AI 生成 | 基于企业就业数据构建的岗位画像 |
| match_summary.dimension_scores | student_capability_profile.capability_scores | 6 个维度能力评分 |
| match_summary.score | AI 计算 | 根据能力评分与岗位画像权重加权计算 |
| match_summary.description | AI 生成 | 基于匹配分析结果的描述文字 |
| match_summary.tags | resume_analysis_result.highlights | 简历亮点转换 |
| skill_radar | student_capability_profile.capability_scores | 直接使用 6 个维度分数 |
| actions | resume_analysis_result.suggestions | 将建议转换为行动项 |
| market_trends | 预置市场数据 | 基于企业统计的岗位需求趋势 |

### student_capability_profile.capability_scores 结构

```json
{
  "professional_skill": 85,    // 专业技能 (0-100)
  "certificate": 70,           // 证书 (0-100)
  "innovation": 80,            // 创新能力 (0-100)
  "learning": 88,              // 学习能力 (0-100)
  "resilience": 75,            // 抗压能力 (0-100)
  "communication": 82,         // 沟通能力 (0-100)
  "internship": 78             // 实习能力 (0-100)
}
```

### resume_analysis_result.suggestions 结构

```json
[
  {
    "type": "SKILL",
    "content": "建议学习 React 框架以提升技术栈广度"
  },
  {
    "type": "CERTIFICATE",
    "content": "考取软件设计师证书增强竞争力"
  }
]
```

---

## job_profile JSON 结构

```json
{
  "id": 1001,
  "name": "UI/视觉设计师",
  "industry": "互联网/科技",
  "city": "北京",
  "min_experience_years": 0,
  "max_experience_years": 2,
  "salary_range_min": 8000,
  "salary_range_max": 15000,
  "description": "负责公司 UI/视觉设计工作...",
  "skills": [
    {"name": "Photoshop", "level": 4, "category": "专业技能"},
    {"name": "Figma", "level": 4, "category": "专业技能"},
    {"name": "沟通能力", "level": 3, "category": "软技能"}
  ],
  "capability_weights": {
    "professional_skill": 1.5,   // 专业技能权重
    "certificate": 1.0,          // 证书权重
    "innovation": 1.3,           // 创新能力权重
    "learning": 1.2,             // 学习能力权重
    "resilience": 1.0,           // 抗压能力权重
    "communication": 1.2,        // 沟通能力权重
    "internship": 1.3            // 实习能力权重
  }
}
```

---

## match_summary JSON 结构

```json
{
  "score": 85,
  "description": "您与目标岗位匹配度很高，创意 UI 和用户共情是您的突出优势",
  "tags": ["创意 UI", "用户共情", "作品集完整"],
  "dimension_scores": {
    "technical": 80,      // 对应 professional_skill
    "innovation": 90,
    "resilience": 70,
    "communication": 85,
    "learning": 88,
    "internship": 75
  }
}
```

---

## skill_radar JSON 结构

```json
{
  "technical": 80,        // 对应 student_capability_profile.capability_scores.professional_skill
  "innovation": 90,       // 对应 innovation
  "resilience": 70,       // 对应 resilience
  "communication": 85,    // 对应 communication
  "learning": 88,         // 对应 learning
  "internship": 75        // 对应 internship
}
```

---

## actions JSON 结构

```json
[
  {
    "id": "a_001",
    "title": "完成 React 认证",
    "desc": "弥补关键技术差距",
    "icon": "school",
    "link": "/goals",
    "priority": "high"
  }
]
```

---

## market_trends JSON 结构

```json
[
  {
    "name": "UI/视觉设计",
    "growth": 0.12,
    "value": 85,
    "source": "企业统计"
  },
  {
    "name": "交互设计",
    "growth": 0.18,
    "value": 92,
    "source": "企业统计"
  }
]
```

---

## 步骤字段结构说明

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

## AI 服务模块

### Dashboard-AI 服务职责

由专门的 **Dashboard-AI** 服务负责以下分析任务：

1. **岗位画像生成**：基于企业 1w 条就业数据，使用 AI 构建不少于 10 个就业岗位画像
   - 自动提取岗位所需的专业技能、证书要求
   - 分析创新能力、学习能力、抗压能力、沟通能力、实习能力等软性要求
   - 设定各能力维度的权重系数（capability_weights）

2. **人岗匹配计算**：根据用户简历与岗位画像，计算匹配度
   - 从 `student_capability_profile.capability_scores` 获取用户能力评分
   - 与岗位画像的 `capability_weights` 加权计算匹配分数
   - 生成匹配描述和亮点标签

3. **行动建议转换**：将简历分析建议转换为可执行的行动项
   - 从 `resume_analysis_result.suggestions` 读取建议
   - 转换为带优先级、图标、跳转链接的行动项

4. **市场趋势分析**：基于企业就业数据统计
   - 岗位需求趋势
   - 薪资水平分布
   - 热门技能排行

---

## 3.1 获取仪表盘汇总

### 3.1.1 基本信息

请求路径：/api/dashboard/summary

请求方式：GET

接口描述：该接口用于获取当前登录用户的仪表盘汇总信息，包括匹配度摘要、市场趋势、能力雷达图和行动建议。

**数据来源说明**：
- 后端从 `user_career_data` 表读取数据
- 如果 `user_career_data` 无数据，则触发 AI 服务重新计算：
  - 从 `student_capability_profile` 获取能力评分 → 填充 `skill_radar` 和 `match_summary.dimension_scores`
  - 从 `resume_analysis_result.highlights` 获取亮点 → 转换为 `match_summary.tags`
  - 从 `resume_analysis_result.suggestions` 获取建议 → 转换为 `actions`

---

### 3.1.2 请求参数

参数说明：

| 参数名 | 类型 | 是否必须 | 默认值 | 备注 |
|--------|------|----------|--------|------|
| job_profile_id | BIGINT | 非必须 | null | 指定岗位画像 ID，不传则返回用户最匹配的岗位 |

---

### 3.1.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
| :--- | :--- | :--- | :--- |
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据 |
| &#124;- job_profile | object | 非必须 | 当前匹配的岗位画像信息 |
| &#124;- &#124;- id | BIGINT | 非必须 | 岗位画像 ID |
| &#124;- &#124;- name | string | 非必须 | 岗位名称 |
| &#124;- &#124;- industry | string | 非必须 | 所属行业 |
| &#124;- &#124;- city | string | 非必须 | 工作城市 |
| &#124;- match_summary | object | 非必须 | 匹配度摘要 |
| &#124;- &#124;- score | number | 非必须 | 匹配分数 (0-100) |
| &#124;- &#124;- description | string | 非必须 | AI 生成的匹配描述文字 |
| &#124;- &#124;- tags | string[] | 非必须 | 亮点标签列表 |
| &#124;- &#124;- dimension_scores | object | 非必须 | 各维度匹配得分 |
| &#124;- market_trends | object[] | 非必须 | 市场趋势列表 |
| &#124;- &#124;- name | string | 非必须 | 专业/岗位名称 |
| &#124;- &#124;- growth | number | 非必须 | 增长率 (小数格式，如 0.14 表示 14%) |
| &#124;- &#124;- value | number | 非必须 | 热度值 (0-100) |
| &#124;- &#124;- source | string | 非必须 | 数据来源（企业统计/AI 预测） |
| &#124;- skill_radar | object | 非必须 | 能力雷达图数据 |
| &#124;- &#124;- technical | number | 非必须 | 技术能力得分 (0-100)，来自 capability_scores.professional_skill |
| &#124;- &#124;- innovation | number | 非必须 | 创新能力得分 (0-100) |
| &#124;- &#124;- resilience | number | 非必须 | 抗压能力得分 (0-100) |
| &#124;- &#124;- communication | number | 非必须 | 沟通能力得分 (0-100) |
| &#124;- &#124;- learning | number | 非必须 | 学习能力得分 (0-100) |
| &#124;- &#124;- internship | number | 非必须 | 实习能力得分 (0-100) |
| &#124;- actions | object[] | 非必须 | 行动建议列表 |
| &#124;- &#124;- id | string | 非必须 | 行动项 ID |
| &#124;- &#124;- title | string | 非必须 | 行动标题 |
| &#124;- &#124;- desc | string | 非必须 | 行动描述 |
| &#124;- &#124;- icon | string | 非必须 | 图标名称 |
| &#124;- &#124;- link | string | 非必须 | 跳转链接 |
| &#124;- &#124;- priority | string | 非必须 | 优先级 (high/medium/low) |

响应数据样例：

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "job_profile": {
      "id": 1001,
      "name": "UI/视觉设计师",
      "industry": "互联网/科技",
      "city": "北京"
    },
    "match_summary": {
      "score": 85,
      "description": "您与目标岗位匹配度很高，创意 UI 和用户共情是您的突出优势",
      "tags": ["创意 UI", "用户共情", "作品集完整"],
      "dimension_scores": {
        "technical": 80,
        "innovation": 90,
        "resilience": 70,
        "communication": 85,
        "learning": 88,
        "internship": 75
      }
    },
    "market_trends": [
      {
        "name": "UI/视觉设计",
        "growth": 0.12,
        "value": 85,
        "source": "企业统计"
      },
      {
        "name": "交互设计",
        "growth": 0.18,
        "value": 92,
        "source": "企业统计"
      },
      {
        "name": "产品经理",
        "growth": 0.08,
        "value": 78,
        "source": "企业统计"
      }
    ],
    "skill_radar": {
      "technical": 80,
      "innovation": 90,
      "resilience": 70,
      "communication": 85,
      "learning": 88,
      "internship": 75
    },
    "actions": [
      {
        "id": "a_001",
        "title": "完成 React 认证",
        "desc": "弥补关键技术差距",
        "icon": "school",
        "link": "/goals",
        "priority": "high"
      },
      {
        "id": "a_002",
        "title": "参与实际项目实习",
        "desc": "提升实战经验和抗压能力",
        "icon": "briefcase",
        "link": "/internships",
        "priority": "medium"
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
| `404` | 用户尚未上传简历，无 Dashboard 数据 | `{"code": 404, "msg": "请先上传简历以生成职业分析"}` |

---

### 3.1.5 前端处理建议

**未上传简历的处理**：

当接口返回 `404` 状态码时，前端应：
1. 拦截该响应，不展示错误提示
2. 跳转到简历上传页面，或展示引导弹窗
3. 提示文案示例：
   - 标题：**"暂无简历分析数据"**
   - 内容："Dashboard 功能需要基于您的简历进行分析。请先上传简历，我们将为您生成职业匹配度分析、能力雷达图和发展建议。"
   - 操作按钮："去上传简历" → 跳转到 `/resume/upload` 页面

**空数据兜底处理**：

当 `market_trends`、`actions` 等字段为空数组时，前端应展示空状态占位 UI，而非报错。

---

## 3.2 获取仪表盘进化路线

### 3.2.1 基本信息

请求路径：/api/dashboard/roadmap

请求方式：GET

接口描述：该接口用于获取当前登录用户的职业发展路径信息，即首页"职业进化地图"卡片数据。数据由 AI 职业规划算法根据用户简历和目标岗位生成。

**数据来源说明**：
- 后端从 `user_roadmap_steps` 表读取数据
- 如果 `user_roadmap_steps` 无数据，则触发 AI 服务根据用户能力画像和岗位画像生成职业发展路径

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
      },
      {
        "title": "UI 设计专家/设计经理",
        "time": "目标：第 6-8 年",
        "status": "待解锁",
        "icon": "diamond",
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
| `404` | 用户尚未上传简历，无 Roadmap 数据 | `{"code": 404, "msg": "请先上传简历以生成职业规划"}` |

---

### 3.2.5 前端处理建议

**未上传简历的处理**：

当接口返回 `404` 状态码时，前端应：
1. 拦截该响应，不展示错误提示
2. 跳转到简历上传页面，或展示引导弹窗
3. 提示文案示例：
   - 标题：**"暂无职业规划数据"**
   - 内容："职业发展路径需要根据您的简历和能力画像生成。请先上传简历，我们将为您规划职业发展路线。"
   - 操作按钮："去上传简历" → 跳转到 `/resume/upload` 页面

**数据加载状态**：

当用户刚上传完简历但 Dashboard 数据尚未生成完成时，前端可展示加载状态或骨架屏，等待数据就绪后自动刷新。

---

## 数据流转说明

```
用户上传简历
    ↓
Resume-AI 服务异步分析：
  - 解析简历 → resume_analysis_result.parsed_data
  - 评分建议 → resume_analysis_result.scores, suggestions, highlights
  - 生成能力画像 → student_capability_profile.capability_scores
    ↓
Dashboard-AI 服务聚合计算：
  - 从 student_capability_profile 读取 6 个维度能力评分
  - 与岗位画像的 capability_weights 加权计算 → match_summary.score
  - 将 resume_analysis_result.highlights 转换为 → match_summary.tags
  - 将 resume_analysis_result.suggestions 转换为 → actions
  - 复制 capability_scores → skill_radar
    ↓
聚合存储到：
  - user_career_data 表：job_profile, match_summary, market_trends, skill_radar, actions
  - user_roadmap_steps 表：steps
    ↓
GET /api/dashboard/summary → 返回 user_career_data 数据
GET /api/dashboard/roadmap → 返回 user_roadmap_steps 数据
```

---

## 表关系说明

```
user_vector_store (简历向量存储)
    ↓ (1:1)
resume_analysis_result (简历分析结果)
    ↓ (1:1)
student_capability_profile (学生能力画像)
    ↓ (AI 聚合计算)
user_career_data (Dashboard 职业数据)
user_roadmap_steps (职业发展路径)
```

---

## 接口列表汇总

| 接口编号 | 接口名 | 方法 | 路径 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| 3.1 | 获取仪表盘汇总 | GET | /api/dashboard/summary | 匹配摘要、趋势、雷达、行动项 |
| 3.2 | 获取仪表盘进化路线 | GET | /api/dashboard/roadmap | 首页"职业进化地图"卡片数据 |
