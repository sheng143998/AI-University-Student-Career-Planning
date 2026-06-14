# Goals AI Advice Python RAG 验证日志

- 运行时间：2026-06-09 17:10 +08:00
- 工作区：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-goals`
- 分支：`ai-rag-goals-advice-python-rag`
- 测试对象：`POST /api/goals/{id}/ai-advice/generate` 到 Python `POST /internal/goals/advice`
- 测试原因：收口 Goals AI 建议 Python RAG 边界，验证文档、Java、Python、前端 API、测试和提交范围一致。

## 测试环境

- Windows PowerShell
- Java 21.0.7 / Maven 3.9.4
- Python 3.13
- Python smoke：`python -m ai_service.market_ai_service --host 127.0.0.1 --port 8090`
- Frontend：`website` 目录已安装依赖，执行 `npm run build`

## 请求样例

```json
{
  "userId": "10001",
  "goal": {
    "id": "7",
    "title": "AI application engineer",
    "desc": "Build Python RAG evidence",
    "status": "IN_PROGRESS",
    "progress": 45,
    "eta": "2026-09",
    "isPrimary": true
  },
  "milestones": [
    {
      "id": "1",
      "title": "Finish RAG project",
      "desc": "Implement retrieval rerank evaluation",
      "status": "IN_PROGRESS",
      "progress": 40,
      "order": 1
    }
  ],
  "retrievalOptions": {
    "metadataFilters": {
      "userId": "20002",
      "goalId": "999",
      "documentTypes": ["milestone"],
      "visibilityScope": "USER_PRIVATE"
    }
  }
}
```

说明：样例故意传入伪造的 `retrievalOptions.metadataFilters.userId=20002` 与 `goalId=999`，用于验证 Python diagnostics 使用顶层可信 `userId=10001` 和 `goal.id=7`；`documentTypes=["milestone"]` 用于验证候选 evidence 真实过滤到 milestone。

## 执行命令与结果

1. `python -B -m pytest -q -p no:cacheprovider ai_service/test_goals_rag_service.py --tb=short`
   - 结果：退出码 0，`12 passed in 2.71s`。
   - 覆盖：Goals RAG contract、PII/token 过滤、metadata filter 防伪、`documentTypes` 候选过滤、HTTP handler 和旧 endpoint 404。
2. `python -B -m unittest discover -s ai_service -p "test_goals_rag_service.py"`
   - 结果：退出码 0，`Ran 12 tests in 2.592s OK`。
   - 覆盖：同 pytest 路径，作为标准库测试入口复验。
3. `mvn -pl server -am "-Dtest=PythonGoalsAdviceClientTest,GoalsServiceImplTest,GoalsControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
   - 结果：退出码 0，`Tests run: 16, Failures: 0, Errors: 0, Skipped: 0`。
   - Surefire XML：`PythonGoalsAdviceClientTest tests=6`、`GoalsControllerTest tests=2`、`GoalsServiceImplTest tests=8`。
   - 覆盖：client 200/400/500/non-json/empty/timeout，controller success/error Result，service cross-user no Python、按 id+user 更新 content、PII/token 过滤、目标更新使用 `updateByIdAndUserId`、里程碑更新使用 `updateByIdAndUserId`。
4. `mvn -pl server -am -DskipTests compile`
   - 结果：退出码 0，`BUILD SUCCESS`。
5. `cd website && npm run build`
   - 结果：退出码 0，`vue-tsc && vite build` 成功，`125 modules transformed`。
6. Python 8090 smoke
   - 命令：启动 `python -m ai_service.market_ai_service --host 127.0.0.1 --port 8090` 后 POST `/internal/goals/advice`，再 POST `/api/v1/goals/advice`。
   - 结果：退出码 0，`{"status":200,"hasContent":true,"evidenceCount":1,"retrieval":"multi_query+bm25+embedding","fusion":"rag_fusion_rrf","userId":"10001","goalId":"7","documentTypes":"milestone","evidenceSourceTypes":"milestone","legacyStatus":404}`。

## 修复记录

