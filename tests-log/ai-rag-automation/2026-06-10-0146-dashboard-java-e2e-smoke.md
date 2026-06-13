# Dashboard-RAG Java 8081 e2e smoke 日志

## 测试对象

- `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\接口文档\接口文档_3_Dashboard.md`
- `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\server\src\main\java\com\itsheng\service\service\Impl\DashboardServiceImpl.java`
- `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\server\src\main\java\com\itsheng\service\client\PythonDashboardAiClient.java`
- `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\ai_service\market_ai_service.py`
- `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\ai_service\dashboard_rag_service.py`

## 测试原因

上轮 Dashboard-RAG 已完成 Python 8090 smoke、Java 单测、Maven compile、前端 build 与监督复验，但仍未执行 Java 8081 到 Python Dashboard-AI 的真实端到端 smoke。本轮目标是补齐 Redis、PostgreSQL/pgvector、Java 8081、Python 8090、JWT 登录态同环境下的 Dashboard e2e 验证，并证明 `/api/dashboard/roadmap` 真实触发 `/internal/dashboard/target-job/match`。

## 测试环境

- 时间：2026-06-10 01:46:51 +08:00
- Worktree：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard`
- 分支：`ai-rag-dashboard-target-job-match`
- e2e 前 HEAD：`ea8edd67ef324e0e60e7a8c80abff8934e122f55`
- 临时运行目录：`C:\Users\WhenJayHe\AppData\Local\Temp\ai-rag-e2e-20260610014221`
- Java：21.0.7
- Maven：3.9.4
- Python：3.13.13
- Node：24.14.0
- Docker：29.3.1

## 测试方法与步骤

1. Git 前置断言：`git rev-parse --show-toplevel` 指向 Dashboard 隔离 worktree，`git status --porcelain` 为空，`origin/master...HEAD` 为 `0 1`。
2. 启动专用容器：`ai-rag-pgvector-e2e` 映射 `127.0.0.1:55433->5432`，`ai-rag-redis-e2e` 映射 `127.0.0.1:56379->6379`；未复用 `sub2api-*` 或本机 `5432`。
3. 容器断言：`docker inspect` 记录镜像、端口和 volume；`psql` TCP 证明目标为容器内 `ai_career_plan|ai_career_plan|127.0.0.1/32|5432`；Redis `PING=PONG`。
4. 运行 `database/init.sql`，使用 `ON_ERROR_STOP=1` 记录失败点：第 351 行重复创建 `idx_job_category`。
5. 不修改 `database/init.sql`，在隔离容器内创建临时最小 schema 和 seed。
6. 启动实际 Python 服务到 `127.0.0.1:18090`；启动临时 Node 代理监听 `127.0.0.1:8090`，只记录 path、request_id、fusion、evidence_count、matched_job_id，不保存简历正文。
7. 启动 Java 8081，显式覆盖 datasource、Redis、Python base-url、Dashboard timeout、server port、OpenAI key 占位和 AliOSS 占位，避免读取 `application-dev.yml` 远程 RDS。
8. 使用 `/api/user/login` 获取 `data.token`，以 header `token: <jwt>` 调用 `/api/user/info` 验证登录态，再调用 `/api/dashboard/roadmap`。
9. 用代理观测、DB 查询和 Java 日志共同断言 Java 到 Python Dashboard-AI 边界。
10. 安全扫描临时日志，清理本轮进程、容器和 volume。

## 测试数据与请求样例

seed 摘要：

```text
user_id=910001
username=ai_rag_e2e_dashboard
resume_analysis_id=990001
vector_store_id=e2e-resume-910001
job_ids=920001..920004
job_base_code=AI_ENGINEER
levels=INTERNSHIP,JUNIOR,MID,SENIOR
```

seed 前置断言：

```text
ASSERT_VECTOR_LINK=1
ASSERT_LATEST_RESUME=990001
ASSERT_ROADMAP_COUNT=0
ASSERT_TARGET_JOB_NULL=1
ASSERT_VERTICAL_PATH_COUNT=4
ASSERT_JSONB_TYPES=4
```

HTTP 请求：

```text
POST /api/user/login
GET /api/user/info
GET /api/dashboard/roadmap
GET /api/dashboard/summary
```

## 实际结果

Docker 与 DB：

```text
PG_IMAGE=pgvector/pgvector:pg16
PG_PORTS=5432/tcp -> 127.0.0.1:55433
PG_MOUNTS=ai-rag-pgvector-e2e-data:/var/lib/postgresql/data
REDIS_IMAGE=redis:8-alpine
REDIS_PORTS=6379/tcp -> 127.0.0.1:56379
REDIS_PING=PONG
DB_PROOF_TCP=ai_career_plan|ai_career_plan|127.0.0.1/32|5432
```

`database/init.sql` 结果：

```text
INIT_EXIT=3
psql:/tmp/init.sql:351: ERROR: relation "idx_job_category" already exists
INIT_SQL_EXPECTED_FAILURE_RECORDED
```

HTTP e2e：

```text
LOGIN_CODE=1
TOKEN_LENGTH=116
INFO_CODE=1
INFO_USERNAME=ai_rag_e2e_dashboard
ROADMAP_CODE=1
ROADMAP_TARGET_JOB_ID=920003
ROADMAP_TARGET_JOB_NAME=AI算法工程师
ROADMAP_STEPS_COUNT=4
SUMMARY_CODE=1
SUMMARY_HAS_JOB_PROFILE=True
HTTP_E2E_ASSERTIONS_PASS
```

Python 8090 代理观测：

```json
{"method":"POST","path":"/internal/dashboard/target-job/match","request_id":"dashboard-target-job-910001-990001","status":200,"code":1,"fusion_method":"rrf","evidence_count":2,"matched_job_id":920003}
```

DB 写入断言：

```text
CAREER_TARGET=920003|AI算法工程师
ROADMAP_WRITES=1
ROADMAP_STEP_COUNT=4
TARGET_IN_SEED=1
```

Java 日志证据：

```text
Tomcat started on port 8081
Python Dashboard-AI diagnostics: fusionMethod=rrf, candidateCount=25, evidenceCount=2
Python Dashboard-AI 为用户匹配目标岗位成功，userId: 910001, jobId: 920003, score: 0.99
已回填用户目标岗位到职业数据，userId: 910001, jobId: 920003
用户职业发展路径已创建，userId: 910001, roadmapId: 1
```

清理结果：

```text
PORT_55433=STOPPED
PORT_56379=STOPPED
PORT_8081=STOPPED
PORT_8090=STOPPED
PORT_18090=STOPPED
LEFT_CONTAINER=none
LEFT_VOLUME=none
```

## 失败原因与修复记录

- 首次容器清理命令对不存在容器返回非零退出码，已改为逐个容器存在性判断，只清理 `ai-rag-*e2e` 专用资源。
- 首次 DB proof 使用 Unix socket，`inet_server_port()` 为空；改为容器内 TCP `-h 127.0.0.1` 后通过。
- `database/init.sql` 因重复索引失败，按计划记录失败点并使用隔离容器临时 schema/seed，不修改数据库脚本。
- Java/Python 启动生成 `ai_service/__pycache__`，已确认位于隔离 worktree 内并删除。
- 第一次进程清理脚本误用 PowerShell 只读变量 `$PID`，已改用 `$processId` 并清理 Java、Python、Node 进程及专用 Docker 容器/volume。
- 代码审查发现临时 Java 运行日志中存在完整 JWT 输出，来源为全局 `JwtTokenInterceptor` 的 `jwt 校验:{token}` 日志。仓库内本测试日志未写入 JWT 原文；临时运行目录 `C:\Users\WhenJayHe\AppData\Local\Temp\ai-rag-e2e-20260610014221` 已删除。该 Auth 安全问题超出本轮 AI/RAG/Python 修改范围，已作为需要人工确认/后续 Auth 安全修复的风险记录，本轮不越界修改拦截器。

## 子 Agent 验收结论

- Plan 需求覆盖复审：PASS。
- Plan 技术风险复审：PASS，要求补强 Python 路径/env、最小 schema、token header、matched_job 与 DB 回填断言。
- Goal 边界审查：PASS。
- Goal 验证命令审查：前两轮 FAIL，指出 Python 侧可观测机制、Docker inspect、schema/seed、Windows 进程清理和 git/log 收口缺口；第三轮具体命令序列复审 PASS。
- 代码/行为回归审查：Dashboard-RAG 主链路无回归，但因全局 JWT 日志泄露判定 FAIL；该问题已记录为非 AI/RAG 范围的人工风险。
- 集成审查：PASS，确认 Java 8081 -> Python 8090 代理 -> Python 18090 -> `match_target_job` 的证据链充分，前端契约未变化，无需本轮 `npm run build`。
- 测试覆盖审查：PASS，确认 e2e 覆盖 Docker、DB、JWT、roadmap、Python 代理观测、DB 写入和清理；建议补充临时 JWT 日志删除说明，已处理。
- 测试日志可信度审查：初审 FAIL，原因是提交归属和 Obsidian 同步结论未闭合；本段已补充当前待提交状态和 Obsidian 同步路径，提交后需复审。

## 剩余风险

- 本轮验证的是 Dashboard-RAG deterministic fallback 端到端边界，不声明真实 Dashscope embedding/LLM、真实 pgvector 语义检索、cross-encoder/ranking model 或离线 RAG 质量评估完成。
- 本轮使用隔离容器临时 schema，不修改 `database/init.sql` 的重复索引问题；生产初始化脚本仍需单独修复。
- Java 启动使用 OpenAI 和 AliOSS 占位值，仅证明 Dashboard 路径可启动与可调用，不声明真实外部服务联调通过。
- `resume_content` 仍按当前合同截断后跨 Java/Python 边界传输，后续建议改为脱敏摘要或结构化 evidence summary。
- 全局 JWT 拦截器会在运行日志中打印完整 token，属于 Auth 安全风险；本轮只记录，不在 AI/RAG 自动化范围内修改。

## 优化建议

- 单独修复 `database/init.sql` 重复索引，并对 `job.job_profile` 与 mapper 契约做初始化脚本一致性检查。
- 为 Dashboard-AI 增加可配置的内部请求 trace 字段或脱敏访问日志，减少 e2e 需要临时代理的复杂度。
- 后续进入真实 pgvector/Dashscope 接入前，准备小型离线评估集，覆盖 context recall、context precision 和 ranking sanity。

## 关联代码、接口文档与提交

- 接口文档：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\接口文档\接口文档_3_Dashboard.md`
- Python 服务：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\ai_service\dashboard_rag_service.py`
- Java client：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\server\src\main\java\com\itsheng\service\client\PythonDashboardAiClient.java`
- Java service：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\server\src\main\java\com\itsheng\service\service\Impl\DashboardServiceImpl.java`
- 本轮 e2e 前提交：`ea8edd67ef324e0e60e7a8c80abff8934e122f55`
- 当前提交状态：本日志已准备纳入 Dashboard-RAG 本地提交 amend；在 amend 前属于待提交证据，不能作为已提交证据引用。
- 最终提交归属：仓库内日志不写固定最终 hash，避免 amend hash 回填循环；最终 hash 以提交后 `git rev-parse HEAD`、真实 Obsidian 记录和 automation memory 为准。
- Obsidian 同步：真实 Obsidian 记录 `C:\Users\WhenJayHe\notes\study\项目使用记录\AI-University-Student-Career-Planning\接口文档_3_Dashboard_RAG优化记录.md` 已包含 `2026-06-10 01:46 Dashboard Java 8081 e2e smoke` 段落，并引用本日志路径与核心 e2e 结果。
