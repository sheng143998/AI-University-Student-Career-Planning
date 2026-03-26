# 职引AI - Chat 模块接口文档

## 模块概述
AI 导师对话相关接口，包括会话管理、消息收发和每日建议。

---

## 接口列表

| 接口名 | 方法 | 路径 | 说明 |
| :--- | :--- | :--- | :--- |
| 获取会话列表 | GET | `/api/chat/conversations` | 左侧会话栏 |
| 新建会话 | POST | `/api/chat/conversations` | "新对话" |
| 重命名会话 | PATCH | `/api/chat/conversations/{id}` | 修改 title |
| 删除会话 | DELETE | `/api/chat/conversations/{id}` | 删除会话 |
| 获取会话消息 | GET | `/api/chat/conversations/{id}/messages` | 分页拉取 |
| 发送消息 | POST | `/api/chat/conversations/{id}/messages` | 发送并返回 AI 回复与建议问题 |
| 获取每日建议 | GET | `/api/chat/daily-suggestions` | 右侧"今日建议" |

---

## 详细接口定义

### 获取会话列表
- **请求方法**: `GET`
- **请求路径**: `/api/chat/conversations`
- **鉴权**: 需要
- **Query 参数**:
  - `cursor` (string, 可选)
  - `limit` (number, 可选, 默认 20)
- **响应示例**:
```json
{
  "code": 200,
  "data": {
    "items": [
      { "id": "c_001", "title": "简历优化建议", "last_message_at": "2026-03-26T09:10:00+08:00" }
    ],
    "next_cursor": null
  }
}
```

---

### 新建会话
- **请求方法**: `POST`
- **请求路径**: `/api/chat/conversations`
- **鉴权**: 需要
- **请求体**:
```json
{ "title": "新对话" }
```
- **响应示例**:
```json
{ "code": 200, "data": { "id": "c_002", "title": "新对话" } }
```

---

### 重命名会话
- **请求方法**: `PATCH`
- **请求路径**: `/api/chat/conversations/{id}`
- **鉴权**: 需要
- **请求体**:
```json
{ "title": "简历优化建议（第 2 版）" }
```
- **响应示例**:
```json
{ "code": 200, "data": { "updated": true } }
```

---

### 删除会话
- **请求方法**: `DELETE`
- **请求路径**: `/api/chat/conversations/{id}`
- **鉴权**: 需要
- **响应示例**:
```json
{ "code": 200, "data": { "deleted": true } }
```

---

### 获取会话消息（分页）
- **请求方法**: `GET`
- **请求路径**: `/api/chat/conversations/{id}/messages`
- **鉴权**: 需要
- **Query 参数**:
  - `cursor` (string, 可选)
  - `limit` (number, 可选, 默认 20)
- **响应示例**:
```json
{
  "code": 200,
  "data": {
    "items": [
      { "id": "m_001", "conversation_id": "c_001", "role": "assistant", "content": "你好...", "created_at": "2026-03-26T09:10:00+08:00" }
    ],
    "next_cursor": null
  }
}
```

---

### 发送消息（按会话）
- **请求方法**: `POST`
- **请求路径**: `/api/chat/conversations/{id}/messages`
- **鉴权**: 需要
- **Path 参数**:
  - `id` (string) 会话 ID
- **请求体**:
```json
{
  "content": "我想了解一下前端开发的未来趋势。"
}
```
- **响应示例**:
```json
{
  "code": 200,
  "data": {
    "user_message": {
      "id": "m_100",
      "role": "user",
      "content": "我想了解一下前端开发的未来趋势。",
      "created_at": "2026-03-26T09:12:00+08:00"
    },
    "assistant_message": {
      "id": "m_101",
      "role": "assistant",
      "content": "前端开发正朝着 AI 集成、工程化与体验融合方向发展...",
      "created_at": "2026-03-26T09:12:02+08:00",
      "suggestions": [
        { "title": "追问", "text": "如何学习 AI 集成？" },
        { "title": "追问", "text": "目前流行的低代码工具有哪些？" }
      ],
      "tip": "提示：把目标岗位 JD 发我，我可以按要求对齐你的经历表述。"
    },
    "suggestion_questions": ["如何学习 AI 集成？", "目前流行的低代码工具有哪些？"]
  }
}
```

---

### 获取每日建议
- **请求方法**: `GET`
- **请求路径**: `/api/chat/daily-suggestions`
- **鉴权**: 需要
- **响应示例**:
```json
{
  "code": 200,
  "data": {
    "items": [
      { "title": "今日建议", "content": "用 STAR 法则重写 1 条项目经历" }
    ]
  }
}
```