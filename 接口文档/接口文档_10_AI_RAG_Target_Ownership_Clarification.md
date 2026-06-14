# Interface Document 10 AI/RAG Target Ownership Clarification

## Change Summary

- Date: 2026-06-08
- Scope: `/api/feedback/ai-rag`
- Purpose: Clarify `target_type + target_id` ownership rules for AI/RAG feedback before Java forwards sanitized events to Python.

## Target ID Rules

| target_type | target_id format | Java ownership check |
| --- | --- | --- |
| `CHAT_MESSAGE` | Numeric chat message id | `chat_message.user_id == current user` |
| `RESUME_ANALYSIS` | Numeric resume analysis id | `resume_analysis_result.user_id == current user` |
| `JOB_MATCH` | Numeric job id | Job exists in `job`; Java sends only sanitized feedback metadata |
| `MARKET_INSIGHT` | Numeric job id | Job exists in `job`; Java sends only sanitized feedback metadata |
| `REPORT` | Numeric report id | `career_reports.user_id == current user` |
| `ROADMAP` | Numeric `user_roadmap_steps.id` | `user_roadmap_steps.user_id == current user` |
| `GOAL_ADVICE` | Numeric goal id | `goal.user_id == current user` |
| `NOTIFICATION_AI_ADVICE` | `sourceType:sourceId`, for example `GOAL_ADVICE:12`, `REPORT:88`, or `CHAT_MESSAGE:1001` | Java validates ownership of the referenced source and rejects recursive `NOTIFICATION_AI_ADVICE:*` references |

## Java To Python Boundary

Java must validate the target ownership first. The Python `/internal/rag/feedback` endpoint receives only:

- `request_id`
- `user_id`
- target type/id/page
- rating and reason tags
- `retrieval_trace_id`
- evidence reference ids
- user action

Java must not forward full resume text, full JD text, full prompts, or other private source content to Python for feedback handling.

## Verification

Run these checks when this contract changes:

```powershell
$env:PYTHONPATH='ai-service'; python -B -m pytest ai-service/tests/test_feedback_service.py -q -p no:cacheprovider
mvn -pl server -am -DskipTests compile
```
