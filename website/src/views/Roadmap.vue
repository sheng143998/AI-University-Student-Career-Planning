<template>
  <div class="space-y-8">
    <header class="flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
      <div class="space-y-1">
        <h1 class="text-4xl font-extrabold font-headline tracking-tight text-on-surface">职业路径探索</h1>
        <p class="text-on-surface-variant text-sm">基于AI算法的行业人才流动与晋升图谱</p>
      </div>

      <div class="flex flex-wrap items-center gap-4">
        <div class="relative">
          <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-outline">search</span>
          <input
            v-model="query"
            class="pl-10 pr-4 py-2.5 w-64 bg-surface-container-highest border-none rounded-xl focus:ring-2 focus:ring-tertiary-fixed-dim focus:ring-offset-0 text-sm outline-none"
            placeholder="搜索职业节点..."
            type="text"
          />
        </div>

        <div class="flex items-center bg-surface-container-low rounded-xl p-1">
          <button
            v-for="s in segments"
            :key="s"
            type="button"
            class="px-4 py-1.5 text-xs font-semibold rounded-lg transition-colors"
            :class="activeSegment === s ? 'bg-white shadow-sm text-primary' : 'text-on-surface-variant hover:bg-surface-container-high'"
            @click="activeSegment = s"
          >
            {{ s }}
          </button>
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
import { computed, ref } from 'vue'

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

const nodes = ref<RoadmapNode[]>([
  {
    id: 'visual',
    label: '视觉设计师',
    kind: 'secondary',
    icon: 'brush',
    iconClass: 'text-outline',
    pos: { left: '6%', top: '70%' },
    searchable: '视觉设计师 visual designer',
    segment: '互联网',
    tagline: '视觉表达与品牌一致性',
    description: '聚焦视觉体系、品牌与视觉表现，支撑产品设计语言的一致性。',
    skills: [
      { name: '视觉规范', value: 'Design System' },
      { name: '工具', value: 'Figma / PS' },
      { name: '表达', value: 'Motion' },
      { name: '协作', value: 'Hand-off' }
    ],
    salaryBands: [
      { label: '初级', range: '8k-15k', width: '33%', barClass: 'bg-primary-fixed-dim' },
      { label: '资深', range: '20k-35k', width: '60%', barClass: 'bg-primary' },
      { label: '专家', range: '50k+', width: '100%', barClass: 'bg-tertiary' }
    ]
  },
  {
    id: 'ui',
    label: 'UI 设计师',
    subLabel: undefined,
    kind: 'core',
    icon: 'grid_view',
    iconClass: 'text-white',
    levelLabel: 'L1',
    pos: { left: '20%', top: '45%' },
    searchable: 'ui 设计师 ui designer',
    segment: '互联网',
    tagline: '用户界面与视觉交互专家',
    description: '负责产品的界面设计、交互原型制作及视觉风格定义。需要紧密配合产品经理和前端工程师，确保设计稿的高质量还原与极致用户体验。',
    skills: [
      { name: '设计工具', value: 'Figma / PS' },
      { name: '交互逻辑', value: 'Prototyping' },
      { name: '审美', value: 'Visual Theory' },
      { name: '协作', value: 'Hand-off' }
    ],
    salaryBands: [
      { label: '初级', range: '8k-15k', width: '33%', barClass: 'bg-primary-fixed-dim' },
      { label: '资深', range: '20k-35k', width: '60%', barClass: 'bg-primary' },
      { label: '专家', range: '50k+', width: '100%', barClass: 'bg-tertiary' }
    ]
  },
  {
    id: 'ux',
    label: 'UX 研究员',
    subLabel: 'L2 进阶路径',
    kind: 'secondary',
    icon: 'psychology',
    iconClass: 'text-secondary',
    pos: { left: '42%', top: '22%' },
    searchable: 'ux 研究员 ux researcher',
    segment: '互联网',
    tagline: '洞察用户与验证假设',
    description: '通过定性/定量研究方法洞察用户需求，建立可验证的产品与设计决策依据。',
    skills: [
      { name: '研究方法', value: 'Interview' },
      { name: '数据', value: 'Analytics' },
      { name: '实验', value: 'A/B Test' },
      { name: '表达', value: 'Storytelling' }
    ],
    salaryBands: [
      { label: '初级', range: '10k-18k', width: '35%', barClass: 'bg-primary-fixed-dim' },
      { label: '资深', range: '22k-38k', width: '65%', barClass: 'bg-primary' },
      { label: '专家', range: '55k+', width: '100%', barClass: 'bg-tertiary' }
    ]
  },
  {
    id: 'interaction',
    label: '交互设计师',
    kind: 'secondary',
    icon: 'gesture',
    iconClass: 'text-secondary',
    pos: { left: '54%', top: '65%' },
    searchable: '交互设计师 interaction designer',
    segment: '互联网',
    tagline: '流程与体验打磨',
    description: '聚焦关键流程、动效与反馈机制，提升可用性与体验一致性。',
    skills: [
      { name: '信息架构', value: 'IA' },
      { name: '动效', value: 'Motion' },
      { name: '可用性', value: 'Usability' },
      { name: '交付', value: 'Spec' }
    ],
    salaryBands: [
      { label: '初级', range: '9k-16k', width: '33%', barClass: 'bg-primary-fixed-dim' },
      { label: '资深', range: '20k-34k', width: '60%', barClass: 'bg-primary' },
      { label: '专家', range: '48k+', width: '100%', barClass: 'bg-tertiary' }
    ]
  },
  {
    id: 'product-lead',
    label: '产品负责人',
    subLabel: '战略决策层',
    kind: 'secondary',
    icon: 'rocket_launch',
    iconClass: 'text-tertiary',
    pos: { left: '76%', top: '30%' },
    searchable: '产品负责人 product lead',
    segment: '互联网',
    tagline: '业务与战略增长',
    description: '负责产品战略规划与团队协作，推动关键指标增长与跨团队资源整合。',
    skills: [
      { name: '战略', value: 'Strategy' },
      { name: '增长', value: 'Growth' },
      { name: '管理', value: 'Leadership' },
      { name: '商业', value: 'Business' }
    ],
    salaryBands: [
      { label: '初级', range: '18k-28k', width: '40%', barClass: 'bg-primary-fixed-dim' },
      { label: '资深', range: '30k-50k', width: '70%', barClass: 'bg-primary' },
      { label: '核心', range: '60k+', width: '100%', barClass: 'bg-tertiary' }
    ]
  }
])

