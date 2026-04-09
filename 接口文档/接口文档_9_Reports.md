# 职引 AI - Reports 模块接口文档

## 模块概述

职业发展报告相关接口，包括报告生成、获取、详情查看、编辑优化和 PDF 下载。
本模块基于学生能力画像和岗位画像匹配，由 AI 生成职业生涯发展报告。

---

## 数据模型

### career_reports 表（职业生涯发展报告表）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGSERIAL | 主键 |
| report_no | VARCHAR(50) | 报告编号（唯一） |
| user_id | BIGINT | 用户 ID |
| student_capability_id | BIGINT | 关联学生能力画像 ID |
| target_job_profile_id | BIGINT | 目标岗位画像 ID |
| match_score | INT | 人岗匹配总分 (0-100) |
| match_details | JSONB | 匹配详情（各维度得分） |
| self_discovery | JSONB | 自我认知模块内容 |
| target_job | JSONB | 职业目标设定(从user_career_job中获取target_job) |
| development_path | JSONB | 职业发展路径（从user_career_date中获取用户更新时间距离当前时间最近的列，然后获取target_job字段，然后再从job表中获取job_category_name与target_job相同，但是job_level不同的，（由INTERNSHIP -> JUNIOR -> MID -> SENIOR）(如果存在的话） |
| action_plan | JSONB | 行动计划(从goal表中获取) |
| ai_suggestions | TEXT | AI 建议(从resume_analysis_result表中suggestions字段获取即可) |
| status | VARCHAR(20) | 状态：DRAFT/PROCESSING/COMPLETED/ARCHIVED/FAILED |
| is_editable | BOOLEAN | 是否允许编辑 |
| pdf_file_path | VARCHAR(500) | 生成 PDF 的 OSS 路径 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

#### match_details 字段结构示例

```json
{
  "overall": 85,
  "basic_requirements": 90,      // 基础要求匹配分
  "professional_skills": 82,     // 职业技能匹配分
  "professional_quality": 80,    // 职业素养匹配分
  "development_potential": 85    // 发展潜力匹配分
}
```

#### self_discovery 字段结构示例

```json
{
  "title": "自我认知",
  "capability_summary": "你具备扎实的专业技能和较强的学习能力...",
  "radar_chart": {
    "professional_skill": 85,
    "certificate": 70,
    "innovation": 80,
    "learning": 88,
    "resilience": 75,
    "communication": 82,
    "internship": 78
  },
  "strengths": ["学习能力强", "专业技能扎实", "创新意识好"],
  "weaknesses": ["证书较少", "实战经验不足"]
}
```

#### career_goal 字段结构示例

```json
{
  "title": "职业目标设定",
  "short_term": {
    "period": "1-2 年",
    "goal": "初级 UI/视觉设计师",
    "description": "掌握核心设计技能，完成 3-5 个完整项目"
  },
  "medium_term": {
    "period": "3-5 年",
    "goal": "高级 UI 设计师/体验设计组长",
    "description": "独立负责产品线设计，带领小团队"
  },
  "long_term": {
    "period": "5-10 年",
    "goal": "设计总监/创意总监",
    "description": "负责整体设计战略，管理设计团队"
  }
}
```

#### development_path 字段结构示例

```json
{
  "title": "职业发展路径",
  "path_type": "vertical",
  "steps": [
    {
      "order": 1,
      "title": "初级 UI/视觉设计师",
      "period": "第 1-2 年",
      "match_rate": 85,
      "requirements": ["掌握设计工具", "理解设计理论", "完成作品集"],
      "status": "current"
    },
    {
      "order": 2,
      "title": "高级 UI 设计师",
      "period": "第 3-5 年",
      "match_rate": 70,
      "requirements": ["独立完成项目", "带新人", "跨部门协作"],
      "status": "future"
    },
    {
      "order": 3,
      "title": "设计总监",
      "period": "第 5-10 年",
      "match_rate": 50,
      "requirements": ["战略规划", "团队管理", "行业影响力"],
      "status": "future"
    }
  ]
}
```

#### action_plan 字段结构示例

```json
{
  "title": "行动计划",
  "short_term_plan": {
    "period": "1-6 个月",
    "goals": [
      {
        "id": "a_001",
        "title": "完成 Figma 高级教程学习",
        "type": "learning",
        "priority": "high",
        "deadline": "2026-06-30",
        "status": "pending"
      },
      {
        "id": "a_002",
        "title": "参与 2 个实际设计项目",
        "type": "practice",
        "priority": "high",
        "deadline": "2026-09-30",
        "status": "pending"
      }
    ]
  },
  "medium_term_plan": {
    "period": "6-12 个月",
    "goals": [
      {
        "id": "a_003",
        "title": "考取 Adobe 认证专家证书",
        "type": "certificate",
        "priority": "medium",
        "deadline": "2026-12-31",
        "status": "pending"
      }
    ]
  },
  "evaluation_cycle": {
    "type": "quarterly",
    "metrics": ["技能提升", "项目数量", "作品集质量"]
  }
}
```

---

## AI 服务模块

### Reports-AI 服务职责

由专门的 **Reports-AI** 服务负责以下分析任务：

1. **人岗匹配分析**：
   - 基于学生能力画像和岗位画像进行匹配
   - 从基础要求、职业技能、职业素养、发展潜力 4 个维度分析
   - 量化呈现契合度与差距
   - 关键技能匹配准确率不低于 80%

2. **职业目标设定与路径规划**：
   - 结合职业探索结果与个人意愿制定职业目标
   - 分析本职业的社会需求与行业发展趋势
   - 分析企业岗位数据关联性
   - 构建清晰的职业发展路径

3. **行动计划生成**：
   - 制定分阶段（短期、中期）的个性化成长计划
   - 包括学习路径、实践安排（实习、项目等）
   - 设计评估周期与指标，支持动态调整

4. **报告润色与优化**：
   - 智能润色报告文字
   - 内容完整性检查
   - 支持手动编辑调整

---

## 接口列表

| 接口名 | 方法 | 路径 | 说明 |
| :--- | :--- | :--- | :--- |
| 生成职业报告 | POST | `/api/reports/generate` | 异步生成报告 |
| 获取最新报告 | GET | `/api/reports/latest` | 获取用户最新报告概览 |
| 获取报告列表 | GET | `/api/reports` | 获取用户历史报告列表（默认最近 20 条） |
| 获取报告详情 | GET | `/api/reports/{id}` | 按 id 获取 |
| 更新报告内容 | PUT | `/api/reports/{id}` | 编辑报告内容 |
| 重新生成报告 | POST | `/api/reports/{id}/regenerate` | 基于既有报告参数重新生成（异步） |
| 下载报告 PDF | GET | `/api/reports/{id}/download` | 二进制流 |
| 删除报告 | DELETE | `/api/reports/{id}` | 删除报告 |

---

## 详细接口定义

### 9.1 生成职业报告

- **请求方法**: `POST`
- **请求路径**: `/api/reports/generate`
- **鉴权**: 需要
- **请求体**:
```json
{
  "target_job_profile_id": 1001,    // 目标岗位画像 ID，不传则使用最匹配岗位
  "career_preference": {
    "preferred_city": "深圳",        // 偏好城市（可选）
    "expected_salary": "15-25k",    // 期望薪资（可选）
    "career_direction": "技术路线"   // 职业方向偏好（可选）
  }
}
```
- **响应示例（异步）**:
```json
{
  "code": 200,
  "msg": "报告生成中，请稍后查看",
  "data": {
    "report_id": "rpt_20260330_001",
    "report_no": "CR202603300001",
    "status": "PROCESSING",
    "estimated_time": 60
  }
}
```

**说明**：
- 报告生成为异步过程，前端需轮询 `/api/reports/{id}` 获取状态
- 预计生成时间约 60 秒
- 生成完成后状态变为 `COMPLETED`
- 若生成失败，推荐后端将 `status` 置为 `FAILED` 并返回失败原因（或使用 `4xx/5xx` 直接返回错误）

---

### 9.2 获取最新报告

- **请求方法**: `GET`
- **请求路径**: `/api/reports/latest`
- **鉴权**: 需要
- **响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 1,
    "report_no": "CR202603300001",
    "title": "职业生涯发展报告",
    "status": "COMPLETED",
    "match_score": 85,
    "target_job": "UI/视觉设计师",
    "generated_at": "2026-03-30T10:30:00+08:00",
    "updated_at": "2026-03-30T10:32:00+08:00"
  }
}
```

---

### 9.2.1 获取报告列表

- **请求方法**: `GET`
- **请求路径**: `/api/reports`
- **鉴权**: 需要
- **Query 参数**:
  - `limit`：可选，默认 `20`
  - `status`：可选，按状态过滤（`DRAFT`/`PROCESSING`/`COMPLETED`/`ARCHIVED`/`FAILED`）
- **响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "items": [
      {
        "id": 1,
        "title": "职业生涯发展报告",
        "status": "COMPLETED",
        "match_score": 85,
        "target_job": "UI/视觉设计师",
        "generated_at": "2026-03-30T10:30:00+08:00"
      }
    ]
  }
}
```

