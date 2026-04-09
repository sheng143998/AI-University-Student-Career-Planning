<template>

  <div class="space-y-8">

    <header class="flex flex-col md:flex-row justify-between items-start md:items-end gap-6">

      <div>

        <nav class="flex gap-2 text-xs text-on-surface-variant mb-2">

          <span>职业报告</span>

          <span>/</span>

          <span class="text-primary font-semibold">2024 年度职业发展规划</span>

        </nav>

        <h1 class="text-4xl font-black font-headline tracking-tight text-on-surface">

          职业生涯发展报告

          <span class="text-primary-container font-medium text-lg ml-2">(Draft v2.4)</span>

        </h1>

      </div>



      <div class="flex gap-3">

        <button class="flex items-center gap-2 px-4 py-2.5 bg-surface-container-high text-on-secondary-container font-semibold rounded-xl hover:bg-surface-container-highest transition-all" type="button">

          <span class="material-symbols-outlined text-[20px]">check_circle</span>

          完整性检查

        </button>

        <button class="flex items-center gap-2 px-4 py-2.5 bg-surface-container-high text-on-secondary-container font-semibold rounded-xl hover:bg-surface-container-highest transition-all" type="button" @click="onGenerate" :disabled="loading || generating">

          <span class="material-symbols-outlined text-[20px]" :class="generating ? 'animate-spin' : ''">auto_awesome</span>

          {{ generating ? '生成中...' : '生成报告' }}

        </button>

        <button class="flex items-center gap-2 px-4 py-2.5 bg-primary-container text-white font-bold rounded-xl shadow-lg hover:shadow-xl transition-all disabled:opacity-60 disabled:cursor-not-allowed" type="button" @click="onDownload" :disabled="!reportDetail?.id || downloading">

          <span class="material-symbols-outlined text-[20px]">download</span>

          {{ downloading ? '导出中...' : '导出报告' }}

        </button>

      </div>

    </header>



    <section v-if="error" class="bg-red-50 text-red-700 border border-red-200 p-4 rounded-xl">

      {{ error }}

    </section>



    <section class="bg-surface-container-lowest p-6 rounded-2xl shadow-sm">

      <div class="space-y-6">

        <section class="relative h-64 rounded-xl overflow-hidden group">

          <div class="absolute inset-0 bg-gradient-to-r from-primary/90 to-transparent"></div>

          <div class="absolute inset-0 flex flex-col justify-center px-10 text-white">

            <h2 class="text-3xl font-black font-headline mb-2">{{ cover.title }}</h2>

            <p class="text-lg opacity-90 font-medium">{{ cover.subtitle }}</p>

            <div class="mt-6 flex flex-wrap gap-3">

              <span v-for="t in cover.tags" :key="t" class="px-3 py-1 bg-white/20 backdrop-blur-md rounded-full text-xs font-semibold">{{ t }}</span>

            </div>

          </div>

        </section>



        <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">

          <div class="bg-surface-container-lowest p-6 rounded-xl shadow-sm">

            <div class="flex items-center gap-2 mb-4">

              <span class="material-symbols-outlined text-primary">auto_awesome</span>

              <h3 class="font-headline font-bold text-on-surface">AI 智能分析</h3>

            </div>

            <div class="space-y-4">

              <div v-for="c in aiCards" :key="c.title" class="p-3 rounded-lg border-l-4" :class="c.boxClass">

                <p class="text-xs font-semibold mb-1" :class="c.titleClass">{{ c.title }}</p>

                <p class="text-sm text-on-surface-variant">{{ c.desc }}</p>

              </div>

            </div>

            <button class="w-full mt-6 py-3 bg-surface-container-low text-primary font-bold rounded-xl hover:bg-primary-fixed transition-colors flex items-center justify-center gap-2" type="button">

              <span class="material-symbols-outlined text-[18px]">temp_preferences_custom</span>

              AI 一键润色

            </button>

          </div>



          <div class="bg-surface-container-lowest p-6 rounded-xl shadow-sm">

            <h3 class="font-headline font-bold text-on-surface mb-4">报告完整度</h3>

            <div class="flex items-end gap-2 mb-2">

              <span class="text-3xl font-black text-primary">{{ reportCompleteness }}%</span>

              <span class="text-xs text-on-surface-variant pb-1">Excellent</span>

            </div>

            <div class="h-2 w-full bg-surface-container-high rounded-full overflow-hidden">

              <div class="h-full bg-primary-container rounded-full" :style="{ width: reportCompleteness + '%' }"></div>

            </div>

            <ul class="mt-6 space-y-3">

              <li v-for="i in completenessChecklist" :key="i.label" class="flex items-center gap-2 text-sm text-on-surface-variant">

                <span class="material-symbols-outlined text-[18px]" :class="i.iconClass">{{ i.icon }}</span>

                {{ i.label }}

              </li>

            </ul>

          </div>

        </div>

      </div>

    </section>



    <section class="bg-surface-container-lowest p-6 rounded-2xl shadow-sm">

      <div class="space-y-8">

        <section class="bg-surface-container-lowest p-10 rounded-xl shadow-sm">

          <div class="flex items-center gap-4 mb-8">

            <div class="h-12 w-1.5 bg-primary-container rounded-full"></div>

            <h2 class="text-2xl font-black font-headline">01 自我认知 <span class="text-sm font-normal text-on-surface-variant ml-4 uppercase tracking-widest">Self-Discovery</span></h2>

          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-10">

            <div>

              <h4 class="font-bold text-on-surface mb-4 flex items-center gap-2">

                <span class="material-symbols-outlined text-primary">psychology</span>

                核心能力象限

              </h4>

              <div class="space-y-4">

                <div v-for="s in capabilityScores" :key="s.label">

                  <div class="flex justify-between text-sm mb-1">

                    <span class="text-on-surface-variant">{{ s.label }}</span>

                    <span class="font-bold">{{ s.value }}%</span>

                  </div>

                  <div class="h-1.5 w-full bg-surface-container-low rounded-full">

                    <div class="h-full bg-primary-container rounded-full" :style="{ width: s.value + '%' }"></div>

                  </div>

                </div>

              </div>

            </div>

            <div class="bg-surface-container-low p-6 rounded-xl">

              <h4 class="font-bold text-on-surface mb-3">性格特质 ({{ personality.type }})</h4>

              <p class="text-sm text-on-surface-variant leading-relaxed">{{ personality.desc }}</p>

            </div>

          </div>

        </section>



        <section class="bg-surface-container-lowest p-10 rounded-xl shadow-sm">

          <div class="flex items-center gap-4 mb-8">

            <div class="h-12 w-1.5 bg-tertiary rounded-full"></div>

            <h2 class="text-2xl font-black font-headline">02 人岗匹配分析 <span class="text-sm font-normal text-on-surface-variant ml-4 uppercase tracking-widest">Match Analysis</span></h2>

          </div>

          <div class="bg-surface p-8 rounded-xl flex flex-col md:flex-row items-center gap-12">

            <div class="relative w-48 h-48 flex items-center justify-center">

              <svg class="w-full h-full transform -rotate-90">

                <circle class="text-surface-container-high" cx="96" cy="96" r="88" fill="transparent" stroke="currentColor" stroke-width="12" />

                <circle

                  class="text-tertiary-fixed-dim"

                  cx="96"

                  cy="96"

                  r="88"

                  fill="transparent"

                  stroke="currentColor"

                  stroke-width="12"

                  stroke-linecap="round"

                  :stroke-dasharray="matchCircumference"

                  :stroke-dashoffset="matchDashOffset"

                />

              </svg>

              <div class="absolute inset-0 flex flex-col items-center justify-center">

                <span class="text-4xl font-black font-headline text-on-surface">{{ matchPercent }}%</span>

                <span class="text-xs text-on-surface-variant">总体匹配度</span>

              </div>

            </div>

            <div class="flex-1 space-y-6">

              <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">

                <div class="p-4 bg-white rounded-lg">

                  <p class="text-xs text-on-surface-variant mb-1">技能契合点</p>

                  <p class="text-sm font-bold text-green-600">{{ matchHighlights }}</p>

                </div>

                <div class="p-4 bg-white rounded-lg">

                  <p class="text-xs text-on-surface-variant mb-1">潜在差距</p>

                  <p class="text-sm font-bold text-red-500">{{ matchGaps }}</p>

                </div>

              </div>

              <p class="text-sm text-on-surface-variant italic">{{ matchQuote }}</p>

            </div>

          </div>

        </section>



        <section class="bg-surface-container-lowest p-10 rounded-xl shadow-sm">

          <div class="flex items-center gap-4 mb-10">

            <div class="h-12 w-1.5 bg-secondary rounded-full"></div>

            <h2 class="text-2xl font-black font-headline">03 职业发展路线 <span class="text-sm font-normal text-on-surface-variant ml-4 uppercase tracking-widest">Roadmap</span></h2>

          </div>

          <div class="relative">

            <div class="absolute left-6 top-0 bottom-0 w-1 bg-surface-container-high rounded-full"></div>

            <div class="space-y-12">

              <div v-for="st in stages" :key="st.title" class="relative pl-16">

                <div class="absolute left-3 top-0 w-8 h-8 rounded-full border-4 border-white flex items-center justify-center z-10 shadow-md" :class="st.dotClass">

                  <span class="material-symbols-outlined text-[16px]" :class="st.iconClass" :style="st.fill ? 'font-variation-settings: \'FILL\' 1;' : ''">{{ st.icon }}</span>

                </div>

                <div>

                  <p class="text-xs font-bold mb-1" :class="st.labelClass">{{ st.period }}</p>

                  <h4 class="text-lg font-bold text-on-surface mb-2">{{ st.title }}</h4>

                  <p class="text-sm text-on-surface-variant">{{ st.desc }}</p>

                </div>

              </div>

            </div>

          </div>

        </section>



        <section class="bg-surface-container-lowest p-10 rounded-xl shadow-sm">

          <div class="flex items-center gap-4 mb-8">

            <div class="h-12 w-1.5 bg-primary rounded-full"></div>

            <h2 class="text-2xl font-black font-headline">04 行动计划 <span class="text-sm font-normal text-on-surface-variant ml-4 uppercase tracking-widest">Action Plan</span></h2>

          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-8">

            <div class="bg-primary-fixed/30 p-8 rounded-2xl">

              <h3 class="text-lg font-black font-headline mb-6 flex items-center gap-2">

                <span class="material-symbols-outlined text-primary">school</span>

                学习路径

              </h3>

              <div class="space-y-6">

                <div v-for="(p, idx) in learningPlan" :key="p.title" class="flex gap-4">

                  <div class="w-12 h-12 rounded-xl bg-white flex items-center justify-center shrink-0 shadow-sm">

                    <span class="text-primary font-bold text-xl">{{ idx + 1 }}</span>

                  </div>

                  <div>

                    <p class="font-bold text-sm text-on-surface">{{ p.title }}</p>

                    <p class="text-xs text-on-surface-variant">{{ p.desc }}</p>

                  </div>

                </div>

              </div>

            </div>



            <div class="bg-secondary-fixed/30 p-8 rounded-2xl">

              <h3 class="text-lg font-black font-headline mb-6 flex items-center gap-2">

                <span class="material-symbols-outlined text-secondary">engineering</span>

                实践安排

              </h3>

              <div class="space-y-6">

                <div v-for="a in practicePlan" :key="a.title" class="flex gap-4">

                  <div class="w-12 h-12 rounded-xl bg-white flex items-center justify-center shrink-0 shadow-sm">

                    <span class="material-symbols-outlined text-secondary">{{ a.icon }}</span>

                  </div>

                  <div>

                    <p class="font-bold text-sm text-on-surface">{{ a.title }}</p>

                    <p class="text-xs text-on-surface-variant">{{ a.desc }}</p>

                  </div>

                </div>

              </div>

            </div>

          </div>

        </section>



      </div>

    </section>

  </div>

