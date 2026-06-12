# 2026-06-13 00:25 Resume-AI Python RAG

## 对象
- 接口文档：`接口文档/接口文档_5_Resume.md`
- Python：`ai_service/resume_ai_service.py`, `ai_service/test_resume_ai_service.py`, `ai_service/README.md`
- Java：`PythonResumeAiClient`, `PythonAiProperties`, `ResumeServiceImpl`, `ResumeMapper`, `UserVectorStoreMapper`, mapper XML
- Java tests：`PythonResumeAiClientTest`, `ResumeServiceImplResumePythonTest`
- 前端审计：`website/src/api/resume.ts` 只读，未改

## 原因
将 Resume AI/RAG 从 Java Spring AI/pgvector 直连迁到 Python HTTP 边界，形成接口文档_5 的最小闭环。主工作区 `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning` 保持只读；本轮只在隔离 worktree `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-resume` 操作。

隔离 worktree 无本地 `AGENTS.md`，按本轮用户消息中的 AGENTS 指令执行。接口文档_5 存在历史乱码，文档变更采用 append-only/小块追加，不重排旧内容、不修复历史乱码。

## 环境
- cwd: `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-resume`
- branch: `ai-rag-resume-python-rag`
- remote: `https://github.com/sheng143998/AI-University-Student-Career-Planning.git`
- 时间：2026-06-13 00:25 +08:00

## 测试命令与结果

### Python unit
```powershell
python -B -m unittest discover -s ai_service -p "test_resume*.py"
```
结果：exit 0；`Ran 5 tests in 0.596s`; `OK`; failures=0, errors=0, skipped=0。

最终复跑：exit 0；`Ran 5 tests in 0.601s`; `OK`; failures=0, errors=0, skipped=0。

覆盖：contract shape、validation、recursive chunking、summary index、metadata filter including `document_type == resume`、diagnostics 脱敏、HTTP 200/400。

### Python HTTP smoke
```powershell
@'
import json, subprocess, sys, time, urllib.error, urllib.request
port = 8091
proc = subprocess.Popen([sys.executable, "-B", "-m", "ai_service.resume_ai_service", "--host", "127.0.0.1", "--port", str(port)])
try:
    base = f"http://127.0.0.1:{port}"
    # Readiness: POST invalid payload and accept HTTP 400 validation response.
    # Valid request: POST /api/v1/resume/analyze with synthetic vector_store_id, user_id,
    # synthetic resume_text, file_type, original_file_name, resume_file_path, and
    # metadata.source/visibility/document_type.
    # Assertions: valid 200, invalid blank resume_text 400, completed status,
    # parsed_data/scores/capability_profile present, diagnostics retrieval fields,
    # metadata_filters.document_type=resume, sensitive_text_included=false.
finally:
    proc.terminate()
'@ | python -B -
```
请求样例字段：`vector_store_id=vs-final-smoke-1`, `user_id=1001`, `resume_text` 为合成短文本，`metadata.source=resume_upload`, `metadata.visibility=user`, `metadata.document_type=resume`。该样例不包含真实简历正文、token、API key 或 OSS credential。

第一次终验脚本错误地轮询 `/health`，但 `resume_ai_service.py` 未暴露该路径，readiness 失败。已改为轮询实际 `POST /api/v1/resume/analyze` 的 400 validation 响应作为服务就绪信号。

最终结果：exit 0；valid HTTP 200；invalid HTTP 400；schema 完整；`rag_diagnostics.retrieval` 为 `bm25=true`, `embedding_fallback=hash`, `fusion=rrf`, `reranker=deterministic`; `metadata_filters.document_type=resume`; `sensitive_text_included=false`; final smoke sample `chunk_count=1`; 进程已清理。

### Java 指定测试
第一次未加引号运行 Maven 指定测试时 PowerShell 将逗号解析为参数列表，命令解析失败，已修复为引号参数。

```powershell
mvn -pl server -am "-Dtest=PythonResumeAiClientTest,ResumeServiceImplResumePythonTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```
最终结果：exit 0；Surefire:
- `PythonResumeAiClientTest`: tests=6, failures=0, errors=0, skipped=0
- `ResumeServiceImplResumePythonTest`: tests=4, failures=0, errors=0, skipped=0
- total: tests=10, failures=0, errors=0, skipped=0

