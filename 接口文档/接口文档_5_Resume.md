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

---

### student_capability_profile 表（学生就业能力画像表）

**新增表**，存储通过大模型技术从简历拆解的学生就业能力画像数据。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGSERIAL | 主键 |
| user_id | BIGINT | 用户 ID |
| resume_analysis_id | BIGINT | 关联 resume_analysis_result.id |
| overall_score | INT | 综合能力评分 (0-100) |
| completeness_score | INT | 简历完整度评分 (0-100) |
| competitiveness_score | INT | 竞争力评分 (0-100) |
| capability_scores | JSONB | 各维度能力得分 |
| professional_skills | JSONB | 专业技能列表 |
| certificates | JSONB | 证书列表 |
| soft_skills | JSONB | 软技能评估 |
| ai_evaluation | TEXT | AI 综合评价 |
| generated_at | DATETIME | 生成时间 |
| updated_at | DATETIME | 更新时间 |

#### capability_scores 字段结构示例

```json
{
  "professional_skill": 85,    // 专业技能 (0-100)
  "certificate": 70,           // 证书 (0-100)
  "innovation": 80,            // 创新能力 (0-100)
  "learning": 88,              // 学习能力 (0-100)
  "resilience": 75,            // 抗压能力 (0-100)
  "communication": 82,         // 沟通能力 (0-100)
  "internship": 78             // 实习能力 (0-100)
}
```

#### professional_skills 字段结构示例

```json
[
  {
    "name": "Vue.js",
    "proficiency": 4,          // 熟练度 (1-5)
    "years": 2,                // 使用年限
    "evidence": "完成 3 个中大型项目"
  },
  {
    "name": "React",
    "proficiency": 3,
    "years": 1,
    "evidence": "个人项目实践"
  }
]
```

#### soft_skills 字段结构示例

