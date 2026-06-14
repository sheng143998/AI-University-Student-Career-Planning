# Chat debug endpoint env binding verification

- Time: 2026-06-09 10:07 +08:00
- Automation ID: ai-rag
- Worktree: `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-chat`
- Branch: `ai-rag-chat-python-boundary`
- Baseline: `c7c4774`
- Scope: Chat legacy `/ai/chat` debug endpoint OS-env relaxed binding gate

## Test Object

- Interface document: `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-chat\接口文档\接口文档_6_Chat.md`
- Java test: `server/src/test/java/com/itsheng/service/controller/ChatControllerDebugEndpointEnabledTest.java`
- Existing Java boundary files:
  - `server/src/main/java/com/itsheng/service/controller/ChatController.java`
  - `server/src/main/java/com/itsheng/service/config/PythonAiProperties.java`
  - `server/src/main/java/com/itsheng/service/client/PythonChatClient.java`
- Python fallback pipeline: `ai-service/tests/test_chat_pipeline.py`
- Obsidian record: `C:\Users\WhenJayHe\notes\study\项目使用记录\AI-University-Student-Career-Planning\接口文档_6_Chat_监督记录.md`

## Test Reason

The previous gate covered property-based enablement with `fuchuang.ai.python.debug-chat-endpoint-enabled=true`, but the interface document also promised `FUCHUANG_AI_PYTHON_DEBUG_CHAT_ENDPOINT_ENABLED=true`. This run verifies the OS environment variable style through Spring relaxed binding, without changing production code or production YAML.

## Environment

- OS: Windows / PowerShell
- Java/Maven: local Maven command
- Python: local `python`
- Frontend: `website` local npm build
- Runtime smoke prerequisites:
  - Redis `127.0.0.1:6379`: not verified as available
  - PostgreSQL/pgvector `127.0.0.1:5433`: not verified as available
  - Java `127.0.0.1:8081`: not running
  - Python Chat `127.0.0.1:8092`: not running
  - `OPENAI_API_KEY`: not verified as present

This run does not claim Java 8081 to Python 8092 runtime smoke, real pgvector retrieval, Dashscope embedding, Dashscope LLM generation, ranking model quality, or full repository regression.

## Changes Verified

- `ChatControllerDebugEndpointEnabledTest` now injects a `SystemEnvironmentPropertySource` containing `FUCHUANG_AI_PYTHON_DEBUG_CHAT_ENDPOINT_ENABLED=true`.
- The test asserts that `GET /ai/chat` is registered under OS-env style enablement and calls `PythonChatClient.complete(...)`.
- The existing default-off test still asserts `GET /ai/chat` returns 404 and does not call Python by default.
- The existing enabled test still asserts `HEAD`, `OPTIONS`, `POST`, `PUT`, `PATCH`, and `DELETE` return 405 and do not call Python.
- The formal `POST /api/chat/messages` endpoint remains covered by `ChatRestControllerMessagesTest`.
- `application.yml`, `application-dev.yml`, `website/**`, `database/**`, `ai_service/**`, and non-Chat modules were not changed.

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

First run after adding the env-binding test failed because the test response assertion expected the full async character stream `env answer`, while the synchronous MockMvc read saw the first emitted character. This was a test assertion issue, not a route registration or Python client interaction failure.

Fix: changed the env-binding stub content and assertion to single-character `e`, while keeping the strict `PythonChatClient.complete(...)` verification.

Final result: passed.

- Tests run: 7
- Failures: 0
- Errors: 0
- Skipped: 0
- Build: SUCCESS

### Backend compile

```powershell
mvn -pl server -am -DskipTests compile
```

Result: passed. Reactor modules `fuchuang`, `common`, `pojo`, and `service` completed successfully.

### Frontend compatibility build

```powershell
cd website
npm run build
```

Result: passed. No frontend source files were changed.

### Boundary checks

```powershell
rg -n "debug-chat-endpoint-enabled|FUCHUANG_AI_PYTHON_DEBUG_CHAT_ENDPOINT_ENABLED" server/src/main/resources
```

Result: no matches. The debug endpoint is not enabled through YAML or resource configuration.

```powershell
rg -n "org\.springframework\.ai\.chat\.client\.ChatClient" server/src/main/java/com/itsheng/service/controller/ChatController.java server/src/main/java/com/itsheng/service/service/Impl/ChatServiceImpl.java server/src/main/java/com/itsheng/service/client/PythonChatClient.java
```

Result: no matches. Chat boundary code does not directly import Spring AI `ChatClient`.

```powershell
git diff --check HEAD
```

