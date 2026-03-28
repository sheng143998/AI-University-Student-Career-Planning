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
        <button class="flex items-center gap-2 px-4 py-2.5 bg-primary-container text-white font-bold rounded-xl shadow-lg hover:shadow-xl transition-all" type="button">
          <span class="material-symbols-outlined text-[20px]">download</span>
          导出报告
        </button>
      </div>
    </header>

    <div class="grid grid-cols-12 gap-6">
      <aside class="col-span-12 lg:col-span-3 order-last lg:order-none space-y-6">
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
      </aside>

      <main class="col-span-12 lg:col-span-9 space-y-8">
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
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

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

const reportCompleteness = ref(85)
const completenessChecklist = ref([
  { label: '自我认知已完成', icon: 'check_circle', iconClass: 'text-green-500' },
  { label: '发展路线已规划', icon: 'check_circle', iconClass: 'text-green-500' },
  { label: '行动计划待细化', icon: 'error', iconClass: 'text-orange-400' }
])

const cover = ref({
  title: '高级互联网产品经理',
  subtitle: '2024 - 2026 个人职业发展战略蓝图',
  tags: ['战略管理', 'AI 赋能', '全域增长']
})

const capabilityScores = ref([
  { label: '战略思维', value: 92 },
  { label: '产品设计', value: 88 },
  { label: '团队管理', value: 75 }
])

const personality = ref({
  type: 'ENTP',
  desc: '天生的开拓者与创新者。具备极强的逻辑分析能力与洞察力，能够快速识别复杂系统中的关键漏洞。在快速变化的互联网环境中展现出卓越的适应性，但在执行细节层面仍有提升空间。'
})

const matchPercent = ref(75)
const matchCircumference = 2 * Math.PI * 88
const matchDashOffset = computed(() => ((100 - matchPercent.value) / 100) * matchCircumference)

const matchHighlights = ref('用户洞察, 敏捷开发, 架构理解')
const matchGaps = ref('财务预算管理, AIGC 模型微调')
const matchQuote = ref('“通过对互联网大厂 12,000+ 岗位描述的深度学习，我们认为您当前具备冲击 P7 级岗位的能力储备，核心增长点在于提升商业闭环的设计能力。”')

const stages = ref([
  {
    period: '第一阶段: 巩固与深挖 (0-6个月)',
    title: '资深产品经理 (Senior PM)',
    desc: '主攻行业深度认知，负责千万级 DAU 产品的核心模块，建立标准化的产品生命周期管理流程。',
    dotClass: 'bg-primary-container',
    icon: 'star',
    iconClass: 'text-white',
    labelClass: 'text-primary',
    fill: true
  },
  {
    period: '第二阶段: 跨界与赋能 (1-2年)',
    title: '产品负责人 (Product Lead)',
    desc: '带领 5-10 人团队，跨部门协同实现业务增长。将 AI 能力整合进现有流程，提升团队产研效率 30% 以上。',
    dotClass: 'bg-tertiary',
    icon: 'rocket_launch',
    iconClass: 'text-white',
    labelClass: 'text-tertiary',
    fill: true
  },
  {
    period: '愿景目标 (3-5年)',
    title: '产品总监 / 创业合伙人',
    desc: '定义产品愿景，决定公司级战略方向，构建可持续发展的商业化生态。',
    dotClass: 'bg-surface-container-high',
    icon: 'mountain_flag',
    iconClass: 'text-outline',
    labelClass: 'text-on-surface-variant',
    fill: false
  }
])

const learningPlan = ref([
  { title: 'Python 数据科学', desc: '3个月 - 获取 Coursera 认证' },
  { title: '商业分析与建模', desc: '持续 6 个月 - 复旦管院公开课' }
])

const practicePlan = ref([
  { icon: 'assignment_ind', title: '企业导师计划', desc: '每周一次 1-on-1 深度辅导' },
  { icon: 'groups', title: '开源项目贡献', desc: '参与一个 AIGC 社区项目迭代' }
])
</script>
