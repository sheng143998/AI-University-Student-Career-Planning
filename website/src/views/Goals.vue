<template>
  <div class="space-y-10">
    <header class="flex flex-col md:flex-row md:items-end justify-between gap-6">
      <div>
        <h1 class="text-4xl font-extrabold font-headline tracking-tight text-on-surface mb-2">我的目标</h1>
        <p class="text-on-surface-variant max-w-lg">通过 AI 驱动的路径规划，将你的职业抱负转化为可执行的里程碑。</p>
      </div>
      <button class="flex items-center gap-2 bg-gradient-to-br from-primary to-primary-container text-white px-6 py-3 rounded-xl font-bold hover:opacity-90 transition-all shadow-lg shadow-primary/20" type="button">
        <span class="material-symbols-outlined">add</span>
        添加新目标
      </button>
    </header>

    <div class="grid grid-cols-12 gap-8">
      <div class="col-span-12 lg:col-span-8 space-y-8">
        <section class="bg-primary-container rounded-xl p-8 text-white relative overflow-hidden">
          <div class="relative z-10">
            <div class="flex flex-wrap items-center gap-2 mb-4">
              <span class="bg-white/20 px-3 py-1 rounded-full text-xs font-bold tracking-wider">首要目标</span>
              <span class="text-primary-fixed-dim text-xs font-medium">预计达成：{{ primaryGoal.eta }}</span>
            </div>
            <h2 class="text-3xl font-bold font-headline mb-6">{{ primaryGoal.title }}</h2>
            <div class="space-y-2">
              <div class="flex justify-between text-sm font-medium">
                <span>总体进度</span>
                <span>{{ primaryGoal.progress }}%</span>
              </div>
              <div class="w-full bg-white/20 h-3 rounded-full overflow-hidden">
                <div class="bg-tertiary-fixed-dim h-full rounded-full" :style="{ width: primaryGoal.progress + '%' }"></div>
              </div>
            </div>
          </div>
          <div class="absolute -right-10 -bottom-10 w-64 h-64 bg-white/5 rounded-full blur-3xl"></div>
          <div class="absolute top-0 right-0 p-6 opacity-20">
            <span class="material-symbols-outlined text-9xl">rocket_launch</span>
          </div>
        </section>

        <section class="bg-surface-container-low rounded-xl p-8">
          <div class="flex justify-between items-center mb-6">
            <h3 class="text-xl font-bold font-headline flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">format_list_bulleted</span>
              短期里程碑
            </h3>
            <span class="text-xs text-on-surface-variant font-medium">{{ completedMilestonesCount }} / {{ milestones.length }} 已完成</span>
          </div>

          <div class="space-y-4">
            <div
              v-for="m in milestones"
              :key="m.id"
              class="flex items-center gap-4 bg-surface-container-lowest p-4 rounded-xl group hover:shadow-md transition-shadow"
              :class="m.status !== 'done' ? 'border-2 border-transparent hover:border-primary-fixed transition-all' : ''"
            >
              <div
                class="w-6 h-6 rounded-md border-2 flex items-center justify-center"
                :class="m.status === 'done' ? 'border-primary bg-primary text-white' : 'border-outline-variant'"
              >
                <span v-if="m.status === 'done'" class="material-symbols-outlined text-sm">check</span>
              </div>
              <div class="flex-grow">
                <p class="font-semibold text-on-surface">{{ m.title }}</p>
                <p class="text-xs text-on-surface-variant">{{ m.desc }}</p>
              </div>
              <span v-if="m.status === 'done'" class="text-xs font-bold text-tertiary px-2 py-1 bg-tertiary-fixed rounded">已完成</span>
              <div v-else-if="m.status === 'in_progress'" class="flex items-center gap-2">
                <span class="text-xs font-medium text-on-surface-variant">进行中</span>
                <div class="w-16 h-1.5 bg-surface-variant rounded-full overflow-hidden">
                  <div class="bg-primary h-full" :style="{ width: m.progress + '%' }"></div>
                </div>
              </div>
              <span v-else class="text-xs font-medium text-on-surface-variant">待开始</span>
            </div>
          </div>
        </section>
      </div>

      <div class="col-span-12 lg:col-span-4 space-y-8">
        <section class="bg-surface-container-lowest rounded-xl p-6 shadow-sm">
          <h3 class="text-lg font-bold font-headline mb-6 flex items-center gap-2">
            <span class="material-symbols-outlined text-tertiary">verified</span>
            成功准则
          </h3>
          <div class="space-y-6">
            <div>
              <p class="text-xs text-on-surface-variant mb-2 uppercase tracking-widest font-bold">薪资预期</p>
              <p class="text-xl font-bold text-tertiary">{{ successCriteria.salary }}</p>
            </div>
            <div class="pt-4 border-t border-surface-variant">
              <p class="text-xs text-on-surface-variant mb-2 uppercase tracking-widest font-bold">目标公司</p>
              <div class="flex flex-wrap gap-2">
                <span v-for="c in successCriteria.companies" :key="c" class="px-3 py-1 bg-surface-container text-on-secondary-container text-sm rounded-lg">{{ c }}</span>
              </div>
            </div>
            <div class="pt-4 border-t border-surface-variant">
              <p class="text-xs text-on-surface-variant mb-2 uppercase tracking-widest font-bold">核心城市</p>
              <p class="font-semibold">{{ successCriteria.cities }}</p>
            </div>
          </div>
        </section>

        <section class="bg-surface-variant/70 backdrop-blur-xl rounded-xl p-6">
          <h3 class="text-lg font-bold font-headline mb-4 flex items-center gap-2">
            <span class="material-symbols-outlined text-primary">auto_awesome</span>
            长期愿景
          </h3>
          <div class="space-y-4">
            <div v-for="a in longTermAspirations" :key="a.title" class="p-4 bg-white/40 rounded-xl">
              <p class="text-sm font-bold text-primary mb-1">{{ a.title }}</p>
              <p class="text-xs text-on-surface-variant leading-relaxed">{{ a.desc }}</p>
            </div>
          </div>
        </section>

        <div class="bg-primary-fixed rounded-xl p-4 flex items-start gap-4">
          <span class="material-symbols-outlined text-primary mt-1" style="font-variation-settings: 'FILL' 1;">lightbulb</span>
          <div>
            <p class="text-xs font-bold text-on-primary-fixed-variant mb-1">AI 建议</p>
            <p class="text-xs text-on-primary-fixed-variant leading-relaxed">{{ aiAdvice }}</p>
          </div>
        </div>
      </div>
    </div>

    <section class="pt-2">
      <h3 class="text-2xl font-bold font-headline mb-8">其他并行目标</h3>
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div
          v-for="g in parallelGoals"
          :key="g.id"
          class="bg-surface-container-low rounded-xl p-6 hover:translate-y-[-4px] transition-transform"
          :class="g.variant === 'add' ? 'border-2 border-dashed border-outline-variant flex flex-col items-center justify-center text-on-surface-variant cursor-pointer hover:bg-surface-container transition-colors' : ''"
        >
          <template v-if="g.variant !== 'add'">
            <div class="flex justify-between items-start mb-4">
              <div class="p-2 rounded-lg" :class="g.iconBgClass">
                <span class="material-symbols-outlined">{{ g.icon }}</span>
              </div>
              <span class="text-xs font-bold text-on-surface-variant">{{ g.statusLabel }}</span>
            </div>
            <h4 class="font-bold mb-2">{{ g.title }}</h4>
            <p class="text-xs text-on-surface-variant mb-4">{{ g.desc }}</p>
            <div class="w-full bg-surface-variant h-1.5 rounded-full overflow-hidden">
              <div class="h-full" :class="g.progressBarClass" :style="{ width: g.progress + '%' }"></div>
            </div>
          </template>

          <template v-else>
            <span class="material-symbols-outlined text-4xl mb-2">add_circle</span>
            <span class="text-sm font-bold">设定新领域目标</span>
          </template>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const primaryGoal = {
  title: '成为头部互联网公司的高级前端开发工程师',
  eta: '2025年12月',
  progress: 65
}