- 保留 Goals AI advice 最小闭环，同时修复同一路径暴露出的目标里程碑用户隔离风险，避免 AI tool/REST 在目标删除、里程碑替换、完成里程碑和 payload 构造时跨用户读取或删除。
- `GoalMilestoneMapper` 与 `CareerPlanningTools` 改为统一使用 `findByGoalIdAndUserId` / `deleteByGoalIdAndUserId`；旧 `findByGoalId` / `deleteByGoalId` 已删除。
- `GoalMapper` 本轮涉及多参数方法显式补 `@Param`，降低 MyBatis 运行时参数绑定风险；目标和里程碑通用更新均改为 `updateByIdAndUserId`，SQL 层带 `AND user_id = #{userId}`。
- `ai_service/goals_rag_service.py` 让 `documentTypes` metadata filter 真实过滤 summary records 与 evidence references，并补专项测试。
- `接口文档_7_Goals.md` 与 `ai_service/README.md` 已同步 `market_ai_service.py` 聚合入口和 `documentTypes` 真过滤口径。
- Java 到 Python 只持久化 `content`；`evidenceReferences` 与 `retrievalDiagnostics` 只在生成接口实时返回。
- 验证纠偏：`mvn -pl server clean test ...` 因缺少 `-am`，会在 clean 后使用本地仓库旧 `pojo` 产物导致找不到 `AiAdviceVO` 新 setter，判定为无效命令；随后改用 `mvn -pl server -am clean test ...` 串行重跑并通过。一次并行启动的 `mvn -pl server -am -DskipTests compile` 与 clean test 互相竞争 target 目录，出现上游类型缺失，判定为并发验证冲突；已串行重跑同一 compile 命令并通过。

## 子 Agent 验收结论

- Plan 覆盖审查：PASS。
- Plan 技术门禁审查：PASS。
- Goal 边界审查：PASS。
- Goal 退出条件审查：初审 FAIL，补充测试路径、断言范围、smoke 样例、allowlist/denylist、单提交验证和日志字段后 PASS。
- 代码/集成验收：PASS；最终审查确认旧 `/api/v1/goals/advice` 无实现残留，Java/Python/前端契约一致，旧 `findByGoalId` / `deleteByGoalId` 无残留。
- 测试覆盖/日志可信度验收：初审 FAIL；阻塞项为尚未形成单提交、Java tests 需 `git add -f`、Obsidian 与测试日志需回填提交、worktree 仍有 SQL 层用户隔离补强未提交、环境口径写成 Java 17；本轮已强制纳入测试、补齐日志、回填 Obsidian，改为 Java 21.0.7 / Maven 3.9.4 口径，并保持单个本地提交。

## 剩余风险

- Java 8081 端到端 e2e 需要 Redis、PostgreSQL/pgvector、Java 8081、Python 8090、登录/JWT 同时可用；条件不齐，本轮只声明 Python service smoke 与 Java 单元/集成边界通过。
- 当前 Goals-RAG 是 deterministic fallback，不声明真实 pgvector、Dashscope LLM、cross-encoder/ranking model 或离线 RAG 质量评估集完成。
- `npm audit` 既有依赖告警不属于本轮 AI/RAG 范围。
- 全仓库既有 `application.yml` / `application-dev.yml` 中的本地配置或 secret-like 值不属于本轮 Goals-RAG 提交范围；本轮未修改这些配置文件，敏感信息门禁只针对本提交新增/修改的 Goals RAG payload、日志与测试样例。

## 关联文件

