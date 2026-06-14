# Dashboard-AI 目标岗位匹配复验日志

## 测试对象

- `接口文档/接口文档_3_Dashboard.md`
- `ai_service/dashboard_rag_service.py`
- `ai_service/market_ai_service.py`
- `server/src/main/java/com/itsheng/service/config/PythonAiProperties.java`
- `server/src/main/java/com/itsheng/service/client/PythonDashboardAiClient.java`
- `server/src/main/java/com/itsheng/service/controller/DashboardController.java`
- `server/src/main/java/com/itsheng/service/service/Impl/DashboardServiceImpl.java`
- Dashboard 相关 Python/Java 测试

## 测试原因

本轮自动化复验 Dashboard-AI 目标岗位匹配 Python RAG 分支，确认接口文档、Python RAG、Java/Python 胶水、测试日志、Obsidian 记录和 git 范围门禁一致。此前 pytest 缺失导致只能降级到 unittest，本轮已安装 pytest 并补跑。代码审查发现 `resume_profile` 允许字段值中若混入邮箱、手机号或 token-like 值，可能进入 diagnostics 的 `query_variants`，本轮已按文档先行补充值级 PII 过滤，并将 8090 聚合入口的 `VALIDATION_ERROR` 收窄到 Dashboard endpoint。

## 测试环境

