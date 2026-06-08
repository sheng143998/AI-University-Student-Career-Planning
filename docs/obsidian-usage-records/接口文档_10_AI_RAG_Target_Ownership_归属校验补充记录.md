# 接口文档 10 AI/RAG Target Ownership 归属校验补充记录

## 使用时间

2026-06-08

## 使用的笔记知识点

- [[RAG相关/Pre-Retrieval预检索/索引优化/元数据过滤-痛点分析]]
- [[RAG相关/RAG应用效果评估/RAG检索评估]]
- [[RAG相关/RAG应用效果评估/RAG响应评估]]
- [[RAG相关/RAG应用效果评估/RAG评估指标总结]]

## 应用到的项目位置

- 接口文档：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning\接口文档\接口文档_10_AI_RAG_Target_Ownership_Clarification.md`
- Java 服务：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning\server\src\main\java\com\itsheng\service\service\Impl\AiRagFeedbackServiceImpl.java`
- Java Mapper：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning\server\src\main\java\com\itsheng\service\mapper\UserRoadmapStepsMapper.java`
- MyBatis XML：`C:\Users\WhenJayHe\IdeaProjects\AI-University-Student-Career-Planning\server\src\main\resources\mapper\UserRoadmapStepsMapper.xml`

## 知识点如何影响设计

- 元数据过滤思想用于反馈目标归属校验：Java 必须先确认 `target_type + target_id` 属于当前用户或是可公开反馈的岗位目标，再把脱敏事件发给 Python。
- RAG 检索/响应评估要求反馈事件能归因到具体 trace 和 evidence ref，因此 `NOTIFICATION_AI_ADVICE` 采用 `sourceType:sourceId`，通过源对象归属校验来避免通知 ID 缺少持久化表时无法追踪。
- 反馈闭环只传 ID、标签、trace 和 evidence refs，不传完整简历、JD 或 prompt，降低隐私泄露风险。
