# 职引 AI - Market 模块接口文档

## 模块概述

市场探索相关接口，包括岗位画像列表、市场趋势、AI 深度洞察、热门岗位和岗位详情。
本模块基于企业 1w 条就业数据，由 Dashboard-AI 服务分析生成市场趋势和岗位画像。

---

## 数据模型

本模块数据来源于以下表：

| 数据 | 来源表 | 说明 |
|------|--------|------|
| 岗位画像 | `job_profiles` + `job_profile_skills` + `job_profile_capabilities` | 岗位详细信息 |
| 市场趋势 | 企业就业数据统计 / AI 分析 | 岗位需求、薪资趋势 |
| 岗位关联 | `job_graph_nodes` + `job_graph_edges` | 岗位间关联关系 |

---

## AI 服务模块

### Dashboard-AI 服务职责（Market 相关）

由 **Dashboard-AI** 服务负责以下分析任务：

1. **市场趋势分析**：
   - 基于企业 1w 条就业数据统计
   - 岗位需求趋势（增长率、热度值）
   - 薪资水平分布
   - 热门技能排行

2. **热门岗位分析**：
   - 需求旺盛岗位识别
   - 薪资潜力岗位分析
   - 新兴岗位发现

3. **AI 深度洞察**：
   - 行业发展趋势分析
   - 岗位技能需求变化
   - 区域性需求差异
   - 求职建议生成

**注**：当前企业 1w 条就业数据尚未提供，市场趋势数据暂为空。待数据提供后，由 Dashboard-AI 服务完成分析。

---

## 接口列表

| 接口编号 | 接口名 | 方法 | 路径 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| 4.1 | 获取岗位画像列表 | GET | `/api/market/profiles` | 岗位画像列表（支持筛选） |
| 4.2 | 获取市场趋势 | GET | `/api/market/trends` | 薪资/需求/更新时间 |
| 4.3 | 获取 AI 深度洞察 | GET | `/api/market/insight` | AI 生成的市场洞察 |
| 4.4 | 获取热门岗位 | GET | `/api/market/hot-jobs` | 热门岗位画像列表 |
| 4.5 | 获取岗位详情 | GET | `/api/market/jobs/{job_id}` | 岗位画像详情 |

---

## 详细接口定义

### 4.1 获取岗位画像列表

- **请求方法**: `GET`
- **请求路径**: `/api/market/profiles`
- **鉴权**: 需要
- **Query 参数**:
  - `industry` (string, 可选) 所属行业分段
  - `city` (string, 可选) 工作城市
  - `keyword` (string, 可选) 关键词搜索
  - `page` (number, 可选，默认 1) 页码
  - `size` (number, 可选，默认 20) 每页数量
- **响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "total": 100,
    "page": 1,
    "size": 20,
    "items": [
      {
        "id": 1001,
        "job_name": "UI/视觉设计师",
        "industry_segment": "互联网/科技",
        "city": "深圳",
        "salary_range": {
          "min": 8000,
          "max": 15000,
          "currency": "CNY",
          "unit": "month"
        },
        "experience_range": {
          "min": 0,
          "max": 2,
          "unit": "years"
        },
        "core_skills": ["Figma", "Sketch", "Adobe Creative Suite"],
        "demand_level": "HIGH",
        "updated_at": "2026-03-30T09:00:00+08:00"
      },
      {
        "id": 1002,
        "job_name": "前端工程师",
        "industry_segment": "互联网/科技",
        "city": "深圳",
        "salary_range": {
          "min": 10000,
          "max": 20000,
          "currency": "CNY",
          "unit": "month"
        },
        "experience_range": {
          "min": 1,
          "max": 3,
          "unit": "years"
        },
        "core_skills": ["Vue", "React", "TypeScript"],
        "demand_level": "HIGH",
        "updated_at": "2026-03-30T09:00:00+08:00"
      }
    ]
  }
}
```

**注**：完整版本应包含不少于 10 个就业岗位画像。

---

### 4.2 获取市场趋势

- **请求方法**: `GET`
- **请求路径**: `/api/market/trends`
- **鉴权**: 需要
- **Query 参数**:
  - `job_profile_id` (BIGINT, 可选) 指定岗位 ID，不传则返回整体趋势
  - `city` (string, 可选) 指定城市
  - `time_range` (string, 可选) 时间范围：`quarter`/`half_year`/`year`（默认 quarter）
- **响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "job_profile_id": 1001,
    "job_name": "UI/视觉设计师",
    "city": "深圳",
    "time_range": "quarter",
    "salary": {
      "current": {
        "min": 8000,
        "max": 15000,
        "avg": 11500,
        "currency": "CNY",
        "unit": "month"
      },
      "previous": {
        "min": 7500,
        "max": 14000,
        "avg": 10750,
        "currency": "CNY",
        "unit": "month"
      },
      "yoy_growth": 0.07,
      "trend": "up"
    },
    "demand": {
      "level": "HIGH",
      "current_quarter": 1200,
      "previous_quarter": 1000,
      "growth_rate": 0.20,
      "trend": "up",
      "histogram": [800, 950, 1000, 1200]
    },
    "hot_skills": [
      { "skill": "Figma", "demand_count": 850, "growth": 0.25 },
      { "skill": "动效设计", "demand_count": 400, "growth": 0.30 },
      { "skill": "3D 设计", "demand_count": 300, "growth": 0.40 }
    ],
    "updated_at": "2026-03-30T09:00:00+08:00"
  }
}
```

