/** Resume module API — aligns with 接口文档_5_Resume.md */

import { apiBase, headersAuth, headersMultipartAuth, isApiSuccess, parseApiResponse } from '@/api/client'
import type { ApiResult } from '@/api/types'

export type { ApiResult } from '@/api/types'

export interface ResumeUploadData {
  id: string
  user_id: number
  resume_file_path: string
  created_at: string
}

export interface ResumeEducation {
  school?: string
  major?: string
  degree?: string
  period?: string
}

export interface ResumeExperience {
  company?: string
  position?: string
  period?: string
  description?: string
}

export interface ResumeParsedData {
  name?: string
  target_role?: string
  skills?: string[]
  experience_years?: number
  education?: ResumeEducation[]
  experience?: ResumeExperience[]
}

export interface ResumeScores {
  keyword_match?: number
  layout?: number
  skill_depth?: number
  experience?: number
}

export interface ResumeSuggestion {
  type?: string
  content?: string
}

/** 与 resume_analysis_result.status 对齐 */
export type ResumeAnalysisStatus = 'pending' | 'processing' | 'completed' | 'failed' | 'stopped'

export interface ResumeAnalysisDetail {
  vector_store_id?: string
  analysis_id?: number
  user_id?: number
  file_type?: string
  original_file_name?: string
  resume_content?: string
  resume_file_path?: string
  /** 分析任务状态：轮询据此结束 */
  status?: ResumeAnalysisStatus | string
  /** 失败/停止等时的说明（若后端返回） */
  status_message?: string
  /** 处理进度 0-100，与 resume_analysis_result.progress 对齐 */
  progress?: number
  parsed_data?: ResumeParsedData
  scores?: ResumeScores
  highlights?: string[]
  suggestions?: ResumeSuggestion[]
  created_at?: string
  updated_at?: string
}

export interface ResumeAnalysisListItem {
  vector_store_id: string
  analysis_id?: number
  file_type?: string
  original_file_name?: string
  resume_file_path?: string
  created_at?: string
  updated_at?: string
}

export interface ResumeAnalysisListData {
  items: ResumeAnalysisListItem[]
  next_cursor: string | null
}

export async function uploadResume(file: File): Promise<ApiResult<ResumeUploadData>> {
  const fd = new FormData()
  fd.append('file', file)
  const res = await fetch(`${apiBase()}/api/resume/upload`, {
    method: 'POST',
    body: fd,
    headers: headersMultipartAuth(),
    credentials: 'include',
  })
  const raw = await parseApiResponse<unknown>(res)
  if (!isApiSuccess(raw.code)) return raw as ApiResult<ResumeUploadData>
  const data = normalizeResumeUploadData(raw.data)
  return { ...raw, data: data ?? (raw.data as ResumeUploadData) } as ApiResult<ResumeUploadData>
}

export async function getResumeAnalysis(id: string): Promise<ApiResult<ResumeAnalysisDetail>> {
  const enc = encodeURIComponent(id)
  const res = await fetch(`${apiBase()}/api/resume/analysis/${enc}`, {
    method: 'GET',
    headers: headersAuth(),
    credentials: 'include',
  })
  const raw = await parseApiResponse<unknown>(res)
  if (!isApiSuccess(raw.code)) return raw as ApiResult<ResumeAnalysisDetail>
  const data = raw.data != null ? normalizeResumeAnalysisDetail(raw.data) : undefined
  return { ...raw, data }
}

/**
 * GET /api/resume/analysis/{id}/preview
 * 需带 token 头；勿用 iframe 直链此 URL（无法携带自定义头），应 fetch 为 Blob 后 createObjectURL。
 */
export async function fetchResumeAnalysisPreview(
  id: string,
  disposition: 'inline' | 'attachment' = 'inline'
): Promise<{ ok: true; blob: Blob } | { ok: false; status: number }> {
  const enc = encodeURIComponent(id)
  const q = new URLSearchParams()
  if (disposition === 'attachment') q.set('disposition', 'attachment')
  const qs = q.toString()
  const url = `${apiBase()}/api/resume/analysis/${enc}/preview${qs ? `?${qs}` : ''}`
  const res = await fetch(url, {
    method: 'GET',
    headers: headersAuth(),
    credentials: 'include',
  })
  if (res.ok) {
    const blob = await res.blob()
    return { ok: true, blob }
  }
  return { ok: false, status: res.status }
}