```json
{
  "innovation": {
    "score": 80,
    "evidence": ["主导创新项目 2 项", "获省级竞赛奖项"],
    "description": "具备较强的创新意识和实践能力"
  },
  "learning": {
    "score": 88,
    "evidence": ["自学完成 3 门技术栈", "技术博客 50+ 篇"],
    "description": "学习能力强，能快速掌握新技术"
  },
  "resilience": {
    "score": 75,
    "evidence": ["实习期间承担高压项目"],
    "description": "能在压力下保持工作效率"
  },
  "communication": {
    "score": 82,
    "evidence": ["担任学生会干部", "技术分享 10+ 场"],
    "description": "沟通表达能力良好"
  },
  "internship": {
    "score": 78,
    "evidence": ["2 段相关企业实习经历", "参与核心模块开发"],
    "description": "具备一定的实战经验"
  }
}
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

---

## 5.3 获取学生能力画像

### 5.3.1 基本信息

请求路径：/api/resume/capability-profile

请求方式：GET

接口描述：该接口用于获取当前用户的学生就业能力画像。通过大模型技术将用户简历数据拆解成能力画像，包含完整度评分、竞争力评分及各维度能力评估。

**数据来源**：基于简历解析结果，由 AI 服务分析生成。

---

### 5.3.2 请求参数

参数说明：

本接口无需请求参数。

---

### 5.3.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
| :--- | :--- | :--- | :--- |
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据 |
| &#124;- id | number | 非必须 | 画像记录 ID |
| &#124;- user_id | number | 非必须 | 用户 ID |
| &#124;- overall_score | number | 非必须 | 综合能力评分 (0-100) |
| &#124;- completeness_score | number | 非必须 | 简历完整度评分 (0-100) |
| &#124;- competitiveness_score | number | 非必须 | 竞争力评分 (0-100) |
| &#124;- capability_scores | object | 非必须 | 各维度能力得分 |
| &#124;- &#124;- professional_skill | number | 非必须 | 专业技能 (0-100) |
| &#124;- &#124;- certificate | number | 非必须 | 证书 (0-100) |
| &#124;- &#124;- innovation | number | 非必须 | 创新能力 (0-100) |
| &#124;- &#124;- learning | number | 非必须 | 学习能力 (0-100) |
| &#124;- &#124;- resilience | number | 非必须 | 抗压能力 (0-100) |
| &#124;- &#124;- communication | number | 非必须 | 沟通能力 (0-100) |
| &#124;- &#124;- internship | number | 非必须 | 实习能力 (0-100) |
| &#124;- professional_skills | object[] | 非必须 | 专业技能列表 |
| &#124;- &#124;- name | string | 非必须 | 技能名称 |
| &#124;- &#124;- proficiency | number | 非必须 | 熟练度 (1-5) |
| &#124;- &#124;- years | number | 非必须 | 使用年限 |
| &#124;- &#124;- evidence | string | 非必须 | 能力证明 |
| &#124;- certificates | string[] | 非必须 | 证书列表 |
| &#124;- soft_skills | object | 非必须 | 软技能评估详情 |
| &#124;- ai_evaluation | string | 非必须 | AI 综合评价文字 |
| &#124;- generated_at | string | 非必须 | 生成时间 |

响应数据样例：
```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "id": 1,
    "user_id": 1001,
    "overall_score": 82,
    "completeness_score": 88,
    "competitiveness_score": 78,
    "capability_scores": {
      "professional_skill": 85,
      "certificate": 70,
      "innovation": 80,
      "learning": 88,
      "resilience": 75,
      "communication": 82,
      "internship": 78
    },
    "professional_skills": [
      {
        "name": "Vue.js",
        "proficiency": 4,
        "years": 2,
        "evidence": "完成 3 个中大型项目"
      },
      {
        "name": "React",
        "proficiency": 3,
        "years": 1,
        "evidence": "个人项目实践"
      }
    ],
    "certificates": ["大学英语六级", "软件设计师（中级）"],
    "soft_skills": {
      "innovation": {
        "score": 80,
        "evidence": ["主导创新项目 2 项", "获省级竞赛奖项"],
        "description": "具备较强的创新意识和实践能力"
      },
      "learning": {
        "score": 88,
        "evidence": ["自学完成 3 门技术栈", "技术博客 50+ 篇"],
        "description": "学习能力强，能快速掌握新技术"
      },
      "resilience": {
        "score": 75,
        "evidence": ["实习期间承担高压项目"],
        "description": "能在压力下保持工作效率"
      },
      "communication": {
        "score": 82,
        "evidence": ["担任学生会干部", "技术分享 10+ 场"],
        "description": "沟通表达能力良好"
      },
      "internship": {
        "score": 78,
        "evidence": ["2 段相关企业实习经历", "参与核心模块开发"],
        "description": "具备一定的实战经验"
      }
    },
    "ai_evaluation": "该同学专业技能扎实，学习能力突出，具备较好的创新意识和团队协作能力。建议进一步加强企业级项目实战经验，补充相关权威证书以提升竞争力。",
    "generated_at": "2026-03-30T10:00:00+08:00"
  }
}
```

---

## 5.4 简历预览

### 5.4.1 基本信息

请求路径：/api/resume/analysis/{vector_store_id}/preview

请求方式：GET

接口描述：该接口用于在浏览器内嵌预览简历原件，由后端从 OSS 读取文件并返回适合浏览器内嵌展示的响应头。解决了 OSS 直链触发下载、无法内嵌预览的问题。

---

### 5.4.2 请求参数

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
| :--- | :--- | :--- | :--- |
| vector_store_id | string | 必须 | 向量存储记录 ID（UUID）（Path 参数） |
| disposition | string | 非必须 | 响应头设置：`inline`（默认，内联预览）/ `attachment`（强制下载） |

请求参数样例：
```
GET /api/resume/analysis/37a77bb4-08e0-47c2-b504-5137b1e4ebc9/preview
GET /api/resume/analysis/37a77bb4-08e0-47c2-b504-5137b1e4ebc9/preview?disposition=attachment
```

---

### 5.4.3 响应数据

响应头：

| Header | 要求 |
| :--- | :--- |
| `Content-Type` | 与文件一致，例如 PDF：`application/pdf` |
| `Content-Disposition` | `inline` 时：`inline; filename*=UTF-8''<编码后的文件名>` |
| `Cache-Control` | 建议：`private, max-age=300`（5 分钟），避免公共缓存 |
| `X-Content-Type-Options` | 建议：`nosniff` |

响应体：文件二进制流

---

### 5.4.4 错误响应

| HTTP 状态码 | 场景 | JSON 响应样例 |
| :--- | :--- | :--- |
| `401` | 未登录或 token 失效 | `{"code": 401, "msg": "未登录"}` |
| `403` | 无权限访问该记录 | `{"code": 403, "msg": "无权访问该简历"}` |
| `404` | 记录不存在或文件已删除 | `{"code": 404, "msg": "简历文件不存在或已过期"}` |
| `415` | 不支持预览该文件类型 | `{"code": 415, "msg": "暂不支持预览该文件类型，请下载后查看"}` |

---

### 5.4.5 前端使用示例

```html
<!-- 在历史简历列表中，使用 iframe 内嵌预览 -->
<iframe
  src="/api/resume/analysis/37a77bb4-08e0-47c2-b504-5137b1e4ebc9/preview"
  width="100%"
  height="600px"
  style="border: none;">