---

### 4.3 获取 AI 深度洞察

- **请求方法**: `GET`
- **请求路径**: `/api/market/insight`
- **鉴权**: 需要
- **Query 参数**:
  - `job_profile_id` (BIGINT, 可选) 目标岗位，不传则返回整体洞察
  - `city` (string, 可选) 目标城市
- **响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "job_profile_id": 1001,
    "job_name": "UI/视觉设计师",
    "city": "深圳",
    "insight": {
      "title": "AI 深度洞察",
      "summary": "未来 6-12 个月 UI/视觉设计师岗位需求持续增长，尤其具备 AI 协作能力和动效设计技能的设计师更受市场青睐。",
      "market_signals": [
        { "label": "需求强度", "value": "HIGH", "trend": "up" },
        { "label": "竞争强度", "value": "MEDIUM", "trend": "stable" },
        { "label": "薪资增长", "value": "7%", "trend": "up" },
        { "label": "技能更新", "value": "FAST", "trend": "accelerating" }
      ],
      "industry_trends": [
        "AI 工具（如 Midjourney、Stable Diffusion）正在改变设计工作流，掌握 AI 协作能力的设计师效率提升明显",
        "动效设计和交互设计能力成为区分初级和中级设计师的关键技能",
        "3D 设计需求增长迅速，建议提前布局学习"
      ],
      "suggested_actions": [
        {
          "title": "学习 AI 设计工具",
          "desc": "掌握 Midjourney、Stable Diffusion 等 AI 工具，提升设计效率",
          "priority": "high"
        },
        {
          "title": "补齐动效设计能力",
          "desc": "学习 After Effects、Principle 等动效工具",
          "priority": "medium"
        },
        {
          "title": "关注 3D 设计趋势",
          "desc": "学习 Blender、C4D 等 3D 设计工具",
          "priority": "low"
        }
      ]
    },
    "updated_at": "2026-03-30T09:00:00+08:00"
  }
}
```

---

### 4.4 获取热门岗位

- **请求方法**: `GET`
- **请求路径**: `/api/market/hot-jobs`
- **鉴权**: 需要
- **Query 参数**:
  - `limit` (number, 可选，默认 10) 返回数量
  - `city` (string, 可选) 指定城市
  - `industry` (string, 可选) 指定行业
- **响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "items": [
      {
        "id": 1003,
        "job_name": "AI 算法工程师",
        "industry_segment": "人工智能",
        "city": "深圳",
        "tag": "高新潜力",
        "salary_range": {
          "min": 30000,
          "max": 60000,
          "currency": "CNY",
          "unit": "month"
        },
        "demand_level": "VERY_HIGH",
        "highlights": ["需求旺盛", "技术门槛高", "薪资增长快"],
        "core_skills": ["Python", "PyTorch", "数学基础"],
        "growth_rate": 0.35,
        "icon": "psychology"
      },
      {
        "id": 1002,
        "job_name": "前端工程师",
        "industry_segment": "互联网/科技",
        "city": "深圳",
        "tag": "稳定需求",
        "salary_range": {
          "min": 10000,
          "max": 20000,
          "currency": "CNY",
          "unit": "month"
        },
        "demand_level": "HIGH",
        "highlights": ["需求稳定", "技能迭代快"],
        "core_skills": ["Vue", "React", "TypeScript"],
        "growth_rate": 0.12,
        "icon": "code"
      },
      {
        "id": 1001,
        "job_name": "UI/视觉设计师",
        "industry_segment": "互联网/科技",
        "city": "深圳",
        "tag": "入门友好",
        "salary_range": {
          "min": 8000,
          "max": 15000,
          "currency": "CNY",
          "unit": "month"
        },
        "demand_level": "HIGH",
        "highlights": ["入门门槛较低", "作品集重要"],
        "core_skills": ["Figma", "Sketch", "Adobe Creative Suite"],
        "growth_rate": 0.15,
        "icon": "palette"
      }
    ],
    "updated_at": "2026-03-30T09:00:00+08:00"
  }
}
```