- 接口文档：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-goals\接口文档\接口文档_7_Goals.md`
- Python：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-goals\ai_service\goals_rag_service.py`
- Java client：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-goals\server\src\main\java\com\itsheng\service\client\PythonGoalsAdviceClient.java`
- Java service：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-goals\server\src\main\java\com\itsheng\service\service\Impl\GoalsServiceImpl.java`
- Java AI tool：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-goals\server\src\main\java\com\itsheng\service\tool\CareerPlanningTools.java`
- Mapper：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-goals\server\src\main\java\com\itsheng\service\mapper\GoalMilestoneMapper.java`
- Frontend：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-goals\website\src\api\goals.ts`
- Obsidian：`C:\Users\WhenJayHe\notes\study\项目使用记录\AI-University-Student-Career-Planning\接口文档_7_Goals_RAG优化记录.md`
- 关联提交：本提交，提交信息 `feat: route goals advice through python rag`；精确哈希以 `git rev-parse HEAD` 为准，避免在同一提交内容中自引用已改变的哈希。
## 本轮自动化复验（2026-06-09 19:57 +08:00）

- 复验原因：自动化重新执行 Plan/Goal 门禁后，对已提交的 Goals AI advice Python RAG 最小闭环做退出前复验。
- 当前 HEAD：`d1c71f42c327bdbfe7166fc09d64b5162dda449f`。
- 范围门禁：`git status --short` 为空，`git diff --cached --name-status` 为空，`git diff --check origin/master..HEAD` 通过；`origin/master..HEAD` 未命中 `application*.yml`、`database/`、`pom.xml`、缓存、构建产物或编译产物 denylist。
- 测试纳入门禁：`git ls-files --stage` 确认 `ai_service/test_goals_rag_service.py`、3 个 Java 测试类和本测试日志均已被 Git 跟踪。
- Python 复验：`python -B -m pytest -q -p no:cacheprovider ai_service/test_goals_rag_service.py --tb=short`，结果 `12 passed in 2.64s`。
- Java 复验：`mvn -pl server -am "-Dtest=PythonGoalsAdviceClientTest,GoalsServiceImplTest,GoalsControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`，结果 `Tests run: 16, Failures: 0, Errors: 0, Skipped: 0`。
- Surefire 计数：`PythonGoalsAdviceClientTest tests=6 failures=0 errors=0`；`GoalsControllerTest tests=2 failures=0 errors=0`；`GoalsServiceImplTest tests=8 failures=0 errors=0`。
- 后端编译复验：`mvn -pl server -am -DskipTests compile`，结果 `BUILD SUCCESS`。
- 前端复验：`cd website && npm run build`，结果 `vue-tsc && vite build` 成功，`125 modules transformed`。
- Python 8090 smoke 复验：临时启动 `python -B -m ai_service.market_ai_service --host 127.0.0.1 --port 8090`，请求 `POST /internal/goals/advice` 返回 `status=200`、`hasContent=true`、`retrieval=multi_query+bm25+embedding`、`fusion=rag_fusion_rrf`、`userId=10001`、`goalId=7`、`documentTypes=milestone`、`evidenceSourceTypes=milestone`；旧 `POST /api/v1/goals/advice` 返回 `404`。
- Plan 子 Agent 复验：需求覆盖 PASS，技术风险 PASS。
- Goal 子 Agent 复验：边界 PASS，验证/退出条件 PASS。
- 仍未声明通过项：未运行 Java 8081 真实端到端 smoke；该项仍需 Redis、PostgreSQL/pgvector、Java 8081、Python 8090、JWT 登录态同时可用。

## 复验口径修正（2026-06-09 20:00 +08:00）

- `git status --short` 和 `git diff --cached --name-status` 为空的结论，指追加本轮复验日志前的隔离 worktree 状态。
- 追加本轮复验日志后，当前未提交变更仅限本文件：`tests-log/ai-rag-automation/2026-06-09-1710-goals-ai-advice-python-rag.md`。
- 关联提交精确哈希：`d1c71f42c327bdbfe7166fc09d64b5162dda449f`（`feat: route goals advice through python rag`）。若后续为纳入本复验日志执行 `commit --amend`，以 amend 后的新 `git rev-parse HEAD` 为准，并同步回填 Obsidian 与 automation memory。

## 优化建议

- 在 Redis、PostgreSQL/pgvector、Java 8081、Python 8090 和 JWT 登录态同时可用后，补跑真实 `POST /api/goals/{id}/ai-advice/generate` 端到端 smoke。
- 在 Java 17 环境补充复验当前 Java 测试与 `mvn -pl server -am -DskipTests compile`，避免仅依赖本轮 Java 21.0.7 结果。
- 下一轮生产化重点应从 deterministic fallback 推进到真实 pgvector、Dashscope embedding/LLM、可选 cross-encoder/ranking model 和离线 RAG 质量评估集。
- 保留 `git ls-files --stage` 门禁，防止被 `.gitignore` 忽略的 Java 测试在后续迭代中重新变成未跟踪文件。

## 代码审查反馈修复（2026-06-09 20:14 +08:00）

