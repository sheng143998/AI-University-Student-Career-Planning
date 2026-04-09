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



        <!-- 视图切换按钮 -->

        <div class="flex items-center gap-2 bg-surface-container-low px-3 py-2 rounded-xl">

          <button

            type="button"

            class="px-3 py-1.5 text-xs font-medium rounded-lg transition-colors"

            :class="viewType === 'global' ? 'bg-primary text-white' : 'text-on-surface-variant hover:bg-surface-container-high'"

            @click="switchToGlobalView"

          >

            全局视图

          </button>

          <button

            type="button"

            class="px-3 py-1.5 text-xs font-medium rounded-lg transition-colors"

            :class="viewType === 'focused' ? 'bg-primary text-white' : 'text-on-surface-variant hover:bg-surface-container-high'"

            @click="resetFocus"

          >

            聚焦视图

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



    <div 

      ref="mapContainer"

      class="relative w-full h-[600px] bg-gradient-to-br from-surface-container-lowest via-surface-container-low to-surface-container rounded-[2rem] overflow-hidden border border-outline-variant/10 shadow-2xl"

      @mousedown="onDragStart"

      @mousemove="onDragMove"

      @mouseup="onDragEnd"

      @mouseleave="onDragEnd"

      @wheel="onWheel"

    >

      <!-- Grid Background -->

      <div class="absolute inset-0 opacity-30 pointer-events-none">

        <svg width="100%" height="100%" xmlns="http://www.w3.org/2000/svg">

          <defs>

            <pattern id="grid" width="40" height="40" patternUnits="userSpaceOnUse">

              <path d="M 40 0 L 0 0 0 40" fill="none" stroke="currentColor" stroke-width="0.5" class="text-outline-variant"/>

            </pattern>

            <radialGradient id="gridFade" cx="50%" cy="50%" r="70%">

              <stop offset="0%" stop-color="currentColor" stop-opacity="0.4"/>

              <stop offset="100%" stop-color="currentColor" stop-opacity="0"/>

            </radialGradient>

          </defs>

          <rect width="100%" height="100%" fill="url(#grid)" class="text-outline-variant"/>

        </svg>

      </div>



      <!-- SVG Canvas -->

      <svg 

        class="absolute w-full h-full"

        :style="svgTransformStyle"

        :viewBox="svgViewBox"

        preserveAspectRatio="xMidYMid meet"

      >

        <defs>

          <!-- Node Gradients -->

          <linearGradient id="nodeGradientCore" x1="0%" y1="0%" x2="100%" y2="100%">

            <stop offset="0%" stop-color="#6366f1"/>

            <stop offset="100%" stop-color="#4f46e5"/>

          </linearGradient>

          <linearGradient id="nodeGradientSecondary" x1="0%" y1="0%" x2="100%" y2="100%">

            <stop offset="0%" stop-color="#f8fafc"/>

            <stop offset="100%" stop-color="#e2e8f0"/>

          </linearGradient>

          <linearGradient id="nodeGradientActive" x1="0%" y1="0%" x2="100%" y2="100%">

            <stop offset="0%" stop-color="#818cf8"/>

            <stop offset="100%" stop-color="#6366f1"/>

          </linearGradient>

          

          <!-- Path Gradients -->

          <linearGradient id="pathGradientVertical" x1="0%" y1="100%" x2="0%" y2="0%">

            <stop offset="0%" stop-color="#6366f1" stop-opacity="0.3"/>

            <stop offset="50%" stop-color="#818cf8" stop-opacity="0.8"/>

            <stop offset="100%" stop-color="#a5b4fc" stop-opacity="0.3"/>

          </linearGradient>

          <linearGradient id="pathGradientLateral" x1="0%" y1="0%" x2="100%" y2="0%">

            <stop offset="0%" stop-color="#06b6d4" stop-opacity="0.3"/>

            <stop offset="50%" stop-color="#22d3ee" stop-opacity="0.8"/>

            <stop offset="100%" stop-color="#67e8f9" stop-opacity="0.3"/>

          </linearGradient>

          

          <!-- Glow Filters -->

          <filter id="glowCore" x="-50%" y="-50%" width="200%" height="200%">

            <feGaussianBlur stdDeviation="4" result="coloredBlur"/>

            <feMerge>

              <feMergeNode in="coloredBlur"/>

              <feMergeNode in="SourceGraphic"/>

            </feMerge>

          </filter>

          <filter id="glowActive" x="-50%" y="-50%" width="200%" height="200%">

            <feGaussianBlur stdDeviation="8" result="coloredBlur"/>

            <feMerge>

              <feMergeNode in="coloredBlur"/>

              <feMergeNode in="SourceGraphic"/>

            </feMerge>

          </filter>

          <filter id="shadowNode" x="-50%" y="-50%" width="200%" height="200%">

            <feDropShadow dx="0" dy="4" stdDeviation="6" flood-color="#1e1b4b" flood-opacity="0.15"/>

          </filter>

          

          <!-- Arrow Markers -->

          <marker

            v-for="marker in arrowMarkers"

            :key="marker.id"

            :id="marker.id"

            markerWidth="10"

            markerHeight="10"

            refX="9"

            refY="3"

            orient="auto"

            markerUnits="strokeWidth"

          >

            <path d="M0,0 L0,6 L9,3 z" :fill="marker.color" />

          </marker>

        </defs>

        

        <!-- Paths Layer -->

        <g class="paths-layer">

          <path

            v-for="(p, idx) in visiblePaths"

            :key="idx"

            class="roadmap-path"

            :class="pathClass(p)"

            :d="p.d"

            fill="none"

            :stroke="getPathGradient(p)"

            :stroke-width="p.width"

            :stroke-dasharray="p.dashArray"

            :stroke-dashoffset="pathDashOffset"

            :marker-end="p.showArrow ? `url(#arrow-${p.variant})` : undefined"

            :opacity="p.opacity"

          />

        </g>

      </svg>



      <!-- Nodes Layer -->

      <div 

        class="absolute inset-0 pointer-events-none"

        :style="nodesTransformStyle"

      >

        <button

          v-for="n in visibleNodes"

          :key="n.id"

          type="button"

          class="absolute group pointer-events-auto cursor-pointer"

          :class="nodeTransitionClass(n)"

          :style="nodePositionStyle(n)"

          @click="selectNode(n.id)"

          @mouseenter="onNodeHover(n.id)"

          @mouseleave="onNodeLeave"

        >

          <!-- Glow Ring -->

          <div 

            v-if="n.id === activeNodeId || hoveredNodeId === n.id"

            class="absolute inset-0 rounded-2xl animate-pulse-ring"

            :style="getGlowStyle(n)"

          ></div>

          

          <!-- Node Card -->

          <div

            class="relative flex items-center justify-center transition-all duration-300 transform"

            :class="getNodeCardClass(n)"

            :style="getNodeCardStyle(n)"

          >

            <!-- Core Node Content -->

            <template v-if="n.kind === 'core'">

              <div class="flex flex-col items-center justify-center relative z-10">

                <span class="material-symbols-outlined text-white text-2xl drop-shadow-md">{{ getNodeIcon(n) }}</span>

                <span class="text-[9px] font-bold text-white/90 mt-0.5 tracking-wider">{{ n.levelLabel }}</span>

              </div>

            </template>

            

            <!-- Secondary Node Content -->

            <template v-else>

              <div class="flex flex-col items-center justify-center relative z-10">

                <span 

                  class="material-symbols-outlined transition-colors duration-300" 

                  :class="n.id === activeNodeId ? 'text-white' : 'text-primary'"

                >{{ getNodeIcon(n) }}</span>

              </div>

            </template>

          </div>

          

          <!-- Node Label -->

          <div 

            class="absolute left-1/2 -translate-x-1/2 mt-2 text-center min-w-[90px] transition-all duration-300"

            :class="getLabelClass(n)"

          >

            <span 

              class="block text-[11px] font-semibold leading-tight transition-colors duration-300"

              :class="n.id === activeNodeId ? 'text-primary' : 'text-on-surface'"

            >{{ n.label }}</span>

            <p v-if="n.subLabel" class="text-[9px] text-on-surface-variant mt-0.5 leading-tight">{{ n.subLabel }}</p>

          </div>

        </button>

      </div>



        <div v-if="aiChip" class="absolute top-8 right-8">

          <div class="px-4 py-2 bg-primary-fixed text-primary text-xs font-bold rounded-full shadow-lg flex items-center gap-2">

            <span class="material-symbols-outlined text-sm">auto_awesome</span>

            {{ aiChip }}

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

            <button @click="goToCareerMap" class="w-full py-3 bg-gradient-to-r from-primary to-primary-container text-white font-bold rounded-xl shadow-lg hover:shadow-primary/20 transition-all flex items-center justify-center gap-2" type="button">

              定制我的进阶计划

              <span class="material-symbols-outlined text-sm">arrow_forward</span>

            </button>

          </div>

        </template>

      </div>

    </div>





    <div v-if="showRecommendations && recommendations" class="mt-8 space-y-6">

      <div class="flex items-center justify-between">

        <div>

          <h2 class="text-2xl font-bold font-headline text-on-surface flex items-center gap-2">

            <span class="material-symbols-outlined text-primary">auto_awesome</span>

            AI 个性化职业推荐

          </h2>

          <p class="text-sm text-on-surface-variant mt-1">基于您的简历分析和当前岗位: <span class="font-semibold text-primary">{{ recommendations.currentJob }}</span></p>

        </div>

        <button @click="showRecommendations = !showRecommendations" class="p-2 hover:bg-surface-container-high rounded-lg transition-colors">

          <span class="material-symbols-outlined text-on-surface-variant">{{ showRecommendations ? 'visibility_off' : 'visibility' }}</span>

        </button>

      </div>



      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">

        <div class="bg-gradient-to-br from-primary/5 to-primary-container/10 p-6 rounded-[1.5rem] border border-primary/20 shadow-sm">

          <div class="flex items-center gap-3 mb-4">

            <div class="w-10 h-10 bg-primary rounded-xl flex items-center justify-center">

              <span class="material-symbols-outlined text-white">trending_up</span>

            </div>

            <div>

              <h3 class="text-lg font-bold text-on-surface">垂直晋升路径</h3>

              <p class="text-xs text-on-surface-variant">与您当前岗位最匹配的职业晋升路径</p>

            </div>

            <div class="ml-auto px-3 py-1 bg-primary/20 rounded-full">

              <span class="text-sm font-bold text-primary">{{ (recommendations.verticalPath?.similarityScore || 0) * 100 }}% 匹配度</span>

            </div>

          </div>



          <div v-if="recommendations.verticalPath" class="space-y-3">

            <div class="flex items-center gap-2 text-sm text-on-surface-variant mb-3">

              <span class="material-symbols-outlined text-sm">work</span>

              <span>匹配岗位: <span class="font-semibold text-on-surface">{{ recommendations.verticalPath.matchedJobName }}</span></span>

            </div>



            <div class="relative">

              <div class="flex items-center justify-between">

                <template v-for="(node, idx) in recommendations.verticalPath.nodes" :key="node.id">

                  <div class="flex flex-col items-center relative" style="flex: 1;">

                    <div

                      class="w-16 h-16 rounded-xl flex items-center justify-center shadow-md transition-all cursor-pointer hover:scale-105"

                      :class="node.isCurrentLevel ? 'bg-primary ring-2 ring-primary ring-offset-2' : (idx < (recommendations.verticalPath?.currentLevelIndex || 0) ? 'bg-outline-variant' : 'bg-white border border-outline-variant')"

                      @click="viewRecommendationPath(recommendations.verticalPath?.categoryCode || '')"

                    >

                      <div class="text-center">

                        <span class="material-symbols-outlined text-lg" :class="node.isCurrentLevel ? 'text-white' : 'text-on-surface'">{{ node.isCurrentLevel ? 'star' : 'work' }}</span>

                      </div>

                    </div>

                    <div class="mt-2 text-center px-1">

                      <p class="text-xs font-bold" :class="node.isCurrentLevel ? 'text-primary' : 'text-on-surface'">{{ node.levelName }}</p>

                      <p class="text-[10px] text-on-surface-variant truncate max-w-[80px]">{{ node.title }}</p>

                      <p class="text-[10px] text-primary font-semibold">{{ node.salaryRange }}</p>

                    </div>

                    <div v-if="node.isCurrentLevel" class="absolute -top-2 -right-1 px-1.5 py-0.5 bg-primary text-white text-[8px] font-bold rounded-full">

                      当前岗位

                    </div>

                  </div>

                  <div v-if="idx < recommendations.verticalPath.nodes.length - 1" class="flex-1 h-0.5 mx-1 mt-[-24px]"

                    :class="(idx < (recommendations.verticalPath?.currentLevelIndex || 0)) ? 'bg-outline-variant' : 'bg-primary/40'"></div>

                </template>

              </div>

            </div>



            <div class="mt-4 flex items-center justify-between text-sm">

              <div class="flex items-center gap-1 text-on-surface-variant">

                <span class="material-symbols-outlined text-sm">schedule</span>

                <span>预计 {{ recommendations.verticalPath.estimatedMonthsToNext }} 个月可晋升</span>

              </div>

              <button

                @click="viewRecommendationPath(recommendations.verticalPath?.categoryCode || '')"

                class="px-4 py-2 bg-primary text-white rounded-lg font-semibold text-sm hover:bg-primary/90 transition-colors flex items-center gap-1"

              >

                <span>查看路径</span>

                <span class="material-symbols-outlined text-sm">arrow_forward</span>

              </button>

            </div>

          </div>

        </div>



        <div class="bg-surface-container-lowest p-6 rounded-[1.5rem] border border-outline-variant/20 shadow-sm">

          <div class="flex items-center gap-3 mb-4">

            <div class="w-10 h-10 bg-tertiary rounded-xl flex items-center justify-center">

              <span class="material-symbols-outlined text-white">swap_horiz</span>

            </div>

            <div>

              <h3 class="text-lg font-bold text-on-surface">横向换岗推荐</h3>

              <p class="text-xs text-on-surface-variant">AI 推荐的职业转型路径（至少2条）</p>

            </div>

          </div>



          <div v-if="recommendations.lateralPaths && recommendations.lateralPaths.length > 0" class="space-y-3">

            <div

              v-for="(path, idx) in recommendations.lateralPaths"

              :key="path.targetJobId"

              class="p-4 bg-white rounded-xl border border-outline-variant/10 hover:border-primary/30 hover:shadow-md transition-all cursor-pointer"

              @click="viewRecommendationPath(path.targetCategoryCode)"

            >

              <div class="flex items-start justify-between mb-2">

                <div class="flex-1">

                  <div class="flex items-center gap-2">

                    <span class="text-sm font-bold text-on-surface">{{ path.targetJobName }}</span>

                    <span class="px-2 py-0.5 rounded-full text-[10px] font-bold" :class="getDifficultyClass(path.transitionDifficulty)">

                      {{ getDifficultyText(path.transitionDifficulty) }}

                    </span>

                  </div>

                  <p class="text-xs text-on-surface-variant mt-1">{{ path.aiRecommendationReason }}</p>

                </div>

                <div class="text-right">

                  <div class="text-sm font-bold text-primary">{{ (path.matchScore || 0) * 100 }}%</div>

                  <div class="text-[10px] text-on-surface-variant">匹配度</div>

                </div>

              </div>



              <div class="flex items-center gap-4 text-xs text-on-surface-variant">

                <div class="flex items-center gap-1">

                  <span class="material-symbols-outlined text-sm">schedule</span>

                  <span>{{ path.estimatedMonths }} 个月</span>

                </div>

                <div v-if="path.requiredSkills && path.requiredSkills.length > 0" class="flex items-center gap-1">

                  <span class="material-symbols-outlined text-sm">add_circle</span>

                  <span>需补充 {{ path.requiredSkills.length }} 项技能</span>

                </div>

                <div v-if="path.possessedSkills && path.possessedSkills.length > 0" class="flex items-center gap-1">

                  <span class="material-symbols-outlined text-sm">check_circle</span>

                  <span>已具备 {{ path.possessedSkills.length }} 项技能</span>

                </div>

              </div>



              <div v-if="path.requiredSkills && path.requiredSkills.length > 0" class="mt-3 flex flex-wrap gap-1">

                <span v-for="skill in path.requiredSkills.slice(0, 4)" :key="skill" class="px-2 py-0.5 bg-tertiary/10 text-tertiary text-[10px] rounded-full">

                  {{ skill }}

                </span>

                <span v-if="path.requiredSkills.length > 4" class="px-2 py-0.5 bg-outline-variant/20 text-on-surface-variant text-[10px] rounded-full">

                  +{{ path.requiredSkills.length - 4 }} 项

                </span>

              </div>

            </div>

          </div>



          <div v-else class="text-center py-8 text-on-surface-variant">

            <span class="material-symbols-outlined text-4xl mb-2">info</span>

            <p class="text-sm">暂无横向换岗推荐</p>

          </div>

        </div>

      </div>

    </div>



    <div v-else-if="recommendationsLoading" class="mt-8 flex items-center justify-center py-12">

      <div class="flex items-center gap-3 text-on-surface-variant">

        <span class="material-symbols-outlined animate-spin">progress_activity</span>

        <span class="text-sm">正在加载个性化推荐...</span>

      </div>

    </div>

  </div>

