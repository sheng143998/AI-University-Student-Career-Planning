# Chat Java-to-Python RAG 边界复验

- 时间：2026-06-08 23:19:13 +08:00
- 自动化 ID：ai-rag
- 隔离 worktree：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-chat`
- 分支：`ai-rag-chat-python-boundary`
- 复验时提交：`3cdd9f73fb2bef02cc04092e0787d480ba77cf85`
- amend 后最终本地提交：以 `git log -1` 为准 (`feat: route chat rag through python service`)
- 远端：`https://github.com/sheng143998/AI-University-Student-Career-Planning.git`

## 测试对象

- `ai-service` Chat 专属 Python RAG 边界服务。
- Java Chat 到 Python 的胶水层：`PythonAiProperties`、`PythonChatClient`、`ChatController`、`ChatRestController`、`ChatServiceImpl`、`ChatMessageMapper` 与 XML。
- 接口文档：`接口文档/接口文档_6_Chat.md`。
- 测试：`ai-service/tests/test_chat_pipeline.py`、`server/src/test/java/com/itsheng/service/client/PythonChatClientTest.java`。

## 测试原因

- 上轮已形成 Chat Python 边界本地提交，本轮自动化需要复验文档、实现、测试日志和真实 Obsidian 记录是否一致。
- 主工作区存在大量既有脏文件，本轮必须继续只使用隔离 worktree，防止 `application.yml`、`application-dev.yml`、`ai_service/**`、`website/**`、`database/**` 或非 Chat 模块进入提交。
- 代码审查子 Agent 曾因 503 失败，本轮需补齐替代审查和测试日志验收。

## 测试环境

- OS：Windows / PowerShell
- Java/Maven：使用项目本地 Maven 命令执行。
- Python：直接执行 `python ai-service\tests\test_chat_pipeline.py`。
- Frontend：在 `website` 中执行 `npm run build`。
- runtime smoke 前置条件探测：
  - `127.0.0.1:6379=False`
  - `127.0.0.1:5433=False`
  - `127.0.0.1:8081=False`
  - `127.0.0.1:8092=False`
  - `OPENAI_API_KEY=False`

## 测试方法与命令

1. Python pipeline：
   `python ai-service\tests\test_chat_pipeline.py`
2. Java Chat client 窄测：
   `mvn -pl server -am -Dtest=PythonChatClientTest "-Dsurefire.failIfNoSpecifiedTests=false" test`
3. Java 编译：
   `mvn -pl server -am -DskipTests compile`
4. 前端兼容构建：
   `cd website && npm run build`
5. 端口/契约反查：
   `rg -n "8090|8091|8092|FUCHUANG_AI_PYTHON_CHAT_BASE_URL|AI_SERVICE_PORT|chat-base-url|chatBaseUrl|/api/v1/chat" ai-service server/src/main/java 接口文档\接口文档_6_Chat.md`
6. Spring AI ChatClient 边界反查：
   `rg -n "org\.springframework\.ai\.chat\.client\.ChatClient" server/src/main/java/com/itsheng/service/controller/ChatController.java server/src/main/java/com/itsheng/service/service/Impl/ChatServiceImpl.java server/src/main/java/com/itsheng/service/client/PythonChatClient.java`
7. Chat-only 范围门禁：
   `git status --short --branch`
   `git -c core.quotepath=false diff --name-only origin/master..HEAD`
   denylist 反查：`application.yml|application-dev.yml|^ai_service/|^website/|^database/|__pycache__|\.pyc$|\.zip$|target/|node_modules|dist/`
   `git diff --check origin/master..HEAD`

## 测试数据或请求样例

最终 Java-to-Python Chat 契约样例：

```json
{
  "userId": 1,
  "conversationId": 10,
  "content": "前端开发未来趋势，并结合我的简历给建议",
  "resumeId": 123,
  "parsedData": {
    "skills": ["Vue", "TypeScript"],
    "targetRole": "前端开发工程师"
  },
  "history": [
    { "role": "user", "content": "我想准备 TypeScript 面试", "createdAt": "2026-06-08 09:00:00" }
  ],
  "retrievalOptions": {
    "multiQuery": true,
    "hybridSearch": true,
    "ragFusion": true,
    "metadataFilter": {
      "userId": 1,
      "documentTypes": ["resume", "job", "chat_context"],
      "resumeId": 123,
      "visibilityScope": "private"
    }
  }
}
```

Python Chat HTTP contract 窄测中另使用了 `documentTypes=["resume"]` 的最小请求体，仅用于验证 handler 返回 `content`、`evidence`、`diagnostics` 等字段，不代表最终 Java 实际请求的完整 metadata filter。

Java client 窄测断言命中：

```text
POST /api/v1/chat/complete
POST /api/v1/chat/daily-suggestions
```

## 实际结果

