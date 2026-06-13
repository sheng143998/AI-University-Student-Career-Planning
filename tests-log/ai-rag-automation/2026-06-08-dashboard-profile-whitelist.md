# Dashboard-AI resume_profile 白名单测试日志

## 测试对象

白名单补充对象（最终随 Dashboard-AI 单提交一起保留）：

- `接口文档/接口文档_3_Dashboard.md`
- `server/src/main/java/com/itsheng/service/service/Impl/DashboardServiceImpl.java`
- `server/src/test/java/com/itsheng/service/service/Impl/DashboardServiceImplTest.java`
- `tests-log/ai-rag-automation/2026-06-08-dashboard-profile-whitelist.md`

最终单提交中的 Dashboard-AI 三层 summary index 关联对象：

- `ai_service/dashboard_rag_service.py`
- `ai_service/test_dashboard_rag_service.py`
- `server/src/test/java/com/itsheng/service/client/PythonDashboardAiClientTest.java`

## 测试原因

本轮收窄 Dashboard-AI Java 到 Python 的 `resume_profile` 出站数据面，避免把 `resume_analysis_result.parsed_data` 中的联系方式、教育/经历/项目详情、原文类字段或未知扩展对象透传到 `/internal/dashboard/target-job/match`。需要验证接口文档、Java payload 构造、Python 合约兼容、测试覆盖和提交范围一致。

## 测试环境

- Worktree: `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard`
- Branch: `ai-rag-dashboard-target-job-match`
- Baseline: `origin/master`
- Shell: Windows PowerShell
- Python: 当前 `python`
- Maven: 本地 `mvn`

## 测试数据

Java 单测构造的 `parsedData` 含允许字段和敏感/未知字段：

```json
{
  "target_role": "  AI算法工程师  ",
  "skills": ["Python", "", "RAG", "Python", "机器学习", "模型部署", "Spring", "PostgreSQL", "Redis", "Docker", "Kubernetes", "向量检索", "BM25", "RRF", "LLM"],
  "experience_years": 1,
  "phone": "13800000000",
  "email": "student@example.com",
  "name": "张三",
  "location": "杭州",
  "education": [{"school": "某大学"}],
  "experience": [{"company": "某公司"}],
  "projects": [{"name": "敏感项目"}],
  "raw_text": "完整简历原文",
  "unknown_nested": {"rawText": "不应透传"}
}
```

预期出站 `resume_profile`：

```json
{
  "target_role": "AI算法工程师",
  "skills": ["Python", "RAG", "机器学习", "模型部署", "Spring", "PostgreSQL", "Redis", "Docker", "Kubernetes", "向量检索", "BM25", "RRF"],
  "experience_years": 1
}
```

另有字符串技能样例 `Python，RAG、Python,  ,BM25`，预期归一化为 `["Python", "RAG", "BM25"]`，空 `target_role`、字符串 `experience_years` 和画像内 `rawText` 被丢弃。

## 测试方法与命令

```powershell
python -m unittest discover -s ai_service -p "test_dashboard_rag_service.py"
mvn -pl common,pojo -am install -DskipTests
mvn -pl server -Dtest=DashboardServiceImplTest test
mvn -pl server -am -DskipTests compile
```

## 实际结果

- `python -m unittest discover -s ai_service -p "test_dashboard_rag_service.py"`: 白名单阶段初次运行退出码 0，8 tests OK；三层 summary index 收口后重跑当前最终工作区，退出码 0，10 tests OK。
- `mvn -pl common,pojo -am install -DskipTests`: 退出码 0，BUILD SUCCESS。
- `mvn -pl server -Dtest=DashboardServiceImplTest test`: 最终复验退出码 0，7 tests。覆盖既有目标岗位、缺失目标岗位调 Python、Python 返回不存在岗位、跨用户 vectorStore、无效目标岗位重匹配，以及本轮新增 `resume_profile` 白名单、字符串技能归一化、嵌套对象丢弃。
- `mvn -pl server -am -DskipTests compile`: 退出码 0，BUILD SUCCESS。
- 2026-06-09 02:12-02:16 最终复验：Python Dashboard unittest 10 tests OK；`mvn -pl common,pojo -am install -DskipTests` BUILD SUCCESS；`PythonDashboardAiClientTest` 9 tests OK；`DashboardServiceImplTest` 7 tests OK；`DashboardControllerTest` 1 test OK；`mvn -pl server -am -DskipTests compile` BUILD SUCCESS。`pytest` 因本地缺少依赖仍输出 `No module named pytest`，按日志门禁降级到 unittest。

## 失败与修复记录

