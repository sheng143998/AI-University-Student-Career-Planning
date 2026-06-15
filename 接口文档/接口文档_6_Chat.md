# 接口文档 6：AI 聊天

Chat 模块由 Java 提供会话、消息、附件和流式响应接口。Java 负责鉴权、会话归属校验和消息落库；Python Chat RAG 负责生成回复和每日建议。

## 创建会话

| 项目 | 内容 |
| --- | --- |
| 方法 | `POST` |
| 路径 | `/api/chat/conversations` |
| 鉴权 | 需要 |
| 请求体 | `ChatCreateConversationDTO` |
| 响应 | `Result<ChatConversationVO>` |

## 获取会话列表

| 项目 | 内容 |
| --- | --- |
| 方法 | `GET` |
| 路径 | `/api/chat/conversations` |
| 参数 | `cursor`、`limit` |
| 响应 | `Result<List<ChatConversationVO>>` |

## 获取会话消息

| 项目 | 内容 |
| --- | --- |
| 方法 | `GET` |
| 路径 | `/api/chat/conversations/{conversationId}/messages` |
| 参数 | `cursor`、`limit` |
| 响应 | `Result<List<ChatMessageVO>>` |

## 发送消息

| 项目 | 内容 |
| --- | --- |
| 方法 | `POST` |
| 路径 | `/api/chat/messages` |
| 鉴权 | 需要 |
| 请求体 | `ChatSendMessageDTO`，包含 `conversationId`、`content`、可选 `resumeId` |
| 响应 | `text/html;charset=utf-8` 流式文本 |

Java 会先保存用户消息，再调用 Python `/api/v1/chat/complete` 获取回复，并以文本流返回给前端。

## 删除会话

| 项目 | 内容 |
| --- | --- |
| 方法 | `DELETE` |
| 路径 | `/api/chat/conversations/{conversationId}` |
| 响应 | `Result<Void>` |

## 每日建议

| 项目 | 内容 |
| --- | --- |
| 方法 | `GET` |
| 路径 | `/api/chat/daily-suggestions` |
| 参数 | `resumeId` 可选 |
| 响应 | `Result<ChatDailySuggestionsVO>` |

## 上传附件

| 项目 | 内容 |
| --- | --- |
| 方法 | `POST` |
| 路径 | `/api/chat/attachments` |
| 请求类型 | `multipart/form-data`，字段名 `file` |
| 响应 | `Result<String>` |

## 语音转文字

| 项目 | 内容 |
| --- | --- |
| 方法 | `POST` |
| 路径 | `/api/chat/voice` |
| 请求类型 | `multipart/form-data`，字段名 `file` |
| 响应 | `Result<String>` |

## 调试入口

| 项目 | 内容 |
| --- | --- |
| 方法 | `GET` |
| 路径 | `/ai/chat` |
| 开关 | `fuchuang.ai.python.debug-chat-endpoint-enabled=false` 默认关闭 |

该入口只用于本地调试，生产环境不应开启。

## Python 内部接口

| 路径 | 用途 |
| --- | --- |
| `POST /api/v1/chat/complete` | 生成聊天回复 |
| `POST /api/v1/chat/daily-suggestions` | 生成每日建议 |

## 前端调用

- `website/src/api/chat.ts`
- `website/src/views/Chat.vue`
