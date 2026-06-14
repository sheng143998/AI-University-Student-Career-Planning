# Reports-RAG Python 化最小闭环测试日志

## 测试对象
- 接口文档：`接口文档/接口文档_9_Reports.md`
- Python：`ai-service/app/main.py`、`ai-service/career_ai/report_support_service.py`、`ai-service/tests/test_report_support_service.py`
- Java：`PythonReportsAiClient`、`PythonAiProperties`、`ReportServiceImpl`、`ReportDetailVO`
- Frontend：`website/src/api/reports.ts`

## 测试原因
本轮将 Reports 生成链路中的 AI 建议/evidence/diagnostics 从 Java Spring AI 直连迁移为 Java 调用 Python Reports-RAG 支撑服务。

## 测试环境
- Worktree：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-reports`
- Branch：`ai-rag-reports-python-rag`
- Python：本机 `python`
- Java/Maven：本机 Maven，项目 release 17
- Frontend：`website` 下先执行 `npm install` 后 `npm run build`

## 测试命令与结果

### Python 配置探测
命令：检查仓库根目录和 `ai-service/` 下 `pyproject.toml`、`setup.cfg`、`tox.ini`、`ruff.toml`、`.flake8`。
结果：未发现 Python formatter/linter 配置，本轮未运行 ruff/black/flake8。

### Python pytest
命令：
```powershell
$env:PYTHONPATH='ai-service'; python -B -m pytest ai-service/tests/test_report_support_service.py -q -p no:cacheprovider
```
初次结果：`8 passed in 2.25s`。
最终结果：2026-06-12 19:20 复跑 `9 passed in 2.76s`。
覆盖：RAG service 成功、evidence snippet 脱敏、跨 snippet 边界敏感值不泄露、empty retrieval、缺 reportId/userId；HTTP 200、400 空 body、400 非 JSON、500 handler 异常、empty retrieval 响应。

代表性请求样例：
```json
{
  "reportId": 12,
  "userId": 1001,
  "targetJobName": "Java backend engineer",
  "capabilityProfile": {"id": 7, "professionalSkills": ["Java", "Spring Boot"]},
  "resumeAnalysis": {"id": 44, "suggestions": ["Add performance metrics"]},
  "matchDetails": {"overall": 85},
  "metadataFilters": {
    "userId": 1001,
    "visibility": "private",
    "documentTypes": ["resume_analysis", "career_data", "capability_profile", "match_details", "action_plan", "development_path"]
  }
}
```

代表性响应样例：
```json
{
  "status": "OK",
  "aiSuggestions": "For Java backend engineer, the current match score is about 85...",
  "evidenceRefs": [
    {
      "id": "resume_analysis:12:summary:0",
      "sourceType": "resume_analysis",
      "snippet": "suggestions: Add performance metrics",
      "metadata": {"documentType": "resume_analysis", "documentId": 44, "reportId": 12}
    }
  ],
  "ragDiagnostics": {
    "status": "OK",
    "retrievalMode": "deterministic_fallback",
    "embeddingMode": "hash_embedding_fallback",
    "fusion": "rrf"
  }
}
```

### Java 指定测试
命令：
```powershell
mvn -pl server -am "-Dtest=PythonReportsAiClientTest,ReportServiceImplReportsRagTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```
初次结果：`BUILD SUCCESS`，总计 `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`。

二次修复后复跑：2026-06-12 18:49 重新执行同一命令，结果仍为 `BUILD SUCCESS`，总计 `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`。

代码审查阻断修复后最终复跑：2026-06-12 19:02 重新执行同一命令，结果为 `BUILD SUCCESS`，总计 `Tests run: 13, Failures: 0, Errors: 0, Skipped: 0`。

脱敏边界修复后最终复跑：2026-06-12 19:20 重新执行同一命令，结果为 `BUILD SUCCESS`，总计 `Tests run: 13, Failures: 0, Errors: 0, Skipped: 0`。

