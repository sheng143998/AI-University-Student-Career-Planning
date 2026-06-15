# 接口文档 2：用户档案

用户档案接口由 Java 后端提供，数据主要来自用户基础信息、简历 AI 分析结果和能力画像。

## 获取个人档案概览

| 项目 | 内容 |
| --- | --- |
| 方法 | `GET` |
| 路径 | `/api/user/profile` |
| 鉴权 | 需要 |
| 响应 | `Result<UserProfileVO>` |

主要字段：

| 字段 | 说明 |
| --- | --- |
| `id` | 用户 ID |
| `name` | 昵称或姓名 |
| `avatar` | 头像地址 |
| `location` | 所在地 |
| `currentRole` | 当前岗位 |
| `targetRole` | 目标岗位 |
| `matchScore` | 画像匹配分 |

## 更新个人档案概览

| 项目 | 内容 |
| --- | --- |
| 方法 | `PUT` |
| 路径 | `/api/user/profile` |
| 鉴权 | 需要 |
| 请求体 | `UserProfileUpdateDTO` |
| 响应 | `Result<Void>` |

当前主要支持更新 `name`、`avatar` 等用户可编辑字段。岗位、匹配分等字段来自简历分析和职业规划，不作为普通手动编辑项。

## 获取详细档案

| 项目 | 内容 |
| --- | --- |
| 方法 | `GET` |
| 路径 | `/api/user/profile/detail` |
| 鉴权 | 需要 |
| 响应 | `Result<UserProfileDetailVO>` |

详细档案包含教育经历、工作经历、技能列表和项目经历。

## 前端调用

- `website/src/api/userProfile.ts`
- `website/src/views/Settings.vue`