</iframe>

<!-- 下载按钮 -->
<button onclick="downloadResume('37a77bb4-08e0-47c2-b504-5137b1e4ebc9')">下载简历</button>

<script>
  function downloadResume(vectorStoreId) {
    window.location.href = `/api/resume/analysis/${vectorStoreId}/preview?disposition=attachment`;
  }
</script>
```

---

### 5.4.6 支持预览的文件类型

| 类型 | Content-Type | 策略 |
| :--- | :--- | :--- |
| PDF | `application/pdf` | 直接流式返回 `inline` |
| DOCX | — | 返回 `415` + 提示「请下载后查看」 |
| PPTX | — | 返回 `415` + 提示「请下载后查看」 |
| HTML | `text/html` | 直接流式返回 `inline` |
| TXT | `text/plain` | 直接流式返回 `inline` |

首版可仅支持 PDF 内嵌预览，与现有前端 `historyRemotePdfUrl` 逻辑对齐。

---

## 5.5 获取简历预览 URL（可选）

### 5.5.1 基本信息

请求路径：/api/resume/analysis/{vector_store_id}/preview-url

请求方式：GET

接口描述：该接口返回一个短期有效的 OSS 签名 URL，用于不经过应用服务器中转的直接预览。若 OSS 禁止被第三方页面 iframe，此方案可能失败。

---

### 5.5.2 请求参数

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
| :--- | :--- | :--- | :--- |
| vector_store_id | string | 必须 | 向量存储记录 ID（UUID）（Path 参数） |

---

### 5.5.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
| :--- | :--- | :--- | :--- |
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据 |
| &#124;- url | string | 非必须 | 短期有效 HTTPS 地址，`Content-Disposition` 应为 `inline` |
| &#124;- expires_at | string | 非必须 | 过期时间（ISO8601） |
| &#124;- mime_type | string | 非必须 | 文件 MIME 类型 |

响应数据样例：
```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "url": "https://bucket.oss.aliyuncs.com/...?Expires=...&Signature=...",
    "expires_at": "2026-03-28T12:10:00+08:00",
    "mime_type": "application/pdf"
  }
}
```

---

## AI 服务模块

### Resume-AI 服务职责

由专门的 **Resume-AI** 服务负责以下分析任务：

1. **简历解析与结构化**：
   - 从简历中提取基本信息、教育经历、工作经历、项目经历、技能列表等
   - 支持 PDF、DOCX、PPTX、HTML、TXT 多种格式

2. **学生就业能力画像生成**：
   - 通过大模型技术将简历数据拆解成能力画像
   - 评估维度：专业技能、证书、创新能力、学习能力、抗压能力、沟通能力、实习能力
   - 生成完整度评分和竞争力评分
   - 提供 AI 综合评价文字

3. **简历评分与建议**：
   - 关键词匹配度评分
   - 布局评分
   - 技能深度评分
   - 经历评分
   - 生成优化建议

### Dashboard-AI 服务职责

由专门的 **Dashboard-AI** 服务负责以下任务（在 Resume-AI 完成后自动触发）：

1. **人岗匹配计算**：
   - 从 `student_capability_profile.capability_scores` 获取用户能力评分
   - 与岗位画像库中的岗位进行匹配计算
   - 生成 `user_career_data.match_summary`（匹配分数、描述、标签、各维度得分）

2. **能力雷达数据填充**：
   - 将 `student_capability_profile.capability_scores` 映射为 `user_career_data.skill_radar`

3. **行动建议转换**：
   - 将 `resume_analysis_result.suggestions` 转换为可执行的行动项
   - 填充到 `user_career_data.actions`

4. **职业路径生成**：
   - 根据用户能力画像和岗位画像生成职业发展路径
   - 填充到 `user_roadmap_steps.steps`

---

## 数据流转说明

```
前端上传文件
    ↓