最终复跑：exit 0；Surefire XML 再核对：
- `TEST-com.itsheng.service.client.PythonResumeAiClientTest.xml`: tests=6, failures=0, errors=0, skipped=0
- `TEST-com.itsheng.service.service.Impl.ResumeServiceImplResumePythonTest.xml`: tests=4, failures=0, errors=0, skipped=0

覆盖：Python client success, 5xx, timeout, empty body, invalid JSON, schema failure；Resume success flow, failure flow, user boundary, idempotent row reuse, Python payload and persistence of `parsed_data/scores/highlights/suggestions/capability_profile`。

### Java compile
```powershell
mvn -pl server -am -DskipTests compile
```
结果：exit 0；BUILD SUCCESS。曾失败于 `ResumeServiceImpl.java` 首字节 BOM，已用 UTF-8 no BOM 重写修复。

最终复跑：exit 0；BUILD SUCCESS。

### Static gates
```powershell
git diff --check
git diff --cached --check
rg -n "\bChatClient\b|\bChatResponse\b|\bVectorStore\b|\bOpenAiEmbeddingModel\b|\bSystemConstants\b|\bJobVectorSearchService\b|resumeAnalysisChatClient|pgVectorStore|embeddingModel|pgVectorStore\.add|callAiForAnalysis|AI 返回的解析结果|AI 返回的能力画像 JSON|raw Python response|raw resume" server\src\main\java\com\itsheng\service\service\Impl\ResumeServiceImpl.java
```
结果：`git diff --check` exit 0；`git diff --cached --check` exit 0；grep gate 无匹配。

### Scope, secret and remote gates
```powershell
git -c core.quotepath=false diff --cached --name-only
git -c core.quotepath=false diff --name-only
git -c core.quotepath=false ls-files --others --exclude-standard
git remote -v
git ls-remote --heads origin ai-rag-resume-python-rag
gh pr list --head ai-rag-resume-python-rag --state all --json number,title,state,url
```
结果：
- changed scope 共 14 个文件，均在 Resume/Python/接口文档/测试日志 allowlist 内。
- denylist 命中 0：未改 `database/`、`website/`、`ai-service/`、`application*.yml`、`target/`、`dist/`、`node_modules/`、缓存目录。
- strict diff-only secret scan：`strict_diff_secret_hits=0`。第一次文件级扫描把普通变量名 `token` / `TokenTextSplitter` 误报为密钥，已改为只检查新增 diff 中真实 secret assignment、Bearer、private key、OSS secret、raw response/raw resume 形态。
- remote 指向 `https://github.com/sheng143998/AI-University-Student-Career-Planning.git`。
- 远端同名分支无输出；`gh pr list` 返回 `[]`，未 push、未 merge、未开 PR。

### Java tests tracking
```powershell
git check-ignore -v server/src/test/java/com/itsheng/service/client/PythonResumeAiClientTest.java server/src/test/java/com/itsheng/service/service/Impl/ResumeServiceImplResumePythonTest.java
git add -f server/src/test/java/com/itsheng/service/client/PythonResumeAiClientTest.java server/src/test/java/com/itsheng/service/service/Impl/ResumeServiceImplResumePythonTest.java
git ls-files 'server/src/test/**'
```
结果：`.gitignore` 命中 `**/test/`；已用 `git add -f`；`git ls-files` 可见两个测试文件。

### Frontend audit
`website/src/api/resume.ts` 未改；只读审计确认浏览器仅调用 Java `/api/resume/**`，未直连 Python `/api/v1/resume/analyze` 或 8091。因此未运行 `npm run build`。

## 修复记录
- `ResumeServiceImpl` 移除 Java `ChatClient/ChatResponse/VectorStore/OpenAiEmbeddingModel/SystemConstants/JobVectorSearchService` Resume AI/RAG 执行路径，改用 `PythonResumeAiClient.analyze`。
- `upload` 不再写 fake embedding/Java pgvector，改为 content-only `user_vector_store.upsert`。
- `analyzeAndSave` 先按 `vector_store_id + user_id` 查最新行，存在则复用并按 `id + user_id` 更新；不存在才 insert，后续进度/完成/失败均按 `id + user_id` 更新。
- `getAnalysisResult`、`preview`、`getPreviewUrl` 改为 BaseContext userId 归属查询。
- Python Resume service 实现 deterministic fallback RAG：recursive chunking、summary index、metadata filter、Multi-Query、BM25、hash embedding fallback、RRF、deterministic rerank、sanitized diagnostics。
- `PythonResumeAiClient` 非 2xx 不保存响应正文，只保存响应长度摘要。
- `ResumeMapper.xml` 的 `selectByVectorStoreIdAndUserId` 改为 `ORDER BY id DESC LIMIT 1`，新增 `updateByIdAndUserId`。
- `metadata_filter` 增加 `document_type == resume`。
- 接口文档_5 补 Java 层幂等实现、`FUCHUANG_AI_PYTHON_BASE_URL` 优先级和验证门禁。

