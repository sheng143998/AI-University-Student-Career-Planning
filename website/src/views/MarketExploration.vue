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
          <div 
            v-for="(node, idx) in graphNodes" 
            :key="idx"
            class="absolute flex flex-col items-center gap-2 group cursor-pointer"
            :style="node.position"
            @click.stop="handleGraphNodeClick(node)"
          >
            <div 
              class="rounded-full flex items-center justify-center shadow-lg transition-transform group-hover:scale-110"
              :class="node.bgClass"
              :style="{ width: node.size + 'px', height: node.size + 'px' }"
            >
              <span class="material-symbols-outlined" :class="node.iconClass">{{ node.icon }}</span>
            </div>
            <span class="text-[10px] font-bold text-on-surface-variant">{{ node.label }}</span>
          </div>

          <!-- Connection Lines SVG overlay -->
          <svg class="absolute inset-0 w-full h-full pointer-events-none opacity-40">
            <line stroke="#0040a1" stroke-dasharray="4" stroke-width="2" x1="50%" x2="25%" y1="50%" y2="15%"></line>
            <line stroke="#0040a1" stroke-width="2" x1="50%" x2="75%" y1="50%" y2="15%"></line>
            <line stroke="#0040a1" stroke-dasharray="2" stroke-width="1" x1="50%" x2="33%" y1="50%" y2="85%"></line>
            <line stroke="#0056d2" stroke-width="4" x1="50%" x2="67%" y1="50%" y2="85%"></line>
          </svg>
        </div>
        <div class="mt-4 flex justify-end">
          <button
            @click="router.push({ name: 'roadmap' })"
            class="px-5 py-2.5 bg-primary text-white rounded-lg font-bold text-sm shadow hover:shadow-md transition-all flex items-center gap-2"
          >
            <span class="material-symbols-outlined text-base">map</span>
            在职业地图中探索
          </button>
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
          <div v-if="trendsLoading" class="flex items-center justify-center py-8">
            <span class="material-symbols-outlined animate-spin text-primary">progress_activity</span>
          </div>
          <div v-else class="space-y-6">
            <div>
              <div class="flex justify-between items-end mb-2">
                <span class="text-xs font-bold text-on-surface-variant">薪资水平 (Monthly)</span>
                <span class="text-lg font-black text-primary">{{ trendsDisplay.salaryRange }}</span>
              </div>
              <div class="h-2 w-full bg-surface-container-low rounded-full overflow-hidden">
                <div class="h-full bg-primary-container" :style="{ width: trendsSalaryPercent + '%' }"></div>
              </div>
              <p class="text-[10px] text-on-surface-variant mt-1">{{ trendsSalaryGrowth }}</p>
            </div>
            <div>
              <div class="flex justify-between items-end mb-2">
                <span class="text-xs font-bold text-on-surface-variant">市场需求量</span>
                <span class="text-lg font-black text-tertiary">{{ trendsDisplay.demandLevel }}</span>
              </div>
              <div class="flex gap-1 h-8 items-end">
                <div v-for="(h, idx) in trendsDemandBars" :key="idx" class="flex-1 bg-tertiary-container rounded-t" :style="{ height: (h * 100) + '%' }"></div>
              </div>
              <p class="text-[10px] text-on-surface-variant mt-1">{{ trendsDemandDesc }}</p>
            </div>
          </div>
        </div>

        <div class="bg-primary-container rounded-xl p-6 text-on-primary shadow-lg overflow-hidden relative">
          <div class="absolute -right-10 -bottom-10 w-40 h-40 bg-white/10 rounded-full blur-3xl"></div>
          <h4 class="font-bold text-lg mb-4 flex items-center gap-2">
            <span class="material-symbols-outlined">auto_awesome</span>
            AI 深度洞察
          </h4>
          <div v-if="insightLoading" class="flex items-center justify-center py-4">
            <span class="material-symbols-outlined animate-spin">progress_activity</span>
          </div>
          <template v-else>
            <p class="text-sm opacity-90 leading-relaxed mb-4">
              {{ aiInsight.text }}
            </p>
            <div v-if="aiInsightData?.insight?.recommendations?.length" class="mb-4 space-y-2">
              <div v-for="rec in aiInsightData.insight.recommendations.slice(0, 3)" :key="rec" class="flex items-start gap-2 text-sm">
                <span class="material-symbols-outlined text-sm mt-0.5">check_circle</span>
                <span>{{ rec }}</span>
              </div>
            </div>
          </template>
          <button class="w-full py-2 bg-surface-container-lowest text-primary font-bold rounded-lg text-sm">获取转型建议</button>
        </div>
      </section>

      <section class="lg:col-span-12">
        <div class="flex items-center justify-between mb-6">
          <h3 class="text-2xl font-bold font-headline">热门岗位画像</h3>
          <div class="flex items-center gap-4">
            <span class="text-sm text-on-surface-variant">共 {{ totalItems }} 个岗位</span>
            <div class="flex gap-2">
              <button 
                @click="prevPage" 
                :disabled="currentPage <= 1"
                class="p-2 bg-surface-container-high rounded-full disabled:opacity-50 disabled:cursor-not-allowed hover:bg-surface-container transition-colors"
              >
                <span class="material-symbols-outlined">chevron_left</span>
              </button>
              <div class="flex items-center gap-1 px-3 py-1 bg-surface-container-high rounded-full">
                <button 
                  v-for="page in displayedPages" 
                  :key="page"
                  @click="goToPage(page)"
                  :class="[
                    'w-8 h-8 rounded-full text-sm font-medium transition-colors',
                    currentPage === page 
                      ? 'bg-primary text-white' 
                      : 'hover:bg-surface-container'
                  ]"
                >
                  {{ page }}
                </button>
              </div>
              <button 
                @click="nextPage" 
                :disabled="currentPage >= totalPages"
                class="p-2 bg-surface-container-high rounded-full disabled:opacity-50 disabled:cursor-not-allowed hover:bg-surface-container transition-colors"
              >
                <span class="material-symbols-outlined">chevron_right</span>
              </button>
            </div>
          </div>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <div 
            v-for="job in displayJobs" 
            :key="job.id" 
            class="bg-surface-container-lowest rounded-xl p-6 shadow-sm hover:shadow-md transition-all cursor-pointer hover:scale-[1.02]" 
            :class="job.borderClass"
            @click="openJobDetail(Number(job.id))"
          >
            <!-- 头部：标题和标签 -->
            <div class="flex justify-between items-start mb-4">
              <div class="flex-1">
                <h4 class="font-bold text-lg mb-1">{{ job.title }}</h4>
                <div class="flex items-center gap-2 flex-wrap">
                  <span class="text-[10px] px-2 py-0.5 rounded-full font-bold" :class="job.badgeClass">{{ job.badge }}</span>
                  <span class="text-[10px] text-on-surface-variant flex items-center gap-1">
                    <span class="material-symbols-outlined text-[12px]">location_on</span>
                    {{ job.city }}
                  </span>
                  <span class="text-[10px] text-primary font-medium">{{ job.salaryText }}</span>
                </div>
              </div>
              <span class="material-symbols-outlined text-3xl" :class="job.iconClass">{{ job.icon }}</span>
            </div>

            <div class="space-y-4">
              <!-- 专业技能 -->
              <div>
                <p class="text-[10px] font-bold text-on-surface-variant uppercase mb-2 tracking-wider flex items-center gap-1">
                  <span class="material-symbols-outlined text-[12px]">code</span>
                  专业技能
                </p>
                <div class="flex flex-wrap gap-2">
                  <span v-for="s in job.technicalSkills" :key="s" class="px-2 py-1 bg-primary-container/30 text-[10px] font-medium rounded text-primary">{{ s }}</span>
                </div>
              </div>

              <!-- 证书要求 -->
              <div v-if="job.certificates && job.certificates.length > 0">
                <p class="text-[10px] font-bold text-on-surface-variant uppercase mb-2 tracking-wider flex items-center gap-1">
                  <span class="material-symbols-outlined text-[12px]">verified</span>
                  证书要求
                </p>
                <div class="flex flex-wrap gap-2">
                  <span v-for="c in job.certificates" :key="c" class="px-2 py-1 bg-tertiary-container/30 text-[10px] font-medium rounded text-tertiary">{{ c }}</span>
                </div>
              </div>

              <!-- 能力要求（创新能力、学习能力、抗压能力、沟通能力、实习能力） -->
              <div v-if="job.capabilities && job.capabilities.length > 0">
                <p class="text-[10px] font-bold text-on-surface-variant uppercase mb-2 tracking-wider flex items-center gap-1">
                  <span class="material-symbols-outlined text-[12px]">fitness_center</span>
                  能力要求
                </p>
                <div class="grid grid-cols-5 gap-1">
                  <div v-for="cap in job.capabilities" :key="cap.label" class="flex flex-col items-center p-1 bg-surface-container-low rounded">
                    <span class="material-symbols-outlined text-[14px] text-secondary">{{ cap.icon }}</span>
                    <span class="text-[8px] text-on-surface-variant mt-1">{{ cap.label }}</span>
                    <div class="w-full h-1 bg-surface-container-high rounded-full mt-1 overflow-hidden">
                      <div class="h-full bg-secondary rounded-full" :style="{ width: cap.value + '%' }"></div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 岗位亮点/软技能 -->
              <div v-if="job.softSkills && job.softSkills.length > 0">
                <p class="text-[10px] font-bold text-on-surface-variant uppercase mb-2 tracking-wider flex items-center gap-1">
                  <span class="material-symbols-outlined text-[12px]">stars</span>
                  岗位亮点
                </p>
                <div class="flex flex-wrap gap-2">
                  <span v-for="s in job.softSkills" :key="s" class="px-2 py-1 bg-surface-container-low text-[10px] font-medium rounded">{{ s }}</span>
                </div>
              </div>

              <!-- 底部：招聘信息和在招岗位数 -->
              <div class="pt-4 border-t border-outline-variant/10 flex items-center justify-between">
                <div class="flex -space-x-2">
                  <div v-for="i in 3" :key="i" class="w-6 h-6 rounded-full bg-gradient-to-br from-primary to-tertiary border-2 border-white flex items-center justify-center">
                    <span class="material-symbols-outlined text-[10px] text-white">person</span>
                  </div>
                  <div class="w-6 h-6 rounded-full bg-slate-200 border-2 border-white flex items-center justify-center text-[8px] font-bold">+{{ job.moreAlumniCount }}</div>
                </div>
                <span class="text-[10px] text-on-surface-variant font-medium">{{ job.alumniText }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 底部分页 -->
        <div class="flex items-center justify-center gap-4 mt-8">
          <button 
            @click="prevPage" 
            :disabled="currentPage <= 1"
            class="px-4 py-2 bg-surface-container-high rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-surface-container transition-colors flex items-center gap-1"
          >
            <span class="material-symbols-outlined text-sm">chevron_left</span>
            上一页
          </button>
          <div class="flex items-center gap-1">
            <button 
              v-for="page in displayedPages" 
              :key="page"
              @click="goToPage(page)"
              :class="[
                'w-10 h-10 rounded-lg text-sm font-medium transition-colors',
                currentPage === page 
                  ? 'bg-primary text-white' 
                  : 'bg-surface-container-high hover:bg-surface-container'
              ]"
            >
              {{ page }}
            </button>
          </div>
          <button 
            @click="nextPage" 
            :disabled="currentPage >= totalPages"
            class="px-4 py-2 bg-surface-container-high rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-surface-container transition-colors flex items-center gap-1"
          >
            下一页
            <span class="material-symbols-outlined text-sm">chevron_right</span>
          </button>
        </div>
      </section>
    </div>

    <!-- Job Detail Modal -->
    <div 
      v-if="showModal" 
      class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm"
      @click.self="closeModal"
    >
      <div class="bg-surface-container-lowest rounded-2xl shadow-2xl w-full max-w-4xl max-h-[90vh] overflow-hidden flex flex-col">
        <!-- Modal Header -->
        <div class="sticky top-0 bg-surface-container-lowest border-b border-outline-variant/20 p-6 flex items-start justify-between">
          <div v-if="selectedJobDetail">
            <h2 class="text-2xl font-bold font-headline">{{ selectedJobDetail.jobName }}</h2>
            <div class="flex items-center gap-3 mt-2">
              <span class="px-3 py-1 bg-primary-fixed text-on-primary-fixed-variant text-xs font-bold rounded-full">{{ selectedJobDetail.industrySegment }}</span>
              <span class="text-sm text-on-surface-variant flex items-center gap-1">
                <span class="material-symbols-outlined text-sm">location_on</span>
                {{ selectedJobDetail.cities?.join(' / ') || '全国' }}
              </span>
            </div>
          </div>
          <button 
            @click="closeModal"
            class="p-2 hover:bg-surface-container-high rounded-full transition-colors"
          >
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>

        <!-- Modal Body -->
        <div class="flex-1 overflow-y-auto p-6 space-y-6">
          <!-- Loading State -->
          <div v-if="modalLoading" class="flex items-center justify-center py-12">
            <span class="material-symbols-outlined animate-spin text-4xl text-primary">progress_activity</span>
            <span class="ml-3 text-on-surface-variant">加载岗位详情中...</span>
          </div>

          <!-- Error State -->
          <div v-else-if="modalError" class="text-center py-12">
            <span class="material-symbols-outlined text-4xl text-red-500">error</span>
            <p class="mt-3 text-red-600">{{ modalError }}</p>
            <button 
              @click="closeModal"
              class="mt-4 px-4 py-2 bg-surface-container-high rounded-lg hover:bg-surface-container transition-colors"
            >
              关闭
            </button>
          </div>

          <!-- Job Detail Content -->
          <div v-else-if="selectedJobDetail" class="space-y-6">
            <!-- Overview Cards -->
            <dl class="grid grid-cols-2 sm:grid-cols-4 gap-4">
              <div class="rounded-lg border border-outline-variant/60 bg-surface-container-highest p-4">
                <dt class="text-xs font-bold uppercase tracking-wider text-on-surface-variant">薪资范围</dt>
                <dd class="mt-1 text-lg font-black text-primary">
                  {{ selectedJobDetail.salaryRange?.min && selectedJobDetail.salaryRange?.max 
                    ? `¥${Math.round(selectedJobDetail.salaryRange.min/1000)}k-${Math.round(selectedJobDetail.salaryRange.max/1000)}k`
                    : '面议' }}
                </dd>
              </div>
              <div class="rounded-lg border border-outline-variant/60 bg-surface-container-highest p-4">
                <dt class="text-xs font-bold uppercase tracking-wider text-on-surface-variant">经验要求</dt>
                <dd class="mt-1 text-lg font-black text-on-surface">
                  {{ selectedJobDetail.experienceRange?.min ?? 0 }}-{{ selectedJobDetail.experienceRange?.max ?? 2 }} 年
                </dd>
              </div>
              <div class="rounded-lg border border-outline-variant/60 bg-surface-container-highest p-4">
                <dt class="text-xs font-bold uppercase tracking-wider text-on-surface-variant">学历要求</dt>
                <dd class="mt-1 text-lg font-black text-on-surface">{{ selectedJobDetail.educationRequirement || '不限' }}</dd>
              </div>
              <div class="rounded-lg border border-outline-variant/60 bg-surface-container-highest p-4">
                <dt class="text-xs font-bold uppercase tracking-wider text-on-surface-variant">需求等级</dt>
                <dd class="mt-1 text-lg font-black text-tertiary">{{ selectedJobDetail.demandAnalysis?.level || '中等' }}</dd>
              </div>
            </dl>

            <!-- Job Description -->
            <div v-if="selectedJobDetail.description" class="bg-surface-container-low rounded-xl p-5">
              <h3 class="text-sm font-bold text-on-surface-variant uppercase tracking-wider mb-3">岗位描述</h3>
              <p class="text-sm text-on-surface leading-relaxed">{{ selectedJobDetail.description }}</p>
            </div>

            <!-- Core Skills - Simplified using requiredSkills (max 20) -->
            <div v-if="selectedJobDetail.requiredSkills?.length" class="bg-surface-container-low rounded-xl p-5">
              <h3 class="text-sm font-bold text-on-surface-variant uppercase tracking-wider mb-3 flex items-center gap-2">
                <span class="material-symbols-outlined text-sm">code</span>
                核心技能
              </h3>
              <div class="flex flex-wrap gap-2">
                <span
                  v-for="(skill, idx) in selectedJobDetail.requiredSkills.slice(0, 20)"
                  :key="idx"
                  class="px-3 py-1 rounded-full text-xs font-bold bg-primary-container/30 text-primary"
                >
                  {{ skill }}
                </span>
              </div>
            </div>

            <!-- Soft Skills - 软技能（带描述和证据） -->
            <div v-if="selectedJobDetail.softSkills?.length" class="bg-surface-container-lowest rounded-xl p-6 shadow-sm border border-outline-variant/20">
              <h3 class="text-lg font-bold mb-4 flex items-center gap-2">
                <span class="material-symbols-outlined text-primary">emoji_objects</span>
                软技能
              </h3>
              <div class="space-y-3">
                <div
                  v-for="(skill, idx) in selectedJobDetail.softSkills"
                  :key="idx"
                  class="rounded-lg border border-outline-variant/60 bg-surface-container-highest p-4"
                >
                  <div class="flex items-center justify-between gap-4">
                    <div class="font-bold text-on-surface">{{ skill.name }}</div>
                    <div class="text-sm text-on-surface-variant">{{ skill.score }}</div>
                  </div>
                  <p v-if="skill.description" class="text-sm text-on-surface-variant mt-2 leading-relaxed">{{ skill.description }}</p>
                  <ul v-if="skill.evidence?.length" class="mt-2 list-disc list-inside text-sm space-y-1 text-on-surface-variant">
                    <li v-for="(ev, evIdx) in skill.evidence" :key="evIdx">{{ ev }}</li>
                  </ul>
                </div>
              </div>
            </div>

            <!-- Capability Requirements - 能力要求（简化显示，如果软技能不存在） -->
            <div v-else-if="selectedJobDetail.capabilityRequirements" class="bg-surface-container-lowest rounded-xl p-6 shadow-sm border border-outline-variant/20">
              <h3 class="text-lg font-bold mb-4 flex items-center gap-2">
                <span class="material-symbols-outlined text-primary">psychology</span>
                能力要求
              </h3>
              <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                <div
                  v-for="(value, key) in selectedJobDetail.capabilityRequirements"
                  :key="key"
                  class="rounded-lg border border-outline-variant/60 bg-surface-container-highest p-4"
                >
                  <div class="flex items-center justify-between gap-4">
                    <span class="text-xs font-bold text-on-surface-variant">{{ capabilityLabels[key] || key }}</span>
                    <span class="text-lg font-black text-on-surface">{{ value }}</span>
                  </div>
                  <div class="mt-3 w-full h-2 bg-surface-container-high rounded-full overflow-hidden">
                    <div class="h-full bg-gradient-to-r from-primary to-primary-container" :style="{ width: value + '%' }" />
                  </div>
                </div>
              </div>
            </div>

            <!-- Certificate Requirements -->
            <div v-if="selectedJobDetail.certificateRequirements?.length" class="bg-surface-container-low rounded-xl p-5">
              <h3 class="text-sm font-bold text-on-surface-variant uppercase tracking-wider mb-3 flex items-center gap-2">
                <span class="material-symbols-outlined text-sm">verified</span>
                证书要求
              </h3>
              <div class="flex flex-wrap gap-2">
                <span
                  v-for="(cert, idx) in selectedJobDetail.certificateRequirements"
                  :key="idx"
                  class="px-3 py-1 rounded-full text-xs font-bold bg-primary-fixed text-on-primary-fixed-variant"
                >
                  {{ cert }}
                </span>
              </div>
            </div>

            <!-- Career Path -->
            <div v-if="selectedJobDetail.careerPath" class="bg-surface-container-low rounded-xl p-5">
              <h3 class="text-sm font-bold text-on-surface-variant uppercase tracking-wider mb-4 flex items-center gap-2">
                <span class="material-symbols-outlined text-sm">route</span>
                职业路径
              </h3>
              <button
                @click="router.push({ name: 'roadmap', query: { jobId: String(selectedJobDetail.id) } })"
                class="w-full px-4 py-3 bg-surface-container-highest rounded-lg hover:bg-surface-container transition-colors text-sm font-bold text-primary flex items-center justify-center gap-2"
              >
                <span class="material-symbols-outlined text-base">map</span>
                职业地图中查看详细
              </button>
            </div>

            <!-- Demand Analysis -->
            <div v-if="selectedJobDetail.demandAnalysis" class="bg-primary-container/20 rounded-xl p-5">
              <h3 class="text-sm font-bold text-on-surface-variant uppercase tracking-wider mb-3 flex items-center gap-2">
                <span class="material-symbols-outlined text-sm">trending_up</span>
                需求分析
              </h3>
              <div class="grid grid-cols-3 gap-4">
                <div>
                  <div class="text-xs text-on-surface-variant">需求等级</div>
                  <div class="text-lg font-bold text-primary">{{ selectedJobDetail.demandAnalysis.level }}</div>
                </div>
                <div>
                  <div class="text-xs text-on-surface-variant">增长率</div>
                  <div class="text-lg font-bold text-tertiary">{{ selectedJobDetail.demandAnalysis.growthRate }}</div>
                </div>
                <div>
                  <div class="text-xs text-on-surface-variant">趋势</div>
                  <div class="text-lg font-bold text-on-surface">{{ selectedJobDetail.demandAnalysis.trend }}</div>
                </div>
              </div>
            </div>

            <!-- Updated Time -->
            <div class="text-xs text-on-surface-variant text-right">
              更新时间：{{ formatTime(selectedJobDetail.updatedAt) }}
            </div>
          </div>
        </div>

        <!-- Modal Footer -->
        <div class="sticky bottom-0 bg-surface-container-lowest border-t border-outline-variant/20 p-4 flex justify-end gap-3">
          <button 
            @click="closeModal"
            class="px-6 py-2 bg-surface-container-high rounded-lg hover:bg-surface-container transition-colors font-bold"
          >
            关闭
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getMarketHotJobs, getMarketTrends, getMarketInsight, getMarketJobDetail, type MarketHotJobItem, type MarketTrends, type MarketInsight, type MarketJobDetail } from '@/api/market'

