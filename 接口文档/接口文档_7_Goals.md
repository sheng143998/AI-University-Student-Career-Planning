# 接口文档 7：目标管理

Goals 模块负责目标、里程碑、成功标准、长期愿景和目标 AI 建议。Java 提供对前端接口；AI 建议通过 Python Goals RAG 生成。

## 获取目标总览

| 项目 | 内容 |
| --- | --- |
| 方法 | `GET` |
| 路径 | `/api/goals/overview` |
| 鉴权 | 需要 |
| 响应 | `Result<GoalsOverviewVO>` |

## 创建目标

| 项目 | 内容 |
| --- | --- |
| 方法 | `POST` |
| 路径 | `/api/goals` |
| 请求体 | `GoalCreateDTO` |
| 响应 | `Result<IdVO>` |

## 获取目标详情

| 项目 | 内容 |
| --- | --- |
| 方法 | `GET` |
| 路径 | `/api/goals/{id}` |
| 响应 | `Result<GoalDetailVO>` |

## 生成目标 AI 建议

| 项目 | 内容 |
| --- | --- |
| 方法 | `POST` |
| 路径 | `/api/goals/{id}/ai-advice/generate` |
| 响应 | `Result<AiAdviceVO>` |

Java 会读取目标上下文并调用 Python `/internal/goals/advice`。Python 返回建议正文、证据引用和检索诊断。

## 更新目标

| 项目 | 内容 |
| --- | --- |
| 方法 | `PUT` |
| 路径 | `/api/goals/{id}` |
| 请求体 | `GoalUpdateDTO` |
| 响应 | `Result<Void>` |

## 删除目标

| 项目 | 内容 |
| --- | --- |
| 方法 | `DELETE` |
| 路径 | `/api/goals/{id}` |
| 响应 | `Result<Void>` |

## 创建里程碑

| 项目 | 内容 |
| --- | --- |
| 方法 | `POST` |
| 路径 | `/api/goals/{id}/milestones` |
| 请求体 | `GoalMilestoneCreateDTO` |
| 响应 | `Result<IdVO>` |

## 更新里程碑

| 项目 | 内容 |
| --- | --- |
| 方法 | `PATCH` |
| 路径 | `/api/goals/{id}/milestones/{msId}` |
| 请求体 | `GoalMilestoneUpdateDTO` |
| 响应 | `Result<Void>` |

## 状态值

目标和里程碑状态使用业务枚举字符串，例如 `TODO`、`IN_PROGRESS`、`DONE`。这是业务状态，不是文档待办清单。

## Python 内部接口

| 项目 | 内容 |
| --- | --- |
| 方法 | `POST` |
| 路径 | `/internal/goals/advice` |
| 调用方 | Java `PythonGoalsAdviceClient` |

## 前端调用

- `website/src/api/goals.ts`
- `website/src/views/Goals.vue`
