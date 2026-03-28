<template>
  <div class="space-y-10">
    <!-- Header Section -->
    <header class="flex flex-col md:flex-row md:items-end justify-between gap-6">
      <div>
        <h1 class="text-4xl font-extrabold font-headline tracking-tight text-on-surface mb-2">职业市场探索</h1>
        <p class="text-on-surface-variant max-w-xl">基于AI神经网络分析，揭示岗位间的潜在关联与未来晋升路径，助您精准定位职业赛道。</p>
      </div>
      <div class="flex gap-3">
        <button class="px-6 py-3 bg-gradient-to-br from-primary to-primary-container text-white rounded-xl font-bold text-sm shadow-lg flex items-center gap-2">
          <span class="material-symbols-outlined text-lg">filter_list</span>
          高级筛选
        </button>
        <button class="px-6 py-3 bg-surface-container-high text-on-secondary-container rounded-xl font-bold text-sm flex items-center gap-2">
          <span class="material-symbols-outlined text-lg">download</span>
          导出报告
        </button>
      </div>
    </header>

    <!-- Bento Grid Layout -->
    <div class="grid grid-cols-1 lg:grid-cols-12 gap-6">
      <!-- Job Relationship Graph (Large Bento Cell) -->
      <section class="lg:col-span-8 bg-surface-container-lowest rounded-xl p-6 shadow-sm relative overflow-hidden min-h-[500px]">
        <div class="flex items-center justify-between mb-8">
          <div>
            <h3 class="text-xl font-bold font-headline mb-1">岗位关联图谱</h3>
            <p class="text-xs text-on-surface-variant">可视化核心岗位及横向/纵向职业迁徙路径</p>
          </div>
          <div class="flex gap-2">
            <span class="px-3 py-1 bg-primary-fixed text-on-primary-fixed-variant text-[10px] font-bold rounded-full">3D View Enabled</span>
            <span class="px-3 py-1 bg-tertiary-fixed text-on-tertiary-fixed-variant text-[10px] font-bold rounded-full">Current Focus: Tech</span>
          </div>
        </div>

        <!-- SVG Simulation for Graph -->
        <div class="relative w-full h-[380px] bg-surface-container-low rounded-xl border border-outline-variant/10 flex items-center justify-center">
          <div class="absolute inset-0 opacity-20" style="background-image: radial-gradient(#0040a1 0.5px, transparent 0.5px); background-size: 20px 20px;"></div>
          
          <!-- Center Node -->
          <div class="relative z-10 w-32 h-32 bg-primary rounded-full flex flex-col items-center justify-center text-on-primary shadow-2xl ring-8 ring-primary-fixed/30">
            <span class="material-symbols-outlined text-3xl mb-1">integration_instructions</span>
            <span class="text-xs font-bold">UI Designer</span>
          </div>

          <!-- Satellite Nodes & Lines -->
          <div class="absolute top-10 left-1/4 flex flex-col items-center gap-2 group cursor-pointer">
            <div class="w-16 h-16 bg-surface-container-lowest rounded-full flex items-center justify-center shadow-lg border-2 border-primary-container group-hover:scale-110 transition-transform">
              <span class="material-symbols-outlined text-primary">palette</span>
            </div>
            <span class="text-[10px] font-bold text-on-surface-variant">Visual Design</span>
          </div>

          <div class="absolute top-10 right-1/4 flex flex-col items-center gap-2 group cursor-pointer">
            <div class="w-20 h-20 bg-tertiary-container rounded-full flex items-center justify-center shadow-lg group-hover:scale-110 transition-transform">
              <span class="material-symbols-outlined text-white">psychology</span>
            </div>
            <span class="text-[10px] font-bold text-on-surface-variant">UX Researcher</span>
          </div>

          <div class="absolute bottom-10 left-1/3 flex flex-col items-center gap-2 group cursor-pointer">
            <div class="w-16 h-16 bg-surface-container-lowest rounded-full flex items-center justify-center shadow-lg border-2 border-outline-variant group-hover:scale-110 transition-transform">
              <span class="material-symbols-outlined text-secondary">code</span>
            </div>
            <span class="text-[10px] font-bold text-on-surface-variant">Frontend Dev</span>
          </div>

          <div class="absolute bottom-12 right-1/3 flex flex-col items-center gap-2 group cursor-pointer">
            <div class="w-24 h-24 bg-primary-container rounded-full flex flex-col items-center justify-center shadow-lg text-white group-hover:scale-110 transition-transform">
              <span class="material-symbols-outlined">rocket_launch</span>
              <span class="text-[10px] font-bold">Product Lead</span>
            </div>
            <span class="text-[10px] font-bold text-on-surface-variant">Career Goal</span>
          </div>

          <!-- Connection Lines SVG overlay -->
          <svg class="absolute inset-0 w-full h-full pointer-events-none opacity-40">
            <line stroke="#0040a1" stroke-dasharray="4" stroke-width="2" x1="50%" x2="25%" y1="50%" y2="15%"></line>
            <line stroke="#0040a1" stroke-width="2" x1="50%" x2="75%" y1="50%" y2="15%"></line>
            <line stroke="#0040a1" stroke-dasharray="2" stroke-width="1" x1="50%" x2="33%" y1="50%" y2="85%"></line>
            <line stroke="#0056d2" stroke-width="4" x1="50%" x2="67%" y1="50%" y2="85%"></line>
          </svg>
        </div>
      </section>

      <!-- Market Trends (Side Bento Cell) -->
      <section class="lg:col-span-4 flex flex-col gap-6">
        <div class="bg-surface-container-lowest rounded-xl p-6 shadow-sm flex-1">
          <div class="flex items-center gap-3 mb-6">
            <div class="w-10 h-10 rounded-lg bg-tertiary-fixed flex items-center justify-center">
              <span class="material-symbols-outlined text-on-tertiary-fixed-variant">trending_up</span>
            </div>
            <h3 class="font-bold font-headline">市场趋势</h3>
          </div>
          <div class="space-y-6">
            <div>
              <div class="flex justify-between items-end mb-2">
                <span class="text-xs font-bold text-on-surface-variant">薪资水平 (Monthly)</span>
                <span class="text-lg font-black text-primary">¥25k - 45k</span>
              </div>
              <div class="h-2 w-full bg-surface-container-low rounded-full overflow-hidden">
                <div class="h-full bg-primary-container" style="width: 75%"></div>
              </div>
              <p class="text-[10px] text-on-surface-variant mt-1">较去年同期增长 12.5%</p>
            </div>
            <div>
              <div class="flex justify-between items-end mb-2">
                <span class="text-xs font-bold text-on-surface-variant">市场需求量</span>
                <span class="text-lg font-black text-tertiary">高 / High</span>
              </div>
              <div class="flex gap-1 h-8 items-end">
                <div v-for="h in [0.4, 0.6, 0.8, 1, 0.7, 0.9]" :key="h" class="flex-1 bg-surface-container-high rounded-t" :style="{ height: (h * 100) + '%' }"></div>
              </div>
              <p class="text-[10px] text-on-surface-variant mt-1">互联网与金融科技领域需求最旺</p>
            </div>
          </div>
        </div>

        <div class="bg-primary-container rounded-xl p-6 text-on-primary shadow-lg overflow-hidden relative">
          <div class="absolute -right-10 -bottom-10 w-40 h-40 bg-white/10 rounded-full blur-3xl"></div>
          <h4 class="font-bold text-lg mb-4 flex items-center gap-2">
            <span class="material-symbols-outlined">auto_awesome</span>
            AI 深度洞察
          </h4>
          <p class="text-sm opacity-90 leading-relaxed mb-4">
            {{ aiInsight.text }}
          </p>
          <button class="w-full py-2 bg-surface-container-lowest text-primary font-bold rounded-lg text-sm">获取转型建议</button>
        </div>
      </section>

      <section class="lg:col-span-12">
        <div class="flex items-center justify-between mb-6">
          <h3 class="text-2xl font-bold font-headline">热门岗位画像</h3>
          <div class="flex gap-2">
            <button class="p-2 bg-surface-container-high rounded-full"><span class="material-symbols-outlined">chevron_left</span></button>
            <button class="p-2 bg-surface-container-high rounded-full"><span class="material-symbols-outlined">chevron_right</span></button>
          </div>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <div v-for="job in hotJobs" :key="job.id" class="bg-surface-container-lowest rounded-xl p-6 shadow-sm" :class="job.borderClass">
            <div class="flex justify-between items-start mb-4">
              <div>
                <h4 class="font-bold text-lg">{{ job.title }}</h4>
                <span class="text-[10px] px-2 py-0.5 rounded-full font-bold" :class="job.badgeClass">{{ job.badge }}</span>
              </div>
              <span class="material-symbols-outlined" :class="job.iconClass">{{ job.icon }}</span>
            </div>

            <div class="space-y-4">
              <div>
                <p class="text-[10px] font-bold text-on-surface-variant uppercase mb-2 tracking-wider">Technical Skills</p>
                <div class="flex flex-wrap gap-2">
                  <span v-for="s in job.technicalSkills" :key="s" class="px-2 py-1 bg-surface-container-low text-[10px] font-medium rounded">{{ s }}</span>
                </div>
              </div>
              <div>
                <p class="text-[10px] font-bold text-on-surface-variant uppercase mb-2 tracking-wider">Soft Skills</p>
                <div class="flex flex-wrap gap-2">
                  <span v-for="s in job.softSkills" :key="s" class="px-2 py-1 bg-surface-container-low text-[10px] font-medium rounded">{{ s }}</span>
                </div>
              </div>

              <div class="pt-4 border-t border-outline-variant/10 flex items-center justify-between">
                <div class="flex -space-x-2">
                  <img v-for="(a, idx) in job.alumniAvatars" :key="idx" class="w-6 h-6 rounded-full border-2 border-white object-cover" :src="a" alt="alumni avatar" />
                  <div class="w-6 h-6 rounded-full bg-slate-200 border-2 border-white flex items-center justify-center text-[8px] font-bold">+{{ job.moreAlumniCount }}</div>
                </div>
                <span class="text-[10px] text-on-surface-variant">{{ job.alumniText }}</span>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
