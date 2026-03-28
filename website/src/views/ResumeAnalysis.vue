<template>
  <div class="space-y-8">
    <header>
      <h1 class="text-4xl font-extrabold font-headline tracking-tight text-on-surface mb-2">简历上传与 AI 分析</h1>
      <p class="text-on-surface-variant font-body">根据状态实时分析简历进度。</p>
    </header>

    <div
      v-if="bannerAfterUpload"
      class="rounded-xl border px-4 py-3 text-sm"
      :class="uploadBannerClass"
      role="status"
    >
      <span class="font-bold">{{ bannerText }}</span>
    </div>

    <div v-if="globalError" class="rounded-xl border border-red-200 bg-red-50 dark:bg-red-950/30 dark:border-red-900 px-4 py-3 text-sm text-red-800 dark:text-red-200">
      {{ globalError }}
    </div>

    <div
      v-if="warningNotice"
      class="rounded-xl border border-amber-200 bg-amber-50 dark:bg-amber-950/40 dark:border-amber-800 px-4 py-3 text-sm text-amber-900 dark:text-amber-100"
      role="status"
    >
      {{ warningNotice }}
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-12 gap-6">
      <section class="lg:col-span-8 space-y-6">
        <div class="bg-surface-container-lowest rounded-xl p-6 shadow-sm">
          <input
            ref="fileInputEl"
            class="hidden"
            type="file"
            accept=".pdf,.doc,.docx,.pptx,.html,.htm,.txt"
            :disabled="uploading || polling"
            @change="onFileChange"
          />

          <!-- 无本地文件且未选历史：上传区；有本地文件：本次上传；仅有历史：展示该条 OSS 文件信息 -->
          <div
            v-if="!resumeFileShown && !historyPanelFile"
            class="border-2 border-dashed border-outline-variant rounded-xl p-10 flex flex-col items-center justify-center text-center hover:bg-surface-container-low transition-colors cursor-pointer group"
            role="button"
            tabindex="0"
            @click="chooseFile"
            @keydown.enter.prevent="chooseFile"
          >
            <div class="w-16 h-16 bg-primary-fixed rounded-full flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
              <span class="material-symbols-outlined text-primary text-3xl">cloud_upload</span>
            </div>
            <h3 class="text-xl font-bold mb-2">上传简历</h3>
            <p class="text-on-surface-variant text-sm mb-4">支持 PDF、DOCX、PPTX、HTML、TXT，最大 20MB</p>
            <button
              class="px-8 py-3 bg-primary text-on-primary rounded-xl font-bold shadow-lg hover:shadow-primary/20 transition-all disabled:opacity-50"
              type="button"
              :disabled="uploading || polling"
            >
              {{ uploading ? '上传中…' : '选择文件' }}
            </button>
            <p v-if="selectedFileName && uploading" class="text-xs text-on-surface-variant mt-4">已选择：{{ selectedFileName }}</p>
          </div>

          <div v-else-if="resumeFileShown" class="rounded-xl border border-outline-variant/80 bg-surface-container-low p-5 sm:p-6">
            <div class="flex flex-col sm:flex-row sm:items-start gap-4">
              <div
                class="flex h-14 w-14 shrink-0 items-center justify-center rounded-xl bg-primary-fixed/15 text-primary"
                aria-hidden="true"
              >
                <span class="material-symbols-outlined text-[2.25rem]">{{ resumeFileIcon }}</span>
              </div>
              <div class="min-w-0 flex-1">
                <p class="text-xs font-bold uppercase tracking-wider text-on-surface-variant">当前简历文件（本次上传）</p>
                <p class="mt-1 font-bold text-on-surface break-words">{{ resumeFileShown.name }}</p>
                <p class="mt-1 text-sm text-on-surface-variant">{{ formatFileSize(resumeFileShown.size) }} · {{ resumeFileKindLabel }}</p>
              </div>
              <button
                type="button"
                class="shrink-0 self-start rounded-lg border border-outline-variant px-4 py-2 text-sm font-bold text-primary hover:bg-surface-container-high transition-colors disabled:opacity-50"
                :disabled="uploading || polling"
                @click.stop="clearFilePanel"
              >
                重新上传
              </button>
            </div>
            <div v-if="pdfPreviewUrl" class="mt-4 overflow-hidden rounded-lg border border-outline-variant/60 bg-surface-container-highest">
              <iframe :src="pdfPreviewUrl" title="简历 PDF 预览" class="h-72 w-full" />
            </div>
          </div>

          <div v-else-if="historyPanelFile" class="rounded-xl border border-outline-variant/80 bg-surface-container-low p-5 sm:p-6">
            <div class="flex flex-col sm:flex-row sm:items-start gap-4">
              <div
                class="flex h-14 w-14 shrink-0 items-center justify-center rounded-xl bg-emerald-500/10 text-emerald-700 dark:text-emerald-400"
                aria-hidden="true"
              >
                <span class="material-symbols-outlined text-[2.25rem]">{{ historyFileIcon }}</span>
              </div>
              <div class="min-w-0 flex-1">
                <p class="text-xs font-bold uppercase tracking-wider text-on-surface-variant">历史简历文件</p>
                <p class="mt-1 font-bold text-on-surface break-words">{{ historyPanelFile.name }}</p>
                <p class="mt-1 text-sm text-on-surface-variant">{{ historyFileKindLabel }}</p>
                <p class="mt-2 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm">
                  <a
                    v-if="historyPanelFile.url"
                    :href="historyPanelFile.url"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="font-bold text-primary underline"
                  >在新窗口打开（OSS）</a>
                  <button
                    type="button"
                    class="font-bold text-primary underline"
                    @click="downloadHistoryResume"
                  >
                    下载原件
                  </button>
                </p>
                <p v-if="!historyPanelFile.url" class="mt-1 text-xs text-on-surface-variant">暂无 OSS 直链；可通过「下载原件」从服务端获取。</p>
              </div>
              <button
                type="button"
                class="shrink-0 self-start rounded-lg border border-outline-variant px-4 py-2 text-sm font-bold text-primary hover:bg-surface-container-high transition-colors"
                @click.stop="clearFilePanel"
              >
                上传新简历
              </button>
            </div>
            <p v-if="historyPreviewLoading" class="mt-4 text-sm text-on-surface-variant">正在加载预览…</p>
            <p v-else-if="historyPreviewHint" class="mt-4 text-sm text-amber-800 dark:text-amber-200">{{ historyPreviewHint }}</p>
            <div v-if="historyPreviewBlobUrl" class="mt-4 overflow-hidden rounded-lg border border-outline-variant/60 bg-surface-container-highest">
              <iframe :src="historyPreviewBlobUrl" title="历史简历预览" class="h-72 w-full" />
            </div>
          </div>

          <div class="mt-6">
            <div class="flex justify-between items-center mb-2">
              <span class="text-sm font-bold flex items-center gap-2">
                <span
                  class="material-symbols-outlined text-primary text-sm"
                  :class="{ 'animate-spin': polling }"
                >
                  {{ polling ? 'cycle' : 'hourglass_empty' }}
                </span>
                分析进度
              </span>
              <span class="text-primary font-bold">{{ parsingProgress }}%</span>
            </div>
            <div class="w-full h-2 bg-surface-container-high rounded-full overflow-hidden">
              <div class="h-full bg-gradient-to-r from-primary to-primary-container rounded-full transition-all duration-300" :style="{ width: parsingProgress + '%' }" />
            </div>
            <p class="text-xs text-on-surface-variant mt-3">{{ parsingStatusText }}</p>
          </div>
        </div>

        <div class="bg-surface-container-lowest rounded-xl p-6 shadow-sm">
          <div class="flex justify-between items-center mb-4">
            <h3 class="text-lg font-bold">历史分析记录</h3>
            <button
              type="button"
              class="text-sm font-bold text-primary hover:underline disabled:opacity-50"
              :disabled="loadingList"
              @click="refreshList"
            >
              刷新
            </button>
          </div>
          <p v-if="loadingList && !historyItems.length" class="text-sm text-on-surface-variant">加载中…</p>
          <ul v-else-if="historyItems.length" class="space-y-2">
            <li v-for="row in historyItems" :key="row.vector_store_id">
              <button
                type="button"
                class="w-full text-left rounded-lg border border-outline-variant/60 px-4 py-3 hover:bg-surface-container-low transition-colors"
                :class="selectedId === row.vector_store_id ? 'ring-2 ring-primary' : ''"
                @click="loadDetail(row.vector_store_id)"
              >
                <div class="font-bold text-sm truncate">{{ row.original_file_name || row.vector_store_id }}</div>
                <div class="text-xs text-on-surface-variant mt-1">
                  {{ row.file_type || '—' }} · {{ formatTime(row.updated_at || row.created_at) }}
                </div>
              </button>
            </li>
          </ul>
          <p v-else class="text-sm text-on-surface-variant">暂无记录（登录后可见当前用户列表）</p>
          <button
            v-if="listNextCursor"
            type="button"
            class="mt-4 w-full py-2 text-sm font-bold text-primary border border-primary/40 rounded-lg hover:bg-primary/5"
            :disabled="loadingList"
            @click="loadMoreList"
          >
            加载更多
          </button>
        </div>
      </section>

      <section class="lg:col-span-4 space-y-6">
        <div class="bg-primary-container text-on-primary rounded-xl p-6 shadow-xl relative overflow-hidden">
          <div class="absolute -right-4 -top-4 opacity-10">
            <span class="material-symbols-outlined text-[120px]">analytics</span>
          </div>
          <h3 class="text-lg font-bold mb-4 opacity-90">综合评分（四维均值）</h3>
          <div class="flex items-baseline gap-2">
            <span class="text-6xl font-black">{{ competitivenessScore }}</span>
            <span class="text-xl font-bold">/ 100</span>
          </div>
          <p class="text-sm mt-4 text-primary-fixed-dim">{{ competitivenessNote }}</p>
        </div>

        <div class="bg-surface-container-lowest rounded-xl p-6 shadow-sm">
          <h3 class="text-sm font-bold text-on-surface-variant mb-4 uppercase tracking-wider">简历信息完整度</h3>
          <div class="relative w-32 h-32 mx-auto">
            <svg class="w-full h-full transform -rotate-90">
              <circle class="text-surface-container-high" cx="64" cy="64" r="58" fill="transparent" stroke="currentColor" stroke-width="8" />
              <circle
                class="text-tertiary-fixed-dim"
                cx="64"
                cy="64"
                r="58"
                fill="transparent"
                stroke="currentColor"
                stroke-width="8"
                stroke-linecap="round"
                :stroke-dasharray="circumference"
                :stroke-dashoffset="dashOffset"
              />
            </svg>
            <div class="absolute inset-0 flex items-center justify-center">
              <span class="text-2xl font-black text-on-surface">{{ completenessPercent }}%</span>
            </div>
          </div>
          <ul class="mt-6 space-y-2">
            <li
              v-for="i in completenessItems"
              :key="i.label"
              class="flex items-center gap-2 text-xs"
              :class="i.done ? '' : 'text-on-surface-variant opacity-60'"
            >
              <span
                class="material-symbols-outlined text-sm"
                :class="i.done ? 'text-emerald-500' : ''"
                :style="i.done ? 'font-variation-settings: \'FILL\' 1;' : ''"
              >
                {{ i.done ? 'check_circle' : 'radio_button_unchecked' }}
              </span>
              {{ i.label }}
            </li>
          </ul>
        </div>
      </section>

      <!-- 分析结果：单列自上而下，避免左右分栏高度不齐 -->
      <section class="lg:col-span-12 space-y-6">
        <div class="bg-surface-container-lowest rounded-xl p-6 shadow-sm">
          <div class="flex justify-between items-center mb-6">
            <h3 class="text-xl font-bold">解析概要</h3>
            <span class="material-symbols-outlined text-on-surface-variant">person</span>
          </div>
          <dl v-if="detail?.parsed_data" class="space-y-2 text-sm">
            <div class="flex gap-2"><dt class="text-on-surface-variant shrink-0">姓名</dt><dd class="font-bold">{{ detail.parsed_data.name || '—' }}</dd></div>
            <div class="flex gap-2"><dt class="text-on-surface-variant shrink-0">目标岗位</dt><dd>{{ detail.parsed_data.target_role || '—' }}</dd></div>
            <div class="flex gap-2"><dt class="text-on-surface-variant shrink-0">工作年限</dt><dd>{{ detail.parsed_data.experience_years ?? '—' }}</dd></div>
          </dl>
          <p v-else class="text-sm text-on-surface-variant">上传并等待分析完成后将显示结构化字段。</p>

          <h4 class="text-sm font-bold mt-6 mb-3">技能标签</h4>
          <div class="flex flex-wrap gap-2">
            <span
              v-for="(s, idx) in extractedSkills"
              :key="idx"
              class="px-3 py-1 rounded-full text-xs font-bold bg-primary-fixed text-on-primary-fixed-variant"
            >
              {{ s }}
            </span>
            <span v-if="!extractedSkills.length" class="text-xs text-on-surface-variant">暂无</span>
          </div>
        </div>

        <div v-if="detail?.highlights?.length" class="bg-surface-container-low rounded-xl p-6 shadow-sm">
          <h3 class="text-lg font-bold mb-3 flex items-center gap-2">
            <span class="material-symbols-outlined text-amber-600">star</span>
            亮点
          </h3>
          <ul class="list-disc list-inside text-sm space-y-2 text-on-surface-variant">
            <li v-for="(h, i) in detail.highlights" :key="i" class="leading-relaxed">{{ h }}</li>
          </ul>
        </div>

        <div class="bg-tertiary-container text-on-tertiary-container rounded-xl p-6 shadow-sm">
          <div class="flex items-center gap-3 mb-4">
            <span class="material-symbols-outlined p-2 bg-tertiary text-on-tertiary rounded-lg">lightbulb</span>
            <h3 class="text-xl font-bold">优化建议</h3>
          </div>
          <ul v-if="suggestionRows.length" class="space-y-3">
            <li v-for="(r, i) in suggestionRows" :key="i" class="bg-surface-container-lowest/20 backdrop-blur-md p-4 rounded-xl border border-white/10">
              <span class="text-[10px] font-bold uppercase tracking-wider opacity-70">{{ r.typeLabel }}</span>
              <p class="text-sm mt-1 leading-relaxed">{{ r.content }}</p>
            </li>
          </ul>
          <p v-else class="text-sm opacity-80">暂无建议或分析未完成。</p>
        </div>

        <div class="bg-surface-container-lowest rounded-xl p-6 shadow-sm">
          <h3 class="text-xl font-bold mb-4">教育经历</h3>
          <div v-if="educationRows.length" class="space-y-4">
            <div v-for="(ed, i) in educationRows" :key="i" class="border-l-2 border-primary-fixed pl-4">
              <h4 class="font-bold">{{ ed.school || '—' }}</h4>
              <p class="text-xs text-on-surface-variant">{{ ed.major }} · {{ ed.degree }} · {{ ed.period }}</p>
            </div>
          </div>
          <p v-else class="text-sm text-on-surface-variant">暂无</p>
        </div>

        <div class="bg-surface-container-lowest rounded-xl p-6 shadow-sm">
          <h3 class="text-xl font-bold mb-4">工作经历</h3>
          <div v-if="experienceRows.length" class="space-y-6">
            <div
              v-for="(e, i) in experienceRows"
              :key="i"
              class="group relative pl-6 border-l-2 border-primary-fixed"
            >
              <div class="absolute -left-[9px] top-0 w-4 h-4 rounded-full border-4 border-surface-container-lowest bg-primary" />
              <h4 class="font-bold text-on-surface">{{ e.position || '—' }}</h4>
              <p class="text-xs text-on-surface-variant">{{ e.company }} · {{ e.period }}</p>
              <p class="text-sm text-on-surface-variant leading-relaxed mt-2">{{ e.description || '' }}</p>
            </div>
          </div>
          <p v-else class="text-sm text-on-surface-variant">暂无</p>
        </div>

        <div v-if="detail?.resume_file_path" class="bg-surface-container-lowest rounded-xl p-6 shadow-sm text-sm">
          <a :href="detail.resume_file_path" target="_blank" rel="noopener noreferrer" class="text-primary font-bold underline">打开 OSS 简历文件</a>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { isApiSuccess } from '@/api/client'
