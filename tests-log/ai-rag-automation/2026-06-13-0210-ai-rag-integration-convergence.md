# AI/RAG 集成收敛测试日志

## 测试对象

- 主工作区：C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning
- 集成 worktree：C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-integration
- 基线：origin/master 313b9be
- 当前集成 HEAD：946a396（本日志修订前）
- 清单来源：C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-backups\main-dirty-20260613-013812\untracked\docs\AI_RAG_剩余修改与完善清单.md，已恢复为仓库内 docs/AI_RAG_剩余修改与完善清单.md

## 测试原因

解决主工作区脏改和多个 AI/RAG 分支未合并风险；将可用本地/GitLab 改动纳入统一 ai-service/ Python 服务目录；对不可直接合并的分支给出处置结论；为后续继续按清单修复提供干净集成基线。

## 分支处置台账

| 来源 | 分支/提交 | 处置 | 依据 |
| --- | --- | --- | --- |
| local | Reports a05da0f, 958fcd9 | cherry-picked | 已进入集成分支；Python 测试、Java 窄测、前端 build 通过；历史日志保留 |
| local | Resume 2f60cde | cherry-picked-with-path-migration | 已迁移到 ai-service/career_ai/resume_analysis_service.py；Python/Java 测试通过 |
| local | Goals e0d6ae6 | cherry-picked-with-path-migration | 已迁移到 ai-service/career_ai/goals_advice_service.py，并接入统一 app.main；Python/Java/前端验证通过 |
| local | Chat b0de0e3, c7c4774, 46451df | cherry-picked | 合并 app.main 冲突后保留 Chat 8092、debug gate 和测试；Python/Java 验证通过 |
| local | Dashboard 713eb90 | deferred | 直接 cherry-pick 冲突 PythonAiProperties、UserVectorStoreMapper、XML、旧 ai_service 聚合入口；需单独迁移到 ai-service/ 后验证 |
| local | Roadmap aa79af9, 4678a1d, 9b10a7a | deferred | 首个提交重新引入旧 ai_service 聚合入口且与共享配置冲突；需单独迁移到 ai-service/ 后验证 |
| gitlab | config ports doc 51710b9 | cherry-picked-with-fix | 已纳入 docs/AI_RAG_配置与端口说明.md，修复 Markdown trailing whitespace |
| gitlab | feedback eval queue 4631132 | ported-not-cherry-picked | 原提交直接 cherry-pick 会污染旧目录；已按同等功能移植到 ai-service/career_ai/feedback_* 并通过测试 |

## 测试环境

- Windows PowerShell
- Python：系统 python
- Java/Maven：本机 Maven
- Frontend：website 下 npm ci + npm run build
- Runtime 依赖：未确认 PostgreSQL/pgvector、Redis、OSS、JWT 测试用户、Dashscope API key 是否齐全；本日志不声明端到端 runtime smoke 通过。

## 已执行命令与结果

`powershell
# 主工作区备份/恢复
# 备份目录：C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-backups\main-dirty-20260613-013812
# tracked patch：C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-backups\main-dirty-20260613-013812\tracked-changes.patch
# untracked 清单：C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-backups\main-dirty-20260613-013812\untracked-items.txt
# 恢复说明：C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-backups\main-dirty-20260613-013812\RESTORE.md

git -C C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning status --short --branch
# 结果：clean，仅本地 exclude 忽略被锁打包目录。

# 集成 Python 测试
$env:PYTHONPATH='ai-service'
python -B -m pytest ai-service/tests -q -p no:cacheprovider
# 结果：38 passed in 6.04s / 38 passed in 6.00s

# Maven compile
mvn -pl server -am -DskipTests compile
# 结果：BUILD SUCCESS

# Java 窄测
mvn --% -pl server -am -Dtest=PythonReportsAiClientTest,ReportServiceImplReportsRagTest,PythonResumeAiClientTest,ResumeServiceImplResumePythonTest,PythonGoalsAdviceClientTest,GoalsControllerTest,GoalsServiceImplTest,PythonChatClientTest,ChatControllerDebugEndpointDisabledTest,ChatControllerDebugEndpointEnabledTest,ChatRestControllerMessagesTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test
# 结果：Tests run: 48, Failures: 0, Errors: 0, Skipped: 0；BUILD SUCCESS

# Frontend
cd website
npm ci
npm run build
# 结果：build 成功；npm audit 报 7 vulnerabilities（5 moderate, 2 high），本轮未修依赖安全债。

# Static gates
git diff --check origin/master..HEAD
# 结果：通过（无输出）

# Denylist
git diff --name-only origin/master..HEAD | Select-String 本地产物/缓存/secret 路径规则
# 结果：deny_hits=0

# Strict secret scan（排除测试与测试日志哨兵）
# 结果：strict_secret_hits=0
`

