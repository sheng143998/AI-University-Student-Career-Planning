# 职引AI - Goals 模块接口文档

## 模块概述
目标管理相关接口，包括主目标、里程碑、成功准则和并行目标的管理。

---

## 接口列表

| 接口名 | 方法 | 路径 | 说明 |
| :--- | :--- | :--- | :--- |
| 获取目标总览 | GET | `/api/goals/overview` | 含主目标/里程碑/成功准则/并行目标 |
| 创建目标 | POST | `/api/goals` | 新增目标 |
| 获取目标详情 | GET | `/api/goals/{id}` | 含里程碑/成功准则/AI 建议 |
| 更新目标 | PUT | `/api/goals/{id}` | 更新字段 |
| 删除目标 | DELETE | `/api/goals/{id}` | 删除 |
| 创建里程碑 | POST | `/api/goals/{id}/milestones` | 里程碑 |
| 更新里程碑 | PATCH | `/api/goals/{id}/milestones/{ms_id}` | 勾选/改名/排序 |

---

## 详细接口定义

### 获取目标总览
- **请求方法**: `GET`
- **请求路径**: `/api/goals/overview`
- **鉴权**: 需要（JWT Token）
- **响应示例**:
```json
{
  "code": 1,
  "msg": null,
  "data": {
    "primaryGoal": {
      "id": "1",
      "title": "成为高级前端工程师",
      "desc": "通过工程化与性能体系建设拿到更高级别岗位",
      "status": "IN_PROGRESS",
      "progress": 65,
      "eta": "2025年12月",
      "isPrimary": true
    },
    "milestones": [
      { "id": "1", "goalId": "1", "title": "完善项目作品集", "desc": "整理3个代表性项目", "status": "DONE", "progress": 100, "order": 1 },
      { "id": "2", "goalId": "1", "title": "系统学习性能优化", "desc": "完成性能优化课程", "status": "IN_PROGRESS", "progress": 50, "order": 2 }
    ],
    "milestonesCompleted": 1,
    "milestonesTotal": 2,
    "successCriteria": {
      "salary": "¥30k - ¥45k / 月",
      "companies": ["腾讯", "字节跳动", "阿里巴巴"],
      "cities": ["北京", "上海", "深圳"]
    },
    "longTermAspirations": [
      { "title": "技术专家", "desc": "成为某领域技术专家" },
      { "title": "团队管理", "desc": "带领10人以上团队" }
    ],
    "aiAdvice": {
      "content": "建议将目标拆成 4 周冲刺：工程化、性能、项目表达、面试题体系。"
    },
    "parallelGoals": [
      { "id": "2", "title": "补齐算法基础", "desc": "系统学习算法", "status": "IN_PROGRESS", "progress": 20, "eta": "2025年6月", "isPrimary": false }
    ]
  }
}
```

---

### 创建目标
- **请求方法**: `POST`
- **请求路径**: `/api/goals`
- **鉴权**: 需要
- **请求体**:
```json
{
  "title": "完成 React 认证",
  "desc": "通过官方认证",
  "status": "TODO",
  "progress": 0,
  "eta": "2025年8月",
  "isPrimary": false
}
```
- **响应示例**:
```json
{ "code": 1, "msg": null, "data": { "id": "3" } }
```

---

### 获取目标详情
- **请求方法**: `GET`
- **请求路径**: `/api/goals/{id}`
- **鉴权**: 需要
- **响应示例**:
```json
{
  "code": 1,
  "msg": null,
  "data": {
    "goal": {
      "id": "1",
      "title": "成为高级前端工程师",
      "desc": "通过工程化与性能体系建设拿到更高级别岗位",
      "status": "IN_PROGRESS",
      "progress": 65,
      "eta": "2025年12月",
      "isPrimary": true
    },
    "milestones": [
      { "id": "1", "goalId": "1", "title": "完善项目作品集", "desc": "整理3个代表性项目", "status": "DONE", "progress": 100, "order": 1 }
    ],
    "successCriteria": {
      "salary": "¥30k - ¥45k / 月",
      "companies": ["腾讯", "字节跳动"],
      "cities": ["北京", "上海"]
    },
    "longTermAspirations": [
      { "title": "技术专家", "desc": "成为某领域技术专家" }
    ],
    "aiAdvice": {
      "content": "建议将目标拆成 4 周冲刺..."
    }
  }
}
```