POST /api/resume/upload
    ↓
后端存储至 OSS，创建 user_vector_store 记录
    ↓
异步任务（Resume-AI 服务）：
  1. 解析简历、生成向量
  2. 创建 resume_analysis_result 记录（parsed_data、scores、highlights、suggestions）
  3. 生成 student_capability_profile（学生能力画像，capability_scores 等）
    ↓
异步任务（Dashboard-AI 服务，在 Resume-AI 完成后自动触发）：
  4. 读取 student_capability_profile.capability_scores → 填充 user_career_data.skill_radar
  5. 读取 resume_analysis_result.highlights → 转换为 user_career_data.match_summary.tags
  6. 读取 resume_analysis_result.suggestions → 转换为 user_career_data.actions
  7. 结合岗位画像库计算 → user_career_data.match_summary.score、dimension_scores
  8. 生成职业发展路径 → user_roadmap_steps.steps
    ↓
前端轮询 GET /api/resume/analysis/{id}
    ↓
查询到 parsed_data 等完整数据时展示结果（此时 Dashboard 数据也已准备就绪）
```

**说明**：
- Dashboard 模块的两张表（`user_career_data`、`user_roadmap_steps`）在简历分析完成后**自动填充**，用户无需额外操作
- 前端在简历上传成功后，可直接跳转 Dashboard 页面，无需等待
- 如果岗位画像库尚未构建完成，match_summary 相关字段可先使用 AI 生成的默认值

---

## 表关系说明

```
user_vector_store (向量存储表)
    ↓ (1:1 关系)
resume_analysis_result (分析结果表)
    ↓ (1:1 关系)
student_capability_profile (学生能力画像表)
    ↓ (AI 聚合计算，自动填充)
user_career_data (Dashboard 职业数据表)
user_roadmap_steps (职业发展路径表)
```

- **user_vector_store**：存储简历的向量化数据，用于 Spring AI 向量搜索
- **resume_analysis_result**：存储详细的分析结果，包括解析数据、评分、建议等
- **student_capability_profile**：存储学生就业能力画像，包括各维度能力评估、证书、软技能等
- **user_career_data**：Dashboard 模块使用，存储岗位匹配、市场趋势、能力雷达、行动建议（数据来源：student_capability_profile、resume_analysis_result）
- **user_roadmap_steps**：Dashboard/Roadmap 模块共用，存储职业发展阶段路径（数据来源：AI 根据用户能力画像生成）

---

## 接口列表汇总

| 接口编号 | 接口名 | 方法 | 路径 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| 5.1 | 上传简历 | POST | /api/resume/upload | 上传简历文件 |
| 5.2 | 获取简历分析结果 | GET | /api/resume/analysis/{id} | 查询简历解析结果 |
| 5.3 | 获取学生能力画像 | GET | /api/resume/capability-profile | 获取能力画像（完整度/竞争力/各维度评分） |
| 5.4 | 简历预览 | GET | /api/resume/analysis/{id}/preview | 内嵌预览简历 |
| 5.5 | 获取预览 URL | GET | /api/resume/analysis/{id}/preview-url | 获取 OSS 签名 URL |

---

## 2026-06-12 Resume-AI Python RAG Contract

### Change Summary

Resume upload and public Java API paths remain unchanged. Java keeps authentication, file validation, OSS upload, base text extraction, async status tracking, and database writes. Resume AI/RAG analysis moves behind a Python HTTP boundary at `POST /api/v1/resume/analyze`.

This migration is a deterministic fallback implementation. It does not claim production pgvector semantic retrieval, Dashscope LLM generation, cross-encoder reranking, or offline RAG quality evaluation.

### Public Java API

| Method | Path | Auth | Purpose |
| :--- | :--- | :--- | :--- |
| POST | `/api/resume/upload` | JWT | Upload resume, create `user_vector_store`, start async Python-backed analysis |
| GET | `/api/resume/analysis/{id}` | JWT | Poll async status and final analysis result |
| GET | `/api/resume/analysis` | JWT | List current user's resume analyses |
| GET | `/api/resume/capability-profile` | JWT | Get latest capability profile for current user |
| GET | `/api/resume/analysis/{id}/preview` | JWT | Stream resume file preview |
| GET | `/api/resume/analysis/{id}/preview-url` | JWT | Return signed preview URL |

All `vectorStoreId` entry points must preserve current-user ownership checks. If a legacy path lacks a strict `vectorStoreId + userId` mapper query, this must be recorded as a remaining risk and not hidden by the Python migration.

### Java To Python Boundary

Java calls Python only through HTTP. Java must not import Python modules or reimplement Resume AI/RAG with Spring AI `ChatClient`, `VectorStore`, or `OpenAiEmbeddingModel`.

Python endpoint:

```http
POST /api/v1/resume/analyze
Content-Type: application/json
```

Resume Python base URL priority:

1. `FUCHUANG_AI_PYTHON_RESUME_BASE_URL`
2. `fuchuang.ai.python.resume-base-url`
3. `FUCHUANG_AI_PYTHON_BASE_URL`
4. `fuchuang.ai.python.base-url`
5. `http://127.0.0.1:8091`

