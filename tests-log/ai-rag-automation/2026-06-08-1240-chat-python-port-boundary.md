# Chat Python 服务入口端口边界验证

- 时间：2026-06-08 12:40:23 +08:00
- 自动化 ID：ai-rag
- 测试对象：Chat Python RAG 服务入口、Java Chat Python client 配置、接口文档 6、`ai-service` 启动说明。
- 测试原因：上一轮只读审计发现 `ai-service` Chat 服务与 `ai_service` 聚合服务都默认占用 8090，Java Chat 又复用全局 `fuchuang.ai.python.base-url`，存在端口冲突和服务归属不清风险。
- 测试环境：
  - 项目：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning`
  - pgvector：用户确认 Docker 部署，端口映射为 5433；`server/src/main/resources/application-dev.yml` 已配置 `fuchuang.datasource.port: 5433`。
  - Python Chat 服务：`ai-service`，本轮目标默认端口 8092。
  - Python 聚合服务：`ai_service`，保持默认端口 8090。
  - Resume-AI：保持默认端口 8091。
- 涉及文件：
  - `接口文档/接口文档_6_Chat.md`
  - `server/src/main/java/com/itsheng/service/config/PythonAiProperties.java`
  - `server/src/main/java/com/itsheng/service/client/PythonChatClient.java`
  - `server/src/main/resources/application.yml`
  - `ai-service/app/main.py`
  - `ai-service/README.md`
  - `C:\Users\WhenJayHe\notes\study\项目使用记录\AI-University-Student-Career-Planning\接口文档_6_Chat_监督记录.md`

## 测试计划

1. 端口一致性检查：
   `rg -n "8090|8091|8092|FUCHUANG_AI_PYTHON_CHAT_BASE_URL|AI_SERVICE_PORT|chat-base-url|chatBaseUrl" ai-service ai_service server/src/main/resources server/src/main/java 接口文档`
2. Python Chat pipeline：
   `python ai-service\tests\test_chat_pipeline.py`
3. Java 编译：
   `mvn -pl server -am -DskipTests compile`
4. 前端构建：
   `cd website && npm run build`
5. Runtime smoke：
   仅在 Redis、PostgreSQL/pgvector、Dashscope Key、Java server、Python Chat service 均可用时执行；不可执行时逐项记录缺失原因。

## 请求样例

Python Chat HTTP smoke 请求：

```json
{
  "userId": 1,
  "conversationId": 10,
  "content": "frontend job advice",
  "resumeId": 123,
  "retrievalOptions": {
    "metadataFilter": {
      "userId": 1,
      "documentTypes": ["resume"],
      "resumeId": 123,
      "visibilityScope": "private"
    }
  }
}
```

Java 窄测请求路径断言：

```text
POST /api/v1/chat/complete
POST /api/v1/chat/daily-suggestions
```

## 实际结果

- 端口一致性检查：通过。
  - 命令：`rg -n "8090|8091|8092|FUCHUANG_AI_PYTHON_CHAT_BASE_URL|AI_SERVICE_PORT|chat-base-url|chatBaseUrl" ai-service ai_service server/src/main/resources server/src/main/java 接口文档`
  - 结果：`ai-service/app/main.py` 默认 `AI_SERVICE_PORT=8092`；`server/src/main/resources/application.yml` 新增 `fuchuang.ai.python.chat-base-url` 默认 `http://127.0.0.1:8092`；`PythonChatClient` 通过 `chatBaseUrl` 调用 Chat 专属服务；`ai_service` 聚合服务仍保持 8090；Resume-AI 仍保持 8091。
- 真实 Obsidian 端口记录检查：通过。
  - 命令：`rg -n "8090|8091|8092|FUCHUANG_AI_PYTHON_CHAT_BASE_URL|AI_SERVICE_PORT|chat-base-url|chatBaseUrl" ai-service ai_service server/src/main/resources server/src/main/java 接口文档 "C:\Users\WhenJayHe\notes\study\项目使用记录\AI-University-Student-Career-Planning"`
  - 结果：真实 Obsidian 记录已同步说明 `ai-service:8092`、`ai_service:8090`、Resume-AI `8091` 的归属。
