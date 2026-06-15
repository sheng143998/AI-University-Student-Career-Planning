# 接口文档 9：职业报告

Reports 模块由 Java 生成报告主体、PDF 和数据库记录；Python Reports RAG 提供 AI 建议、证据引用和检索诊断。

## 生成职业报告

| 项目 | 内容 |
| --- | --- |
| 方法 | `POST` |
| 路径 | `/api/reports/generate` |
| 鉴权 | 需要 |
| 请求体 | `ReportGenerateDTO`，可为空 |
| 响应 | `Result<ReportGenerateVO>` |

Java 会读取用户画像、简历分析和目标岗位信息，然后调用 Python `/api/v1/reports/generate-support` 补充 AI 建议和诊断。

## 获取最新报告

| 项目 | 内容 |
| --- | --- |
| 方法 | `GET` |
| 路径 | `/api/reports/latest` |
| 响应 | `Result<ReportSummaryVO>` |

## 获取报告详情

| 项目 | 内容 |
| --- | --- |
| 方法 | `GET` |
| 路径 | `/api/reports/{id}` |
| 响应 | `Result<ReportDetailVO>` |

## 更新报告

| 项目 | 内容 |
| --- | --- |
| 方法 | `PUT` |
| 路径 | `/api/reports/{id}` |
| 请求体 | `ReportUpdateDTO` |
| 响应 | `Result<Boolean>` |

## 下载报告 PDF

| 项目 | 内容 |
| --- | --- |
| 方法 | `GET` |
| 路径 | `/api/reports/{id}/download` |
| 响应 | `application/pdf` 字节流 |

## 删除报告

| 项目 | 内容 |
| --- | --- |
| 方法 | `DELETE` |
| 路径 | `/api/reports/{id}` |
| 响应 | `Result<Boolean>` |

## Python 内部接口

| 项目 | 内容 |
| --- | --- |
| 方法 | `POST` |
| 路径 | `/api/v1/reports/generate-support` |
| 调用方 | Java `PythonReportsAiClient` |

Python 不可用或空检索时，Java 返回空 AI 建议和诊断信息，不在 Java 内生成伪 AI 建议。

## 前端调用

- `website/src/api/reports.ts`
- `website/src/views/CareerReport.vue`