</template>



<script setup lang="ts">

import { computed, onMounted, ref } from 'vue'

import { fetchReportPdf, generateReport, getLatestReport, getReportDetail, pollReportUntilReady, triggerBrowserDownload, type LatestReport, type ReportDetail } from '@/api/reports'



const aiCards = ref([

  {

    title: '优化建议',

    desc: '你的“自我认知”部分较为薄弱，建议补充具体的 MBTI 测评数据。',

    boxClass: 'bg-primary-fixed border-primary',

    titleClass: 'text-on-primary-fixed-variant'

  },

  {

    title: '匹配度警报',

    desc: '当前技能树与“资深产品经理”目标岗位的匹配度为 72%，需提升数据分析能力。',

    boxClass: 'bg-tertiary-fixed border-tertiary',

    titleClass: 'text-on-tertiary-fixed-variant'

  }

])



type AnyRecord = Record<string, any>

function getSection(detail: ReportDetail | null | undefined, key: string) {
  return detail?.sections?.find(s => s?.key === key)
}

const selfDiscovery = computed(() => {
  const sec = getSection(reportDetail.value, 'self_discovery')
  return (sec?.content ?? null) as AnyRecord | null
})

const matchAnalysis = computed(() => {
  const sec = getSection(reportDetail.value, 'match_analysis')
  return (sec?.content ?? null) as AnyRecord | null
})

