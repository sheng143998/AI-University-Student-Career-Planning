<template>
  <div class="career-map-page">
    <!-- 背景 -->
    <div class="bg-container">
      <div class="grid-pattern"></div>
      <div class="center-glow"></div>
    </div>

    <!-- 头部 -->
    <header class="map-header">
      <button class="back-btn" @click="goBack">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M19 12H5M12 19l-7-7 7-7"/>
        </svg>
        <span>返回</span>
      </button>
      <h1 class="title">
        <span class="title-cn">职业发展路径图</span>
        <span class="title-en">CAREER PATH MAP</span>
      </h1>
      <div class="legend">
        <div class="legend-item">
          <span class="dot vertical"></span>
          <span>晋升路线</span>
        </div>
        <div class="legend-item">
          <span class="dot lateral"></span>
          <span>转型路线</span>
        </div>
      </div>
    </header>

    <!-- 主画布 -->
    <div 
      class="map-container" 
      ref="containerRef"
      @wheel="handleWheel"
      @mousedown="handleMouseDown"
      @mousemove="handleMouseMove"
      @mouseup="handleMouseUp"
      @mouseleave="handleMouseUp"
    >
      <div class="map-stage" :style="stageStyle" :class="{ 'dragging': isDragging }">
        <svg class="paths-svg" :viewBox="viewBox">
          <defs>
            <!-- 垂直路径渐变 -->
            <linearGradient id="verticalGrad" x1="0%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" stop-color="#1E3A8A" stop-opacity="0.3"/>
              <stop offset="50%" stop-color="#3B82F6" stop-opacity="1"/>
              <stop offset="100%" stop-color="#60A5FA" stop-opacity="0.3"/>
            </linearGradient>
            
            <!-- 横向路径渐变 -->
            <linearGradient id="lateralGrad" x1="0%" y1="0%" x2="100%" y2="0%">
              <stop offset="0%" stop-color="#1E3A8A" stop-opacity="0.3"/>
              <stop offset="50%" stop-color="#3B82F6" stop-opacity="1"/>
              <stop offset="100%" stop-color="#60A5FA" stop-opacity="0.3"/>
            </linearGradient>

          <!-- 发光滤镜 -->
          <filter id="glow" x="-50%" y="-50%" width="200%" height="200%">
            <feGaussianBlur stdDeviation="3" result="coloredBlur"/>
            <feMerge>
              <feMergeNode in="coloredBlur"/>
              <feMergeNode in="SourceGraphic"/>
            </feMerge>
          </filter>

          <!-- 中心发光 -->
          <filter id="centerGlow" x="-100%" y="-100%" width="300%" height="300%">
            <feGaussianBlur stdDeviation="8" result="blur"/>
            <feFlood flood-color="#3B82F6" flood-opacity="0.4"/>
            <feComposite in2="blur" operator="in"/>
            <feMerge>
              <feMergeNode/>
              <feMergeNode in="SourceGraphic"/>
            </feMerge>
          </filter>
        </defs>

        <!-- 向上晋升路径 -->
        <g class="vertical-paths-up">
          <path
            v-for="(path, idx) in verticalUpPaths"
            :key="'vu-' + idx"
            :d="path.d"
            fill="none"
            stroke="url(#verticalGrad)"
            stroke-width="3"
            stroke-linecap="round"
            filter="url(#glow)"
            class="path-line fiber-line"
          />
          <!-- 流动粒子 -->
          <circle
            v-for="(particle, idx) in verticalUpParticles"
            :key="'vup-' + idx"
            r="4"
            fill="#3B82F6"
            filter="url(#glow)"
            class="flow-particle"
            :style="{ offsetPath: `path('${particle.path}')`, animationDelay: particle.delay }"
          />
        </g>

        <!-- 向下基础路径 -->
        <g class="vertical-paths-down">
          <path
            v-for="(path, idx) in verticalDownPaths"
            :key="'vd-' + idx"
            :d="path.d"
            fill="none"
            stroke="url(#verticalGrad)"
            stroke-width="3"
            stroke-linecap="round"
            filter="url(#glow)"
            class="path-line fiber-line dashed"
          />
          <circle
            v-for="(particle, idx) in verticalDownParticles"
            :key="'vdp-' + idx"
            r="4"
            fill="#1E3A8A"
            filter="url(#glow)"
            class="flow-particle"
            :style="{ offsetPath: `path('${particle.path}')`, animationDelay: particle.delay }"
          />
        </g>

        <!-- 向左转型路径 -->
        <g class="lateral-paths-left">
          <path
            v-for="(path, idx) in lateralLeftPaths"
            :key="'ll-' + idx"
            :d="path.d"
            fill="none"
            stroke="url(#lateralGrad)"
            stroke-width="2.5"
            stroke-linecap="round"
            stroke-dasharray="8 4"
            filter="url(#glow)"
            class="path-line metro-line"
          />
          <circle
            v-for="(particle, idx) in lateralLeftParticles"
            :key="'llp-' + idx"
            r="3"
            fill="#3B82F6"
            filter="url(#glow)"
            class="flow-particle lateral-flow"
            :style="{ offsetPath: `path('${particle.path}')`, animationDelay: particle.delay }"
          />
        </g>

        <!-- 向右转型路径 -->
        <g class="lateral-paths-right">
          <path
            v-for="(path, idx) in lateralRightPaths"
            :key="'lr-' + idx"
            :d="path.d"
            fill="none"
            stroke="url(#lateralGrad)"
            stroke-width="2.5"
            stroke-linecap="round"
            stroke-dasharray="8 4"
            filter="url(#glow)"
            class="path-line metro-line"
          />
          <circle
            v-for="(particle, idx) in lateralRightParticles"
            :key="'lrp-' + idx"
            r="3"
            fill="#3B82F6"
            filter="url(#glow)"
            class="flow-particle lateral-flow"
            :style="{ offsetPath: `path('${particle.path}')`, animationDelay: particle.delay }"
          />
        </g>
        </svg>

        <!-- 节点层 -->
        <div class="nodes-layer">
        <!-- 中心节点 -->
        <div
          class="node center-node"
          :class="{ 'show': isAnimated }"
          :style="centerNodeStyle"
          @click="showDetail(centerNode)"
        >
          <div class="glow-ring"></div>
          <div class="node-card frosted-card center-card">
            <div class="badge current-badge">当前职位</div>
            <div class="node-icon center-icon">
              <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <path d="M12 6v6l4 2"/>
              </svg>
            </div>
            <div class="node-info">
              <span class="node-title">{{ centerNode.title }}</span>
              <span class="node-level">{{ centerNode.level }}</span>
              <span class="node-salary">{{ centerNode.salary }}</span>
            </div>
          </div>
        </div>

        <!-- 向上晋升节点 -->
        <div
          v-for="(node, idx) in verticalUpNodes"
          :key="'vu-' + idx"
          class="node vertical-up-node"
          :class="{ 'show': isAnimated }"
          :style="getVerticalUpNodeStyle(idx)"
          @click="showDetail(node)"
        >
          <div class="node-card frosted-card">
            <div class="badge promotion-badge">晋升方向</div>
            <div class="node-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 19V5M5 12l7-7 7 7"/>
              </svg>
            </div>
            <div class="node-info">
              <span class="node-title">{{ node.title }}</span>
              <span class="node-level">{{ node.level }}</span>
              <span class="node-salary">{{ node.salary }}</span>
            </div>
          </div>
        </div>

        <!-- 向下基础节点 -->
        <div
          v-for="(node, idx) in verticalDownNodes"
          :key="'vd-' + idx"
          class="node vertical-down-node"
          :class="{ 'show': isAnimated }"
          :style="getVerticalDownNodeStyle(idx)"
          @click="showDetail(node)"
        >
          <div class="node-card frosted-card">
            <div class="badge foundation-badge">基础路线</div>
            <div class="node-icon muted-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 5v14M19 12l-7 7-7-7"/>
              </svg>
            </div>
            <div class="node-info">
              <span class="node-title">{{ node.title }}</span>
              <span class="node-level">{{ node.level }}</span>
              <span class="node-salary">{{ node.salary }}</span>
            </div>
          </div>
        </div>

        <!-- 向左转型节点 -->
        <div
          v-for="(node, idx) in lateralLeftNodes"
          :key="'ll-' + idx"
          class="node lateral-node"
          :class="{ 'show': isAnimated }"
          :style="getLateralLeftNodeStyle(idx)"
          @click="showDetail(node)"
        >
          <div class="node-card frosted-card">
            <div class="badge transfer-badge">← 技能迁移</div>
            <div class="node-icon lateral-left-icon">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M16 3h5v5M8 3H3v5M3 16v5h5M21 16v5h-5"/>
              </svg>
            </div>
            <div class="node-info">
              <span class="node-title">{{ node.title }}</span>
              <span class="node-level">{{ node.level }}</span>
              <span class="node-salary">{{ node.salary }}</span>
            </div>
          </div>
        </div>

        <!-- 向右转型节点 -->
        <div
          v-for="(node, idx) in lateralRightNodes"
          :key="'lr-' + idx"
          class="node lateral-node"
          :class="{ 'show': isAnimated }"
          :style="getLateralRightNodeStyle(idx)"
          @click="showDetail(node)"
        >
          <div class="node-card frosted-card">
            <div class="badge transfer-badge">技能迁移 →</div>
            <div class="node-icon lateral-right-icon">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M16 3h5v5M8 3H3v5M3 16v5h5M21 16v5h-5"/>
              </svg>
            </div>
            <div class="node-info">
              <span class="node-title">{{ node.title }}</span>
              <span class="node-level">{{ node.level }}</span>
              <span class="node-salary">{{ node.salary }}</span>
            </div>
          </div>
        </div>
        </div>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <Transition name="modal">
      <div v-if="selectedNode" class="modal-overlay" @click.self="closeDetail">
        <div class="modal-content frosted-card">
          <button class="modal-close" @click="closeDetail">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M18 6L6 18M6 6l12 12"/>
            </svg>
          </button>
          
          <div class="modal-header">
            <div class="modal-icon" :class="getIconClass(selectedNode)">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <path d="M12 6v6l4 2"/>
              </svg>
            </div>
            <div>
              <h3 class="modal-title">{{ selectedNode.title }}</h3>
              <p class="modal-level">{{ selectedNode.level }}</p>
            </div>
          </div>

          <div class="modal-section">
            <h4 class="section-title">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 2v20M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6"/>
              </svg>
              薪资范围
            </h4>
            <p class="salary-value">{{ selectedNode.salary }}</p>
          </div>

          <div class="modal-section">
            <h4 class="section-title">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14.7 6.3a1 1 0 000 1.4l1.6 1.6a1 1 0 001.4 0l3.77-3.77a6 6 0 01-7.94 7.94l-6.91 6.91a2.12 2.12 0 01-3-3l6.91-6.91a6 6 0 017.94-7.94l-3.76 3.76z"/>
              </svg>
              核心技能
            </h4>
            <div class="skills-grid">
              <div v-for="skill in selectedNode.skills.slice(0, 10)" :key="skill.name" class="skill-item">
                <span class="skill-name">{{ skill.name }}</span>
                <div class="skill-bar">
                  <div class="skill-fill" :style="{ width: skill.level + '%' }"></div>
                </div>
                <span class="skill-level">{{ skill.level }}%</span>
              </div>
            </div>
          </div>

          <div class="modal-section" v-if="selectedNode.description">
            <h4 class="section-title">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/>
                <path d="M14 2v6h6M16 13H8M16 17H8M10 9H8"/>
              </svg>
              职位描述
            </h4>
            <p class="description-text">{{ selectedNode.description }}</p>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import type { JobVerticalPathDetail, JobTransitionPathDetail, JobDetail } from '@/api/roadmap'