import {
  fetchResumeAnalysisPreview,
  getResumeAnalysis,
  isAnalysisComplete,
  listResumeAnalysis,
  normalizeResumeProgress,
  normalizeResumeStatus,
  pollResumeAnalysisUntilTerminal,
  uploadResume,
  type ResumeAnalysisDetail,
  type ResumeAnalysisListItem,
} from '@/api/resume'

const BANNER_MSG_WAIT = '请稍等，简历分析过程大约会耗时一分钟'
const BANNER_MSG_OK = '简历分析已完成，请查看下方解析结果。'

interface HistoryPanelFile {
  name: string
  fileType?: string
  url: string | null
}

const MAX_FILE_BYTES = 20 * 1024 * 1024

const fileInputEl = ref<HTMLInputElement | null>(null)
const selectedFileName = ref('')
/** 上传接口成功后保留本地 File，用于替换上传区展示与 PDF 预览 */
const resumeFileShown = ref<File | null>(null)
const pdfPreviewUrl = ref<string | null>(null)
/** 选中历史记录时展示（OSS 元数据，无本地 File） */
const historyPanelFile = ref<HistoryPanelFile | null>(null)
const selectedId = ref<string | null>(null)
const detail = ref<ResumeAnalysisDetail | null>(null)

const uploading = ref(false)
const polling = ref(false)
const bannerAfterUpload = ref(false)
/** waiting：分析中；success：完成；error：超时/失败；warning：已停止 */
const bannerPhase = ref<'waiting' | 'success' | 'error' | 'warning'>('waiting')
const bannerText = ref(BANNER_MSG_WAIT)

