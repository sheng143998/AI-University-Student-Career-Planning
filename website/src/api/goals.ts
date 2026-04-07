/**
 * Goals 模块 — 对齐 接口文档_7_Goals.md
 */

import { apiBase, headersAuth, headersJson, isApiSuccess, parseApiResponse } from '@/api/client'
import type { ApiResult } from '@/api/types'

// ========== Types ==========

export interface GoalSummary {
  id: string
  title: string
  desc: string
  status: string
  progress: number
  eta: string
  isPrimary: boolean
}

export interface Milestone {
  id: string
  goalId: string
  title: string
  desc: string
  status: string
  progress: number
  order: number
}

export interface SuccessCriteria {
  salary: string
  companies: string[]
  cities: string[]
}

export interface LongTermAspiration {
  title: string
  desc: string
}

export interface AiAdvice {
  content: string
}

export interface GoalsOverview {
  primaryGoal: GoalSummary | null
  milestones: Milestone[]
  milestonesCompleted: number
  milestonesTotal: number
  successCriteria: SuccessCriteria
  longTermAspirations: LongTermAspiration[]
  aiAdvice: AiAdvice
  parallelGoals: GoalSummary[]
}

export interface GoalDetail {
  goal: GoalSummary
  milestones: Milestone[]
  successCriteria: SuccessCriteria
  longTermAspirations: LongTermAspiration[]
  aiAdvice: AiAdvice
}

export interface GoalCreateBody {
  title: string
  desc?: string
  status?: string
  progress?: number
  eta?: string
  isPrimary?: boolean
}

export interface GoalUpdateBody {
  title?: string
  desc?: string
  status?: string
  progress?: number
  eta?: string
  isPrimary?: boolean
  successCriteria?: SuccessCriteria
  longTermAspirations?: LongTermAspiration[]
  aiAdvice?: AiAdvice
}

export interface MilestoneCreateBody {
  title: string
  desc?: string
  status?: string
  progress?: number
  order?: number
}

export interface MilestoneUpdateBody {
  title?: string
  desc?: string
  status?: string
  progress?: number
  order?: number
}

export interface IdData {
  id: string
}

export interface UpdateResult {
  updated: boolean
}

export interface DeleteResult {
  deleted: boolean
}

// ========== API Functions ==========

/** 获取 Goals 页面总览 */
export async function getGoalsOverview(): Promise<ApiResult<GoalsOverview>> {
  const res = await fetch(`${apiBase()}/api/goals/overview`, {
    method: 'GET',
    headers: headersAuth(),
    credentials: 'include',
  })
  return parseApiResponse<GoalsOverview>(res)
}

/** 创建目标 */
export async function createGoal(body: GoalCreateBody): Promise<ApiResult<IdData>> {
  const res = await fetch(`${apiBase()}/api/goals`, {
    method: 'POST',
    headers: headersJson(),
    credentials: 'include',
    body: JSON.stringify(body),
  })
  return parseApiResponse<IdData>(res)
}

/** 获取目标详情 */
export async function getGoalDetail(goalId: string): Promise<ApiResult<GoalDetail>> {
  const res = await fetch(`${apiBase()}/api/goals/${goalId}`, {
    method: 'GET',
    headers: headersAuth(),
    credentials: 'include',
  })
  return parseApiResponse<GoalDetail>(res)
}

/** 更新目标 */
export async function updateGoal(goalId: string, body: GoalUpdateBody): Promise<ApiResult<UpdateResult>> {
  const res = await fetch(`${apiBase()}/api/goals/${goalId}`, {
    method: 'PUT',
    headers: headersJson(),
    credentials: 'include',
    body: JSON.stringify(body),
  })
  return parseApiResponse<UpdateResult>(res)
}

/** 删除目标 */
export async function deleteGoal(goalId: string): Promise<ApiResult<DeleteResult>> {
  const res = await fetch(`${apiBase()}/api/goals/${goalId}`, {
    method: 'DELETE',
    headers: headersAuth(),
    credentials: 'include',
  })
  return parseApiResponse<DeleteResult>(res)
}

/** 创建里程碑 */
export async function createMilestone(goalId: string, body: MilestoneCreateBody): Promise<ApiResult<IdData>> {
  const res = await fetch(`${apiBase()}/api/goals/${goalId}/milestones`, {
    method: 'POST',
    headers: headersJson(),
    credentials: 'include',
    body: JSON.stringify(body),
  })
  return parseApiResponse<IdData>(res)
}

/** 更新里程碑 */
export async function updateMilestone(
  goalId: string,
  milestoneId: string,
  body: MilestoneUpdateBody
): Promise<ApiResult<UpdateResult>> {
  const res = await fetch(`${apiBase()}/api/goals/${goalId}/milestones/${milestoneId}`, {
    method: 'PATCH',
    headers: headersJson(),
    credentials: 'include',
    body: JSON.stringify(body),
  })
  return parseApiResponse<UpdateResult>(res)
}

// ========== Export helpers ==========

export { isApiSuccess }
