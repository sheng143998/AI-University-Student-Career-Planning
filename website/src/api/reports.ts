import { apiBase, headersAuth, headersJson, isApiSuccess, parseApiResponse } from '@/api/client'
import type { ApiResult } from '@/api/types'

export type ReportStatus = 'DRAFT' | 'COMPLETED' | 'ARCHIVED' | 'PROCESSING' | string

export interface ReportGenerateBody {
  targetJobProfileId?: number
  careerPreference?: {
    preferredCity?: string
    expectedSalary?: string
    careerDirection?: string
  }
}

export interface ReportGenerateResult {
  reportId: number
  reportNo: string
  status: ReportStatus
  estimatedTime?: number
}

export interface LatestReport {
  id: number
  reportNo: string
  title: string
  status: ReportStatus
  matchScore: number
  targetJob?: string
  generatedAt?: string
  updatedAt?: string
  editable?: boolean
}

export interface ReportTargetJob {
  id: number
  name: string
  industry?: string
  city?: string
}

export interface ReportMatchDetails {
  overall?: number
  basic_requirements?: number
  professional_skills?: number
  professional_quality?: number
  development_potential?: number
  [k: string]: unknown
}

export interface ReportSection {
  key: string
  title: string
  content: unknown
}

export interface ReportDetail {
  id: number
  reportNo: string
  title: string
  status: ReportStatus
  userId: number
  targetJob?: ReportTargetJob
  matchScore: number
  matchDetails?: ReportMatchDetails
  sections: ReportSection[]
  aiSuggestions?: string
  editable?: boolean
  generatedAt?: string
  updatedAt?: string
}

export interface ReportUpdateBody {
  career_goal?: unknown
  action_plan?: unknown
  target_job?: unknown
  development_path?: unknown
  [k: string]: unknown
}

export type ReportUpdateResult = boolean

export type ReportDeleteResult = boolean

export async function generateReport(body: ReportGenerateBody = {}): Promise<ApiResult<ReportGenerateResult>> {
  const res = await fetch(`${apiBase()}/api/reports/generate`, {
    method: 'POST',
    headers: headersJson(),
    credentials: 'include',
    body: JSON.stringify(body),
  })
  return parseApiResponse<ReportGenerateResult>(res)
}

export async function getLatestReport(): Promise<ApiResult<LatestReport>> {
  const res = await fetch(`${apiBase()}/api/reports/latest`, {
    method: 'GET',
    headers: headersAuth(),
    credentials: 'include',
  })
  return parseApiResponse<LatestReport>(res)
}

export async function getReportDetail(id: number | string): Promise<ApiResult<ReportDetail>> {
  const enc = encodeURIComponent(String(id))
  const res = await fetch(`${apiBase()}/api/reports/${enc}`, {
    method: 'GET',
    headers: headersAuth(),
    credentials: 'include',
  })
  return parseApiResponse<ReportDetail>(res)
}

export async function updateReport(id: number | string, body: ReportUpdateBody): Promise<ApiResult<ReportUpdateResult>> {
  const enc = encodeURIComponent(String(id))
  const res = await fetch(`${apiBase()}/api/reports/${enc}`, {
    method: 'PUT',
    headers: headersJson(),
    credentials: 'include',
    body: JSON.stringify(body),
  })
  return parseApiResponse<ReportUpdateResult>(res)
}

export async function deleteReport(id: number | string): Promise<ApiResult<ReportDeleteResult>> {
  const enc = encodeURIComponent(String(id))
  const res = await fetch(`${apiBase()}/api/reports/${enc}`, {
    method: 'DELETE',
    headers: headersAuth(),
    credentials: 'include',
  })
  return parseApiResponse<ReportDeleteResult>(res)
}

/**
 * GET /api/reports/{id}/download
 * 返回 PDF 二进制流。这里直接 fetch blob，方便前端触发下载。
 */
export async function fetchReportPdf(id: number | string): Promise<{ ok: true; blob: Blob } | { ok: false; status: number }>
{
  const enc = encodeURIComponent(String(id))
  const res = await fetch(`${apiBase()}/api/reports/${enc}/download`, {
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

export function triggerBrowserDownload(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}

export type PollReportTermination = 'completed' | 'archived' | 'draft' | 'failed' | 'timeout'

export interface PollReportResult {
  kind: PollReportTermination
  detail?: ReportDetail
}

function normalizeStatus(status: ReportStatus | undefined): string {
  return String(status ?? '').trim().toUpperCase()
}

/**
 * 每 intervalMs 轮询 GET /api/reports/{id}，直到 status 变为 COMPLETED/DRAFT/ARCHIVED。
 * 注意：后端可能返回 PROCESSING。
 */
export function pollReportUntilReady(
  id: number | string,
  options?: {
    intervalMs?: number
    maxAttempts?: number
    onTick?: (attempt: number, detail: ReportDetail | undefined) => void
  }
): Promise<PollReportResult> {
  const intervalMs = options?.intervalMs ?? 2000
  const maxAttempts = options?.maxAttempts ?? 60

  return new Promise((resolve) => {
    let attempt = 0

    const run = async () => {
      attempt += 1
      if (attempt > maxAttempts) {
        resolve({ kind: 'timeout' })
        return
      }
      const r = await getReportDetail(id)
      if (!isApiSuccess(r.code)) {
        options?.onTick?.(attempt, undefined)
        setTimeout(() => void run(), intervalMs)
        return
      }
      const d = r.data
      options?.onTick?.(attempt, d)
      const s = normalizeStatus(d?.status)

      if (s === 'COMPLETED') {
        resolve({ kind: 'completed', detail: d })
        return
      }
      if (s === 'ARCHIVED') {
        resolve({ kind: 'archived', detail: d })
        return
      }
      if (s === 'DRAFT') {
        resolve({ kind: 'draft', detail: d })
        return
      }
      if (s === 'FAILED') {
        resolve({ kind: 'failed', detail: d })
        return
      }

      // 生成中/未知状态继续轮询
      setTimeout(() => void run(), intervalMs)
    }

    void run()
  })
}
