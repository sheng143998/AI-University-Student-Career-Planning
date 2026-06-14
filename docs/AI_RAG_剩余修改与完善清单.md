# AI/RAG 剩余修改与完善清单

生成时间：2026-06-13
更新：2026-06-15
范围：AI-University-Student-Career-Planning 的 AI/RAG Python 化、Java-Python 集成、接口文档、测试与运行验收。

## 当前总体状态

当前主工作区 `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning` 已恢复为 clean，且 `master` 与 `origin/master` 同步在 PR #1 的 merge commit `63aa2edc419fc5907d1364460f7e28eb818aa93f`。历史脏改已备份到 `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-backups\main-dirty-20260613-013812`。此前“主工作区脏改”和“GitHub PR #1 未合并”风险已关闭。

Canonical 运行门禁结论：Python 8090/8091/8092 smoke 通过；Java 8081 首次因 Redis/env 与远端 PostgreSQL DNS 失败；本地临时 pgvector+Redis+最小种子+JWT Roadmap smoke 通过 HTTP 200/code=1/ragDiagnostics.fusion=rrf；PR #1 已 ready、CI 全绿并合并到 GitHub `master`。

多个 AI/RAG 功能已经在隔离 worktree 中完成本地闭环，并已通过 GitHub PR #1 收敛到 `master`。PR #1 最新状态为 `MERGED`，合并时间 `2026-06-14T15:15:29Z`，合并提交 `63aa2edc419fc5907d1364460f7e28eb818aa93f`；远端 `ai-rag-integration-20260613` 分支已删除。

| 模块 | 隔离分支 | ahead | 最新提交 | 状态 |
| --- | --- | ---: | --- | --- |
| Chat | `ai-rag-chat-python-boundary` | 3 | `46451df test: verify chat debug env binding` | 功能已进入 `ai-rag-integration-20260613`，旧分支只作归档参考 |
| Dashboard | `ai-rag-dashboard-target-job-match` | 1 | `713eb90 feat: route dashboard target matching through python rag` | 已迁移进 `ai-service/`，旧 `ai_service/**` 分支不直接合并 |
| Goals | `ai-rag-goals-advice-python-rag` | 1 | `e0d6ae6 feat: route goals advice through python rag` | 功能已进入 `ai-service/`，旧 `ai_service/**` 分支不直接合并 |
| Reports | `ai-rag-reports-python-rag` | 2 | `958fcd9 test: close reports rag post-commit gate` | patch 等价已进入 `ai-rag-integration-20260613` |
| Resume | `ai-rag-resume-python-rag` | 1 | `2f60cde feat: route resume analysis through python rag` | 已迁移进 `ai-service/`，旧 `ai_service/**` 分支不直接合并 |
| Roadmap | `ai-rag-roadmap-python-rag` | 3 | `9b10a7a test: harden roadmap current-job cache handling` | 核心能力已迁入 `ai-rag-integration-20260613`；旧分支的 `JwtTokenInterceptor.java` 与 `Roadmap.vue` 未覆盖，需单独评估 |

## P0：必须先处理的风险

### 1. 主工作区脏改已经隔离，仍需防止回流

主工作区当前 clean。后续需要避免旧备份、旧 `ai_service/` 分支实现、配置文件改动和构建产物重新回流到 `master`。

- 哪些隔离 worktree 分支要 push/merge。
- 旧分支里的 `ai_service/` 新能力不得直接回流；需要时必须重建到统一 `ai-service/`。
- `application.yml`、`application-dev.yml` 的历史改动如需恢复，必须单独审查，禁止随 AI/RAG 合并。
- `database/*.sql`、打包产物、`.codex-temp*`、`.m2-codex`、`.claude` 等只保留在备份或本地忽略范围，不纳入 AI/RAG 提交。

建议：继续只在隔离集成 worktree 中收敛，确保每次只迁移一个 AI/RAG 最小项。主工作区在最终验证前保持只读。

### 2. GitHub PR #1 未合并风险已关闭

Resume、Reports、Goals、Dashboard、Chat、Roadmap、Market fallback、Feedback queue 与统一配置文档已经收敛到统一 `ai-service/` 方向。Roadmap 已迁入 `ai-service` 聚合入口，并通过 Python `71 passed`、Java server `88 tests`、Roadmap narrow `19 tests`、前端 build 与子 Agent 复验。

后续分支处理方式：

