# 接口文档 1：认证、用户与通用上传

## 统一约定

- Java 后端统一返回 `Result<T>`：`code` 表示结果码，`msg` 表示提示信息，`data` 为业务数据。
- 需要登录的接口通过请求头 `token` 携带 JWT。
- 浏览器只访问 Java 后端 `/api/**`，不得直连 Python AI 服务。

## 用户注册

| 项目 | 内容 |
| --- | --- |
| 方法 | `POST` |
| 路径 | `/api/user/register` |
| 鉴权 | 不需要 |
| 请求体 | `UserRegisterDTO`，包含 `username`、`password`、`name`、`sex`、`userImage` 等字段 |
| 响应 | `Result<UserVO>` |

## 用户登录

| 项目 | 内容 |
| --- | --- |
| 方法 | `POST` |
| 路径 | `/api/user/login` |
| 鉴权 | 不需要 |
| 请求体 | `UserLoginDTO`，包含 `username`、`password` |
| 响应 | `Result<UserLoginVO>`，`data.token` 为后续请求使用的 JWT |

## 获取当前用户信息

| 项目 | 内容 |
| --- | --- |
| 方法 | `GET` |
| 路径 | `/api/user/info` |
| 鉴权 | 需要 |
| 响应 | `Result<UserVO>` |

## 编辑当前用户信息

| 项目 | 内容 |
| --- | --- |
| 方法 | `PUT` |
| 路径 | `/api/user/edit` |
| 鉴权 | 需要 |
| 请求体 | `UserDTO` |
| 响应 | `Result<UserVO>` |

## 退出登录

| 项目 | 内容 |
| --- | --- |
| 方法 | `POST` |
| 路径 | `/api/user/logout` |
| 鉴权 | 需要 |
| 响应 | `Result<Void>` |

## 通用文件上传

| 项目 | 内容 |
| --- | --- |
| 方法 | `POST` |
| 路径 | `/api/common/upload` |
| 鉴权 | 需要 |
| 请求类型 | `multipart/form-data`，字段名 `file` |
| 响应 | `Result<String>`，`data` 为 OSS 文件访问地址 |

## 前端调用

- `website/src/api/auth.ts`
- `website/src/lib/authToken.ts`
- `website/src/stores/auth.ts`

## 错误说明

- 用户名重复、账号不存在、密码错误等业务异常由 Java 后端转换为 `Result.error(...)`。
- 文件上传失败会抛出业务异常并返回失败响应。
