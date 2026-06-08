# Chat debug endpoint gate verification

- Time: 2026-06-09 03:08 +08:00
- Automation ID: ai-rag
- Worktree: `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-chat`
- Branch: `ai-rag-chat-python-boundary`
- Scope: Chat legacy `/ai/chat` debug endpoint gate

## Test Object

- Interface document: `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-chat\接口文档\接口文档_6_Chat.md`
- Java config: `server/src/main/java/com/itsheng/service/config/PythonAiProperties.java`
- Java controller: `server/src/main/java/com/itsheng/service/controller/ChatController.java`
- Java tests:
  - `server/src/test/java/com/itsheng/service/controller/ChatControllerDebugEndpointDisabledTest.java`
  - `server/src/test/java/com/itsheng/service/controller/ChatControllerDebugEndpointEnabledTest.java`
  - `server/src/test/java/com/itsheng/service/controller/ChatRestControllerMessagesTest.java`
  - `server/src/test/java/com/itsheng/service/client/PythonChatClientTest.java`
- Python fallback pipeline: `ai-service/tests/test_chat_pipeline.py`

## Test Reason

The previous Chat Python boundary review left a P3 risk: legacy `/ai/chat` is outside `/api/**` JWT interception and could be exposed in production. This run gates the endpoint so it is not registered by default, while preserving the formal `/api/chat/messages` path and the Python Chat/RAG boundary.

## Environment

- OS: Windows / PowerShell
- Java/Maven: local Maven command
- Python: local `python`
- Frontend: `website` local npm build
- Runtime smoke prerequisites:
  - `127.0.0.1:6379=False`
  - `127.0.0.1:5433=False`
  - `127.0.0.1:8081=False`
  - `127.0.0.1:8092=False`
  - `OPENAI_API_KEY=False`

Because the runtime prerequisites were not all available, this run does not claim Java 8081 to Python 8092 end-to-end runtime smoke, real pgvector retrieval, Dashscope embedding, or LLM quality verification.

## Changes Verified

- `/ai/chat` is controlled by `fuchuang.ai.python.debug-chat-endpoint-enabled`.
- Default behavior is route not registered, so `GET /ai/chat` returns 404.
- Explicit enablement registers only `GET /ai/chat`.
- `HEAD`, `OPTIONS`, `POST`, `PUT`, `PATCH`, and `DELETE` to `/ai/chat` are not accepted and do not call Python.
- Enabled `/ai/chat` still calls `PythonChatClient.complete(...)`, so it does not bypass the Python Chat/RAG boundary.
- The formal `POST /api/chat/messages` endpoint still streams through `ChatService` when the debug endpoint is disabled.
- `application.yml` and `application-dev.yml` were not changed.
- `website/**`, `ai_service/**`, `database/**`, and non-Chat modules were not changed.

## Commands And Results

### Python Chat pipeline

```powershell
python ai-service\tests\test_chat_pipeline.py
```

Result: passed. Output included `chat pipeline tests passed`.

### Java Chat narrow tests