- Maven 指定测试采用稳定口径：先安装 `common,pojo`，再在 `server` 模块单独运行目标测试，避免 `-am -Dtest=...` 在依赖模块无同名测试时产生误失败。
- Python unittest 生成 `ai_service/__pycache__/` 缓存产物；已确认路径位于当前 worktree 后删除，提交门禁继续禁止缓存/构建产物。
- 后续范围复审发现 Dashboard-AI 三层 summary index、HTTP 204 映射测试和主测试日志需要与白名单提交形成连续验收链路；最终收口改为把白名单、三层 summary index、HTTP 204 测试、接口文档和测试日志收敛为 `origin/master..HEAD` 的一个 Dashboard-AI 单提交。
- 同一接口文档同时包含白名单合同与三层 summary index 合同，最终以 `origin/master..HEAD` 的完整 Dashboard-AI diff 统一校验。

## 子 Agent 验收结论

- Plan 需求覆盖子 Agent：PASS。
- Plan 技术风险子 Agent：PASS。
- Goal 边界子 Agent：首轮 FAIL，要求明确 `skills` 上限、字段类型、空值策略和 `resume_profile.raw_text` 与顶层 `resume_content` 边界；已修正，复审 PASS。
- Goal 验证命令与退出条件子 Agent：首轮 FAIL，要求 Python 回归必跑、文档门禁、正确 denylist、baseline HEAD 和 Obsidian 单独检查；已修正，复审 PASS。
- 代码审查子 Agent：首轮 PASS，无必须修复项；指出非阻断风险为允许字段使用 `String.valueOf` 时可能把对象/数组字符串化。已处理：实现改为仅接收字符串、数字、布尔、字符等标量值，补充嵌套对象丢弃单测，并重跑 Java 测试到 7 tests 通过。
- 集成审查子 Agent：首轮 PASS，无必须修复项；确认 Java payload、接口文档、Python 字段读取和前端 `/api/dashboard/roadmap` 契约一致。指出测试日志仍有未完成表述，已补充当前结论和最终测试结果。
- 最终代码审查子 Agent：首轮 FAIL，阻断项仅为本日志存在未完成占位和提交状态表述不准；Java 白名单实现、接口文档和测试覆盖通过审查。已删除占位表述并修正提交状态记录。
- 最终代码审查子 Agent：复审 PASS，确认白名单实现、三层 summary index、`top_k=1` chunk evidence、HTTP 204 测试和接口文档一致。
- 最终集成审查子 Agent：复审 PASS，确认 final diff denylist 未命中 `ai-service/**`、`website/**`、`database/**`、`application*.yml`、缓存或构建产物，前端契约未变。
- 测试覆盖验收子 Agent：PASS，无必须补测项；确认敏感字段/未知字段/嵌套对象不透传、`skills` 去空去重限量、顶层 `resume_content` 保持、Python 合约回归 10 tests OK、Java 目标测试 7 tests OK、Java client 9 tests OK 和 compile 通过足够支撑提交。
- 测试日志可信性验收子 Agent：PASS，无必须修正项；确认本日志已统一最终单提交范围、未 push 状态和剩余风险。

## 剩余风险

- 本轮只收窄 Java 到 Python 的画像字段，不接入真实 pgvector、Dashscope embedding、cross-encoder 或 LLM。
- 顶层 `resume_content` 仍按既有 4000 字符截断发送给 Python；该字段是当前 Dashboard RAG 必需输入，后续可继续拆成更细的安全摘要字段。
- Java 端到端 runtime smoke 仍依赖 Redis、PostgreSQL/pgvector、Java 8081、Python 8090 和 `OPENAI_API_KEY` 同时可用，本轮不声明端到端通过。

## 优化建议

- 后续将顶层 `resume_content` 拆为更短的 `resume_evidence_summary` 与结构化能力摘要，进一步减少敏感数据传输面。
- 接入真实岗位/JD 向量库、embedding reranker 和 RAG 评估集后，继续用 context precision / recall 指标评估白名单对匹配质量的影响。

## 关联代码、接口文档和提交

- 接口文档：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\接口文档\接口文档_3_Dashboard.md`
- Java service：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\server\src\main\java\com\itsheng\service\service\Impl\DashboardServiceImpl.java`
- Java 测试：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\server\src\test\java\com\itsheng\service\service\Impl\DashboardServiceImplTest.java`
- Python 合约回归：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-dashboard\ai_service\test_dashboard_rag_service.py`
- 提交：本日志随 Dashboard-AI 三层 summary index 修复一起收敛到当前未推送本地单提交；最终提交范围以 `origin/master..HEAD` 校验，必须同时包含 profile 白名单、三层 summary index、接口文档、测试与测试日志，并排除缓存、构建产物和非 Dashboard-AI 文件。本轮不 push。