import { getVerticalPathByJobName, recommendTransitionPathsByJobName, isApiSuccess } from '@/api/roadmap'

const router = useRouter()
const route = useRoute()
const isAnimated = ref(false)
const selectedNode = ref<CareerNode | null>(null)
const loading = ref(false)

const containerRef = ref<HTMLElement | null>(null)

const DESIGN_W = 1400
const DESIGN_H = 900
const scale = ref(1)
const stageOffsetX = ref(0)
const stageOffsetY = ref(0)
let ro: ResizeObserver | null = null

// 拖动状态
const isDragging = ref(false)
const dragStartX = ref(0)
const dragStartY = ref(0)
const dragStartOffsetX = ref(0)
const dragStartOffsetY = ref(0)

// 视图框
const viewBox = ref(`0 0 ${DESIGN_W} ${DESIGN_H}`)

// 画布中心点
const centerX = 700
const centerY = 500

const stageStyle = computed<Record<string, string>>(() => ({
  width: `${DESIGN_W}px`,
  height: `${DESIGN_H}px`,
  transform: `translate(${stageOffsetX.value}px, ${stageOffsetY.value}px) scale(${scale.value})`,
}))

function updateStageTransform() {
  const el = containerRef.value
  if (!el) return
  const rect = el.getBoundingClientRect()
  const s = Math.min(rect.width / DESIGN_W, rect.height / DESIGN_H)
  scale.value = Number.isFinite(s) && s > 0 ? s : 1
  stageOffsetX.value = Math.max(0, (rect.width - DESIGN_W * scale.value) / 2)
  stageOffsetY.value = Math.max(0, (rect.height - DESIGN_H * scale.value) / 2)
}

