# 职引 AI - Resume 模块接口文档

---

## 数据模型

### user_vector_store 表（向量存储表）

存储简历的向量化数据，用于 Spring AI PgVectorStore 集成。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | VARCHAR(255) | UUID 格式主键（如 37a77bb4-08e0-47c2-b504-5137b1e4ebc9） |
| user_id | BIGINT | 用户 ID（逻辑外键） |
| content | TEXT | 简历原始内容 |
| embedding | vector(1024) | 向量化表示（1024 维） |
| resume_file_path | VARCHAR(500) | OSS 文件路径 |
| vector_type | VARCHAR(50) | 向量类型（默认 'resume'） |
| metadata | JSONB | 元数据，包含 user_id、file_path、file_type、document_id、page_number 等 |
| create_time | TIMESTAMP | 创建时间 |
| update_time | TIMESTAMP | 更新时间 |

#### metadata 字段示例

```json
{
  "user_id": 1,
  "file_path": "https://itxiang-sky-out.oss-cn-chengdu.aliyuncs.com/8c0ec69d-1e43-4541-bbb4-eb7f0624c95c.pdf",
  "file_type": "pdf",
  "document_id": "d2e6be76-a7b9-4543-b08b-2ec69db34103",
  "page_number": 3
}
```

---

### resume_analysis_result 表（分析结果表）

存储详细的简历分析结果。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGSERIAL | 主键 |
| vector_store_id | VARCHAR(255) | 关联 user_vector_store.id |
| user_id | BIGINT | 用户 ID（逻辑外键） |
| file_type | VARCHAR(20) | 文件类型：pdf / docx / pptx / html / txt |
| original_file_name | VARCHAR(500) | 原始文件名 |
| parsed_data | JSONB | 解析后的结构化数据 |
| scores | JSONB | 各维度评分 |
| highlights | JSONB | 亮点列表（JSON 数组） |
| suggestions | JSONB | 优化建议 |
| create_time | TIMESTAMP | 创建时间 |
| update_time | TIMESTAMP | 更新时间 |

#### parsed_data 字段示例

```json
{
  "name": "张三",
  "target_role": "前端工程师",
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

#### scores 字段示例

```json
{
  "keyword_match": 82,
  "layout": 95,
  "skill_depth": 78,
  "experience": 88
}
```

#### suggestions 字段示例

```json
[
  {
    "type": "CONTENT",
    "content": "增加更多量化的项目成果"
  },
  {
    "type": "SKILL",
    "content": "建议补充 Node.js 后端经验"
  },
  {
    "type": "LAYOUT",
    "content": "建议调整简历排版，突出核心经历"
  }
]
```

---

## 5.1 上传简历

### 5.1.1 基本信息

请求路径：/api/resume/upload

请求方式：POST

接口描述：该接口用于上传简历文件，后端接收文件后存储至阿里云 OSS，并异步进行简历解析和向量化处理。支持的文件类型：PDF、DOCX、PPTX、HTML、TXT，文件大小不超过 20MB。

---

### 5.1.2 请求参数

参数格式：multipart/form-data

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
| :--- | :--- | :--- | :--- |
| file | file | 必须 | 简历文件，支持 PDF / DOCX / PPTX / HTML / TXT，最大 20MB |

请求参数样例：
```
POST /api/resume/upload
Content-Type: multipart/form-data

