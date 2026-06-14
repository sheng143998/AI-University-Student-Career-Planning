# Dashboard-AI 目标岗位匹配监督复验日志

## 测试对象

- `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\接口文档\接口文档_3_Dashboard.md`
- `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\ai_service\dashboard_rag_service.py`
- `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\ai_service\market_ai_service.py`
- `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\server\src\main\java\com\itsheng\service\client\PythonDashboardAiClient.java`
- `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\server\src\main\java\com\itsheng\service\service\Impl\DashboardServiceImpl.java`
- `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\server\src\main\java\com\itsheng\service\mapper\UserVectorStoreMapper.java`
- `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\server\src\main\resources\mapper\UserVectorStoreMapper.xml`
- Dashboard 相关 Python / Java 测试、接口文档、测试日志和 Obsidian 使用记录

## 测试原因

本轮自动化对隔离 worktree 中 Dashboard-AI 目标岗位匹配 Python RAG 单提交做监督复验，校准上轮日志与当前事实，确认接口文档、Java-Python 契约、Python RAG、Java 集成、测试结果、Obsidian 记录和 git 范围一致。

## 测试环境

- 时间：2026-06-09 12:49:39 +08:00
- Worktree：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard`
- 分支：`ai-rag-dashboard-target-job-match`
- 当前提交口径：`origin/master..HEAD` 的唯一 Dashboard-AI 本地提交；最终哈希以 `git log --oneline origin/master..HEAD` 为准，提交信息为 `feat: route dashboard target matching through python rag`
- 基准：`origin/master`
- 提交口径：`origin/master..HEAD` 为 1 个 Dashboard-AI 本地提交，未 push、未 merge、未开 PR
- 主工作区：仍存在大量无关脏文件，本轮未触碰

## 测试方法与命令

```powershell
git status --short --branch
git rev-list --count origin/master..HEAD
git log --oneline origin/master..HEAD
git diff --name-status origin/master..HEAD
git diff --name-only
git diff --cached --name-only
git ls-files --others --exclude-standard
git diff --check origin/master..HEAD
```

```powershell
$env:PYTHONIOENCODING='utf-8'
python -B -m pytest -q -p no:cacheprovider ai_service/test_dashboard_rag_service.py --tb=short
python -B -m unittest discover -s ai_service -p "test_dashboard_rag_service.py"
```

```powershell
mvn -pl common,pojo -am install -DskipTests
mvn -pl server -Dtest=PythonDashboardAiClientTest test
mvn -pl server -Dtest=DashboardServiceImplTest test
mvn -pl server -Dtest=DashboardControllerTest test
mvn -pl server -am -DskipTests compile
```

```powershell
python -B -m ai_service.market_ai_service --host 127.0.0.1 --port 8090
# 使用 Python 标准库向 /internal/dashboard/target-job/match 发送 UTF-8 JSON smoke 请求
```

```powershell
rg -n "log\.(warn|error|info|debug).*response\.body\(\)|log\.(warn|error|info|debug).*body=\{}|log\.(warn|error|info|debug).*requestBody|log\.(warn|error|info|debug).*resume_content|log\.(warn|error|info|debug).*resumeContent|log\.(warn|error|info|debug).*parsedData|log\.(warn|error|info|debug).*getResumeContent|log\.(trace|debug|info|warn|error).*\bretrieval\b|token|phone|email" `
  server/src/main/java/com/itsheng/service/client/PythonDashboardAiClient.java `
  server/src/main/java/com/itsheng/service/service/Impl/DashboardServiceImpl.java `
  ai_service/dashboard_rag_service.py `
  ai_service/market_ai_service.py
```

## 测试数据

Python 8090 smoke 使用最小 Dashboard-AI 请求，包含：

- `request_id=dashboard-smoke-20260609-1248`
- `resume_profile.target_role` 为嵌套对象 `{raw_text: nested-secret-role}`
- `resume_profile.skills` 包含 `Python`、`机器学习`、`RAG`、嵌套对象和 `api_key=...`
- `resume_profile.experience_years` 为非数字 `api_key=...`
- 1 个 `AI算法工程师` 岗位候选，要求 `Python`、`机器学习`、`模型部署`

该样例用于验证嵌套画像值和 token-like 敏感值不会进入 `query_variants`，同时确认返回 chunk evidence。

## 实际结果

- `python -B -m pytest -q -p no:cacheprovider ai_service/test_dashboard_rag_service.py --tb=short`：补测前 14 passed；补充 scope gate 后复跑为 16 passed。
- `python -B -m unittest discover -s ai_service -p "test_dashboard_rag_service.py"`：补测前 Ran 14 tests, OK；补充 scope gate 后复跑为 Ran 16 tests, OK。
- `mvn -pl common,pojo -am install -DskipTests`：BUILD SUCCESS。
- `mvn -pl server -Dtest=PythonDashboardAiClientTest test`：补测前 9 tests OK；补充 HTTP 400 validation 映射后复跑为 10 tests OK。
- `mvn -pl server -Dtest=DashboardServiceImplTest test`：Tests run: 8, Failures: 0, Errors: 0。
- `mvn -pl server -Dtest=DashboardControllerTest test`：Tests run: 1, Failures: 0, Errors: 0。
- `mvn -pl server -Dtest=UserVectorStoreMapperXmlTest test`：首轮因测试路径多拼 `server/` 导致 `FileNotFoundException`；修正为模块内 `src/main/resources/...` 后复跑，Tests run: 2, Failures: 0, Errors: 0。
- `mvn -pl server -am -DskipTests compile`：BUILD SUCCESS。
- Python 8090 smoke：`SMOKE_STATUS=200`、`SMOKE_CODE=1`、`SMOKE_JOB_ID=101`、`SMOKE_FUSION=rrf`、`SMOKE_EVIDENCE_COUNT=2`、`SMOKE_PII_IN_QUERIES=false`。
- smoke 停止服务后端口检查最初出现 `TimeWait` 记录，随后确认无 `Listen` 进程；本轮记录为无残留监听进程。

## 范围门禁

- `git status --short --branch`：补充日志与测试时存在预期的 Dashboard-AI 测试/日志 dirty；暂存并 amend 后必须复查为 `## ai-rag-dashboard-target-job-match...origin/master [ahead 1]` 且无业务 dirty。
- `git rev-list --count origin/master..HEAD`：`1`。
- `git log --oneline origin/master..HEAD`：唯一提交，提交信息为 `feat: route dashboard target matching through python rag`；最终哈希以 amend 后复查为准。
- `git diff --name-status origin/master..HEAD`：仅包含 Dashboard-AI allowlist 文件。
- denylist 未命中 `website/`、`database/`、`server/src/main/resources/application*.yml`、`__pycache__`、`.pytest_cache` 或构建产物。
- `git ls-files --others --exclude-standard`：补充日志和 `UserVectorStoreMapperXmlTest` 前会显示未跟踪项；暂存并 amend 后必须无输出。
- `git diff --check origin/master..HEAD` 无 whitespace error。
- `git clean -ndX` 仅预览 `common/target/`、`pojo/target/`、`server/target/` 三个 Maven ignored 产物。