const uploadBannerClass = computed(() => {
  switch (bannerPhase.value) {
    case 'success':
      return 'border-emerald-200 bg-emerald-50 text-emerald-900 dark:bg-emerald-950/40 dark:border-emerald-800 dark:text-emerald-100'
    case 'error':
      return 'border-red-200 bg-red-50 text-red-800 dark:bg-red-950/35 dark:border-red-900 dark:text-red-200'
    case 'warning':
      return 'border-amber-200 bg-amber-50 text-amber-900 dark:bg-amber-950/40 dark:border-amber-800 dark:text-amber-100'
    default:
      return 'border-amber-200 bg-amber-50 text-amber-900 dark:bg-amber-950/40 dark:border-amber-800 dark:text-amber-100'
  }
})
const globalError = ref('')
const warningNotice = ref('')
const parsingProgress = ref(0)
const parsingStatusText = ref('等待上传简历…')

const historyItems = ref<ResumeAnalysisListItem[]>([])
const listNextCursor = ref<string | null>(null)
const loadingList = ref(false)

/** 历史记录：经 fetch + Blob 预览（iframe 无法带 token 头，不能直链 /preview） */
const historyPreviewBlobUrl = ref<string | null>(null)
const historyPreviewLoading = ref(false)
const historyPreviewHint = ref('')
let historyPreviewSeq = 0