type Milestone = {
  id: string
  title: string
  desc: string
  status: 'done' | 'in_progress' | 'todo'
  progress?: number
}

const milestones: Milestone[] = [
  { id: 'm1', title: '完成 React 高级模式课程', desc: '学习 Hooks 优化与组件复用逻辑', status: 'done' },
  { id: 'm2', title: '重构个人作品集网站', desc: '提升响应式设计与交互体验', status: 'done' },
  { id: 'm3', title: '通过 AWS 认证助理开发者考试', desc: '掌握基础云架构与部署流程', status: 'in_progress', progress: 40 },
  { id: 'm4', title: '在 GitHub 贡献 5 个开源 PR', desc: '提升代码质量与协作能力', status: 'todo' },
  { id: 'm5', title: '补齐系统设计基础', desc: '掌握常见系统设计题与权衡分析', status: 'todo' },
  { id: 'm6', title: '准备大厂行为面试题库', desc: '沉淀 STAR 案例库', status: 'done' }
]

const completedMilestonesCount = computed(() => milestones.filter((m) => m.status === 'done').length)

const successCriteria = {
  salary: '¥30k - ¥45k / 月',
  companies: ['腾讯 (Tencent)', '字节跳动', '阿里巴巴', '美团'],
  cities: '北京、深圳、上海'
}

const longTermAspirations = [
  { title: '技术领导者', desc: '在 5 年内带领 20+ 人的技术团队，主导核心架构设计。' },
  { title: '行业影响力', desc: '成为前端社区活跃讲师，在全球技术大会发表主题演讲。' },
  { title: '数字化公益', desc: '利用技术开发辅助偏远地区教育的开源平台。' }
]

const aiAdvice = '根据你的进度，建议在下周开始准备“系统架构设计”相关的深度学习，这会显著提升你的大厂竞争力。'

type ParallelGoal = {
  id: string
  variant?: 'add'
  title?: string
  desc?: string
  icon?: string
  iconBgClass?: string
  statusLabel?: string
  progress?: number
  progressBarClass?: string
}

const parallelGoals: ParallelGoal[] = [
  {
    id: 'g1',
    title: '雅思 (IELTS) 达到 7.5',
    desc: '为未来的海外技术交流与远程工作做准备。',
    icon: 'translate',
    iconBgClass: 'bg-secondary-container text-primary',
    statusLabel: '进行中',
    progress: 35,
    progressBarClass: 'bg-secondary'
  },
  {
    id: 'g2',
    title: '身心平衡计划',
    desc: '每周 3 次高强度健身，保持高效工作的体能。',
    icon: 'fitness_center',
    iconBgClass: 'bg-tertiary-fixed text-tertiary',
    statusLabel: '进行中',
    progress: 80,
    progressBarClass: 'bg-tertiary'
  },
  { id: 'g_add', variant: 'add' }
]
</script>
