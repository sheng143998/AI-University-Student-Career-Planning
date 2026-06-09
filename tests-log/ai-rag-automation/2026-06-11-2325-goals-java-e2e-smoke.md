# Goals-RAG Java 8081 e2e Smoke 验证日志

- 运行时间：2026-06-11 23:21-23:24 +08:00（Java 8081 e2e 主链路）；补充验证命令与日志证据整理完成于 2026-06-11 23:38 +08:00
- 工作区：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-goals`
- 分支：`ai-rag-goals-advice-python-rag`
- 当前提交基线：`d7207a3d7b0b2f61646c2834c62d7bbaf7da924a`（本日志将 amend 进同一 Goals-RAG 提交）
- 测试对象：`POST /api/goals/{id}/ai-advice/generate` 通过真实 Java 8081 调用 Python `POST /internal/goals/advice`
- 测试原因：补齐 2026-06-09 日志中未覆盖的 Java 8081 登录态端到端 smoke，并验证错误映射、DB content-only、权限隔离和日志脱敏。

## 测试环境

- Windows PowerShell
- Java 21.0.7 / Maven 3.9.4
- Python 3.13
- Docker 29.3.1
- PostgreSQL/pgvector：`pgvector/pgvector:pg16` 临时容器，随机宿主端口
- Redis：`redis:7-alpine` 临时容器，随机宿主端口
- Python aggregate：`python -B -m ai_service.market_ai_service --host 127.0.0.1 --port <random>`
- Java：`java -jar server/target/server-0.0.1-SNAPSHOT.jar --server.port=8081 ...`
- Python 故障注入代理：`127.0.0.1:<random>`，转发或模拟 `400/422/504/500/slow/nonjson/empty`
- 运行证据：`C:\Users\WHENJA~1\AppData\Local\Temp\ai-rag-goals-e2e-20260611-232155\summary.json`
- 补充命令原始输出：
  - `C:\Users\WHENJA~1\AppData\Local\Temp\ai-rag-goals-e2e-20260611-232155\pytest-goals.log`
  - `C:\Users\WHENJA~1\AppData\Local\Temp\ai-rag-goals-e2e-20260611-232155\unittest-goals.log`
  - `C:\Users\WHENJA~1\AppData\Local\Temp\ai-rag-goals-e2e-20260611-232155\maven-goals-tests.log`
  - `C:\Users\WHENJA~1\AppData\Local\Temp\ai-rag-goals-e2e-20260611-232155\maven-compile.log`
  - `C:\Users\WHENJA~1\AppData\Local\Temp\ai-rag-goals-e2e-20260611-232155\scope-gate.log`
  - `C:\Users\WHENJA~1\AppData\Local\Temp\ai-rag-goals-e2e-20260611-232155\surefire-counts.json`

## 测试数据与请求样例

测试使用临时数据库最小 schema：`users`、`goal`、`goal_milestone`、`user_vector_store`、`job`、`job_vector_store`。

数据样例已脱敏：

```json
{
  "userA": "goals_e2e_user_a",
  "userB": "goals_e2e_user_b",
  "goalA": {
    "id": 70001,
    "userId": 10001,
    "title": "AI application engineer",
    "desc": "Build Goals RAG proof with <PHONE> <EMAIL> <TOKEN_LIKE>",
    "aiAdviceBefore": "OLD_ADVICE_A"
  },
  "goalB": {
    "id": 70002,
    "userId": 10002,
    "aiAdviceBefore": "OLD_ADVICE_B"
  }
}
```

Java 请求链路：

```http
POST /api/goals/70001/ai-advice/generate
token: <JWT_FROM_LOGIN>
Content-Type: application/json

{}
```

Java 到 Python 内部请求由代理观测，不记录原始 payload，只记录 `method/path/mode/status/requestLength/countAfter/containsSentinel`。

## 执行步骤与命令

1. `git status --short --branch --untracked-files=all`
   - 结果：`## ai-rag-goals-advice-python-rag...origin/master [ahead 1]`
2. `git diff --check origin/master..HEAD`
   - 结果：退出码 0，无输出。