- 时间：2026-06-09 04:14:15 +08:00
- Worktree：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard`
- 分支：`ai-rag-dashboard-target-job-match`
- 复验基准：`origin/master..HEAD` 的唯一 Dashboard-AI 本地提交；最终提交哈希以 `git log --oneline origin/master..HEAD` 为准
- Python：`C:\Users\WhenJayHe\miniforge3\python.exe`，3.13.13
- pytest：9.0.3，本轮通过 `python -m pip install pytest` 安装到当前 Python 环境
- Maven：本地 `mvn`

## 测试命令与结果

```powershell
git show --stat --oneline --decorate HEAD
git rev-parse --short HEAD
git rev-list --count origin/master..HEAD
git log --oneline origin/master..HEAD
git diff --name-only origin/master..HEAD
git diff --name-only
git diff --cached --name-only
git status --short --branch --ignored
git ls-files --others --exclude-standard
git diff --check origin/master..HEAD
git diff --cached --check
git ls-remote --heads origin ai-rag-dashboard-target-job-match
```

结果：`origin/master..HEAD` 为 1 个 Dashboard-AI 本地提交；远端无同名分支输出；业务 worktree 和暂存区无 dirty；提交范围未包含 `website/`、`database/`、`application*.yml`、缓存或构建产物。2026-06-09 10:46 复验未发现 `__pycache__` 或 `.pytest_cache` 残留目录。日志修正将 amend 进同一个 Dashboard-AI 本地提交，最终提交哈希以 amend 后 `git log --oneline origin/master..HEAD` 为准。

```powershell
python -B -m pytest -p no:cacheprovider ai_service/test_dashboard_rag_service.py
python -B -m unittest discover -s ai_service -p "test_dashboard_rag_service.py"
```

结果：pytest `13 passed in 1.64s`；unittest `Ran 13 tests ... OK`。新增覆盖 `query_variants` 值级 PII 过滤、非数字 `experience_years` 不进入 summary records，以及非 Dashboard endpoint validation error 保持历史 `message` 形态。

```powershell
mvn -pl common,pojo -am install -DskipTests
mvn -pl server -Dtest=PythonDashboardAiClientTest test
mvn -pl server -Dtest=DashboardServiceImplTest test
mvn -pl server -Dtest=DashboardControllerTest test
mvn -pl server -am -DskipTests compile
```

结果：`common,pojo` install BUILD SUCCESS；`PythonDashboardAiClientTest` 9 tests OK；`DashboardServiceImplTest` 8 tests OK；`DashboardControllerTest` 1 test OK；server compile BUILD SUCCESS。新增 Java 测试覆盖 `target_role`/`skills` 允许字段中的邮箱、手机号和 `api_key` 形式值被丢弃。

```powershell
python -B -m ai_service.market_ai_service --host 127.0.0.1 --port 8090
```

Python 8090 smoke 使用 UTF-8 JSON 调用 `/internal/dashboard/target-job/match`：

最小请求样例：

```json
{
  "request_id": "dashboard-smoke",
  "user_id": 1,
  "resume_analysis_id": 10,
  "resume_vector_store_id": "resume_vec_10",
  "resume_profile": {
    "target_role": "AI算法工程师",
    "skills": ["Python", "机器学习", "RAG"],
    "experience_years": 1
  },
  "resume_content": "候选人熟悉 Python、机器学习、RAG 检索增强生成、模型评估和后端服务集成。",
  "job_candidates": [
    {
      "job_id": 101,
      "job_name": "AI算法工程师",
      "job_category_code": "AI_ENGINEER_JUNIOR",
      "job_level": "JUNIOR",
      "required_skills": ["Python", "机器学习", "模型部署"],
      "job_description": "负责模型训练、评估、RAG 应用和 AI 服务落地。"
    }
  ],
  "filters": {
    "document_type": ["resume", "job", "jd"],
    "visibility_scope": "user_or_public",
    "language": "zh-CN"
  },
  "top_k": 5
}
```

```text
SMOKE_STATUS=200
SMOKE_CODE=1
SMOKE_JOB_ID=101
SMOKE_FUSION=rrf
SMOKE_EVIDENCE_COUNT=2
SMOKE_PII_IN_QUERIES=false
PORT_8090_REMAINING=none
```

## 安全与范围检查

- `ai_service/market_ai_service.py` diff 挂载 `/internal/dashboard/target-job/match`，并把 `VALIDATION_ERROR` 响应收窄到 Dashboard endpoint；Market/Goals/Roadmap/Reports/Feedback 等非 Dashboard endpoint 的 validation error 保持历史 `message` 形态。集成审查 P3 指出 8090 聚合入口也把其他既有 endpoint 的导入移动到 endpoint 分支内延迟执行；接口文档已补充该调整仅用于 Dashboard-only 隔离分支启动兼容，不改变非 Dashboard endpoint 路径、schema、业务逻辑或错误形态。
- Dashboard Java 本地随机/兜底检索 grep 无命中。
- 精确日志语句检查结果：`SENSITIVE_LOG_STATEMENTS=none`，未记录完整 response body、request body、`resume_content`、`parsedData`、token、phone、email 等敏感体。
- tests-log 与 Obsidian 记录已覆盖 13/9/8/1 测试数量、8090 smoke、`rrf`、evidence 非空、PII 不进入 query variants、非数字 `experience_years` 不进入 summary records、未 push、Java e2e 未声明通过。

## 子 Agent 验收结论

- Plan 需求覆盖审查：PASS。
- Plan 技术风险审查：PASS（补充 allowlist/denylist、git/cached/remote 门禁后通过）。
- Goal 边界审查：PASS（明确不 push、不 merge、不开 PR）。
- Goal 验证命令审查：初始 FAIL，原因是缺少 git/cached/remote 门禁；补充后仍因 pytest 缺失 FAIL；安装 pytest 并清理 `__pycache__` 后最终两次复审均 PASS，允许进入本地复验。
- 代码审查：初始 FAIL，指出 `resume_profile` 允许字段值存在 PII 回显到 `query_variants` 的风险、04:14 复验日志误写到主工作区、以及 `market_ai_service.py` validation error 全局生效风险；已补文档、Java/Python PII 过滤、非 Dashboard validation error 兼容测试，并把日志放到正确隔离 worktree。
- 最终代码审查：PASS，无 P0-P3 findings；确认无前端、数据库、`application*.yml` 越界修改，`query_variants` 和 evidence 引用符合接口文档。
- 最终集成审查：PASS，P3 提示 8090 聚合入口的非 Dashboard endpoint 延迟导入需补文档；已在接口文档 3 和本日志中说明该改动不改变非 Dashboard endpoint 行为。
- 最终测试覆盖验收：PASS；覆盖 Python RAG、Java client、Java service、controller、server compile、Python 8090 smoke 和范围门禁。
- 最终测试日志可信度验收：PASS；补充本日志的最小 smoke 请求样例，保留 Java 8081 e2e 未跑和真实 pgvector/embedding/LLM 未验收的边界声明。

## 失败原因与修复记录

- `pytest` 初始缺失：本轮执行 `python -m pip install pytest`，随后 `python -B -m pytest -p no:cacheprovider ai_service/test_dashboard_rag_service.py` 通过 13 tests；2026-06-09 10:47 对当前 Dashboard-AI 单提交复跑结果为 `13 passed in 1.69s`。
- `pytest` 早期运行产生 `ai_service/__pycache__/`：已验证路径位于隔离 worktree 内后删除，后续使用 `python -B` 和 `-p no:cacheprovider` 降低缓存残留。
- 复验日志初次误写到主工作区：已删除主工作区误建文件，并复制到 `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\tests-log\ai-rag-automation\2026-06-09-0414-dashboard-target-job-revalidation.md`。
- `resume_profile` 允许字段值 PII 风险：已补 Java 出站过滤和 Python 防御过滤，邮箱、手机号、`token`/`secret`/`api_key`、token-like 长串以及非数字 `experience_years` 不会进入 `query_variants` 或 summary records。
- `market_ai_service.py` 全局 validation error 风险：已将 `VALIDATION_ERROR` 响应限制到 `/internal/dashboard/target-job/match`，非 Dashboard endpoint 保持 `{"message": ...}`。
- Java e2e smoke 未执行：仍需 Redis、PostgreSQL/pgvector、Java 8081、Python 8090 和 `OPENAI_API_KEY` 同时可用，本轮不声明 Java 端到端通过。

## 剩余风险与建议

- 当前 Dashboard-AI RAG 仍是 deterministic fallback，未接真实 pgvector、Dashscope embedding、cross-encoder 或 LLM。
- 生产部署如果将 Python 8090 暴露到非本机网络，需要网关或内部 token 保护。
- 后续可补充真实岗位/JD 向量库、RAG 离线评估集、context precision / recall 指标和耗时 trace。

## 关联提交

- 本轮复验目标：Dashboard-AI 单提交 `feat: route dashboard target matching through python rag`，最终哈希以 `git log --oneline origin/master..HEAD` 为准。
- 本轮不 push、不 merge、不开 PR。

## 2026-06-09 10:46 提交范围可信性补充

- 当前隔离 worktree：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard`
- 当前分支：`ai-rag-dashboard-target-job-match`
- 复验基准：`origin/master..HEAD` 的唯一 Dashboard-AI 本地提交；最终提交哈希以 `git log --oneline origin/master..HEAD` 为准
- `git status --short --branch`：`## ai-rag-dashboard-target-job-match...origin/master [ahead 1]`
- `git log --oneline origin/master..HEAD`：唯一提交，提交信息为 `feat: route dashboard target matching through python rag`
- `git diff --name-status origin/master..HEAD`：仅包含 Dashboard-AI allowlist 文件：`ai_service/README.md`、`ai_service/__init__.py`、`ai_service/dashboard_rag_service.py`、`ai_service/market_ai_service.py`、`ai_service/test_dashboard_rag_service.py`、`server/src/main/java/com/itsheng/service/client/PythonDashboardAiClient.java`、`server/src/main/java/com/itsheng/service/config/PythonAiProperties.java`、`server/src/main/java/com/itsheng/service/controller/DashboardController.java`、`server/src/main/java/com/itsheng/service/service/Impl/DashboardServiceImpl.java`、Dashboard Java tests、`tests-log/ai-rag-automation/**`、`接口文档/接口文档_3_Dashboard.md`。
- `git diff --check origin/master..HEAD` 与 `git diff --cached --check`：通过。
- `git ls-files --others --exclude-standard`：无输出。
- `git clean -ndX`：只预览、不删除；输出仅包含 `common/target/`、`pojo/target/`、`server/target/` 三个 Maven 构建产物目录，未发现 Python cache、测试日志、接口文档或源码被忽略。
- `git check-ignore -v` 检查 Dashboard Java/Python 新增测试：无输出，说明未被忽略。
- `git ls-remote --heads origin ai-rag-dashboard-target-job-match`：无输出，远端同名分支不存在。
- `python -B -m pytest -p no:cacheprovider ai_service/test_dashboard_rag_service.py`：13 passed。
- `python -B -m unittest discover -s ai_service -p "test_dashboard_rag_service.py"`：Ran 13 tests, OK。
- `mvn -pl common,pojo -am install -DskipTests`：BUILD SUCCESS。
- `mvn -pl server -Dtest=PythonDashboardAiClientTest test`：Tests run: 9, Failures: 0, Errors: 0。
- `mvn -pl server -Dtest=DashboardServiceImplTest test`：Tests run: 8, Failures: 0, Errors: 0。
- `mvn -pl server -Dtest=DashboardControllerTest test`：Tests run: 1, Failures: 0, Errors: 0。
- `mvn -pl server -am -DskipTests compile`：BUILD SUCCESS。
- Python 8090 smoke：`SMOKE_STATUS=200`、`SMOKE_CODE=1`、`SMOKE_JOB_ID=101`、`SMOKE_FUSION=rrf`、`SMOKE_EVIDENCE_COUNT=2`、`SMOKE_PII_IN_QUERIES=false`、`PORT_8090_REMAINING=none`。
- Java 8081 端到端 smoke 仍未执行，原因不变：Redis、PostgreSQL/pgvector、Java 8081、Python 8090 和 `OPENAI_API_KEY` 未作为同一运行环境同时验证；本轮不声明端到端通过。

