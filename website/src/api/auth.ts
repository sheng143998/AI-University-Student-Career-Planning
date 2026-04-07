/**
 * Auth 模块 — 对齐 接口文档_1_Auth.md
 * 成功码：文档示例为 code=200（与简历模块 code=1 不同，统一用 isApiSuccess 判断）
 */

import { apiBase, headersAuth, headersMultipartAuth, parseApiResponse } from '@/api/client'
import type { ApiResult } from '@/api/types'

export interface AuthUser {
  id: number
  name: string
  username: string
  userImage?: string
  sex?: number
}

export interface LoginBody {
  username: string
  password: string
}

/** 后端 UserLoginVO：token + user… */
export interface LoginData {
  token: string
  username: string
  sex?: number
  userImage?: string
  createtime?: string
}

export interface RegisterBody {
  username: string
  password: string
  name?: string
  sex?: number
  userImage?: string
}

export interface RegisterData extends AuthUser {}

export interface LogoutData {
  ok: boolean
}

export interface UserEditBody {
  name?: string
  oldPassword?: string
  newPassword?: string
  userImage?: string
  sex?: number
}

export interface UserEditData {
  updated: boolean
  user: AuthUser
}

export async function login(body: LoginBody): Promise<ApiResult<LoginData>> {
  const res = await fetch(`${apiBase()}/api/user/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify(body),
  })
  return parseApiResponse<LoginData>(res)
}

export async function register(body: RegisterBody): Promise<ApiResult<RegisterData>> {
  const res = await fetch(`${apiBase()}/api/user/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify(body),
  })
  return parseApiResponse<RegisterData>(res)
}

export async function logout(): Promise<ApiResult<LogoutData>> {
  const res = await fetch(`${apiBase()}/api/user/logout`, {
    method: 'POST',
    headers: headersAuth(),
    credentials: 'include',
  })
  return parseApiResponse<LogoutData>(res)
}

/** 1.4 获取用户信息 */
export async function getUserInfo(): Promise<ApiResult<AuthUser>> {
  const res = await fetch(`${apiBase()}/api/user/info`, {
    method: 'GET',
    headers: headersAuth(),
    credentials: 'include',
  })
  return parseApiResponse<AuthUser>(res)
}

/** 1.5 编辑用户信息 */
export async function updateUserInfo(body: UserEditBody): Promise<ApiResult<UserEditData>> {
  const h = new Headers({ 'Content-Type': 'application/json' })
  const token = (headersAuth() as any)['token'] // 获取 token 名称对应的 value
  if (token) h.set('token', token)

  const res = await fetch(`${apiBase()}/api/user/edit`, {
    method: 'PUT',
    headers: h,
    credentials: 'include',
    body: JSON.stringify(body),
  })
  return parseApiResponse<UserEditData>(res)
}

/** 公共接口 - 文件上传 */
export async function uploadFile(file: File): Promise<ApiResult<string>> {
  const formData = new FormData()
  formData.append('file', file)
  
  const res = await fetch(`${apiBase()}/api/common/upload`, {
    method: 'POST',
    headers: headersMultipartAuth(),
    credentials: 'include',
    body: formData,
  })
  return parseApiResponse<string>(res)
}
