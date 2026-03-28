import { apiBase, headersAuth, headersJson, parseApiResponse } from '@/api/client'
import type { ApiResult } from '@/api/types'

export interface UserProfileOverview {
  id?: number
  name?: string
  avatar?: string
  location?: string
  current_role?: string
  target_role?: string
  match_score?: number
}

/** 对应后端返回的 camelCase 结构 */
interface UserProfileApiResponse {
  id?: number
  name?: string
  avatar?: string
  location?: string
  currentRole?: string
  targetRole?: string
  matchScore?: number
}

export interface UpdateUserProfileBody {
  name?: string
  avatar?: string
}

export interface UpdateUserProfileData {
  updated?: boolean
}

export interface ProfileEducation {
  school?: string
  major?: string
  degree?: string
  period?: string
}

export interface ProfileExperience {
  company?: string
  position?: string
  period?: string
  description?: string
}

export interface ProfileProject {
  name?: string
  link?: string
  tech_stack?: string[]
}

export interface UserProfileDetail {
  education?: ProfileEducation[]
  experience?: ProfileExperience[]
  skills?: string[]
  projects?: ProfileProject[]
}

export async function getUserProfile(): Promise<ApiResult<UserProfileOverview>> {
  const res = await fetch(`${apiBase()}/api/user/profile`, {
    method: 'GET',
    headers: headersAuth(),
    credentials: 'include',
  })
  const apiResult = await parseApiResponse<UserProfileApiResponse>(res)
  
  // 将 camelCase 转换为 snake_case 以兼容现有前端代码
  const mappedData: UserProfileOverview | undefined = apiResult.data ? {
    id: apiResult.data.id,
    name: apiResult.data.name,
    avatar: apiResult.data.avatar,
    location: apiResult.data.location,
    current_role: apiResult.data.currentRole,
    target_role: apiResult.data.targetRole,
    match_score: apiResult.data.matchScore,
  } : undefined

  return {
    ...apiResult,
    data: mappedData
  }
}

export async function updateUserProfile(body: UpdateUserProfileBody): Promise<ApiResult<UpdateUserProfileData>> {
  const res = await fetch(`${apiBase()}/api/user/profile`, {
    method: 'PUT',
    headers: headersJson(),
    credentials: 'include',
    body: JSON.stringify(body),
  })
  return parseApiResponse<UpdateUserProfileData>(res)
}

export async function getUserProfileDetail(): Promise<ApiResult<UserProfileDetail>> {
  const res = await fetch(`${apiBase()}/api/user/profile/detail`, {
    method: 'GET',
    headers: headersAuth(),
    credentials: 'include',
  })
  return parseApiResponse<UserProfileDetail>(res)
}
