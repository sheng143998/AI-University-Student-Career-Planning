# Dashboard-RAG 集成分支迁移验证日志

## 测试对象

- `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-integration\ai-service\career_ai\dashboard_rag_service.py`
- `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-integration\ai-service\app\main.py`
- `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-integration\server\src\main\java\com\itsheng\service\client\PythonDashboardAiClient.java`
- `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-integration\server\src\main\java\com\itsheng\service\service\Impl\DashboardServiceImpl.java`
- `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-integration\接口文档\接口文档_3_Dashboard.md`

## 测试原因

目标是降低分支未合并风险：将 `ai-rag-dashboard-target-job-match` 中已闭环的 Dashboard-RAG 能力从旧 `ai_service/` 迁入统一 `ai-service/` 集成分支，避免后续删除或归档 Dashboard 分支时丢失功能。

## 测试环境

- 时间：2026-06-14 07:39 +08:00
- Worktree：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-integration`
- 分支：`ai-rag-integration-20260613`
- 基线：`origin/master` = `313b9be`
- Dashboard 来源提交：`713eb90 feat: route dashboard target matching through python rag`
- Python：`python` 当前环境
- Java/Maven：本机 Maven，JDK 按 Maven 输出为 release 17 编译目标

## 测试方法与步骤

1. 对 Dashboard 来源提交执行 `git cherry-pick -n 713eb90d9c64cac5fe2b73d30e1fc65e8024ad91`。
2. 解决冲突：保留集成分支已有 `PythonAiProperties`、`UserVectorStoreMapper`、`UserVectorStoreMapper.xml` 逻辑，并补 Dashboard timeout 与按 `id + user_id` 查询。
3. 将 `ai_service/dashboard_rag_service.py` 迁移为 `ai-service/career_ai/dashboard_rag_service.py`。
4. 将 `ai_service/test_dashboard_rag_service.py` 迁移为 `ai-service/tests/test_dashboard_rag_service.py`。
5. 接入统一入口 `ai-service/app/main.py` 的 `POST /internal/dashboard/target-job/match`。
6. 恢复旧 `ai_service/README.md` 与 `ai_service/market_ai_service.py`，避免本轮误删旧目录历史文件。
7. 运行 Python py_compile、Dashboard pytest、Dashboard Java 窄测。

## 测试命令

```powershell
$env:PYTHONPATH='ai-service'
python -B -m py_compile ai-service\career_ai\dashboard_rag_service.py ai-service\tests\test_dashboard_rag_service.py ai-service\app\main.py

$env:PYTHONPATH='ai-service'
python -B -m pytest ai-service/tests/test_dashboard_rag_service.py -q -p no:cacheprovider

mvn --% -pl server -am -Dtest=PythonDashboardAiClientTest,DashboardControllerTest,DashboardServiceImplTest,UserVectorStoreMapperXmlTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test

$env:PYTHONPATH='ai-service'
python -B -m pytest ai-service/tests -q -p no:cacheprovider

mvn -pl server -am -DskipTests compile

mvn --% -pl server -am -Dtest=PythonReportsAiClientTest,ReportServiceImplReportsRagTest,PythonResumeAiClientTest,ResumeServiceImplResumePythonTest,PythonGoalsAdviceClientTest,GoalsControllerTest,GoalsServiceImplTest,PythonChatClientTest,ChatControllerDebugEndpointDisabledTest,ChatControllerDebugEndpointEnabledTest,ChatRestControllerMessagesTest,PythonDashboardAiClientTest,DashboardControllerTest,DashboardServiceImplTest,UserVectorStoreMapperXmlTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test

cd website
npm run build
```

## 实际结果

- `py_compile`：通过，无输出。
- 第一次 Dashboard pytest：`14 passed, 2 failed`。
  - 失败 1：统一入口对 Dashboard validation error 返回通用 `error/message`，不符合 Dashboard `Result` 风格。
  - 失败 2：未知路径非法 JSON 在统一入口先解析 JSON，返回 `400 INVALID_JSON`，旧测试预期为旧 market handler 的 404/legacy shape。
- 修复后 Dashboard pytest：`16 passed in 1.69s`。
- 第一次 Java 窄测：失败，`PythonAiProperties.java` 文件头存在 UTF-8 BOM，`javac` 报 `非法字符: '\ufeff'`。
- 移除 BOM 后 Java 窄测：`Tests run: 21, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- 全量 `ai-service/tests`：`55 passed in 7.64s`。
- Maven compile：`BUILD SUCCESS`。
- AI/RAG Java 窄测集合：`Tests run: 69, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- 前端构建：`npm run build` 成功，`vue-tsc && vite build` 通过。

## 修复记录

- 在 `ai-service/app/main.py` 对 `/internal/dashboard/target-job/match` 单独捕获 `ValueError`，返回 `{"code":0,"msg":"VALIDATION_ERROR","data":{"error":"..."}}` 与 Dashboard 契约一致。
- 将 Dashboard 测试 import 从旧 `ai_service.*` 改为 `career_ai.dashboard_rag_service` 与 `app.main.AiServiceHandler`。
- 修正未知路径非法 JSON 测试预期为统一入口 `400 INVALID_JSON`。
- 移除 `PythonAiProperties.java` BOM，并保留 Reports/Resume/Chat/Feedback 既有配置，同时加入 Dashboard timeout getter。
- 恢复旧 `ai_service/README.md` 与 `ai_service/market_ai_service.py`，本轮不删除旧目录历史文件。

## 子 Agent 验收结论

- Plan 覆盖审查：FAIL，但结论支持继续 Dashboard/Roadmap 迁移；必须补充目录不变量、冲突清零、逐分支覆盖矩阵和具体测试命令。
- Goal 完成定义审查：FAIL，原因是当时集成 worktree 处于冲突态且 Dashboard/Roadmap 未覆盖；本轮已先解决 Dashboard 冲突与迁移，Goal 仍未完成，因为 Roadmap 尚未迁移。

## 剩余风险

- Roadmap-RAG 仍未迁入 `ai-service/`，仍是当前最大的本地分支未合并风险。
- 本轮已运行全量 `ai-service/tests`、AI/RAG Java 窄测、Maven compile 和 `npm run build`；未运行真实 Java 8081 + Python 8090 + PostgreSQL/pgvector + Redis + OSS/JWT runtime smoke。
- Dashboard 仍是 deterministic fallback RAG，不声明真实 pgvector、Dashscope、cross-encoder 或离线质量评估完成。
- Dashboard 来源分支旧日志仍引用 `ai_service.market_ai_service`，已作为历史证据保留；当前集成分支的新增能力不再依赖旧目录。

## 关联代码/接口文档/提交

- 来源分支提交：`713eb90 feat: route dashboard target matching through python rag`
- 当前集成分支：`ai-rag-integration-20260613`
- 接口文档：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-integration\接口文档\接口文档_3_Dashboard.md`
- 端口说明：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-integration\docs\AI_RAG_配置与端口说明.md`
- 剩余清单：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-integration\docs\AI_RAG_剩余修改与完善清单.md`