Resume Python timeout priority:

1. `FUCHUANG_AI_PYTHON_RESUME_TIMEOUT_SECONDS`
2. `fuchuang.ai.python.resume-timeout-seconds`
3. `fuchuang.ai.python.timeout-seconds`
4. `30`

Retry rule: Java does not retry. The async task marks the analysis as `failed` when Python is unavailable, times out, or returns an invalid response.

Idempotency key: `vectorStoreId + userId`. Repeated calls for the same pair must be safe to process and overwrite the same analysis row. Java owns database persistence.

Java enforces this in the service and mapper layer for this migration: before Python analysis starts, Java queries the latest `resume_analysis_result` by `vector_store_id + user_id`; if a row exists, Java resets that row to `processing` and later updates by `id + user_id`; if no row exists, Java inserts one row and uses the generated `id` for progress, completion, and failure updates. This pass does not add a database migration or unique constraint.

### Python Request Schema

```json
{
  "vector_store_id": "37a77bb4-08e0-47c2-b504-5137b1e4ebc9",
  "user_id": 1001,
  "resume_text": "sanitized extracted resume text",
  "file_type": "pdf",
  "original_file_name": "resume.pdf",
  "resume_file_path": "https://oss.example/resume.pdf",
  "metadata": {
    "source": "resume_upload",
    "visibility": "user",
    "document_type": "resume"
  }
}
```

Required fields: `vector_store_id`, `user_id`, `resume_text`. Empty or whitespace-only `resume_text` is invalid and maps to Java upload/analysis failure.

### Python Success Response Schema

```json
{
  "status": "completed",
  "parsed_data": {
    "name": "",
    "target_role": "AI Agent Intern",
    "location": "",
    "current_role": "",
    "skills": ["Python", "RAG"],
    "experience_years": 0,
    "match_score": 70,
    "education": [],
    "experience": []
  },
  "scores": {
    "keyword_match": 70,
    "layout": 70,
    "skill_depth": 70,
    "experience": 60
  },
  "highlights": [
    "Evidence-backed resume highlight"
  ],
  "suggestions": [
    {
      "type": "SKILL",
      "content": "Add measurable RAG project outcomes."
    }
  ],
  "capability_profile": {
    "overall_score": 70,
    "completeness_score": 70,
    "competitiveness_score": 65,
    "capability_scores": {
      "professional_skill": 70,
      "certificate": 50,
      "innovation": 65,
      "learning": 75,
      "resilience": 65,
      "communication": 65,
      "internship": 60
    },
    "professional_skills": [
      {
        "name": "Python",
        "proficiency": 3,
        "years": 1,
        "evidence": "Detected from resume evidence"
      }
    ],
    "certificates": [],
    "soft_skills": {},
    "ai_evaluation": "Deterministic fallback evaluation based on extracted evidence."
  },
  "rag_diagnostics": {
    "chunk_count": 4,
    "summary_index_count": 5,
    "metadata_filters": {
      "user_id": 1001,
      "document_type": "resume",
      "visibility": "user"
    },
    "queries": ["resume skills", "target role", "project evidence"],
    "retrieval": {
      "bm25": true,
      "embedding_fallback": "hash",
      "fusion": "rrf",
      "reranker": "deterministic"
    },
    "selected_evidence_ids": ["chunk-0"],
    "sensitive_text_included": false
  }
}
```

