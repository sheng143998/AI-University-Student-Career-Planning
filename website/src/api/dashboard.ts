import { apiBase, headersAuth, headersJson, parseApiResponse } from '@/api/client'
import type { ApiResult } from '@/api/types'

export interface JobProfile {
  id?: number
  name?: string
  industry?: string
  city?: string
  min_experience_years?: number
  max_experience_years?: number
  salary_range_min?: number
  salary_range_max?: number
  description?: string
  skills?: Array<{ name?: string; level?: number; category?: string }>
  capability_weights?: Record<string, number>
}

export interface MatchSummary {
  score?: number
  description?: string
  tags?: string[]
  dimension_scores?: Record<string, number>
}

export interface MarketTrend {
  name?: string
  growth?: number
  value?: number
  source?: string
}

export interface SkillRadar {
  technical?: number
  innovation?: number
  resilience?: number
  communication?: number
  learning?: number
  internship?: number
}

export interface DashboardAction {
  id?: string
  title?: string
  desc?: string
  icon?: string
  link?: string
  priority?: 'high' | 'medium' | 'low' | string
}

export interface DashboardSummary {
  job_profile?: JobProfile
  match_summary?: MatchSummary
  market_trends?: MarketTrend[]
  skill_radar?: SkillRadar
  actions?: DashboardAction[]
}

export interface DashboardRoadmapStep {
  job_id?: number
  title?: string
  level?: string
  level_name?: string
  time?: string
  status?: string
  icon?: string
  active?: boolean
}

export interface DashboardRoadmap {
  current_step_index?: number
  target_job_id?: number
  target_job_name?: string
  steps?: DashboardRoadmapStep[]
}

export interface UpdateCurrentStepBody {
  current_step_index: number
}

export async function getDashboardSummary(jobProfileId?: number): Promise<ApiResult<DashboardSummary>> {
  const qs = new URLSearchParams()
  if (typeof jobProfileId === 'number') qs.set('job_profile_id', String(jobProfileId))

  const url = qs.toString() ? `${apiBase()}/api/dashboard/summary?${qs.toString()}` : `${apiBase()}/api/dashboard/summary`
  const res = await fetch(url, {
    method: 'GET',
    headers: headersAuth(),
    credentials: 'include',
  })
  return parseApiResponse<DashboardSummary>(res)
}

export async function getDashboardRoadmap(): Promise<ApiResult<DashboardRoadmap>> {
  const res = await fetch(`${apiBase()}/api/dashboard/roadmap`, {
    method: 'GET',
    headers: headersAuth(),
    credentials: 'include',
  })
  return parseApiResponse<DashboardRoadmap>(res)
}

export async function updateDashboardCurrentStep(body: UpdateCurrentStepBody): Promise<ApiResult<DashboardRoadmapStep[]>> {
  const res = await fetch(`${apiBase()}/api/dashboard/roadmap/current-step`, {
    method: 'PUT',
    headers: headersJson(),
    credentials: 'include',
    body: JSON.stringify(body),
  })
  return parseApiResponse<DashboardRoadmapStep[]>(res)
}