- `ai-rag-integration-20260613` 已作为唯一 GitHub 候选集成分支完成合并，远端分支已删除。
- 不再直接 merge 旧 `ai_service/**` 分支；旧分支覆盖矩阵作为本地归档依据，不再随 GitHub 仓库提交。
- 旧 Roadmap 分支中的 `JwtTokenInterceptor.java` 与 `website/src/views/Roadmap.vue` 不随集成分支进入，需要时另起最小任务重审。
- 后续新功能仍必须先通过接口文档、测试日志、CI 和必要的 runtime smoke 证据，再进入 `master`。

### 3. 运行门禁已经补充，但仍不能替代生产 runtime smoke

已记录的运行门禁证据：

- Python `ai-service` tests: `71 passed`
- Java server tests: `88 tests`, 0 failures/errors/skipped
- Roadmap narrow Java tests: `19 tests`, 0 failures/errors/skipped
- Frontend `npm run build`: passed, 125 modules transformed
- Python 8090/8091/8092 smoke 通过
- Java 8081 + 临时 pgvector + Redis + JWT Roadmap smoke 通过 HTTP 200、`code=1`、`ragDiagnostics.fusion=rrf`
- GitHub PR #1 CI 全绿：Workflow lint、Python tests、Maven tests、Frontend build 均通过。

限制：上述证据说明合并后运行风险已显著降低，但仍不能替代长期环境中的真实 Java 8081 + Python 8090/8091/8092 + Redis + PostgreSQL/pgvector + JWT 运行时 smoke、真实模型调用和离线 RAG 质量评估。

## P1：功能还不够完善的地方

### 4. 当前 RAG 多数是 deterministic fallback，不是生产级 RAG

多个模块已经具备可测试的 fallback RAG：递归分块、摘要索引、元数据过滤、Multi-Query、BM25、hash embedding、RRF、deterministic rerank。

但还不等于生产级 RAG。仍需补：

- 真实 embedding 模型接入。
- pgvector 存储、索引、召回和过滤。
- Dashscope/Qwen 生成链路。
- 可选 cross-encoder 或 ranking model。
- 离线评估集和 RAG 质量指标。
- 召回、重排、生成的可观测日志和脱敏审计。

### 5. Python 服务目录已统一，后续需防回流

当前运行目录统一为 `ai-service/`，旧 `ai_service/` 目录已移除。新增能力必须继续落到 `ai-service/`，不能重新创建旧目录。统一布局为：

- `app/`：HTTP 入口。
- `rag/`：chunker、summary index、retriever、fusion、reranker。
- `schemas/`：请求/响应模型。
- `tests/`：单元和集成测试。
- `.env.example` / README：端口、模型、DB、Redis、环境变量。

否则后续合并分支时会再次出现路径冲突和运维混乱。

### 6. Interface 10 反馈与设置闭环仍需继续核对

历史记录显示接口 10 的 AI/RAG feedback/settings 有文档和部分实现痕迹，但仍需确认：

- `/api/feedback/ai-rag` 是否完整实现。
- `/api/settings/ai-rag` 是否完整实现。
- `/internal/rag/feedback`、`/internal/rag/preferences/validate` 是否有 Python/Java 闭环。
- AI 建议反馈是否能进入评估队列。
- 用户归属、sourceType/sourceId 校验是否覆盖所有入口。
- 前端是否有对应 API 调用和用户状态。

### 7. Market / Recruitment JD 方向还需要补强

接口文档里提到市场数据、招聘数据和企业 1w 条就业数据仍有待补充。当前还需要确认：

- 招聘 JD ingestion 是否从 Java 转到 Python。
- JD 分块、摘要索引、metadata filter 是否落地。
- job_vector_store 是否仍有 Java fake embedding 或空向量风险。
- 市场趋势分析是否有真实数据源和 Python RAG 支撑；当前仅有 `ai-service` deterministic fallback。
- Dashboard/Roadmap 是否依赖同一套岗位知识库，避免重复实现。

### 8. Resume 闭环仍缺真实 PDF/OCR 和数据库约束

Resume 分支已完成 Python fallback RAG，但仍有后续完善项：

- 图片型 PDF/OCR 解析仍需按 `docs/简历图片型PDF解析改造计划.md` 继续落地。
- 未新增 `(vector_store_id, user_id)` 唯一约束，目前靠 Java/MyBatis 层幂等。
- 还未做真实 OSS 文件、真实用户、真实异步轮询的端到端测试。
- 未做真实 embedding/pgvector 的简历召回质量评估。

### 9. Chat 调试入口和生产暴露边界要复查

历史测试日志提到 `/ai/chat` 兼容调试入口不在 `/api/**` 鉴权范围内。需要确认：

