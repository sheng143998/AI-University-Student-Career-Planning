# 2026-06-14 09:00 Roadmap-RAG 集成迁移验证日志

## 测试对象

- 集成 worktree：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-integration`
- 分支：`ai-rag-integration-20260613`
- Roadmap 对外接口：`GET /api/roadmap/recommendations/personalized`
- Java 到 Python 内部接口：`POST /api/roadmap/recommendations/personalized`
- Python 聚合入口：`ai-service/app/main.py`
- Python Roadmap-RAG：`ai-service/career_ai/roadmap_rag_service.py`
- Java 胶水：`PythonRoadmapRagClient`、`RoadmapServiceImpl`、`RoadmapController`
- 前端契约：`website/src/api/roadmapRecommendation.ts`
- 接口文档：`接口文档/接口文档_8_Roadmap.md`

## 测试原因

Roadmap 旧闭环分支 `ai-rag-roadmap-python-rag` 仍停留在旧 `ai_service/` 目录。本轮将 Roadmap-RAG 迁入统一 `ai-service/` 聚合服务，降低本地分支未合并导致功能丢失的风险。此提交不是完整 Goal 完成；完整 Goal 仍需要后续分支覆盖/归档、push/PR/merge 决策、全局 worktree clean 和远端 refs 台账。

## 测试环境

- OS/Shell：Windows PowerShell
- 主工作区：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning`
- 主工作区状态：`master...origin/master` clean
- 集成 worktree 初始状态：`ai-rag-integration-20260613...origin/master [ahead 12]`，Roadmap staged/unstaged 分裂待收敛
- `AGENTS.md`：主工作区与集成 worktree 均未发现；本轮按 `$ai-career-rag-python-optimizer`、接口文档优先和自动化指令执行
- Runtime smoke：未启动真实 Java 8081 + Python 8090 + PostgreSQL/pgvector + Redis + JWT 链路；本轮以 Python/Java 窄测、编译、前端构建和静态门禁替代，不声明真实端到端上线通过

## Roadmap 源分支覆盖矩阵

| 源提交 | 旧分支内容 | 本轮处置 |
| --- | --- | --- |
| `aa79af9 feat: route roadmap recommendations through python rag` | 新增旧 `ai_service/roadmap_rag_service.py`、`ai_service/test_roadmap_rag_service.py`、Java client/service/config/VO、前端 API、接口文档和测试日志 | 功能迁入 `ai-service/career_ai/roadmap_rag_service.py`、`ai-service/app/main.py`、`ai-service/tests/test_roadmap_rag_service.py`；旧 `ai_service/**` 不纳入本轮提交 |
| `4678a1d test: harden roadmap rag e2e smoke` | 增补 Java 8081 e2e smoke、`JwtTokenInterceptor.java` 日志脱敏、`Roadmap.vue` 当前岗位刷新逻辑和 e2e 测试日志 | 本轮不纳入 `JwtTokenInterceptor.java` 与 `website/src/views/Roadmap.vue`；缓存驱逐和失败语义用 Java 单测/MockMvc 覆盖，真实 e2e 保留为旧分支证据，不声明本轮 runtime e2e 通过 |
| `9b10a7a test: harden roadmap current-job cache handling` | 增补 `RoadmapControllerTest.java`、current-job 失败语义、测试日志 post-commit gate | `RoadmapControllerTest.java` 以精确 `git add -f` 纳入；current-job Redis/cache 失败语义和 cache eviction 纳入 `RoadmapServiceImplTest` 与 MockMvc 测试；旧 Roadmap tests-log 不直接纳入，改用本集成日志记录 |

## 子 Agent 门禁结论

- Plan 初审：FAIL。指出缺 Roadmap 专用测试日志、真实 Obsidian 记录、secret 哨兵处理、暂存收敛和 ignored controller test 纳入证明。
- Plan 最终复审：PASS。确认待执行 Plan 覆盖需求、范围、文件排除、测试计划和完整 Goal 不提前关闭。
- Goal 初审：FAIL。指出需补 Roadmap 三提交覆盖矩阵、提交前后 status、ignored test force-add 证明、真实 Obsidian 路径、post-commit rev-list 和 CRLF 接受条件。
- Goal 最终复审：PASS。确认本轮 Roadmap 提交可作为完整 Goal 的风险降低步骤，但完整 Goal 仍 active。

## 测试方法与命令

### Python

```powershell
cd C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-integration\ai-service
python -B -m py_compile app/main.py career_ai/roadmap_rag_service.py tests/test_roadmap_rag_service.py
python -B -m pytest tests/test_roadmap_rag_service.py -q -p no:cacheprovider
python -B -m pytest tests -q -p no:cacheprovider
```

### Java