const router = useRouter()

// 响应式数据
const hotJobs = ref<MarketHotJobItem[]>([])
const trends = ref<MarketTrends | null>(null)
const aiInsightData = ref<MarketInsight | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
const trendsLoading = ref(true)
const insightLoading = ref(true)

// 分页参数
const currentPage = ref(1)
const pageSize = ref(6)
const totalItems = ref(0)

// 弹窗相关
const showModal = ref(false)
const selectedJobDetail = ref<MarketJobDetail | null>(null)
const modalLoading = ref(false)
const modalError = ref('')

// 图节点数据
const graphNodes = [
  { label: 'Visual Design', icon: 'palette', size: 64, bgClass: 'bg-surface-container-lowest border-2 border-primary-container', iconClass: 'text-primary', position: { top: '10%', left: '25%' }, jobId: null },
  { label: 'UX Researcher', icon: 'psychology', size: 80, bgClass: 'bg-tertiary-container', iconClass: 'text-white', position: { top: '10%', right: '25%' }, jobId: null },
  { label: 'Frontend Dev', icon: 'code', size: 64, bgClass: 'bg-surface-container-lowest border-2 border-outline-variant', iconClass: 'text-secondary', position: { bottom: '10%', left: '33%' }, jobId: null },
  { label: 'Product Lead', icon: 'rocket_launch', size: 96, bgClass: 'bg-primary-container', iconClass: 'text-white', position: { bottom: '12%', right: '33%' }, jobId: null }
]