## 2026-06-09 12:08 自动化门禁复跑补充

- `git rev-list --count origin/master..HEAD`：`1`；`git log --oneline origin/master..HEAD` 为唯一 Dashboard-AI 本地提交，提交信息为 `feat: route dashboard target matching through python rag`；最终哈希以当前复查命令为准。
- `git diff --name-status`：仅包含本日志文件；`git diff --check`：仅 CRLF warning，无 whitespace error。
- UTF-8 / 占位检查：`接口文档/接口文档_3_Dashboard.md`、本测试日志、Obsidian 记录 `接口文档_3_Dashboard_RAG优化记录.md` 均 `utf8=ok`、`replacement=0`，未发现待办或修复占位标记。
- 敏感日志扫描：Dashboard-AI allowlist 文件中未发现记录完整 `request body`、`response body`、`resume_content`、`parsedData`、`token`、`phone`、`email` 的日志语句。
- `python -B -m pytest -p no:cacheprovider ai_service/test_dashboard_rag_service.py`：首次由工具层超时并伴随 stdout flush 异常，不能计为通过；随后设置 `PYTHONIOENCODING=utf-8` 并使用 `python -B -m pytest -q -p no:cacheprovider ai_service/test_dashboard_rag_service.py --tb=short` 复跑，结果 `13 passed in 1.69s`。
- `python -B -m unittest discover -s ai_service -p "test_dashboard_rag_service.py"`：`Ran 13 tests in 1.574s`，`OK`。
- `mvn -pl common,pojo -am install -DskipTests`：`BUILD SUCCESS`。
- `mvn -pl server -Dtest=PythonDashboardAiClientTest test`：`Tests run: 9, Failures: 0, Errors: 0`。
- `mvn -pl server -Dtest=DashboardServiceImplTest test`：`Tests run: 8, Failures: 0, Errors: 0`。
- `mvn -pl server -Dtest=DashboardControllerTest test`：`Tests run: 1, Failures: 0, Errors: 0`。
- `mvn -pl server -am -DskipTests compile`：`BUILD SUCCESS`。
- Python 8090 smoke：`SMOKE_STATUS=200`、`SMOKE_CODE=1`、`SMOKE_JOB_ID=101`、`SMOKE_FUSION=rrf`、`SMOKE_EVIDENCE_COUNT=2`、`SMOKE_PII_IN_QUERIES=false`、`PORT_8090_REMAINING=none`。
- Frontend：`website/` 未变更且 `/api/dashboard/roadmap` TypeScript 契约不变，本轮未运行 `npm run build`。
- Java 8081 端到端 smoke：仍未执行；原因是 Redis、PostgreSQL/pgvector、Java 8081、Python 8090 与 `OPENAI_API_KEY` 未作为同一运行环境同时验证。本轮不声明真实端到端、pgvector、Dashscope embedding/LLM、cross-encoder 或离线 RAG 质量通过。

