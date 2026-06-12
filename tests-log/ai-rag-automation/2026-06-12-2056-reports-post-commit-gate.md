# Reports-RAG post-commit 分支门禁与前端契约复验

## 测试对象
- 分支：`ai-rag-reports-python-rag`
- Commit：`a05da0f` 起的 Reports-RAG 分支 diff，以及本轮前端更新契约收紧补丁。
- 关键文件：`website/src/api/reports.ts`、`接口文档/接口文档_9_Reports.md`、`tests-log/ai-rag-automation/2026-06-12-1835-reports-python-rag.md`。

## 测试原因
Goal 验证命令子 Agent 指出 post-commit 退出门禁缺少分支级 `origin/master..HEAD` allowlist/denylist、strict diff-only secret scan、远端分支/PR 结果同步；技术风险子 Agent 指出 `ReportUpdateBody` 宽泛索引签名会允许前端把 `evidenceRefs` 或 `ragDiagnostics` 写回。

## 测试环境
- Worktree：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-reports`
- 主工作区：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning`，仍有既有脏改，本轮未触碰。
- Python/Java/Node：沿用 `2026-06-12-1835-reports-python-rag.md` 的本地环境。

## 变更与修复记录
- `website/src/api/reports.ts`：移除 `ReportUpdateBody` 的 `[k: string]: unknown`，并把 `evidenceRefs`、`ragDiagnostics`、`aiSuggestions`、`matchDetails` 标记为 `never`，阻止对象字面量在 PUT body 中写回 AI/RAG 证据、诊断和建议字段。
- `接口文档/接口文档_9_Reports.md`：同步前端影响，明确 `ReportUpdateBody` 只声明可编辑字段，RAG 字段类型层面禁止写回。

## Post-commit git gate
命令：
```powershell
git status --short --branch
```
结果：在 `a05da0f` Reports-RAG 基线复核时为 `## ai-rag-reports-python-rag...origin/master [ahead 1]`，工作区 clean。

本日志创建后追加了前端契约收紧、接口文档同步和本日志自身，因此提交前状态预期为：
```text
 M website/src/api/reports.ts
 M 接口文档/接口文档_9_Reports.md
?? tests-log/ai-rag-automation/2026-06-12-2056-reports-post-commit-gate.md
```
最终提交后的 clean gate 将由真实 Obsidian 记录和 automation memory 承载，避免仓库内日志自引用最终 commit hash。

命令：
```powershell
git -c core.quotepath=false diff --name-only origin/master..HEAD
```
结果：`a05da0f` 基线为 15 个 Reports-RAG allowlist 文件；本轮补丁提交后将增加 `tests-log/ai-rag-automation/2026-06-12-2056-reports-post-commit-gate.md`，最终 allowlist 应为 16 个文件：
- `ai-service/README.md`
- `ai-service/app/__init__.py`
- `ai-service/app/main.py`
- `ai-service/career_ai/__init__.py`
- `ai-service/career_ai/report_support_service.py`
- `ai-service/tests/test_report_support_service.py`
- `pojo/src/main/java/com/itsheng/pojo/vo/ReportDetailVO.java`
- `server/src/main/java/com/itsheng/service/client/PythonReportsAiClient.java`
- `server/src/main/java/com/itsheng/service/config/PythonAiProperties.java`
- `server/src/main/java/com/itsheng/service/service/Impl/ReportServiceImpl.java`
- `server/src/test/java/com/itsheng/service/client/PythonReportsAiClientTest.java`
- `server/src/test/java/com/itsheng/service/service/Impl/ReportServiceImplReportsRagTest.java`
- `tests-log/ai-rag-automation/2026-06-12-1835-reports-python-rag.md`
- `website/src/api/reports.ts`
- `接口文档/接口文档_9_Reports.md`

Allowlist/denylist 结果：`files=15`、`extra=0`、`missing=0`、`deny=0`。Denylist 覆盖 `application*.yml`、`database/`、`pom.xml`、`target/`、`dist/`、`node_modules/`、旧 `ai_service/`、`__pycache__`、`.pyc`。

命令：
```powershell
git diff --check HEAD
```
结果：无输出。

命令：
```powershell
git diff origin/master..HEAD -- . ':(exclude)website/package-lock.json' | strict secret scan
```
结果：`secret_hits=0`。扫描模式覆盖高风险 `api key`、`secret`、`token`、`password`、AWS key、OSS secret、Dashscope key、JWT 形态值。

命令：
```powershell
git ls-files server/src/test/java/com/itsheng/service/client/PythonReportsAiClientTest.java server/src/test/java/com/itsheng/service/service/Impl/ReportServiceImplReportsRagTest.java
```
结果：两个 Java 测试均已被 Git 跟踪，`.gitignore` 的 `**/test/` 风险已通过精确 `git add -f` 关闭。

## Remote / PR gate
命令：
```powershell
git ls-remote --heads origin ai-rag-reports-python-rag
```
结果：无输出，远端同名分支不存在。

命令：
```powershell
gh pr list --head ai-rag-reports-python-rag --state all
```
结果：无输出，未开 PR。

## Obsidian 记录核验
命令：
```powershell
rg -n "Reports-RAG|接口文档_9|a05da0f|generate-support" "C:\Users\WhenJayHe\notes\study\项目使用记录\AI-University-Student-Career-Planning"
```
结果：命中 `接口文档_9_Reports_RAG优化记录.md`，记录包含来源笔记、实现文件、测试结果、commit hash `a05da0f`、未 push/merge/PR 和剩余风险。

## 复验计划
- 因 `website/src/api/reports.ts` 类型收紧，已复跑 `cd website; npm run build`，结果 `vue-tsc && vite build` 成功，125 modules transformed，`built in 2.37s`。
- Python pytest 复跑：
```powershell
$env:PYTHONPATH='ai-service'; python -B -m pytest ai-service/tests/test_report_support_service.py -q -p no:cacheprovider
```
结果：`9 passed in 2.76s`。
- Java 指定测试复跑：
```powershell
mvn -pl server -am "-Dtest=PythonReportsAiClientTest,ReportServiceImplReportsRagTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```
结果：`Tests run: 13, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- Maven compile 复跑：
```powershell
mvn -pl server -am -DskipTests compile
```
结果：`BUILD SUCCESS`。

## Runtime smoke
本轮仍未执行真实 Java 8081 + Python 8090 + Redis + PostgreSQL/pgvector + OSS/JWT 端到端 smoke。原因同 `2026-06-12-1835-reports-python-rag.md`：Java 8081、Python Reports 8090、Redis 6379 未同时可用，OSS 凭据和有效 JWT 不齐；不声明端到端通过。

## 子 Agent 结论
- Plan 需求覆盖 PASS。
- Plan 技术风险初审 FAIL，补 allowlist、secret、remote/PR、Obsidian、前端契约门禁后复审 PASS。
- Goal 边界 PASS。
- Goal 验证命令初审 FAIL，补 post-commit 分支级 gate 后复审 PASS。

## 剩余风险
- Reports-RAG 仍是 deterministic fallback，不代表真实 pgvector、Dashscope embedding/LLM、cross-encoder 或离线质量评估完成。
- 本轮仅收紧前端 PUT 类型契约和补充分支级验收记录；未执行真实 Java 8081 runtime smoke，不能声明端到端通过。
