# AI/RAG 剩余修改与完善清单

生成时间：2026-06-13
范围：AI-University-Student-Career-Planning 的 AI/RAG Python 化、Java-Python 集成、接口文档、测试与运行验收。

## 当前总体状态

当前主工作区 `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning` 仍然很脏，存在约 66 条已修改、删除或未跟踪文件。不要在主工作区直接执行 `git add .` 或批量提交。

多个 AI/RAG 功能已经在隔离 worktree 中完成本地闭环，但还没有 push、merge 或开 PR：

| 模块 | 隔离分支 | ahead | 最新提交 | 状态 |
| --- | --- | ---: | --- | --- |
| Chat | `ai-rag-chat-python-boundary` | 3 | `46451df test: verify chat debug env binding` | 本地闭环待合并决策 |
| Dashboard | `ai-rag-dashboard-target-job-match` | 1 | `713eb90 feat: route dashboard target matching through python rag` | 本地闭环待合并决策 |
| Goals | `ai-rag-goals-advice-python-rag` | 1 | `e0d6ae6 feat: route goals advice through python rag` | 本地闭环待合并决策 |
| Reports | `ai-rag-reports-python-rag` | 2 | `958fcd9 test: close reports rag post-commit gate` | 本地闭环待合并决策 |
| Resume | `ai-rag-resume-python-rag` | 1 | `2f60cde feat: route resume analysis through python rag` | 本地闭环待合并决策 |
| Roadmap | `ai-rag-roadmap-python-rag` | 3 | `9b10a7a test: harden roadmap current-job cache handling` | 本地闭环待合并决策 |

## P0：必须先处理的风险

### 1. 主工作区脏改需要隔离整理

主工作区当前同时包含旧改动、未跟踪目录、配置文件改动和多个模块变更。需要先决定：

- 哪些隔离 worktree 分支要 push/merge。
- 主工作区里的 `ai_service/` 删除和 `ai-service/` 未跟踪目录如何统一。
- `application.yml`、`application-dev.yml` 的改动哪些属于必要配置，哪些应排除。
- `database/*.sql`、打包产物、`.codex-temp*`、`.m2-codex`、`.claude` 等是否保留、忽略或清理。

建议：先不要继续在主工作区实现新功能，优先用隔离分支逐个合并或 cherry-pick，确保每次只合并一个 AI/RAG 最小项。

### 2. 所有本地闭环分支仍未 push/merge/PR

Resume、Reports、Roadmap、Goals、Dashboard、Chat 都还停留在本地隔离分支。功能虽然已有本地验证，但主分支和远端并没有真正获得这些改动。

需要决定每个分支的处理方式：

- push 到远端并开 PR。
- cherry-pick 到一个集成分支。
- 先废弃部分分支，只保留最稳定的实现。
- 重新按统一 Python 服务目录合并后再提交。

### 3. Java + Python + DB + Redis + OSS/JWT 端到端 smoke 基本都未跑

当前多数模块只完成了单元测试、窄集成测试、编译和 HTTP handler smoke。还缺真实运行链路：

- Java `8081` 启动。
- Python AI/RAG 服务端口启动，例如 Resume `8091`、Chat `8092`、Reports/Dashboard 等。
- PostgreSQL + pgvector 可用。
- Redis 可用。
- OSS/JWT/测试用户和测试文件准备好。
- 前端通过真实 Java API 调用并轮询状态。

这一步不补，不能宣称“端到端可上线”。

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

### 5. Python 服务目录需要统一

当前仓库里出现两个目录概念：

- `ai_service/`
- `ai-service/`

隔离分支中也存在不同模块落在不同目录的情况。继续扩展前需要统一 Python 服务布局，例如：

- `app/`：HTTP 入口。
- `rag/`：chunker、summary index、retriever、fusion、reranker。
- `schemas/`：请求/响应模型。
- `tests/`：单元和集成测试。
- `.env.example` / README：端口、模型、DB、Redis、环境变量。

否则后续合并分支时会出现路径冲突和运维混乱。

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
- 市场趋势分析是否有真实数据源和 Python RAG 支撑。
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

现在已有多份 `tests-log/ai-rag-automation/*.md` 和 Obsidian 使用记录，但还缺一个总索引：

- 每个接口文档对应哪个分支、哪个提交、哪份测试日志。
- 哪些分支已合并，哪些只是本地闭环。
- 哪些 runtime smoke 未执行。
- 哪些测试是 fallback 级别，哪些是真实服务级别。

### 12. 配置命名和端口需要最终定稿

已有端口分工大致如下，但还需要写入统一配置说明：

- Java backend：`8081`
- Resume Python：`8091`
- Chat Python：`8092`
- 其他 Python RAG 服务：需要明确是否聚合到一个服务，还是每个模块独立端口。

还要统一环境变量优先级，例如：

- `FUCHUANG_AI_PYTHON_BASE_URL`
- `FUCHUANG_AI_PYTHON_RESUME_BASE_URL`
- `FUCHUANG_AI_PYTHON_CHAT_BASE_URL`
- 各模块 timeout 配置。

### 13. 需要补真实安全和隐私审计

当前已有 diff-only secret scan 和日志脱敏约束，但生产前还应补：

- raw resume / raw JD / raw prompt 不落日志检查。
- OSS URL、JWT、API key 不落日志检查。
- Python diagnostics 不返回敏感正文。
- 前端不展示敏感 retrieval snippets。
- 测试日志只使用合成数据。

### 14. 需要决定最终集成策略

推荐顺序：

1. 先冻结主工作区，不继续直接开发。
2. 按模块选择最稳定的隔离分支。
3. 每次只合并一个模块，先跑该模块测试，再跑全局 compile/build。
4. 合并后建立一个统一 AI/RAG 总测试日志。
5. 最后再做真实端到端 smoke。

## 建议下一步

最优先不是继续写新功能，而是做一次“集成收敛”：

1. 选择要先合并的隔离分支，建议从 Resume 或 Reports 开始，因为它们的测试日志和 gate 最完整。
2. 清理或隔离主工作区脏改，避免污染合并。
3. 统一 `ai_service/` 与 `ai-service/` 目录策略。
4. 准备真实 runtime smoke 环境。
5. 再推进 Market/JD ingestion、Interface 10 feedback/settings、生产级 pgvector/Dashscope/评估闭环。