- 代码审查发现 `PATCH /api/goals/{id}/milestones/{ms_id}` 只按 `ms_id + user_id` 更新，未校验路径父目标 `id` 与里程碑归属一致。
- 修复范围：`GoalsController` 将 `goalId` 传入 service；`GoalsService.updateMilestone` 签名改为 `goalId + milestoneId + dto`；`GoalMilestoneMapper` 增加 `findByGoalIdAndIdAndUserId`；XML 查询增加 `goal_id = #{goalId} AND id = #{id} AND user_id = #{userId}`。
- 测试补充：`GoalsControllerTest.updateMilestonePassesGoalIdAndMilestoneIdToService` 覆盖 controller 传参；`GoalsServiceImplTest.updateMilestoneDoesNotUpdateWhenPathGoalDoesNotOwnMilestone` 覆盖路径父目标不一致时不写入。
- 复跑记录：首次 Java 复跑因新增 controller 测试混用 Mockito matcher 与原始参数失败，修复测试断言后重跑通过。
- Java 复验结果：`mvn -pl server -am "-Dtest=PythonGoalsAdviceClientTest,GoalsServiceImplTest,GoalsControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`，结果 `Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`。
- Surefire 计数：`PythonGoalsAdviceClientTest tests=6 failures=0 errors=0 skipped=0`；`GoalsControllerTest tests=3 failures=0 errors=0 skipped=0`；`GoalsServiceImplTest tests=9 failures=0 errors=0 skipped=0`。

## 最终复验与当前工作树口径（2026-06-09 20:16 +08:00）

- 当前未提交变更不再仅限本日志；代码审查修复后，未提交变更包含 `接口文档/接口文档_7_Goals.md`、`GoalsController.java`、`GoalsService.java`、`GoalsServiceImpl.java`、`GoalMilestoneMapper.java`、`GoalMilestoneMapper.xml`、`GoalsControllerTest.java`、`GoalsServiceImplTest.java` 和本测试日志。
- 这些变更均属于 Goals AI advice 链路、REST 父子资源一致性、用户隔离安全胶水、接口文档和测试日志范围；未命中 `application*.yml`、`database/`、`pom.xml`、缓存、构建产物或编译产物 denylist。
- 最终 Python 复验：`python -B -m pytest -q -p no:cacheprovider ai_service/test_goals_rag_service.py --tb=short`，结果 `12 passed in 2.82s`。
- 最终 Java 复验：`mvn -pl server -am "-Dtest=PythonGoalsAdviceClientTest,GoalsServiceImplTest,GoalsControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`，结果 `Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`。
- 最终后端编译：`mvn -pl server -am -DskipTests compile`，结果 `BUILD SUCCESS`。
- 最终前端构建：`cd website && npm run build`，结果 `vue-tsc && vite build` 成功，`125 modules transformed`。
- 最终 Python 8090 smoke：`POST /internal/goals/advice` 返回 `status=200`、`hasContent=true`、`retrieval=multi_query+bm25+embedding`、`fusion=rag_fusion_rrf`、`userId=10001`、`goalId=7`、`documentTypes=milestone`、`evidenceSourceTypes=milestone`；旧 `POST /api/v1/goals/advice` 返回 `404`。
- 仍未覆盖：Java 8081 真实端到端 smoke 仍需 Redis、PostgreSQL/pgvector、Java 8081、Python 8090 和 JWT 登录态同时可用；本轮不声明端到端通过。
- 提交口径：上述变更通过最终验收后将 amend 到 Goals-RAG 单一提交；amend 后必须以新的 `git rev-parse HEAD` 回填 Obsidian 记录和 automation memory。

## 自动化最终复验追加（2026-06-09 21:30 +08:00）

