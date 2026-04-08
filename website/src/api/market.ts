import { apiBase, headersJson, isApiSuccess, parseApiResponse } from './client'
import type { ApiResult } from './types'

// ==================== 类型定义 ====================

export interface MarketSalaryRange {
  min: number | null
  max: number | null
  currency: string
  unit: string
}

export interface MarketExperienceRange {
  min: number
  max: number
  unit: string
}

export interface MarketCapabilityRequirements {
  innovation: number
  learning: number
  resilience: number
  communication: number
  internship: number
}

export interface MarketProfileItem {
  id: number
  jobName: string
  industrySegment: string
  city: string
  salaryRange: MarketSalaryRange
  experienceRange: MarketExperienceRange
  coreSkills: string[]
  certificateRequirements: string[]
  capabilityRequirements: MarketCapabilityRequirements
  demandLevel: string
  updatedAt: string
}

export interface MarketProfileList {
  total: number
  page: number
  size: number
  items: MarketProfileItem[]
  updatedAt: string
}

export interface MarketSalarySnapshot {
  min: number | null
  max: number | null
  avg: number | null
  currency: string
  unit: string
}

export interface MarketSalaryTrend {
  current: MarketSalarySnapshot
  previous: MarketSalarySnapshot
  yoyGrowth: number
  trend: string
}

export interface MarketDemandTrend {
  level: string
  currentQuarter: number
  previousQuarter: number
  growthRate: number
  trend: string
  histogram: number[]
}

export interface MarketTrends {
  jobProfileId: number
  jobName: string
  city: string
  timeRange: string
  salary: MarketSalaryTrend
  demand: MarketDemandTrend
  hotSkills?: Array<{ name: string; count: number; growthRate: number }>
  updatedAt: string
}

export interface MarketSignal {
  label: string
  value: string
  trend: string
}

export interface MarketSuggestedAction {
  title: string
  desc: string
  priority: string
}

export interface MarketInsightContent {
  title: string
  summary: string
  marketSignals: MarketSignal[]
  industryTrends: string[]
  suggestedActions: MarketSuggestedAction[]
  /** @deprecated Use suggestedActions instead */
  recommendations?: string[]
}

export interface MarketInsight {
  jobProfileId: number
  jobName: string
  city: string
  insight: MarketInsightContent
  updatedAt: string
}

export interface MarketHotJobItem {
  id: number
  jobName: string
  industrySegment: string
  city: string
  tag: string
  salaryRange: MarketSalaryRange
  demandLevel: string
  highlights: string[]
  coreSkills: string[]
  certificateRequirements?: string[]
  capabilityRequirements?: MarketCapabilityRequirements
  growthRate: string
  icon: string
  sourceJobCount?: number
}

export interface MarketHotJobs {
  total: number
  page: number
  size: number
  items: MarketHotJobItem[]
  jobs?: MarketHotJobItem[]  //  backwards compatibility
  updatedAt: string
}

export interface MarketSkillRequirement {
  name: string
  proficiencyRequired: number
}

export interface MarketCareerPath {
  vertical: string[]  // 职位名称数组
  lateral: string[]   // 职位名称数组
}

export interface MarketDemandAnalysis {
  level: string
  growthRate: string
  trend: string
}

export interface SoftSkillItem {
  name: string
  score: number
  description: string
  evidence: string[]
}

export interface MarketJobDetail {
  id: number
  jobName: string
  industrySegment: string
  description: string
  cities: string[]
  salaryRange: MarketSalarySnapshot
  experienceRange: MarketExperienceRange
  educationRequirement: string
  requiredSkills?: string[]  // 来自job表的required_skills
  coreSkills: MarketSkillRequirement[]
  capabilityRequirements: MarketCapabilityRequirements
  softSkills?: SoftSkillItem[]  // 软技能详情（描述+证据）
  certificateRequirements: string[]
  companyBenefits: string[]
  careerPath: MarketCareerPath
  demandAnalysis: MarketDemandAnalysis
  updatedAt: string
}