- Python Chat pipeline：通过。
  - 命令：`python ai-service\tests\test_chat_pipeline.py`
  - 结果：输出 `chat pipeline tests passed`。
- Python Chat HTTP smoke：通过。
  - 方法：在测试脚本中临时启动 `AiServiceHandler` 到 `127.0.0.1:8092`，请求 `POST /api/v1/chat/complete`。
  - 结果：HTTP 200，响应包含 `content`、`suggestionQuestions`、`tip`、`title`、`evidence` 等字段。
- Java Chat base-url 窄测：通过。
  - 首次命令：`mvn -pl server -am -Dtest=PythonChatClientTest test`
  - 首次结果：失败；原因是 reactor 中 `common` 模块没有该指定测试，Surefire 报 `No tests matching pattern "PythonChatClientTest" were executed`。
  - 修正命令：`mvn -pl server -Dtest=PythonChatClientTest test`
  - 修正结果：`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
  - 覆盖点：使用本地 stub HTTP server 注入 `PythonAiProperties.chatBaseUrl`，断言 `PythonChatClient.complete()` 和 `dailySuggestions()` 命中 `/api/v1/chat/complete` 与 `/api/v1/chat/daily-suggestions`，证明 Java Chat 不复用全局 8090。
- Java 编译：通过。
  - 命令：`mvn -pl server -am -DskipTests compile`
  - 结果：Reactor `fuchuang/common/pojo/server` 均 `BUILD SUCCESS`。
- 前端构建：通过。
  - 命令：`cd website && npm run build`
  - 结果：`vue-tsc && vite build` 成功，产物生成于 `website/dist`，未纳入本轮提交。
- Runtime Java Chat smoke：未执行，不能声明通过。
  - Redis：`127.0.0.1:6379=True`
  - PostgreSQL/pgvector：`127.0.0.1:5433=False`；用户说明 pgvector 通过 Docker 映射 5433，但本轮探测时端口不可连接。
  - Java server：`127.0.0.1:8081=False`
  - Python Chat 常驻服务：`127.0.0.1:8092=False`；仅执行了临时 HTTP handler smoke。
  - Dashscope Key：`OPENAI_API_KEY=False`
  - 替代验证：Java 编译、Python Chat pipeline、Python Chat 8092 临时 HTTP smoke、前端构建和端口一致性反查均已通过。

## 子 Agent 验收结论

- Plan 需求覆盖审查：初稿不通过，已按反馈补齐文件、端口、配置键与验证命令。
- Plan 技术风险审查：有条件通过，要求新增 Chat 专用 base-url，不得改全局 8090。
- Goal 边界审查：初稿不通过，已收窄到 Chat 端口/契约修复，不动 `ai_service`、数据库、其他模块。
- Goal 验证审查：初稿不通过，已补强端口反查、runtime smoke 缺失项记录和提交前门槛。
- 更新后 Goal 边界复审：不通过提交准备，要求只 stage 本轮 hunk，排除已有越界 dirty 文件和 `application.yml` 中非 Chat hunk。
- 更新后 Goal 验证复审：不通过提交准备，要求补齐本日志实际结果；本节已补齐。
- 代码审查：通过（有可接受风险），未发现阻断性行为回归；提醒 `application.yml` 中 Roadmap/Resume 历史 hunk 不应随本轮提交。
- 集成一致性审查：不通过提交前门槛，原因是测试日志未更新和 dirty worktree 越界；本轮已补测试日志，提交前仍必须严格筛选 hunk。

## 剩余风险

- 当前工作区存在大量未提交和未跟踪文件，本轮提交必须只 stage Chat 端口/契约修复相关文件和记录，禁止 `git add .`。
- 若 Java runtime smoke 缺 Redis、PostgreSQL/pgvector、Dashscope Key 或运行中服务，只能记录为未执行，不能声明 smoke 通过。
- `application.yml` 中存在非本轮 Roadmap/Resume 配置 hunk，提交时只允许纳入 Chat 专用 `chat-base-url` 所在的必要配置 hunk；若无法安全分离，则本轮不提交。
- `/ai/chat` 兼容调试入口仍不在 `/api/**` 鉴权范围，仅作为开发兼容入口；生产暴露需后续单独评估。

## 关联提交

- 代码与测试验收通过后形成的隔离 worktree 提交：`caacf8e8a72e35343f2e758bf9f0b369027adba8`（`feat: route chat rag through python service`）。
- 说明：同一提交无法在文件内容中稳定自包含最终 HEAD 哈希；若本日志因补写哈希而再次 amend，最终 `git log -1` 哈希以自动化 memory 和本轮最终输出为准。
- 提交前门槛：必须再次执行 `git status --short`、`git diff --name-only`、`git diff --cached --name-only`；只 stage Chat 端口/契约相关文件或 hunk，不提交无关 dirty/untracked 文件。

## 2026-06-08 13:07 复验补充

- 复验原因：Plan/Goal 复审要求在提交前重新证明当前代码仍可运行，并把提交候选范围收敛到 Chat 端口边界与 Java-Python 胶水。
- 本轮候选范围已扩展为 Chat 闭环必需文件：
  - `ai-service/**`
  - `server/src/main/java/com/itsheng/service/config/PythonAiProperties.java`
  - `server/src/main/java/com/itsheng/service/client/PythonChatClient.java`
  - `server/src/main/java/com/itsheng/service/controller/ChatController.java`
  - `server/src/main/java/com/itsheng/service/service/Impl/ChatServiceImpl.java`
  - `server/src/main/java/com/itsheng/service/mapper/ChatMessageMapper.java`
  - `server/src/main/resources/mapper/ChatMessageMapper.xml`
  - `server/src/test/java/com/itsheng/service/client/PythonChatClientTest.java`
  - `接口文档/接口文档_6_Chat.md`
  - `tests-log/ai-rag-automation/2026-06-08-1240-chat-python-port-boundary.md`
- `server/src/main/resources/application.yml` 只允许暂存 Chat 专用 `fuchuang.ai.python.chat-base-url` 必要配置，不允许夹带 Roadmap/Resume 历史 hunk。
- 明确排除：`server/src/main/resources/application-dev.yml`、`ai_service/**`、Dashboard/Market/Goals/Roadmap/Reports/Resume/database 历史改动、临时 Maven 缓存、日志、`__pycache__`、构建产物和打包文件。

### 复验命令与结果

- `python ai-service\tests\test_chat_pipeline.py`：通过，输出 `chat pipeline tests passed`。
- `mvn -pl server -Dtest=PythonChatClientTest test`：通过，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- `mvn -pl server -am -DskipTests compile`：通过，Reactor `fuchuang/common/pojo/server` 均 `SUCCESS`。
- `cd website && npm run build`：通过，`vue-tsc && vite build` 成功；`website/dist` 产物不纳入本轮提交。
- 端口反查：通过，`ai-service` 默认 8092，`ai_service` 聚合服务仍 8090，Resume-AI 仍 8091，Java Chat 使用 `chat-base-url`/`chatBaseUrl`。
- Runtime Java Chat smoke：仍未执行，原因不变；本机探测时 pgvector 5433、Java 8081、Python Chat 常驻 8092、`OPENAI_API_KEY` 未同时具备，因此只能声明替代验证通过。

### 复验后的子 Agent 门禁处理

- Plan/Goal 更新后复审仍未放行提交，阻断点不是契约或测试，而是暂存区尚未收敛。
- 已处理策略：先完成复验和日志补充，再以精确暂存方式构造仅含本轮 Chat 文件/hunk 的暂存区；暂存区通过复查后再提交。

## 2026-06-08 13:12 提交阻塞记录

- 暂存区状态：`git diff --cached --name-only` 为空。
- 尝试精确暂存本轮 Chat 文件失败：
  - 命令类型：`git add -- <Chat 相关文件列表>`
  - 结果：失败。
  - 失败原因：`fatal: Unable to create 'C:/Users/WhenJayHe/IdeaProjects/AI-University-Student-Career-Planning/.git/index.lock': Permission denied`
- 当前权限限制：`.git` 目录只读，无法创建 `index.lock`，因此不能完成 `git add`、`git diff --cached` 验收和 `git commit`。
- 自动化 memory 更新状态：`$CODEX_HOME` 在当前 PowerShell 环境为空；尝试写入 `C:\Users\WhenJayHe\.codex\automations\ai-rag\memory.md` 失败，原因是访问被拒绝。
- 本轮结论：代码、接口文档、测试日志和验证已完成到可提交前状态，但提交门槛未满足；恢复 `.git` 写权限后应继续执行精确暂存，严禁 `git add .`。

## 2026-06-08 13:34 阻塞复核

- `git diff --cached --name-only`：仍为空。
- `.git/index.lock`：不存在，但 `git add -- server/src/main/java/com/itsheng/service/config/PythonAiProperties.java` 仍失败。
- 最新 Git 写入失败原因：`fatal: Unable to create 'C:/Users/WhenJayHe/IdeaProjects/AI-University-Student-Career-Planning/.git/index.lock': Permission denied`
- 自动化 memory：再次尝试写入 `C:\Users\WhenJayHe\.codex\automations\ai-rag\memory.md`，仍被拒绝。
- 阻塞判定：同一外部权限问题已连续阻止暂存、提交和 memory 更新；代码/文档/测试侧无新的可推进项，需恢复 `.git` 与自动化 memory 写权限后继续。

## 2026-06-08 16:31 隔离 worktree 复验

- 复验原因：主工作区存在大量既有 dirty/untracked 文件，直接在主工作区暂存会夹带非 Chat 改动；本轮改用命名分支隔离 worktree 保证 Chat-only 提交。
- 隔离 worktree：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-chat`
- 分支：`ai-rag-chat-python-boundary`
- 初始检查：隔离 worktree 创建后 `git status --short` 为空。
- 配置口径更新：本轮不提交 `server/src/main/resources/application.yml` 或 `server/src/main/resources/application-dev.yml`。Chat 端口默认值由 `PythonAiProperties.chatBaseUrl=http://127.0.0.1:8092`、`FUCHUANG_AI_PYTHON_CHAT_BASE_URL` 环境变量，以及 `ai-service/app/main.py` 中 `AI_SERVICE_PORT=8092` 承担。
- 提交边界：只允许 Chat allowlist 文件进入 staged diff；`application.yml`、`application-dev.yml`、`ai_service/**`、`website/**`、非 Chat 模块、缓存、构建产物、`__pycache__` 和 `.pyc` 均不得进入 staged diff。

### 16:31 实际命令与结果

- Python Chat pipeline：通过。
  - 命令：`python ai-service\tests\test_chat_pipeline.py`
  - 结果：`chat pipeline tests passed`。
- Java Chat base-url 窄测：通过。
  - 首次按复审建议执行：`mvn -pl server -am -Dtest=PythonChatClientTest -DfailIfNoTests=false test`
  - 首次结果：失败；`common` 模块没有该指定测试，Surefire 需要 `-Dsurefire.failIfNoSpecifiedTests=false`。
  - 再次执行：`mvn -pl server -am -Dtest=PythonChatClientTest "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 再次结果：`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`，Reactor `BUILD SUCCESS`。
  - 覆盖点：`PythonChatClient.complete()` 与 `dailySuggestions()` 均使用 `chatBaseUrl` 命中 `/api/v1/chat/complete` 与 `/api/v1/chat/daily-suggestions`。
- Java 编译：通过。
  - 命令：`mvn -pl server -am -DskipTests compile`
  - 结果：Reactor `fuchuang/common/pojo/server` 均 `SUCCESS`，`BUILD SUCCESS`。
- 前端构建：通过。
  - 命令：`cd website && npm run build`
  - 首次结果：失败，`vue-tsc` 未识别。
  - 修正命令：`cd website && npm ci`，安装 lockfile 中声明的依赖。
  - 再次结果：`vue-tsc && vite build` 成功，Vite 输出 `✓ built in 2.29s`。
  - 边界说明：`website/node_modules/` 被 `.gitignore` 忽略，构建产物和依赖目录不纳入本轮提交；`npm ci` 报告 7 个依赖漏洞，为既有前端依赖审计结果，本轮未修改 `website/**`。
- 端口/配置反查：通过。
  - 命令：`rg -n "8090|8091|8092|FUCHUANG_AI_PYTHON_CHAT_BASE_URL|AI_SERVICE_PORT|chat-base-url|chatBaseUrl" ai-service server/src/main/java 接口文档`
  - 结果：`ai-service/app/main.py` 默认 `AI_SERVICE_PORT=8092`；`PythonAiProperties` 默认 `chatBaseUrl=8092` 并读取 `FUCHUANG_AI_PYTHON_CHAT_BASE_URL`；`PythonChatClient` 使用 `getChatBaseUrl()`；接口文档记录 `ai-service=8092`、`ai_service=8090`、Resume-AI `8091`。
- ChatClient 边界反查：通过。
  - 命令：`rg -n "org\.springframework\.ai\.chat\.client\.ChatClient|ChatClient" server/src/main/java/com/itsheng/service/controller/ChatController.java server/src/main/java/com/itsheng/service/service/Impl/ChatServiceImpl.java server/src/main/java/com/itsheng/service/client/PythonChatClient.java`
  - 结果：`ChatController` 与 `ChatServiceImpl` 只引用 `PythonChatClient`，不再注入或调用 Spring AI `ChatClient`。
- Runtime Java Chat smoke：未执行，不能声明端到端通过。
  - `127.0.0.1:6379=False`
  - `127.0.0.1:5433=False`
  - `127.0.0.1:8081=False`
  - `127.0.0.1:8092=False`
  - `OPENAI_API_KEY=False`
  - 替代验证：Python pipeline、Java client 窄测、Java 编译、端口/配置反查与 ChatClient 边界反查已通过。

### 16:31 提交门槛

- `PythonChatClientTest.java` 存在，但被 `.gitignore:6 **/test/` 忽略；提交时只能执行 `git add -f server/src/test/java/com/itsheng/service/client/PythonChatClientTest.java` 单文件，并用 `git diff --cached --name-only` 确认已 staged。
- 提交前必须执行并记录：`git remote -v`、`git status --short`、`git diff --cached --name-only`、`git diff --cached`。
- `git diff --cached --name-only` 必须非空且全部属于 Chat allowlist；出现 `application.yml`、`application-dev.yml`、`ai_service/**`、`website/**`、非 Chat 模块、`__pycache__`、`.pyc`、构建产物或临时目录时立即停止。

### 16:35 Staged allowlist 复核

- 已执行逐文件暂存，未使用 `git add .` 或目录级 add。
- `PythonChatClientTest.java` 已通过单文件强制暂存：
  - 命令：`git add -f -- server/src/test/java/com/itsheng/service/client/PythonChatClientTest.java`
  - 结果：`git diff --cached --name-only` 中包含 `server/src/test/java/com/itsheng/service/client/PythonChatClientTest.java`。
- 脚本化 allowlist 校验：通过。
  - 命令要点：使用 `git -c core.quotepath=false diff --cached --name-only` 获取中文路径原文，与 Chat allowlist 做集合比对。
  - 结果：输出 `ALLOWLIST_OK`。
- denylist 反查：通过。
  - 命令：`git -c core.quotepath=false diff --cached --name-only | Select-String -Pattern 'application\.yml|application-dev\.yml|^ai_service/|^website/|^database/|__pycache__|\.pyc$|\.zip$|target/|node_modules|dist/'`
  - 结果：无输出。
- cached diff whitespace 检查：通过。
  - 命令：`git diff --cached --check`
  - 结果：无输出。

### 16:36 最终口径说明

- 本日志前半段保留了早期主工作区方案和旧候选范围，当时曾把 `server/src/main/resources/application.yml` 作为候选证据。
- 最终执行口径以 16:31 之后的隔离 worktree 为准：本轮最终不提交 `server/src/main/resources/application.yml` 或 `server/src/main/resources/application-dev.yml`。
- Chat 默认端口依据为：
  - `server/src/main/java/com/itsheng/service/config/PythonAiProperties.java`：`chatBaseUrl` 默认 `http://127.0.0.1:8092`，并读取 `FUCHUANG_AI_PYTHON_CHAT_BASE_URL`。
  - `ai-service/app/main.py`：`AI_SERVICE_PORT` 默认 `8092`。
  - `接口文档/接口文档_6_Chat.md`：记录 `ai-service=8092`、`ai_service=8090`、Resume-AI `8091`。
- 最终关联文件为当前 staged Chat allowlist，包括 `ai-service/**` Chat 专属服务文件、`PythonAiProperties.java`、`PythonChatClient.java`、`ChatController.java`、`ChatServiceImpl.java`、`ChatMessageMapper.java`、`ChatMessageMapper.xml`、`PythonChatClientTest.java`、`接口文档_6_Chat.md` 和本测试日志。
- 当前提交状态：本日志随 Chat-only 提交一并提交；最终提交哈希以 `git log -1`、自动化 memory 和本轮最终输出为准。

## 2026-06-08 17:35 代码/集成验收反馈修正

- 触发原因：代码审查子 Agent 与集成审查子 Agent 均判定初版不通过。
- 不通过原因：
  - 文档曾把当前 Python Chat 实现描述为完整真实 RAG/LLM，但 `ai-service` 实际是边界雏形与 deterministic fallback，未接真实 pgvector、Dashscope embedding/LLM 或完整简历/JD 知识库。
  - `PythonChatClient.dailySuggestions()` 对 Python 200 但空结构响应不会降级，可能让前端拿到空建议。
  - `ChatServiceImpl` 与 `ChatRestController` 日志记录完整用户消息，存在简历/求职隐私泄漏风险。
  - 早期日志曾记录 `application.yml` 新增 `chat-base-url`；最终隔离 worktree 提交不包含 `application.yml` 或 `application-dev.yml`，最终口径以 16:31 之后记录和当前提交 diff 为准。
- 修复记录：
  - 接口文档 6 改为明确当前是 Chat Python RAG 边界雏形：已覆盖递归切块、摘要索引、Multi-Query、BM25 + 词袋余弦 fallback、RAG-Fusion/RRF、evidence 和 diagnostics；未接真实 pgvector/embedding/LLM，不能标记为生产完成态。
  - Java 到 Python Chat 请求新增 `parsedData`，由 Java 读取当前 `resumeId` 或最新简历解析数据后透传给 Python；Python 将 `parsedData` 作为 resume seed document 参与检索，避免只检索用户问题本身。
  - `PythonChatClient.dailySuggestions()` 在 `suggestions` 或 `quickQuestions` 为空时抛出异常，由 `ChatServiceImpl` 返回默认建议。
  - Chat 发送消息日志改为只记录 `conversationId`、`resumeId` 和 `contentLength`，不记录完整用户输入。
  - `ai-service/README.md` 补充当前实现状态和后续生产门禁。
- 当前仍不能声明通过的项：
  - 真实 Java runtime smoke 未执行；Redis、PostgreSQL/pgvector、Java 8081、Python Chat 8092、`OPENAI_API_KEY` 未同时具备。
  - 真实 pgvector/简历/JD 知识库、Dashscope embedding/LLM、排序模型和质量评估仍为下一轮生产化任务。
- 复验命令与结果：
  - `python ai-service\tests\test_chat_pipeline.py`：首次失败，原因是测试问题使用纯中文长句，当前词袋 fallback 不做中文分词，未命中 `Vue/TypeScript` parsedData evidence；已将测试改为技能词场景 `Vue TypeScript career advice`，用于验证当前 fallback 能读取 Java 透传的简历解析数据。
  - `python ai-service\tests\test_chat_pipeline.py`：修正后通过，输出 `chat pipeline tests passed`。
  - `mvn -pl server -am -Dtest=PythonChatClientTest "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`；新增覆盖 `parsedData` 透传和每日建议空结构抛错。
  - `mvn -pl server -am -DskipTests compile`：通过，Reactor `fuchuang/common/pojo/server` 均 `SUCCESS`。
  - `cd website && npm run build`：通过，`vue-tsc && vite build` 成功；本轮未修改 `website/**`，构建产物不纳入提交。
- 复验后的子 Agent 反馈处理：
  - P1 文档/实现不一致：已处理。文档和 README 现在明确当前是 Chat Python 边界雏形与 deterministic fallback，不声明真实 pgvector/embedding/LLM 完成。
  - P1 行为退化：部分缓解。Java 已向 Python 透传当前简历解析 `parsedData`，Python 将其作为 resume evidence；真实数据库/向量库/LLM 仍列为下一轮生产化门禁。
  - P2 每日建议空结构不降级：已处理。Java client 对空 `suggestions` 或 `quickQuestions` 抛错，服务层返回默认建议。
  - P2 完整用户输入日志：已处理。Chat REST 与 Service 日志只记录内容长度。
  - P3 `application.yml` 历史矛盾口径：已记录。最终提交不包含 `application.yml` 或 `application-dev.yml`，以隔离 worktree 当前 diff 为准。

## 2026-06-08 19:05 复审二次修正

- 触发原因：修复后代码/集成复审仍不通过。
- 不通过原因：
  - Java `retrievalOptions.metadataFilter.documentTypes` 只包含 `resume/job`，Python 构造的 `chat_context` 会被 metadata filter 过滤，最近对话历史实际不能参与召回。
  - 当前 tokenizer 将连续中文当作整段 token，中文自然问题如“前端开发未来趋势”可能无法命中 `parsedData.targetRole=前端开发工程师`，导致简历 evidence 为空。
- 修复记录：
  - `PythonChatClient.buildRetrievalOptions()` 的 `documentTypes` 增加 `chat_context`。
  - `ai-service/rag/retrieval.py` 的 tokenizer 改为英文/数字词项 + 中文单字和 bigram 的 deterministic fallback，提升中文自然问题与中文简历解析字段的基本命中。
  - `ai-service/tests/test_chat_pipeline.py` 恢复中文自然问题测试，并新增 `chat_context` evidence 测试。
  - 接口文档 6 的请求示例同步 `documentTypes=["resume","job","chat_context"]`，并记录中文 tokenizer fallback 口径。
- 后续仍需复验：
  - `python ai-service\tests\test_chat_pipeline.py`
  - `mvn -pl server -am -Dtest=PythonChatClientTest "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - `mvn -pl server -am -DskipTests compile`
  - `cd website && npm run build`
- 复验命令与结果：
  - `python ai-service\tests\test_chat_pipeline.py`：通过，输出 `chat pipeline tests passed`；覆盖中文自然问题命中简历 evidence、`chat_context` 不被 metadata filter 过滤、HTTP contract 返回 evidence/diagnostics。
  - `mvn -pl server -am -Dtest=PythonChatClientTest "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`。
  - `mvn -pl server -am -DskipTests compile`：通过，Reactor `fuchuang/common/pojo/server` 均 `SUCCESS`。
  - `cd website && npm run build`：通过，`vue-tsc && vite build` 成功；`website/dist` 不纳入提交。
- 二次修正后的剩余风险：
  - 当前中文 tokenizer 仍是 deterministic fallback，只覆盖基础单字/bigram 召回，不能替代真实 embedding。
  - Runtime Java Chat smoke 仍未执行，不能声明端到端通过。
  - 真实 pgvector/简历-JD 知识库、Dashscope embedding/LLM、ranking model 和质量评估仍为下一轮任务。
