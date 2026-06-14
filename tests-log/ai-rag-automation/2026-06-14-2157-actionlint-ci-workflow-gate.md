# 2026-06-14 21:57 actionlint CI workflow gate

## 测试对象

- Worktree: `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning-ai-rag-integration`
- Workflow: `.github/workflows/ai-rag-ci.yml`
- 工具链: `actionlint`, `winget`, `jq`, `yq`, `uv`, `go`

## 测试原因

GitHub PR #1 当前 `statusCheckRollup=[]`，需要补齐 GitHub Actions CI workflow，并在提交前使用 `actionlint` 做本地语法门禁。用户同时要求安装完成后配置环境变量，便于后续快速调用。

## 测试环境

- Windows / PowerShell
- `winget`: `v1.28.240`
- `actionlint`: `1.7.12`
- `jq`: `1.8.1`
- `yq`: `4.53.3`
- `uv`: `0.11.21`
- `go`: `1.26.4`

## 安装与环境变量

- `actionlint` 通过 `winget install --id rhysd.actionlint -e --source winget --scope user --location C:\Users\WhenJayHe\bin` 安装。
- `actionlint` 来源: `https://github.com/rhysd/actionlint/releases/download/v1.7.12/actionlint_1.7.12_windows_amd64.zip`
- winget 校验安装器 SHA256 通过。
- 用户级 PATH 已包含：
  - `C:\Users\WhenJayHe\bin`
  - `C:\Users\WhenJayHe\AppData\Local\Microsoft\WindowsApps`
  - `C:\Users\WhenJayHe\AppData\Local\Microsoft\WinGet\Packages\jqlang.jq_Microsoft.Winget.Source_8wekyb3d8bbwe`
  - `C:\Users\WhenJayHe\AppData\Local\Microsoft\WinGet\Packages\MikeFarah.yq_Microsoft.Winget.Source_8wekyb3d8bbwe`
  - `C:\Users\WhenJayHe\AppData\Local\Microsoft\WinGet\Packages\astral-sh.uv_Microsoft.Winget.Source_8wekyb3d8bbwe`
- `go` 通过官方 zip `https://go.dev/dl/go1.26.4.windows-amd64.zip` 安装到 `C:\Users\WhenJayHe\sdk\go1.26.4`，并写入用户级 `GOROOT` / `GOPATH` / PATH。

## 测试方法与命令

```powershell
$env:PATH = [Environment]::GetEnvironmentVariable('Path','User') + ';' + [Environment]::GetEnvironmentVariable('Path','Machine')
Get-Command actionlint
actionlint -version
actionlint '.github\workflows\ai-rag-ci.yml'
jq --version
yq --version
uv --version
go version
yq '.name' '.github\workflows\ai-rag-ci.yml'
```

## 实际结果

- `Get-Command actionlint` 命中 `C:\Users\WhenJayHe\bin\actionlint.exe`。
- `actionlint -version` 返回 `1.7.12`。
- `actionlint '.github\workflows\ai-rag-ci.yml'` 无输出，退出码 0，表示 0 error / 0 warning。
- workflow 新增 `actionlint` job，使用 `docker://rhysd/actionlint:1.7.12` 校验 `.github/workflows/ai-rag-ci.yml`。
- `jq --version` 返回 `jq-1.8.1`。
- `yq --version` 返回 `v4.53.3`。
- `uv --version` 返回 `uv 0.11.21`。
- `go version` 返回 `go1.26.4 windows/amd64`，`go env GOROOT GOPATH` 返回 `C:\Users\WhenJayHe\sdk\go1.26.4` 与 `C:\Users\WhenJayHe\go`。
- `yq '.name' '.github\workflows\ai-rag-ci.yml'` 返回 `ai-rag-ci`。

## 失败与修复记录

- GitHub API 匿名请求触发 rate limit，因此改用 `winget show rhysd.actionlint` 确认官方 release URL、版本和安装器哈希。
- `winget` 不在当前 PATH 中，但 `C:\Users\WhenJayHe\AppData\Local\Microsoft\WindowsApps\winget.exe` 存在；已把 WindowsApps 写入用户 PATH。
- `GoLang.Go` 通过 `winget --scope user` 安装失败，原因是该包没有适用的用户级安装器。随后改用官方 zip 手动安装到用户目录并补齐 `GOROOT` / `GOPATH` / PATH。
- 一次 `jq --null-input` 示例因 PowerShell 引号传递导致表达式变成 `{tool:jq,ok:true}` 而失败；该失败属于验证命令写法问题，不影响 `jq --version` 和安装可用性结论。
- 首次日志误落到主工作区 `C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning\tests-log\...`，已移动到集成 worktree 并删除主工作区误放文件。
- `mvn -pl server -Dtest=ChatControllerDebugEndpointEnabledTest test` 在本机因 reactor 里 common 模块未匹配测试而失败；随后改为 `mvn -pl server -am -Dtest=ChatControllerDebugEndpointEnabledTest "-Dsurefire.failIfNoSpecifiedTests=false" test` 并通过。
- `ChatControllerDebugEndpointEnabledTest` 已改为 `asyncDispatch` 处理 `Flux<String>` 异步响应，避免 CI 只读到部分正文。

## 子 Agent 验收结论

- Plan 覆盖审查: PASS。
- 技术风险审查: PASS。
- 测试覆盖初审: FAIL。指出缺少 actionlint job、日志、PATH、新 shell 可发现和 Go 失败记录；这些已全部补齐。
- 测试日志可信度复审: PASS。最终确认 `Go` 已安装到用户目录，旧的“Go 未安装”状态没有残留。

## 剩余风险

- 该 workflow 已通过本地 `actionlint`，且 CI 内包含 actionlint job；GitHub Actions 云端实际运行结果仍需推送后由 PR checks 验证。
- 当前 worktree 仍只新增 `.github/workflows/ai-rag-ci.yml`、本测试日志和一个 ChatControllerDebugEndpointEnabledTest 修复；尚未提交。

## 优化建议

1. 提交并推送 `.github/workflows/ai-rag-ci.yml` 后，复核 PR #1 是否出现 Python/Maven/frontend 三个 checks。
2. 后续继续补 CLI 时，优先使用用户级 `winget` 或固定用户目录，避免系统级 PATH 扩散。
3. 如后续继续补 CLI，优先选择可回滚的用户目录 zip 安装，并记录 `GOROOT`、`GOPATH` 与 PATH。

## 关联文件

- `.github/workflows/ai-rag-ci.yml`
- `server/src/test/java/com/itsheng/service/controller/ChatControllerDebugEndpointEnabledTest.java`
- `tests-log/ai-rag-automation/2026-06-14-2157-actionlint-ci-workflow-gate.md`
- `C:\Users\WhenJayHe\notes\study\项目使用记录\AI-University-Student-Career-Planning\AI_RAG_CI_actionlint_2026-06-14.md`