// 图节点点击处理 - 跳转到职业地图页面
function handleGraphNodeClick(node: typeof graphNodes[0]) {
  // 根据节点标签查找对应岗位
  const jobMap: Record<string, string> = {
    'Visual Design': '视觉设计',
    'UX Researcher': '用户体验',
    'Frontend Dev': '前端',
    'Product Lead': '产品'
  }
  const keyword = jobMap[node.label]
  if (keyword) {
    const matchedJob = hotJobs.value.find(j => j.jobName?.includes(keyword))
    if (matchedJob) {
      // 跳转到职业地图页面
      router.push({
        name: 'roadmap',
        query: { jobId: String(matchedJob.id) }
      })
    }
  }
}

// 计算属性：AI洞察文本
const aiInsight = computed(() => {
  if (aiInsightData.value?.insight) {
    return {
      text: aiInsightData.value.insight.summary || '正在加载AI洞察...'
    }
  }
  return {
    text: '根据您的履历，向 AI工程师 方向转型预计需要补充 3 个核心技能点，平均薪资增幅可达 40%。'
  }
})

// 计算属性：趋势显示
const trendsDisplay = computed(() => {
  if (trends.value?.salary?.current) {
    const salary = trends.value.salary.current
    const minK = salary.min ? Math.round(salary.min / 1000) : 15
    const maxK = salary.max ? Math.round(salary.max / 1000) : 45
    return {
      salaryRange: `¥${minK}k - ${maxK}k`,
      demandLevel: trends.value.demand?.level || '高'
    }
  }
  return {
    salaryRange: '¥25k - 45k',
    demandLevel: '高'
  }
})

