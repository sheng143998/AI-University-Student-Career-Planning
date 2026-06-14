# Dashboard-AI 目标岗位匹配闭环测试日志

## 测试对象

- `接口文档/接口文档_3_Dashboard.md`
- `ai_service/__init__.py`
- `ai_service/dashboard_rag_service.py`
- `ai_service/market_ai_service.py`
- `ai_service/test_dashboard_rag_service.py`
- `ai_service/README.md`
- `server/src/main/java/com/itsheng/service/config/PythonAiProperties.java`
- `server/src/main/java/com/itsheng/service/client/PythonDashboardAiClient.java`
- `server/src/main/java/com/itsheng/service/service/Impl/DashboardServiceImpl.java`
- `server/src/main/java/com/itsheng/service/controller/DashboardController.java`
- `server/src/test/java/com/itsheng/service/client/PythonDashboardAiClientTest.java`
- `server/src/test/java/com/itsheng/service/service/Impl/DashboardServiceImplTest.java`
- `server/src/test/java/com/itsheng/service/controller/DashboardControllerTest.java`

## 测试原因

本轮将 Dashboard `/api/dashboard/roadmap` 在目标岗位缺失或无效时的匹配逻辑切到 Python Dashboard-AI RAG 边界，并补齐 Dashboard Python RAG 的 `document/section/chunk` 三层 summary index 与 Java `resume_profile` 出站白名单。需要验证接口文档、Python RAG、Java 调用链、错误映射、用户归属校验、日志脱敏、测试覆盖、smoke 和提交范围一致。

## 测试环境

- Worktree: `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard`
- Branch: `ai-rag-dashboard-target-job-match`
- Baseline: `origin/master`
- 本轮状态：已收敛为 `origin/master..HEAD` 的 1 个 Dashboard-AI 本地提交；最终哈希以 `git log --oneline origin/master..HEAD` 为准，worktree clean/ahead 1，本轮未 push。
- Shell: Windows PowerShell
- Python: 当前 `python`
- Maven: 本地 `mvn`
- Runtime: Python 8090 smoke 不依赖 Redis/PostgreSQL/Dashscope；Java 端到端 smoke 仍需 Redis、PostgreSQL/pgvector、Java 8081、Python 8090 和 `OPENAI_API_KEY`

## 测试数据

Python 8090 smoke 使用样例：

