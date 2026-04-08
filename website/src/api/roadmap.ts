/**
 * Roadmap 模块 — 对齐 接口文档_8_Roadmap.md
 */

import { apiBase, headersAuth, headersJson, isApiSuccess, parseApiResponse } from '@/api/client'
import type { ApiResult } from '@/api/types'

// ========== Types ==========

export interface PathStep {
  step: number
  jobName: string
  skills: string[]
  avgTimeMonths: number
  difficulty: number
}

export interface JobVerticalPath {
  id: number
  pathType: string
  targetJobName: string
  totalSteps: number
  estimatedTotalMonths: number
  confidenceScore: number
  pathSteps: PathStep[]
}

export interface SkillsGap {
  skill: string
  level: number
  priority: string
}

export interface JobTransitionPath {
  id: number
  toJobId: number
  toJobName: string
  transitionDifficulty: number
  avgTransitionTimeMonths: number
  similarityScore: number
  requiredSkillsGap: SkillsGap[]
}

export interface JobVerticalPathDetail {
  jobId: number
  jobName: string
  paths: JobVerticalPath[]
}

export interface JobTransitionPathDetail {
  jobId: number
  jobName: string
  transitionPaths: JobTransitionPath[]
}

export interface MatchedJob {
  id: number
  jobName: string
  matchScore: number
}

export interface UserVerticalRecommendation {
  currentJobName: string
  matchedJob: MatchedJob | null
  recommendedPaths: JobVerticalPath[]
  message: string
}

export interface TransitionRecommendationItem {
  id: number
  recommendationId: number
  toJobId: number
  toJobName: string
  matchScore: number
  transitionDifficulty: number
  avgTransitionTimeMonths: number
  requiredSkillsGap: SkillsGap[]
}

export interface UserTransitionRecommendation {
  currentSkills: string[]
  recommendations: TransitionRecommendationItem[]
  message: string
}

export interface UserVerticalPathRecommendation {
  id: number
  currentJobName: string
  matchedJobName: string
  targetJobName: string
  pathSteps: PathStep[]
  currentStepIndex: number
  totalSteps: number
  estimatedTotalMonths: number
  matchScore: number
  status: string
  createTime: string
}

export interface UserTransitionPathRecommendation {
  id: number
  toJobName: string
  matchScore: number
  transitionDifficulty: number
  requiredSkillsGap: SkillsGap[]
  status: string
  createTime: string
}

export interface JobSearchResult {
  id: number
  jobName: string
  industry: string
  salaryRange: string
  similarityScore: number
}

export interface SkillAdvice {
  name: string
  priority: string
  advice: string
}

export interface JobDetail {
  id: number
  jobName: string
  jobLevel: string
  jobLevelName: string
  description: string
  industry: string
  salaryRange: string
  requiredSkills: string[]
  advancementSkills: SkillAdvice[]
}

export interface GenerateResult {
  totalJobs: number
  generatedPaths: number
  failedJobs: number
  message: string
}

export interface RoadmapGenerateBody {
  forceRegenerate?: boolean
  minPathsPerJob?: number
}

export interface UpdateStepBody {
  stepIndex: number
}

// ========== API Functions ==========

/** AI 批量生成所有岗位的垂直晋升路径（管理员） */
export async function generateVerticalPaths(body?: RoadmapGenerateBody): Promise<ApiResult<GenerateResult>> {
  const res = await fetch(`${apiBase()}/api/roadmap/paths/vertical/generate`, {
    method: 'POST',
    headers: headersJson(),
    credentials: 'include',
    body: JSON.stringify(body || {}),
  })
  return parseApiResponse<GenerateResult>(res)
}

/** AI 批量生成所有岗位的换岗路径（管理员） */
export async function generateTransitionPaths(body?: RoadmapGenerateBody): Promise<ApiResult<GenerateResult>> {
  const res = await fetch(`${apiBase()}/api/roadmap/paths/transition/generate`, {
    method: 'POST',
    headers: headersJson(),
    credentials: 'include',
    body: JSON.stringify(body || {}),
  })
  return parseApiResponse<GenerateResult>(res)
}

/** 获取指定岗位的垂直晋升路径 */
export async function getVerticalPathsByJobId(jobId: number): Promise<ApiResult<JobVerticalPathDetail>> {
  const res = await fetch(`${apiBase()}/api/roadmap/paths/vertical/${jobId}`, {
    method: 'GET',
    headers: headersAuth(),
    credentials: 'include',
  })
  return parseApiResponse<JobVerticalPathDetail>(res)
}

/** 获取指定岗位的换岗路径 */
export async function getTransitionPathsByJobId(jobId: number): Promise<ApiResult<JobTransitionPathDetail>> {
  const res = await fetch(`${apiBase()}/api/roadmap/paths/transition/${jobId}`, {
    method: 'GET',
    headers: headersAuth(),
    credentials: 'include',
  })
  return parseApiResponse<JobTransitionPathDetail>(res)
}