// 计算属性：薪资百分比
const trendsSalaryPercent = computed(() => {
  if (trends.value?.salary?.current) {
    const max = trends.value.salary.current.max || 45000
    return Math.min(100, Math.round(max / 600))
  }
  return 75
})

// 计算属性：薪资增长率文本
const trendsSalaryGrowth = computed(() => {
  const growth = trends.value?.salary?.yoyGrowth
  if (growth) {
    return `较去年同期增长 ${growth.toFixed(1)}%`
  }
  return '较去年同期增长 12.5%'
})

// 计算属性：需求条形图
const trendsDemandBars = computed(() => {
  const histogram = trends.value?.demand?.histogram
  if (histogram && histogram.length > 0) {
    const maxVal = Math.max(...histogram)
    return histogram.map(v => Math.min(1, v / maxVal))
  }
  return [0.4, 0.6, 0.8, 1, 0.7, 0.9]
})

// 计算属性：需求描述
const trendsDemandDesc = computed(() => {
  const growthRate = trends.value?.demand?.growthRate
  if (growthRate) {
    return `需求量较上季度增长 ${growthRate.toFixed(1)}%`
  }
  return '互联网与金融科技领域需求最旺'
})

// 将API岗位数据映射为显示格式
function mapJobToDisplay(job: MarketHotJobItem) {
  const iconMap: Record<string, string> = {
    'AI': 'psychology',
    'PRODUCT': 'tactic',
    'FRONTEND': 'code',
    'BACKEND': 'integration_instructions',
    'DATA': 'analytics',
    'DEVOPS': 'cloud',
    'MOBILE': 'smartphone',
    'TEST': 'bug_report',
    'DESIGN': 'palette',
    'SECURITY': 'security',
    'DEFAULT': 'work'
  }
  
  const code = job.industrySegment?.toUpperCase() || ''
  let icon = 'work'
  for (const key of Object.keys(iconMap)) {
    if (code.includes(key)) {
      icon = iconMap[key]
      break
    }
  }
  
  const borderColors = ['border-t-4 border-primary', 'border-t-4 border-tertiary', 'border-t-4 border-secondary']
  const badgeColors = ['bg-primary-fixed text-on-primary-fixed-variant', 'bg-tertiary-fixed text-on-tertiary-fixed-variant', 'bg-secondary-fixed text-on-secondary-fixed-variant']
  const iconColors = ['text-primary-container', 'text-tertiary-container', 'text-secondary']
  
  const idx = job.id % 3
  
  // 能力要求处理（创新能力、学习能力、抗压能力、沟通能力、实习能力）
  const capabilities = job.capabilityRequirements
  const capabilityList = capabilities ? [
    { label: '创新', value: capabilities.innovation, icon: 'lightbulb' },
    { label: '学习', value: capabilities.learning, icon: 'school' },
    { label: '抗压', value: capabilities.resilience, icon: 'fitness_center' },
    { label: '沟通', value: capabilities.communication, icon: 'chat' },
    { label: '实习', value: capabilities.internship, icon: 'work' }
  ] : []
  
  // 薪资格式化
  const salaryMin = job.salaryRange?.min ? Math.round(job.salaryRange.min / 1000) : null
  const salaryMax = job.salaryRange?.max ? Math.round(job.salaryRange.max / 1000) : null
  const salaryText = salaryMin && salaryMax ? `¥${salaryMin}k-${salaryMax}k` : '薪资面议'
  
  return {
    id: String(job.id),
    title: job.jobName,
    badge: job.tag || job.demandLevel,
    icon: job.icon || icon,
    city: job.city || '北京',
    salaryText,
    technicalSkills: job.coreSkills?.slice(0, 4) || [],
    softSkills: job.highlights?.slice(0, 3) || ['市场需求旺盛', '职业路径清晰'],
    certificates: job.certificateRequirements?.slice(0, 3) || [],
    capabilities: capabilityList,
    alumniAvatars: [] as string[],
    moreAlumniCount: Math.floor(Math.random() * 10) + 5,
    alumniText: `${job.sourceJobCount || 100}+ 岗位在招`,
    borderClass: borderColors[idx],
    badgeClass: badgeColors[idx],
    iconClass: iconColors[idx]
  }
}