// 鼠标滚轮缩放
function handleWheel(e: WheelEvent) {
  e.preventDefault()
  const delta = e.deltaY > 0 ? -0.1 : 0.1
  const newScale = Math.max(0.3, Math.min(3, scale.value + delta))
  
  // 以鼠标位置为中心缩放
  const el = containerRef.value
  if (!el) return
  const rect = el.getBoundingClientRect()
  const mouseX = e.clientX - rect.left
  const mouseY = e.clientY - rect.top
  
  // 计算缩放前鼠标在画布上的位置
  const beforeX = (mouseX - stageOffsetX.value) / scale.value
  const beforeY = (mouseY - stageOffsetY.value) / scale.value
  
  scale.value = newScale
  
  // 调整偏移使鼠标位置保持不变
  stageOffsetX.value = mouseX - beforeX * newScale
  stageOffsetY.value = mouseY - beforeY * newScale
}

// 鼠标拖动开始
function handleMouseDown(e: MouseEvent) {
  if (e.button !== 0) return // 只响应左键
  isDragging.value = true
  dragStartX.value = e.clientX
  dragStartY.value = e.clientY
  dragStartOffsetX.value = stageOffsetX.value
  dragStartOffsetY.value = stageOffsetY.value
}

// 鼠标拖动中
function handleMouseMove(e: MouseEvent) {
  if (!isDragging.value) return
  const dx = e.clientX - dragStartX.value
  const dy = e.clientY - dragStartY.value
  stageOffsetX.value = dragStartOffsetX.value + dx
  stageOffsetY.value = dragStartOffsetY.value + dy
}

// 鼠标拖动结束
function handleMouseUp() {
  isDragging.value = false
}

// 重置视图
function resetView() {
  updateStageTransform()
}

// 布局常量 - 增加间距避免重叠
const VERTICAL_SPACING = 210   // 垂直方向节点间距（上下层间距）
const LATERAL_SPACING = 190    // 横向方向节点间距（左右列间距）

// 中心节点与第一圈节点的间距（决定“呼吸感”）
const VERTICAL_GAP = 70
const LATERAL_GAP = 90

// 卡片尺寸常量 - 用于精确计算路径连接点
const CARD_WIDTH = 172         // 标准卡片宽度
const CARD_HEIGHT = 128        // 卡片高度
const CENTER_CARD_WIDTH = 240  // 中心卡片宽度
const CENTER_CARD_HEIGHT = 176 // 中心卡片高度

type AnchorSide = 'top' | 'bottom' | 'left' | 'right'

function getCenterAnchor(side: AnchorSide): { x: number; y: number } {
  switch (side) {
    case 'top':
      return { x: centerX, y: centerY - CENTER_CARD_HEIGHT / 2 }
    case 'bottom':
      return { x: centerX, y: centerY + CENTER_CARD_HEIGHT / 2 }
    case 'left':
      return { x: centerX - CENTER_CARD_WIDTH / 2, y: centerY }
    case 'right':
      return { x: centerX + CENTER_CARD_WIDTH / 2, y: centerY }
  }
}

function getNodeCenter(type: 'vertical-up' | 'vertical-down' | 'lateral-left' | 'lateral-right', idx: number): { x: number; y: number } {
  if (type === 'vertical-up') {
    const base = centerY - CENTER_CARD_HEIGHT / 2 - VERTICAL_GAP - CARD_HEIGHT / 2
    return { x: centerX, y: base - idx * VERTICAL_SPACING }
  }
  if (type === 'vertical-down') {
    const base = centerY + CENTER_CARD_HEIGHT / 2 + VERTICAL_GAP + CARD_HEIGHT / 2
    return { x: centerX, y: base + idx * VERTICAL_SPACING }
  }
  if (type === 'lateral-left') {
    const x = centerX - CENTER_CARD_WIDTH / 2 - LATERAL_GAP - CARD_WIDTH / 2
    const y = centerY - ((lateralLeftNodes.value.length - 1) * LATERAL_SPACING) / 2 + idx * LATERAL_SPACING
    return { x, y }
  }

  const x = centerX + CENTER_CARD_WIDTH / 2 + LATERAL_GAP + CARD_WIDTH / 2
  const y = centerY - ((lateralRightNodes.value.length - 1) * LATERAL_SPACING) / 2 + idx * LATERAL_SPACING
  return { x, y }
}