```powershell
mvn -pl server -am "-Dtest=PythonChatClientTest,ChatControllerDebugEndpointDisabledTest,ChatControllerDebugEndpointEnabledTest,ChatRestControllerMessagesTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Result: passed.

- Tests run: 6
- Failures: 0
- Errors: 0
- Skipped: 0
- Build: SUCCESS

Coverage:

- `PythonChatClientTest`: Python Chat client still calls `/api/v1/chat/complete` and `/api/v1/chat/daily-suggestions`.
- `ChatControllerDebugEndpointDisabledTest`: default config returns 404 for `GET /ai/chat` and does not call `PythonChatClient`.
- `ChatControllerDebugEndpointEnabledTest`: explicit enablement returns a text stream for `GET /ai/chat`, verifies `PythonChatClient.complete(0L, 42L, ...)`, and rejects `HEAD`/`OPTIONS`/`POST`/`PUT`/`PATCH`/`DELETE` with no Python client call.
- `ChatRestControllerMessagesTest`: default debug endpoint closure does not break formal `POST /api/chat/messages`; the endpoint still returns the async text stream from `ChatService`.

### Backend compile

```powershell
mvn -pl server -am -DskipTests compile
```

Result: passed. Reactor modules `fuchuang`, `common`, `pojo`, and `service` all completed successfully.

### Frontend compatibility build

```powershell
cd website
npm run build
```

Result: passed. No frontend source files were changed in this run.

### Boundary checks

```powershell
rg -n "org\.springframework\.ai\.chat\.client\.ChatClient" server/src/main/java/com/itsheng/service/controller/ChatController.java server/src/main/java/com/itsheng/service/service/Impl/ChatServiceImpl.java server/src/main/java/com/itsheng/service/client/PythonChatClient.java
```

Result: no matches. Chat code does not directly import Spring AI `ChatClient`.

```powershell
rg -n "debug-chat-endpoint-enabled" server/src/main/resources
```

Result: no matches. The endpoint is not enabled through `application.yml` or `application-dev.yml`.

```powershell
git diff --cached --name-only
git diff --cached --check
git diff --name-only HEAD
```

Result: staged scope contains only:

- `server/src/main/java/com/itsheng/service/config/PythonAiProperties.java`
- `server/src/main/java/com/itsheng/service/controller/ChatController.java`
- `server/src/test/java/com/itsheng/service/controller/ChatControllerDebugEndpointDisabledTest.java`
- `server/src/test/java/com/itsheng/service/controller/ChatControllerDebugEndpointEnabledTest.java`
- `server/src/test/java/com/itsheng/service/controller/ChatRestControllerMessagesTest.java`
- `tests-log/ai-rag-automation/2026-06-09-0308-chat-debug-endpoint-gate.md`
- `接口文档/接口文档_6_Chat.md`

`git diff --cached --check` passed. The three new/updated controller test files were forced into the index with `git add -f` because project `.gitignore` ignores `*Test.java` and `**/test/`.

## Failure And Fix Record

- Initial PowerShell run of the Maven narrow-test command failed because the comma-separated `-Dtest` value was not quoted. It was rerun with the whole `-Dtest=...` argument quoted and passed.
- Initial code/integration review failed because the new controller tests were ignored by `.gitignore` and were not visible in the staged diff. Fixed by force-staging:

```powershell
git add -f -- server/src/test/java/com/itsheng/service/controller/ChatControllerDebugEndpointDisabledTest.java server/src/test/java/com/itsheng/service/controller/ChatControllerDebugEndpointEnabledTest.java
```

- Test coverage review required explicit coverage for the formal `/api/chat/messages` path and the remaining non-GET methods `HEAD`/`OPTIONS`/`PATCH`. Fixed by adding `ChatRestControllerMessagesTest`, extending `ChatControllerDebugEndpointEnabledTest`, and rerunning the expanded Java narrow test command.
- The first extended non-GET check found `OPTIONS /ai/chat` could be handled automatically with 200. Fixed by adding an explicit non-GET reject mapping in `ChatController`, then verified `HEAD`/`OPTIONS`/`POST`/`PUT`/`PATCH`/`DELETE` all return 405 without Python client interaction.
- The first formal endpoint test read an async `Flux` response too early. Fixed by using `asyncDispatch(result)` before asserting `text/html;charset=utf-8` and the streamed body.

## Sub-Agent Validation

- Plan gate: two gpt-5.5 + xhigh review agents initially failed the plan because default 200 disabled text was not safe enough. Plan was revised to conditionally register the controller, default 404, GET-only, and then both agents passed.
- Goal gate: two gpt-5.5 + xhigh review agents initially failed due to missing hard exclusions and validation boundaries. Goal boundaries were revised to exclude real pgvector, Dashscope embedding/LLM, complete runtime smoke, push, and merge; both agents passed.
- Code review gate: initial code/integration review failed only because ignored test files were not staged. After `git add -f`, both review agents passed with no P0/P1/P2 blockers.
- Final code review gate: gpt-5.5 + xhigh review passed with no P0/P1/P2/P3 findings. It confirmed the staged scope contains only the seven Chat gate files and does not include `application.yml`, `application-dev.yml`, `website/**`, `ai_service/**`, `database/**`, or non-Chat modules.
- Final integration review gate: gpt-5.5 + xhigh review passed with no blocking items. It confirmed `/ai/chat` defaults to 404, explicit enablement is GET-only, non-GET methods return 405, Java still calls `PythonChatClient`, formal `/api/chat/messages` remains compatible, and the document/runtime-smoke caveats are consistent.
- Final test coverage gate: gpt-5.5 + xhigh review passed. It confirmed coverage for default 404/no Python call, enabled GET/Python call, `HEAD`/`OPTIONS`/`POST`/`PUT`/`PATCH`/`DELETE` 405/no Python call, formal `/api/chat/messages` async text stream, `PythonChatClient` contract, and Python fallback pipeline.
- Final test-log credibility gate: gpt-5.5 + xhigh review passed. It confirmed this log records the tested objects, reasons, environment, commands, results, failure/fix history, remaining risks, and scoped staged files; no log correction was required before commit.

## Remaining Risks

- `FUCHUANG_AI_PYTHON_DEBUG_CHAT_ENDPOINT_ENABLED=true` OS-level relaxed binding was not separately tested; property-based enablement is covered.
- `/ai/chat` remains unauthenticated if explicitly enabled. This is acceptable only as a development compatibility path because the default behavior is route not registered.
- Real Java runtime smoke, pgvector retrieval, Dashscope embedding/LLM generation, ranking model, and RAG quality evaluation are still future productionization work.

## Related Artifacts

- Code commit target: current local branch, not pushed.
- Interface document: `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-chat\接口文档\接口文档_6_Chat.md`
- Obsidian record: `C:\Users\WhenJayHe\notes\study\项目使用记录\AI-University-Student-Career-Planning\接口文档_6_Chat_监督记录.md`
