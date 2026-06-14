# 2026-06-14 11:25 AI/RAG 分支覆盖与合并风险审计

## 审计对象

- 主工作区：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning`
- 集成 worktree：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-integration`
- 基线：`origin/master` / `master` = `313b9be fix: tighten ai rag feedback ownership`
- 集成分支：`ai-rag-integration-20260613`
- Roadmap 功能集成快照：`2ceba44 feat: integrate roadmap rag into ai service`
- 文档/审计收口提交：`8b0329b docs: refine ai rag branch coverage audit` 及后续仅文档修订提交；最终以远端分支 HEAD 为准。
- GitLab 远端：`https://gitlab.com/yunqianqin-group/AI-University-Student-Career-Planning.git`

## 审计原因

Roadmap-RAG 已迁入统一 `ai-service/` 并提交到集成分支。完整目标仍不是“Roadmap 单项完成”，而是解决主工作区脏改和分支未合并风险，确保后续合并后能正常运行、无明显 bug。本日志用于确认当前各本地/GitLab AI/RAG 分支相对集成分支的覆盖关系，避免后续把旧 `ai_service/**`、UI/JWT 旁支或构建产物误合并回主分支。

## 当前状态

- 主工作区：`git status --short --branch` 输出 `## master...origin/master`，无普通脏改。
- Roadmap 功能集成提交后，集成 worktree 曾为 `## ai-rag-integration-20260613...origin/master [ahead 13]`；2026-06-14 文档/审计收口后继续追加文档提交，当前状态需以复查命令为准。
- Roadmap 功能集成提交后，集成分支相对 `origin/master` 曾为 `0 13`；文档/审计收口提交后左右计数会增加，提交前必须重新执行 `git rev-list --left-right --count origin/master...HEAD`。
- 集成分支总变更文件数：`git diff --name-only origin/master..ai-rag-integration-20260613` 为 113 个文件。
- GitLab 访问：普通 `git ls-remote` 会受本机 `http.proxy/https.proxy=http://127.0.0.1:10808` 影响；使用 `git -c http.proxy= -c https.proxy= ls-remote --heads ...` 可访问。
- GitLab 当前 heads：
  - `master` = `40e7351 chore: upload project snapshot for gitlab ai iteration`
  - `ai-rag-interface10-feedback-eval-queue` = `4631132 feat(ai-service): persist AI/RAG feedback into idempotent eval queue`
  - `ai-rag-config-ports-doc` = `51710b9 docs: add unified AI/RAG config and port reference (checklist P2-12)`
- GitLab refs 与当前 GitHub `origin/master`/集成分支无共同 merge-base；`git merge-base ai-rag-integration-20260613 40e7351/4631132/51710b9` 均 exit 1。后续不能直接 merge GitLab refs，只能按功能对照移植。
- `origin/master..ai-rag-integration-20260613` 对 `ai_service/**`、`website/src/views/Roadmap.vue`、`server/src/main/java/com/itsheng/service/interceptor/JwtTokenInterceptor.java` 无 diff。旧目录仍在基线中存在，但不是集成分支新增或修改内容。

## 分支覆盖矩阵

