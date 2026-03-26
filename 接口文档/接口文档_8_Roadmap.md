# 职引AI - Roadmap 模块接口文档

## 模块概述
职业地图相关接口，包括行业分段、地图图谱、节点详情和节点搜索。

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
  - `mode` (string, 可选) `vertical` / `lateral`
  - `q` (string, 可选) 搜索关键字
- **响应示例**:
```json
{
  "code": 200,
  "data": {
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
      { "from": "n_001", "to": "n_002", "variant": "primary" }
    ]
  }
}
```

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
    "next_nodes": ["n_002"]
  }
}
```

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
      { "id": "n_002", "title": "中级前端工程师", "subtitle": "2-4 年", "tags": ["工程化"], "variant": "neutral" }
    ]
  }
}
```