## 失败原因与修复记录

- 本轮验收发现测试覆盖缺口并补齐：新增 Python scope gate 负向用例、Java client HTTP 400 validation 映射用例、UserVectorStoreMapper XML 静态合同测试；未改生产业务代码。
- `UserVectorStoreMapperXmlTest` 首轮失败原因是测试文件路径按 Maven `server` 模块工作目录解析时多拼了一层 `server`，不是 SQL 合同断言失败；已修正测试路径并复跑通过。
- Python smoke 首次端口收尾显示 `PORT_8090_REMAINING=present`，复查为 `127.0.0.1:8090 TimeWait OwningProcess=0`，无监听进程；按无残留服务进程记录。
- Java 8081 端到端 smoke 未执行，原因是 Redis、PostgreSQL/pgvector、Java 8081、Python 8090 和 `OPENAI_API_KEY` 未作为同一运行环境同时验证。本轮不声明端到端通过。
- Java 8081 端到端 smoke 属于真实运行环境验证，不以 mock/controller 单测替代；本轮继续记录为未执行。

## 子 Agent 验收结论

- Plan 需求覆盖审查：PASS。确认单提交范围、契约、SQL 归属读取、Python RAG 和 e2e 未跑边界均纳入。
- Plan 技术风险审查：PASS。确认 allowlist/denylist、接口文档与实现、测试口径和剩余风险充分。
- Goal 边界审查：PASS。确认目标限定为 Dashboard-AI RAG 复验与小补缺，不 push、不 merge、不碰主工作区。
- Goal 验证命令审查：PASS。确认 Python、Java、scope gate、smoke、日志和 Obsidian 更新口径足够。
- 代码审查：PASS。确认 Python RAG 的递归切块、三层 summary index、BM25 + embedding-like、RRF、chunk evidence、脱敏，以及 Java 触发条件、SQL 归属读取、profile 白名单和 client 错误映射均符合契约。
- 集成审查：首轮 FAIL，指出新监督日志尚未纳入提交、旧日志存在过期固定 hash；已 amend 新日志并清理旧 hash，复审 PASS。
- 测试覆盖验收：首轮 FAIL，要求补 Python scope gate、Java HTTP 400 validation、Mapper XML SQL 合同测试，并明确 Java 8081 e2e 未跑边界；已补测并复跑，复审 PASS。
- 测试日志可信度验收：PASS。确认日志覆盖对象、原因、环境、命令、测试数据、实际结果、失败修复、风险、建议和提交口径，且 `TimeWait` 端口解释可信。

## 剩余风险

- 当前 Dashboard-AI RAG 仍是 deterministic fallback，未接真实 pgvector、Dashscope embedding/LLM、cross-encoder 或 ranking model。
- Java 8081 runtime smoke 未跑，不能声明真实前后端/数据库/Python 端到端通过。
- Python 8090 是内部服务边界，若部署到非本机网络，需要网关隔离或内部鉴权。
- 主工作区仍很脏，本轮所有验证和记录均基于隔离 worktree。

## 优化建议

- 下一轮优先推进真实向量库/embedding 接入前的离线 RAG 质量评估集，至少记录 context recall / context precision / ranking sanity。
- 若需要上线 Python 8090 到内网，补充内部鉴权或网关 ACL，并把契约写入接口文档。
- 在依赖齐全时补跑 Java 8081 e2e smoke，验证 Redis、PostgreSQL/pgvector、Java、Python 和环境变量组合。

## 关联代码、接口文档和提交

- 接口文档：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\接口文档\接口文档_3_Dashboard.md`
- Python RAG：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\ai_service\dashboard_rag_service.py`
- Python 聚合入口：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\ai_service\market_ai_service.py`
- Java client：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\server\src\main\java\com\itsheng\service\client\PythonDashboardAiClient.java`
- Java service：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\server\src\main\java\com\itsheng\service\service\Impl\DashboardServiceImpl.java`
- Java mapper：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\server\src\main\java\com\itsheng\service\mapper\UserVectorStoreMapper.java`
- MyBatis XML：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\server\src\main\resources\mapper\UserVectorStoreMapper.xml`
- 当前提交：`origin/master..HEAD` 的唯一 Dashboard-AI 本地提交，提交信息为 `feat: route dashboard target matching through python rag`