**说明**：
- 默认返回最近 `limit` 条报告（按创建时间倒序）
- 列表用于前端历史切换，不返回完整 `sections`

---

### 9.3 获取报告详情

- **请求方法**: `GET`
- **请求路径**: `/api/reports/{id}`
- **鉴权**: 需要
- **响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 1,
    "report_no": "CR202603300001",
    "title": "职业生涯发展报告",
    "status": "COMPLETED",
    "user_id": 1001,
    "target_job": {
      "id": 1001,
      "name": "UI/视觉设计师",
      "industry": "互联网/科技",
      "city": "深圳"
    },
    "match_score": 85,
    "match_details": {
      "overall": 85,
      "basic_requirements": 90,
      "professional_skills": 82,
      "professional_quality": 80,
      "development_potential": 85
    },
    "sections": [
      {
        "key": "self_discovery",
        "title": "自我认知",
        "content": {
          "capability_summary": "你具备扎实的专业技能和较强的学习能力...",
          "radar_chart": {
            "professional_skill": 85,
            "certificate": 70,
            "innovation": 80,
            "learning": 88,
            "resilience": 75,
            "communication": 82,
            "internship": 78
          },
          "strengths": ["学习能力强", "专业技能扎实", "创新意识好"],
          "weaknesses": ["证书较少", "实战经验不足"]
        }
      },
      {
        "key": "match_analysis",
        "title": "人岗匹配分析",
        "content": {
          "summary": "你与 UI/视觉设计师岗位的匹配度为 85%，在基础要求和专业技能方面表现良好...",
          "dimension_analysis": {
            "basic_requirements": {
              "score": 90,
              "description": "学历、专业等基础条件符合要求"
            },
            "professional_skills": {
              "score": 82,
              "description": "掌握核心设计工具，建议深化专业技能"
            },
            "professional_quality": {
              "score": 80,
              "description": "具备良好的职业素养和团队协作能力"
            },
            "development_potential": {
              "score": 85,
              "description": "学习能力强，发展潜力较大"
            }
          },
          "gap_analysis": [
            {
              "dimension": "职业技能",
              "gap": "缺少商业项目实战经验",
              "suggestion": "建议参与实际设计项目积累经验"
            },
            {
              "dimension": "证书",
              "gap": "缺少权威设计证书",
              "suggestion": "建议考取 Adobe 认证专家证书"
            }
          ]
        }
      },
      {
        "key": "career_goal",
        "title": "职业目标设定",
        "content": {
          "short_term": {
            "period": "1-2 年",
            "goal": "初级 UI/视觉设计师",
            "description": "掌握核心设计技能，完成 3-5 个完整项目"
          },
          "medium_term": {
            "period": "3-5 年",
            "goal": "高级 UI 设计师/体验设计组长",
            "description": "独立负责产品线设计，带领小团队"
          },
          "long_term": {
            "period": "5-10 年",
            "goal": "设计总监/创意总监",
            "description": "负责整体设计战略，管理设计团队"
          }
        }
      },
      {
        "key": "development_path",
        "title": "职业发展路径",
        "content": {
          "path_type": "vertical",
          "steps": [
            {
              "order": 1,
              "title": "初级 UI/视觉设计师",
              "period": "第 1-2 年",
              "match_rate": 85,
              "requirements": ["掌握设计工具", "理解设计理论", "完成作品集"],
              "status": "current"
            },
            {
              "order": 2,
              "title": "高级 UI 设计师",
              "period": "第 3-5 年",
              "match_rate": 70,
              "requirements": ["独立完成项目", "带新人", "跨部门协作"],
              "status": "future"
            },
            {
              "order": 3,
              "title": "设计总监",
              "period": "第 5-10 年",
              "match_rate": 50,
              "requirements": ["战略规划", "团队管理", "行业影响力"],
              "status": "future"
            }
          ]
        }
      },
      {
        "key": "action_plan",
        "title": "行动计划",
        "content": {
          "short_term_plan": {
            "period": "1-6 个月",
            "goals": [
              {
                "id": "a_001",
                "title": "完成 Figma 高级教程学习",
                "type": "learning",
                "priority": "high",
                "deadline": "2026-06-30",
                "status": "pending"
              },
              {
                "id": "a_002",
                "title": "参与 2 个实际设计项目",
                "type": "practice",
                "priority": "high",
                "deadline": "2026-09-30",
                "status": "pending"
              }
            ]
          },
          "medium_term_plan": {
            "period": "6-12 个月",
            "goals": [
              {
                "id": "a_003",
                "title": "考取 Adobe 认证专家证书",
                "type": "certificate",
                "priority": "medium",
                "deadline": "2026-12-31",
                "status": "pending"
              }
            ]
          },
          "evaluation_cycle": {
            "type": "quarterly",
            "metrics": ["技能提升", "项目数量", "作品集质量"]
          }
        }
      }
    ],
    "ai_suggestions": "建议你重点关注实战经验的积累，通过参与实际项目提升设计能力。同时，考取权威证书可以增强简历竞争力。保持学习热情，你的发展前景广阔。",
    "is_editable": true,
    "generated_at": "2026-03-30T10:30:00+08:00",
    "updated_at": "2026-03-30T10:32:00+08:00"
  }
}
```

---

### 9.3.1 重新生成报告

- **请求方法**: `POST`
- **请求路径**: `/api/reports/{id}/regenerate`
- **鉴权**: 需要
- **请求体**（可选覆盖参数，不传则沿用原报告生成参数/默认策略）:
```json
{
  "target_job_profile_id": 1001,
  "career_preference": {
    "preferred_city": "深圳",
    "expected_salary": "15-25k",
    "career_direction": "技术路线"
  }
}
```
- **响应示例（异步）**:
```json
{
  "code": 200,
  "msg": "报告生成中，请稍后查看",
  "data": {
    "report_id": "rpt_20260330_002",
    "report_no": "CR202603300002",
    "status": "PROCESSING",
    "estimated_time": 60
  }
}
```

**说明**：
- 与 `POST /api/reports/generate` 一样为异步过程，前端轮询 `GET /api/reports/{id}`
- 推荐用于“重新生成/优化生成”入口

---

### 9.4 更新报告内容

- **请求方法**: `PUT`
- **请求路径**: `/api/reports/{id}`
- **鉴权**: 需要
- **请求体**:
```json
{
  "career_goal": {
    "short_term": {
      "period": "1-2 年",
      "goal": "初级 UI 设计师",
      "description": "自定义描述..."
    }
  },
  "action_plan": {
    "short_term_plan": {
      "goals": [
        {
          "id": "a_001",
          "title": "自定义目标",
          "type": "learning",
          "priority": "high",
          "deadline": "2026-06-30",
          "status": "pending"
        }
      ]
    }
  }
}
```
- **响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "updated": true,
    "updated_at": "2026-03-30T11:00:00+08:00"
  }
}
```