/** GET /api/resume/analysis/{id}/preview-url — 短期 OSS 签名 URL（可选兜底） */
export async function getResumeAnalysisPreviewUrl(id: string): Promise<ApiResult<string>> {
  const enc = encodeURIComponent(id)
  const res = await fetch(`${apiBase()}/api/resume/analysis/${enc}/preview-url`, {
    method: 'GET',
    headers: headersAuth(),
    credentials: 'include',
  })
  const raw = await parseApiResponse<unknown>(res)
  if (!isApiSuccess(raw.code)) return raw as ApiResult<string>
  const data = raw.data
  const url = typeof data === 'string' ? data : ''
  return { ...raw, data: url }
}

export async function listResumeAnalysis(params?: {
  cursor?: string
  limit?: number
}): Promise<ApiResult<ResumeAnalysisListData>> {
  const q = new URLSearchParams()
  if (params?.cursor) q.set('cursor', params.cursor)
  if (params?.limit != null) q.set('limit', String(params.limit))
  const qs = q.toString()
  const url = `${apiBase()}/api/resume/analysis${qs ? `?${qs}` : ''}`
  const res = await fetch(url, {
    method: 'GET',
    headers: headersAuth(),
    credentials: 'include',
  })
  const raw = await parseApiResponse<unknown>(res)
  if (!isApiSuccess(raw.code)) return raw as ApiResult<ResumeAnalysisListData>
  return { ...raw, data: normalizeListPayload(raw.data) }
}

/** 有解析结果时可用于展示（不用于结束轮询） */
export function isAnalysisComplete(detail: ResumeAnalysisDetail | undefined): boolean {
  const p = detail?.parsed_data
  return p != null && typeof p === 'object' && Object.keys(p).length > 0
}

export function normalizeResumeStatus(raw: string | undefined): string {
  return (raw ?? '').trim().toLowerCase()
}

/** 将后端 progress 约束到 0–100；非法或缺省返回 null */
export function normalizeResumeProgress(value: unknown): number | null {
  let n: number
  if (typeof value === 'number' && !Number.isNaN(value)) {
    n = value
  } else if (typeof value === 'string' && value.trim() !== '') {
    n = Number(value)
    if (Number.isNaN(n)) return null
  } else {
    return null
  }
  return Math.min(100, Math.max(0, Math.round(n)))
}

/** 将后端 camelCase（Jackson 默认）与文档 snake_case 统一为前端使用的 snake_case */
export function normalizeResumeAnalysisDetail(input: unknown): ResumeAnalysisDetail {
  if (input == null || typeof input !== 'object') return {}
  const o = input as Record<string, unknown>
  const parsedRaw = o.parsed_data ?? o.parsedData
  let parsed_data: ResumeParsedData | undefined
  if (parsedRaw != null && typeof parsedRaw === 'object') {
    parsed_data = parsedRaw as ResumeParsedData
  }
  const prog = normalizeResumeProgress(o.progress)
  const rawScores = o.scores
  let scores: ResumeScores | undefined
  if (rawScores != null && typeof rawScores === 'object') {
    const s = rawScores as Record<string, unknown>
    scores = {
      keyword_match: (s.keyword_match ?? s.keywordMatch) as number | undefined,
      layout: s.layout as number | undefined,
      skill_depth: (s.skill_depth ?? s.skillDepth) as number | undefined,
      experience: s.experience as number | undefined,
    }
  }
  return {
    vector_store_id: (o.vector_store_id ?? o.vectorStoreId) as string | undefined,
    analysis_id: (o.analysis_id ?? o.analysisId) as number | undefined,
    user_id: (o.user_id ?? o.userId) as number | undefined,
    file_type: (o.file_type ?? o.fileType) as string | undefined,
    original_file_name: (o.original_file_name ?? o.originalFileName) as string | undefined,
    resume_content: (o.resume_content ?? o.resumeContent) as string | undefined,
    resume_file_path: (o.resume_file_path ?? o.resumeFilePath) as string | undefined,
    status: o.status as string | undefined,
    status_message: (o.status_message ?? o.statusMessage) as string | undefined,
    progress: prog ?? undefined,
    parsed_data,
    scores,
    highlights: o.highlights as string[] | undefined,
    suggestions: o.suggestions as ResumeSuggestion[] | undefined,
    created_at: (o.created_at ?? o.createdAt) as string | undefined,
    updated_at: (o.updated_at ?? o.updatedAt) as string | undefined,
  }
}