const circumference = 2 * Math.PI * 58

watch(resumeFileShown, (f) => {
  if (pdfPreviewUrl.value) {
    URL.revokeObjectURL(pdfPreviewUrl.value)
    pdfPreviewUrl.value = null
  }
  if (f?.type === 'application/pdf') {
    pdfPreviewUrl.value = URL.createObjectURL(f)
  }
})

function revokeHistoryPreviewBlob() {
  if (historyPreviewBlobUrl.value) {
    URL.revokeObjectURL(historyPreviewBlobUrl.value)
    historyPreviewBlobUrl.value = null
  }
}

/** 与后端 ResumePreviewServiceImpl.INLINE_PREVIEW_TYPES 一致 */
function isInlinePreviewSupportedByBackend(name: string, fileType?: string): boolean {
  const ft = (fileType || '').trim().toLowerCase()
  if (['pdf', 'html', 'htm', 'txt'].includes(ft)) return true
  const n = name.toLowerCase()
  return n.endsWith('.pdf') || n.endsWith('.html') || n.endsWith('.htm') || n.endsWith('.txt')
}

watch(
  [selectedId, historyPanelFile],
  async ([id, panel]) => {
    revokeHistoryPreviewBlob()
    historyPreviewHint.value = ''
    if (!id || !panel) return
    if (!isInlinePreviewSupportedByBackend(panel.name, panel.fileType)) {
      historyPreviewHint.value =
        '该格式暂不支持内嵌预览（后端仅支持 PDF / HTML / TXT），请使用「下载原件」或 OSS 链接。'
      return
    }
    const seq = ++historyPreviewSeq
    historyPreviewLoading.value = true
    try {
      const r = await fetchResumeAnalysisPreview(id, 'inline')
      if (seq !== historyPreviewSeq) return
      if (r.ok) {
        historyPreviewBlobUrl.value = URL.createObjectURL(r.blob)
        historyPreviewHint.value = ''
      } else if (r.status === 415) {
        historyPreviewHint.value = '服务器不支持预览该文件类型。'
      } else if (r.status === 401) {
        historyPreviewHint.value = '未登录或会话已过期，无法加载预览。'
      } else if (r.status === 404) {
        historyPreviewHint.value = '未找到简历文件。'
      } else {
        historyPreviewHint.value = `预览加载失败（HTTP ${r.status}）。`
      }
    } finally {
      if (seq === historyPreviewSeq) historyPreviewLoading.value = false
    }
  },
  { deep: true }
)