**说明**：
- 支持部分更新，仅传递需要修改的字段
- 可修改 `target_job`、`development_path`、`action_plan` 等内容
- `self_discovery` 和 `match_analysis` 基于 AI 分析结果，不支持修改
- 当 `is_editable=false` 时，推荐返回 `403` 或 `409`

---

### 9.5 下载职业报告 PDF

- **请求方法**: `GET`
- **请求路径**: `/api/reports/{id}/download`
- **鉴权**: 需要
- **响应**:
  - `200` 返回 `application/pdf`（二进制流）
  - 响应头包含 `Content-Disposition: attachment; filename="职业生涯发展报告.pdf"`

---

### 9.6 删除报告

- **请求方法**: `DELETE`
- **请求路径**: `/api/reports/{id}`
- **鉴权**: 需要
- **响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "deleted": true
  }
}
```

---

## 数据流转说明

```
用户请求生成报告
    ↓
POST /api/reports/generate
    ↓
Reports-AI 服务：
  1. 获取学生能力画像
  2. 获取目标岗位画像
  3. 进行人岗匹配分析（4 个维度）
  4. 生成职业目标和路径规划
  5. 制定行动计划
    ↓
创建 career_reports 记录
    ↓
前端轮询 GET /api/reports/{id} 直到状态为 COMPLETED
    ↓