`rag_diagnostics` must not contain full resume text, raw prompts, raw Python responses, tokens, keys, or OSS credentials. Evidence references should use ids, section labels, or short sanitized snippets only.

### RAG Capability Acceptance

The Python fallback must expose testable behavior for:

- Recursive chunking by section, paragraph, sentence, and size budget.
- Document and section summary index records.
- Metadata filtering by `user_id`, `document_type`, `vector_store_id`, and visibility.
- Multi-Query expansion for career intent, skill evidence, gap analysis, and role matching.
- BM25 plus deterministic hash-embedding hybrid retrieval.
- RAG-Fusion via reciprocal rank fusion.
- Deterministic fallback reranker.
- Evidence/citation ids returned to diagnostics.
- Sanitized diagnostics without full resume text.

### Java Persistence Contract

Java creates or upserts `user_vector_store` after text extraction and before returning the upload id. This write stores `id`, `user_id`, `content`, `resume_file_path`, `vector_type='resume'`, `metadata`, `create_time`, and `update_time`. Java must not write a fake embedding value.

Java inserts `resume_analysis_result` with `status='processing'` and `progress=0`, then updates it to:

- `completed`, `progress=100`, with `parsed_data`, `scores`, `highlights`, and `suggestions` from Python.
- `failed`, `progress=0`, with safe empty JSON fields when Python fails or the response is invalid.

Java persists `capability_profile` from the Python `capability_profile` object. Dashboard data generation may reuse the persisted profile and analysis record, but this migration does not move Dashboard generation.

### Error Mapping

| Python/Boundary Case | Java Handling |
| :--- | :--- |
| Empty resume text before Python call | Reject upload or mark analysis failed with `progress=0` |
| Python HTTP 400 | Mark failed; log status and safe message only |
| Python HTTP 5xx | Mark failed; log status only |
| Timeout | Mark failed; no retry |
| Connection refused/unavailable | Mark failed; no retry |
| Empty response body | Mark failed |
| Invalid JSON | Mark failed |
| Schema missing required result fields | Mark failed |
| `status != completed` | Mark failed unless explicitly documented |

Logs must use ids, counts, status codes, and diagnostic ids. Logs must not print raw resume text, raw Python response, raw AI response, JWT tokens, API keys, or OSS credentials.

### Frontend Impact

`website/src/api/resume.ts` continues to call the Java API only. The frontend keeps polling `GET /api/resume/analysis/{id}` by `status` and `progress`; it must not depend on `resume_content` to finish polling.

No direct browser call to Python is supported.

### Verification Gate

Required verification for this migration:

- Python unit tests: `$env:PYTHONPATH='ai-service'; python -B -m pytest ai-service/tests/test_resume_analysis_service.py -q -p no:cacheprovider` exits 0, runs `N > 0`, and has 0 failures, 0 errors, 0 skipped.
- Python HTTP smoke starts `career_ai.resume_analysis_service` from the `ai-service` directory, verifies valid payload returns HTTP 200 with the documented schema, verifies invalid payload returns HTTP 400, and cleans up the process.
- Java tests cover Python client success, timeout, HTTP 5xx, empty response, invalid JSON, schema failure, Resume success flow, Resume failure flow, and `user_vector_store` insert/upsert behavior.
- `mvn -pl server -am -DskipTests compile` passes.
- Frontend build runs only if `website/src/api/resume.ts` changes; otherwise record why it was not needed.
- Static contract gate confirms this document, Python schema/response, and Java client parsing logic agree.
- `git diff --check` runs before commit and `git diff --check origin/master..HEAD` runs after commit.
- Secret/PII gate fails on unredacted secrets, raw resume body, raw Python response, token/key, or OSS credential in code, logs, or tests.
- Scope gate allows only Resume/Python/documentation/test/log/Obsidian files and denies `database/`, `application*.yml`, non-Resume modules, legacy `ai_service/`, cache, and build artifacts.

If detailed automation logs are needed, keep them in local notes or the developer's personal workspace instead of committing them to the GitHub runtime repository. Formal acceptance evidence should be summarized in this document or in a stable `docs/` page.