function getNodeAnchor(type: 'vertical-up' | 'vertical-down' | 'lateral-left' | 'lateral-right', idx: number, side: AnchorSide): { x: number; y: number } {
  const c = getNodeCenter(type, idx)
  switch (side) {
    case 'top':
      return { x: c.x, y: c.y - CARD_HEIGHT / 2 }
    case 'bottom':
      return { x: c.x, y: c.y + CARD_HEIGHT / 2 }
    case 'left':
      return { x: c.x - CARD_WIDTH / 2, y: c.y }
    case 'right':
      return { x: c.x + CARD_WIDTH / 2, y: c.y }
  }
}

// 节点类型
interface CareerNode {
  id: string
  title: string
  level: string
  salary: string
  skills: Array<{ name: string; level: number }>
  description?: string
  type: 'center' | 'vertical-up' | 'vertical-down' | 'lateral-left' | 'lateral-right'
  jobId?: number
  stepValue?: number  // 用于排序垂直节点
}

// 中心节点
const centerNode = ref<CareerNode>({
  id: 'current',
  title: '互联网科研人员',
  level: '中级岗',
  salary: '15K - 25K',
  type: 'center',
  skills: [
    { name: '数据分析', level: 75 },
    { name: '研究方法', level: 70 },
    { name: 'Python/R', level: 65 },
    { name: '机器学习', level: 60 }
  ],
  description: '负责互联网领域的科学研究、数据分析和前沿技术创新。'
})

// 晋升路线节点（上）
const verticalUpNodes = ref<CareerNode[]>([])

// 基础路线节点（下）
const verticalDownNodes = ref<CareerNode[]>([])

// 转型路线节点（左）
const lateralLeftNodes = ref<CareerNode[]>([])

// 转型路线节点（右）
const lateralRightNodes = ref<CareerNode[]>([])

// 从后端获取职业路径数据
// From backend get career path data
async function loadCareerPaths() {
  loading.value = true
  
  const jobName = route.query.title as string || centerNode.value.title
  const jobLevel = route.query.level as string || ''
  console.log('[CareerMap] Loading career paths for job:', jobName, 'level:', jobLevel)
  
  // Update center node title
  if (jobName) {
    centerNode.value.title = jobName
  }

  // Update center node level (avoid always showing default '中级岗')
  if (jobLevel) {
    centerNode.value.level = jobLevel
  }
  
  try {
    // Fetch vertical path and transition recommendations in parallel
    const [verticalResult, transitionResult] = await Promise.all([
      getVerticalPathByJobName(jobName, jobLevel),
      recommendTransitionPathsByJobName(jobName, jobLevel)
    ])
    
    // Process vertical path
    if (isApiSuccess(verticalResult.code) && verticalResult.data) {
      console.log('[CareerMap] Vertical path data:', verticalResult.data)

      // If backend returns center job level, override display to keep consistent
      if ((verticalResult.data as any).jobLevelName || (verticalResult.data as any).jobLevel) {
        centerNode.value.level = (verticalResult.data as any).jobLevelName || (verticalResult.data as any).jobLevel
      }

      processVerticalPaths(verticalResult.data)
    } else {
      console.warn('[CareerMap] Vertical path API failed, using default data')
      useDefaultData()
    }
    
    // Process transition recommendations
    if (isApiSuccess(transitionResult.code) && transitionResult.data) {
      console.log('[CareerMap] Transition data:', transitionResult.data)
      processTransitionRecommendations(transitionResult.data)
    } else {
      console.warn('[CareerMap] Transition API failed')
    }
    
  } catch (error) {
    console.error('[CareerMap] API call failed:', error)
    useDefaultData()
  }
  
  loading.value = false
  // Trigger animation
  setTimeout(() => {
    isAnimated.value = true
  }, 100)
}

// 处理晋升路径数据
function processVerticalPaths(data: JobVerticalPathDetail) {
  // 清空现有数据
  verticalUpNodes.value = []
  verticalDownNodes.value = []
  
  if (!data.paths || data.paths.length === 0) {
    console.log('[CareerMap] 没有晋升路径数据')
    return
  }
  
  console.log('[CareerMap] 处理晋升路径:', data.paths.length, '条路径')
  
  data.paths.forEach((path, pathIndex) => {
    if (!path.pathSteps || path.pathSteps.length === 0) return
    
    path.pathSteps.forEach((step, stepIndex) => {
      const node: CareerNode = {
        id: `vu-${pathIndex}-${stepIndex}`,
        title: step.jobName,
        level: step.jobLevelName || '中级岗',
        salary: step.salaryRange || '薪资面议',
        type: step.step > 0 ? 'vertical-up' : 'vertical-down',
        skills: step.skills?.map((skill: string, idx: number) => ({
          name: skill,
          level: Math.max(30, 90 - idx * 15)
        })) || [],
        description: `预计耗时${step.avgTimeMonths || '?'}个月，难度${step.difficulty || '?'}级`,
        jobId: step.jobId,
        stepValue: step.step  // 保存step值用于排序
      }
      
      // step.step > 0 表示晋升方向，step.step < 0 表示基础方向
      if (step.step > 0) {
        verticalUpNodes.value.push(node)
      } else if (step.step < 0) {
        verticalDownNodes.value.push(node)
      }
    })
  })
  
  // 按stepValue排序向下节点：MID(-1) 应该最靠近中心，然后是 JUNIOR(-2)，最后是 INTERNSHIP(-3)
  // 降序排序：-1 > -2 > -3
  verticalDownNodes.value.sort((a, b) => (b.stepValue || 0) - (a.stepValue || 0))
  
  console.log('[CareerMap] 向上晋升节点:', verticalUpNodes.value.length)
  console.log('[CareerMap] 向下基础节点:', verticalDownNodes.value.length)
  
  // 打印排序后的节点顺序
  verticalDownNodes.value.forEach((node, idx) => {
    console.log(`[CareerMap] 向下节点[${idx}]:`, node.title, node.level, 'stepValue:', node.stepValue)
  })
  
  // 限制节点数量避免 overcrowding
  verticalUpNodes.value = verticalUpNodes.value.slice(0, 6)
  verticalDownNodes.value = verticalDownNodes.value.slice(0, 6)
}