```json
{
  "request_id": "dashboard-smoke-1",
  "user_id": 1,
  "resume_analysis_id": 10,
  "resume_vector_store_id": "resume_vec_10",
  "resume_profile": {
    "target_role": "AI算法工程师",
    "skills": ["Python", "机器学习", "RAG"],
    "experience_years": 1
  },
  "resume_content": "项目经历：使用 Python 构建 RAG 检索增强系统，包含 BM25、向量检索、RRF 重排和模型部署。熟悉机器学习训练和评估。",
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
    },
    {
      "job_id": 202,
      "job_name": "UI设计师",
      "job_category_code": "UI_DESIGN_JUNIOR",
      "job_level": "JUNIOR",
      "job_level_name": "初级",
      "required_skills": ["Figma", "视觉设计"],
      "job_description": "负责界面视觉设计和交互规范。",
      "job_profile": {"industrySegment": "设计"}
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

## 测试方法与命令

```powershell
mvn -pl common,pojo -am install -DskipTests
python -m pytest ai_service/test_dashboard_rag_service.py
python -m unittest discover -s ai_service -p "test_dashboard_rag_service.py"
python -m unittest ai_service.test_dashboard_rag_service
python -m unittest ai_service.test_dashboard_rag_service.DashboardRagServiceTest.test_query_variants_do_not_include_raw_resume_content
mvn -pl server -Dtest=PythonDashboardAiClientTest test
mvn -pl server -Dtest=DashboardServiceImplTest test
mvn -pl server -Dtest=DashboardControllerTest test
mvn -pl server -Dtest=PythonDashboardAiClientTest#mapsTimeoutToTimeoutMessage test
mvn -pl server -Dtest=DashboardServiceImplTest#getRoadmapRejectsVectorStoreOwnedByOtherUser test
mvn -pl server -Dtest=DashboardServiceImplTest#getRoadmapCallsPythonWhenExistingTargetJobInvalid test
mvn -pl server -am -DskipTests compile
python -m ai_service.market_ai_service --host 127.0.0.1 --port 8090
rg -n "JobVectorSearchService|searchSimilarJobs|random|随机|兜底" server/src/main/java/com/itsheng/service/service/Impl/DashboardServiceImpl.java server/src/test/java/com/itsheng/service/service/Impl/DashboardServiceImplTest.java
rg -n "log\.(warn|error|info|debug).*response\.body\(\)|log\.(warn|error|info|debug).*body=\{}|log\.(warn|error|info|debug).*requestBody|log\.(warn|error|info|debug).*resume_content|log\.(warn|error|info|debug).*resumeContent|log\.(warn|error|info|debug).*parsedData|log\.(warn|error|info|debug).*getResumeContent|log\.(trace|debug|info|warn|error).*\bretrieval\b" server/src/main/java/com/itsheng/service/client/PythonDashboardAiClient.java server/src/main/java/com/itsheng/service/service/Impl/DashboardServiceImpl.java
git diff --cached --name-only
git diff --name-only
git diff --cached --check
git diff --check
git ls-files --others --exclude-standard
git status --short --ignored
```

## 实际结果

- `mvn -pl common,pojo -am install -DskipTests`: 退出码 0，BUILD SUCCESS。
- `python -m pytest ai_service/test_dashboard_rag_service.py`: 退出码 1，失败类型为 pytest runner 环境问题，输出 `No module named pytest`；按门禁允许降级到 unittest。
- `python -m unittest discover -s ai_service -p "test_dashboard_rag_service.py"`: 退出码 0，10 tests OK，新增覆盖 `summary_level=document|section|chunk`、`record_id`、`parent_id`、evidence_refs/selected_evidence_ids 只引用 chunk，以及 `top_k=1` 时仍补足 chunk evidence。
- `python -m unittest ai_service.test_dashboard_rag_service`: 退出码 0，10 tests OK。
- `python -m unittest ai_service.test_dashboard_rag_service.DashboardRagServiceTest.test_query_variants_do_not_include_raw_resume_content`: 退出码 0，1 test OK。
- `mvn -pl server -Dtest=PythonDashboardAiClientTest test`: 退出码 0，9 tests，覆盖默认 baseUrl/timeout、成功响应、`NO_MATCH`、HTTP 204、validation、5xx、非 JSON、timeout、连接失败。
- `mvn -pl server -Dtest=DashboardServiceImplTest test`: 退出码 0，7 tests，覆盖已有有效 target 不调 Python、缺失 target 调 Python、Python 返回不存在岗位不落库、跨用户 vectorStore 拒绝、无效 target 转 Python、`resume_profile` 白名单、skills 字符串归一化和 12 项上限。
- `mvn -pl server -Dtest=DashboardControllerTest test`: 退出码 0，1 test，覆盖 `DashboardAiException` 到 `Result.error`。
- `mvn -pl server -Dtest=PythonDashboardAiClientTest#mapsTimeoutToTimeoutMessage test`: 退出码 0，1 test。
- `mvn -pl server -Dtest=DashboardServiceImplTest#getRoadmapRejectsVectorStoreOwnedByOtherUser test`: 退出码 0，1 test。
- `mvn -pl server -Dtest=DashboardServiceImplTest#getRoadmapCallsPythonWhenExistingTargetJobInvalid test`: 退出码 0，1 test。
- `mvn -pl server -am -DskipTests compile`: 退出码 0，BUILD SUCCESS。
- Python 8090 smoke: 首次 `Invoke-WebRequest` 客户端抛 `NullReferenceException`，第二次文件 payload 因 BOM/编码导致 HTTP 400；最终改用 Python 标准库直接构造 UTF-8 JSON body 后通过，退出码 0，`SMOKE_STATUS=200`、`SMOKE_CODE=1`、`SMOKE_JOB_ID=101`、`SMOKE_FUSION=rrf`、`SMOKE_EVIDENCE_COUNT=2`；服务进程已停止，`PORT_8090_REMAINING=none`。
- Dashboard 本地检索/随机/兜底禁止词 grep: 退出码 1，无命中，符合预期。
- Dashboard 日志脱敏 grep: 退出码 1，无命中，符合预期。
- 2026-06-09 02:12-02:16 最终复验：`python -m pytest ai_service/test_dashboard_rag_service.py` 仍因缺少 pytest 输出 `No module named pytest`；`python -m unittest discover -s ai_service -p "test_dashboard_rag_service.py"` 10 tests OK；`mvn -pl common,pojo -am install -DskipTests` BUILD SUCCESS；`mvn -pl server -Dtest=PythonDashboardAiClientTest test` 9 tests OK；`mvn -pl server -Dtest=DashboardServiceImplTest test` 7 tests OK；`mvn -pl server -Dtest=DashboardControllerTest test` 1 test OK；`mvn -pl server -am -DskipTests compile` BUILD SUCCESS。
- 最终门禁预检：`git rev-parse --show-toplevel` 指向隔离 worktree；`git ls-files --others --exclude-standard` 无输出；`git diff --name-only` 无输出；`git diff --cached --check` 通过；cached denylist 无命中。`common/target`、`pojo/target`、`server/target` 为 Maven ignored 产物，未暂存。

## 失败与修复记录