```powershell
cd C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-integration
mvn --% -pl server -am -Dtest=PythonRoadmapRagClientTest,RoadmapServiceImplTest,RoadmapControllerTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl server -am -DskipTests compile
```

### Frontend

```powershell
cd C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-integration\website
npm run build
```

### 静态门禁

```powershell
git diff --check
git diff --cached --check
git diff --name-only --diff-filter=U
git status --porcelain=v1 -- ai_service/
git diff --cached --name-only -- ai_service/
git check-ignore -v -- server/src/test/java/com/itsheng/service/controller/RoadmapControllerTest.java
git diff --cached --name-status
# 实际执行时使用 staged 新增行扫描，避免删除行和本日志中的扫描说明误命中。
# old import：检查 Roadmap 测试和入口文件中是否出现旧 Python 包导入或旧 handler 名。
# text encoding：检查 staged 文本文件中是否出现 UTF-8 损坏特征。
# credential：检查 staged 新增行中是否出现云访问密钥、长 bearer、明文令牌键、私钥等真实凭据特征。
```

## 测试数据与请求样例

Python 单测构造 payload：

```json
{
  "userId": 7,
  "currentJob": "Frontend Engineer",
  "userSkills": ["Vue", "TypeScript", "Python"],
  "resumeData": {
    "target_role": "AI Application Engineer",
    "current_role": "Frontend Engineer",
    "experience_years": 1,
    "skills": ["Vue", "TypeScript", "Python"],
    "match_score": 78
  },
  "jobs": [
    {"id": 1, "categoryCode": "FRONTEND_JUNIOR", "baseCategoryCode": "FRONTEND"},
    {"id": 2, "categoryCode": "AI_APP_JUNIOR", "baseCategoryCode": "AI_APP"},
    {"id": 3, "categoryCode": "DATA_JUNIOR", "baseCategoryCode": "DATA"}
  ],
  "retrieval": {
    "topK": 10,
    "filters": {
      "excludeSameCategory": true,
      "documentTypes": ["job", "resume_summary", "jd_summary"]
    }
  }
}
```

## 实际结果

### Python

- `python -B -m py_compile app/main.py career_ai/roadmap_rag_service.py tests/test_roadmap_rag_service.py`：exit 0。
- `python -B -m pytest tests/test_roadmap_rag_service.py -q -p no:cacheprovider`：初次 `11 passed`；补齐可选 `jd_summary` summary index 后复跑 `12 passed in 0.19s`。
- `python -B -m pytest tests -q -p no:cacheprovider`：初次 `66 passed`；补齐可选 `jd_summary` summary index 后复跑 `67 passed in 7.72s`。

### Java

- `mvn --% -pl server -am -Dtest=PythonRoadmapRagClientTest,RoadmapServiceImplTest,RoadmapControllerTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test`：初次 BUILD SUCCESS，`Tests run: 17, Failures: 0, Errors: 0, Skipped: 0`；修复相似度日志脱敏后复跑 BUILD SUCCESS，`Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`；修复 fallback 同类目排除与 possessedSkills 脱敏后复跑 BUILD SUCCESS，`Tests run: 19, Failures: 0, Errors: 0, Skipped: 0`。
- Surefire 摘要：
  - `PythonRoadmapRagClientTest`：5 tests，0 failures/errors/skipped。
  - `RoadmapControllerTest`：2 tests，0 failures/errors/skipped。
  - `RoadmapServiceImplTest`：12 tests，0 failures/errors/skipped。
- `mvn -pl server -am -DskipTests compile`：BUILD SUCCESS。
- `mvn -pl server -am test`：BUILD SUCCESS，`Tests run: 88, Failures: 0, Errors: 0, Skipped: 0`。该命令覆盖当前集成分支中已纳入的 Java 客户端、Controller、Service 和 mapper 测试，比 Roadmap 窄测更接近“合并后无明显单测回归”的门槛。

### Frontend

- `npm run build`：成功，`125 modules transformed`，Vite build 完成。

### 静态门禁

