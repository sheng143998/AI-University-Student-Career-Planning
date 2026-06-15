# 接口文档 4：市场探索与岗位画像

Market 模块由 Java 后端对前端提供 `/api/market/**` 接口。市场洞察、软技能生成、岗位分类、岗位索引和岗位检索中的 AI 能力通过 Python 聚合服务完成。

## 岗位画像列表

| 项目 | 内容 |
| --- | --- |
| 方法 | `GET` |
| 路径 | `/api/market/profiles` |
| 参数 | `industry`、`city`、`keyword`、`page`、`size` |
| 响应 | `Result<MarketProfileListVO>` |

## 市场趋势

| 项目 | 内容 |
| --- | --- |
| 方法 | `GET` |
| 路径 | `/api/market/trends` |
| 参数 | `job_profile_id`、`city`、`time_range` |
| 响应 | `Result<MarketTrendsVO>` |

## AI 市场洞察

| 项目 | 内容 |
| --- | --- |
| 方法 | `GET` |
| 路径 | `/api/market/insight` |
| 参数 | `job_profile_id`、`city` |
| 响应 | `Result<MarketInsightVO>` |

Java 调用 Python `/api/v1/market/insight` 生成洞察内容。Python 不可用时 Java 返回空洞察状态，不再在 Java 内生成静态 AI 文案。

## 热门岗位

| 项目 | 内容 |
| --- | --- |
| 方法 | `GET` |
| 路径 | `/api/market/hot-jobs` |
| 参数 | `limit`、`city`、`industry` |
| 响应 | `Result<MarketHotJobsVO>` |

## 岗位详情

| 项目 | 内容 |
| --- | --- |
| 方法 | `GET` |
| 路径 | `/api/market/jobs/{job_id}` |
| 响应 | `Result<MarketJobDetailVO>` |

## 生成岗位画像

| 项目 | 内容 |
| --- | --- |
| 方法 | `POST` |
| 路径 | `/api/market/jobs/{job_id}/generate` |
| 响应 | `Result<MarketJobDetailVO>` |

## 批量生成岗位画像

| 项目 | 内容 |
| --- | --- |
| 方法 | `POST` |
| 路径 | `/api/market/jobs/generate-all` |
| 响应 | `Result<Integer>` |

## 岗位数据导入与维护

这些接口由 `JobImportController` 提供，属于岗位数据维护接口，通常由管理员或运维任务调用。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/job/import` | 上传 Excel 导入岗位数据，请求类型为 `multipart/form-data`，字段名 `file` |
| `POST` | `/api/job/classify-and-import` | 对招聘数据做 AI 分类后写入岗位表 |
| `POST` | `/api/job/consolidate-category-codes` | 整合岗位类别编码 |
| `POST` | `/api/job/generate-profiles` | 为岗位生成职业画像 |

## Python 内部接口

| 路径 | 用途 |
| --- | --- |
| `POST /api/v1/market/insight` | 生成岗位市场洞察 |
| `POST /api/v1/market/soft-skills` | 生成软技能描述和证据 |
| `POST /internal/market/jobs/classify` | 对招聘 JD 或岗位样本分类 |
| `POST /internal/market/jobs/index` | 生成岗位索引记录、metadata 和 embedding 字符串 |
| `POST /internal/market/jobs/search` | 对 Java 提供的候选岗位做混合检索和排序 |

## 前端调用

- `website/src/api/market.ts`
- `website/src/views/MarketExploration.vue`