// 处理换岗路径数据
function processTransitionPaths(data: JobTransitionPathDetail) {
  lateralLeftNodes.value = []
  lateralRightNodes.value = []
  
  if (!data.transitionPaths || data.transitionPaths.length === 0) {
    console.log('[CareerMap] 没有换岗路径数据')
    return
  }
  
  console.log('[CareerMap] 处理换岗路径:', data.transitionPaths.length, '条路径')
  
  const leftNodes: CareerNode[] = []
  const rightNodes: CareerNode[] = []
  const seen = new Set<string>()
  
  data.transitionPaths.forEach((path: any, index: number) => {
    const dedupeKey = String(path.toJobId ?? path.id ?? path.toJobName ?? '')
    if (dedupeKey && seen.has(dedupeKey)) return
    if (dedupeKey) seen.add(dedupeKey)

    const node: CareerNode = {
      id: `lt-${index}`,
      title: path.toJobName,
      level: path.toJobLevelName || '转型推荐',
      salary: path.salaryRange || '薪资面议',
      type: index % 2 === 0 ? 'lateral-left' : 'lateral-right',
      skills: path.requiredSkillsGap?.map((gap: any) => ({
        name: gap.skill,
        level: Math.round((gap.level || 0) * 100)
      })) || [],
      description: `匹配度${Math.round((path.similarityScore || 0) * 100)}%，转型难度${path.transitionDifficulty || '?'}级，预计${path.avgTransitionTimeMonths || '?'}个月`,
      jobId: path.toJobId
    }
    
    if (index % 2 === 0) {
      leftNodes.push(node)
    } else {
      rightNodes.push(node)
    }
  })
  
  lateralLeftNodes.value = leftNodes.slice(0, 4)
  lateralRightNodes.value = rightNodes.slice(0, 4)
  
  console.log('[CareerMap] 向左转型节点:', lateralLeftNodes.value.length)
  console.log('[CareerMap] 向右转型节点:', lateralRightNodes.value.length)
}

// Process transition recommendations from backend (UserTransitionRecommendation type)
function processTransitionRecommendations(data: any) {
  lateralLeftNodes.value = []
  lateralRightNodes.value = []
  
  if (!data.recommendations || data.recommendations.length === 0) {
    console.log('[CareerMap] No transition recommendations')
    return
  }
  
  console.log('[CareerMap] Processing transition recommendations:', data.recommendations.length)
  
  const leftNodes: CareerNode[] = []
  const rightNodes: CareerNode[] = []
  const seen = new Set<string>()
  
  data.recommendations.forEach((rec: any, index: number) => {
    const dedupeKey = String(rec.toJobId ?? rec.id ?? rec.toJobName ?? '')
    if (dedupeKey && seen.has(dedupeKey)) return
    if (dedupeKey) seen.add(dedupeKey)

    const node: CareerNode = {
      id: `lt-${index}`,
      title: rec.toJobName,
      level: rec.toJobLevelName || 'transform recommendation',
      salary: rec.salaryRange || 'salary negotiable',
      type: 'lateral-left',
      skills: rec.requiredSkillsGap?.map((gap: any, idx: number) => ({
        name: gap.skill,
        level: Math.max(30, 90 - idx * 15)
      })) || [],
      description: `match ${Math.round((rec.matchScore || 0) * 100)}%, difficulty ${rec.transitionDifficulty || '?'}/10, estimate ${rec.avgTransitionTimeMonths || '?'} months`,
      jobId: rec.toJobId
    }
    
    // Distribute evenly: first half to left, second half to right
    const halfSize = Math.ceil(data.recommendations.length / 2)
    if (leftNodes.length < halfSize) {
      node.type = 'lateral-left'
      leftNodes.push(node)
    } else {
      node.type = 'lateral-right'
      rightNodes.push(node)
    }
  })
  
  lateralLeftNodes.value = leftNodes.slice(0, 4)
  lateralRightNodes.value = rightNodes.slice(0, 4)
  
  console.log('[CareerMap] Left transition nodes:', lateralLeftNodes.value.length)
  console.log('[CareerMap] Right transition nodes:', lateralRightNodes.value.length)
}

// 更新中心节点信息
function updateCenterNode(detail: JobDetail) {
  centerNode.value = {
    ...centerNode.value,
    title: detail.jobName,
    level: detail.jobLevelName || detail.jobLevel,
    salary: detail.salaryRange,
    skills: detail.requiredSkills?.map((skill, idx) => ({
      name: skill,
      level: Math.max(40, 80 - idx * 10)
    })) || [],
    description: detail.description
  }
}

