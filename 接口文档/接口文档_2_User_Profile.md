# 职引AI - User & Profile 模块接口文档

## 模块概述
用户个人档案管理相关接口，包括基础信息、详细档案和作品集上传。

---

## 接口列表

| 接口名 | 方法 | 路径 | 说明 |
| :--- | :--- | :--- | :--- |
| 获取个人档案概览 | GET | `/api/user/profile` | 头像、定位、当前/目标岗位、匹配分 |
| 更新个人档案概览 | PUT | `/api/user/profile` | 更新基础字段 |
| 获取详细档案 | GET | `/api/user/profile/detail` | 教育/经历/项目/技能等 |
| 更新详细档案 | PUT | `/api/user/profile/detail` | 用于"档案进阶"维护 |
| 上传作品集/附件 | POST | `/api/user/portfolio/upload` | 上传图片/文件，返回 URL |

---

## 详细接口定义

### 获取个人档案概览
- **请求方法**: `GET`
- **请求路径**: `/api/user/profile`
- **鉴权**: 需要
- **响应示例**:
```json
{
  "code": 200,
  "data": {
    "id": "u_001",
    "name": "Alex",
    "avatar": "https://example.com/avatar.png",
    "location": "深圳",
    "current_role": "前端开发",
    "target_role": "高级前端工程师",
    "match_score": 85,
    "updated_at": "2026-03-26T09:00:00+08:00"
  }
}
```

---

### 更新个人档案概览
- **请求方法**: `PUT`
- **请求路径**: `/api/user/profile`
- **鉴权**: 需要
- **请求体**:
```json
{
  "name": "Alex",
  "location": "深圳",
  "current_role": "前端开发",
  "target_role": "高级前端工程师"
}
```
- **响应示例**:
```json
{ "code": 200, "data": { "updated": true } }
```

---

### 获取详细档案
- **请求方法**: `GET`
- **请求路径**: `/api/user/profile/detail`
- **鉴权**: 需要
- **响应示例**:
```json
{
  "code": 200,
  "data": {
    "education": [
      { "school": "XX大学", "major": "计算机科学", "degree": "学士", "period": "2018-2022" }
    ],
    "experience": [
      { "company": "YY科技", "position": "前端开发实习生", "period": "2021-2022", "description": "负责组件库维护..." }
    ],
    "skills": ["Vue3", "TypeScript", "TailwindCSS"],
    "projects": [
      { "name": "个人职业看板", "link": "github.com/alex/dashboard", "tech_stack": ["Vue3", "Vite"] }
    ]
  }
}
```

---

### 更新详细档案
- **请求方法**: `PUT`
- **请求路径**: `/api/user/profile/detail`
- **鉴权**: 需要
- **请求体**: 同"获取详细档案"的 `data` 结构
- **响应示例**:
```json
{ "code": 200, "data": { "updated": true } }
```

---

### 上传作品集/附件
- **请求方法**: `POST`
- **请求路径**: `/api/user/portfolio/upload`
- **鉴权**: 需要
- **Content-Type**: `multipart/form-data`
- **表单字段**:
  - `file` (File, 必填)
- **响应示例**:
```json
{ "code": 200, "data": { "url": "https://cdn.example.com/portfolio/p1.png" } }
```