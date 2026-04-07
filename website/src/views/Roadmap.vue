<template>
  <div class="space-y-8">
    <header class="flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
      <div class="space-y-1">
        <h1 class="text-4xl font-extrabold font-headline tracking-tight text-on-surface">职业路径探索</h1>
        <p class="text-on-surface-variant text-sm">基于AI算法的行业人才流动与晋升图谱</p>
      </div>

      <div class="flex flex-wrap items-center gap-4">
        <!-- 搜索框带下拉建议 -->
        <div class="relative" ref="searchContainer">
          <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-outline z-10">search</span>
          <input
            v-model="query"
            @input="onSearchInput"
            @focus="showSuggestions = query.length > 0"
            @keydown.enter="onSearchEnter"
            class="pl-10 pr-4 py-2.5 w-64 bg-surface-container-highest border-none rounded-xl focus:ring-2 focus:ring-tertiary-fixed-dim focus:ring-offset-0 text-sm outline-none"
            placeholder="搜索职业节点..."
            type="text"
          />
          <!-- 搜索建议下拉列表 -->
          <div v-if="showSuggestions && searchResults.length > 0" class="absolute top-full left-0 right-0 mt-2 bg-surface-container-highest rounded-xl shadow-lg border border-outline-variant/20 max-h-64 overflow-y-auto z-50">
            <div
              v-for="item in searchResults"
              :key="item.id"
              @click="selectSearchResult(item)"
              class="px-4 py-3 hover:bg-primary/10 cursor-pointer border-b border-outline-variant/10 last:border-b-0"
            >
              <div class="flex items-center justify-between">
                <div>
                  <div class="text-sm font-semibold text-on-surface">{{ item.title }}</div>
                  <div class="text-xs text-on-surface-variant">{{ item.subtitle }}</div>
                </div>
                <span v-if="item.tags && item.tags.length > 0" class="text-[10px] px-2 py-1 rounded-full bg-primary/20 text-primary">{{ item.tags[0] }}</span>
              </div>
            </div>
          </div>
          <!-- 无结果提示 -->
          <div v-if="showSuggestions && query.length > 0 && searchResults.length === 0 && !searchLoading" class="absolute top-full left-0 right-0 mt-2 bg-surface-container-highest rounded-xl shadow-lg border border-outline-variant/20 p-4 text-center text-sm text-on-surface-variant z-50">
            未找到相关职业
          </div>
        </div>

        <div class="flex items-center gap-2 bg-surface-container-low px-3 py-2 rounded-xl">
          <span class="text-xs font-medium text-on-surface-variant">纵向晋升</span>
          <button
            type="button"
            class="w-10 h-5 rounded-full relative transition-colors"
            :class="verticalMode ? 'bg-primary' : 'bg-surface-container-high'"
            @click="verticalMode = !verticalMode"
          >
            <div
              class="absolute top-1 w-3 h-3 bg-white rounded-full transition-transform"
              :class="verticalMode ? 'right-1' : 'left-1'"
            ></div>
          </button>
          <span class="text-xs font-medium text-on-surface-variant">横向转型</span>
        </div>
      </div>
    </header>

    <div class="relative w-full h-[600px] bg-surface-container-low rounded-[2rem] overflow-hidden border border-outline-variant/10 shadow-inner">
      <svg class="absolute inset-0 w-full h-full pointer-events-none">
        <path
          v-for="(p, idx) in visiblePaths"
          :key="idx"
          class="opacity-40"
          :d="p.d"
          fill="none"
          :stroke="p.variant === 'primary' ? '#0056d2' : '#c3c6d6'"
          :stroke-width="p.variant === 'primary' ? 2 : 1"
          :stroke-dasharray="p.variant === 'primary' ? '4' : '4'"
        ></path>
      </svg>

      <div class="absolute inset-0 p-10">
        <button
          v-for="n in visibleNodes"
          :key="n.id"
          type="button"
          class="absolute group"
          :style="{ left: n.pos.left, top: n.pos.top }"
          @click="selectNode(n.id)"
        >
          <div
            class="flex items-center justify-center shadow-lg transition-all"
            :class="nodeCardClass(n)"
          >
            <div v-if="n.kind === 'core'" class="flex flex-col items-center justify-center">
              <span class="material-symbols-outlined text-white text-3xl">{{ n.icon }}</span>
              <span class="text-[10px] font-bold text-white mt-1">{{ n.levelLabel }}</span>
            </div>
            <span v-else class="material-symbols-outlined" :class="n.iconClass">{{ n.icon }}</span>
          </div>
          <div class="mt-2 text-center min-w-[80px]">
            <span class="text-xs font-bold" :class="n.id === activeNodeId ? 'text-primary' : 'text-on-surface'">{{ n.label }}</span>
            <p v-if="n.subLabel" class="text-[10px] text-on-surface-variant">{{ n.subLabel }}</p>
          </div>
        </button>

        <div v-if="aiChip" class="absolute top-8 right-8">
          <div class="px-4 py-2 bg-primary-fixed text-primary text-xs font-bold rounded-full shadow-lg flex items-center gap-2">
            <span class="material-symbols-outlined text-sm">auto_awesome</span>
            {{ aiChip }}
          </div>
        </div>
      </div>

      <div
        class="absolute top-0 right-0 w-80 h-full bg-surface-variant/70 backdrop-blur-xl p-6 flex flex-col shadow-2xl border-l border-white/20"
        :class="activeNode ? '' : 'hidden lg:flex'"
      >
        <template v-if="activeNode">
          <div class="flex justify-between items-start mb-6">
            <div>
              <span class="px-2 py-0.5 bg-tertiary-fixed text-tertiary text-[10px] font-bold rounded-md uppercase tracking-wider">Active Focus</span>
              <h3 class="text-2xl font-black font-headline text-on-surface mt-1">{{ activeNode.label }}</h3>
              <p class="text-xs text-on-surface-variant">{{ activeNode.tagline }}</p>
            </div>
            <button class="p-1 hover:bg-white/50 rounded-lg" type="button" @click="activeNodeId = null">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>

          <div class="space-y-6 flex-1 overflow-y-auto pr-2">
            <section>
              <h4 class="text-xs font-bold text-tertiary mb-2 flex items-center gap-1">
                <span class="material-symbols-outlined text-sm">description</span>
                岗位描述
              </h4>
              <p class="text-sm text-on-surface-variant leading-relaxed">{{ activeNode.description }}</p>
            </section>

            <section>
              <h4 class="text-xs font-bold text-tertiary mb-3 flex items-center gap-1">
                <span class="material-symbols-outlined text-sm">bolt</span>
                核心技能要求
              </h4>
              <div class="grid grid-cols-2 gap-2">
                <div v-for="s in activeNode.skills" :key="s.name" class="p-2 bg-white/60 rounded-lg">
                  <p class="text-[10px] text-on-surface-variant">{{ s.name }}</p>
                  <p class="text-xs font-bold">{{ s.value }}</p>
                </div>
              </div>
            </section>

            <section>
              <h4 class="text-xs font-bold text-tertiary mb-3 flex items-center gap-1">
                <span class="material-symbols-outlined text-sm">trending_up</span>
                薪资晋升路径
              </h4>
              <div class="space-y-3">
                <div v-for="b in activeNode.salaryBands" :key="b.label" class="flex items-center gap-4">
                  <div class="w-10 text-[10px] text-on-surface-variant">{{ b.label }}</div>
                  <div class="flex-1 h-2 bg-surface-container rounded-full overflow-hidden">
                    <div class="h-full" :class="b.barClass" :style="{ width: b.width }"></div>
                  </div>
                  <div class="text-xs font-bold">{{ b.range }}</div>
                </div>
              </div>
            </section>
          </div>

          <div class="mt-6">
            <button class="w-full py-3 bg-gradient-to-r from-primary to-primary-container text-white font-bold rounded-xl shadow-lg hover:shadow-primary/20 transition-all flex items-center justify-center gap-2" type="button">
              定制我的进阶计划
              <span class="material-symbols-outlined text-sm">arrow_forward</span>
            </button>
          </div>
        </template>
      </div>
    </div>

    <div class="hidden md:flex justify-between items-start gap-6">
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6 flex-1">
        <div class="bg-surface-container-lowest p-6 rounded-[1.5rem] shadow-sm flex flex-col gap-4">
          <div class="flex items-center justify-between">
            <span class="text-xs font-bold text-tertiary uppercase">Market Heat</span>
            <span class="material-symbols-outlined text-tertiary-fixed-dim">local_fire_department</span>
          </div>
          <div>
            <p class="text-3xl font-black font-headline">高热度</p>
            <p class="text-sm text-on-surface-variant mt-1">互联网设计类岗位需求稳步上升</p>
          </div>
          <div class="mt-2 h-24 bg-surface-container-low rounded-xl overflow-hidden relative">
            <div class="absolute bottom-0 left-0 w-full h-1/2 bg-gradient-to-t from-primary/10 to-transparent"></div>
            <div class="absolute bottom-4 left-4 right-4 flex items-end gap-1 h-12">
              <div v-for="(h, i) in heatBars" :key="i" class="flex-1 rounded-t" :class="h.class" :style="{ height: h.height }"></div>
            </div>
          </div>
        </div>

        <div class="md:col-span-2 bg-primary-container p-6 rounded-[1.5rem] shadow-lg text-white relative overflow-hidden">
          <div class="absolute -right-10 -bottom-10 opacity-10">
            <span class="material-symbols-outlined text-[12rem]">query_stats</span>
          </div>
          <div class="relative z-10 flex flex-col h-full justify-between">
            <div>
              <div class="flex items-center gap-2 mb-4">
                <span class="px-2 py-1 bg-white/20 rounded-md text-[10px] font-bold">AI PATH INSIGHT</span>
              </div>
              <h3 class="text-2xl font-bold font-headline mb-2 leading-tight">从 UI 设计师到 产品负责人 的黄金路径</h3>
              <p class="text-sm text-primary-fixed max-w-md">数据显示，拥有 3 年 UI 经验的从业者，如果增加“用户研究”与“商业逻辑”技能点，转型产品岗的成功率提高 72%。</p>
            </div>
            <div class="mt-6 flex gap-4">
              <button class="px-6 py-2 bg-white text-primary rounded-lg font-bold text-sm hover:scale-95 transition-all" type="button">查看学习路径</button>
              <button class="px-6 py-2 border border-white/30 rounded-lg font-bold text-sm hover:bg-white/10 transition-all" type="button">岗位推荐</button>
            </div>
          </div>
        </div>
      </div>

      <div class="hidden lg:flex bg-white/80 backdrop-blur-md px-4 py-2 rounded-full shadow-lg border border-outline-variant/30 items-center gap-6 h-fit">
        <div class="flex items-center gap-2">
          <div class="w-3 h-3 rounded-full bg-primary-container"></div>
          <span class="text-[10px] font-bold text-on-surface-variant">核心节点</span>
        </div>
        <div class="flex items-center gap-2">
          <div class="w-3 h-3 rounded-full bg-white border border-outline-variant"></div>
          <span class="text-[10px] font-bold text-on-surface-variant">次要节点</span>
        </div>
        <div class="flex items-center gap-2">
          <div class="w-6 h-0.5 bg-primary opacity-40 border-t border-dashed"></div>
          <span class="text-[10px] font-bold text-on-surface-variant">晋升路径</span>
        </div>
        <div class="flex items-center gap-2">
          <div class="w-6 h-0.5 bg-outline-variant border-t border-dashed"></div>
          <span class="text-[10px] font-bold text-on-surface-variant">转型参考</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'

