# 职引AI - Reports 模块接口文档

## 模块概述
职业发展报告相关接口，包括报告生成、获取、详情查看和 PDF 下载。

---

## 接口列表

| 接口名 | 方法 | 路径 | 说明 |
| :--- | :--- | :--- | :--- |
| 生成职业报告 | POST | `/api/reports/generate` | 异步/同步均可 |
| 获取最新报告 | GET | `/api/reports/latest` | 报告页默认 |
| 获取报告详情 | GET | `/api/reports/{id}` | 按 id 获取 |
| 下载报告 PDF | GET | `/api/reports/{id}/download` | 二进制流 |

---

## 详细接口定义

### 生成职业报告
- **请求方法**: `POST`
- **请求路径**: `/api/reports/generate`
- **鉴权**: 需要
- **请求体**:
```json
{
  "source": {
    "resume_analysis_id": "res_123456"
  }
}
```
- **响应示例（异步）**:
```json
{
  "code": 200,
  "data": {
    "report_id": "rpt_20260326_001",
    "status": "PROCESSING"
  }
}
```

---

### 获取最新报告
- **请求方法**: `GET`
- **请求路径**: `/api/reports/latest`
- **鉴权**: 需要
- **响应示例**:
```json
{
  "code": 200,
  "data": {
    "id": "rpt_001",
    "title": "职业生涯发展报告",
    "status": "COMPLETED",
    "generated_at": "2026-03-26T09:30:00+08:00"
  }
}
```

---

### 获取报告详情
- **请求方法**: `GET`
- **请求路径**: `/api/reports/{id}`
- **鉴权**: 需要
- **响应示例（精简，章节建议用数组表达以便前端渲染）**:
```json
{
  "code": 200,
  "data": {
    "id": "rpt_001",
    "title": "职业生涯发展报告",
    "status": "COMPLETED",
    "generated_at": "2026-03-26T09:30:00+08:00",
    "sections": [
      { "key": "self_discovery", "title": "自我认知", "content": "..." },
      { "key": "match_analysis", "title": "人岗匹配", "content": "..." },
      { "key": "roadmap", "title": "发展路线", "content": "..." },
      { "key": "action_plan", "title": "行动计划", "content": "..." }
    ],
    "action_items": [
      { "title": "补齐工程化体系", "desc": "CI/CD、Monorepo、性能预算" }
    ]
  }
}
```

---

### 下载职业报告 PDF
- **请求方法**: `GET`
- **请求路径**: `/api/reports/{id}/download`
- **鉴权**: 需要
- **响应**:
  - `200` 返回 `application/pdf`（二进制流）