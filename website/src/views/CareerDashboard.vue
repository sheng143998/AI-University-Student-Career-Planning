<template>
  <div class="space-y-10">
    <!-- Header Section -->
    <header class="flex flex-col md:flex-row md:items-end justify-between gap-6">
      <div>
        <h1 class="text-4xl font-extrabold headline-font tracking-tight text-on-surface mb-2">欢迎回来，Alex。</h1>
        <p class="text-on-surface-variant body-md max-w-xl leading-relaxed">您的 AI 职业导师分析了 12 个新的市场趋势和您最新的作品集更新。您正处于<span class="text-primary font-bold">前端开发</span>职业发展的快速通道上。</p>
      </div>
      <button class="bg-surface-container-high text-on-secondary-container px-6 py-3 rounded-xl font-semibold hover:bg-surface-container-highest transition-colors">
        升级档案
      </button>
    </header>

    <!-- Bento Grid Layout -->
    <div class="grid grid-cols-1 md:grid-cols-12 gap-6">
      <!-- AI Match Summary Card (Span 8) -->
      <div class="md:col-span-8 bg-surface-container-lowest rounded-xl p-8 shadow-[0_20px_40px_rgba(25,28,30,0.04)] relative overflow-hidden group">
        <div class="absolute top-0 right-0 w-64 h-64 bg-primary-fixed/20 rounded-full -mr-20 -mt-20 blur-3xl group-hover:bg-primary-fixed/30 transition-colors"></div>
        <div class="flex flex-col md:flex-row gap-10 items-center relative z-10">
          <div class="flex-shrink-0 text-center">
            <div class="w-32 h-32 rounded-full border-8 border-primary-fixed flex items-center justify-center bg-white shadow-inner">
              <span class="text-4xl font-extrabold headline-font text-primary">85</span>
              <span class="text-sm font-bold text-on-surface-variant self-end mb-4 ml-1">/100</span>
            </div>
            <p class="mt-4 text-xs font-bold uppercase tracking-widest text-primary">匹配得分</p>
          </div>
          <div>
            <div class="flex items-center gap-2 mb-3">
              <span class="material-symbols-outlined text-tertiary-fixed-dim bg-tertiary-container p-1 rounded-md text-sm">auto_awesome</span>
              <h2 class="text-xl font-bold headline-font">前端开发匹配度</h2>
            </div>
            <p class="text-on-surface-variant leading-relaxed text-lg italic">
              “您与前端开发职位的匹配度很高。您的创意技能非常出色，但您可以进一步提升系统架构方面的知识。”
            </p>
            <div class="mt-6 flex gap-4">
              <div class="bg-primary-fixed/50 px-3 py-1.5 rounded-lg text-xs font-bold text-primary flex items-center gap-2">
                <span class="material-symbols-outlined text-sm">check_circle</span> 创意 UI
              </div>
              <div class="bg-primary-fixed/50 px-3 py-1.5 rounded-lg text-xs font-bold text-primary flex items-center gap-2">
                <span class="material-symbols-outlined text-sm">check_circle</span> 用户共情
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Market Trends (Span 4) -->
      <div class="md:col-span-4 bg-surface-container-low rounded-xl p-6 flex flex-col">
        <div class="flex items-center justify-between mb-6">
          <h3 class="font-bold headline-font text-on-surface">市场趋势</h3>
          <span class="text-[10px] font-bold bg-tertiary-fixed text-on-tertiary-fixed-variant px-2 py-1 rounded-full uppercase tracking-tighter">战略增长</span>
        </div>
        <div class="space-y-4 flex-1">
          <div class="p-4 bg-surface-container-lowest rounded-lg">
            <div class="flex justify-between items-center mb-1">
              <span class="text-sm font-medium">计算机科学</span>
              <span class="text-xs font-bold text-green-600">+14%</span>
            </div>
            <div class="h-1.5 w-full bg-surface-container-highest rounded-full overflow-hidden">
              <div class="h-full bg-primary-container" style="width: 88%"></div>
            </div>
          </div>
          <div class="p-4 bg-surface-container-lowest rounded-lg">
            <div class="flex justify-between items-center mb-1">
              <span class="text-sm font-medium">AI 工程</span>
              <span class="text-xs font-bold text-green-600">+32%</span>
            </div>
            <div class="h-1.5 w-full bg-surface-container-highest rounded-full overflow-hidden">
              <div class="h-full bg-tertiary-container" style="width: 95%"></div>
            </div>
          </div>
        </div>
        <router-link to="/market" class="w-full mt-6 text-primary text-xs text-center font-bold hover:underline">探索市场</router-link>
      </div>

      <!-- Skill Gap Visualization (Span 6) -->
      <div class="md:col-span-6 bg-surface-container-lowest rounded-xl p-8 shadow-[0_20px_40px_rgba(25,28,30,0.04)]">
        <h3 class="text-lg font-bold headline-font mb-8">技能差距可视化</h3>
        <div class="aspect-square max-w-[300px] mx-auto relative flex items-center justify-center">
          <div class="absolute inset-0 border border-outline-variant/20 rounded-full scale-100"></div>
          <div class="absolute inset-0 border border-outline-variant/20 rounded-full scale-[0.75]"></div>
          <div class="absolute inset-0 border border-outline-variant/20 rounded-full scale-[0.5]"></div>
          <div class="absolute inset-0 border border-outline-variant/20 rounded-full scale-[0.25]"></div>
          <svg class="w-full h-full transform -rotate-18 overflow-visible" viewBox="0 0 100 100">
            <polygon fill="rgba(0, 86, 210, 0.1)" points="50,10 90,50 50,90 10,50" stroke="#0056d2" stroke-width="2"></polygon>
            <polygon fill="rgba(76, 214, 255, 0.2)" points="50,25 70,50 50,75 30,50" stroke="#004d60" stroke-dasharray="2,2" stroke-width="1.5"></polygon>
          </svg>
          <div class="absolute top-0 left-1/2 -translate-x-1/2 -translate-y-4 text-[10px] font-bold text-on-surface-variant uppercase">技术</div>
          <div class="absolute right-0 top-1/2 translate-x-12 -translate-y-1/2 text-[10px] font-bold text-on-surface-variant uppercase">创新</div>
          <div class="absolute bottom-0 left-1/2 -translate-x-1/2 translate-y-4 text-[10px] font-bold text-on-surface-variant uppercase">韧性</div>
          <div class="absolute left-0 top-1/2 -translate-x-12 -translate-y-1/2 text-[10px] font-bold text-on-surface-variant uppercase">沟通</div>
        </div>
        <div class="mt-8 flex justify-center gap-6">
          <div class="flex items-center gap-2">
            <span class="w-3 h-3 bg-primary rounded-full"></span>
            <span class="text-xs text-on-surface-variant">您的水平</span>
          </div>
          <div class="flex items-center gap-2">
            <span class="w-3 h-3 bg-tertiary-fixed-dim rounded-full"></span>
            <span class="text-xs text-on-surface-variant">行业前 10%</span>
          </div>
        </div>
      </div>

      <!-- Action Items (Span 6) -->
      <div class="md:col-span-6 bg-surface-container-lowest rounded-xl p-8 shadow-[0_20px_40px_rgba(25,28,30,0.04)]">
        <div class="flex items-center justify-between mb-8">
          <h3 class="text-lg font-bold headline-font">行动指南</h3>
          <span class="material-symbols-outlined text-primary">priority_high</span>
        </div>
        <div class="space-y-4">
          <div v-for="(item, index) in actions" :key="index" class="group flex items-center justify-between p-4 bg-surface rounded-xl hover:bg-primary-fixed/30 transition-all cursor-pointer">
            <div class="flex items-center gap-4">
              <div class="w-10 h-10 rounded-lg bg-surface-container-highest flex items-center justify-center text-primary group-hover:bg-primary group-hover:text-white transition-colors">
                <span class="material-symbols-outlined">{{ item.icon }}</span>
              </div>
              <div>
                <p class="text-sm font-bold">{{ item.title }}</p>
                <p class="text-[10px] text-on-surface-variant">{{ item.desc }}</p>
              </div>
            </div>
            <span class="material-symbols-outlined text-on-surface-variant">chevron_right</span>
          </div>
        </div>
      </div>

      <!-- Career Path Roadmap (Span 12) -->
      <div class="md:col-span-12 bg-surface-container-low rounded-2xl p-8 mt-4">
        <h3 class="text-xl font-extrabold headline-font mb-10 text-center md:text-left">您的职业进化地图</h3>
        <div class="relative py-12">
          <div class="absolute top-1/2 left-0 w-full h-1 bg-gradient-to-r from-primary/10 via-primary to-primary/10 -translate-y-1/2 hidden md:block"></div>
          <div class="flex flex-col md:flex-row justify-between relative z-10 gap-12 md:gap-4">
            <div v-for="(node, index) in roadmap" :key="index" class="flex flex-col items-center text-center max-w-[200px]">
              <div :class="['w-14 h-14 rounded-full flex items-center justify-center shadow-lg mb-4 border-4 border-white', node.active ? 'bg-primary-container text-white' : 'bg-surface-container-highest text-on-surface group-hover:bg-primary group-hover:text-white transition-all']">
                <span class="material-symbols-outlined">{{ node.icon }}</span>
              </div>
              <h4 class="font-bold text-sm">{{ node.title }}</h4>
              <p class="text-[10px] text-on-surface-variant mt-1 uppercase">{{ node.time }}</p>
              <div :class="['mt-4 px-3 py-1 bg-white/50 rounded-full text-[9px] font-bold', node.active ? 'text-primary' : 'text-on-surface-variant']">{{ node.status }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const actions = [
  { title: '完成 React 认证', desc: '弥补关键技术差距', icon: 'school' },
  { title: '用 UX 案例研究更新简历', desc: '将档案曝光度提高 25%', icon: 'browse_gallery' },
  { title: '与 3 位资深设计师建立联系', desc: '解锁隐藏的市场机会', icon: 'group' }
]

const roadmap = [
  { title: '初级 UI/视觉设计师', time: '目标：第 1-2 年', status: '85% 匹配', icon: 'person', active: true },
  { title: '资深体验主管', time: '目标：第 4-6 年', status: '弥补差距', icon: 'stars', active: false },
  { title: '设计总 Director', time: '目标：8 年以上', status: '未来高峰', icon: 'architecture', active: false }
]
</script>