## 测试数据或请求样例

- Reports：ai-service/tests/test_report_support_service.py 中的合成 reports payload。
- Resume：ai-service/tests/test_resume_analysis_service.py 中的 sample_payload()，包含合成简历文本。
- Goals：ai-service/tests/test_goals_advice_service.py 中的目标、里程碑、成功标准样例，并验证 PII 不泄露。
- Chat：ai-service/tests/test_chat_pipeline.py 中的前端岗位咨询和 TypeScript 面试准备样例。
- Feedback：ai-service/tests/test_feedback_service.py 中的 CHAT_MESSAGE 反馈、偏好校验、重复 request_id 幂等队列样例。

## 失败原因与修复记录

- 主工作区存在大量 tracked/untracked 脏改：已写入仓库外备份并恢复 Git clean；被锁打包目录保留原地并写入本地 exclude。
- Resume/Goals 原分支落在旧 ai_service/：已迁移到 ai-service/career_ai 与 ai-service/tests。
- Chat 与 Reports/Goals 对 app.main 发生 add/add 冲突：已合并成统一 AiServiceHandler，并保留 ReportsHandler 兼容别名。
- GitLab feedback queue 原提交直接 cherry-pick 会污染旧目录：已按同等功能移植到统一目录，保留 JSONL 幂等队列和脱敏约束。
- GitLab 配置端口文档有 trailing whitespace：已修复。
- 初次 frontend build 缺 node_modules，vue-tsc 不存在：已执行 npm ci 后 build 通过。
- 初次 Maven 窄测被 PowerShell 参数解析影响：已用 --% 重跑通过。
- 初次 secret scan 命中测试哨兵 sk-abcdefghijkl / sk-proj-...：严格扫描排除测试/日志后为 0，测试哨兵用于验证脱敏逻辑，不是真实凭据。

## 子 Agent 验收结论

- Plan 需求覆盖审查：FAIL 后已补强 GitLab 分支处置、备份证据、主工作区 clean 门禁、分支台账、清单入口顺序。
- Plan 技术风险审查：FAIL 后已改为以干净集成 worktree 为合并现场，并硬性排除本地产物、旧目录污染和未验收分支。
- Goal 边界审查：PASS，但指出冲突和清单缺失；当前已解决冲突并恢复清单。
- Goal 验证命令审查：FAIL 后已补充验证对象、基线、分支处置、目录策略、Python/Java/前端/安全证据。

## 剩余风险

- Dashboard 与 Roadmap 尚未进入本集成分支，状态为 deferred，不声明风险已消除，只声明已有明确处置结论。
- 当前 RAG 仍为 deterministic fallback，不声明生产级 pgvector/Dashscope/cross-encoder 质量。
- 真实 Java 8081 + Python 8090/8091/8092 + PostgreSQL/pgvector + Redis + OSS/JWT runtime smoke 未执行。
- npm audit 存在 7 个依赖漏洞（5 moderate, 2 high），非本轮 AI/RAG 集成修改引入，需后续单独评估。

## 优化建议

1. 下一轮优先单独迁移 Dashboard 到 ai-service/，处理 UserVectorStoreMapper 冲突。
2. 再单独迁移 Roadmap 到 ai-service/，统一 8090 聚合入口。
3. 随后推进真实 pgvector/Dashscope 和 runtime smoke。
