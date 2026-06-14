# Dashboard-RAG 自动化监督复验日志

## 测试对象

- `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\接口文档\接口文档_3_Dashboard.md`
- `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\ai_service\dashboard_rag_service.py`
- `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\ai_service\market_ai_service.py`
- `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\server\src\main\java\com\itsheng\service\client\PythonDashboardAiClient.java`
- `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\server\src\main\java\com\itsheng\service\service\Impl\DashboardServiceImpl.java`
- `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\server\src\main\resources\mapper\UserVectorStoreMapper.xml`

## 测试原因

本轮自动化对 Dashboard-RAG 本地提交做监督验收。Goal 验证命令子 Agent 初审指出退出门禁不闭合：Python 验证产生未跟踪 `__pycache__`、allowlist/denylist 需要可执行断言、Python 8090 smoke 需要明确断言、日志泄露扫描和 Java 8081 e2e 未执行条件需要记录。本日志记录修正后的复验结果。最终提交 hash 会随本日志纳入提交而改变，因此仓库内日志不固定最终 hash，以 `git rev-parse HEAD`、Obsidian 使用记录和 automation memory 为准。

## 测试环境

- 时间：2026-06-10 00:09:48 +08:00
- Worktree：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard`
- 分支：`ai-rag-dashboard-target-job-match`
- 复验前 HEAD：Dashboard-RAG 单提交，最终 hash 以 amend 后 `git rev-parse HEAD` 为准。
- Maven：3.9.4
- Python：使用本机 `python`
- Frontend：`website/node_modules` 已存在，执行 `npm run build`

## 测试方法与命令

```powershell
git status --short
git rev-list --left-right --count origin/master...HEAD
git log --oneline origin/master..HEAD
git diff --name-status origin/master..HEAD
git diff --check origin/master...HEAD
git ls-files --others --exclude-standard
```

执行可断言 allowlist/denylist 脚本，允许范围限定为 Dashboard 接口文档、`ai_service` Dashboard 服务与 8090 聚合入口、Java Dashboard client/config/controller/service/mapper、Dashboard 测试和 `tests-log/ai-rag-automation/*.md`；禁止 `website/`、`database/`、`application*.yml`、`ai-service/`、`__pycache__`、`.pyc`、`target/`、`dist/`、`build/`、`cache/` 进入 diff 或未跟踪区。

```powershell
python -B -m py_compile ai_service/dashboard_rag_service.py ai_service/market_ai_service.py
python -B -m pytest -q -p no:cacheprovider ai_service/test_dashboard_rag_service.py --tb=short
python -B -m unittest discover -s ai_service -p "test_dashboard_rag_service.py"
mvn -pl common,pojo -am clean install -DskipTests
mvn -pl server "-Dtest=PythonDashboardAiClientTest,DashboardServiceImplTest,DashboardControllerTest,UserVectorStoreMapperXmlTest" test
mvn -pl server -am -DskipTests compile
cd website
npm run build
```

日志泄露扫描：

```powershell
rg -n "log\.(info|debug|warn|error).*?(payloadJson|response\.body|resume_content|resumeContent|parsedData|query_variants|selected_evidence_ids|diagnostics)" server/src/main/java ai_service
```

Java 8081 e2e 前置条件检查：

```powershell
Get-NetTCPConnection -LocalPort 5432,6379,8081,8090 -State Listen
$env:OPENAI_API_KEY
```

## 测试数据与请求样例

Python 8090 smoke 使用 `POST /internal/dashboard/target-job/match`，关键字段如下：

```json
{
  "request_id": "dashboard-smoke-20260610-0008",
  "user_id": 10001,
  "resume_analysis_id": 20001,
  "resume_vector_store_id": "resume_vec_20001",
  "resume_profile": {
    "target_role": {"raw_text": "nested-secret-role"},
    "skills": ["Python", "机器学习", "RAG", {"raw_text": "nested-secret-skill"}, "[REDACTED_API_KEY_PATTERN]"],
    "experience_years": "token-like-abcdef1234567890abcdef"
  },
  "resume_content": "[REDACTED_RAW_RESUME_WITH_EMAIL_PHONE_TOKEN_SENTINELS] 使用 Python 构建 RAG 检索系统，包含 BM25、RRF 和模型部署。",
  "job_candidates": [
    {
      "job_id": 101,
      "job_name": "AI算法工程师",
      "job_category_code": "AI_ENGINEER_JUNIOR",
      "job_level": "JUNIOR",
      "job_level_name": "初级",
      "required_skills": ["Python", "机器学习", "模型部署"],
      "job_description": "负责模型训练、评估、RAG 应用和服务部署。",
      "job_profile": {"industrySegment": "人工智能"}
    }
  ],
  "filters": {
    "document_type": ["resume", "job", "jd"],
    "visibility_scope": "user_or_public",
    "language": "zh-CN"
  },
  "top_k": 1
}
```

## 实际结果

- `git rev-list --left-right --count origin/master...HEAD`：`0 1`
- `git log --oneline origin/master..HEAD`：Dashboard-RAG 单提交，提交信息为 `feat: route dashboard target matching through python rag`，最终 hash 以本日志纳入提交后的 `git rev-parse HEAD` 为准。
- 清理 `ai_service/__pycache__` 后，`git status --short` 与 `git ls-files --others --exclude-standard` 均无输出。
- allowlist/denylist：`ALLOWLIST_DENYLIST_PASS`、`TRACKED_FORBIDDEN_PASS`、`UNTRACKED_FORBIDDEN_PASS`。
- `git diff --check origin/master...HEAD`：无输出。
- Python `py_compile`：通过。
- Python `pytest`：`16 passed in 1.69s`。
- Python `unittest`：`Ran 16 tests`，`OK`。
- `mvn -pl common,pojo -am clean install -DskipTests`：`BUILD SUCCESS`。
- Dashboard Java 指定测试：`Tests run: 21, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- `mvn -pl server -am -DskipTests compile`：`BUILD SUCCESS`。
- `npm run build`：通过，Vite `125 modules transformed`。

Python 8090 smoke 断言：

```text
HTTP_200=true
CODE_1=true
JOB_101=true
FUSION_RRF=true
EVIDENCE_NONEMPTY=true
PII_IN_QUERIES_FALSE=true
SMOKE_STATUS=200
SMOKE_CODE=1
SMOKE_JOB_ID=101
SMOKE_FUSION=rrf
SMOKE_EVIDENCE_COUNT=2
PORT_8090_LISTEN_AFTER=false
```

Java 8081 e2e 前置条件：

```text
PORT_5432=LISTEN
PORT_6379=NO_LISTEN
PORT_8081=NO_LISTEN
PORT_8090=NO_LISTEN
OPENAI_API_KEY=NOT_SET
```

因此本轮不声明 Java 8081 端到端通过。

## 失败原因与修复记录

- Goal 验证命令初审 FAIL 的直接原因是 `python -m py_compile` 生成 `ai_service/__pycache__`，退出门禁缺少干净状态断言。已在隔离 worktree 内删除该缓存，并增加 `git ls-files --others --exclude-standard` 与 forbidden pattern 断言。
- 初审指出 smoke 断言不足，已补充 HTTP、业务 code、匹配岗位、RRF、evidence 非空和 query variants 脱敏断言。
- 初审指出日志可信度需要扫描。扫描命中 `PythonDashboardAiClient.java` 的 debug diagnostics 日志，只记录 `fusionMethod`、`candidateCount`、`evidenceCount`，属于允许的脱敏摘要。另命中既有非 Dashboard `ResumeOcrServiceImpl.java` OCR response body 日志，非本轮 diff，记录为残余风险，不在本轮越界修复。

## 子 Agent 验收结论

- Plan 需求覆盖审查：PASS，建议把 `ai_service/README.md` 与 `ai_service/__init__.py` 纳入 allowlist。
- Plan 技术风险审查：PASS，确认主工作区很脏但本轮隔离 worktree 闭合，后续只能从隔离 worktree 操作。
- Goal 边界审查：PASS，要求明确本轮是验收既有本地提交，不 push/merge。
- Goal 验证命令审查：初审 FAIL；本日志记录已补齐的 clean-state、allowlist/denylist、smoke、日志扫描和 e2e 前置条件。

## 剩余风险

- Java 8081 真实 e2e smoke 未执行，缺 Redis 6379、Java 8081 常驻服务、Python 8090 常驻服务、`OPENAI_API_KEY` 与 JWT 登录态组合；本轮不声明端到端通过。
- Dashboard-RAG 仍为 deterministic fallback，未接真实 pgvector、Dashscope embedding/LLM、cross-encoder/ranking model 或离线 RAG 质量评估集。
- Python 8090 若部署到非本机/非内网，需要网关 ACL 或 internal token。
- 顶层 `resume_content` 仍按 4000 字符截断后传入 Python，后续建议改为脱敏摘要或结构化 evidence summary。
- 既有非 Dashboard `ResumeOcrServiceImpl.java` 日志存在 OCR response body 输出风险，本轮未越界修复。

## 优化建议

下一轮优先补 Java 8081 e2e smoke 编排，确保 Redis、PostgreSQL/pgvector、Java 8081、Python 8090、OPENAI_API_KEY 和 JWT 登录态同时可用。随后推进真实 pgvector/Dashscope 检索生成与离线 RAG 质量评估集。

## 关联代码、文档与提交

- 接口文档：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\接口文档\接口文档_3_Dashboard.md`
- Python 服务：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\ai_service\dashboard_rag_service.py`
- Java client：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\server\src\main\java\com\itsheng\service\client\PythonDashboardAiClient.java`
- Java service：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\server\src\main\java\com\itsheng\service\service\Impl\DashboardServiceImpl.java`
- 当前本地提交：最终 hash 以本日志纳入提交后的 `git rev-parse HEAD` 为准，提交信息 `feat: route dashboard target matching through python rag`。
