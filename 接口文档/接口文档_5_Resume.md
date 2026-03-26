# 职引AI - Resume 模块接口文档

---

## 5.1 上传简历

### 5.1.1 基本信息

请求路径：/api/resume/upload

请求方式：POST

接口描述：该接口用于上传简历文件，后端接收文件后存储至阿里云 OSS，并异步进行简历解析和向量化处理。支持的文件类型：PDF、DOCX、PPTX、HTML、TXT，文件大小不超过 10MB。

---

### 5.1.2 请求参数

参数格式：multipart/form-data

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
| :--- | :--- | :--- | :--- |
| file | MultipartFile | 必须 | 简历文件，支持 PDF / DOCX / PPTX / HTML / TXT，最大 10MB |

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
| &#124;- id | string | 非必须 | 记录 ID，对应 user_vector_store 表主键 |
| &#124;- user_id | string | 非必须 | 用户 ID，对应 user_vector_store.user_id |
| &#124;- resume_file_path | string | 非必须 | 后端上传至 OSS 后的文件访问 URL，对应 user_vector_store.resume_file_path |
| &#124;- resume_analysis_id | string | 非必须 | 简历分析 ID，对应 user_vector_store.resume_analysis_id |
| &#124;- parsing_status | string | 非必须 | 解析状态：PROCESSING / COMPLETED / FAILED，取自 user_vector_store.metadata->>'parsing_status' |
| &#124;- created_at | string | 非必须 | 创建时间，对应 user_vector_store.create_time |

响应数据样例：
```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "id": "1",
    "user_id": "1001",
    "resume_file_path": "https://your-bucket.oss-cn-shenzhen.aliyuncs.com/resumes/2026/03/26/u_001_1711361200.pdf",
    "resume_analysis_id": "ra_20260326_001",
    "parsing_status": "PROCESSING",
    "created_at": "2026-03-26T09:10:00+08:00"
  }
}
```

---

## 5.2 获取简历分析结果

### 5.2.1 基本信息

请求路径：/api/resume/analysis/{id}

请求方式：GET

接口描述：该接口用于根据记录 ID 查询简历分析结果。解析为异步过程，前端可每隔 2s 轮询一次，最多等待 60s；当 status 为 COMPLETED 时停止轮询并展示结果，为 FAILED 时提示用户重新提交。

---

### 5.2.2 请求参数

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
| :--- | :--- | :--- | :--- |
| id | string | 必须 | 简历分析记录 ID，对应 user_vector_store.id（Path 参数） |