function normalizeResumeUploadData(input: unknown): ResumeUploadData | undefined {
  if (input == null || typeof input !== 'object') return undefined
  const o = input as Record<string, unknown>
  const id = o.id ?? o.vectorStoreId
  return {
    id: String(id ?? ''),
    user_id: Number(o.user_id ?? o.userId ?? 0),
    resume_file_path: String(o.resume_file_path ?? o.resumeFilePath ?? ''),
    created_at: String(o.created_at ?? o.createdAt ?? ''),
  }
}

function normalizeListPayload(data: unknown): ResumeAnalysisListData {
  if (Array.isArray(data)) {
    return {
      items: data.map((row) => listItemFromNormalized(normalizeResumeAnalysisDetail(row))),
      next_cursor: null,
    }
  }
  if (data != null && typeof data === 'object') {
    const d = data as Record<string, unknown>
    const rawItems = d.items ?? d.list
    const arr = Array.isArray(rawItems) ? rawItems : []
    const next = d.next_cursor ?? d.nextCursor
    return {
      items: arr.map((row) => listItemFromNormalized(normalizeResumeAnalysisDetail(row))),
      next_cursor: next != null && next !== '' ? String(next) : null,
    }
  }
  return { items: [], next_cursor: null }
}

function listItemFromNormalized(full: ResumeAnalysisDetail): ResumeAnalysisListItem {
  return {
    vector_store_id: full.vector_store_id ?? '',
    analysis_id: full.analysis_id,
    file_type: full.file_type,
    original_file_name: full.original_file_name,
    resume_file_path: full.resume_file_path,
    created_at: full.created_at,
    updated_at: full.updated_at,
  }
}

export function isTerminalResumeAnalysisStatus(status: string | undefined): boolean {
  const s = normalizeResumeStatus(status)
  return s === 'completed' || s === 'failed' || s === 'stopped'
}

export type PollResumeTermination = 'completed' | 'failed' | 'stopped' | 'timeout'

export interface PollResumeAnalysisResult {
  kind: PollResumeTermination
  detail?: ResumeAnalysisDetail
}

/**
 * 每 2s 轮询 GET /api/resume/analysis/{id}，依据 data.status 结束：
 * completed / failed / stopped 停止；pending / processing 继续；最多 30 次（60s）超时。
 */
export function pollResumeAnalysisUntilTerminal(
  id: string,
  options?: {
    intervalMs?: number
    maxAttempts?: number
    onTick?: (attempt: number, detail: ResumeAnalysisDetail | undefined) => void
  }
): Promise<PollResumeAnalysisResult> {
  const intervalMs = options?.intervalMs ?? 2000
  const maxAttempts = options?.maxAttempts ?? 30

  return new Promise((resolve) => {
    let attempt = 0

    const run = async () => {
      attempt += 1
      if (attempt > maxAttempts) {
        resolve({ kind: 'timeout' })
        return
      }
      const r = await getResumeAnalysis(id)
      if (!isApiSuccess(r.code)) {
        options?.onTick?.(attempt, undefined)
        setTimeout(() => void run(), intervalMs)
        return
      }
      const d = r.data
      options?.onTick?.(attempt, d)
      const s = normalizeResumeStatus(d?.status as string | undefined)
      if (s === 'completed') {
        resolve({ kind: 'completed', detail: d })
        return
      }
      if (s === 'failed') {
        resolve({ kind: 'failed', detail: d })
        return
      }
      if (s === 'stopped') {
        resolve({ kind: 'stopped', detail: d })
        return
      }
      if (s === 'pending' || s === 'processing' || s === '') {
        setTimeout(() => void run(), intervalMs)
        return
      }
      setTimeout(() => void run(), intervalMs)
    }

    void run()
  })
}