---

### 4.5 获取岗位详情

- **请求方法**: `GET`
- **请求路径**: `/api/market/jobs/{job_id}`
- **鉴权**: 需要
- **响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 1001,
    "job_name": "UI/视觉设计师",
    "industry_segment": "互联网/科技",
    "description": "负责公司产品的 UI/视觉设计工作，包括界面设计、图标设计、运营设计等",
    "cities": ["深圳", "上海", "北京", "广州"],
    "salary_range": {
      "min": 8000,
      "max": 15000,
      "avg": 11500,
      "currency": "CNY",
      "unit": "month"
    },
    "experience_range": {
      "min": 0,
      "max": 2,
      "unit": "years"
    },
    "education_requirement": "大专及以上",
    "core_skills": [
      { "name": "Figma", "proficiency_required": 4 },
      { "name": "Sketch", "proficiency_required": 3 },
      { "name": "Adobe Creative Suite", "proficiency_required": 4 },
      { "name": "设计理论", "proficiency_required": 3 }
    ],
    "capability_requirements": {
      "innovation": 80,
      "learning": 75,
      "communication": 80,
      "resilience": 70,
      "attention_to_detail": 90
    },
    "certificate_requirements": [
      "Adobe 认证专家（加分项）",
      "UI 设计相关证书（加分项）"
    ],
    "company_benefits": [
      "五险一金",
      "弹性工作",
      "年度体检",
      "培训补贴"
    ],
    "career_path": {
      "vertical": [
        "初级 UI 设计师 → 中级 UI 设计师 → 高级 UI 设计师 → 设计专家/设计经理"
      ],
      "lateral": [
        "UI 设计师 → 交互设计师",
        "UI 设计师 → 产品经理",
        "UI 设计师 → 前端工程师"
      ]
    },
    "demand_analysis": {
      "level": "HIGH",
      "growth_rate": 0.15,
      "trend": "stable"
    },
    "updated_at": "2026-03-30T09:00:00+08:00"
  }
}
```

---

## 数据流转说明

```
企业 1w 条就业数据导入
    ↓
Dashboard-AI 分析统计
    ↓
生成岗位画像数据（job_profiles + job_profile_skills + job_profile_capabilities）
计算市场趋势数据
    ↓
GET /api/market/profiles
GET /api/market/trends
GET /api/market/insight
GET /api/market/hot-jobs
    ↓
返回市场数据供前端展示
```

**注**：当前企业 1w 条就业数据尚未提供，市场数据暂为空。待数据提供后，由 Dashboard-AI 服务完成分析。

---

## 接口列表汇总

| 接口编号 | 接口名 | 方法 | 路径 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| 4.1 | 获取岗位画像列表 | GET | /api/market/profiles | 岗位画像列表（支持筛选） |
| 4.2 | 获取市场趋势 | GET | /api/market/trends | 薪资/需求/技能趋势 |
| 4.3 | 获取 AI 深度洞察 | GET | /api/market/insight | AI 生成的市场洞察和建议 |
| 4.4 | 获取热门岗位 | GET | /api/market/hot-jobs | 热门岗位画像列表 |
| 4.5 | 获取岗位详情 | GET | /api/market/jobs/{job_id} | 岗位画像详细信息 |

---

## 与 Dashboard 模块的关系

| 模块 | 职责 | 数据来源 |
|------|------|----------|
| **Dashboard** | 个人化仪表盘、人岗匹配、岗位画像管理 | `job_profiles` + 用户能力画像 |
| **Market** | 市场趋势、热门岗位、AI 洞察 | 企业数据统计 + AI 分析 |

- **Dashboard** 侧重个人匹配和岗位画像管理
- **Market** 侧重市场整体趋势和探索