// 软技能详情（带描述和证据）
export interface SoftSkillItem {
  name: string
  score: number
  description: string
  evidence: string[]
}

// ==================== API 函数 ====================

/**
 * 获取岗位画像列表（支持筛选和分页）
 */
export async function getMarketProfiles(params: {
  industry?: string
  city?: string
  keyword?: string
  page?: number
  size?: number
}): Promise<ApiResult<MarketProfileList>> {
  const query = new URLSearchParams()
  if (params.industry) query.set('industry', params.industry)
  if (params.city) query.set('city', params.city)
  if (params.keyword) query.set('keyword', params.keyword)
  if (params.page) query.set('page', String(params.page))
  if (params.size) query.set('size', String(params.size))

  const res = await fetch(`${apiBase()}/api/market/profiles?${query.toString()}`, {
    method: 'GET',
    headers: headersJson(),
  })
  return parseApiResponse<MarketProfileList>(res)
}

/**
 * 获取岗位市场趋势
 */
export async function getMarketTrends(params: {
  job_profile_id?: number
  city?: string
  time_range?: string
}): Promise<ApiResult<MarketTrends>> {
  const query = new URLSearchParams()
  if (params.job_profile_id) query.set('job_profile_id', String(params.job_profile_id))
  if (params.city) query.set('city', params.city)
  if (params.time_range) query.set('time_range', params.time_range)

  const res = await fetch(`${apiBase()}/api/market/trends?${query.toString()}`, {
    method: 'GET',
    headers: headersJson(),
  })
  return parseApiResponse<MarketTrends>(res)
}

/**
 * 获取AI深度洞察
 */
export async function getMarketInsight(params: {
  job_profile_id?: number
  city?: string
}): Promise<ApiResult<MarketInsight>> {
  const query = new URLSearchParams()
  if (params.job_profile_id) query.set('job_profile_id', String(params.job_profile_id))
  if (params.city) query.set('city', params.city)

  const res = await fetch(`${apiBase()}/api/market/insight?${query.toString()}`, {
    method: 'GET',
    headers: headersJson(),
  })
  return parseApiResponse<MarketInsight>(res)
}

/**
 * 获取热门岗位列表
 */
export async function getMarketHotJobs(params?: {
  limit?: number
  city?: string
  industry?: string
  page?: number
  size?: number
}): Promise<ApiResult<MarketHotJobs>> {
  const query = new URLSearchParams()
  if (params?.limit) query.set('limit', String(params.limit))
  if (params?.city) query.set('city', params.city)
  if (params?.industry) query.set('industry', params.industry)
  if (params?.page) query.set('page', String(params.page))
  if (params?.size) query.set('size', String(params.size))

  const res = await fetch(`${apiBase()}/api/market/hot-jobs?${query.toString()}`, {
    method: 'GET',
    headers: headersJson(),
  })
  return parseApiResponse<MarketHotJobs>(res)
}

/**
 * 根据ID获取岗位详情
 */
export async function getMarketJobDetail(jobId: number): Promise<ApiResult<MarketJobDetail>> {
  const res = await fetch(`${apiBase()}/api/market/jobs/${jobId}`, {
    method: 'GET',
    headers: headersJson(),
  })
  return parseApiResponse<MarketJobDetail>(res)
}

/**
 * 生成并保存岗位画像（AI生成软技能）
 */
export async function generateMarketJobProfile(jobId: number): Promise<ApiResult<MarketJobDetail>> {
  const res = await fetch(`${apiBase()}/api/market/jobs/${jobId}/generate`, {
    method: 'POST',
    headers: headersJson(),
  })
  return parseApiResponse<MarketJobDetail>(res)
}

/**
 * Batch generate all job profiles (AI generates soft skills for all jobs)
 */
export async function generateAllJobProfiles(): Promise<ApiResult<number>> {
  const res = await fetch(`${apiBase()}/api/market/jobs/generate-all`, {
    method: 'POST',
    headers: headersJson(),
  })
  return parseApiResponse<number>(res)
}