onUnmounted(() => {
  if (pdfPreviewUrl.value) URL.revokeObjectURL(pdfPreviewUrl.value)
  revokeHistoryPreviewBlob()
})

const resumeFileIcon = computed(() => {
  const file = resumeFileShown.value
  if (!file) return 'description'
  const t = file.type
  if (t === 'application/pdf') return 'picture_as_pdf'
  if (t.includes('wordprocessing') || t.includes('msword') || file.name.toLowerCase().endsWith('.docx')) return 'article'
  if (t.includes('presentation') || file.name.toLowerCase().match(/\.pptx?$/)) return 'slideshow'
  if (t.includes('html') || file.name.toLowerCase().endsWith('.html') || file.name.toLowerCase().endsWith('.htm')) return 'html'
  if (t.startsWith('text/')) return 'notes'
  return 'description'
})

const resumeFileKindLabel = computed(() => {
  const file = resumeFileShown.value
  if (!file) return ''
  if (file.type === 'application/pdf') return 'PDF'
  if (file.name.toLowerCase().match(/\.docx?$/)) return 'Word'
  if (file.name.toLowerCase().match(/\.pptx?$/)) return 'PPT'
  if (file.name.toLowerCase().match(/\.html?$/)) return 'HTML'
  if (file.name.toLowerCase().endsWith('.txt') || file.type.startsWith('text/')) return '文本'
  return file.type || '文件'
})