---

### 更新目标
- **请求方法**: `PUT`
- **请求路径**: `/api/goals/{id}`
- **鉴权**: 需要
- **请求体**:
```json
{
  "title": "成为高级前端工程师",
  "desc": "通过工程化与性能体系建设拿到更高级别岗位",
  "status": "IN_PROGRESS",
  "progress": 70,
  "eta": "2025年11月",
  "isPrimary": true,
  "successCriteria": {
    "salary": "¥35k - ¥50k / 月",
    "companies": ["腾讯", "字节跳动", "阿里巴巴", "美团"],
    "cities": ["北京", "上海", "深圳", "杭州"]
  },
  "longTermAspirations": [
    { "title": "技术专家", "desc": "成为某领域技术专家" },
    { "title": "创业", "desc": "创办自己的公司" }
  ],
  "aiAdvice": {
    "content": "更新后的AI建议..."
  }
}
```
- **响应示例**:
```json
{ "code": 1, "msg": null, "data": { "updated": true } }
```

---

### 删除目标
- **请求方法**: `DELETE`
- **请求路径**: `/api/goals/{id}`
- **鉴权**: 需要
- **响应示例**:
```json
{ "code": 1, "msg": null, "data": { "deleted": true } }
```

---

### 创建里程碑
- **请求方法**: `POST`
- **请求路径**: `/api/goals/{id}/milestones`
- **鉴权**: 需要
- **请求体**:
```json
{
  "title": "完成 1 次性能优化复盘",
  "desc": "包含数据对比和优化方案",
  "status": "TODO",
  "progress": 0,
  "order": 3
}
```
- **响应示例**:
```json
{ "code": 1, "msg": null, "data": { "id": "10" } }
```

---

### 更新里程碑
- **请求方法**: `PATCH`
- **请求路径**: `/api/goals/{id}/milestones/{ms_id}`
- **鉴权**: 需要
- **请求体（任选字段）**:
```json
{
  "title": "完成 1 次性能优化复盘（含数据对比）",
  "desc": "已完成",
  "status": "DONE",
  "progress": 100,
  "order": 2
}
```
- **响应示例**:
```json
{ "code": 1, "msg": null, "data": { "updated": true } }
```

---

## 数据结构定义

### GoalSummary
| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| id | string | 目标ID |
| title | string | 目标标题 |
| desc | string | 目标描述 |
| status | string | 状态：TODO/IN_PROGRESS/DONE |
| progress | number | 进度 0-100 |
| eta | string | 预计达成时间 |
| isPrimary | boolean | 是否为主目标 |

### Milestone
| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| id | string | 里程碑ID |
| goalId | string | 所属目标ID |
| title | string | 里程碑标题 |
| desc | string | 里程碑描述 |
| status | string | 状态：TODO/IN_PROGRESS/DONE |
| progress | number | 进度 0-100 |
| order | number | 排序顺序 |

### SuccessCriteria
| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| salary | string | 薪资预期 |
| companies | string[] | 目标公司列表 |
| cities | string[] | 目标城市列表 |

### LongTermAspiration
| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| title | string | 愿景标题 |
| desc | string | 愿景描述 |

### AiAdvice
| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| content | string | AI建议内容 |

## 建表语句

