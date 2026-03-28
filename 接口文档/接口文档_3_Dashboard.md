# 职引 AI - Dashboard 模块接口文档

---

## 数据模型

本模块**不需要新建表**，所有数据从现有的 `users` 表、`resume_analysis_result` 表以及 AI 分析计算结果中获取。

### 数据来源说明

| 数据模块 | 数据来源 | 说明 |
|----------|----------|------|
| match_summary | `resume_analysis_result.parsed_data.match_score` + AI 生成描述 | 匹配度分数及文字描述 |
| market_trends | 外部就业市场数据 API / 内部统计 | 专业/岗位趋势数据 |
| skill_radar | `resume_analysis_result.parsed_data.skills` + AI 评估 | 四维能力雷达图 |
| actions | AI 根据差距分析生成 | 个性化行动建议 |
| roadmap | AI 职业规划算法 | 职业发展路径 |

#### skill_radar 字段结构说明

```json
{
  "technical": 80,        // 技术能力 (0-100)
  "innovation": 90,       // 创新能力 (0-100)
  "resilience": 70,       // 抗压能力 (0-100)
  "communication": 85     // 沟通能力 (0-100)
}
```

#### actions 字段结构说明

```json
[
  {
    "id": "a_001",
    "title": "完成 React 认证",
    "desc": "弥补关键技术差距",
    "icon": "school",
    "link": "/goals"
  }
]
```

#### roadmap steps 字段结构说明

```json
[
  {
    "title": "初级 UI/视觉设计师",
    "time": "目标：第 1-2 年",
    "status": "85% 匹配",
    "icon": "person",
    "active": true
  }
]
```

---

## 3.1 获取仪表盘汇总

### 3.1.1 基本信息

请求路径：/api/dashboard/summary

请求方式：GET

接口描述：该接口用于获取当前登录用户的仪表盘汇总信息，包括匹配度摘要、市场趋势、能力雷达图和行动建议。数据从 `resume_analysis_result` 表的 `parsed_data` 字段、外部就业市场数据及 AI 分析结果中提取。

---

### 3.1.2 请求参数

参数说明：

本接口无需请求参数。

---

### 3.1.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
| :--- | :--- | :--- | :--- |
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据 |
| &#124;- match_summary | object | 非必须 | 匹配度摘要 |
| &#124;- &#124;- score | number | 非必须 | 匹配分数 (0-100) |
| &#124;- &#124;- description | string | 非必须 | AI 生成的匹配描述文字 |
| &#124;- &#124;- tags | string[] | 非必须 | 亮点标签列表 |
| &#124;- market_trends | object[] | 非必须 | 市场趋势列表 |
| &#124;- &#124;- name | string | 非必须 | 专业/岗位名称 |
| &#124;- &#124;- growth | number | 非必须 | 增长率 (小数格式，如 0.14 表示 14%) |
| &#124;- &#124;- value | number | 非必须 | 热度值 (0-100) |
| &#124;- skill_radar | object | 非必须 | 能力雷达图数据 |
| &#124;- &#124;- technical | number | 非必须 | 技术能力得分 (0-100) |
| &#124;- &#124;- innovation | number | 非必须 | 创新能力得分 (0-100) |
| &#124;- &#124;- resilience | number | 非必须 | 抗压能力得分 (0-100) |
| &#124;- &#124;- communication | number | 非必须 | 沟通能力得分 (0-100) |
| &#124;- actions | object[] | 非必须 | 行动建议列表 |
| &#124;- &#124;- id | string | 非必须 | 行动项 ID |
| &#124;- &#124;- title | string | 非必须 | 行动标题 |
| &#124;- &#124;- desc | string | 非必须 | 行动描述 |
| &#124;- &#124;- icon | string | 非必须 | 图标名称 |
| &#124;- &#124;- link | string | 非必须 | 跳转链接 |

响应数据样例：
```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "match_summary": {
      "score": 85,
      "description": "您与目标岗位匹配度很高，创意 UI 和用户共情是您的突出优势",
      "tags": ["创意 UI", "用户共情"]
    },
    "market_trends": [
      {
        "name": "计算机科学",
        "growth": 0.14,
        "value": 88
      }
    ],
    "skill_radar": {
      "technical": 80,
      "innovation": 90,
      "resilience": 70,
      "communication": 85
    },
    "actions": [
      {
        "id": "a_001",
        "title": "完成 React 认证",
        "desc": "弥补关键技术差距",
        "icon": "school",
        "link": "/goals"
      }
    ]
  }
}
```

---

### 3.1.4 错误响应

| HTTP 状态码 | 场景 | JSON 响应样例 |
| :--- | :--- | :--- |
| `401` | 未登录或 token 失效 | `{"code": 401, "msg": "未登录"}` |

---

## 3.2 获取仪表盘进化路线

### 3.2.1 基本信息

请求路径：/api/dashboard/roadmap

请求方式：GET

接口描述：该接口用于获取当前登录用户的职业发展路径信息，即首页"职业进化地图"卡片数据。数据由 AI 职业规划算法根据用户简历和目标岗位生成。

---

### 3.2.2 请求参数

参数说明：

本接口无需请求参数。

---

### 3.2.3 响应数据

参数格式：application/json

参数说明：

| 参数名 | 类型 | 是否必须 | 备注 |
| :--- | :--- | :--- | :--- |
| code | number | 必须 | 响应码，1 代表成功，0 代表失败 |
| msg | string | 非必须 | 提示信息 |
| data | object | 非必须 | 返回的数据 |
| &#124;- current_step_index | number | 非必须 | 当前所在阶段索引 (从 0 开始) |
| &#124;- steps | object[] | 非必须 | 职业发展阶段列表 |
| &#124;- &#124;- title | string | 非必须 | 阶段岗位名称 |
| &#124;- &#124;- time | string | 非必须 | 时间目标描述 |
| &#124;- &#124;- status | string | 非必须 | 匹配状态文字 |
| &#124;- &#124;- icon | string | 非必须 | 图标名称 |
| &#124;- &#124;- active | boolean | 非必须 | 是否当前阶段 |

响应数据样例：
```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "current_step_index": 0,
    "steps": [
      {
        "title": "初级 UI/视觉设计师",
        "time": "目标：第 1-2 年",
        "status": "85% 匹配",
        "icon": "person",
        "active": true
      },
      {
        "title": "高级 UI 设计师",
        "time": "目标：第 3-5 年",
        "status": "待解锁",
        "icon": "star",
        "active": false
      }
    ]
  }
}
```

---

### 3.2.4 错误响应

| HTTP 状态码 | 场景 | JSON 响应样例 |
| :--- | :--- | :--- |
| `401` | 未登录或 token 失效 | `{"code": 401, "msg": "未登录"}` |

---

## 数据流转说明

```
用户完成简历解析
    ↓
resume_analysis_result 记录生成
    ↓
AI 异步计算匹配度、雷达图、行动建议、进化路线
    ↓
GET /api/dashboard/summary 或 /api/dashboard/roadmap
    ↓
返回汇总数据供前端仪表盘展示
```

---

## 接口列表汇总

| 接口编号 | 接口名 | 方法 | 路径 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| 3.1 | 获取仪表盘汇总 | GET | /api/dashboard/summary | 匹配摘要、趋势、雷达、行动项 |
| 3.2 | 获取仪表盘进化路线 | GET | /api/dashboard/roadmap | 首页"职业进化地图"卡片数据 |