// 使用默认数据（当API失败时）
function useDefaultData() {
  verticalUpNodes.value = [
    {
      id: 'vu1',
      title: '高级科研人员',
      level: '高级岗',
      salary: '25K - 40K',
      type: 'vertical-up',
      skills: [
        { name: '高级数据分析', level: 80 },
        { name: '团队领导', level: 75 },
        { name: '研究策略', level: 85 },
        { name: '项目管理', level: 70 }
      ]
    },
    {
      id: 'vu2',
      title: '首席科研专家',
      level: '专家岗',
      salary: '40K - 60K',
      type: 'vertical-up',
      skills: [
        { name: '创新战略', level: 90 },
        { name: '跨部门领导', level: 85 },
        { name: '行业研究', level: 95 },
        { name: '技术愿景', level: 88 }
      ]
    }
  ]
  
  verticalDownNodes.value = [
    {
      id: 'vd1',
      title: '初级科研人员',
      level: '初级岗',
      salary: '8K - 15K',
      type: 'vertical-down',
      skills: [
        { name: '数据收集', level: 55 },
        { name: '基础分析', level: 50 },
        { name: '报告撰写', level: 60 },
        { name: '研究工具', level: 45 }
      ]
    },
    {
      id: 'vd2',
      title: '科研实习生',
      level: '实习岗',
      salary: '3K - 5K',
      type: 'vertical-down',
      skills: [
        { name: '数据录入', level: 35 },
        { name: '文献综述', level: 30 },
        { name: '基础统计', level: 25 },
        { name: '学习能力', level: 40 }
      ]
    }
  ]
  
  lateralLeftNodes.value = [
    {
      id: 'll1',
      title: '数据分析师',
      level: '转型岗',
      salary: '12K - 20K',
      type: 'lateral-left',
      skills: [
        { name: 'SQL', level: 70 },
        { name: '数据可视化', level: 65 },
        { name: 'BI工具', level: 60 },
        { name: '商业智能', level: 55 }
      ]
    },
    {
      id: 'll2',
      title: '算法工程师',
      level: '转型岗',
      salary: '20K - 35K',
      type: 'lateral-left',
      skills: [
        { name: '算法设计', level: 80 },
        { name: '优化技术', level: 75 },
        { name: '数学建模', level: 85 },
        { name: '编程开发', level: 70 }
      ]
    }
  ]
  
  lateralRightNodes.value = [
    {
      id: 'lr1',
      title: '产品经理',
      level: '转型岗',
      salary: '15K - 30K',
      type: 'lateral-right',
      skills: [
        { name: '用户研究', level: 60 },
        { name: '需求分析', level: 65 },
        { name: '沟通协调', level: 70 },
        { name: '数据决策', level: 55 }
      ]
    },
    {
      id: 'lr2',
      title: 'AI研究员',
      level: '转型岗',
      salary: '20K - 40K',
      type: 'lateral-right',
      skills: [
        { name: '深度学习', level: 75 },
        { name: '神经网络', level: 70 },
        { name: 'PyTorch/TensorFlow', level: 65 },
        { name: '模型部署', level: 60 }
      ]
    }
  ]
}

// 节点位置样式
const centerNodeStyle = computed(() => ({
  left: `${centerX}px`,
  top: `${centerY}px`,
  animationDelay: '0s'
}))

function getVerticalUpNodeStyle(idx: number): Record<string, string> {
  const { x, y } = getNodeCenter('vertical-up', idx)
  return {
    left: `${x}px`,
    top: `${y}px`,
    animationDelay: `${(idx + 1) * 0.15}s`
  }
}

function getVerticalDownNodeStyle(idx: number): Record<string, string> {
  const { x, y } = getNodeCenter('vertical-down', idx)
  return {
    left: `${x}px`,
    top: `${y}px`,
    animationDelay: `${(idx + 1) * 0.15}s`
  }
}

function getLateralLeftNodeStyle(idx: number): Record<string, string> {
  const { x, y } = getNodeCenter('lateral-left', idx)
  return {
    left: `${x}px`,
    top: `${y}px`,
    animationDelay: `${(idx + verticalUpNodes.value.length + 1) * 0.12}s`
  }
}

function getLateralRightNodeStyle(idx: number): Record<string, string> {
  const { x, y } = getNodeCenter('lateral-right', idx)
  return {
    left: `${x}px`,
    top: `${y}px`,
    animationDelay: `${(idx + verticalUpNodes.value.length + lateralLeftNodes.value.length + 1) * 0.12}s`
  }
}

// 生成贝塞尔曲线路径
interface PathData {
  d: string
}

// 向上路径 - 从中心卡片上边缘连接到上方节点下边缘
const verticalUpPaths = computed<PathData[]>(() => {
  const paths: PathData[] = []
  const start = getCenterAnchor('top')
  
  verticalUpNodes.value.forEach((_, idx) => {
    const end = getNodeAnchor('vertical-up', idx, 'bottom')
    const midY = (start.y + end.y) / 2
    const d = `M ${start.x} ${start.y} C ${start.x} ${midY}, ${end.x} ${midY}, ${end.x} ${end.y}`
    paths.push({ d })
  })
  
  return paths
})

// 向下路径 - 从中心卡片下边缘连接到下方节点上边缘
const verticalDownPaths = computed<PathData[]>(() => {
  const paths: PathData[] = []
  const start = getCenterAnchor('bottom')
  
  verticalDownNodes.value.forEach((_, idx) => {
    const end = getNodeAnchor('vertical-down', idx, 'top')
    const midY = (start.y + end.y) / 2
    const d = `M ${start.x} ${start.y} C ${start.x} ${midY}, ${end.x} ${midY}, ${end.x} ${end.y}`
    paths.push({ d })
  })
  
  return paths
})

// 向左路径 - 从中心卡片左边缘连接到左侧节点右边缘
const lateralLeftPaths = computed<PathData[]>(() => {
  const paths: PathData[] = []
  const start = getCenterAnchor('left')
  
  lateralLeftNodes.value.forEach((_, idx) => {
    const end = getNodeAnchor('lateral-left', idx, 'right')
    const midX = (start.x + end.x) / 2
    const d = `M ${start.x} ${start.y} C ${midX} ${start.y}, ${midX} ${end.y}, ${end.x} ${end.y}`
    paths.push({ d })
  })
  
  return paths
})

// 向右路径 - 从中心卡片右边缘连接到右侧节点左边缘
const lateralRightPaths = computed<PathData[]>(() => {
  const paths: PathData[] = []
  const start = getCenterAnchor('right')
  
  lateralRightNodes.value.forEach((_, idx) => {
    const end = getNodeAnchor('lateral-right', idx, 'left')
    const midX = (start.x + end.x) / 2
    const d = `M ${start.x} ${start.y} C ${midX} ${start.y}, ${midX} ${end.y}, ${end.x} ${end.y}`
    paths.push({ d })
  })
  
  return paths
})