const paths = ref<Path[]>([
  { d: 'M 180 300 Q 250 250 320 220', variant: 'primary', mode: 'vertical', segment: '互联网' },
  { d: 'M 440 200 Q 550 180 660 210', variant: 'primary', mode: 'vertical', segment: '互联网' },
  { d: 'M 180 320 Q 300 380 420 400', variant: 'secondary', mode: 'lateral', segment: '互联网' },
  { d: 'M 50 450 Q 100 400 120 340', variant: 'secondary', mode: 'lateral', segment: '互联网' }
])

const activeNodeId = ref<string | null>('ui')
const activeNode = computed(() => nodes.value.find((n) => n.id === activeNodeId.value) ?? null)

const visibleNodes = computed(() => {
  const q = query.value.trim().toLowerCase()
  return nodes.value.filter((n) => {
    if (n.segment !== activeSegment.value) return false
    if (!q) return true
    return n.searchable.toLowerCase().includes(q)
  })
})

const visiblePaths = computed(() => {
  const mode = verticalMode.value ? 'vertical' : 'lateral'
  return paths.value.filter((p) => p.segment === activeSegment.value && p.mode === mode)
})

const aiChip = computed(() => {
  if (activeSegment.value !== '互联网') return null
  return 'AI 建议：UX研究员目前需求量增长 24%'
})

function selectNode(id: string) {
  activeNodeId.value = id
}

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
