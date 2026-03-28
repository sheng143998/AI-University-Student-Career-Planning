import { getToken } from '@/lib/authToken'
import type { ApiResult } from '@/api/types'

export function apiBase(): string {
  return (import.meta.env.VITE_API_BASE as string | undefined) || ''
}

/**
 * 与后端 `fuchuang.jwt.tokenName` 一致：本项目为 `token`（见 JwtTokenInterceptor#getHeader(tokenName)）
 * 若部署其它网关需要 Authorization，可设 VITE_AUTH_TOKEN_HEADER=Authorization 并自行拼接前缀（不推荐混用）
 */
function tokenHeaderName(): string {
  const n = (import.meta.env.VITE_AUTH_TOKEN_HEADER as string | undefined)?.trim()
  return n || 'token'
}

function applyTokenHeader(target: Headers) {
  const t = getToken()
  if (!t) return
  target.set(tokenHeaderName(), t)
}

/** 与后端约定：简历等模块常用 code=1；Auth 文档为 code=200 */
export function isApiSuccess(code: number | undefined): boolean {
  return code === 1 || code === 200
}

export async function parseApiResponse<T>(res: Response): Promise<ApiResult<T>> {
  const text = await res.text()
  let result: ApiResult<T>
  try {
    result = text ? (JSON.parse(text) as ApiResult<T>) : { code: 0, msg: `HTTP ${res.status}` }
  } catch {
    result = { code: 0, msg: text || `HTTP ${res.status}` }
  }
  if (res.status === 401) {
    return {
      ...result,
      code: 0,
      msg: result.msg || '未登录或会话已过期，请先登录',
    }
  }
  return result
}

export function headersJson(): HeadersInit {
  const h = new Headers({ 'Content-Type': 'application/json' })
  applyTokenHeader(h)
  return h
}

/** 仅带 Token，用于 multipart（勿设 Content-Type，由浏览器写 boundary） */
export function headersMultipartAuth(): HeadersInit {
  const t = getToken()
  if (!t) return {}
  return { [tokenHeaderName()]: t }
}

/** GET 等无需 JSON Content-Type 的请求 */
export function headersAuth(): HeadersInit {
  const t = getToken()
  if (!t) return {}
  return { [tokenHeaderName()]: t }
}
