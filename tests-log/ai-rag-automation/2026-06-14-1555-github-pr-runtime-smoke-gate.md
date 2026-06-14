# 2026-06-14 15:55 GitHub PR runtime smoke gate

## 测试对象

- GitHub PR: `https://github.com/sheng143998/AI-University-Student-Career-Planning/pull/1`
- 分支: `ai-rag-integration-20260613`
- 基线: `origin/master` = `313b9be fix: tighten ai rag feedback ownership`
- 范围: AI/RAG Python 服务、Java-Python 调用边界、GitHub 合并前 runtime smoke gate

## Canonical 结论

Python 8090/8091/8092 smoke 通过；Java 8081 首次因 Redis/env 与远端 PostgreSQL DNS 失败；本地临时 pgvector+Redis+最小种子+JWT Roadmap smoke 通过 HTTP 200/code=1/ragDiagnostics.fusion=rrf；PR #1 保持 draft，等待审查/CI/人工确认。

## 测试原因

Plan 与 Goal 交叉审查要求补齐 GitHub PR #1 的运行门禁证据，避免把 GitHub 的 `mergeable=MERGEABLE` 误判成可以直接合并到 `master`。本轮只整理 GitHub，不处理 GitLab，不修改业务代码。

## 测试环境

- Windows 11 / PowerShell
- Python 3.13.13
- Java 21.0.7
- Maven 3.9.4
- Node v24.14.0 / npm 11.9.0
- Docker 可用
- `origin`: GitHub `sheng143998/AI-University-Student-Career-Planning`
- PR 状态: open, draft, mergeable

## 既有静态与单元验证证据

已记录在 `tests-log/ai-rag-automation/` 的当前集成分支证据：

- Python `ai-service` tests: `67 passed`
- Java server tests: `88 tests`, 0 failures/errors/skipped
- Roadmap narrow Java tests: `19 tests`, 0 failures/errors/skipped
- Frontend `npm run build`: passed, 125 modules transformed
- `git diff --check origin/master..HEAD`: passed

本轮没有修改业务代码，因此不重跑全量业务测试；这些既有测试证据只作为 PR 运行门禁背景，不能替代 GitHub CI、人工审查或最终 ready 判断。

## 测试方法与步骤

### 1. GitHub 与静态状态

执行：

```powershell
git status --short --branch
git rev-list --left-right --count origin/master...HEAD
git diff --check origin/master..HEAD
gh pr view 1 --json state,isDraft,mergeable,mergeStateStatus,statusCheckRollup,reviewDecision,headRefName,baseRefName,url
```

实际结果：

- 集成 worktree: `## ai-rag-integration-20260613...origin/ai-rag-integration-20260613`
- 分支相对 `origin/master`: `0 15`
- `git diff --check origin/master..HEAD`: 无输出
- PR #1: `state=OPEN`, `isDraft=true`, `mergeable=MERGEABLE`, `mergeStateStatus=CLEAN`, `statusCheckRollup=[]`

判定：GitHub 分支层面可合并，但 checks 为空，PR 必须继续保持 draft，不能标记 ready。

### 2. Python 8090 聚合服务 smoke

启动：

```powershell
$env:PYTHONPATH='ai-service'
$env:AI_SERVICE_HOST='127.0.0.1'
$env:AI_SERVICE_PORT='8090'
python -m app.main
```

请求：

```powershell
GET http://127.0.0.1:8090/health
POST http://127.0.0.1:8090/api/v1/chat/complete
POST http://127.0.0.1:8090/internal/dashboard/target-job/match
```

实际结果：

- `/health`: HTTP 200, `{"status": "ok"}`
- `/api/v1/chat/complete`: HTTP 200，返回 `content`、`suggestionQuestions`、`evidence`、`diagnostics.expandedQueries`
- `/internal/dashboard/target-job/match`: HTTP 200，返回 `code=1`、`matched_job`、`retrieval.fusion_method=rrf`、`evidence_refs`

### 3. Python 8091 Resume 服务 smoke

启动：

```powershell
$env:PYTHONPATH='ai-service'
python -m career_ai.resume_analysis_service --host 127.0.0.1 --port 8091
```

请求：

```powershell
POST http://127.0.0.1:8091/api/v1/resume/analyze
```

测试数据为合成简历片段，包含目标岗位和 Python/RAG/PostgreSQL 技能，不含真实用户简历。

实际结果：

- HTTP 200
- 返回 `status=completed`
- 返回 `parsed_data`、`scores`、`capability_profile`、`rag_diagnostics`
- `rag_diagnostics.retrieval.fusion=rrf`
- `rag_diagnostics.sensitive_text_included=false`

### 4. Python 8092 Chat 服务 smoke

启动：

```powershell
$env:PYTHONPATH='ai-service'
$env:AI_SERVICE_HOST='127.0.0.1'
$env:AI_SERVICE_PORT='8092'
python -m app.main
```

请求：

```powershell
GET http://127.0.0.1:8092/health
POST http://127.0.0.1:8092/api/v1/chat/complete
```

实际结果：

- `/health`: HTTP 200, `{"status": "ok"}`
- `/api/v1/chat/complete`: HTTP 200，返回 `content`、`evidence`、`diagnostics.retrieval=bm25+embedding`、`diagnostics.fusion=rag-fusion`

### 5. Java 8081 Roadmap smoke：首次失败

