# 接口文档 5：简历

简历模块由 Java 负责文件接收、OSS、PDF 渲染、数据库落库和统一响应；Python 负责简历分析和图片型 PDF OCR。

## 上传简历

| 项目 | 内容 |
| --- | --- |
| 方法 | `POST` |
| 路径 | `/api/resume/upload` |
| 鉴权 | 需要 |
| 请求类型 | `multipart/form-data`，字段名 `file` |
| 响应 | `Result<ResumeUploadVO>` |

支持 PDF、DOCX、PPTX、HTML、TXT 等文件。上传后 Java 保存文件并异步触发解析；前端通过分析查询接口轮询状态。

## 获取简历分析结果

| 项目 | 内容 |
| --- | --- |
| 方法 | `GET` |
| 路径 | `/api/resume/analysis/{id}` |
| 鉴权 | 需要 |
| 响应 | `Result<ResumeAnalysisResultVO>` |

`id` 为向量存储记录 ID 或简历分析关联 ID。状态字段包括 `pending`、`processing`、`completed`、`failed`、`stopped`。

## 获取简历分析列表

| 项目 | 内容 |
| --- | --- |
| 方法 | `GET` |
| 路径 | `/api/resume/analysis` |
| 参数 | `cursor`、`limit` |
| 响应 | `Result<List<ResumeAnalysisResultVO>>` |

## 预览简历文件

| 项目 | 内容 |
| --- | --- |
| 方法 | `GET` |
| 路径 | `/api/resume/analysis/{id}/preview` |
| 参数 | `disposition=inline|attachment` |
| 响应 | 文件字节流 |

## 获取预览 URL

| 项目 | 内容 |
| --- | --- |
| 方法 | `GET` |
| 路径 | `/api/resume/analysis/{id}/preview-url` |
| 响应 | `Result<String>` |

## 获取能力画像

| 项目 | 内容 |
| --- | --- |
| 方法 | `GET` |
| 路径 | `/api/resume/capability-profile` |
| 响应 | `Result<CapabilityProfileVO>` |

## Python 服务接口

| 路径 | 端口 | 用途 |
| --- | --- | --- |
| `POST /api/v1/resume/analyze` | `8091` | 简历结构化分析 |
| `POST /internal/resume/ocr` | `8090` 或 `8091` | 图片型 PDF 页面 OCR |

OCR 真实模型调用需要配置模型 API key；本地测试可使用 `FUCHUANG_RESUME_OCR_MOCK_TEXT`。

## 前端调用

- `website/src/api/resume.ts`
- `website/src/views/ResumeAnalysis.vue`