最终 Surefire XML 硬验收：
- `server/target/surefire-reports/TEST-com.itsheng.service.client.PythonReportsAiClientTest.xml`：`tests=9 failures=0 errors=0 skipped=0`
- `server/target/surefire-reports/TEST-com.itsheng.service.service.Impl.ReportServiceImplReportsRagTest.xml`：`tests=4 failures=0 errors=0 skipped=0`

覆盖：客户端 200、400、500、timeout、invalid JSON、2xx 缺失字段、未知 status、字段类型错误、empty retrieval、无重试；服务 Python 成功、Python 失败 fallback、empty retrieval fallback、能力画像缺失 FAILED。

### Maven compile
命令：
```powershell
mvn -pl server -am -DskipTests compile
```
结果：`BUILD SUCCESS`。2026-06-12 18:52、19:02 与 19:20 多次复跑仍为 `BUILD SUCCESS`。

### Frontend build
第一次命令：
```powershell
cd website
npm run build
```
失败原因：隔离 worktree 初始没有 `node_modules`，`vue-tsc` 不存在。

修复：
```powershell
npm install
npm run build
```
结果：`vite build` 成功。`npm install` 报告 7 个依赖漏洞（5 moderate、2 high），未执行 `npm audit fix`，因为会改依赖树且不属于本轮 Reports-RAG 范围。

二次复跑：2026-06-12 18:52、19:02 与 19:20 执行 `npm run build`，`vue-tsc && vite build` 成功。

### Whitespace 与运行时依赖探测
- `git diff --check HEAD`：通过，仅有 CRLF 转换 warning，无 whitespace error。
- Runtime 端口短超时探测：Java 8081 未监听，Python Reports 8090 未监听，PostgreSQL 5432 监听，PostgreSQL 5433 未监听，Redis 6379 未监听。
- 环境变量探测：`OPENAI_API_KEY`、`OSS_ACCESS_KEY_ID`、`OSS_ACCESS_KEY_SECRET`、`ALIYUN_OSS_ACCESS_KEY_ID`、`ALIYUN_OSS_ACCESS_KEY_SECRET` 均未设置。

## 失败与修复记录
- Python handler 初版将 `json.JSONDecodeError` 先被 `ValueError` 捕获，导致非 JSON 返回 `VALIDATION_ERROR`；已调整异常顺序。
- Python handler 初版使用实例字段 `self.service`，测试替换类变量不生效；已改为 `type(self).service`。
- Java 初版局部替换 `ReportServiceImpl.java` 时因 PowerShell `Set-Content` 改变编码导致编译失败；已恢复到 HEAD 后用最小字节级改动重新接入 `PythonReportsAiClient`，避免改变原有报告章节文案、PDF/CRUD 和状态生命周期语义。
- Java timeout 测试初始延迟不足，已调为 2 秒触发 1 秒超时。
- PDF mock 初始未返回上传结果，产生无关错误日志；已补 `Result.success("oss://reports/test.pdf")`。
- 集成审查指出 `metadataFilters.documentTypes` 示例、`ReportUpdateBody` 字段命名、`ReportTargetJob.id` 可空性和 `ReportDetailVO.status` 说明不一致；已统一为 Java 实际发送的 camelCase/可空/状态枚举口径，并在 2026-06-12 18:49 重新运行 Java 指定测试通过。
- 代码审查指出 Java 2xx invalid body 校验不足；已新增 `PythonReportsInvalidResponseException`，要求 status 为 `OK/EMPTY_RETRIEVAL`、`evidenceRefs` 为数组、`ragDiagnostics` 为对象、`aiSuggestions` 为字符串，并补 `{}`、未知状态、字段类型错误测试。
- 代码审查指出 Python evidence snippet 未脱敏；已对邮箱、手机号、身份证号做轻量脱敏，并补测试确认敏感值不会进入 snippet。
- 代码审查非阻断指出 evidence 可追踪性偏弱；已在 `metadata` 增加 `documentId` 与 `reportId`。
- 代码复审指出脱敏应先处理完整 chunk 再截取 snippet，避免敏感值跨 160 字符边界时留下前缀片段；已调整为完整文本脱敏后截取，并补边界测试。首次边界测试断言过紧导致 1 次 Python 测试失败，修正测试覆盖点后 2026-06-12 19:20 复跑通过。
- 集成复审指出 Java 测试文件被 `.gitignore` 忽略；提交门禁将使用 `git add -f` 纳入 `PythonReportsAiClientTest.java` 与 `ReportServiceImplReportsRagTest.java`，并用 `git ls-files` / cached diff 复验。