- 复验原因：`ai-rag` 自动化继续执行 Plan/Goal 门禁后，对当前已提交 Goals AI advice Python RAG 最小闭环做退出前复验。
- 当前工作区：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-goals`。
- 当前分支：`ai-rag-goals-advice-python-rag`。
- 当前提交：本日志随 `feat: route goals advice through python rag` 同一提交提交；精确哈希以最终 `git rev-parse HEAD`、Obsidian 使用记录和 automation memory 为准，避免在同一提交内容中自引用导致 hash 变化。

### Plan 与 Goal 门禁

- Plan 需求覆盖子 Agent：PASS；建议把 `pojo/src/main/java/com/itsheng/pojo/vo/AiAdviceVO.java` 明确纳入契约文件，已采纳到本轮计划口径。
- Plan 技术风险子 Agent：PASS；确认 worktree clean、`origin/master..HEAD` 只包含 Goals-RAG allowlist、被 `.gitignore` 忽略的测试已跟踪、Obsidian 已回填当前 HEAD。
- Goal 边界子 Agent：PASS；确认本轮不混入真实 pgvector、Dashscope LLM、cross-encoder 或离线评估集生产化任务。
- Goal 验证/退出条件子 Agent：PASS（有条件）；要求 Surefire 三个测试类 `tests > 0`，Python smoke 断言可信 metadata filter 和 `documentTypes` evidence 真实过滤；Java 8081 真实 e2e smoke 仍记录为环境依赖风险。

### 范围门禁

- 首次 allowlist 脚本因 Windows `git diff --name-only` 默认转义中文路径，导致 `接口文档/接口文档_7_Goals.md` 被误判为 allowlist miss；该失败为脚本路径编码问题，不是代码或提交范围问题。
- 修正命令：使用 `git -c core.quotePath=false diff --name-only origin/master..HEAD` 后重跑同一门禁。
- 结果：20 个变更文件全部在 Goals-RAG allowlist 内；未命中 `application*.yml`、`database/`、`pom.xml`、缓存、构建产物、`target/`、`dist/`、`node_modules/` denylist。
- 追加本段日志前，`git status --short` 为空；`git diff --check origin/master..HEAD` 通过。
- 追加本段日志后，当前未提交变更仅限本文件：`tests-log/ai-rag-automation/2026-06-09-1710-goals-ai-advice-python-rag.md`。该日志将 amend 进同一提交；最终精确哈希不写入本提交内部，改由 Obsidian 使用记录和 automation memory 记录。
- `git ls-files --stage` 确认以下文件已跟踪：`ai_service/test_goals_rag_service.py`、`PythonGoalsAdviceClientTest.java`、`GoalsControllerTest.java`、`GoalsServiceImplTest.java`、本测试日志。

### 复验命令与结果

1. `python -B -m pytest -q -p no:cacheprovider ai_service/test_goals_rag_service.py --tb=short`
   - 结果：退出码 0，`12 passed in 2.67s`。
2. `python -B -m unittest discover -s ai_service -p "test_goals_rag_service.py"`
   - 结果：退出码 0，`Ran 12 tests in 2.563s OK`。
3. `mvn -pl server -am "-Dtest=PythonGoalsAdviceClientTest,GoalsServiceImplTest,GoalsControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
   - 结果：退出码 0，`Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`。
   - Surefire XML 计数门禁：`PythonGoalsAdviceClientTest tests=6 failures=0 errors=0 skipped=0`；`GoalsControllerTest tests=3 failures=0 errors=0 skipped=0`；`GoalsServiceImplTest tests=9 failures=0 errors=0 skipped=0`。
4. `mvn -pl server -am -DskipTests compile`
   - 结果：退出码 0，`BUILD SUCCESS`。
5. `cd website && npm run build`
   - 结果：退出码 0，`vue-tsc && vite build` 成功，`125 modules transformed`。
6. Python 8090 internal smoke
   - 请求样例：沿用本文前面的 “请求样例”，其中 `retrievalOptions.metadataFilters.userId=20002`、`goalId=999` 为故意伪造值，`documentTypes=["milestone"]` 用于验证证据类型过滤。
   - 首次脚本失败原因：当前 PowerShell 不支持 `Invoke-WebRequest -SkipHttpErrorCheck`，未执行到业务断言；已删除该参数并用异常捕获读取旧路径 404 后重跑。
   - 最终命令：临时启动 `python -B -m ai_service.market_ai_service --host 127.0.0.1 --port 8090`，POST `/internal/goals/advice`，并 POST 旧 `/api/v1/goals/advice`。
   - 结果：退出码 0，`{"status":200,"hasContent":true,"evidenceCount":1,"retrieval":"multi_query+bm25+embedding","fusion":"rag_fusion_rrf","userId":"10001","goalId":"7","documentTypes":"milestone","evidenceSourceTypes":"milestone","legacyStatus":404}`。
   - 断言：伪造的 `retrievalOptions.metadataFilters.userId=20002` 与 `goalId=999` 未污染 diagnostics；Python 使用顶层可信 `userId=10001` 与 `goal.id=7`；`documentTypes=["milestone"]` 真实过滤 evidence sourceType。

### 本轮结论

- Goals AI advice Python RAG 最小闭环在隔离 worktree 内通过：接口文档、Python internal API、Java client/service/controller/mapper 安全胶水、`AiAdviceVO`、前端 goals API、测试、测试日志和 Obsidian 记录一致。
- 当前仍只能声明 Python service smoke、Java 单元/边界集成、后端 compile、前端 build 通过；不声明 Java 8081 真实端到端 e2e 通过。
- 剩余环境风险：Java 8081 真实 `POST /api/goals/{id}/ai-advice/generate` e2e smoke 仍需要 Redis、PostgreSQL/pgvector、Java 8081、Python 8090、JWT 登录态同时可用。
