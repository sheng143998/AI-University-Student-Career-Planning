import { headersJson, parseApiResponse } from '@/api/client'
import type { ApiResult } from '@/api/types'

export type AiRagTargetType =
  | 'CHAT_MESSAGE'
  | 'RESUME_ANALYSIS'
  | 'JOB_MATCH'
  | 'MARKET_INSIGHT'
  | 'REPORT'
  | 'ROADMAP'
  | 'GOAL_ADVICE'
  | 'NOTIFICATION_AI_ADVICE'

export type AiRagRating = -1 | 0 | 1

export interface AiRagFeedbackBody {
  target_type: AiRagTargetType
  target_id: string
  rating: AiRagRating
  reason_tags?: string[]
  comment?: string
  retrieval_trace_id?: string
  evidence_ref_ids?: string[]
  page?: string
  user_action?: 'thumb_up' | 'thumb_down' | 'dismiss' | 'save' | 'click_evidence' | string
}

export interface AiRagFeedbackResult {
  feedback_id: string
  accepted: boolean
  used_for?: string[]
  quality_dimensions?: Record<string, unknown>
  diagnostics?: Record<string, unknown>
}

export interface AiRagSettings {
  enable_ai_advice_notifications: boolean
  enable_rag_personalization: boolean
  preferred_city?: string
  preferred_industries: string[]
  preferred_job_levels: string[]
  career_direction?: string
  result_language: 'zh-CN' | 'en-US'
  feedback_usage_scope: 'local_eval_only' | 'personalization' | 'disabled'
}

export interface AiRagSettingsUpdateResult extends AiRagSettings {
  updated: boolean
  effective_filters?: Record<string, unknown>
  diagnostics?: Record<string, unknown>
}

export async function submitAiRagFeedback(body: AiRagFeedbackBody): Promise<ApiResult<AiRagFeedbackResult>> {
  const res = await fetch('/api/feedback/ai-rag', {
    method: 'POST',
    headers: headersJson(),
    body: JSON.stringify(body),
  })
  return parseApiResponse<AiRagFeedbackResult>(res)
}

export async function getAiRagSettings(): Promise<ApiResult<AiRagSettings>> {
  const res = await fetch('/api/settings/ai-rag', {
    method: 'GET',
    headers: headersJson(),
  })
  return parseApiResponse<AiRagSettings>(res)
}

export async function updateAiRagSettings(body: AiRagSettings): Promise<ApiResult<AiRagSettingsUpdateResult>> {
  const res = await fetch('/api/settings/ai-rag', {
    method: 'PUT',
    headers: headersJson(),
    body: JSON.stringify(body),
  })
  return parseApiResponse<AiRagSettingsUpdateResult>(res)
}