### 代码验收 P1 修复记录

- 代码审查 P1：Python `resume_profile` 防御性过滤不足，嵌套对象可能被 `str()` 化后进入 `query_variants` 或 summary records。修复：`ai_service/dashboard_rag_service.py` 新增标量值校验，仅允许字符串、数字和布尔值进入 `target_role` / `skills`，嵌套对象和数组对象直接丢弃；`ai_service/test_dashboard_rag_service.py` 新增嵌套画像值不进入 query/summary 的测试。
- 集成审查 P1：Dashboard Java 侧原先通过 `selectByVectorStoreId` 读取 `user_vector_store.content` 后再校验 `user_id`，不满足“归属校验后才读取正文”。修复：`UserVectorStoreMapper` / XML 新增 `selectByVectorStoreIdAndUserId(id, userId)`，Dashboard 路径改为 SQL 层 `id + user_id` 过滤后再读取正文；`DashboardServiceImplTest` 跨用户用例补充断言旧 `selectByVectorStoreId` 不被调用。
- 接口文档同步：`接口文档/接口文档_3_Dashboard.md` 已补充 SQL 层归属读取门禁与 Python 嵌套对象防御性过滤规则。

### P1 修复后验证

- `python -B -m pytest -q -p no:cacheprovider ai_service/test_dashboard_rag_service.py --tb=short`：`14 passed in 1.75s`。
- `python -B -m unittest discover -s ai_service -p "test_dashboard_rag_service.py"`：`Ran 14 tests in 1.586s`，`OK`。
- `mvn -pl server -Dtest=DashboardServiceImplTest test`：`Tests run: 8, Failures: 0, Errors: 0`。
- `mvn -pl server -Dtest=PythonDashboardAiClientTest test`：`Tests run: 9, Failures: 0, Errors: 0`。
- `mvn -pl server -Dtest=DashboardControllerTest test`：`Tests run: 1, Failures: 0, Errors: 0`。
- `mvn -pl server -am -DskipTests compile`：`BUILD SUCCESS`。
- Python 8090 smoke（含嵌套画像值 `nested-secret` 防御性过滤样例）：`SMOKE_STATUS=200`、`SMOKE_CODE=1`、`SMOKE_JOB_ID=101`、`SMOKE_FUSION=rrf`、`SMOKE_EVIDENCE_COUNT=2`、`SMOKE_PII_IN_QUERIES=false`、`PORT_8090_REMAINING=none`。
- Java 8081 端到端 smoke 仍未执行，原因不变：Redis、PostgreSQL/pgvector、Java 8081、Python 8090 和 `OPENAI_API_KEY` 未作为同一运行环境同时验证；本轮不声明端到端通过。