const historyFileIcon = computed(() => {
  const h = historyPanelFile.value
  if (!h) return 'description'
  const n = h.name.toLowerCase()
  const ft = (h.fileType || '').toLowerCase()
  if (ft === 'pdf' || n.endsWith('.pdf')) return 'picture_as_pdf'
  if (ft.includes('word') || n.endsWith('.docx') || n.endsWith('.doc')) return 'article'
  if (ft.includes('ppt') || n.match(/\.pptx?$/)) return 'slideshow'
  if (n.endsWith('.html') || n.endsWith('.htm')) return 'html'
  if (n.endsWith('.txt')) return 'notes'
  return 'description'
})

const historyFileKindLabel = computed(() => {
  const h = historyPanelFile.value
  if (!h) return ''
  const ft = (h.fileType || '').toLowerCase()
  if (ft === 'pdf') return 'PDF'
  if (ft.includes('doc')) return 'Word'
  if (ft.includes('ppt')) return 'PPT'
  if (ft.includes('html')) return 'HTML'
  if (ft === 'txt') return '文本'
  if (h.fileType) return h.fileType
  return '简历附件'
})

async function downloadHistoryResume() {
  const id = selectedId.value
  if (!id) return
  globalError.value = ''
  const r = await fetchResumeAnalysisPreview(id, 'attachment')
  if (!r.ok) {
    globalError.value = r.status === 401 ? '请先登录' : `下载失败（HTTP ${r.status}）`
    return
  }
  const name = historyPanelFile.value?.name?.trim() || 'resume'
  const url = URL.createObjectURL(r.blob)
  const a = document.createElement('a')
  a.href = url
  a.download = name
  a.rel = 'noopener'
  a.click()
  URL.revokeObjectURL(url)
}

function formatFileSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function resetUploadBanner() {
  bannerAfterUpload.value = false
  bannerPhase.value = 'waiting'
  bannerText.value = BANNER_MSG_WAIT
}

function clearFilePanel() {
  resumeFileShown.value = null
  historyPanelFile.value = null
  selectedFileName.value = ''
  resetUploadBanner()
  if (fileInputEl.value) fileInputEl.value.value = ''
}

/** 进度条：优先使用接口 progress(0–100)；缺省时按状态兜底 */
function applyProgressFromDetail(d: ResumeAnalysisDetail | null | undefined) {
  const p = normalizeResumeProgress(d?.progress)
  if (p !== null) {
    parsingProgress.value = p
    return
  }
  const st = normalizeResumeStatus(d?.status as string | undefined)
  if (st === 'completed' || isAnalysisComplete(d ?? undefined)) {
    parsingProgress.value = 100
  } else if (st === 'failed' || st === 'stopped') {
    parsingProgress.value = 0
  } else if (st === 'pending' || st === 'processing') {
    parsingProgress.value = 5
  } else {
    parsingProgress.value = 0
  }
}

function chooseFile() {
  if (uploading.value || polling.value) return
  fileInputEl.value?.click()
}

function formatTime(iso?: string) {
  if (!iso) return '—'
  try {
    return new Date(iso).toLocaleString()
  } catch {
    return iso
  }
}

function suggestionTypeLabel(t?: string) {
  const m: Record<string, string> = {
    CONTENT: '内容',
    SKILL: '技能',
    LAYOUT: '排版',
  }
  return t ? m[t] || t : '建议'
}

const suggestionRows = computed(() => {
  const list = detail.value?.suggestions ?? []
  return list.map((s) => ({
    typeLabel: suggestionTypeLabel(s.type),
    content: s.content ?? '',
  }))
})