const developmentPath = computed(() => {
  const sec = getSection(reportDetail.value, 'development_path')
  return (sec?.content ?? null) as AnyRecord | null
})

const actionPlanSection = computed(() => {
  const sec = getSection(reportDetail.value, 'action_plan')
  return (sec?.content ?? null) as AnyRecord | null
})

const reportCompleteness = computed(() => {
  const d = reportDetail.value
  if (!d?.sections?.length) return 0
  const keys = ['self_discovery', 'match_analysis', 'development_path', 'action_plan']
  const filled = keys.filter(k => {
    const sec = getSection(d, k)
    const c = sec?.content
    if (c == null) return false
    if (typeof c === 'string') return c.trim().length > 0
    if (Array.isArray(c)) return c.length > 0
    if (typeof c === 'object') return Object.keys(c as object).length > 0
    return true
  }).length
  return Math.round((filled / keys.length) * 100)
})

const completenessChecklist = computed(() => {
  const d = reportDetail.value
  const hasSelf = !!getSection(d, 'self_discovery')?.content
  const hasDev = !!getSection(d, 'development_path')?.content
  const hasAction = !!getSection(d, 'action_plan')?.content
  return [
    { label: hasSelf ? '自我认知已完成' : '自我认知暂无数据', icon: hasSelf ? 'check_circle' : 'error', iconClass: hasSelf ? 'text-green-500' : 'text-orange-400' },
    { label: hasDev ? '发展路线已规划' : '发展路线暂无数据', icon: hasDev ? 'check_circle' : 'error', iconClass: hasDev ? 'text-green-500' : 'text-orange-400' },
    { label: hasAction ? '行动计划已生成' : '行动计划暂无数据', icon: hasAction ? 'check_circle' : 'error', iconClass: hasAction ? 'text-green-500' : 'text-orange-400' },
  ]
})