import { getRoadmapGraph, getRoadmapNodeDetail, isApiSuccess, searchRoadmapNodes, type RoadmapGraph, type RoadmapNodeDetail, type RoadmapSearchItem } from '@/api/roadmap'

type Segment = '互联网' | '金融科技' | '人工智能'

type NodeKind = 'core' | 'secondary'

type RoadmapNode = {
  id: string
  label: string
  subLabel?: string
  kind: NodeKind
  icon: string
  iconClass: string
  levelLabel?: string
  pos: { left: string; top: string }
  searchable: string
  segment: Segment
  tagline: string
  description: string
  skills: Array<{ name: string; value: string }>
  salaryBands: Array<{ label: string; range: string; width: string; barClass: string }>
}

type Path = {
  d: string
  variant: 'primary' | 'secondary'
  mode: 'vertical' | 'lateral'
  segment: Segment
}

const segments: Segment[] = ['互联网', '金融科技', '人工智能']
const activeSegment = ref<Segment>('互联网')
const verticalMode = ref(true)
const query = ref('')
const searchResults = ref<RoadmapSearchItem[]>([])
const showSuggestions = ref(false)
const searchLoading = ref(false)
const searchContainer = ref<HTMLElement | null>(null)
let searchDebounceTimer: ReturnType<typeof setTimeout> | null = null

