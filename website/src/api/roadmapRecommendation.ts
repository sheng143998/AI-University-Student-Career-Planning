import { apiBase, headersAuth, parseApiResponse } from '@/api/client'
import type { ApiResult } from '@/api/types'

/** 路径节点 */
export interface PathNode {
  id: string
  title: string
  levelName: string
  salaryRange: string
  skills: string[]
  isCurrentLevel: boolean
}

/** 垂直晋升路径推荐 */
export interface VerticalPathRecommendation {
  categoryCode: string
  matchedJobName: string
  similarityScore: number
  nodes: PathNode[]
  currentLevelIndex: number
  estimatedMonthsToNext: number
}

/** 横向换岗路径推荐 */
export interface LateralPathRecommendation {
  targetJobId: number
  targetJobName: string
  targetCategoryCode: string
  matchScore: number
  transitionDifficulty: number
  estimatedMonths: number
  requiredSkills: string[]
  possessedSkills: string[]
  aiRecommendationReason: string
  pathNodes: PathNode[]
}

/** 个性化职业路径推荐结果 */
export interface CareerPathRecommendation {
  currentJob: string
  verticalPath: VerticalPathRecommendation
  lateralPaths: LateralPathRecommendation[]
  generatedAt: string
}

/**
 * 获取个性化职业路径推荐
 * @returns 推荐结果（垂直晋升路径 + 横向换岗路径）
 */
export async function getPersonalizedRecommendations(): Promise<ApiResult<CareerPathRecommendation>> {
  const res = await fetch(`${apiBase()}/api/roadmap/recommendations/personalized`, {
    method: 'GET',
    headers: headersAuth(),
    credentials: 'include',
  })
  return parseApiResponse<CareerPathRecommendation>(res)
}

/**
 * 清空个性化推荐缓存
 * @returns 操作结果
 */
export async function clearPersonalizedRecommendationsCache(): Promise<ApiResult<string>> {
  const res = await fetch(`${apiBase()}/api/roadmap/recommendations/personalized/cache`, {
    method: 'DELETE',
    headers: headersAuth(),
    credentials: 'include',
  })
  return parseApiResponse<string>(res)
}

/**
 * 保存用户手动设置的当前岗位
 * @param currentJob 当前岗位
 * @returns 操作结果
 */
export async function saveUserCurrentJob(currentJob: string): Promise<ApiResult<string>> {
  const res = await fetch(`${apiBase()}/api/roadmap/user/current-job`, {
    method: 'POST',
    headers: {
      ...headersAuth(),
      'Content-Type': 'application/json',
    },
    credentials: 'include',
    body: JSON.stringify({ currentJob }),
  })
  return parseApiResponse<string>(res)
}

/**
 * 获取用户手动设置的当前岗位
 * @returns 当前岗位
 */
export async function getUserCurrentJob(): Promise<ApiResult<string>> {
  const res = await fetch(`${apiBase()}/api/roadmap/user/current-job`, {
    method: 'GET',
    headers: headersAuth(),
    credentials: 'include',
  })
  return parseApiResponse<string>(res)
}