```sql
DROP TABLE IF EXISTS ai_career_plan.goal_milestone CASCADE;
DROP TABLE IF EXISTS ai_career_plan.goal CASCADE;
-- =============================================
-- goal 表（目标表）
-- =============================================
CREATE TABLE ai_career_plan.goal (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    goal_desc TEXT,
    status VARCHAR(50) DEFAULT 'TODO',              -- TODO / IN_PROGRESS / DONE
    progress INTEGER DEFAULT 0,                      -- 0-100
    eta VARCHAR(100),                                -- 预计达成时间
    is_primary BOOLEAN DEFAULT FALSE,                -- 是否为主目标
    success_salary VARCHAR(100),                     -- 成功准则-薪资
    success_companies TEXT,                          -- 成功准则-目标公司(JSON数组)
    success_cities TEXT,                             -- 成功准则-目标城市(JSON数组)
    long_term_aspirations TEXT,                      -- 长期愿景(JSON数组)
    ai_advice TEXT,                                  -- AI建议
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- goal_milestone 表（里程碑表）
-- =============================================
CREATE TABLE ai_career_plan.goal_milestone (
    id BIGSERIAL PRIMARY KEY,
    goal_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    milestone_desc TEXT,
    status VARCHAR(50) DEFAULT 'TODO',              -- TODO / IN_PROGRESS / DONE
    progress INTEGER DEFAULT 0,                      -- 0-100
    sort_order INTEGER DEFAULT 1,                    -- 排序顺序
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_goal_user_id ON ai_career_plan.goal(user_id);
CREATE INDEX idx_goal_is_primary ON ai_career_plan.goal(is_primary);
CREATE INDEX idx_goal_milestone_goal_id ON ai_career_plan.goal_milestone(goal_id);
CREATE INDEX idx_goal_milestone_user_id ON ai_career_plan.goal_milestone(user_id);
-- goal 表注释
COMMENT ON TABLE ai_career_plan.goal IS '目标表';
COMMENT ON COLUMN ai_career_plan.goal.id IS '主键';
COMMENT ON COLUMN ai_career_plan.goal.user_id IS '用户ID';
COMMENT ON COLUMN ai_career_plan.goal.title IS '目标标题';
COMMENT ON COLUMN ai_career_plan.goal.goal_desc IS '目标描述';
COMMENT ON COLUMN ai_career_plan.goal.status IS '状态：TODO/IN_PROGRESS/DONE';
COMMENT ON COLUMN ai_career_plan.goal.progress IS '进度：0-100';
COMMENT ON COLUMN ai_career_plan.goal.eta IS '预计达成时间';
COMMENT ON COLUMN ai_career_plan.goal.is_primary IS '是否为主目标';
COMMENT ON COLUMN ai_career_plan.goal.success_salary IS '成功准则-薪资预期';
COMMENT ON COLUMN ai_career_plan.goal.success_companies IS '成功准则-目标公司(JSON数组)';
COMMENT ON COLUMN ai_career_plan.goal.success_cities IS '成功准则-目标城市(JSON数组)';
COMMENT ON COLUMN ai_career_plan.goal.long_term_aspirations IS '长期愿景(JSON数组)';
COMMENT ON COLUMN ai_career_plan.goal.ai_advice IS 'AI建议';

-- goal_milestone 表注释
COMMENT ON TABLE ai_career_plan.goal_milestone IS '里程碑表';
COMMENT ON COLUMN ai_career_plan.goal_milestone.id IS '主键';
COMMENT ON COLUMN ai_career_plan.goal_milestone.goal_id IS '关联目标ID';
COMMENT ON COLUMN ai_career_plan.goal_milestone.user_id IS '用户ID';
COMMENT ON COLUMN ai_career_plan.goal_milestone.title IS '里程碑标题';
COMMENT ON COLUMN ai_career_plan.goal_milestone.milestone_desc IS '里程碑描述';
COMMENT ON COLUMN ai_career_plan.goal_milestone.status IS '状态：TODO/IN_PROGRESS/DONE';
COMMENT ON COLUMN ai_career_plan.goal_milestone.progress IS '进度：0-100';
COMMENT ON COLUMN ai_career_plan.goal_milestone.sort_order IS '排序顺序';
```