请求参数样例：
```
/api/resume/analysis/1
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
| &#124;- id | string | 非必须 | 记录 ID，对应 user_vector_store.id |
| &#124;- user_id | string | 非必须 | 用户 ID，对应 user_vector_store.user_id |
| &#124;- resume_content | string | 非必须 | 简历解析后的文本内容，对应 user_vector_store.resume_content |
| &#124;- resume_file_path | string | 非必须 | OSS 文件访问 URL，对应 user_vector_store.resume_file_path |
| &#124;- resume_analysis_id | string | 非必须 | 简历分析 ID，对应 user_vector_store.resume_analysis_id |
| &#124;- embedding_vector | number[] | 非必须 | 简历向量表示（1024 维），对应 user_vector_store.embedding_vector |
| &#124;- vector_type | string | 非必须 | 向量类型，默认 resume，对应 user_vector_store.vector_type |
| &#124;- metadata | object | 非必须 | 元数据，对应 user_vector_store.metadata |
| &#124;- &#124;- file_type | string | 非必须 | 文件类型：pdf / docx / pptx / html / txt |
| &#124;- &#124;- original_file_name | string | 非必须 | 原始文件名 |
| &#124;- &#124;- parsed_data | object | 非必须 | 解析后的结构化简历数据 |
| &#124;- &#124;- &#124;- name | string | 非必须 | 姓名 |
| &#124;- &#124;- &#124;- target_role | string | 非必须 | 目标岗位 |
| &#124;- &#124;- &#124;- skills | string[] | 非必须 | 技能列表 |
| &#124;- &#124;- &#124;- experience_years | number | 非必须 | 工作年限 |
| &#124;- &#124;- scores | object | 非必须 | 各维度评分（0-100） |
| &#124;- &#124;- &#124;- keyword_match | number | 非必须 | 关键词匹配分 |
| &#124;- &#124;- &#124;- layout | number | 非必须 | 布局评分 |
| &#124;- &#124;- &#124;- skill_depth | number | 非必须 | 技能深度评分 |
| &#124;- &#124;- &#124;- experience | number | 非必须 | 经历评分 |
| &#124;- &#124;- highlights | string[] | 非必须 | 简历亮点 |
| &#124;- &#124;- suggestions | object[] | 非必须 | 优化建议列表 |
| &#124;- &#124;- &#124;- type | string | 非必须 | 建议类型：CONTENT / SKILL / LAYOUT |
| &#124;- &#124;- &#124;- content | string | 非必须 | 建议内容 |
| &#124;- &#124;- parsing_status | string | 非必须 | 解析状态：PROCESSING / COMPLETED / FAILED |
| &#124;- status | string | 非必须 | 解析状态（同 metadata.parsing_status，便于前端直接读取） |
| &#124;- created_at | string | 非必须 | 创建时间，对应 user_vector_store.create_time |
| &#124;- updated_at | string | 非必须 | 更新时间，对应 user_vector_store.update_time |

响应数据样例（解析中）：
```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "id": "1",
    "user_id": "1001",
    "resume_file_path": "https://your-bucket.oss-cn-shenzhen.aliyuncs.com/resumes/2026/03/26/u_001_1711361200.pdf",
    "resume_analysis_id": "ra_20260326_001",
    "status": "PROCESSING",
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
    "id": "1",
    "user_id": "1001",
    "resume_content": "张三\n求职意向：前端工程师\n工作经历：...",
    "resume_file_path": "https://your-bucket.oss-cn-shenzhen.aliyuncs.com/resumes/2026/03/26/u_001_1711361200.pdf",
    "resume_analysis_id": "ra_20260326_001",
    "embedding_vector": [0.12, -0.45, 0.78],
    "vector_type": "resume",
    "metadata": {
      "file_type": "pdf",
      "original_file_name": "张三_前端工程师简历.pdf",
      "parsed_data": {
        "name": "张三",
        "target_role": "前端工程师",
        "skills": ["Vue", "React", "TypeScript"],
        "experience_years": 3
      },
      "scores": {
        "keyword_match": 82,
        "layout": 95,
        "skill_depth": 78,
        "experience": 88
      },
      "highlights": ["项目描述清晰", "技能栈匹配度高"],
      "suggestions": [
        { "type": "CONTENT", "content": "增加更多量化的项目成果" },
        { "type": "SKILL", "content": "建议补充 Node.js 后端经验" }
      ],
      "parsing_status": "COMPLETED"
    },
    "status": "COMPLETED",
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
| &#124;- &#124;- id | string | 非必须 | 记录 ID，对应 user_vector_store.id |
| &#124;- &#124;- resume_file_path | string | 非必须 | OSS 文件访问 URL，对应 user_vector_store.resume_file_path |
| &#124;- &#124;- resume_analysis_id | string | 非必须 | 简历分析 ID，对应 user_vector_store.resume_analysis_id |
| &#124;- &#124;- file_type | string | 非必须 | 文件类型，取自 user_vector_store.metadata->>'file_type' |
| &#124;- &#124;- file_name | string | 非必须 | 原始文件名，取自 user_vector_store.metadata->>'original_file_name' |
| &#124;- &#124;- status | string | 非必须 | 解析状态，取自 user_vector_store.metadata->>'parsing_status' |
| &#124;- &#124;- created_at | string | 非必须 | 创建时间，对应 user_vector_store.create_time |
| &#124;- &#124;- updated_at | string | 非必须 | 更新时间，对应 user_vector_store.update_time |
| &#124;- next_cursor | string | 非必须 | 下一页游标，为 null 时表示已是最后一页 |

响应数据样例：
```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "items": [
      {
        "id": "1",
        "resume_file_path": "https://your-bucket.oss-cn-shenzhen.aliyuncs.com/resumes/2026/03/26/u_001_1711361200.pdf",
        "resume_analysis_id": "ra_20260326_001",
        "file_type": "pdf",
        "file_name": "张三_前端工程师简历.pdf",
        "status": "COMPLETED",
        "created_at": "2026-03-26T09:10:00+08:00",
        "updated_at": "2026-03-26T09:12:00+08:00"
      },
      {
        "id": "2",
        "resume_file_path": "https://your-bucket.oss-cn-shenzhen.aliyuncs.com/resumes/2026/03/25/u_001_1711274800.pdf",
        "resume_analysis_id": "ra_20260325_001",
        "file_type": "docx",
        "file_name": "张三_简历_v2.docx",
        "status": "COMPLETED",
        "created_at": "2026-03-25T09:10:00+08:00",
        "updated_at": "2026-03-25T09:12:00+08:00"
      }
    ],
    "next_cursor": null
  }
}
```
