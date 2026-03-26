# 职引AI - Auth 模块接口文档

## 模块概述
用户认证相关接口，包括登录、注册、验证码发送和退出登录。

---

## 接口列表

| 接口名 | 方法 | 路径 | 说明 |
| :--- | :--- | :--- | :--- |
| 用户登录 | POST | `/api/user/login` | 登录获取 Token（路径保持不变） |
| 用户注册 | POST | `/api/user/register` | 注册（路径保持不变） |
| 发送验证码 | POST | `/api/auth/verify-code` | 注册/登录/找回密码验证码 |
| 退出登录 | POST | `/api/auth/logout` | 使 token 失效（可选） |

---

## 详细接口定义

### 用户登录
- **请求方法**: `POST`
- **请求路径**: `/api/user/login`
- **鉴权**: 不需要
- **请求体**:
```json
{
  "username": "user@example.com",
  "password": "plain_or_hashed_password"
}
```
- **响应示例**:
```json
{
  "code": 200,
  "data": {
    "token": "<jwt>",
    "expires_in": 3600,
    "user": { "id": "u_001", "name": "Alex" }
  }
}
```

---

### 用户注册
- **请求方法**: `POST`
- **请求路径**: `/api/user/register`
- **鉴权**: 不需要
- **请求体**:
```json
{
  "username": "user@example.com",
  "password": "password",
  "verify_code": "123456"
}
```
- **响应示例**:
```json
{ "code": 200, "data": { "registered": true } }
```

---

### 发送验证码
- **请求方法**: `POST`
- **请求路径**: `/api/auth/verify-code`
- **鉴权**: 不需要
- **请求体**:
```json
{
  "channel": "email",
  "target": "user@example.com",
  "scene": "REGISTER"
}
```
- **响应示例**:
```json
{ "code": 200, "data": { "sent": true, "cooldown": 60 } }
```

---

### 退出登录
- **请求方法**: `POST`
- **请求路径**: `/api/auth/logout`
- **鉴权**: 需要
- **响应示例**:
```json
{ "code": 200, "data": { "ok": true } }
```