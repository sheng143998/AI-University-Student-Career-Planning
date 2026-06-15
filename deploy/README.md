# 部署说明

本目录保存服务器部署脚本和生产配置模板。当前项目运行时由四类服务组成：Nginx 静态前端、Java 后端、Python AI 服务和基础设施（PostgreSQL/pgvector、Redis、OSS）。

## 当前推荐部署口径

| 组件 | 推荐部署方式 | 说明 |
| --- | --- | --- |
| 前端 | `website` 构建后放入 `/opt/app/frontend/dist` | Nginx 监听 80/443 并转发 `/api/**` |
| Java 后端 | 打包为 `/opt/app/backend/app.jar` | 默认监听 `127.0.0.1:8081` |
| Python 聚合 AI/RAG | systemd 或手动 uvicorn | 默认监听 `127.0.0.1:8090` |
| Python Resume 服务 | systemd 或手动 uvicorn | 默认监听 `127.0.0.1:8091` |
| Python Chat 服务 | systemd 或手动 uvicorn | 默认监听 `127.0.0.1:8092` |
| 数据库 | PostgreSQL + pgvector | schema 为 `ai_career_plan` |
| 缓存 | Redis | Java 后端使用 |

`deploy-server-v2.sh` 是当前较接近项目现状的服务器初始化脚本；使用前仍需检查服务器发行版、systemd 服务名、环境变量文件和生产密钥。`deploy-server.sh` 保留为历史参考，不建议直接用于当前 PostgreSQL + Python AI 架构。

## 服务器目录

```text
/opt/app/
  frontend/dist/      # 前端构建产物
  backend/app.jar     # Java 后端 jar
  ai-service/         # Python AI 服务代码，可按需部署
  config/             # application-prod.yml、env 等配置
  logs/               # 应用日志
  backup/             # jar 和前端构建备份
  scripts/            # 运维脚本
```

## 构建命令

后端：

```bash
mvn -pl server -am clean package -DskipTests
```

前端：

```bash
cd website
npm install
npm run build
```

Python 依赖：

```bash
python -m pip install -r ai-service/requirements.txt
```

## 生产环境变量

Java 后端至少需要：

```bash
FUCHUANG_DB_HOST=your-postgresql-host
FUCHUANG_DB_PORT=5432
FUCHUANG_DB_NAME=ai_career_plan
FUCHUANG_DB_SCHEMA=ai_career_plan
FUCHUANG_DB_USERNAME=your-db-user
FUCHUANG_DB_PASSWORD=your-db-password
FUCHUANG_REDIS_HOST=127.0.0.1
FUCHUANG_REDIS_PORT=6379
FUCHUANG_REDIS_DATABASE=0
FUCHUANG_JWT_SECRET=your-jwt-secret
FUCHUANG_ALIOSS_ENDPOINT=your-oss-endpoint
FUCHUANG_ALIOSS_ACCESS_KEY_ID=your-access-key
FUCHUANG_ALIOSS_ACCESS_KEY_SECRET=your-access-secret
FUCHUANG_ALIOSS_BUCKET_NAME=your-bucket
FUCHUANG_AI_PYTHON_BASE_URL=http://127.0.0.1:8090
FUCHUANG_AI_PYTHON_RESUME_BASE_URL=http://127.0.0.1:8091
FUCHUANG_AI_PYTHON_CHAT_BASE_URL=http://127.0.0.1:8092
```

Python OCR 或真实模型调用按需配置：

```bash
OPENAI_API_KEY=your-dashscope-compatible-key
DASHSCOPE_API_KEY=your-dashscope-key
FUCHUANG_RESUME_OCR_MODEL=qwen-vl-ocr-2025-11-20
```

数据库连接以 `server/src/main/resources/application-*.yml` 和实际生产配置为准。不要在文档或提交中写入真实数据库密码、OSS 密钥、JWT 密钥或模型 API key。

## 本地部署脚本

从本机打包并上传：

```bash
bash deploy/deploy-local.sh
```

只更新前端或后端：

```bash
bash deploy/deploy-hot.sh fe
bash deploy/deploy-hot.sh be
bash deploy/deploy-hot.sh all
```

上述脚本中的 `SERVER_IP`、`SERVER_USER` 和远端目录需要按真实服务器修改后再执行。

## 健康检查

Java 后端：

```bash
curl http://127.0.0.1:8081/api/market/hot-jobs
```

Python 聚合服务：

```bash
curl http://127.0.0.1:8090/health
```

Python Resume 服务：

```bash
curl http://127.0.0.1:8091/health
```

前端：

```bash
curl http://127.0.0.1/
```

## Nginx 代理要求

前端只暴露静态资源和 Java API：

- `/` 指向 `website/dist`
- `/api/` 反向代理到 `http://127.0.0.1:8081`
- Python `8090/8091/8092` 只监听本机，不暴露到公网

## 验证清单

部署后至少执行：

```bash
curl http://127.0.0.1:8090/health
curl http://127.0.0.1:8091/health
curl http://127.0.0.1:8081/api/market/hot-jobs
curl http://127.0.0.1/
```

同时查看：

```bash
journalctl -u app -f
journalctl -u nginx -f
```