const educationRows = computed(() => detail.value?.parsed_data?.education ?? [])
const experienceRows = computed(() => detail.value?.parsed_data?.experience ?? [])
const extractedSkills = computed(() => detail.value?.parsed_data?.skills ?? [])

const competitivenessScore = computed(() => {
  const s = detail.value?.scores
  if (!s) return '—'
  const nums = [s.keyword_match, s.layout, s.skill_depth, s.experience].filter((n) => typeof n === 'number') as number[]
  if (!nums.length) return '—'
  return String(Math.round(nums.reduce((a, b) => a + b, 0) / nums.length))
})

const competitivenessNote = computed(() => {
  if (!detail.value?.scores) return '分析完成后将显示各维度评分。'
  return '基于关键词匹配、排版、技能深度与经历四项得分的平均值。'
})

const completenessPercent = computed(() => {
  const p = detail.value?.parsed_data
  if (!p) return 0
  let n = 0
  const total = 5
  if (p.name) n++
  if (p.target_role) n++
  if (p.skills?.length) n++
  if (p.education?.length) n++
  if (p.experience?.length) n++
  return Math.round((n / total) * 100)
})

const dashOffset = computed(() => ((100 - completenessPercent.value) / 100) * circumference)

const completenessItems = computed(() => {
  const p = detail.value?.parsed_data
  return [
    { label: '姓名 / 意向岗位', done: !!(p?.name && p?.target_role) },
    { label: '技能列表', done: !!(p?.skills?.length) },
    { label: '教育经历', done: !!(p?.education?.length) },
    { label: '工作经历', done: !!(p?.experience?.length) },
  ]
})

async function refreshList() {
  loadingList.value = true
  listNextCursor.value = null
  try {
    const r = await listResumeAnalysis({ limit: 20 })
    if (!isApiSuccess(r.code)) {
      globalError.value = r.msg || '获取分析列表失败'
      historyItems.value = []
      return
    }
    historyItems.value = r.data?.items ?? []
    listNextCursor.value = r.data?.next_cursor ?? null
  } catch (e) {
    globalError.value = e instanceof Error ? e.message : '网络错误'
    historyItems.value = []
  } finally {
    loadingList.value = false
  }
}

async function loadMoreList() {
  if (!listNextCursor.value || loadingList.value) return
  loadingList.value = true
  try {
    const r = await listResumeAnalysis({ cursor: listNextCursor.value, limit: 20 })
    if (!isApiSuccess(r.code)) {
      globalError.value = r.msg || '加载更多失败'
      return
    }
    const more = r.data?.items ?? []
    historyItems.value = [...historyItems.value, ...more]
    listNextCursor.value = r.data?.next_cursor ?? null
  } catch (e) {
    globalError.value = e instanceof Error ? e.message : '网络错误'
  } finally {
    loadingList.value = false
  }
}

async function loadDetail(id: string) {
  globalError.value = ''
  warningNotice.value = ''
  selectedId.value = id
  resumeFileShown.value = null
  const row = historyItems.value.find((x) => x.vector_store_id === id)
  historyPanelFile.value = {
    name: row?.original_file_name || id,
    fileType: row?.file_type,
    url: row?.resume_file_path ?? null,
  }
  try {
    const r = await getResumeAnalysis(id)
    if (!isApiSuccess(r.code)) {
      globalError.value = r.msg || '获取分析详情失败'
      detail.value = null
      historyPanelFile.value = null
      return
    }
    detail.value = r.data ?? null
    const d = detail.value
    if (d) {
      historyPanelFile.value = {
        name: d.original_file_name || row?.original_file_name || id,
        fileType: d.file_type ?? row?.file_type,
        url: d.resume_file_path ?? row?.resume_file_path ?? null,
      }
    }
    applyProgressFromDetail(detail.value)
    const st = normalizeResumeStatus(detail.value?.status as string | undefined)
    const progHint = `当前进度 ${parsingProgress.value}%。`
    if (st === 'completed') {
      parsingStatusText.value = `分析已完成（completed）。${progHint}`
    } else if (st === 'failed') {
      parsingStatusText.value = `该记录分析失败（failed）。${progHint}`
      globalError.value = detail.value?.status_message || '简历分析失败，请稍后重试或重新上传。'
    } else if (st === 'stopped') {
      parsingStatusText.value = `该记录已停止分析（stopped）。${progHint}`
      warningNotice.value = detail.value?.status_message || '分析已停止，未生成完整结果。'
    } else if (st === 'pending' || st === 'processing') {
      parsingStatusText.value =
        st === 'pending'
          ? `该记录排队中（pending）。${progHint}可稍后点击刷新。`
          : `该记录分析处理中（processing）。${progHint}可稍后刷新。`
    } else if (isAnalysisComplete(detail.value ?? undefined)) {
      parsingStatusText.value = `已加载解析结果（未返回 status 时的兼容）。${progHint}`
    } else {
      parsingStatusText.value = `暂无明确状态或结果。${progHint}`
    }
  } catch (e) {
    globalError.value = e instanceof Error ? e.message : '网络错误'
    detail.value = null
    historyPanelFile.value = null
  }
}

