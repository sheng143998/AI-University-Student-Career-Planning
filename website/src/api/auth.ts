/**
 * Auth 模块 — 对齐 接口文档_1_Auth.md
 * 成功码：文档示例为 code=200（与简历模块 code=1 不同，统一用 isApiSuccess 判断）
 */

import { apiBase, headersJson, parseApiResponse } from '@/api/client'
import type { ApiResult } from '@/api/types'

export interface AuthUser {
  id: string
  name: string
}

export interface LoginBody {
  username: string
  password: string
}

/** 后端 UserLoginVO：token + username…；接口文档里的 user/expires_in 为可选扩展 */
export interface LoginData {
  token: string
  username?: string
  sex?: number
  userimage?: string
  createtime?: string
  expires_in?: number
  user?: AuthUser
}

export interface RegisterBody {
  username: string
  password: string
  /** 可选：JPEG Base64（无 data: 前缀），与登录 VO 的 userimage 对齐 */
  userimage?: string
}

export interface RegisterData {
  registered: boolean
}

export interface LogoutData {
  ok: boolean
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
  const res = await fetch(`${apiBase()}/api/auth/logout`, {
    method: 'POST',
    headers: headersJson(),
    credentials: 'include',
    body: JSON.stringify({}),
  })
  return parseApiResponse<LogoutData>(res)
}