## 子 Agent 验收
- Plan 需求覆盖：初审 FAIL，补强后 PASS。
- Plan 技术风险：多轮 FAIL，修正 allowlist/denylist、grep、append-only、Java tests gate 后 PASS。
- Goal 边界：PASS。
- Goal 验证/退出条件：初审 FAIL，补 Python fallback、Skipped=0、remote gate 后 PASS。
- 代码审查：初审 FAIL，指出 progress userId、metadata document_type、测试跟踪；已修。
- 集成审查：初审 FAIL，指出分析记录幂等；已修，复审 PASS。
- 代码最终复审：PASS。阻塞已关闭：`analysisId == null` 时不再回退 `resumeMapper.update(vectorStoreId)`；完成/失败/进度主路径均走 `updateByIdAndUserId`；新增 Python/Java 文件已暂存；Java 指定测试 10/10 通过。
- 测试覆盖验收：待本日志更新后由子 Agent 只读验收。
- 测试日志可信度验收：待本日志更新后由子 Agent 只读验收。

## 剩余风险
- 仍为 deterministic fallback，不声明真实 pgvector、Dashscope embedding/LLM、cross-encoder 或离线 RAG 质量评估完成。
- 未执行真实 Java 8081 + Python 8091 + PostgreSQL/Redis/OSS/JWT 端到端 runtime smoke。
- 数据库未新增 `(vector_store_id,user_id)` 唯一约束，本轮用 Java/MyBatis 层幂等收敛，避免触碰 `database/`。
- Maven 使用阿里云仓库时出现 bouncycastle metadata checksum warning，但构建和测试最终通过。

## 优化建议
- 依赖齐备后补真实端到端 smoke：启动 Java 8081、Python Resume 8091、PostgreSQL/pgvector、Redis、OSS/JWT 测试凭据，验证上传、异步轮询、预览和能力画像读取。
- 后续若允许触碰数据库迁移，可评估 `(vector_store_id,user_id)` 唯一约束或局部唯一索引，减少应用层幂等对历史重复行的依赖。
- 生产化阶段再接入真实 embedding/pgvector、Dashscope LLM、可选 cross-encoder/reranker，并补离线 RAG 质量评估集，不把 deterministic fallback 误认为最终质量闭环。
- 若前端将来展示 `rag_diagnostics` 或 evidence，需要先补 TypeScript 类型和用户态脱敏展示测试。

## 关联文件
- `接口文档/接口文档_5_Resume.md`
- `ai_service/resume_ai_service.py`
- `ai_service/test_resume_ai_service.py`
- `ai_service/README.md`
- `server/src/main/java/com/itsheng/service/client/PythonResumeAiClient.java`
- `server/src/main/java/com/itsheng/service/config/PythonAiProperties.java`
- `server/src/main/java/com/itsheng/service/service/Impl/ResumeServiceImpl.java`
- `server/src/main/java/com/itsheng/service/mapper/ResumeMapper.java`
- `server/src/main/java/com/itsheng/service/mapper/UserVectorStoreMapper.java`
- `server/src/main/resources/mapper/ResumeMapper.xml`
- `server/src/main/resources/mapper/UserVectorStoreMapper.xml`
- `server/src/test/java/com/itsheng/service/client/PythonResumeAiClientTest.java`
- `server/src/test/java/com/itsheng/service/service/Impl/ResumeServiceImplResumePythonTest.java`

## 提交
预提交状态：14 个文件已暂存，均在 Resume/Python/接口文档/测试日志 allowlist 内；尚未本地提交，未 push、未 merge、未开 PR。本日志随本轮本地提交一起进入仓库，避免在同一文件内自引用最终 commit hash。最终 hash、post-commit gate、未 push/merge/PR 状态由仓库外 Obsidian 记录和 automation memory 承载。
