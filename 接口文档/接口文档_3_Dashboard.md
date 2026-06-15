# 接口文档 3：Dashboard

Dashboard 由 Java 后端对前端提供接口。Java 聚合用户画像、简历、岗位和职业数据；需要目标岗位匹配时调用 Python Dashboard RAG。

## 获取 Dashboard 汇总

| 项目 | 内容 |
| --- | --- |
| 方法 | `GET` |
| 路径 | `/api/dashboard/summary` |
| 鉴权 | 需要 |
| 响应 | `Result<Map<String,Object>>` |

返回内容包含岗位画像、匹配摘要、市场趋势、技能雷达和行动建议。

## 获取职业发展路径

| 项目 | 内容 |
| --- | --- |
| 方法 | `GET` |
| 路径 | `/api/dashboard/roadmap` |
| 鉴权 | 需要 |
| 响应 | `Result<Map<String,Object>>` |

如果用户还没有职业数据，Java 会尝试读取简历和岗位库，并调用 Python `/internal/dashboard/target-job/match` 匹配目标岗位。

## 更新当前阶段

| 项目 | 内容 |
| --- | --- |
| 方法 | `PUT` |
| 路径 | `/api/dashboard/roadmap/current-step` |
| 鉴权 | 需要 |
| 请求体 | `{ "current_step_index": 1 }` |
| 响应 | `Result<List<Map<String,Object>>>` |

## Python 内部接口

| 项目 | 内容 |
| --- | --- |
| 方法 | `POST` |
| 路径 | `/internal/dashboard/target-job/match` |
| 调用方 | Java `PythonDashboardAiClient` |
| 响应 | 目标岗位匹配结果、证据引用和检索诊断 |

Python 侧当前为确定性 RAG 降级实现，包含摘要索引、元数据过滤、多查询扩展、BM25/哈希向量风格召回、RRF 融合和确定性重排。

## 前端调用

- `website/src/api/dashboard.ts`
- `website/src/views/CareerDashboard.vue`