const aiInsight = {
  text: '根据您的履历，向 AI工程师 方向转型预计需要补充 3 个核心技能点，平均薪资增幅可达 40%。'
}

type HotJob = {
  id: string
  title: string
  badge: string
  icon: string
  technicalSkills: string[]
  softSkills: string[]
  alumniAvatars: string[]
  moreAlumniCount: number
  alumniText: string
  borderClass: string
  badgeClass: string
  iconClass: string
}

const hotJobs: HotJob[] = [
  {
    id: 'ai-algorithm',
    title: 'AI 算法工程师',
    badge: '高新潜力',
    icon: 'psychology',
    technicalSkills: ['PyTorch', 'NLP', 'Transformer'],
    softSkills: ['逻辑思维', '持续学习'],
    alumniAvatars: [
      'https://lh3.googleusercontent.com/aida-public/AB6AXuDnACHcnm796JzZVGKWsAR8ozAAtmjW2d00k5vSo1rMlvm5b8WSsCCweZcB_-GBK9nuGhj8pMDKMjVqaZPZwGxsUSu8OePPRDOpERQVi6-_lw8x7wot1HOsmQk1lD8Fw0sbI0kI2B8v2CjAAHkigFHIHjlop0E7Ox6heZkH78gxrJjdD7WgQjnUaD13ltb-T-IGi2QOixRzjn0BqibzsgzX8qjD7NV8rC9Cxf28bQ8fwTcNmHexwR_8emTqFXHjH8FjG_2MXqQBzH8',
      'https://lh3.googleusercontent.com/aida-public/AB6AXuDxkcWwtIIdYfOxU1LT5S7jVYsgJcLdmSsaIZ5y7AOCb0B-Q1e956eridyL9Tx6TjNRVtEPN1WTN_zk3ISko4o6dt5f96DULm28gESKn9WNKplpyIZ5Vua2aFPP2nD0618tTfoyYWaGKmPbjxsrLwLxZs3G6Tst6NNxeOJZwcY-WSuegIjJbzHpLwd1_SaUg_DyZsLC_rSjaiP0UQnEeztDpP34U3eOFTeMxCzQLpvKUYexrODyJVKef0vcCEQFrHsLmmqdOgto4JA'
    ],
    moreAlumniCount: 12,
    alumniText: '2.4k+ 位校友在职',
    borderClass: 'border-t-4 border-primary',
    badgeClass: 'bg-primary-fixed text-on-primary-fixed-variant',
    iconClass: 'text-primary-container'
  },
  {
    id: 'pm',
    title: '产品经理 (PM)',
    badge: '综合管理',
    icon: 'tactic',
    technicalSkills: ['原型设计', 'PRD撰写', 'SQL'],
    softSkills: ['跨部门沟通', '商业洞察'],
    alumniAvatars: [
      'https://lh3.googleusercontent.com/aida-public/AB6AXuDjUi5XP5dIGCR6ILGVo77sGTgrHYtLlLYpiCvnE2o1toIdMU7-nSax2c693sSBcaO61p94IP7Hl2LLVeOdOJm4Tmwyvo1_M7WSb-aStTE3BGWepz9ISejv9Mk6P62fEKp-aF6HmCDbVU1Xz5ypEe1JV3RIfioJUFy9bsyMszOhpErxwjjptqKrtxYRyKfsd2bbYUyAnFw2PAxE7CHSL6haRW0KFY1_N-iwVtWayoR2tyZ9vHHwaqTqnnA5nosFSNvQJQQC3S7B7hY',
      'https://lh3.googleusercontent.com/aida-public/AB6AXuAP1Zj3MPrVmVExlw9IDVD2T6a-a9tahTPyQmeDLAYpMOle-3CMSs1bIKBtT-oD49coScIfpjuGV2gTtjuNCXl3LYJPQh3io8HmhiWSkdfgWMPYLIcNMKMF0ffgAx2OMOzGKFTzCT2ZD-bquZGG3AKKQ7_9Nwm7v-pg_wZEl7sXWxBoGBmKmqTykfqfUcDc4mJ1hB3KLrXvOLiqFfMRhx4fhie2SIvb5E3wjx_RB0R8ZLNEaropmPgrjpUiTeCoRViNloKD-O_cusY'
    ],
    moreAlumniCount: 8,
    alumniText: '1.8k+ 位校友在职',
    borderClass: 'border-t-4 border-tertiary',
    badgeClass: 'bg-tertiary-fixed text-on-tertiary-fixed-variant',
    iconClass: 'text-tertiary-container'
  },
  {
    id: 'fullstack',
    title: '全栈开发工程师',
    badge: '技术全面',
    icon: 'terminal',
    technicalSkills: ['React/Vue', 'Node.js', 'Docker'],
    softSkills: ['架构设计', '独立开发'],
    alumniAvatars: [
      'https://lh3.googleusercontent.com/aida-public/AB6AXuAfwhIE5Kzdoea7tWOla_T5FRzWKsdzNr-AL5y3NGHYVG6Y5btU6U4QVIihCMgHscWx6pUVR9n5XPoiQU2apxDlM6TaUw5naVB3H19K5ZdnYl-xPCDAdLRmGsFZaMgA_asXI0MIt9W6dpDWM188ijTZZEJ0PAOLVAcWH3oz4-sQ6l5LbhW7pCc6nu55Xyc0Kjqz1uabI0FzNLZCh76HpHd5IKiBg5GRhWXSwIgO16La1vJYWRDGiDp7InbNgpHjfJd65OLaAfh-SGE',
      'https://lh3.googleusercontent.com/aida-public/AB6AXuD6NZ7oMgZIPET2JhBDjQvm08SqfQeIXW15TBt0GMWTG6jVQtgiv3_uxCNTRKvAlCAYg6cUY6Z4BTcQeqXHJ6wz408XViMU2aRCYdcL3lrxC3wd6bypmVmhW4WuPGMUHKQSSt3dOHlmrEJWzxolktDEEybDYTbZANFoZQOD04RmLmA3T3spX1Iycs4oxB7QNMMd2VhXHwjUdaUvtWhCLkUrIEivLYxpqpJTwIjBckRdZezYyzieZi3BT_TnGGefzEhbfYUM6OSLl-A'
    ],
    moreAlumniCount: 5,
    alumniText: '3.1k+ 位校友在职',
    borderClass: 'border-t-4 border-secondary',
    badgeClass: 'bg-secondary-fixed text-on-secondary-fixed-variant',
    iconClass: 'text-secondary'
  }
]
</script>