const nodes = ref<RoadmapNode[]>([])
const paths = ref<Path[]>([])

const activeNodeId = ref<string | null>(null)
const currentNodeDetail = ref<RoadmapNodeDetail | null>(null)

const activeNode = computed(() => {
  if (!activeNodeId.value) return null

  const base = nodes.value.find((n) => n.id === activeNodeId.value)
  if (!base) return null

  const detail = currentNodeDetail.value
  if (!detail || detail.id !== base.id) return base

  const salaryRange = detail.salaryRange || ''
  const salaryBands = salaryRange
    ? [
        { label: detail.levelName || '', range: salaryRange, width: '70%', barClass: 'bg-primary' }
      ]
    : []

  const skills = (detail.requirements || []).slice(0, 4).map((s) => ({ name: s, value: '' }))

  return {
    ...base,
    tagline: detail.levelName || base.tagline,
    description: detail.summary || base.description,
    skills,
    salaryBands
  }
})

function generatePathD(from: { x: number; y: number }, to: { x: number; y: number }) {
  const dx = Math.abs(to.x - from.x)
  const cx1 = from.x + dx * 0.5
  const cy1 = from.y
  const cx2 = to.x - dx * 0.5
  const cy2 = to.y
  return `M ${from.x} ${from.y} C ${cx1} ${cy1}, ${cx2} ${cy2}, ${to.x} ${to.y}`
}

