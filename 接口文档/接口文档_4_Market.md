# 职引AI - Market 模块接口文档

## 模块概述
市场探索相关接口，包括岗位关联图谱、市场趋势、AI 深度洞察、热门岗位画像和岗位详情。

---

## 接口列表

| 接口名 | 方法 | 路径 | 说明 |
| :--- | :--- | :--- | :--- |
| 获取岗位关联图谱 | GET | `/api/market/graph` | 节点+连线 |
| 获取市场趋势 | GET | `/api/market/trends` | 薪资/需求/更新时间 |
| 获取 AI 深度洞察 | GET | `/api/market/insight` | 对应 Market 页面洞察卡片 |
| 获取热门岗位画像 | GET | `/api/market/hot-jobs` | 对应热门画像列表 |
| 获取岗位详情 | GET | `/api/market/jobs/{job_id}` | 画像展开/详情页（预留） |

---

## 详细接口定义

### 获取岗位关联图谱
- **请求方法**: `GET`
- **请求路径**: `/api/market/graph`
- **鉴权**: 需要
- **Query 参数**:
  - `category` (string, 可选)
- **响应示例**:
```json
{
  "code": 200,
  "data": {
    "nodes": [
      { "id": "ui", "label": "UI Designer", "type": "center", "icon": "palette" }
    ],
    "links": [
      { "source": "ui", "target": "fe", "type": "dashed", "weight": 0.5 }
    ]
  }
}
```

---

### 获取市场趋势
- **请求方法**: `GET`
- **请求路径**: `/api/market/trends`
- **鉴权**: 需要
- **Query 参数**:
  - `role` (string, 可选)
  - `city` (string, 可选)
- **响应示例**:
```json
{
  "code": 200,
  "data": {
    "salary": { "currency": "CNY", "unit": "month", "min": 25000, "max": 45000, "yoy": 0.125 },
    "demand": { "level": "HIGH", "histogram": [0.4, 0.6, 0.8] },
    "updated_at": "2026-03-26T09:00:00+08:00"
  }
}
```

---

### 获取 AI 深度洞察
- **请求方法**: `GET`
- **请求路径**: `/api/market/insight`
- **鉴权**: 需要
- **Query 参数**:
  - `role` (string, 可选) 目标岗位
  - `city` (string, 可选)
- **响应示例**:
```json
{
  "code": 200,
  "data": {
    "title": "AI 深度洞察",
    "summary": "未来 6-12 个月该岗位更偏向工程化与 AI 协作能力",
    "signals": [
      { "label": "需求强度", "value": "HIGH" },
      { "label": "竞争强度", "value": "MEDIUM" }
    ],
    "suggested_actions": [
      { "title": "补齐工程化体系", "desc": "CI/CD、Monorepo、性能预算" }
    ],
    "updated_at": "2026-03-26T09:00:00+08:00"
  }
}
```

---

### 获取热门岗位画像
- **请求方法**: `GET`
- **请求路径**: `/api/market/hot-jobs`
- **鉴权**: 需要
- **Query 参数**:
  - `limit` (number, 可选, 默认 6)
- **响应示例**:
```json
{
  "code": 200,
  "data": {
    "items": [
      {
        "id": "job_001",
        "job_id": "job_001",
        "title": "AI 算法工程师",
        "tag": "高新潜力",
        "salary": { "min": 30000, "max": 60000, "unit": "month", "currency": "CNY" },
        "highlights": ["需求旺盛", "技术门槛高"],
        "icon": "psychology"
      }
    ]
  }
}
```

---

### 获取岗位详情
- **请求方法**: `GET`
- **请求路径**: `/api/market/jobs/{job_id}`
- **鉴权**: 需要
- **响应示例**:
```json
{
  "code": 200,
  "data": {
    "job_id": "job_001",
    "title": "AI 算法工程师",
    "tag": "高新潜力",
    "salary": { "min": 30000, "max": 60000, "unit": "month", "currency": "CNY" },
    "cities": ["深圳", "上海"],
    "core_skills": ["Python", "PyTorch", "数学基础"],
    "description": "...",
    "updated_at": "2026-03-26T09:00:00+08:00"
  }
}
```