file: [二进制文件内容]
```

---

### 5.1.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
| :--- | :--- | :--- | :--- |
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据 |
| &#124;- id | string | 非必须 | 向量存储记录 ID（UUID），对应 user_vector_store.id |
| &#124;- user_id | number | 非必须 | 用户 ID，对应 user_vector_store.user_id |
| &#124;- resume_file_path | string | 非必须 | OSS 文件访问 URL，对应 user_vector_store.resume_file_path |
| &#124;- created_at | string | 非必须 | 创建时间 |

响应数据样例：
```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "id": "37a77bb4-08e0-47c2-b504-5137b1e4ebc9",
    "user_id": 1001,
    "resume_file_path": "https://itxiang-sky-out.oss-cn-chengdu.aliyuncs.com/8c0ec69d-1e43-4541-bbb4-eb7f0624c95c.pdf",
    "created_at": "2026-03-26T09:10:00+08:00"
  }
}
```

---

### 5.1.4 前端处理建议

**重要：上传成功后的用户提示**

前端在调用上传接口成功后，**必须**在界面上展示以下提示信息：

> **请稍等，简历分析过程大约会耗时一分钟**

同时，前端应：
1. 显示 loading 状态或进度提示
2. 使用返回的 `id` 字段，每隔 2s 轮询一次 `GET /api/resume/analysis/{id}` 接口
3. 最多轮询 60s（30 次），如果超时后仍未返回完整数据，提示用户"分析超时，请稍后重试"
4. 当轮询获取到包含 `parsed_data` 的完整响应时，停止轮询并展示分析结果

---

## 5.2 获取简历分析结果

### 5.2.1 基本信息

请求路径：/api/resume/analysis/{id}

请求方式：GET

接口描述：该接口用于根据向量存储记录 ID 查询简历分析结果。解析为异步过程，前端可每隔 2s 轮询一次，最多等待 60s；当查询到完整数据时展示结果。

---

### 5.2.2 请求参数

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
| :--- | :--- | :--- | :--- |
| id | string | 必须 | 向量存储记录 ID（UUID），对应 user_vector_store.id（Path 参数） |

请求参数样例：
```
/api/resume/analysis/37a77bb4-08e0-47c2-b504-5137b1e4ebc9
```

---

### 5.2.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
| :--- | :--- | :--- | :--- |
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据 |
| &#124;- vector_store_id | string | 非必须 | 向量存储记录 ID，对应 user_vector_store.id |
| &#124;- analysis_id | number | 非必须 | 分析结果记录 ID，对应 resume_analysis_result.id |
| &#124;- user_id | number | 非必须 | 用户 ID |
| &#124;- file_type | string | 非必须 | 文件类型：pdf / docx / pptx / html / txt |
| &#124;- original_file_name | string | 非必须 | 原始文件名 |
| &#124;- resume_content | string | 非必须 | 简历原始内容，对应 user_vector_store.content |
| &#124;- resume_file_path | string | 非必须 | OSS 文件访问 URL |
| &#124;- parsed_data | object | 非必须 | 解析后的结构化数据，对应 resume_analysis_result.parsed_data |
| &#124;- &#124;- name | string | 非必须 | 姓名 |
| &#124;- &#124;- target_role | string | 非必须 | 目标岗位 |
| &#124;- &#124;- skills | string[] | 非必须 | 技能列表 |
| &#124;- &#124;- experience_years | number | 非必须 | 工作年限 |
| &#124;- &#124;- education | object[] | 非必须 | 教育经历 |
| &#124;- &#124;- experience | object[] | 非必须 | 工作经历 |
| &#124;- scores | object | 非必须 | 各维度评分，对应 resume_analysis_result.scores |
| &#124;- &#124;- keyword_match | number | 非必须 | 关键词匹配分（0-100） |
| &#124;- &#124;- layout | number | 非必须 | 布局评分（0-100） |
| &#124;- &#124;- skill_depth | number | 非必须 | 技能深度评分（0-100） |
| &#124;- &#124;- experience | number | 非必须 | 经历评分（0-100） |
| &#124;- highlights | string[] | 非必须 | 简历亮点列表，对应 resume_analysis_result.highlights |
| &#124;- suggestions | object[] | 非必须 | 优化建议，对应 resume_analysis_result.suggestions |
| &#124;- &#124;- type | string | 非必须 | 建议类型：CONTENT / SKILL / LAYOUT |
| &#124;- &#124;- content | string | 非必须 | 建议内容 |
| &#124;- created_at | string | 非必须 | 创建时间 |
| &#124;- updated_at | string | 非必须 | 更新时间 |

响应数据样例（解析中，暂无分析结果）：
```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "vector_store_id": "37a77bb4-08e0-47c2-b504-5137b1e4ebc9",
    "user_id": 1001,
    "resume_file_path": "https://itxiang-sky-out.oss-cn-chengdu.aliyuncs.com/8c0ec69d-1e43-4541-bbb4-eb7f0624c95c.pdf",
    "created_at": "2026-03-26T09:10:00+08:00",
    "updated_at": "2026-03-26T09:10:00+08:00"
  }
}
```

响应数据样例（解析完成）：
```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "vector_store_id": "37a77bb4-08e0-47c2-b504-5137b1e4ebc9",
    "analysis_id": 1,
    "user_id": 1001,
    "file_type": "pdf",
    "original_file_name": "张三_前端工程师简历.pdf",
    "resume_content": "张三\n求职意向：前端工程师\n工作经历：...",
    "resume_file_path": "https://itxiang-sky-out.oss-cn-chengdu.aliyuncs.com/8c0ec69d-1e43-4541-bbb4-eb7f0624c95c.pdf",
    "parsed_data": {
      "name": "张三",
      "target_role": "前端工程师",
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
    },
    "scores": {
      "keyword_match": 82,
      "layout": 95,
      "skill_depth": 78,
      "experience": 88
    },
    "highlights": [
      "项目描述清晰",
      "技能栈匹配度高",
      "工作经历完整"
    ],
    "suggestions": [
      {
        "type": "CONTENT",
        "content": "增加更多量化的项目成果"
      },
      {
        "type": "SKILL",
        "content": "建议补充 Node.js 后端经验"
      },
      {
        "type": "LAYOUT",
        "content": "建议调整简历排版，突出核心经历"
      }
    ],
    "created_at": "2026-03-26T09:10:00+08:00",
    "updated_at": "2026-03-26T09:12:00+08:00"
  }
}
```

---

## 5.3 获取简历分析列表

### 5.3.1 基本信息

请求路径：/api/resume/analysis

请求方式：GET

接口描述：该接口用于查询当前用户的历史简历分析记录列表，支持游标分页。

---

### 5.3.2 请求参数

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
| :--- | :--- | :--- | :--- |
| cursor | string | 非必须 | 游标，用于分页，首次请求不传 |
| limit | number | 非必须 | 每页数量，默认 20，最大 100 |

请求参数样例：
```
/api/resume/analysis?limit=20
/api/resume/analysis?cursor=xxx&limit=20
```

---

### 5.3.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
| :--- | :--- | :--- | :--- |
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据 |
| &#124;- items | object[] | 非必须 | 分析记录列表 |
| &#124;- &#124;- vector_store_id | string | 非必须 | 向量存储记录 ID（UUID） |
| &#124;- &#124;- analysis_id | number | 非必须 | 分析结果记录 ID |
| &#124;- &#124;- file_type | string | 非必须 | 文件类型：pdf / docx / pptx / html / txt |
| &#124;- &#124;- original_file_name | string | 非必须 | 原始文件名 |
| &#124;- &#124;- resume_file_path | string | 非必须 | OSS 文件访问 URL |
| &#124;- &#124;- created_at | string | 非必须 | 创建时间 |
| &#124;- &#124;- updated_at | string | 非必须 | 更新时间 |
| &#124;- next_cursor | string | 非必须 | 下一页游标，为 null 时表示已是最后一页 |

响应数据样例：
```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "items": [
      {
        "vector_store_id": "37a77bb4-08e0-47c2-b504-5137b1e4ebc9",
        "analysis_id": 1,
        "file_type": "pdf",
        "original_file_name": "张三_前端工程师简历.pdf",
        "resume_file_path": "https://itxiang-sky-out.oss-cn-chengdu.aliyuncs.com/8c0ec69d-1e43-4541-bbb4-eb7f0624c95c.pdf",
        "created_at": "2026-03-26T09:10:00+08:00",
        "updated_at": "2026-03-26T09:12:00+08:00"
      },
      {
        "vector_store_id": "48b88cc5-19f1-58d3-c615-6248c2f5fd0a",
        "analysis_id": 2,
        "file_type": "docx",
        "original_file_name": "张三_简历_v2.docx",
        "resume_file_path": "https://itxiang-sky-out.oss-cn-chengdu.aliyuncs.com/9d1fd70e-2f54-5652-cccc-fc8g0735d96d.docx",
        "created_at": "2026-03-25T09:10:00+08:00",
        "updated_at": "2026-03-25T09:12:00+08:00"
      }
    ],
    "next_cursor": null
  }
}
```

---

## 数据流转说明

```
前端上传文件
    ↓
POST /api/resume/upload
    ↓
后端存储至 OSS，创建 user_vector_store 记录
    ↓
异步任务：解析简历、生成向量、创建 resume_analysis_result 记录
    ↓
前端轮询 GET /api/resume/analysis/{id}
    ↓
查询到 parsed_data 等完整数据时展示结果
```

---

## 表关系说明

```
user_vector_store (向量存储表)
    ↓ (1:1 关系)
resume_analysis_result (分析结果表)
    ↓ (通过 vector_store_id 关联)
```

- **user_vector_store**：存储简历的向量化数据，用于 Spring AI 向量搜索
- **resume_analysis_result**：存储详细的分析结果，包括解析数据、评分、建议等

两表通过 `vector_store_id` 关联，一条向量记录对应一条分析结果记录。