3. `mvn -pl server -am -DskipTests package`
   - 结果：`BUILD SUCCESS`，产物 `server\target\server-0.0.1-SNAPSHOT.jar`。
4. 启动独立 Docker pgvector 与 Redis，导入临时最小 schema 与脱敏测试数据。
   - 结果：容器 ready，未复用或污染现有 `sub2api*` 容器。
5. 启动 Python aggregate、故障注入代理和 Java 8081。
   - Java 显式覆盖 datasource、Redis、Python base-url、timeout、OpenAI/AliOSS 占位配置。
   - Java 启动时将 `JwtTokenInterceptor`、`UserController`、`OssConfiguration` 日志级别调为 WARN/OFF 口径，避免本轮运行日志落盘完整敏感值。
6. 请求 `/api/user/login` 获取真实 token，再请求 `POST /api/goals/70001/ai-advice/generate`。
7. 覆盖鉴权负例、归属负例、错误映射矩阵、DB before/after、proxy sentinel、保存日志敏感扫描和 cleanup。
8. `python -B -m pytest -q -p no:cacheprovider ai_service/test_goals_rag_service.py --tb=short`
   - 结果：`12 passed in 2.70s`。
   - 原始输出：`C:\Users\WHENJA~1\AppData\Local\Temp\ai-rag-goals-e2e-20260611-232155\pytest-goals.log`。
9. `python -B -m unittest discover -s ai_service -p "test_goals_rag_service.py"`
   - 结果：`Ran 12 tests in 2.570s OK`。
   - 原始输出：`C:\Users\WHENJA~1\AppData\Local\Temp\ai-rag-goals-e2e-20260611-232155\unittest-goals.log`。