</template>



<script setup lang="ts">

import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

import { getRoadmapGraph, getRoadmapNodeDetail, isApiSuccess, searchRoadmapNodes, type RoadmapGraph, type RoadmapNodeDetail, type RoadmapSearchItem } from '@/api/roadmap'

import { getPersonalizedRecommendations, type CareerPathRecommendation, type PathNode } from '@/api/roadmapRecommendation'



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

  pos: { left: string; top: string; x: number; y: number }

  searchable: string

  segment: Segment

  tagline: string

  description: string

  skills: Array<{ name: string; value: string }>

  salaryBands: Array<{ label: string; range: string; width: string; barClass: string }>

  categoryCode?: string

  opacity?: number

}



type Path = {

  d: string

  from: string

  to: string

  variant: 'primary' | 'secondary'

  mode: 'vertical' | 'lateral'

  segment: Segment

  color: string

  width: number

  dashArray: string

  showArrow: boolean

  opacity: number

  edgeType?: 'vertical' | 'lateral'

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



// 视图类型和动画状态

const viewType = ref<'global' | 'focused'>('global')

const currentCategoryCode = ref<string>('')

const isAnimating = ref(false)



// 视图变换参数

const viewTransform = ref({

  x: 0,

  y: 0,

  scale: 1,

  targetX: 0,

  targetY: 0,

  targetScale: 1

})



// SVG视口配置

const svgViewport = ref({

  width: 800,

  height: 600,

  minX: 0,

  minY: 0

})



// 个性化推荐数据

const recommendations = ref<CareerPathRecommendation | null>(null)

const recommendationsLoading = ref(false)

const showRecommendations = ref(true)



const activeNodeId = ref<string | null>(null)

const currentNodeDetail = ref<RoadmapNodeDetail | null>(null)

const hoveredNodeId = ref<string | null>(null)

const mapContainer = ref<HTMLElement | null>(null)



// Drag state

const isDragging = ref(false)

const dragStart = ref({ x: 0, y: 0 })

const dragOffset = ref({ x: 0, y: 0 })



// Path animation offset

const pathDashOffset = ref(0)

let pathAnimationId: number | null = null



// Start path animation

function startPathAnimation() {

  if (pathAnimationId) return

  

  function animate() {

    pathDashOffset.value = (pathDashOffset.value + 0.5) % 100

    pathAnimationId = requestAnimationFrame(animate)

  }

  pathAnimationId = requestAnimationFrame(animate)

}



// Stop path animation

function stopPathAnimation() {

  if (pathAnimationId) {

    cancelAnimationFrame(pathAnimationId)

    pathAnimationId = null

  }

}



// Get path gradient URL

function getPathGradient(p: Path): string {

  if (p.edgeType === 'vertical') {

    return 'url(#pathGradientVertical)'

  }

  return 'url(#pathGradientLateral)'

}



// Get node icon based on type

function getNodeIcon(n: RoadmapNode): string {

  const icons: Record<string, string> = {

    'core': 'star',

    'secondary': 'work'

  }

  return n.icon || icons[n.kind] || 'work'

}



// Get glow style for active/hovered nodes

function getGlowStyle(n: RoadmapNode): Record<string, string> {

  const isActive = n.id === activeNodeId.value

  const isHovered = n.id === hoveredNodeId.value

  

  if (isActive || isHovered) {

    return {

      background: isActive 

        ? 'radial-gradient(circle, rgba(99, 102, 241, 0.4) 0%, transparent 70%)' 

        : 'radial-gradient(circle, rgba(99, 102, 241, 0.2) 0%, transparent 70%)',

      transform: 'scale(1.5)',

      filter: 'blur(8px)'

    }

  }

  return {}

}



// Enhanced node card class

function getNodeCardClass(n: RoadmapNode): string {

  const isActive = n.id === activeNodeId.value

  const isHovered = n.id === hoveredNodeId.value

  const classes: string[] = []

  

  if (n.kind === 'core') {

    classes.push('w-16', 'h-16', 'rounded-2xl')

    if (isActive) {

      classes.push('bg-gradient-to-br', 'from-primary', 'to-primary-container', 'shadow-xl', 'scale-110')

    } else if (isHovered) {

      classes.push('bg-gradient-to-br', 'from-primary/90', 'to-primary-container/90', 'shadow-lg', 'scale-105')

    } else {

      classes.push('bg-gradient-to-br', 'from-primary', 'to-primary-container', 'shadow-md')

    }

  } else {

    classes.push('w-12', 'h-12', 'rounded-xl')

    if (isActive) {

      classes.push('bg-gradient-to-br', 'from-primary', 'to-primary-container', 'shadow-xl', 'scale-110', 'ring-2', 'ring-primary/30')

    } else if (isHovered) {

      classes.push('bg-white', 'shadow-lg', 'scale-105', 'border-2', 'border-primary/20')

    } else {

      classes.push('bg-white', 'shadow-md', 'border', 'border-outline-variant/50')

    }

  }

  

  return classes.join(' ')

}



// Node card inline style

function getNodeCardStyle(n: RoadmapNode): Record<string, string> {

  const isActive = n.id === activeNodeId.value

  return {

    transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',

    filter: isActive ? 'url(#glowActive)' : 'url(#shadowNode)'

  }

}



// Label class based on node state

function getLabelClass(n: RoadmapNode): string {

  const isActive = n.id === activeNodeId.value

  const isRelated = n.opacity && n.opacity > 0.5

  

  if (isActive) {

    return 'opacity-100 transform scale-105'

  }

  if (isRelated) {

    return 'opacity-100'

  }

  return 'opacity-50'

}



// Node hover handlers

function onNodeHover(nodeId: string) {

  hoveredNodeId.value = nodeId

}



function onNodeLeave() {

  hoveredNodeId.value = null

}



// Drag handlers for panning

function onDragStart(e: MouseEvent) {

  if (e.button !== 0) return

  isDragging.value = true

  dragStart.value = { x: e.clientX, y: e.clientY }

  dragOffset.value = { x: viewTransform.value.x, y: viewTransform.value.y }

}



function onDragMove(e: MouseEvent) {

  if (!isDragging.value) return

  

  const dx = e.clientX - dragStart.value.x

  const dy = e.clientY - dragStart.value.y

  

  viewTransform.value.x = dragOffset.value.x + dx

  viewTransform.value.y = dragOffset.value.y + dy

}



function onDragEnd() {

  isDragging.value = false

}



// Wheel handler for zoom

function onWheel(e: WheelEvent) {

  e.preventDefault()

  

  const delta = e.deltaY > 0 ? -0.1 : 0.1

  const newScale = Math.max(0.5, Math.min(3, viewTransform.value.scale + delta))

  

  viewTransform.value.scale = newScale

}



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

    pos: { 

      left: `${n.x}px`, 

      top: `${n.y}px`,

      x: n.x,

      y: n.y

    },

    searchable: `${n.title} ${n.label} ${n.subtitle}`,

    segment: activeSegment.value,

    tagline: n.subtitle || '',

    description: '',

    skills: [],

    salaryBands: [],

    categoryCode: (n as any).categoryCode,

    opacity: 1

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

    if (!graph) {

      nodes.value = []

      paths.value = []

      return

    }

    console.log('[Roadmap] graph data:', graph)

    nodes.value = (graph.nodes || []).map((n, idx) => toRoadmapNode(n, idx))



    const nodeIndex = new Map<string, { x: number; y: number }>()

    for (const n of graph.nodes || []) {

      nodeIndex.set(n.id, { x: n.x, y: n.y })

    }



    paths.value = (graph.paths || []).map((p) => {

      const from = nodeIndex.get(p.from) || { x: 0, y: 0 }

      const to = nodeIndex.get(p.to) || { x: 0, y: 0 }

      const isLateral = p.edgeType === 'lateral' || p.lineStyle === 'solid'

      const isDashed = p.lineStyle === 'dashed' || p.edgeType === 'vertical'

      return {

        d: generatePathD(from, to),

        from: p.from,

        to: p.to,

        variant: p.variant === 'primary' ? 'primary' : 'secondary',

        mode: verticalMode.value ? 'vertical' : 'lateral',

        segment: activeSegment.value,

        edgeType: p.edgeType || (isLateral ? 'lateral' : 'vertical'),

        color: p.variant === 'primary' ? '#0056d2' : '#c3c6d6',

        width: p.variant === 'primary' ? 2 : 1.5,

        dashArray: isDashed ? '6,4' : '0',

        showArrow: isLateral,

        opacity: 0.6

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

        const isLateral = p.edgeType === 'lateral' || p.lineStyle === 'solid'

        const isDashed = p.lineStyle === 'dashed' || p.edgeType === 'vertical'

        return {

          d: generatePathD(from, to),

          from: p.from,

          to: p.to,

          variant: p.variant === 'primary' ? 'primary' : 'secondary',

          mode: verticalMode.value ? 'vertical' : 'lateral',

          segment: activeSegment.value,

          edgeType: p.edgeType || (isLateral ? 'lateral' : 'vertical'),

          color: p.variant === 'primary' ? '#0056d2' : '#c3c6d6',

          width: p.variant === 'primary' ? 2 : 1.5,

          dashArray: isDashed ? '6,4' : '0',

          showArrow: isLateral,

          opacity: 0.6

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

    currentNodeDetail.value = res.data ?? null

  })

}



onMounted(async () => {

  await loadGraph()

  await loadRecommendations()

})



watch([verticalMode, activeSegment], async () => {

  await loadGraph()

})



// 加载个性化推荐

async function loadRecommendations() {

  recommendationsLoading.value = true

  try {

    const res = await getPersonalizedRecommendations()

    console.log('[Roadmap] recommendations:', res)

    if (isApiSuccess(res.code) && res.data) {

      recommendations.value = res.data

    }

  } catch (e) {

    console.error('[Roadmap] loadRecommendations error:', e)

  } finally {

    recommendationsLoading.value = false

  }

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



// 查看推荐路径详情

function viewRecommendationPath(categoryCode: string) {

  if (categoryCode) {

    currentCategoryCode.value = categoryCode

    viewType.value = 'focused'

    loadGraphWithCategory(categoryCode)

  }

}

// Navigate to career map
function goToCareerMap() {
  const activeNode = nodes.value.find(n => n.id === activeNodeId.value)
  router.push({
    name: 'career-map',
    query: {
      title: activeNode?.label || 'Internet Research Scientist (Mid-level)',
      level: activeNode?.subLabel || 'Mid-level'
    }
  })
}



const heatBars = [

  { height: '33%', class: 'bg-primary-fixed-dim' },

  { height: '50%', class: 'bg-primary-fixed-dim' },

  { height: '66%', class: 'bg-primary-fixed-dim' },

  { height: '100%', class: 'bg-primary' },

  { height: '83%', class: 'bg-primary-fixed-dim' }

]



// 箭头标记定义

const arrowMarkers = computed(() => [

  { id: 'arrow-primary', color: '#0056d2' },

  { id: 'arrow-secondary', color: '#c3c6d6' }

])



// SVG变换样式

const svgTransformStyle = computed(() => {

  const { x, y, scale } = viewTransform.value

  return {

    transform: `translate(${x}px, ${y}px) scale(${scale})`

  }

})



// SVG视口

const svgViewBox = computed(() => {

  const { minX, minY, width, height } = svgViewport.value

  return `${minX} ${minY} ${width} ${height}`

})



// 节点层变换样式

const nodesTransformStyle = computed(() => {

  const { x, y, scale } = viewTransform.value

  return {

    transform: `translate(${x}px, ${y}px) scale(${scale})`

  }

})



// 路径类名

function pathClass(p: Path) {

  return {

    'path-vertical': p.edgeType === 'vertical',

    'path-lateral': p.edgeType === 'lateral',

    'path-primary': p.variant === 'primary',

    'path-secondary': p.variant === 'secondary'

  }

}



// 节点过渡类名

function nodeTransitionClass(n: RoadmapNode) {

  return {

    'node-active': n.id === activeNodeId.value,

    'node-faded': n.opacity !== undefined && n.opacity < 0.5

  }

}



// 节点位置样式

function nodePositionStyle(n: RoadmapNode) {

  return {

    left: n.pos.left,

    top: n.pos.top,

    opacity: n.opacity ?? 1

  }

}



// 切换到全局视图

async function switchToGlobalView() {

  if (viewType.value === 'global') return

  viewType.value = 'global'

  currentCategoryCode.value = ''

  await loadGraph()

  resetViewTransform()

}



// 重置聚焦

async function resetFocus() {

  if (!currentCategoryCode.value) {

    await switchToGlobalView()

    return

  }

  await loadGraphWithCategory(currentCategoryCode.value)

}



// 重置视图变换

function resetViewTransform() {

  viewTransform.value = {

    x: 0,

    y: 0,

    scale: 1,

    targetX: 0,

    targetY: 0,

    targetScale: 1

  }

}



// 动画聚焦到节点

function animateFocusToNode(nodeId: string) {

  const node = nodes.value.find(n => n.id === nodeId)

  if (!node) return

  

  isAnimating.value = true

  

  // 计算目标位置（将节点移到中心）

  const containerWidth = 800

  const containerHeight = 600

  const targetX = containerWidth / 2 - node.pos.x

  const targetY = containerHeight / 2 - node.pos.y

  

  // 设置目标变换

  viewTransform.value.targetX = targetX

  viewTransform.value.targetY = targetY

  viewTransform.value.targetScale = 1.2

  

  // 执行动画

  animateTransform()

  

  // 高亮节点

  activeNodeId.value = nodeId

  

  // 降低其他节点透明度

  nodes.value.forEach(n => {

    if (n.id === nodeId) {

      n.opacity = 1

    } else if (isRelatedNode(n, node)) {

      n.opacity = 0.7

    } else {

      n.opacity = 0.3

    }

  })

}



// 判断是否为相关节点

function isRelatedNode(n1: RoadmapNode, n2: RoadmapNode): boolean {

  // 同一类别

  if (n1.categoryCode && n2.categoryCode && n1.categoryCode === n2.categoryCode) {

    return true

  }

  // 有路径连接

  return paths.value.some(p => 

    (p.from === n1.id && p.to === n2.id) || 

    (p.from === n2.id && p.to === n1.id)

  )

}



// 动画执行

function animateTransform() {

  const { x, y, scale, targetX, targetY, targetScale } = viewTransform.value

  

  // 线性插值

  const speed = 0.1

  const newX = x + (targetX - x) * speed

  const newY = y + (targetY - y) * speed

  const newScale = scale + (targetScale - scale) * speed

  

  viewTransform.value.x = newX

  viewTransform.value.y = newY

  viewTransform.value.scale = newScale

  

  // 检查是否接近目标

  const threshold = 0.5

  if (

    Math.abs(targetX - newX) > threshold ||

    Math.abs(targetY - newY) > threshold ||

    Math.abs(targetScale - newScale) > 0.01

  ) {

    requestAnimationFrame(animateTransform)

  } else {

    viewTransform.value.x = targetX

    viewTransform.value.y = targetY

    viewTransform.value.scale = targetScale

    isAnimating.value = false

  }

}



// 获取难度等级样式

function getDifficultyClass(difficulty: number): string {

  if (difficulty <= 2) return 'bg-green-100 text-green-700'

  if (difficulty <= 3) return 'bg-yellow-100 text-yellow-700'

  return 'bg-red-100 text-red-700'

}



// Get difficulty text

function getDifficultyText(difficulty: number): string {

  if (difficulty <= 2) return 'Easy'

  if (difficulty <= 3) return 'Medium'

  return 'Hard'

}



// Start animations on mount

onMounted(async () => {

  await loadGraph()

  await loadRecommendations()

  startPathAnimation()

})



// Cleanup on unmount

onUnmounted(() => {

  stopPathAnimation()

})

</script>



<style scoped>

/* Path animation for dashed lines */

.roadmap-path {

  transition: stroke-opacity 0.3s ease, stroke-width 0.3s ease;

}



.path-vertical {

  animation: dash-flow 2s linear infinite;

}



.path-lateral {

  animation: dash-flow-reverse 3s linear infinite;

}



@keyframes dash-flow {

  to {

    stroke-dashoffset: -20;

  }

}



@keyframes dash-flow-reverse {

  to {

    stroke-dashoffset: 20;

  }

}



/* Pulse ring animation for active nodes */

.animate-pulse-ring {

  animation: pulse-ring 1.5s cubic-bezier(0.4, 0, 0.6, 1) infinite;

}



@keyframes pulse-ring {

  0%, 100% {

    opacity: 0.5;

    transform: scale(1);

  }

  50% {

    opacity: 0.8;

    transform: scale(1.2);

  }

}



/* Node active state */

.node-active {

  z-index: 10;

}



/* Node faded state */

.node-faded {

  filter: grayscale(30%);

}



/* Smooth cursor for dragging */

.map-container-dragging {

  cursor: grabbing;

}



.map-container {

  cursor: grab;

}



/* Path hover effect */

.roadmap-path:hover {

  stroke-width: 4;

  stroke-opacity: 1;

}



/* Glow effect for nodes */

.glow-effect {

  filter: drop-shadow(0 0 8px rgba(99, 102, 241, 0.5));

}



/* Gradient text for labels */

.gradient-text {

  background: linear-gradient(135deg, #6366f1 0%, #818cf8 100%);

  -webkit-background-clip: text;

  -webkit-text-fill-color: transparent;

  background-clip: text;

}

</style>