/** 岗位路径库：查询多个岗位的垂直晋升路径 */
export async function listVerticalPathLibrary(keyword?: string, limit: number = 20): Promise<ApiResult<JobVerticalPathDetail[]>> {
  const qs = new URLSearchParams()
  if (keyword) qs.set('keyword', keyword)
  qs.set('limit', String(limit))

  const res = await fetch(`${apiBase()}/api/roadmap/paths/vertical?${qs.toString()}`, {
    method: 'GET',
    headers: headersAuth(),
    credentials: 'include',
  })
  return parseApiResponse<JobVerticalPathDetail[]>(res)
}

/** 岗位路径库：查询多个岗位的换岗路径 */
export async function listTransitionPathLibrary(keyword?: string, limit: number = 20): Promise<ApiResult<JobTransitionPathDetail[]>> {
  const qs = new URLSearchParams()
  if (keyword) qs.set('keyword', keyword)
  qs.set('limit', String(limit))

  const res = await fetch(`${apiBase()}/api/roadmap/paths/transition?${qs.toString()}`, {
    method: 'GET',
    headers: headersAuth(),
    credentials: 'include',
  })
  return parseApiResponse<JobTransitionPathDetail[]>(res)
}

/** RAG 推荐用户垂直晋升路径 */
export async function recommendVerticalPath(): Promise<ApiResult<UserVerticalRecommendation>> {
  const res = await fetch(`${apiBase()}/api/roadmap/recommend/vertical`, {
    method: 'POST',
    headers: headersJson(),
    credentials: 'include',
  })
  return parseApiResponse<UserVerticalRecommendation>(res)
}

/** RAG 推荐用户垂直晋升路径（手动指定岗位） */
export async function recommendVerticalPathByJobName(jobName: string): Promise<ApiResult<UserVerticalRecommendation>> {
  const res = await fetch(`${apiBase()}/api/roadmap/recommend/vertical/by-job?jobName=${encodeURIComponent(jobName)}`, {
    method: 'POST',
    headers: headersJson(),
    credentials: 'include',
  })
  return parseApiResponse<UserVerticalRecommendation>(res)
}

/** RAG 推荐用户换岗路径 */
export async function recommendTransitionPaths(): Promise<ApiResult<UserTransitionRecommendation>> {
  const res = await fetch(`${apiBase()}/api/roadmap/recommend/transition`, {
    method: 'POST',
    headers: headersJson(),
    credentials: 'include',
  })
  return parseApiResponse<UserTransitionRecommendation>(res)
}

/** RAG 推荐用户换岗路径（手动指定岗位） */
export async function recommendTransitionPathsByJobName(jobName: string): Promise<ApiResult<UserTransitionRecommendation>> {
  const res = await fetch(`${apiBase()}/api/roadmap/recommend/transition/by-job?jobName=${encodeURIComponent(jobName)}`, {
    method: 'POST',
    headers: headersJson(),
    credentials: 'include',
  })
  return parseApiResponse<UserTransitionRecommendation>(res)
}

/** 获取用户当前激活的晋升路径 */
export async function getActiveVerticalPath(): Promise<ApiResult<UserVerticalPathRecommendation>> {
  const res = await fetch(`${apiBase()}/api/roadmap/user/vertical/active`, {
    method: 'GET',
    headers: headersAuth(),
    credentials: 'include',
  })
  return parseApiResponse<UserVerticalPathRecommendation>(res)
}

/** 获取用户所有晋升路径历史 */
export async function getAllVerticalPaths(): Promise<ApiResult<UserVerticalPathRecommendation[]>> {
  const res = await fetch(`${apiBase()}/api/roadmap/user/vertical`, {
    method: 'GET',
    headers: headersAuth(),
    credentials: 'include',
  })
  return parseApiResponse<UserVerticalPathRecommendation[]>(res)
}

/** 更新晋升路径当前步骤 */
export async function updateStep(id: number, body: UpdateStepBody): Promise<ApiResult<UserVerticalPathRecommendation>> {
  const res = await fetch(`${apiBase()}/api/roadmap/user/vertical/${id}/step`, {
    method: 'PATCH',
    headers: headersJson(),
    credentials: 'include',
    body: JSON.stringify(body),
  })
  return parseApiResponse<UserVerticalPathRecommendation>(res)
}

/** 获取用户换岗路径推荐列表 */
export async function getTransitionPaths(): Promise<ApiResult<UserTransitionPathRecommendation[]>> {
  const res = await fetch(`${apiBase()}/api/roadmap/user/transition`, {
    method: 'GET',
    headers: headersAuth(),
    credentials: 'include',
  })
  return parseApiResponse<UserTransitionPathRecommendation[]>(res)
}

