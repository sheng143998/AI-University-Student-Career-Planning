# 接口文档 8：职业地图与路线推荐

Roadmap 模块负责职业节点搜索、岗位图谱、岗位详情、垂直晋升路径和横向换岗推荐。前端访问 Java `/api/roadmap/**`；Java 在个性化推荐场景调用 Python Roadmap RAG。

## 搜索职业节点

| 项目 | 内容 |
| --- | --- |
| 方法 | `GET` |
| 路径 | `/api/roadmap/search` |
| 参数 | `q`、`limit` |
| 响应 | `Result<RoadmapSearchResultVO>` |

## 搜索岗位

| 项目 | 内容 |
| --- | --- |
| 方法 | `GET` |
| 路径 | `/api/roadmap/jobs/search` |
| 参数 | `q`、`limit` |
| 响应 | `Result<List<JobSearchResultVO>>` |

## 获取地图图谱

| 项目 | 内容 |
| --- | --- |
| 方法 | `GET` |
| 路径 | `/api/roadmap/graph` |
| 参数 | `categoryCode` 可选，`mode=vertical|lateral` |
| 响应 | `Result<RoadmapGraphVO>` |

## 获取节点详情

| 项目 | 内容 |
| --- | --- |
| 方法 | `GET` |
| 路径 | `/api/roadmap/nodes/{id}` |
| 响应 | `Result<RoadmapNodeDetailVO>` |

## 按岗位名称获取晋升路径

| 项目 | 内容 |
| --- | --- |
| 方法 | `GET` |
| 路径 | `/api/roadmap/map/path-by-name` |
| 参数 | `jobName`、`level` 可选 |
| 响应 | `Result<JobVerticalPathDetailVO>` |

## 按岗位名称获取换岗推荐

| 项目 | 内容 |
| --- | --- |
| 方法 | `POST` |
| 路径 | `/api/roadmap/recommend/transition/by-job` |
| 参数 | `jobName`、`level` 可选 |
| 响应 | `Result<UserTransitionRecommendationVO>` |

## 获取岗位详情

| 项目 | 内容 |
| --- | --- |
| 方法 | `GET` |
| 路径 | `/api/roadmap/map/job-detail/{id}` |
| 响应 | `Result<JobDetailVO>` |

## 获取个性化职业路径推荐

| 项目 | 内容 |
| --- | --- |
| 方法 | `GET` |
| 路径 | `/api/roadmap/recommendations/personalized` |
| 响应 | `Result<CareerPathRecommendationVO>` |

Java 调用 Python `/api/roadmap/recommendations/personalized` 获取横向或换岗推荐，不再用 Java 本地技能相似度逻辑补足推荐。

## 清理个性化推荐缓存

| 项目 | 内容 |
| --- | --- |
| 方法 | `DELETE` |
| 路径 | `/api/roadmap/recommendations/personalized/cache` |
| 响应 | `Result<String>` |

## 当前岗位

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/roadmap/user/current-job` | 保存用户手动设置的当前岗位 |
| `GET` | `/api/roadmap/user/current-job` | 获取用户当前岗位 |

## Python 内部接口

| 项目 | 内容 |
| --- | --- |
| 方法 | `POST` |
| 路径 | `/api/roadmap/recommendations/personalized` |
| 调用方 | Java `PythonRoadmapRagClient` |

## 前端调用

- `website/src/api/roadmap.ts`
- `website/src/views/Roadmap.vue`