- Python `pytest` 未安装：原始输出为 `No module named pytest`，不是项目 import、语法或业务断言失败；已降级并通过 unittest。
- 早期 Python NO_MATCH 测试曾出现误召回：已调整 deterministic reranker，要求检索分之外必须有技能、岗位名或目标岗位文本证据。
- 早期 Maven `-pl server -am -Dtest=... test` 会在依赖模块无同名测试时提前失败：已改为先 `common,pojo install`，再用 `mvn -pl server -Dtest=... test`。
- 并行运行三个 `mvn -pl server -Dtest=... test` 时出现 `NoClassDefFoundError`，判断为同一 `server/target` 并发写入竞争；已改为顺序执行并全部通过。
- 代码/集成审查发现 `ai_service/__pycache__/` 未跟踪缓存产物：已删除，后续提交门禁继续禁止缓存/产物。
- Java 测试受 `.gitignore` 影响普通 status 不显示：提交前必须使用 `git add -f` 暂存三份 Dashboard Java 测试，并用 cached allowlist 校验。
- Goal 验证审查发现当前实现只有 chunk 级 summary record：已先更新接口文档，再补充 Python `document/section/chunk` 三层 summary records、`summary_level`、`record_id`、`parent_id` 与测试断言。
- Goal 验证审查发现 `resume_profile` 可能透传 `parsed_data` 未知字段：已保留本轮 Dashboard 出站白名单修复，只允许 `target_role`、`skills`、`experience_years`，丢弃联系方式、教育/经历/项目详情、原文类字段和未知对象。
- 代码复审发现 `top_k=1` 可能只命中 document/section summary，导致成功响应没有 chunk evidence：已在成功返回前补选同岗位 chunk evidence，若仍无 chunk 则返回 `NO_MATCH`，并新增回归测试。

## 子 Agent 验收结论

- Plan 需求覆盖子 Agent：PASS。
- Plan 技术风险子 Agent：PASS。
- Goal 边界子 Agent：PASS。
- Goal 验证命令与退出条件子 Agent：PASS。
- 代码审查子 Agent：前置审查发现日志占位和 evidence chunk 断言不足；已补 `selected_evidence_ids` chunk 断言、`top_k=1` chunk evidence 回归测试和 HTTP 204 映射测试，最终复审结论 PASS。
- 集成审查子 Agent：前置审查发现接口文档三层 summary index 与日志/暂存状态不一致；已补齐接口文档三层 `summary_level/record_id/parent_id` 说明，最终复审结论 PASS。
- 测试覆盖验收子 Agent：PASS。只读验收确认 Python 10、Java client 9、service 7、controller 1、compile、8090 smoke、禁止词、日志脱敏、三层 summary index、`top_k=1` chunk evidence、HTTP 204 映射和 profile 白名单覆盖足够。
- 测试日志可信性验收子 Agent：PASS。本日志与 `2026-06-08-dashboard-profile-whitelist.md` 已统一最终验证数量、HTTP 204、`top_k=1` fallback、smoke evidence_count、单提交收敛口径和未 push 状态。

## 剩余风险

- Python RAG 当前是 deterministic fallback，未接真实 pgvector、Dashscope embedding、cross-encoder 或 LLM。
- Java 端到端 runtime smoke 未执行；需要 Redis、PostgreSQL/pgvector、Java 8081、Python 8090 和 `OPENAI_API_KEY` 同时可用。
- Python internal endpoint 依赖本地/内网隔离，没有应用层鉴权；生产若绑定到 `0.0.0.0`，需要网关或 token。
- `resume_profile` 已白名单化为 `target_role`、`skills`、`experience_years`；顶层 `resume_content` 仍按既有 4000 字符截断发送给 Python，后续可拆为更短安全摘要。

## 优化建议

- 接入真实岗位/JD 向量库、embedding reranker 和 RAG 评估集，补 context recall / context precision 指标。
- 为 Dashboard-AI 增加 trace id 与耗时指标，只记录 query variant 摘要、filters、candidate_count、selected_evidence_ids，继续避免记录完整简历正文。

## 关联代码、接口文档和提交

- 接口文档：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\接口文档\接口文档_3_Dashboard.md`
- Python：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\ai_service\dashboard_rag_service.py`
- Python 入口：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\ai_service\market_ai_service.py`
- Java client：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\server\src\main\java\com\itsheng\service\client\PythonDashboardAiClient.java`
- Java service：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\server\src\main\java\com\itsheng\service\service\Impl\DashboardServiceImpl.java`
- Java controller：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\server\src\main\java\com\itsheng\service\controller\DashboardController.java`
- 测试：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\ai_service\test_dashboard_rag_service.py`、`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\server\src\test\java\com\itsheng\service\client\PythonDashboardAiClientTest.java`、`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\server\src\test\java\com\itsheng\service\service\Impl\DashboardServiceImplTest.java`、`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\server\src\test\java\com\itsheng\service\controller\DashboardControllerTest.java`
- 提交：已收敛为 `origin/master..HEAD` 的 1 个 Dashboard-AI 本地提交，最终哈希以 `git log --oneline origin/master..HEAD` 为准。`git status --short --branch` 为 ahead 1 且无业务 dirty，`git ls-remote --heads origin ai-rag-dashboard-target-job-match` 无输出，本轮未 push。