/** 搜索岗位（向量检索） */
export async function searchJobs(q: string, limit: number = 10): Promise<ApiResult<JobSearchResult[]>> {
  const res = await fetch(`${apiBase()}/api/roadmap/jobs/search?q=${encodeURIComponent(q)}&limit=${limit}`, {
    method: 'GET',
    headers: headersAuth(),
    credentials: 'include',
  })
  return parseApiResponse<JobSearchResult[]>(res)
}

// ========== 职业地图（基于 job 表）==========

/** 职业地图搜索结果项 */
export interface RoadmapSearchItem {
  id: string
  categoryCode: string
  title: string
  subtitle: string
  tags: string[]
  variant: 'primary' | 'neutral'
  hasVerticalPaths: boolean
  hasLateralPaths: boolean
}

/** 职业地图搜索结果 */
export interface RoadmapSearchResult {
  items: RoadmapSearchItem[]
}

/** 职业地图节点 */
export interface RoadmapGraphNode {
  id: string
  title: string
  label: string
  subtitle: string
  subLabel: string
  kind: 'core' | 'secondary'
  tags: string[]
  x: number
  y: number
  variant: 'primary' | 'neutral'
}

/** 职业地图路径 */
export interface RoadmapGraphPath {
  from: string
  to: string
  variant: 'primary' | 'secondary'
  edgeType: 'vertical' | 'lateral'
  lineStyle?: 'dashed' | 'solid'
  difficulty: number
  avgTimeMonths: number
  successRate: number
}

/** 职业地图图谱 */
export interface RoadmapGraph {
  mode: 'vertical' | 'lateral'
  viewType?: 'global' | 'focused'
  centerCategoryCode?: string
  nodes: RoadmapGraphNode[]
  paths: RoadmapGraphPath[]
}

/** 职业地图节点详情 */
export interface RoadmapNodeDetail {
  id: string
  title: string
  summary: string
  requirements: string[]
  recommendedSkills: string[]
  salaryRange: string
  experienceYears: string
  level: string
  levelName: string
}

/** 搜索职业节点（基于 job 表） */
export async function searchRoadmapNodes(q: string, limit: number = 20): Promise<ApiResult<RoadmapSearchResult>> {
  const res = await fetch(`${apiBase()}/api/roadmap/search?q=${encodeURIComponent(q)}&limit=${limit}`, {
    method: 'GET',
    headers: headersAuth(),
    credentials: 'include',
  })
  return parseApiResponse<RoadmapSearchResult>(res)
}

/** 获取职业地图图谱（基于 job 表的垂直晋升路径） */
export async function getRoadmapGraph(categoryCode: string, mode: string = 'vertical'): Promise<ApiResult<RoadmapGraph>> {
  const qs = new URLSearchParams()
  if (categoryCode) qs.set('categoryCode', categoryCode)
  qs.set('mode', mode)

  const res = await fetch(`${apiBase()}/api/roadmap/graph?${qs.toString()}`, {
    method: 'GET',
    headers: headersAuth(),
    credentials: 'include',
  })
  return parseApiResponse<RoadmapGraph>(res)
}

/** 获取职业地图节点详情（基于 job 表） */
export async function getRoadmapNodeDetail(nodeId: string): Promise<ApiResult<RoadmapNodeDetail>> {
  const res = await fetch(`${apiBase()}/api/roadmap/nodes/${nodeId}`, {
    method: 'GET',
    headers: headersAuth(),
    credentials: 'include',
  })
  return parseApiResponse<RoadmapNodeDetail>(res)
}
export async function getRandomVerticalPath(): Promise<ApiResult<JobVerticalPathDetail>> {
  const res = await fetch(`${apiBase()}/api/roadmap/map/random-path`, {
    method: 'GET',
    headers: headersAuth(),
    credentials: 'include',
  })
  return parseApiResponse<JobVerticalPathDetail>(res)
}

/** 获取岗位详情（描述、技能、薪资） */
export async function getJobDetail(jobId: number): Promise<ApiResult<JobDetail>> {
  const res = await fetch(`${apiBase()}/api/roadmap/map/job-detail/${jobId}`, {
    method: 'GET',
    headers: headersAuth(),
    credentials: 'include',
  })
  return parseApiResponse<JobDetail>(res)
}

/** 根据岗位名称获取晋升路径 */
export async function getVerticalPathByJobName(jobName: string): Promise<ApiResult<JobVerticalPathDetail>> {
  const res = await fetch(`${apiBase()}/api/roadmap/map/path-by-name?jobName=${encodeURIComponent(jobName)}`, {
    method: 'GET',
    headers: headersAuth(),
    credentials: 'include',
  })
  return parseApiResponse<JobVerticalPathDetail>(res)
}

// ========== Export helpers ==========

export { isApiSuccess }