- `git diff --check`：exit 0；输出 CRLF warning，但无 whitespace error。
- `git diff --cached --check`：exit 0；无 whitespace error。
- `git diff --name-only --diff-filter=U`：无输出。
- `MERGE_HEAD`、`CHERRY_PICK_HEAD`、`REVERT_HEAD`、`REBASE_HEAD`、`sequencer`、`rebase-apply`、`rebase-merge`：均不存在。
- `git status --porcelain=v1 -- ai_service/` 与 `git diff --cached --name-only -- ai_service/`：均无输出。
- line-ending：`git ls-files --eol` 显示部分 Roadmap 文本文件为 `w/mixed` 或即将 LF->CRLF；本轮接受条件是 `git diff --check` / `git diff --cached --check` 无 whitespace error，并在本日志记录该风险，不把 CRLF warning 单独作为失败。
- old import scan：最终门禁按本轮 Roadmap 文件扫描 `ai-service/tests/test_roadmap_rag_service.py`、`ai-service/app/main.py`、Roadmap 文档和 Java Roadmap 文件；不以 Goals 测试中的历史别名作为 Roadmap 阻断。
- secret scan：最终门禁以收敛后的 staged 新增行为准。已将测试中静态 secret-like 哨兵改为运行时拼接，避免真实 secret scan 误报，同时保留脱敏覆盖。
- mojibake scan：`RoadmapControllerTest.java` 以 UTF-8 读取正常；最终门禁按 staged 文本文件复查。

### 暂存与提交

- 提交前状态：Roadmap 相关 16 个文件已收敛到 index；仍待代码/集成子 Agent 复验、测试覆盖/日志可信度子 Agent 验收、最终静态门禁和提交后审计。
- `RoadmapControllerTest.java` 已通过精确暂存纳入本轮 index，`git diff --cached --name-only -- server/src/test/java/com/itsheng/service/controller/RoadmapControllerTest.java` 可证明该 controller hardening 测试在提交范围内。

## 已处理问题

- 将测试中静态 secret-like 哨兵改为运行时拼接，避免 strict secret scan 把测试样例误判为真实密钥，同时保留 Python/Java 对 credential key、token-like 和裸 key-like 值的脱敏覆盖。
- 修复 `RoadmapServiceImpl` 相似度计算日志中 raw `currentJob` 泄露风险，改为只记录 `currentJobPresent/currentJobLength`，并新增源码级回归测试防止重新打印原文。
- 修复 Java 本地 fallback 与 Roadmap-RAG 横向换岗契约不一致的问题：Python 失败、非法响应或空推荐触发 fallback 时排除当前岗位同 base category，并对 `possessedSkills` 使用同一套敏感信息脱敏。
- 补齐 Python Roadmap-RAG 可选 `jd_summary` summary index 支持，使默认 `documentTypes=["job","resume_summary","jd_summary"]` 与实现一致；Java 当前不传 JD 摘要时保持兼容。
- 补充 `接口文档_8_Roadmap.md` 的静态门禁口径：diff check、unmerged/sequencer、ignored test force-add、denylist、secret scan 和 mojibake scan。
- 修正 `docs/AI_RAG_配置与端口说明.md` 的超时优先级总述和 `base-url` 使用方，明确新 `FUCHUANG_AI_PYTHON_*` 环境变量优先于 legacy，Roadmap 专用顺序与代码一致，`PythonRoadmapRagClient` 使用聚合 `base-url`。

## 剩余风险

- 本轮不纳入旧 `ai_service/**`、`JwtTokenInterceptor.java`、`website/src/views/Roadmap.vue`，因此旧 Roadmap e2e 的 UI/JWT 侧补丁不会作为本集成提交的一部分。
- 未执行真实 Java 8081 + Python 8090 + PostgreSQL/pgvector + Redis + JWT runtime smoke。
- Roadmap-RAG 仍是 deterministic fallback，不声明真实 pgvector 语义召回、Dashscope embedding/LLM、cross-encoder 或离线 RAG 质量评估完成。
- 完整 Goal 仍 active：还需对 Chat、Dashboard、Goals、Reports、Resume、Roadmap、GitLab refs 做最终覆盖/归档/push/PR/merge 决策。

## 关联文件

- `ai-service/app/main.py`
- `ai-service/career_ai/roadmap_rag_service.py`
- `ai-service/tests/test_roadmap_rag_service.py`
- `pojo/src/main/java/com/itsheng/pojo/vo/CareerPathRecommendationVO.java`
- `server/src/main/java/com/itsheng/service/client/PythonRoadmapRagClient.java`
- `server/src/main/java/com/itsheng/service/config/PythonAiProperties.java`
- `server/src/main/java/com/itsheng/service/controller/RoadmapController.java`
- `server/src/main/java/com/itsheng/service/service/Impl/RoadmapServiceImpl.java`
- `server/src/test/java/com/itsheng/service/client/PythonRoadmapRagClientTest.java`
- `server/src/test/java/com/itsheng/service/controller/RoadmapControllerTest.java`
- `server/src/test/java/com/itsheng/service/service/Impl/RoadmapServiceImplTest.java`
- `website/src/api/roadmapRecommendation.ts`
- `接口文档/接口文档_8_Roadmap.md`
- `docs/AI_RAG_配置与端口说明.md`
- `docs/AI_RAG_剩余修改与完善清单.md`

## 提交

- 提交前状态：未提交
- 最终提交：待回填