- Python pipeline：通过，输出 `chat pipeline tests passed`。
- Java Chat client 窄测：通过，`Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- Java 编译：通过，`fuchuang/common/pojo/server` reactor 全部 `SUCCESS`。
- 前端构建：通过，`vue-tsc && vite build` 成功；`website/dist` 不纳入提交。
- 端口/契约反查：通过，`ai-service` 默认 `8092`；`PythonAiProperties.chatBaseUrl` 默认 `http://127.0.0.1:8092` 并支持 `FUCHUANG_AI_PYTHON_CHAT_BASE_URL`；`PythonChatClient` 调用 `/api/v1/chat/complete` 和 `/api/v1/chat/daily-suggestions`；文档记录 `ai-service=8092`、`ai_service=8090`、Resume-AI `8091`。
- Spring AI ChatClient 边界反查：通过，ChatController、ChatServiceImpl、PythonChatClient 中无 `org.springframework.ai.chat.client.ChatClient` 引用。
- Chat-only 范围门禁：通过，当前 diff 相对 `origin/master` 只包含 Chat allowlist 文件；denylist 无命中；`git diff --check origin/master..HEAD` 无输出。

## 失败原因与修复记录

- 本轮固定验证无失败。
- runtime Java Chat smoke 未执行，不声明端到端通过。原因是 Redis、PostgreSQL/pgvector、Java 8081、Python Chat 8092 和 `OPENAI_API_KEY` 未同时具备。
- 早期代码审查子 Agent 一次 503 失败；本轮已补发替代代码审查并通过，无 P0/P1/P2 阻断问题。

## 子 Agent 验收结论

- Plan 需求覆盖初审：FAIL，要求补齐 automation memory/git status 前置、notes 预读前置、真实 Obsidian 路径和越界停止规则；已处理。
- Plan 技术风险初审：FAIL，要求补齐精确 allowlist/denylist、固定验证命令、runtime smoke 不虚报和不 push 边界；已处理。
- Plan 第三版复审：PASS。
- Goal 边界审查：PASS，确认本轮只覆盖 Chat Python 边界，不包含生产级 pgvector/embedding/LLM。
- Goal 验证命令与退出条件审查：PASS，明确不能声明端到端 runtime smoke 通过。
- 集成审查：PASS（有条件），确认 Java-Python 边界、前端文本流兼容、文档/README/测试日志/Obsidian 口径一致；生产化前需补 runtime smoke、`/ai/chat` 生产暴露评估、真实 pgvector/embedding/LLM 和 evidence 展示协议。
- 代码审查：前一次子 Agent 因 503 失败；替代代码审查 PASS，无 P0/P1/P2 阻断问题。P3 风险为 `/ai/chat` 仍是未纳入 `/api/**` JWT 的兼容调试入口，生产环境应关闭或加鉴权；Python diagnostics 中 `retrieval="bm25+embedding"` 为机器可读 fallback 标识，文档/README 已说明不是生产级真实 embedding。

## 剩余风险

- 当前 `ai-service` 是 deterministic fallback 边界雏形，不是生产级完整 RAG/LLM。
- 未接入真实 pgvector、Dashscope embedding/LLM、完整简历/JD 知识库、ranking model 或质量评估集。
- `/ai/chat` 仍是兼容调试入口，不在 `/api/**` JWT 拦截范围内；生产环境应关闭或加鉴权。
- runtime smoke 未执行，不能证明 Java 8081 + Python Chat 8092 + DB/Redis + Dashscope 的端到端链路。
- 现有仓库文件存在历史编码显示问题，本轮未扩大到编码修复。

## 优化建议

1. 下一轮先补真实 runtime smoke 环境：启动 Redis、PostgreSQL/pgvector、Java 8081、Python Chat 8092，并设置 `OPENAI_API_KEY`。
2. 再独立立项接入真实 embedding/pgvector 和 LLM，避免把生产级 RAG 与本轮边界验收混在同一提交。
3. 若前端要展示 evidence、diagnostics 或 suggestionQuestions，应新增 SSE/JSON 流协议，不能破坏当前 `/api/chat/messages` 文本流兼容。

## 关联代码、接口文档与提交

- 代码提交：以 `git log -1` 为准 (`feat: route chat rag through python service`)
- 本复验日志状态：已随本轮 Chat-only amend 纳入本地提交；不属于原始 `3cdd9f73...` 的提交内容。
- Python 服务：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-chat\ai-service`
- Java client：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-chat\server\src\main\java\com\itsheng\service\client\PythonChatClient.java`
- Java service：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-chat\server\src\main\java\com\itsheng\service\service\Impl\ChatServiceImpl.java`
- 接口文档：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-chat\接口文档\接口文档_6_Chat.md`
- Obsidian 使用记录：`C:\Users\WhenJayHe\notes\study\项目使用记录\AI-University-Student-Career-Planning\接口文档_6_Chat_监督记录.md`
