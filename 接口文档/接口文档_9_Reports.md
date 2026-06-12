# 职引 AI - Reports 模块接口文档

## 变更摘要

- 日期：2026-06-12
- 本轮范围：Reports 生成链路的 AI 建议与证据支撑迁移到 Python Reports-RAG 服务。
- Java 对外接口保持现有 `/api/reports/**` 路径和 `Result<T>` 包装：`code=1` 表示成功，`code=0` 表示失败。
- Java 内部新增对 Python 的 `POST /api/v1/reports/generate-support` 调用，用于生成 `aiSuggestions`、`evidenceRefs`、`ragDiagnostics`。
- 本轮 Python 实现是 deterministic/lightweight RAG fallback：递归切分、摘要索引、元数据过滤、Multi-Query、BM25 + hash embedding-like hybrid、RRF/rerank fallback。它不声明真实 pgvector、生产 embedding、LLM 或 cross-encoder 已完成。
- 旧目录 `ai_service/` 不属于本轮实现范围；Reports 新服务放在短横线目录 `ai-service/`。

## 认证与边界

- 外部接口由 Java Spring Boot 暴露，仍受 JWT 拦截器保护。
- Python Reports-RAG 只作为 Java 内部下游服务，不直接向前端开放。
- Java 负责用户鉴权、报告状态、数据库落库、PDF 生成和下游失败 fallback。
- Python 只返回 AI 建议、证据引用和检索诊断，不落库、不处理 JWT、不访问 OSS。

