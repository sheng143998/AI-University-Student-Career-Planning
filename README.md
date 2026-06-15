# AI-University-Student-Career-Planning

本项目是面向大学生职业规划的前后端系统。前端使用 Vue 3，业务后端使用 Java/Spring Boot，AI/RAG 能力统一放在 Python 服务中。浏览器只访问 Java 后端，Java 负责认证、业务编排、文件处理、数据库读写和统一响应；Python 负责简历分析、OCR、岗位检索、RAG 生成、AI 建议和诊断信息。

## 当前架构

| 层级 | 技术 | 说明 |
| --- | --- | --- |
| 前端 | Vue 3、Vite、TypeScript、Pinia、Vue Router、Axios | 位于 `website/`，通过 `/api/**` 调用 Java 后端 |
| Java 后端 | Spring Boot 4、MyBatis、Redis、PostgreSQL、OSS | 位于 `server/`、`common/`、`pojo/`，对前端提供唯一 API 入口 |
| Python AI 服务 | FastAPI、确定性 RAG 降级实现、OCR/分析服务 | 位于 `ai-service/`，只供 Java 在本机 HTTP 调用 |
| 数据库 | PostgreSQL + pgvector schema | 初始化脚本位于 `database/` |

## 主要功能

- 用户注册、登录、资料编辑和 JWT 鉴权。
- 简历上传、文本抽取、图片型 PDF OCR、简历结构化分析、能力画像和预览。
- Dashboard 汇总、目标岗位匹配、职业发展路径自动创建和当前阶段更新。
- 市场探索、岗位画像、热门岗位、市场洞察和岗位详情。
- 职业地图搜索、岗位图谱、岗位详情、晋升路径和换岗推荐。
- 目标管理、里程碑管理和目标 AI 建议。
- AI 导师聊天、会话管理、流式回复、每日建议、附件上传和语音入口。
- 职业报告生成、详情、编辑、下载和删除。
- AI/RAG 反馈与个性化设置闭环。

## 服务端口

| 服务 | 默认地址 | 说明 |
| --- | --- | --- |
| Java 后端 | `http://127.0.0.1:8081` | 前端唯一业务入口 |
| Python 聚合 AI/RAG 服务 | `http://127.0.0.1:8090` | 报告、目标、Dashboard、Roadmap、Market/JD、RAG 反馈 |
| Python Resume 服务 | `http://127.0.0.1:8091` | 简历分析与 OCR 独立服务 |
| Python Chat 服务 | `http://127.0.0.1:8092` | 聊天回复与每日建议 |
| Vue 前端 | `http://127.0.0.1:5173` | 本地开发服务 |

## 本地启动

安装 Python 依赖：

```powershell
python -m pip install -r ai-service/requirements.txt
```

启动 Python 聚合 AI/RAG 服务：

```powershell
$env:PYTHONPATH='ai-service'
$env:AI_SERVICE_PORT='8090'
python -m uvicorn app.main:app --host 127.0.0.1 --port 8090
```

启动 Python Resume 服务：

```powershell
$env:PYTHONPATH='ai-service'
python -m career_ai.resume_analysis_service --host 127.0.0.1 --port 8091
```

启动 Python Chat 服务：

```powershell
$env:PYTHONPATH='ai-service'
$env:AI_SERVICE_PORT='8092'
python -m uvicorn app.main:app --host 127.0.0.1 --port 8092
```

启动 Java 后端：

```powershell
mvn -pl server -am spring-boot:run
```

启动前端：

```powershell
cd website
npm install
npm run dev
```

## 验证命令

```powershell
$env:PYTHONPATH='ai-service'
python -B -m pytest ai-service/tests -q -p no:cacheprovider
```

```powershell
mvn -pl server -am test
```

```powershell
cd website
npm run build
```

## 文档索引

- Python AI 服务说明：[ai-service/README.md](ai-service/README.md)
- 部署说明：[deploy/README.md](deploy/README.md)
- AI/RAG 配置与端口：[docs/AI_RAG_配置与端口说明.md](docs/AI_RAG_配置与端口说明.md)
- 业务流程图：[docs/business-flow.mmd](docs/business-flow.mmd)
- 接口文档：[接口文档/](接口文档)

当前仓库只保留与现有代码一致的说明文档和接口文档。历史待办清单、旧迁移备忘和重复部署说明已清理。
