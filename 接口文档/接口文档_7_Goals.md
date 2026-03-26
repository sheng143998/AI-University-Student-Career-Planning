# 职引AI - Goals 模块接口文档

## 模块概述
目标管理相关接口，包括主目标、里程碑、成功准则和并行目标的管理。

---

## 接口列表

| 接口名 | 方法 | 路径 | 说明 |
| :--- | :--- | :--- | :--- |
| 获取目标列表 | GET | `/api/goals` | 含主目标/并行目标 |
| 创建目标 | POST | `/api/goals` | 新增目标 |
| 获取目标详情 | GET | `/api/goals/{id}` | 含里程碑/成功准则/AI 建议 |
| 更新目标 | PUT | `/api/goals/{id}` | 更新字段 |
| 删除目标 | DELETE | `/api/goals/{id}` | 删除 |
| 创建里程碑 | POST | `/api/goals/{id}/milestones` | 里程碑 |
| 更新里程碑 | PATCH | `/api/goals/{id}/milestones/{ms_id}` | 勾选/改名/排序 |

---

## 详细接口定义

### 获取目标列表
- **请求方法**: `GET`
- **请求路径**: `/api/goals`
- **鉴权**: 需要
- **响应示例**:
```json
{
  "code": 200,
  "data": {
    "primary": { "id": "g_001", "title": "成为高级前端工程师", "progress": 65, "status": "IN_PROGRESS" },
    "others": [
      { "id": "g_002", "title": "补齐算法基础", "progress": 20, "status": "IN_PROGRESS" }
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
{ "title": "完成 React 认证", "desc": "通过官方认证", "status": "TODO" }
```
- **响应示例**:
```json
{ "code": 200, "data": { "id": "g_003" } }
```

---

### 获取目标详情
- **请求方法**: `GET`
- **请求路径**: `/api/goals/{id}`
- **鉴权**: 需要
- **响应示例**:
```json
{
  "code": 200,
  "data": {
    "id": "g_001",
    "title": "成为高级前端工程师",
    "desc": "通过工程化与性能体系建设拿到更高级别岗位",
    "status": "IN_PROGRESS",
    "progress": 65,
    "success_criteria": [
      "能独立完成中大型项目架构设计",
      "拥有可量化的性能优化与工程化案例"
    ],
    "milestones": [
      { "id": "ms_001", "title": "完善项目作品集", "status": "DONE" },
      { "id": "ms_002", "title": "系统学习性能优化", "status": "IN_PROGRESS" }
    ],
    "ai_advice": {
      "title": "AI 建议",
      "content": "建议将目标拆成 4 周冲刺：工程化、性能、项目表达、面试题体系。"
    }
  }
}
```

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
  "progress": 70
}
```
- **响应示例**:
```json
{ "code": 200, "data": { "updated": true } }
```

---

### 删除目标
- **请求方法**: `DELETE`
- **请求路径**: `/api/goals/{id}`
- **鉴权**: 需要
- **响应示例**:
```json
{ "code": 200, "data": { "deleted": true } }
```

---

### 创建里程碑
- **请求方法**: `POST`
- **请求路径**: `/api/goals/{id}/milestones`
- **鉴权**: 需要
- **请求体**:
```json
{ "title": "完成 1 次性能优化复盘", "status": "TODO" }
```
- **响应示例**:
```json
{ "code": 200, "data": { "id": "ms_010" } }
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
  "status": "DONE",
  "order": 2
}
```
- **响应示例**:
```json
{ "code": 200, "data": { "updated": true } }
```