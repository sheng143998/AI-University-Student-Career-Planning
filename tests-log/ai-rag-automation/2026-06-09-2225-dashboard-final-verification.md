# Dashboard-AI 目标岗位匹配最终验证日志

## 测试对象

- `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\接口文档\接口文档_3_Dashboard.md`
- `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\ai_service\dashboard_rag_service.py`
- `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\ai_service\market_ai_service.py`
- `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\server\src\main\java\com\itsheng\service\client\PythonDashboardAiClient.java`
- `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\server\src\main\java\com\itsheng\service\service\Impl\DashboardServiceImpl.java`
- `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\server\src\main\resources\mapper\UserVectorStoreMapper.xml`

## 测试原因

Obsidian Dashboard RAG 记录已引用本日志路径，但隔离 worktree 中缺少该文件。本轮补齐日志并基于真实隔离 worktree 重新执行最终验证，确认接口文档、Python endpoint、Java 胶水、测试结果、范围门禁和 Obsidian 引用一致。

## 测试环境

- 时间：2026-06-09 23:04 +08:00
- Worktree：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard`
- 分支：`ai-rag-dashboard-target-job-match`
- 执行前快照：本日志补建前的 Dashboard-RAG 单提交；最终提交 hash 会随 amend 改变，仓库内日志不固定该 hash
- 提交信息：`feat: route dashboard target matching through python rag`
- Python：3.13.13
- Maven：3.9.4
- Java：21.0.7，编译目标 release 17
- Node：v24.14.0
- npm：11.9.0
- `website/node_modules/.bin/vue-tsc.cmd` 已存在，本轮未执行 `npm ci`
- 最终提交 hash：以本轮 amend 完成后的 `git rev-parse HEAD` 为准，仓库内日志不写固定最终 hash，避免 amend hash 循环

## 测试命令

```powershell
git rev-parse --show-toplevel
git merge-base --is-ancestor origin/master HEAD
git rev-list --count origin/master..HEAD
git log --oneline origin/master..HEAD
git -c core.quotePath=false diff --name-only origin/master...HEAD
git diff --check origin/master...HEAD
git ls-files --others --exclude-standard
```

```powershell
python -B -m py_compile ai_service/dashboard_rag_service.py ai_service/market_ai_service.py
python -B -m pytest -q -p no:cacheprovider ai_service/test_dashboard_rag_service.py --tb=short
python -B -m unittest discover -s ai_service -p "test_dashboard_rag_service.py"
```

```powershell
mvn -pl common,pojo -am clean install -DskipTests
mvn -pl server -Dtest=PythonDashboardAiClientTest test
mvn -pl server -Dtest=DashboardServiceImplTest test
mvn -pl server -Dtest=DashboardControllerTest test
mvn -pl server -Dtest=UserVectorStoreMapperXmlTest test
mvn -pl server "-Dtest=PythonDashboardAiClientTest,DashboardServiceImplTest,DashboardControllerTest,UserVectorStoreMapperXmlTest" test
mvn -pl server -am -DskipTests compile
```

```powershell
cd website
npm run build
```

```powershell
# Python 8090 smoke
# 启动前确认 8090 无 LISTEN；启动本次 Python PID；
# 用 Python urllib 请求 /internal/dashboard/target-job/match；
# finally 只停止本次 PID，停止后确认 8090 无 LISTEN。
```

## 测试数据

Python 8090 smoke 使用 Dashboard-AI 最小请求：

- `request_id=dashboard-smoke-20260609-2308`
- `user_id=10001`
- `resume_profile.target_role` 使用嵌套对象 `{raw_text: nested-secret-role}`
- `resume_profile.skills` 包含 `Python`、`机器学习`、`RAG`、嵌套对象和 `api_key=<redacted-test-key>`
- `resume_profile.experience_years` 使用非数字 token-like 字符串
- `resume_content` 包含 `SENTINEL_RAW_RESUME_SHOULD_NOT_APPEAR_IN_QUERY_VARIANTS`、邮箱、手机号和 token 哨兵
- `job_candidates` 包含 `job_id=101` 的 `AI算法工程师`
- `filters.document_type=["resume","job","jd"]`
- `filters.visibility_scope="user_or_public"`
- `filters.language="zh-CN"`
- `top_k=1`

## 实际结果

- `git rev-parse --show-toplevel`：`C:/Users/WhenJayHe/IdeaProjects/AI-University-Student-Career-Planning-ai-rag-dashboard`，隔离 worktree 正确。
- `git merge-base --is-ancestor origin/master HEAD`：通过。
- `git rev-list --count origin/master..HEAD`：`1`。
- `git log --oneline origin/master..HEAD`：唯一提交信息为 `feat: route dashboard target matching through python rag`；具体 hash 以 amend 后 `git rev-parse HEAD` 为准，不写入仓库内日志以避免 hash 回填循环。
- `git diff --check origin/master...HEAD`：无输出。
- 初始 `git ls-files --others --exclude-standard`：Python py_compile 产生 `ai_service/__pycache__/*.pyc`，已确认在隔离 worktree 内并清理；清理后无输出。
- Allowlist/Denylist：当前 `origin/master...HEAD` 仅包含 Dashboard-RAG allowlist 文件；未命中 `website/`、`database/`、`server/src/main/resources/application*.yml`、`ai-service/`、缓存或构建产物。注意 `ai_service/**` 下划线目录是本轮允许的旧 Python 服务路径，`ai-service/**` 连字符目录禁止纳入。
- `python -B -m py_compile ai_service/dashboard_rag_service.py ai_service/market_ai_service.py`：通过。
- `python -B -m pytest -q -p no:cacheprovider ai_service/test_dashboard_rag_service.py --tb=short`：`16 passed in 1.70s`。
- `python -B -m unittest discover -s ai_service -p "test_dashboard_rag_service.py"`：`Ran 16 tests`，`OK`。
- `mvn -pl common,pojo -am clean install -DskipTests`：`BUILD SUCCESS`。
- `mvn -pl server -Dtest=PythonDashboardAiClientTest test`：10 tests，0 failures/errors/skipped。
- `mvn -pl server -Dtest=DashboardServiceImplTest test`：8 tests，0 failures/errors/skipped。
- `mvn -pl server -Dtest=DashboardControllerTest test`：1 test，0 failures/errors/skipped。
- `mvn -pl server -Dtest=UserVectorStoreMapperXmlTest test`：2 tests，0 failures/errors/skipped。
- `mvn -pl server "-Dtest=PythonDashboardAiClientTest,DashboardServiceImplTest,DashboardControllerTest,UserVectorStoreMapperXmlTest" test`：21 tests，0 failures/errors/skipped。
- `mvn -pl server -am -DskipTests compile`：`BUILD SUCCESS`。
- `npm run build`：通过，Vite `125 modules transformed`。

## Smoke 结果

首次 smoke 使用 `filters.visibility_scope="user_private"`，服务可达且 `PII_IN_QUERIES=false`，但返回 `code=0`、`evidence_count=0`。复核后确认这是测试数据错误：当前合同和单测 `test_scope_filter_blocks_public_job_evidence` 明确 `user_private` 会过滤公开岗位 evidence，预期为 `NO_MATCH`。

修正为 `filters.visibility_scope="user_or_public"` 后复跑通过：

- `HTTP_200=true`
- `CODE_1=true`
- `JOB_101=true`
- `FUSION_RRF=true`
- `EVIDENCE_NONEMPTY=true`
- `PII_IN_QUERIES_FALSE=true`
- `SMOKE_STATUS=200`
- `SMOKE_CODE=1`
- `SMOKE_JOB_ID=101`
- `SMOKE_FUSION=rrf`
- `SMOKE_EVIDENCE_COUNT=2`
- `SMOKE_PII_IN_QUERIES=false`
- 停止本次 Python PID 后 `PORT_8090_LISTEN_AFTER=false`

## 安全扫描

生产代码日志扫描未发现完整 request/response body、`resume_content`、`resumeContent`、`parsedData`、完整 `retrieval` 或完整 diagnostics 输出。命中项仅为敏感值过滤正则与 `tokenize` 函数命名，不是日志泄露。

## 子 Agent 验收结论

- Plan 需求覆盖审查：PASS，要求先补齐 2225 日志并在 amend 后回填仓库外记录。
- Plan 技术风险审查：PASS，要求区分允许的 `ai_service/**` 和禁止的 `ai-service/**`，并继续记录 Java 8081 e2e 未执行。
- Goal 边界审查：PASS，确认目标限定在隔离 worktree Dashboard-RAG 收尾，不 push、不 merge、不开 PR。
- Goal 验证命令审查：初审 FAIL，要求补强 worktree toplevel、allowlist/denylist、8090 smoke finally、npm 依赖前提和 hash 回填规则；二版修正后 PASS。
- 代码审查、集成审查、测试覆盖验收、日志可信度验收将在本日志纳入单提交并完成最终门禁后再次执行。

## 剩余风险

- Java 8081 真实端到端 smoke 未执行，原因是 Redis、PostgreSQL/pgvector、Java 8081、Python 8090、`OPENAI_API_KEY` 和 JWT 登录态未作为同一运行环境同时验证；本轮不声明端到端通过。
- 当前 Dashboard-RAG 仍是 deterministic fallback，未接真实 pgvector、Dashscope embedding/LLM、cross-encoder/ranking model 或离线 RAG 质量评估集。
- Python 8090 是内部服务边界；若部署到非本机或非内网环境，需要网关 ACL 或 internal token。
- 顶层 `resume_content` 仍按合同截断后发送给 Python；后续建议改为脱敏摘要或结构化能力证据。
- `website/node_modules/` 和 `website/dist/` 是前端验证产生或复用的 ignored 产物，禁止纳入提交。

## 优化建议

- 下一轮优先补 Java 8081 e2e smoke 条件编排，验证 Redis、PostgreSQL/pgvector、Java、Python 和登录态组合。
- 为真实 pgvector、Dashscope embedding/LLM、ranking model 接入前准备 Dashboard-RAG 离线评估集，至少覆盖 context recall、context precision 和 ranking sanity。
- 将 `resume_content` 逐步替换为脱敏摘要或分段 evidence summary，降低 Python 边界敏感正文暴露面。

## 关联提交口径

本日志将纳入 `origin/master..HEAD` 的唯一 Dashboard-RAG 本地提交。最终提交 hash 以 amend 完成后的 `git rev-parse HEAD` 为准；本仓库内日志不写固定最终 hash，避免 commit hash 回填循环。