| 来源 | 分支/引用 | 状态 | 覆盖结论 | 仍需注意 |
| --- | --- | --- | --- | --- |
| 本地 | `ai-rag-chat-python-boundary` | ahead origin/master 3 | 功能已在集成分支中重建：`ai-service/app/routes/chat.py`、`rag/chat_pipeline.py`、`PythonChatClient`、debug gate 测试、Chat 接口文档和测试日志均已纳入集成分支。 | 独立分支 commit 未被直接包含，不能用 `branch --contains` 判定；后续归档前以集成分支测试为准。 |
| 本地 | `ai-rag-dashboard-target-job-match` | ahead origin/master 1 | 旧分支落在 `ai_service/**`；集成分支已迁移到 `ai-service/career_ai/dashboard_rag_service.py`、`PythonDashboardAiClient`、Dashboard 测试与日志。 | 旧分支不应直接 merge，否则会回引旧目录；以集成分支实现为准。 |
| 本地 | `ai-rag-goals-advice-python-rag` | ahead origin/master 1 | 旧分支落在 `ai_service/goals_rag_service.py`；集成分支已迁移到 `ai-service/career_ai/goals_advice_service.py`、`PythonGoalsAdviceClient`、Goals Java/前端契约和测试。 | 旧分支不应直接 merge。 |
| 本地 | `ai-rag-reports-python-rag` | ahead origin/master 2 | `git log --cherry-mark --right-only ai-rag-integration-20260613...ai-rag-reports-python-rag` 显示两个提交为 `=`，说明 patch 等价已覆盖。 | 可归档或删除本地 worktree 前先确认远端/PR 策略。 |
| 本地 | `ai-rag-resume-python-rag` | ahead origin/master 1 | 旧分支落在 `ai_service/resume_ai_service.py`；集成分支已迁移到 `ai-service/career_ai/resume_analysis_service.py`、`PythonResumeAiClient`、Resume 测试和接口文档。 | 旧分支不应直接 merge。 |
| 本地 | `ai-rag-roadmap-python-rag` | ahead origin/master 3 | Roadmap 核心能力已于 `2ceba44` 迁入 `ai-service/career_ai/roadmap_rag_service.py`，并补 Java fallback 同类排除、`possessedSkills` 脱敏、可选 `jd_summary`、Java/前端类型与测试日志。 | 旧分支额外包含 `JwtTokenInterceptor.java`、`website/src/views/Roadmap.vue` 和旧 Roadmap e2e 日志；`origin/master..ai-rag-integration` 对这两个源码文件无 diff，当前集成分支有意不纳入。如仍需要 UI/JWT 改动，应另起最小任务重新审查。 |
| GitLab | `gitlab/ai-rag-interface10-feedback-eval-queue` | 远端存在 | 集成分支已在 `2ea5cbf` 和 `b75b8ff` 移植 feedback queue、feedback service、测试与脱敏修复。 | 不是直接 cherry-pick；该 ref 与集成分支无共同 merge-base，直接合并会带入过期 snapshot 历史。 |
| GitLab | `gitlab/ai-rag-config-ports-doc` | 远端存在 | 集成分支已包含统一端口文档，并在 Roadmap 后补 `PythonRoadmapRagClient` 使用 `fuchuang.ai.python.base-url`。 | 该 ref 与集成分支无共同 merge-base，不应直接 merge；以后以集成分支文档为准。 |

## 关键验证证据

Roadmap 集成提交后已执行：

```powershell
cd C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-integration\ai-service
python -B -m py_compile app/main.py career_ai/roadmap_rag_service.py tests/test_roadmap_rag_service.py
python -B -m pytest tests/test_roadmap_rag_service.py -q -p no:cacheprovider
# 12 passed
python -B -m pytest tests -q -p no:cacheprovider
# 67 passed

cd C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-integration
mvn --% -pl server -am -Dtest=PythonRoadmapRagClientTest,RoadmapServiceImplTest,RoadmapControllerTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test
# 19 tests, 0 failures/errors/skipped
mvn -pl server -am test
# 88 tests, 0 failures/errors/skipped

cd C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-integration\website
npm run build
# success, 125 modules transformed
```

静态门禁：

```powershell
git diff --check origin/master..HEAD
# 无输出
git status --porcelain=v1 -uall
# 无普通输出；只有 ignored target/dist/node_modules/__pycache__ 等构建缓存
git -c http.proxy= -c https.proxy= ls-remote --heads https://gitlab.com/yunqianqin-group/AI-University-Student-Career-Planning.git
# 成功列出 GitLab heads
```

## 当前不应直接合并的内容

- 旧 `ai_service/**` 分支实现；注意 `ai_service/**` 在 `origin/master` 中已有历史文件，本项指“不要把旧分支新增/改动回流”，不是要求本轮删除基线历史目录。
- `server/src/main/java/com/itsheng/service/interceptor/JwtTokenInterceptor.java` 的旧 Roadmap e2e 日志脱敏改动。
- `website/src/views/Roadmap.vue` 的旧 current-job UI 刷新改动。
- `application*.yml`、`database/**`、`deploy/**`、`target/**`、`website/dist/**`、`website/node_modules/**`、`__pycache__/**`、`*.pyc`、`.env*`。

## 剩余风险

- 集成分支尚未推送到 GitHub/GitLab，也未合并到 `master`。
- 真实 Java 8081 + Python 8090/8091/8092 + Redis + PostgreSQL/pgvector + JWT runtime smoke 未执行。
- Roadmap 旧分支的 `JwtTokenInterceptor.java` 与 `Roadmap.vue` 改动未覆盖；当前结论是“不直接带入”，不是“功能已等价覆盖”。
- 当前多数 RAG 仍是 deterministic fallback，不声明生产级 pgvector/Dashscope/cross-encoder 质量。
- 本机全局 git/env proxy 指向 `127.0.0.1:10808`，推 GitLab 时需禁用或修复代理。

## 2026-06-14 收口复核补充