Result: passed with CRLF normalization warnings only.

```powershell
git diff --name-status HEAD
```

Result before writing this log:

- `M server/src/test/java/com/itsheng/service/controller/ChatControllerDebugEndpointEnabledTest.java`
- `M 接口文档/接口文档_6_Chat.md`

Denylist check result: no matches for `application*.yml`, `website/**`, `database/**`, `ai_service/**`, Dashboard, Market, Goals, Roadmap, Resume, Reports, cache, pyc, target, node_modules, or dist.

## Failure And Fix Record

- `ChatControllerDebugEndpointEnabledTest.debugEndpointCanBeEnabledByOsEnvironmentVariableName` initially failed on response body assertion because the controller returns `Flux.fromArray(content.split(""))`. The route registered and Python client verification path was valid; the assertion was adjusted to single-character content to avoid conflating stream chunking with env binding.
- No production code or production configuration was changed.

## Sub-Agent Validation

- Plan gate: PASS after two gpt-5.5 + xhigh reviews.
- Goal gate: first pass had one strict FAIL due to missing OS-env relaxed binding verification.
- Revised Goal gate: PASS after two gpt-5.5 + xhigh reviews. One earlier retry pair failed due to local sub-agent service 503 and was not used as approval evidence.
- Final code review gate: PASS. The gpt-5.5 + xhigh review confirmed that the new test uses `SystemEnvironmentPropertySource` to inject `FUCHUANG_AI_PYTHON_DEBUG_CHAT_ENDPOINT_ENABLED=true`, validates actual `/ai/chat` registration, and verifies `PythonChatClient.complete(...)`; no P0/P1/P2/P3 findings.
- Final integration review gate: PASS. The gpt-5.5 + xhigh review confirmed default 404/no Python call, property and OS-env style enablement, GET-only Python path, non-GET 405/no Python call, formal `/api/chat/messages` compatibility, no resource default enablement, no Spring AI `ChatClient` direct import, and no scope boundary violation.
- Final test coverage review gate: PASS. The gpt-5.5 + xhigh review confirmed the coverage is sufficient for this narrow Chat debug endpoint gate: Python Chat pipeline, Java client/controller tests, OS-env relaxed binding, default-off 404/no Python, property enablement, non-GET 405/no Python, formal `/api/chat/messages` compatibility, compile/build, and static boundary checks.
- Final test-log credibility gate: PASS with pre-commit append requirements. The gpt-5.5 + xhigh review confirmed the log is credible and does not overclaim runtime smoke, pgvector, embedding/LLM, full `mvn test`, or push status. Required before commit: append staged scope and final commit hash.

## Staged Scope

Pre-commit staged scope:

- `M server/src/test/java/com/itsheng/service/controller/ChatControllerDebugEndpointEnabledTest.java`
- `A tests-log/ai-rag-automation/2026-06-09-1007-chat-debug-env-binding.md`
- `M 接口文档/接口文档_6_Chat.md`

Pre-commit cached denylist: no matches for `application*.yml`, `website/**`, `database/**`, `ai_service/**`, Dashboard, Market, Goals, Roadmap, Resume, Reports, cache, pyc, target, node_modules, or dist.

## Final Commit

Final local commit message: `test: verify chat debug env binding`.

Final commit hash is recorded in automation memory and final response after amend, because amending this log changes the commit hash.

Remote branch: not pushed at log update time. `git ls-remote --heads origin ai-rag-chat-python-boundary` returned no remote branch before commit.

## Remaining Risks

- `/ai/chat` remains unauthenticated if explicitly enabled. This is acceptable only as a development compatibility path because the default route is not registered.
- Full `mvn test` was not run. This run used Chat-specific Java tests plus `server` compile; non-Chat full repository regression is not claimed.
- Runtime smoke was not executed because Redis, PostgreSQL/pgvector, Java 8081, Python Chat 8092, and `OPENAI_API_KEY` were not all available as a verified runtime set.
- Real pgvector, Dashscope embedding/LLM, ranking model, and RAG quality evaluation remain future productionization work.

## Related Artifacts

- Current local branch: `ai-rag-chat-python-boundary`
- Prior local commits on branch: `b0de0e3 feat: route chat rag through python service`, `c7c4774 fix: gate legacy chat debug endpoint`
- Current run starts from `c7c4774`; final commit hash will be appended after validation and commit.
- Interface document: `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-chat\接口文档\接口文档_6_Chat.md`
- Obsidian record: `C:\Users\WhenJayHe\notes\study\项目使用记录\AI-University-Student-Career-Planning\接口文档_6_Chat_监督记录.md`