const cover = computed(() => {
  const d = reportDetail.value
  const title = d?.targetJob?.name || latestReport.value?.targetJob || '暂无目标岗位（请先生成报告）'
  const industry = d?.targetJob?.industry ? String(d.targetJob.industry) : ''
  const city = d?.targetJob?.city ? String(d.targetJob.city) : ''
  const tags = [industry, city].filter(Boolean)
  return {
    title,
    subtitle: d?.generatedAt ? `生成时间：${d.generatedAt}` : '尚未生成报告',
    tags: tags.length ? tags : ['暂无标签'],
  }
})

const capabilityScores = computed(() => {
  const radar = selfDiscovery.value?.radar_chart
  if (!radar || typeof radar !== 'object') {
    return [{ label: '暂无数据', value: 0 }]
  }
  const entries = Object.entries(radar as Record<string, unknown>)
    .filter(([, v]) => typeof v === 'number')
    .slice(0, 6)
    .map(([k, v]) => ({ label: k, value: Math.max(0, Math.min(100, Math.round(v as number))) }))
  return entries.length ? entries : [{ label: '暂无数据', value: 0 }]
})

const personality = computed(() => {
  const summary = selfDiscovery.value?.capability_summary
  return {
    type: '—',
    desc: typeof summary === 'string' && summary.trim() ? summary : '暂无自我认知摘要（请先生成报告）'
  }
})

const matchPercent = computed(() => {
  const v = reportDetail.value?.matchScore
  if (typeof v !== 'number') return 0
  return Math.max(0, Math.min(100, Math.round(v)))
})

const matchCircumference = 2 * Math.PI * 88

const matchDashOffset = computed(() => ((100 - matchPercent.value) / 100) * matchCircumference)



const matchHighlights = computed(() => {
  const s = matchAnalysis.value?.summary
  return typeof s === 'string' && s.trim() ? s : '暂无匹配分析摘要（请先生成报告）'
})

const matchGaps = computed(() => {
  const gaps = matchAnalysis.value?.gap_analysis
  if (!Array.isArray(gaps) || gaps.length === 0) return '暂无差距分析'
  const titles = gaps.map((g: any) => g?.gap).filter((x: any) => typeof x === 'string' && x.trim())
  return titles.length ? titles.join('；') : '暂无差距分析'
})

const matchQuote = computed(() => {
  const dim = matchAnalysis.value?.dimension_analysis
  if (!dim || typeof dim !== 'object') return '暂无维度分析'
  return '维度分析已生成'
})