// 组件挂载时获取数据
onMounted(async () => {
  try {
    loading.value = true
    
    // 获取热门岗位（获取更多数据用于分页）
    const hotJobsRes = await getMarketHotJobs({ limit: 100 })
    if (hotJobsRes.data) {
      hotJobs.value = hotJobsRes.data.items || hotJobsRes.data.jobs || []
      totalItems.value = hotJobs.value.length
    }
    
    // 获取趋势数据（使用第一个岗位）
    trendsLoading.value = true
    const trendsRes = await getMarketTrends({ 
      job_profile_id: hotJobs.value[0]?.id 
    })
    if (trendsRes.data) {
      trends.value = trendsRes.data
    }
    trendsLoading.value = false
    
    // 获取AI洞察
    insightLoading.value = true
    const insightRes = await getMarketInsight({
      job_profile_id: hotJobs.value[0]?.id
    })
    if (insightRes.data) {
      aiInsightData.value = insightRes.data
    }
    insightLoading.value = false
    
  } catch (e) {
    console.error('获取市场数据失败:', e)
    error.value = '加载市场数据失败'
    trendsLoading.value = false
    insightLoading.value = false
  } finally {
    loading.value = false
  }
})

// 显示的岗位列表（分页后）
const displayJobs = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return hotJobs.value.slice(start, end).map(mapJobToDisplay)
})