第一次启动：

```powershell
mvn -pl server spring-boot:run
```

实际结果：

- 失败于 Spring Boot Redis 配置绑定。
- 直接原因：`FUCHUANG_REDIS_DATABASE` 未设置，`spring.data.redis.database` 无法转换为整数。
- Java 8081 未监听。

第二次尝试：

- 使用临时 Redis 容器绑定 `127.0.0.1:6379`。
- 使用临时本地环境变量补齐 Redis、OSS、OpenAI 和 Python base URL。
- 启动 Python 8090。
- 启动 Java 8081。
- 使用本地生成的测试 JWT 调用：

```powershell
GET http://127.0.0.1:8081/api/roadmap/recommendations/personalized
Header `token`: `<local-smoke-jwt>`
```

实际结果：

- Java 8081 启动成功。
- 请求通过 JWT 拦截器并进入 `RoadmapController.getPersonalizedRecommendations`。
- 运行在查询 `JobCategoryMapper.selectAll` 时失败。
- 失败原因：当前 `application-dev.yml` 指向的远端 PostgreSQL 主机 DNS 无法解析，导致 JDBC connection 获取失败。

判定：该失败属于默认 dev 配置依赖外部数据库的环境阻塞，不是 Roadmap API 逻辑回归的充分证据。

### 6. Java 8081 Roadmap smoke：本地 pgvector 复验通过

在本地临时环境中补齐可复现 smoke 依赖后，Java 路径最终打通：

- 使用临时 `pgvector/pg16` 容器在 `127.0.0.1:5433` 提供 PostgreSQL/pgvector。
- 使用临时 `redis:8-alpine` 容器在 `127.0.0.1:6379` 提供 Redis。
- 对临时 PostgreSQL 执行 `database/alter_job_add_profile.sql`，并插入最小 `job` 与 `resume_analysis_result` 种子数据。
- 启动 Java `8081` 后，使用本地生成的测试 JWT 调用 `GET /api/roadmap/recommendations/personalized`。

最终结果：

- Java `8081` 返回 HTTP 200。
- 响应体返回 `code=1`。
- 响应体包含 `currentJob`、`verticalPath`、`lateralPaths`、`generatedAt`、`ragDiagnostics`。
- `ragDiagnostics.fusion=rrf`。
- `lateralPaths` 的 `evidence` 与 `pathNodes` 均返回了可复查的 Python Roadmap-RAG 证据。

## 清理结果

- 本轮临时 Python 8090/8091/8092 进程已停止。
- 本轮临时 Redis 与 pgvector 容器已删除。
- 本轮集成 worktree `.codex-temp-smoke/` 临时目录已删除，不纳入提交。
- `.codex-temp-init.sql` 已删除且不在 git diff 中。

## 归档说明

- 本轮门禁只面向 GitHub PR #1 和 `origin/master..HEAD` 的当前 diff，不处理 GitLab。
- 仓库基线中存在的历史配置痕迹不属于本轮新增内容；secret scan 只用于审计当前 PR diff 和本轮待提交补丁是否新增敏感值。
- diff-only secret scan 可能命中 `sk-...`、`token=...`、`OPENAI_API_KEY=False` 等测试占位、命令模板或历史日志描述；这些不代表本轮 PR 引入真实凭据，仍需在提交前逐项解释。

## 子 Agent 验收结论

- Plan 白名单审查：PASS。当前状态符合白名单边界，可以进入实现。
- Goal 运行门禁初审：FAIL。要求补明确可执行运行门禁定义。
- 本日志已补充运行门禁定义：既有 Python/Java/前端测试证据、本地 Python endpoint smoke、本地 Java Roadmap runtime smoke、GitHub PR mergeable 判定字段。

## 剩余风险

- PR #1 必须继续保持 draft，不能合并到 `master`。
- `statusCheckRollup=[]`，GitHub 当前没有 CI checks 作为 ready 依据。
- 当前多数 RAG 仍为 deterministic fallback，不声明生产级 pgvector、Dashscope、cross-encoder 或离线质量评估完成。
- 旧隔离分支不应直接进入 `master`；只以 `ai-rag-integration-20260613` 作为 GitHub 候选集成分支。

## 优化建议

1. 为本地 smoke 增加安全的 `application-smoke.yml` 或 Docker Compose，使用本地 PostgreSQL/pgvector 和 Redis，不依赖个人远端数据库。
2. 给 GitHub PR 配置 CI，至少运行 Python tests、Maven tests 和 frontend build。
3. 合并前新增固定 smoke 脚本：启动 Python 8090/8091/8092、启动 Java 8081、生成测试 JWT、调用 Roadmap 或 Dashboard API 并断言 Python 证据字段。

## 关联代码/文档/提交

- `ai-service/app/main.py`
- `ai-service/career_ai/resume_analysis_service.py`
- `server/src/main/java/com/itsheng/service/client/PythonRoadmapRagClient.java`
- `server/src/main/java/com/itsheng/service/service/Impl/RoadmapServiceImpl.java`
- `接口文档/接口文档_3_Dashboard.md`
- `接口文档/接口文档_8_Roadmap.md`
- `docs/AI_RAG_配置与端口说明.md`
- `docs/AI_RAG_剩余修改与完善清单.md`
- GitHub PR #1: `https://github.com/sheng143998/AI-University-Student-Career-Planning/pull/1`