const stages = computed(() => {
  const steps = developmentPath.value?.steps
  if (!Array.isArray(steps) || steps.length === 0) {
    return [
      {
        period: '暂无发展路径（请先生成报告）',
        title: '—',
        desc: '',
        dotClass: 'bg-surface-container-high',
        icon: 'schedule',
        iconClass: 'text-outline',
        labelClass: 'text-on-surface-variant',
        fill: false,
      },
    ]
  }
  return steps.slice(0, 6).map((st: any, idx: number) => {
    const title = typeof st?.title === 'string' && st.title.trim() ? st.title : '—'
    const period = typeof st?.period === 'string' && st.period.trim() ? st.period : `阶段 ${idx + 1}`
    const desc = Array.isArray(st?.requirements) ? st.requirements.join('；') : ''
    const color = idx % 2 === 0 ? 'bg-primary-container' : 'bg-tertiary'
    const label = idx % 2 === 0 ? 'text-primary' : 'text-tertiary'
    return {
      period,
      title,
      desc,
      dotClass: color,
      icon: idx === 0 ? 'star' : 'rocket_launch',
      iconClass: 'text-white',
      labelClass: label,
      fill: true,
    }
  })
})



const learningPlan = computed(() => {
  const goals = actionPlanSection.value?.short_term_plan?.goals
  if (!Array.isArray(goals) || goals.length === 0) {
    return [{ title: '暂无学习计划（请先生成报告）', desc: '' }]
  }
  return goals.slice(0, 6).map((g: any) => ({
    title: typeof g?.title === 'string' && g.title.trim() ? g.title : '未命名目标',
    desc: g?.deadline ? `截止：${g.deadline}` : '',
  }))
})

const practicePlan = computed(() => {
  const goals = actionPlanSection.value?.short_term_plan?.goals
  if (!Array.isArray(goals) || goals.length === 0) {
    return [{ icon: 'info', title: '暂无实践安排（请先生成报告）', desc: '' }]
  }
  return goals.slice(0, 2).map((g: any) => ({
    icon: 'assignment_ind',
    title: typeof g?.title === 'string' && g.title.trim() ? g.title : '未命名安排',
    desc: typeof g?.status === 'string' && g.status.trim() ? `状态：${g.status}` : '',
  }))
})



const loading = ref(false)

const generating = ref(false)

const downloading = ref(false)

const error = ref<string | null>(null)



const latestReport = ref<LatestReport | null>(null)

const reportDetail = ref<ReportDetail | null>(null)



async function loadLatestAndDetail() {

  loading.value = true

  error.value = null

  try {

    const latest = await getLatestReport()

    if (latest.code !== 200 && latest.code !== 1) {

      error.value = latest.msg || '获取最新报告失败'

      latestReport.value = null

      reportDetail.value = null

      return

    }

    if (!latest.data) {

      latestReport.value = null

      reportDetail.value = null

      return

    }

    latestReport.value = latest.data

    const detail = await getReportDetail(latest.data.id)

    if (detail.code !== 200 && detail.code !== 1) {

      error.value = detail.msg || '获取报告详情失败'

      reportDetail.value = null

      return

    }

    reportDetail.value = detail.data || null

  } finally {

    loading.value = false

  }

}



async function onGenerate() {

  generating.value = true

  error.value = null

  try {

    const r = await generateReport({})

    if (r.code !== 200 && r.code !== 1) {

      error.value = r.msg || '生成报告失败'

      return

    }

    const reportId = r.data?.reportId

    if (!reportId) {

      await loadLatestAndDetail()

      return

    }

    const polled = await pollReportUntilReady(reportId, {

      intervalMs: 2000,

      maxAttempts: 60,

    })

    if (polled.kind === 'timeout') {

      error.value = '报告生成超时，请稍后刷新'

      return

    }

    if (polled.kind === 'failed') {

      error.value = '报告生成失败，请稍后重试'

      await loadLatestAndDetail()

      return

    }

    if (polled.detail?.id != null) {

      latestReport.value = {

        id: polled.detail.id,

        reportNo: polled.detail.reportNo,

        title: polled.detail.title,

        status: polled.detail.status,

        matchScore: polled.detail.matchScore,

        targetJob: polled.detail.targetJob?.name || '',

        generatedAt: polled.detail.generatedAt || '',

        updatedAt: polled.detail.updatedAt || '',

      }

    }

    reportDetail.value = polled.detail || null

  } finally {

    generating.value = false

  }

}



async function onDownload() {

  if (!reportDetail.value?.id) return

  downloading.value = true

  error.value = null

  try {

    const r = await fetchReportPdf(reportDetail.value.id)

    if (!r.ok) {

      error.value = r.status === 401 ? '未登录或会话已过期，请先登录' : `导出失败（HTTP ${r.status}）`

      return

    }

    triggerBrowserDownload(r.blob, '职业生涯发展报告.pdf')

  } finally {

    downloading.value = false

  }

}



onMounted(() => {

  void loadLatestAndDetail()

})

</script>