10. `mvn -pl server -am "-Dtest=PythonGoalsAdviceClientTest,GoalsServiceImplTest,GoalsControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
   - 结果：`Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`。
   - 原始输出：`C:\Users\WHENJA~1\AppData\Local\Temp\ai-rag-goals-e2e-20260611-232155\maven-goals-tests.log`。
   - Surefire 计数：`C:\Users\WHENJA~1\AppData\Local\Temp\ai-rag-goals-e2e-20260611-232155\surefire-counts.json`，`PythonGoalsAdviceClientTest=6`、`GoalsControllerTest=3`、`GoalsServiceImplTest=9`。
11. `mvn -pl server -am -DskipTests compile`
   - 结果：`BUILD SUCCESS`。
   - 原始输出：`C:\Users\WHENJA~1\AppData\Local\Temp\ai-rag-goals-e2e-20260611-232155\maven-compile.log`。
12. `cd website && npm run build`
   - 结果：`vue-tsc && vite build` 成功，`125 modules transformed`。
   - 原始输出已随 `summary.json.commands.npm_build.output` 保存。
13. `git -c core.quotePath=false diff --name-only origin/master..HEAD`
   - 结果：仍为既有 Goals-RAG allowlist 范围，未命中 `application*.yml`、`database/`、`pom.xml`、缓存或构建产物。
   - 原始输出：`C:\Users\WHENJA~1\AppData\Local\Temp\ai-rag-goals-e2e-20260611-232155\scope-gate.log`。

## 实际结果

### 成功路径

- `/api/user/login`：HTTP 200，`code=1`，返回 token。
- 无 token / 错 token：HTTP 401，`proxyDelta=0`。
- `POST /api/goals/70001/ai-advice/generate`：HTTP 200，`code=1`。
- 返回 `content` 非空，`evidenceReferences` 非空，`retrievalDiagnostics` 非空。
- `retrievalDiagnostics.metadataFilters.userId == "10001"`。
- `retrievalDiagnostics.metadataFilters.goalId == "70001"`。
- `documentTypes == ["goal", "milestone", "successCriteria"]`。
- Python 代理观测 `/internal/goals/advice` 调用 1 次。
- DB `goal.ai_advice` 精确等于响应 `content`。
- DB `goal.ai_advice` 不包含 `evidenceReferences`、`retrievalDiagnostics`、`expandedQueries`、`sourceId`。
- 其他用户目标 `ai_advice` 保持 `OLD_ADVICE_B`。

### 权限与归属负例

- 跨用户目标 `70002`：HTTP 200，`code=0`，不调用 Python，其他用户 DB 不变。
- 不存在目标 `999999`：HTTP 200，`code=0`，不调用 Python。

### 错误映射矩阵

错误映射唯一 oracle 来自 `接口文档/接口文档_7_Goals.md`，本轮不再使用旧脚本的 `msgContains` 口径。

| Proxy mode | HTTP | code | msg 精确匹配 | DB 不变 | proxyDelta |
| --- | --- | --- | --- | --- | --- |
| `400` | 200 | 0 | `目标AI建议生成失败，请检查目标上下文` | true | 1 |
| `422` | 200 | 0 | `目标AI建议生成失败，请检查目标上下文` | true | 1 |
| `504` | 200 | 0 | `目标AI建议服务超时，请稍后重试` | true | 1 |
| `slow` | 200 | 0 | `目标AI建议服务超时，请稍后重试` | true | 1 |
| `500` | 200 | 0 | `目标AI建议服务暂不可用` | true | 1 |
| `nonjson` | 200 | 0 | `目标AI建议服务暂不可用` | true | 1 |
| `empty` | 200 | 0 | `目标AI建议生成结果为空` | true | 1 |
| connection failure | 200 | 0 | `目标AI建议服务暂不可用` | true | 0 |

### 脱敏与清理

- Proxy request sentinel scan：`events=8`，`hits=0`。
- 保存日志扫描：`java.log`、`python-aggregate.log`、`proxy-events.jsonl`、`maven-package.log` 均未命中本轮真实 JWT、手机号、邮箱、口令明文、token-like、OSS secret。
- cleanup：Postgres、Redis、Python、proxy、Java 端口均未监听；临时 `ai-rag-goals-*` 容器无残留。

## 失败原因与修复记录

- 第一次 Java e2e 临时脚本失败：`DB ai_advice == content` 误判失败。原因是 Windows/psql 输出编码导致中文 DB 文本读取不一致；已把临时脚本 subprocess 输出统一按 UTF-8 解码后重跑通过。
- 第二次临时脚本失败：`mode=400` 仍使用旧 `msgContains` 口径，无法证明接口文档契约；已按文档改为精确 `Result.msg` 断言，并补 `422`。
- 第三次临时脚本失败：`slow` timeout 中 Java 已正确返回超时且 DB 未变，但代理在线程睡眠结束后才记录请求计数；已等待代理事件落盘后再断言 `proxyDelta=1`。
- 最终 e2e 运行 `20260611-232155` 通过。

## 子 Agent 验收结论

- Plan 需求覆盖初审：FAIL；原因是仓库中尚无本轮 e2e 新日志、未显式列出 400/422/DB/log/scope 等矩阵。已补执行版 Plan。
- Plan 需求覆盖复审：PASS；允许进入隔离 e2e。
- Plan 技术风险审查：静态错误映射与文档一致，收尾门禁未放行；要求重跑隔离 e2e、DB content-only、日志扫描和 scope gate。已执行。
- Goal 边界审查：边界 PASS；完成状态未达成，要求补 Java 8081 e2e 后再 amend。已执行。
- Goal 验证命令初审：FAIL；要求把 `msgContains` 改为文档驱动精确 `msg == ...`、补 `422`、Surefire tests>0、allowlist 和日志 denylist。已补强。
- Goal 验证命令复审：PASS（计划层面）；允许执行 runtime matrix。

## 剩余风险

- 本轮 Java 运行通过启动参数压低全局敏感日志类输出，但 `JwtTokenInterceptor`、`UserController`、`OssConfiguration` 等非 AI/RAG 源码层日志风险仍未修复，属于本自动化允许范围外；需要后续单独安全任务处理。
- Goals-RAG 仍是 deterministic fallback，不声明真实 pgvector 向量检索、Dashscope LLM、cross-encoder/ranking model 或离线 RAG 质量评估完成。
- 本轮 e2e 使用临时最小 schema，不修改 `database/init.sql`；全量 init 脚本潜在重复索引/生产迁移问题不在本轮 AI/RAG 范围。
- Java 复验环境为 Java 21.0.7；项目目标 Java 17 兼容性仍建议后续 CI 或本地 Java 17 复验。

## 优化建议

- 将本轮临时 e2e harness 固化为可复用的 CI smoke 脚本，并保留 Docker pgvector/Redis 隔离与日志 denylist。
- 后续把 Goals-RAG 从 deterministic fallback 推进到真实 pgvector + Dashscope embedding/LLM，并建立小型离线评估集。
- 另开非 AI/RAG 安全任务，移除或脱敏全局 JWT、登录、OSS 配置日志。

## 2026-06-12 Post-commit gate 补充说明

- 本日志顶部的 `d7207a3d7b0b2f61646c2834c62d7bbaf7da924a` 是 2026-06-11 e2e 运行与日志 amend 前的提交基线，不是最终分支 HEAD。
- 本轮 post-commit gate 执行前 HEAD 为 `3715910bc7a25d0ff9eb41c595c7a749fc3c8b91`，该 HEAD 相对 `d7207a3d7b0b2f61646c2834c62d7bbaf7da924a` 仅新增本 e2e 日志文件，无业务代码变化。
- 本轮将新增 `tests-log/ai-rag-automation/2026-06-12-1506-goals-post-commit-branch-gate.md` 并随本日志做 tests-log-only amend；amend 后最终提交 hash 会因日志证据变化而更新，最终 hash 记录在真实 Obsidian 使用记录与 automation memory 中，避免在提交内写入自引用 hash。
- 本补充不改变 2026-06-11 e2e 测试事实：Java 8081 成功链路、鉴权/归属、错误映射、DB content-only、日志脱敏、Python/Java/frontend 验证和 scope gate 仍以 2026-06-11 记录为准。

## 关联文件

- 接口文档：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-goals\接口文档\接口文档_7_Goals.md`
- AI 服务 README：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-goals\ai_service\README.md`
- Python RAG：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-goals\ai_service\goals_rag_service.py`
- Python aggregate：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-goals\ai_service\market_ai_service.py`
- Python 测试：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-goals\ai_service\test_goals_rag_service.py`
- Java VO：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-goals\pojo\src\main\java\com\itsheng\pojo\vo\AiAdviceVO.java`
- Java client：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-goals\server\src\main\java\com\itsheng\service\client\PythonGoalsAdviceClient.java`
- Java controller：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-goals\server\src\main\java\com\itsheng\service\controller\GoalsController.java`
- Java service interface：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-goals\server\src\main\java\com\itsheng\service\service\GoalsService.java`
- Java service：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-goals\server\src\main\java\com\itsheng\service\service\Impl\GoalsServiceImpl.java`
- Java AI tool：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-goals\server\src\main\java\com\itsheng\service\tool\CareerPlanningTools.java`
- Java mapper：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-goals\server\src\main\java\com\itsheng\service\mapper\GoalMapper.java`
- Java mapper：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-goals\server\src\main\java\com\itsheng\service\mapper\GoalMilestoneMapper.java`
- Mapper XML：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-goals\server\src\main\resources\mapper\GoalMapper.xml`
- Mapper XML：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-goals\server\src\main\resources\mapper\GoalMilestoneMapper.xml`
- Java 测试：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-goals\server\src\test\java\com\itsheng\service\client\PythonGoalsAdviceClientTest.java`
- Java 测试：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-goals\server\src\test\java\com\itsheng\service\controller\GoalsControllerTest.java`
- Java 测试：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-goals\server\src\test\java\com\itsheng\service\service\Impl\GoalsServiceImplTest.java`
- Frontend API：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-goals\website\src\api\goals.ts`
- 旧测试日志：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-goals\tests-log\ai-rag-automation\2026-06-09-1710-goals-ai-advice-python-rag.md`
- 本测试日志：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-goals\tests-log\ai-rag-automation\2026-06-11-2325-goals-java-e2e-smoke.md`
- Obsidian：`C:\Users\WhenJayHe\notes\study\项目使用记录\AI-University-Student-Career-Planning\接口文档_7_Goals_RAG优化记录.md`