本补充用于关闭本日志本身的 staged/unstaged 不一致和可读性风险。退出标准不是“总 Goal 完成”，而是证明 `ai-rag-integration-20260613` 具备提交、推送和开 PR/MR 的候选状态。

### 复核对象

- `docs/AI_RAG_剩余修改与完善清单.md`
- `tests-log/ai-rag-automation/2026-06-14-1125-ai-rag-branch-coverage-audit.md`
- `docs/AI_RAG_配置与端口说明.md`
- `接口文档/接口文档_3_Dashboard.md`
- `接口文档/接口文档_5_Resume.md`
- `接口文档/接口文档_6_Chat.md`
- `接口文档/接口文档_7_Goals.md`
- `接口文档/接口文档_8_Roadmap.md`
- `接口文档/接口文档_9_Reports.md`
- `接口文档/接口文档_10_Notifications_Favorites_Feedback_Settings.md`
- `接口文档/接口文档_10_AI_RAG_Target_Ownership_Clarification.md`
- Obsidian 记录目录：`C:\Users\WhenJayHe\notes\study\项目使用记录\AI-University-Student-Career-Planning`

### 接口文档一致性结论

- Dashboard、Resume、Chat、Goals、Roadmap、Reports 文档均已写明 Java 对外入口、Python 内部 endpoint、超时/重试/错误映射、前端影响和测试口径。
- Interface 10 的 AI/RAG feedback/settings 文档已写明 `/api/feedback/ai-rag`、`/api/settings/ai-rag`、`/internal/rag/feedback`、`/internal/rag/preferences/validate` 的 Java-Python 边界；Target Ownership 补充文档明确了 `target_type + target_id` 归属规则。
- `docs/AI_RAG_配置与端口说明.md` 已按当前集成分支实际代码更新：Java `8081`、聚合 Python `8090`、Resume `8091`、Chat `8092`；Market 不在本集成分支声明已迁入。

### 提交前静态门禁

提交前必须重新执行并记录：

```powershell
git status --short --branch
git rev-list --left-right --count origin/master...HEAD
git log --oneline --decorate origin/master..HEAD
git diff --check
git diff --cached --check
git diff --cached --name-status
git diff --cached --name-only | rg "^(database/|ai_service/|\\.codex-temp|\\.m2-codex|\\.claude|server/src/main/resources/application.*\\.yml|website/src/views/Roadmap\\.vue|server/src/main/java/com/itsheng/service/interceptor/JwtTokenInterceptor\\.java)"
Get-Content -Raw -Encoding utf8 docs/AI_RAG_剩余修改与完善清单.md
Get-Content -Raw -Encoding utf8 tests-log/ai-rag-automation/2026-06-14-1125-ai-rag-branch-coverage-audit.md
```

验收口径：

- `AM` 状态不得直接进入提交；以 working tree 最新内容为准重新 `git add`，确保 `git diff --name-status` 无普通输出。
- `git diff --check` 和 `git diff --cached --check` 均不得有 whitespace 或 conflict-marker 错误；CRLF warning 只记录为非阻断换行提示。
- 暂存文件只允许 AI/RAG 文档、测试日志和必要 Obsidian 使用记录；不得包含旧 `ai_service/**` 分支实现、`JwtTokenInterceptor.java`、`Roadmap.vue`、`application*.yml`、`database/**`、构建产物或环境文件。
- UTF-8 读取必须可读，不得以默认编码乱码作为最终审计文件。

### Runtime smoke 风险口径

本轮静态与单测证据只能证明“AI/RAG 集成分支具备可提交、可推送、可开 PR/MR 的候选状态”。由于真实 Java `8081` + Python `8090/8091/8092` + Redis + PostgreSQL/pgvector + JWT + 前端链路尚未统一执行，本日志不得宣称合并后端到端可运行或无明显 bug。PR/MR 合并 `master` 前至少补一条已登录 JWT 的 Roadmap 或 Dashboard 调用 smoke。

## 建议下一步

1. 不再逐个 merge 旧隔离分支；以 `ai-rag-integration-20260613` 作为唯一候选集成分支。
2. 先推送集成分支到远端并开 PR/MR，避免直接污染 `master`。
3. PR/MR 前补一次 runtime smoke，至少启动 Java 8081 和 Python 8090，验证一个已登录 JWT 的 Roadmap 或 Dashboard RAG 调用。
4. 若必须合并到本地 `master`，应先从 `master` fast-forward/merge `ai-rag-integration-20260613`，再立即运行 Python/Java/frontend 全套验证；失败则回到集成分支修复，不在 `master` 上继续开发。