function toRoadmapNode(n: RoadmapGraph['nodes'][number], index: number): RoadmapNode {
  const kind: NodeKind = index === 0 ? 'core' : 'secondary'
  const icon = kind === 'core' ? 'grid_view' : 'work'
  const iconClass = kind === 'core' ? 'text-white' : 'text-outline'
  const levelLabel = kind === 'core' ? 'L1' : undefined

  return {
    id: n.id,
    label: n.label || n.title,
    subLabel: n.subLabel || n.subtitle,
    kind,
    icon,
    iconClass,
    levelLabel,
    pos: { left: `${n.x}px`, top: `${n.y}px` },
    searchable: `${n.title} ${n.label} ${n.subtitle}`,
    segment: activeSegment.value,
    tagline: n.subtitle || '',
    description: '',
    skills: [],
    salaryBands: []
  }
}

async function loadGraph() {
  console.log('[Roadmap] loadGraph called')
  const mode = verticalMode.value ? 'vertical' : 'lateral'
  console.log('[Roadmap] calling getRoadmapGraph, mode=', mode)
  try {
    const res = await getRoadmapGraph('', mode)
    console.log('[Roadmap] API response:', res)
    if (!isApiSuccess(res.code)) {
      console.log('[Roadmap] API failed, code:', res.code)
      nodes.value = []
      paths.value = []
      activeNodeId.value = null
      currentNodeDetail.value = null
      return
    }
    console.log('[Roadmap] API success, nodes:', res.data?.nodes?.length || 0)

    const graph = res.data
    console.log('[Roadmap] graph data:', graph)
    nodes.value = (graph.nodes || []).map((n, idx) => toRoadmapNode(n, idx))

    const nodeIndex = new Map<string, { x: number; y: number }>()
    for (const n of graph.nodes || []) {
      nodeIndex.set(n.id, { x: n.x, y: n.y })
    }

    paths.value = (graph.paths || []).map((p) => {
      const from = nodeIndex.get(p.from) || { x: 0, y: 0 }
      const to = nodeIndex.get(p.to) || { x: 0, y: 0 }
      return {
        d: generatePathD(from, to),
        variant: p.variant === 'primary' ? 'primary' : 'secondary',
        mode: verticalMode.value ? 'vertical' : 'lateral',
        segment: activeSegment.value
      }
    })

    activeNodeId.value = nodes.value.length ? nodes.value[0].id : null
    currentNodeDetail.value = null
    console.log('[Roadmap] nodes:', nodes.value.length, 'paths:', paths.value.length)
    if (activeNodeId.value) {
      await selectNode(activeNodeId.value)
    }
  } catch (e) {
    console.error('[Roadmap] loadGraph error:', e)
    nodes.value = []
    paths.value = []
  }
}

const visibleNodes = computed(() => {
  console.log('[Roadmap] visibleNodes computed, nodes:', nodes.value?.length)
  // 暂时直接返回所有节点，搜索功能后续完善
  return nodes.value || []
})

// 搜索输入防抖处理
function onSearchInput() {
  if (searchDebounceTimer) {
    clearTimeout(searchDebounceTimer)
  }
  if (!query.value || query.value.length < 2) {
    searchResults.value = []
    showSuggestions.value = false
    return
  }
  searchDebounceTimer = setTimeout(() => {
    performSearch()
  }, 300)
}