// 显示的页码
const displayedPages = computed(() => {
  const pages: number[] = []
  const maxDisplay = 5
  let start = Math.max(1, currentPage.value - Math.floor(maxDisplay / 2))
  let end = Math.min(totalPages.value, start + maxDisplay - 1)
  
  if (end - start < maxDisplay - 1) {
    start = Math.max(1, end - maxDisplay + 1)
  }
  
  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  return pages
})

// 总页数
const totalPages = computed(() => Math.ceil(totalItems.value / pageSize.value))

// 分页切换
function goToPage(page: number) {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
  }
}

function prevPage() {
  if (currentPage.value > 1) {
    currentPage.value--
  }
}

function nextPage() {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
  }
}

// 打开岗位详情弹窗
async function openJobDetail(jobId: number) {
  showModal.value = true
  modalLoading.value = true
  modalError.value = ''
  selectedJobDetail.value = null
  
  try {
    const res = await getMarketJobDetail(jobId)
    if (res.data) {
      selectedJobDetail.value = res.data
    } else {
      modalError.value = '获取岗位详情失败'
    }
  } catch (e) {
    console.error('获取岗位详情失败:', e)
    modalError.value = '加载岗位详情失败'
  } finally {
    modalLoading.value = false
  }
}

// 关闭弹窗
function closeModal() {
  showModal.value = false
  selectedJobDetail.value = null
}

// 格式化时间
function formatTime(iso?: string) {
  if (!iso) return '—'
  try {
    return new Date(iso).toLocaleString()
  } catch {
    return iso
  }
}

// 能力维度标签映射
const capabilityLabels: Record<string, string> = {
  innovation: '创新能力',
  learning: '学习能力',
  resilience: '抗压能力',
  communication: '沟通能力',
  internship: '实习能力'
}
</script>
