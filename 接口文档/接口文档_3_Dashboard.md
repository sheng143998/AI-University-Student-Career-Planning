# 职引AI - Dashboard 模块接口文档

## 模块概述
职业仪表盘相关接口，提供匹配摘要、趋势、雷达图和行动建议等汇总数据。

---

## 接口列表

| 接口名 | 方法 | 路径 | 说明 |
| :--- | :--- | :--- | :--- |
| 获取仪表盘汇总 | GET | `/api/dashboard/summary` | 匹配摘要、趋势、雷达、行动项 |
| 获取仪表盘进化路线 | GET | `/api/dashboard/roadmap` | 首页"职业进化地图"卡片数据 |

---

## 详细接口定义

### 获取仪表盘汇总
- **请求方法**: `GET`
- **请求路径**: `/api/dashboard/summary`
- **鉴权**: 需要
- **响应示例**:
```json
{
  "code": 200,
  "data": {
    "match_summary": {
      "score": 85,
      "description": "您与目标岗位匹配度很高...",
      "tags": ["创意 UI", "用户共情"]
    },
    "market_trends": [
      { "name": "计算机科学", "growth": 0.14, "value": 88 }
    ],
    "skill_radar": {
      "technical": 80,
      "innovation": 90,
      "resilience": 70,
      "communication": 85
    },
    "actions": [
      { "id": "a_001", "title": "完成 React 认证", "desc": "弥补关键技术差距", "icon": "school", "link": "/goals" }
    ]
  }
}
```

---

### 获取仪表盘进化路线
- **请求方法**: `GET`
- **请求路径**: `/api/dashboard/roadmap`
- **鉴权**: 需要
- **响应示例**:
```json
{
  "code": 200,
  "data": {
    "current_step_index": 0,
    "steps": [
      { "title": "初级 UI/视觉设计师", "time": "目标：第 1-2 年", "status": "85% 匹配", "icon": "person", "active": true }
    ]
  }
}
```