// 粒子数据
interface ParticleData {
  path: string
  delay: string
}

const verticalUpParticles = computed<ParticleData[]>(() => {
  return verticalUpPaths.value.map((p, idx) => ({
    path: p.d,
    delay: `${idx * 0.5}s`
  }))
})

const verticalDownParticles = computed<ParticleData[]>(() => {
  return verticalDownPaths.value.map((p, idx) => ({
    path: p.d,
    delay: `${idx * 0.5}s`
  }))
})

const lateralLeftParticles = computed<ParticleData[]>(() => {
  return lateralLeftPaths.value.map((p, idx) => ({
    path: p.d,
    delay: `${idx * 0.3}s`
  }))
})

const lateralRightParticles = computed<ParticleData[]>(() => {
  return lateralRightPaths.value.map((p, idx) => ({
    path: p.d,
    delay: `${idx * 0.3}s`
  }))
})

// 弹窗功能
function showDetail(node: CareerNode) {
  selectedNode.value = node
}

function closeDetail() {
  selectedNode.value = null
}

function getIconClass(node: CareerNode): string {
  switch (node.type) {
    case 'center':
      return 'center-icon'
    case 'vertical-up':
      return 'promotion-icon'
    case 'vertical-down':
      return 'foundation-icon'
    case 'lateral-left':
      return 'lateral-left-icon'
    case 'lateral-right':
      return 'lateral-right-icon'
    default:
      return 'lateral-left-icon'
  }
}

function goBack() {
  router.back()
}

// 入场动画
onMounted(() => {
  nextTick(() => {
    updateStageTransform()
    if (containerRef.value) {
      ro = new ResizeObserver(() => updateStageTransform())
      ro.observe(containerRef.value)
    }
    loadCareerPaths()
  })
})

onBeforeUnmount(() => {
  if (ro && containerRef.value) ro.unobserve(containerRef.value)
  ro = null
})
</script>

<style scoped>
/* 页面容器 */
.career-map-page {
  position: relative;
  width: 100%;
  height: 100vh;
  background: #FFFFFF;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
}

/* 背景 */
.bg-container {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

.grid-pattern {
  position: absolute;
  inset: 0;
  background-image: 
    linear-gradient(rgba(30, 58, 138, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(30, 58, 138, 0.04) 1px, transparent 1px);
  background-size: 40px 40px;
}

.center-glow {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.08) 0%, transparent 70%);
  transform: translate(-50%, -50%);
  pointer-events: none;
}

/* 头部 */
.map-header {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  padding: 20px 30px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  z-index: 100;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.95) 0%, transparent 100%);
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: #FFFFFF;
  border: 1px solid rgba(30, 58, 138, 0.15);
  border-radius: 10px;
  color: #1E3A8A;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.back-btn:hover {
  border-color: #3B82F6;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.15);
}

.title {
  text-align: center;
}

.title-cn {
  display: block;
  font-size: 22px;
  font-weight: 700;
  color: #1E3A8A;
  letter-spacing: 2px;
}

.title-en {
  display: block;
  font-size: 11px;
  color: #64748B;
  letter-spacing: 3px;
  margin-top: 4px;
}

.legend {
  display: flex;
  gap: 24px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #64748B;
  font-weight: 500;
}

.dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.dot.vertical {
  background: linear-gradient(135deg, #1E3A8A, #3B82F6);
  box-shadow: 0 2px 6px rgba(59, 130, 246, 0.3);
}

.dot.lateral {
  background: linear-gradient(135deg, #3B82F6, #60A5FA);
  box-shadow: 0 2px 6px rgba(96, 165, 250, 0.3);
}

/* 画布容器 */
.map-container {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
  cursor: grab;
  user-select: none;
}

.map-container:active {
  cursor: grabbing;
}

.map-stage {
  position: absolute;
  left: 0;
  top: 0;
  transform-origin: top left;
  transition: none;
}

.map-stage.dragging {
  pointer-events: none;
}

.paths-svg {
  position: absolute;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

/* 路径动画 */
.path-line {
  stroke-dasharray: 1000;
  stroke-dashoffset: 1000;
  animation: drawPath 1.5s ease-out forwards;
}

@keyframes drawPath {
  to { stroke-dashoffset: 0; }
}

.fiber-line {
  animation: drawPath 1.5s ease-out forwards, fiberFlow 2s linear infinite;
}

.fiber-line.dashed {
  stroke-dasharray: 12 6;
  animation: drawPath 1.5s ease-out forwards, dashedFlow 3s linear infinite;
}

.metro-line {
  animation: drawPath 1.5s ease-out forwards, metroFlow 4s linear infinite;
}

@keyframes fiberFlow {
  0% { stroke-dashoffset: 0; }
  100% { stroke-dashoffset: -20; }
}

@keyframes dashedFlow {
  0% { stroke-dashoffset: 0; }
  100% { stroke-dashoffset: -36; }
}

@keyframes metroFlow {
  0% { stroke-dashoffset: 0; }
  100% { stroke-dashoffset: -24; }
}

/* 流动粒子 */
.flow-particle {
  opacity: 0;
  animation: particleMove 3s linear infinite;
}

.lateral-flow {
  animation: particleMoveLateral 4s linear infinite;
}

@keyframes particleMove {
  0% {
    offset-distance: 0%;
    opacity: 0;
  }
  10% { opacity: 0.8; }
  90% { opacity: 0.8; }
  100% {
    offset-distance: 100%;
    opacity: 0;
  }
}

@keyframes particleMoveLateral {
  0% {
    offset-distance: 0%;
    opacity: 0;
  }
  10% { opacity: 0.8; }
  90% { opacity: 0.8; }
  100% {
    offset-distance: 100%;
    opacity: 0;
  }
}

/* 节点层 */
.nodes-layer {
  position: absolute;
  inset: 0;
}

/* 节点基础 */
.node {
  position: absolute;
  transform: translate(-50%, -50%);
  cursor: pointer;
  opacity: 0;
}

.node.show {
  opacity: 1;
  animation: nodePop 0.5s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
}

@keyframes nodePop {
  0% {
    opacity: 0;
    transform: translate(-50%, -50%) scale(0.5);
  }
  100% {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1);
  }
}

.node:hover {
  z-index: 10;
}

.node:hover .node-card {
  box-shadow: 0 20px 60px rgba(30, 58, 138, 0.18);
  transform: translateY(-4px);
}

/* 磨砂卡片 */
.frosted-card {
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(30, 58, 138, 0.12);
  border-radius: 16px;
  box-shadow: 
    0 4px 20px rgba(0, 0, 0, 0.06),
    0 1px 3px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
}

.glow-ring {
  position: absolute;
  inset: -18px;
  border: 2px solid rgba(59, 130, 246, 0.12);
  border-radius: 24px;
  animation: glowPulse 3s ease-in-out infinite;
}

@keyframes glowPulse {
  0%, 100% { 
    opacity: 0.3;
    transform: scale(1);
  }
  50% { 
    opacity: 0.5;
    transform: scale(1.02);
  }
}

/* 节点卡片 - 固定尺寸确保路径连接准确 */
.node-card {
  padding: 14px 18px;
  width: 172px;
  min-height: 128px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  position: relative;
  box-sizing: border-box;
}

/* 中心节点卡片 */
.center-node .node-card {
  width: 240px;
  min-height: 176px;
  padding: 18px 22px;
}

/* 徽章 */
.badge {
  position: absolute;
  top: -10px;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.5px;
  white-space: nowrap;
}

.current-badge {
  background: linear-gradient(135deg, #3B82F6, #60A5FA);
  color: #FFFFFF;
  left: 50%;
  transform: translateX(-50%);
}

.promotion-badge {
  background: linear-gradient(135deg, #1E3A8A, #3B82F6);
  color: #FFFFFF;
  left: 50%;
  transform: translateX(-50%);
}

.foundation-badge {
  background: linear-gradient(135deg, #64748B, #94A3B8);
  color: #FFFFFF;
  left: 50%;
  transform: translateX(-50%);
}

.transfer-badge {
  background: linear-gradient(135deg, #0EA5E9, #38BDF8);
  color: #FFFFFF;
  left: 50%;
  transform: translateX(-50%);
}

/* 节点图标 */
.node-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1E3A8A, #3B82F6);
  color: #FFFFFF;
}

.center-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: linear-gradient(135deg, #1E3A8A, #3B82F6);
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.3);
}

.muted-icon {
  background: linear-gradient(135deg, #64748B, #94A3B8);
}

.lateral-icon {
  background: linear-gradient(135deg, #0EA5E9, #38BDF8);
}

/* 节点信息 */
.node-info {
  text-align: center;
  width: 100%;
}

.node-title {
  display: block;
  font-size: 15px;
  font-weight: 600;
  color: #1E3A8A;
  line-height: 1.4;
  margin-bottom: 6px;
}

.node-level {
  display: block;
  font-size: 11px;
  color: #64748B;
  letter-spacing: 0.5px;
  margin-bottom: 6px;
}

.node-salary {
  display: block;
  font-size: 13px;
  color: #3B82F6;
  font-weight: 600;
}

/* 弹窗 */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.25);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

.modal-content {
  position: relative;
  width: 100%;
  max-width: 460px;
  padding: 32px;
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid rgba(30, 58, 138, 0.1);
  border-radius: 20px;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.12);
}

.modal-close {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(30, 58, 138, 0.05);
  border: none;
  border-radius: 10px;
  color: #64748B;
  cursor: pointer;
  transition: all 0.2s ease;
}

.modal-close:hover {
  background: rgba(30, 58, 138, 0.1);
  color: #1E3A8A;
}

.modal-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.modal-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #FFFFFF;
}

.modal-icon.center-icon {
  background: linear-gradient(135deg, #1E3A8A, #3B82F6);
}

.modal-icon.promotion-icon {
  background: linear-gradient(135deg, #1E3A8A, #3B82F6);
}

.modal-icon.foundation-icon {
  background: linear-gradient(135deg, #64748B, #94A3B8);
}

.modal-icon.lateral-icon {
  background: linear-gradient(135deg, #0EA5E9, #38BDF8);
}

.modal-title {
  font-size: 20px;
  font-weight: 700;
  color: #1E3A8A;
  margin: 0;
}

.modal-level {
  font-size: 13px;
  color: #64748B;
  margin: 4px 0 0;
}

.modal-section {
  margin-bottom: 20px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  font-weight: 600;
  color: #64748B;
  margin: 0 0 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.section-title svg {
  color: #3B82F6;
}

.salary-value {
  font-size: 26px;
  font-weight: 700;
  color: #3B82F6;
  margin: 0;
}

.skills-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skill-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.skill-name {
  flex: 0 0 130px;
  font-size: 13px;
  color: #1E3A8A;
  font-weight: 500;
}

.skill-bar {
  flex: 1;
  height: 8px;
  background: rgba(30, 58, 138, 0.08);
  border-radius: 4px;
  overflow: hidden;
}

.skill-fill {
  height: 100%;
  background: linear-gradient(90deg, #1E3A8A, #3B82F6);
  border-radius: 4px;
  transition: width 0.5s ease;
}

.skill-level {
  flex: 0 0 40px;
  font-size: 12px;
  color: #3B82F6;
  text-align: right;
  font-weight: 600;
}

.description-text {
  font-size: 14px;
  color: #64748B;
  line-height: 1.6;
  margin: 0;
}

/* 弹窗过渡动画 */
.modal-enter-active,
.modal-leave-active {
  transition: all 0.3s ease;
}

.modal-enter-active .modal-content,
.modal-leave-active .modal-content {
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .modal-content,
.modal-leave-to .modal-content {
  opacity: 0;
  transform: scale(0.9) translateY(20px);
}
</style>
