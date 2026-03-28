# 职引 AI - User Profile 模块接口文档

---

## 数据模型

本模块**不需要新建表**，所有数据从现有的 `users` 表和 `resume_analysis_result` 表中获取。

### 数据来源说明

| 字段 | 来源表 | 来源字段 |
|------|--------|----------|
| id, name (userName), avatar (user_image) | `users` | `id`, `user_name`, `user_image` |
| location, current_role, target_role, match_score, education, experience, skills | `resume_analysis_result` | `parsed_data` (JSONB) 中的对应字段 |

#### parsed_data 字段结构示例

```json
{
  "name": "张三",
  "target_role": "前端工程师",
  "location": "深圳",
  "current_role": "初级前端开发",
  "match_score": 85,
  "skills": ["Vue", "React", "TypeScript"],
  "experience_years": 3,
  "education": [
    {
      "school": "XX 大学",
      "major": "计算机科学",
      "degree": "学士",
      "period": "2018-2022"
    }
  ],
  "experience": [
    {
      "company": "YY 科技",
      "position": "前端开发工程师",
      "period": "2022-2026",
      "description": "负责组件库维护..."
    }
  ]
}
```

---

## 2.1 获取个人档案概览

### 2.1.1 基本信息

请求路径：/api/user/profile

请求方式：GET

接口描述：该接口用于获取当前登录用户的个人档案概览信息，包括头像、定位、当前/目标岗位、匹配分等基础字段。数据从 `users` 表和 `resume_analysis_result` 表的 `parsed_data` 字段中提取。

---

### 2.1.2 请求参数

参数说明：

本接口无需请求参数。

---

### 2.1.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
| :--- | :--- | :--- | :--- |
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据 |
| &#124;- id | number | 非必须 | 用户 ID，对应 users.id |
| &#124;- name | string | 非必须 | 昵称/姓名，对应 users.user_name |
| &#124;- avatar | string | 非必须 | 头像 URL，对应 users.user_image |
| &#124;- location | string | 非必须 | 所在城市，从 parsed_data.location 提取 |
| &#124;- current_role | string | 非必须 | 当前岗位，从 parsed_data.current_role 提取 |
| &#124;- target_role | string | 非必须 | 目标岗位，从 parsed_data.target_role 提取 |
| &#124;- match_score | number | 非必须 | 岗位匹配分（0-100），从 parsed_data.match_score 提取 |

响应数据样例：
```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "id": 1001,
    "name": "张三",
    "avatar": "https://example.com/avatar.png",
    "location": "深圳",
    "current_role": "初级前端开发",
    "target_role": "前端工程师",
    "match_score": 85
  }
}
```

---

### 2.1.4 错误响应

| HTTP 状态码 | 场景 | JSON 响应样例 |
| :--- | :--- | :--- |
| `401` | 未登录或 token 失效 | `{"code": 401, "msg": "未登录"}` |

---

## 2.2 更新个人档案概览

### 2.2.1 基本信息

请求路径：/api/user/profile

请求方式：PUT

接口描述：该接口用于更新用户个人档案概览信息，包括昵称、头像 URL。
**注意**：location、current_role、target_role、match_score 等字段来自简历 AI 分析结果，不支持手动修改。

---

### 2.2.2 请求参数

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
| :--- | :--- | :--- | :--- |
| name | string | 非必须 | 昵称/姓名 |
| avatar | string | 非必须 | 头像 URL |

请求参数样例：
```json
{
  "name": "张三",
  "avatar": "https://example.com/new-avatar.png"
}
```

---

### 2.2.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
| :--- | :--- | :--- | :--- |
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据 |
| &#124;- updated | boolean | 非必须 | 是否更新成功 |

响应数据样例：
```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "updated": true
  }
}
```

---

### 2.2.4 错误响应

| HTTP 状态码 | 场景 | JSON 响应样例 |
| :--- | :--- | :--- |
| `401` | 未登录或 token 失效 | `{"code": 401, "msg": "未登录"}` |
| `400` | 请求参数格式错误 | `{"code": 400, "msg": "参数格式错误"}` |

---

## 2.3 获取详细档案

### 2.3.1 基本信息

请求路径：/api/user/profile/detail

请求方式：GET

接口描述：该接口用于获取当前登录用户的详细档案信息，包括教育经历、工作经历、技能列表、项目经历等。数据从 `resume_analysis_result` 表的 `parsed_data` 字段中提取。

---

### 2.3.2 请求参数

参数说明：

本接口无需请求参数。

---

### 2.3.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
| :--- | :--- | :--- | :--- |
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据 |
| &#124;- education | object[] | 非必须 | 教育经历数组 |
| &#124;- &#124;- school | string | 非必须 | 学校名称 |
| &#124;- &#124;- major | string | 非必须 | 专业 |
| &#124;- &#124;- degree | string | 非必须 | 学历（学士/硕士/博士） |
| &#124;- &#124;- period | string | 非必须 | 时间段（如"2018-2022"） |
| &#124;- experience | object[] | 非必须 | 工作经历数组 |
| &#124;- &#124;- company | string | 非必须 | 公司名称 |
| &#124;- &#124;- position | string | 非必须 | 职位 |
| &#124;- &#124;- period | string | 非必须 | 时间段 |
| &#124;- &#124;- description | string | 非必须 | 工作描述 |
| &#124;- skills | string[] | 非必须 | 技能列表 |
| &#124;- projects | object[] | 非必须 | 项目经历数组 |
| &#124;- &#124;- name | string | 非必须 | 项目名称 |
| &#124;- &#124;- link | string | 非必须 | 项目链接 |
| &#124;- &#124;- tech_stack | string[] | 非必须 | 技术栈 |

响应数据样例：
```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "education": [
      {
        "school": "XX 大学",
        "major": "计算机科学",
        "degree": "学士",
        "period": "2018-2022"
      }
    ],
    "experience": [
      {
        "company": "YY 科技",
        "position": "前端开发工程师",
        "period": "2022-2026",
        "description": "负责组件库维护..."
      }
    ],
    "skills": ["Vue", "React", "TypeScript"],
    "projects": [
      {
        "name": "个人作品集",
        "link": "https://github.com/zhangsan/portfolio",
        "tech_stack": ["Vue3", "Vite"]
      }
    ]
  }
}
```

---

### 2.3.4 错误响应

| HTTP 状态码 | 场景 | JSON 响应样例 |
| :--- | :--- | :--- |
| `401` | 未登录或 token 失效 | `{"code": 401, "msg": "未登录"}` |

---

## 数据流转说明

```
用户上传简历
    ↓
POST /api/resume/upload
    ↓
后端存储至 OSS，异步解析并创建 resume_analysis_result 记录
    ↓
GET /api/user/profile 或 /api/user/profile/detail
    ↓
从 users 表和 resume_analysis_result.parsed_data 提取数据返回
```

---

## 接口列表汇总

| 接口编号 | 接口名 | 方法 | 路径 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| 2.1 | 获取个人档案概览 | GET | /api/user/profile | 头像、定位、当前/目标岗位、匹配分 |
| 2.2 | 更新个人档案概览 | PUT | /api/user/profile | 更新昵称、头像 |
| 2.3 | 获取详细档案 | GET | /api/user/profile/detail | 教育/经历/项目/技能等 |
