import axios from 'axios'
import { apiBase, headersAuth, headersMultipartAuth } from '@/api/client'

type ApiResult<T> = {
  code?: number
  msg?: string
  data?: T
}

const api = axios.create({
  baseURL: apiBase(),
  timeout: 30000,
})

api.interceptors.response.use(
  (response) => response.data,
  (error) => {
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)

// ============ 类型定义 ============

export interface Conversation {
  id: number
  title: string
  lastMessageAt: string | null
  createdAt: string
}

export interface Message {
  id: number
  conversationId: number
  role: 'user' | 'assistant'
  content: string
  createdAt: string
}

export interface SuggestionItem {
  title: string
  text: string
}

export interface QuickQuestion {
  title: string
  text: string
}

export interface DailySuggestions {
  suggestions: SuggestionItem[]
  quickQuestions: QuickQuestion[]
}

function unwrapData<T>(res: ApiResult<T> | null | undefined): T | null {
  if (!res) return null
  if (res.data === undefined || res.data === null) return null
  return res.data
}

function toAxiosHeaders(h: HeadersInit): Record<string, string> {
  if (!h) return {}
  if (h instanceof Headers) {
    const obj: Record<string, string> = {}
    h.forEach((value, key) => {
      obj[key] = value
    })
    return obj
  }
  if (Array.isArray(h)) {
    return Object.fromEntries(h)
  }
  return h as Record<string, string>
}

// ============ API 函数 ============

/** 获取会话列表 */
export async function getConversations(cursor?: number, limit = 20): Promise<Conversation[]> {
  const params: Record<string, unknown> = { limit }
  if (cursor) params.cursor = cursor
  const res = (await api.get('/api/chat/conversations', { params, headers: toAxiosHeaders(headersAuth()) })) as ApiResult<Conversation[]>
  const data = unwrapData(res)
  return Array.isArray(data) ? data : []
}

/** 创建会话 */
export async function createConversation(title: string): Promise<Conversation> {
  const res = (await api.post('/api/chat/conversations', { title }, { headers: toAxiosHeaders(headersAuth()) })) as ApiResult<Conversation>
  const data = unwrapData(res)
  if (!data) throw new Error(res?.msg || '创建会话失败')
  return data
}

/** 获取会话消息 */
export async function getMessages(conversationId: number, cursor?: number, limit = 20): Promise<Message[]> {
  const params: Record<string, unknown> = { limit }
  if (cursor) params.cursor = cursor
  const res = (await api.get(`/api/chat/conversations/${conversationId}/messages`, { params, headers: toAxiosHeaders(headersAuth()) })) as ApiResult<Message[]>
  const data = unwrapData(res)
  return Array.isArray(data) ? data : []
}

/** 发送消息（流式响应） */
export async function sendMessage(conversationId: number, content: string): Promise<Response> {
  const authHeaders = toAxiosHeaders(headersAuth()) as Record<string, string>
  const response = await fetch('/api/chat/messages', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(authHeaders || {}),
    },
    body: JSON.stringify({ conversationId, content })
  })
  return response
}

/** 删除会话 */
export async function deleteConversation(conversationId: number): Promise<void> {
  await api.delete(`/api/chat/conversations/${conversationId}`, { headers: toAxiosHeaders(headersAuth()) })
}

/** 获取每日建议 */
export async function getDailySuggestions(resumeId?: number): Promise<DailySuggestions> {
  const params: Record<string, unknown> = {}
  if (resumeId) params.resumeId = resumeId
  const res = (await api.get('/api/chat/daily-suggestions', { params, headers: toAxiosHeaders(headersAuth()) })) as ApiResult<DailySuggestions>
  const data = unwrapData(res)
  return data ?? { suggestions: [], quickQuestions: [] }
}

/** 上传附件 */
export async function uploadAttachment(file: File): Promise<string> {
  const formData = new FormData()
  formData.append('file', file)
  const res = (await api.post('/api/chat/attachments', formData, {
    headers: { ...toAxiosHeaders(headersMultipartAuth()), 'Content-Type': 'multipart/form-data' }
  })) as ApiResult<string>
  const data = unwrapData(res)
  if (!data) throw new Error(res?.msg || '上传失败')
  return data
}

/** 语音转文字 */
export async function voiceToText(file: File): Promise<string> {
  const formData = new FormData()
  formData.append('file', file)
  const res = (await api.post('/api/chat/voice', formData, {
    headers: { ...toAxiosHeaders(headersMultipartAuth()), 'Content-Type': 'multipart/form-data' }
  })) as ApiResult<string>
  const data = unwrapData(res)
  if (!data) throw new Error(res?.msg || '转写失败')
  return data
}

export default api
