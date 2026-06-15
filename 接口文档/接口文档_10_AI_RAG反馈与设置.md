# 接口文档 10：AI/RAG 反馈与设置

当前代码中本接口文档只保留已经实现的 AI/RAG 反馈与个性化设置接口。通知、收藏等历史标题不再作为当前接口内容记录。

## 提交 AI/RAG 反馈

| 项目 | 内容 |
| --- | --- |
| 方法 | `POST` |
| 路径 | `/api/feedback/ai-rag` |
| 鉴权 | 需要 |
| 请求体 | `AiRagFeedbackDTO` |
| 响应 | `Result<AiRagFeedbackVO>` |

Java 负责校验目标归属和业务字段，然后调用 Python `/internal/rag/feedback`。Python 将反馈写入评估队列，供后续质量分析使用。

## 获取 AI/RAG 个性化设置

| 项目 | 内容 |
| --- | --- |
| 方法 | `GET` |
| 路径 | `/api/settings/ai-rag` |
| 鉴权 | 需要 |
| 响应 | `Result<AiRagSettingsVO>` |

## 更新 AI/RAG 个性化设置

| 项目 | 内容 |
| --- | --- |
| 方法 | `PUT` |
| 路径 | `/api/settings/ai-rag` |
| 鉴权 | 需要 |
| 请求体 | `AiRagSettingsDTO` |
| 响应 | `Result<AiRagSettingsVO>` |

Java 保存设置前会调用 Python `/internal/rag/preferences/validate` 校验偏好并生成有效元数据过滤条件。

## Python 内部接口

| 路径 | 用途 |
| --- | --- |
| `POST /internal/rag/feedback` | 接收 AI/RAG 反馈并写入反馈队列 |
| `POST /internal/rag/preferences/validate` | 校验个性化设置并返回有效过滤条件 |

## 前端调用

- `website/src/api/aiRagFeedback.ts`
- `website/src/views/Settings.vue`

## 安全要求

- 反馈目标必须属于当前登录用户。
- Python 队列不得记录 JWT、OSS 签名 URL、API key 或原始敏感简历正文。
- 前端展示诊断信息或证据引用时必须保持脱敏。