## 当前支持的 Java 外部接口

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/reports/generate` | 需要 | 创建 `PROCESSING` 报告并触发内容生成 |
| GET | `/api/reports/latest` | 需要 | 获取当前用户最新报告 |
| GET | `/api/reports/{id}` | 需要 | 获取报告详情 |
| PUT | `/api/reports/{id}` | 需要 | 编辑可编辑报告的部分内容 |
| GET | `/api/reports/{id}/download` | 需要 | 下载 PDF |
| DELETE | `/api/reports/{id}` | 需要 | 删除报告 |

> `GET /api/reports` 和 `POST /api/reports/{id}/regenerate` 当前 Controller 未实现，本轮不纳入最小闭环；前端不得依赖这两个路径。

## 9.1 生成职业报告

- 方法：`POST`
- 路径：`/api/reports/generate`
- 认证：需要
- 请求体：可为空。

```json
{
  "targetJobProfileId": 1001,
  "careerPreference": {
    "preferredCity": "深圳",
    "expectedSalary": "15-25k",
    "careerDirection": "技术路线"
  }
}
```

成功响应：

```json
{
  "code": 1,
  "msg": null,
  "data": {
    "reportId": 12,
    "reportNo": "CR202606120001",
    "status": "PROCESSING",
    "estimatedTime": 60
  }
}
```

失败响应示例：

```json
{
  "code": 0,
  "msg": "用户未登录",
  "data": null
}
```

生命周期：

- Java 先创建 `PROCESSING` 报告记录。
- 内容生成完成后状态更新为 `COMPLETED`。
- 能力画像或职业数据等前置数据缺失时状态更新为 `FAILED`。
- Python 下游 400、5xx、超时、不可用、无效 JSON 或空检索不应导致整份报告失败；Java 使用 deterministic fallback 生成建议，并在 `ragDiagnostics` 中标记 `FALLBACK` 原因。

## 9.2 获取最新报告

- 方法：`GET`
- 路径：`/api/reports/latest`
- 认证：需要

成功响应：

```json
{
  "code": 1,
  "msg": null,
  "data": {
    "id": 12,
    "reportNo": "CR202606120001",
    "title": "职业生涯发展报告",
    "status": "COMPLETED",
    "matchScore": 85,
    "targetJob": "Java 后端开发工程师",
    "generatedAt": "2026-06-12T10:30:00",
    "updatedAt": "2026-06-12T10:32:00",
    "editable": true
  }
}
```

## 9.3 获取报告详情

- 方法：`GET`
- 路径：`/api/reports/{id}`
- 认证：需要，只能访问当前用户自己的报告。

成功响应：

```json
{
  "code": 1,
  "msg": null,
  "data": {
    "id": 12,
    "reportNo": "CR202606120001",
    "title": "职业生涯发展报告",
    "status": "COMPLETED",
    "userId": 1001,
    "targetJob": {
      "id": null,
      "name": "Java 后端开发工程师",
      "industry": null,
      "city": null
    },
    "matchScore": 85,
    "matchDetails": {
      "overall": 85,
      "basic_requirements": 90,
      "professional_skills": 82,
      "professional_quality": 80,
      "development_potential": 85,
      "evidence_refs": [
        {
          "id": "resume:1001:summary:0",
          "sourceType": "resume_analysis",
          "title": "简历分析摘要",
          "score": 0.91,
          "snippet": "项目经历和 Java/Spring 能力与目标岗位相关",
          "metadata": {
            "documentType": "resume_analysis",
            "documentId": 44,
            "reportId": 12,
            "section": "summary",
            "chunkIndex": -1
          }
        }
      ],
      "rag_diagnostics": {
        "status": "OK",
        "retrievalMode": "deterministic_fallback",
        "expandedQueryCount": 4,
        "candidateCount": 8,
        "selectedEvidenceCount": 3,
        "fallbackReason": null
      }
    },
    "sections": [],
    "aiSuggestions": "优先补强项目中的高并发、数据库优化和可观测性经验，并将简历中的后端项目按业务结果量化。",
    "evidenceRefs": [
      {
        "id": "resume:1001:summary:0",
        "sourceType": "resume_analysis",
        "title": "简历分析摘要",
        "score": 0.91,
        "snippet": "项目经历和 Java/Spring 能力与目标岗位相关",
        "metadata": {
          "documentType": "resume_analysis",
          "documentId": 44,
          "reportId": 12,
          "section": "summary",
          "chunkIndex": -1
        }
      }
    ],
    "ragDiagnostics": {
      "status": "OK",
      "retrievalMode": "deterministic_fallback",
      "expandedQueryCount": 4,
      "candidateCount": 8,
      "selectedEvidenceCount": 3
    },
    "editable": true,
    "generatedAt": "2026-06-12T10:30:00",
    "updatedAt": "2026-06-12T10:32:00"
  }
}
```

错误映射：

```json
{
  "code": 0,
  "msg": "报告不存在: 12",
  "data": null
}
```

```json
{
  "code": 0,
  "msg": "无权访问该报告",
  "data": null
}
```

## 9.4 更新报告

- 方法：`PUT`
- 路径：`/api/reports/{id}`
- 认证：需要。
- 限制：仅允许更新可编辑报告的 `careerGoal`、`actionPlan`、`targetJob`、`developmentPath` 等前端编辑内容；AI 检索证据和诊断不由前端写入。

成功响应：

```json
{
  "code": 1,
  "msg": null,
  "data": true
}
```

## 9.5 下载 PDF

- 方法：`GET`
- 路径：`/api/reports/{id}/download`
- 认证：需要。
- 成功：返回 `application/pdf` 二进制流。
- 若报告未完成，Java 返回失败响应或业务异常。

## 9.6 删除报告

- 方法：`DELETE`
- 路径：`/api/reports/{id}`
- 认证：需要。

成功响应：

```json
{
  "code": 1,
  "msg": null,
  "data": true
}
```

## Java -> Python 内部契约

### POST /api/v1/reports/generate-support

用途：Java 在报告内容生成过程中调用 Python Reports-RAG，获取 AI 建议、证据引用和检索诊断。

调用规则：

- Base URL：默认复用 `fuchuang.ai.python.base-url`，可通过 `FUCHUANG_AI_PYTHON_REPORTS_BASE_URL` 覆盖。
- Timeout：默认 8 秒，可通过 `FUCHUANG_AI_PYTHON_REPORTS_TIMEOUT_SECONDS` 覆盖。
- Retry：无自动重试，避免异步报告生成重复阻塞。
- Idempotency：`reportId + userId` 作为幂等键；Python 不落库，因此重复请求应返回等价 deterministic 结果。
- Content-Type：`application/json`。

请求体：

```json
{
  "reportId": 12,
  "userId": 1001,
  "targetJobName": "Java 后端开发工程师",
  "capabilityProfile": {
    "id": 7,
    "overallScore": 85,
    "completenessScore": 90,
    "competitivenessScore": 80,
    "capabilityScores": {"java": 88, "database": 82},
    "professionalSkills": ["Java", "Spring Boot", "PostgreSQL"],
    "softSkills": ["沟通", "学习能力"],
    "aiEvaluation": "后端基础扎实，项目经验需要进一步量化。"
  },
  "careerData": {
    "targetJob": "Java 后端开发工程师",
    "targetJobId": 101,
    "jobProfile": {"requirements": ["Spring Boot", "数据库优化", "接口设计"]},
    "matchSummary": {"overall": 85},
    "actions": []
  },
  "resumeAnalysis": {
    "id": 44,
    "parsedData": {"projects": ["校园招聘系统"]},
    "scores": {"backend": 86},
    "highlights": ["Spring Boot 项目经验"],
    "suggestions": ["补充性能优化指标"]
  },
  "matchDetails": {
    "overall": 85,
    "basic_requirements": 90,
    "professional_skills": 82,
    "professional_quality": 80,
    "development_potential": 85
  },
  "actionPlan": {
    "short_term_plan": {"goals": []}
  },
  "developmentPath": {
    "steps": []
  },
  "metadataFilters": {
    "userId": 1001,
    "visibility": "private",
    "documentTypes": [
      "resume_analysis",
      "career_data",
      "capability_profile",
      "match_details",
      "action_plan",
      "development_path"
    ]
  }
}
```

字段来源：

| 字段 | Java 来源 |
| --- | --- |
| `reportId` | `career_reports.id` |
| `userId` | `BaseContext` / service 入参 |
| `targetJobName` | `user_career_data.target_job` 或 `jobProfile.target_job` |
| `capabilityProfile` | `student_capability_profile` |
| `careerData` | `user_career_data` |
| `resumeAnalysis` | 最新一条 `resume_analysis_result` |
| `matchDetails` | Java deterministic 匹配分数 |
| `actionPlan` | Java 生成的行动计划 JSON |
| `developmentPath` | Java 生成的发展路径 JSON |

成功响应：

```json
{
  "status": "OK",
  "aiSuggestions": "优先补强项目中的高并发、数据库优化和可观测性经验，并将简历中的后端项目按业务结果量化。",
  "evidenceRefs": [
    {
      "id": "resume_analysis:44:chunk:0",
      "sourceType": "resume_analysis",
      "title": "简历分析摘要",
      "score": 0.91,
      "snippet": "Spring Boot 项目经验与目标岗位要求匹配",
      "metadata": {
        "documentType": "resume_analysis",
        "documentId": 44,
        "reportId": 12,
        "section": "summary",
        "chunkIndex": -1
      }
    }
  ],
  "ragDiagnostics": {
    "status": "OK",
    "retrievalMode": "deterministic_fallback",
    "embeddingMode": "hash_embedding_fallback",
    "expandedQueryCount": 4,
    "candidateCount": 8,
    "selectedEvidenceCount": 3,
    "scoreNormalization": "min_max",
    "fusion": "rrf",
    "reranker": "deterministic_keyword_overlap",
    "emptyRetrieval": false
  }
}
```

空检索响应：

```json
{
  "status": "EMPTY_RETRIEVAL",
  "aiSuggestions": "",
  "evidenceRefs": [],
  "ragDiagnostics": {
    "status": "EMPTY_RETRIEVAL",
    "retrievalMode": "deterministic_fallback",
    "embeddingMode": "hash_embedding_fallback",
    "candidateCount": 0,
    "selectedEvidenceCount": 0,
    "emptyRetrieval": true
  }
}
```

Python 校验失败：

```json
{
  "error": "VALIDATION_ERROR",
  "message": "reportId and userId are required"
}
```

非 JSON：

```json
{
  "error": "INVALID_JSON",
  "message": "request body must be a JSON object"
}
```

内部异常：

```json
{
  "error": "INTERNAL_ERROR",
  "message": "reports support generation failed"
}
```

Java 错误映射：

| Python 情况 | Java 行为 | `ragDiagnostics.status` |
| --- | --- | --- |
| 2xx + `OK` | 使用 Python 建议与证据 | `OK` |
| 2xx + `EMPTY_RETRIEVAL` | 使用 deterministic fallback 建议，证据为空 | `FALLBACK` |
| 400 | 使用 fallback，不重试 | `FALLBACK` |
| 5xx | 使用 fallback，不重试 | `FALLBACK` |
| Timeout | 使用 fallback，不重试 | `FALLBACK` |
| 连接失败 | 使用 fallback，不重试 | `FALLBACK` |
| 响应 JSON 无效或 2xx schema 缺失/状态未知/字段类型错误 | 使用 fallback，不重试 | `FALLBACK` |

`evidenceRefs[].snippet` 只能返回经过轻量脱敏的短文本片段，至少遮蔽邮箱、手机号和身份证号；完整简历、职业数据或能力画像原文不得写入诊断日志。

## 前端影响

- `website/src/api/reports.ts` 的 `ReportDetail` 增加：
  - `evidenceRefs?: ReportEvidenceRef[]`
  - `ragDiagnostics?: ReportRagDiagnostics`
- 现有轮询、下载、编辑路径不变。
- 前端可选择展示证据和诊断；不应把诊断视为用户可编辑字段。
- `ReportUpdateBody` 仅声明 `careerGoal`、`actionPlan`、`targetJob`、`developmentPath` 等可编辑字段；`evidenceRefs`、`ragDiagnostics`、`aiSuggestions`、`matchDetails` 在类型层面禁止由前端写回。

## 测试口径

必须覆盖：

- Python：RAG service 单元测试、HTTP handler 200、400/空 body、非 JSON、handler 5xx、empty retrieval。
- Java：`PythonReportsAiClientTest` 覆盖 200、400、5xx、timeout、invalid body、empty retrieval、无重试。
- Java：`ReportServiceImplReportsRagTest` 覆盖 Python 成功、Python 失败 fallback、空检索 fallback、前置数据缺失 `FAILED`。
- Maven：指定 Reports 测试的 Surefire XML 必须存在，且 `tests>0 failures=0 errors=0 skipped=0`。
- Frontend：若 `website/src/api/reports.ts` 改动，必须运行 `npm run build`。
- Runtime smoke：若 Java 8081、Python Reports 服务、PostgreSQL、Redis、OSS/JWT 可用，则请求实际 `/api/reports/generate` 并轮询详情；不可用时逐项记录缺失依赖，不声明端到端通过。