// 执行搜索
async function performSearch() {
  if (!query.value || query.value.length < 2) return
  searchLoading.value = true
  try {
    const res = await searchRoadmapNodes(query.value, 10)
    console.log('[Roadmap] search API response:', res)
    if (isApiSuccess(res.code)) {
      searchResults.value = res.data?.items || []
      console.log('[Roadmap] search results:', searchResults.value)
      showSuggestions.value = searchResults.value.length > 0
    }
  } catch (e) {
    console.error('[Roadmap] search error:', e)
    searchResults.value = []
  } finally {
    searchLoading.value = false
  }
}

// 选择搜索结果
async function selectSearchResult(item: RoadmapSearchItem) {
  console.log('[Roadmap] selected:', item)
  query.value = item.title
  showSuggestions.value = false
  
  // 使用 categoryCode 加载图谱
  if (item.categoryCode) {
    await loadGraphWithCategory(item.categoryCode)
  }
}

// 加载指定类别的图谱
async function loadGraphWithCategory(categoryCode: string) {
  const mode = verticalMode.value ? 'vertical' : 'lateral'
  try {
    const res = await getRoadmapGraph(categoryCode, mode)
    if (isApiSuccess(res.code) && res.data) {
      const graph = res.data
      nodes.value = (graph.nodes || []).map((n, idx) => toRoadmapNode(n, idx))
      
      const nodeIndex = new Map<string, { x: number; y: number }>()
      for (const n of graph.nodes || []) {
        nodeIndex.set(n.id, { x: n.x, y: n.y })
      }
      
      paths.value = (graph.paths || []).map((p) => {
        const from = nodeIndex.get(p.from) || { x: 0, y: 0 }
        const to = nodeIndex.get(p.to) || { x: 0, y: 0 }
        return {
          d: generatePathD(from, to),
          variant: p.variant === 'primary' ? 'primary' : 'secondary',
          mode: verticalMode.value ? 'vertical' : 'lateral',
          segment: activeSegment.value
        }
      })
      
      activeNodeId.value = nodes.value.length ? nodes.value[0].id : null
      if (activeNodeId.value) {
        await selectNode(activeNodeId.value)
      }
    }
  } catch (e) {
    console.error('[Roadmap] loadGraphWithCategory error:', e)
  }
}

// 点击外部关闭建议
function onClickOutside(e: MouseEvent) {
  if (searchContainer.value && !searchContainer.value.contains(e.target as Node)) {
    showSuggestions.value = false
  }
}

// 回车搜索
function onSearchEnter() {
  if (searchResults.value.length > 0) {
    selectSearchResult(searchResults.value[0])
  }
}

// 监听点击外部
if (typeof document !== 'undefined') {
  document.addEventListener('click', onClickOutside)
}

const visiblePaths = computed(() => {
  const mode = verticalMode.value ? 'vertical' : 'lateral'
  // TODO: 后端数据暂无行业分类，暂时不过滤 segment
  return paths.value.filter((p) => p.mode === mode)
})

const aiChip = computed(() => {
  if (activeSegment.value !== '互联网') return null
  return 'AI 建议：UX研究员目前需求量增长 24%'
})

function selectNode(id: string) {
  activeNodeId.value = id
  getRoadmapNodeDetail(id).then((res) => {
    if (!isApiSuccess(res.code)) return
    currentNodeDetail.value = res.data
  })
}

onMounted(async () => {
  await loadGraph()
})

watch([verticalMode, activeSegment], async () => {
  await loadGraph()
})

function nodeCardClass(n: RoadmapNode) {
  const active = n.id === activeNodeId.value
  if (n.kind === 'core') {
    return [
      'w-20 h-20 rounded-2xl bg-primary-container border-4 border-white',
      active ? 'shadow-xl ring-2 ring-primary-container' : 'shadow-xl'
    ].join(' ')
  }

  if (n.id === 'product-lead') {
    return [
      'w-24 h-24 rounded-3xl bg-white border border-outline-variant overflow-hidden relative',
      active ? 'ring-2 ring-primary-container shadow-xl' : ''
    ].join(' ')
  }

  return [
    'w-16 h-16 bg-white',
    n.id === 'interaction' || n.id === 'visual' ? 'rounded-full' : 'rounded-2xl',
    'border border-outline-variant',
    active ? 'ring-2 ring-primary-container shadow-xl' : ''
  ].join(' ')
}

const heatBars = [
  { height: '33%', class: 'bg-primary-fixed-dim' },
  { height: '50%', class: 'bg-primary-fixed-dim' },
  { height: '66%', class: 'bg-primary-fixed-dim' },
  { height: '100%', class: 'bg-primary' },
  { height: '83%', class: 'bg-primary-fixed-dim' }
]
</script>