- 生产环境是否暴露 `/ai/chat`。
- 如果暴露，是否需要 JWT、限流、脱敏和审计。
- Chat 流式响应是否和 evidence/diagnostics 落库兼容。
- 前端 stream 读取逻辑是否能兼容 Python 下游异常和降级。

### 10. 前端契约需要统一验收

部分分支改过 `website/src/api/*.ts`，但并非所有模块都跑过完整前端 build。需要统一做：

- `npm run build`。
- TypeScript 类型与 Java `Result<T>` 包装一致性。
- 禁止浏览器直连 Python 内部接口。
- 对 fallback、timeout、empty retrieval、权限失败的用户态提示。
- RAG evidence/diagnostics 如果展示，必须脱敏。

## P2：工程质量和可维护性

### 11. 测试日志和 Obsidian 记录需要统一索引

自动化运行日志和 Obsidian 使用记录保留在本地工作区或个人笔记中，不再作为 GitHub 运行仓库内容提交。后续仍需要在 GitHub PR 合并前后维护最终总索引，但索引应放在当前文档或正式 `docs/` 文档中：

- 每个接口文档对应哪个分支、哪个提交、哪份测试日志。
- 哪些分支已合并，哪些只是本地闭环。
- 哪些 runtime smoke 未执行。
- 哪些测试是 fallback 级别，哪些是真实服务级别。

### 12. 配置命名和端口已形成当前定稿，仍需随 GitHub PR 复核

当前端口分工已经写入 `docs/AI_RAG_配置与端口说明.md`，并按 `ai-rag-integration-20260613` 的实际代码复核：

- Java backend：`8081`
- Resume Python：`8091`
- Chat Python：`8092`
- 聚合 Python RAG 服务：`8090`，当前承载 Reports、Goals、Dashboard、Roadmap、Feedback 与偏好校验。

已记录的主要环境变量包括：

- `FUCHUANG_AI_PYTHON_BASE_URL`
- `FUCHUANG_AI_PYTHON_RESUME_BASE_URL`
- `FUCHUANG_AI_PYTHON_CHAT_BASE_URL`
- `FUCHUANG_AI_PYTHON_REPORTS_BASE_URL`
- 各模块 timeout 配置。

剩余风险不是“端口未定稿”，而是后续仍需用真实 Java `8081` + Python `8090/8091/8092` + Redis + PostgreSQL/pgvector + JWT 环境做 runtime smoke，确认配置在运行时生效。

### 12.1 常用 CLI 工具环境已补齐，后续新增工具仍需登记

本轮已复核 `winget`、`git`、`gh`、`rg`、`actionlint`、`jq`、`yq`、`uv`、`go`、`java`、`mvn`、`node`、`npm`、`python`、`pip`、`docker` 均可在当前环境直接找到。用户级 PATH 已包含 `C:\Users\WhenJayHe\bin`、WindowsApps、winget 包目录、`C:\Users\WhenJayHe\sdk\go1.26.4\bin` 与 `C:\Users\WhenJayHe\go\bin`；用户级 `GOROOT` / `GOPATH` 已持久化。后续新增常用工具时仍需记录安装来源、版本、PATH 或环境变量。

### 13. 需要补真实安全和隐私审计

当前已有 diff-only secret scan 和日志脱敏约束，但生产前还应补：

- raw resume / raw JD / raw prompt 不落日志检查。
- OSS URL、JWT、API key 不落日志检查。
- Python diagnostics 不返回敏感正文。
- 前端不展示敏感 retrieval snippets。
- 测试日志只使用合成数据。

### 14. 需要决定最终集成策略

推荐顺序：

1. 继续冻结主工作区的非 AI/RAG 临时改动，不直接恢复旧备份。
2. 旧隔离分支只按覆盖矩阵归档，不再直接 merge 到 `master`。
3. 后续每个新优化项都单独开最小分支或最小提交，避免再次出现大批分支长期未合并。
4. 新优化项合并前补真实端到端 smoke；如果无法运行，必须在 PR、测试日志和 Obsidian 记录中写明替代验证与剩余风险。

## 建议下一步

最优先不是继续写新功能，而是基于已合并的 `master` 做运行时验收：

1. 针对当前 `master` 运行 Java 8081 + Python 8090/8091/8092 + Redis/JWT 的最小 runtime smoke。
2. 明确旧 Roadmap 分支的 `JwtTokenInterceptor.java` 和 `Roadmap.vue` 是否仍需要；需要则另起最小任务重审。
3. 再推进 Market/JD ingestion、Interface 10 feedback/settings、生产级 pgvector/Dashscope/评估闭环。
4. 为真实 pgvector、Dashscope/Qwen、cross-encoder 和离线评估补独立接口文档、测试和测试日志。
