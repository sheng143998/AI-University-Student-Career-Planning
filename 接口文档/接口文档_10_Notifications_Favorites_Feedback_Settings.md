# 职引AI - Notifications / Favorites / Feedback / Settings 模块接口文档

## 模块概述
系统通知、收藏、用户反馈和设置相关接口。

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
| 获取设置 | GET | `/api/settings` | 主题/通知/语言等 |
| 更新设置 | PUT | `/api/settings` | 保存 |

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