async function onFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return

  globalError.value = ''
  warningNotice.value = ''
  resetUploadBanner()
  if (file.size > MAX_FILE_BYTES) {
    globalError.value = '文件超过 20MB 限制'
    selectedFileName.value = ''
    return
  }

  selectedFileName.value = file.name
  uploading.value = true
  parsingProgress.value = 5
  parsingStatusText.value = '正在上传文件…'
  try {
    const r = await uploadResume(file)
    if (!isApiSuccess(r.code) || !r.data?.id) {
      globalError.value = r.msg || '上传失败'
      uploading.value = false
      return
    }
    resumeFileShown.value = file
    historyPanelFile.value = null
    bannerPhase.value = 'waiting'
    bannerText.value = BANNER_MSG_WAIT
    bannerAfterUpload.value = true
    selectedId.value = r.data.id
    parsingStatusText.value = '上传成功，正在等待分析结果（每 2 秒轮询，最多 60 秒）…'
    uploading.value = false
    polling.value = true
    parsingProgress.value = 15

    const pollResult = await pollResumeAnalysisUntilTerminal(r.data.id, {
      onTick: (attempt, d) => {
        const p = normalizeResumeProgress(d?.progress)
        if (p !== null) {
          parsingProgress.value = p
        } else {
          parsingProgress.value = Math.min(95, 15 + Math.floor((attempt / 30) * 80))
        }
        const st = normalizeResumeStatus(d?.status as string | undefined)
        const pct = parsingProgress.value
        if (st === 'pending') {
          parsingStatusText.value = `排队中（pending），进度 ${pct}%… 第 ${attempt}/30 次`
        } else if (st === 'processing') {
          parsingStatusText.value = `分析处理中（processing），进度 ${pct}%… 第 ${attempt}/30 次`
        } else {
          parsingStatusText.value = `等待任务状态，进度 ${pct}%… 第 ${attempt}/30 次`
        }
      },
    })

    polling.value = false

    if (pollResult.kind === 'timeout') {
      bannerAfterUpload.value = true
      bannerPhase.value = 'error'
      bannerText.value =
        '分析超时，请稍后重试。您可在历史记录中点击该项稍后查看。'
      globalError.value = ''
      parsingStatusText.value = `已停止轮询（当前进度 ${parsingProgress.value}%）。您可在历史记录中点击该项稍后查看。`
      await refreshList()
      return
    }

    detail.value = pollResult.detail ?? null
    applyProgressFromDetail(detail.value)

    if (pollResult.kind === 'completed') {
      bannerAfterUpload.value = true
      bannerPhase.value = 'success'
      bannerText.value = BANNER_MSG_OK
      warningNotice.value = ''
      globalError.value = ''
      parsingStatusText.value = `分析已完成（completed），进度 ${parsingProgress.value}%。以下为解析结果。`
    } else if (pollResult.kind === 'failed') {
      bannerAfterUpload.value = true
      bannerPhase.value = 'error'
      bannerText.value =
        pollResult.detail?.status_message || '简历分析失败，请稍后重试或重新上传。'
      warningNotice.value = ''
      parsingStatusText.value = `分析失败（failed），进度 ${parsingProgress.value}%。`
      globalError.value = ''
    } else if (pollResult.kind === 'stopped') {
      bannerAfterUpload.value = true
      bannerPhase.value = 'warning'
      bannerText.value =
        pollResult.detail?.status_message || '分析已停止，未生成完整结果。'
      globalError.value = ''
      parsingStatusText.value = `分析已停止（stopped），进度 ${parsingProgress.value}%。`
      warningNotice.value = ''
    }

    await refreshList()
  } catch (err) {
    uploading.value = false
    polling.value = false
    globalError.value = err instanceof Error ? err.message : '网络错误'
  }
}

onMounted(() => {
  void refreshList()
})
</script>