## Runtime smoke
未执行真实 Java 8081 + Python service + DB/Redis/OSS/JWT smoke。
原因：Java 8081 和 Python Reports 8090 未监听，Redis 6379 未监听，OSS 凭据和有效 JWT 不可用；不能声明端到端 runtime 通过。替代验证为 Python HTTP 契约测试、Java client/service 单元测试、Maven compile 和 frontend build。

## 子 Agent 验收结论
- Plan 需求覆盖：初审 FAIL 后修订 PASS。
- Plan 技术风险：初审 FAIL，补 automation memory 可审计 Plan 后 PASS。
- Goal 边界：PASS。
- Goal 验证命令与退出条件：PASS。
- 初次代码审查：FAIL，阻断项为 Java 2xx invalid body 校验不足与 Python evidence snippet 未脱敏；已修复并补测。
- 初次集成审查：PASS。
- 代码复审：首次 FAIL，阻断项为 snippet 脱敏截断顺序和 Java 测试未进入版本控制；已改为完整 chunk 脱敏后截断、补边界测试，并通过 `git add -f` 暂存两个 Java 测试文件。
- 最终代码复审：PASS，确认 invalid body、fallback 生命周期、snippet 脱敏边界和暂存范围无阻断。
- 最终集成复审：PASS，确认文档、Python、Java、前端类型、测试、测试日志和 cached allowlist 一致；runtime smoke 未执行原因合理且未声明端到端通过。
- 测试覆盖验收：PASS，确认当前 Python/Java/frontend/compile/diff 检查足以支撑 Reports-RAG 最小闭环；runtime smoke 属于后续依赖齐备后的非阻断项。
- 测试日志可信度验收：PASS，确认日志、暂存代码、Surefire XML、Python 测试数量、force-add Java 测试、runtime smoke 未执行原因和主工作区误触风险记录一致。

## 剩余风险
- Python Reports-RAG 仍是 deterministic fallback，不声明真实 pgvector、Dashscope embedding、LLM、cross-encoder 或离线质量评估完成。
- `ReportServiceImpl.java` 已从整文件重写回退为最小接入；仍需代码审查确认新增 Python fallback 不改变原有报告生成主流程。
- 主工作区曾被本轮误用相对 `apply_patch` 短暂写入，已尽力恢复通用未跟踪 `ai-service` 文件；主工作区 `接口文档_9_Reports.md` 原本就是脏文件，无法无损恢复到误写前状态。最终提交只在隔离 worktree 执行，并需记录该风险。

## 优化建议
- 依赖齐备后补真实 runtime smoke：启动 Java 8081、Python Reports 8090、PostgreSQL/pgvector、Redis，并使用有效 JWT 请求 `/api/reports/generate` 后轮询详情。
- 后续真实 RAG 阶段接入 pgvector、Dashscope embedding/LLM、可选 cross-encoder 和离线质量评估，不复用 deterministic fallback 作为生产质量声明。
- 可继续补两个窄测：职业数据缺失 `FAILED`、非报告 owner 访问被拒绝。

## 关联文件
- `接口文档/接口文档_9_Reports.md`
- `ai-service/app/main.py`
- `ai-service/career_ai/report_support_service.py`
- `server/src/main/java/com/itsheng/service/client/PythonReportsAiClient.java`
- `server/src/main/java/com/itsheng/service/service/Impl/ReportServiceImpl.java`
- `pojo/src/main/java/com/itsheng/pojo/vo/ReportDetailVO.java`
- `website/src/api/reports.ts`

## 关联提交
本日志随 Reports-RAG 提交一起提交；最终 commit hash 由真实 Obsidian 记录和 automation memory 承载，避免仓库内日志自引用导致 hash 循环。