用户查看/编辑报告
    ↓
可选：下载 PDF / 删除报告
```

---

## 接口列表汇总

| 接口编号 | 接口名 | 方法 | 路径 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| 9.1 | 生成职业报告 | POST | /api/reports/generate | 异步生成职业生涯发展报告 |
| 9.2.1 | 获取报告列表 | GET | /api/reports | 获取用户历史报告列表（默认最近 20 条） |
| 9.2 | 获取最新报告 | GET | /api/reports/latest | 获取用户最新报告概览 |
| 9.3 | 获取报告详情 | GET | /api/reports/{id} | 获取报告完整内容（含 5 个模块） |
| 9.4 | 更新报告内容 | PUT | /api/reports/{id} | 编辑报告内容（支持手动调整） |
| 9.3.1 | 重新生成报告 | POST | /api/reports/{id}/regenerate | 基于既有报告参数重新生成（异步） |
| 9.5 | 下载职业报告 PDF | GET | /api/reports/{id}/download | 下载 PDF 格式报告 |
| 9.6 | 删除报告 | DELETE | /api/reports/{id} | 删除报告记录 |

---

## 报告模块功能要求对照

| 大赛要求 | 对应功能 | 接口 |
|----------|----------|------|
| 职业探索与岗位匹配 | 人岗匹配度分析，量化呈现契合度与差距 | 9.1 生成、9.3 查询 |
| 职业目标设定与路径规划 | 职业目标、发展路径、行业趋势分析 | 9.1 生成、9.3 查询 |
| 行动计划与成果展示 | 分阶段成长计划、评估周期 | 9.1 生成、9.3 查询、9.4 更新 |
| 编辑优化与导出 | 智能润色、内容编辑、PDF 导出 | 9.4 更新、9.5 下载 |
