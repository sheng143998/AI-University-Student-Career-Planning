<template>
  <div class="space-y-10">
    <header class="flex flex-col md:flex-row md:items-end justify-between gap-6">
      <div>
        <h1 class="text-4xl font-extrabold font-headline tracking-tight text-on-surface mb-2">我的目标</h1>
        <p class="text-on-surface-variant max-w-lg">通过 AI 驱动的路径规划，将你的职业抱负转化为可执行的里程碑。</p>
      </div>
      <button 
        @click="handleAddGoal"
        class="flex items-center gap-2 bg-gradient-to-br from-primary to-primary-container text-white px-6 py-3 rounded-xl font-bold hover:opacity-90 transition-all shadow-lg shadow-primary/20" 
        type="button"
      >
        <span class="material-symbols-outlined">add</span>
        添加新目标
      </button>
    </header>

    <!-- 加载状态 -->
    <div v-if="loading" class="text-center py-20">
      <span class="material-symbols-outlined text-6xl text-on-surface-variant animate-spin">progress_activity</span>
      <p class="text-on-surface-variant mt-4">加载中...</p>
    </div>

    <!-- 无目标状态 -->
    <div v-else-if="!overview.primaryGoal && overview.parallelGoals.length === 0" class="text-center py-20">
      <span class="material-symbols-outlined text-6xl text-on-surface-variant mb-4">flag</span>
      <p class="text-on-surface-variant">还没有设定任何目标，点击上方按钮开始吧！</p>
    </div>

    <!-- 主内容 -->
    <div v-else class="grid grid-cols-12 gap-8">
      <div class="col-span-12 lg:col-span-8 space-y-8">
        <!-- 主目标卡片 -->
        <section v-if="overview.primaryGoal" class="bg-primary-container rounded-xl p-8 text-white relative overflow-hidden">
          <div class="relative z-10">
            <div class="flex flex-wrap items-center justify-between gap-2 mb-4">
              <div class="flex flex-wrap items-center gap-2">
                <span class="bg-white/20 px-3 py-1 rounded-full text-xs font-bold tracking-wider">首要目标</span>
                <span class="text-primary-fixed-dim text-xs font-medium">预计达成：{{ overview.primaryGoal.eta || '未设定' }}</span>
              </div>
              <button 
                @click="handleCompleteGoal(overview.primaryGoal)"
                class="flex items-center gap-1 bg-white/20 hover:bg-white/30 px-3 py-1 rounded-full text-xs font-bold transition-colors"
              >
                <span class="material-symbols-outlined text-sm">check_circle</span>
                完成目标
              </button>
            </div>
            <h2 class="text-3xl font-bold font-headline mb-6">{{ overview.primaryGoal.title }}</h2>
            <div class="space-y-2">
              <div class="flex justify-between text-sm font-medium">
                <span>总体进度</span>
                <span>{{ overview.primaryGoal.progress }}%</span>
              </div>
              <div class="w-full bg-white/20 h-3 rounded-full overflow-hidden">
                <div class="bg-tertiary-fixed-dim h-full rounded-full" :style="{ width: overview.primaryGoal.progress + '%' }"></div>
              </div>
            </div>
          </div>
          <div class="absolute -right-10 -bottom-10 w-64 h-64 bg-white/5 rounded-full blur-3xl"></div>
          <div class="absolute top-0 right-0 p-6 opacity-20">
            <span class="material-symbols-outlined text-9xl">rocket_launch</span>
          </div>
        </section>

        <!-- 无主目标提示 -->
        <section v-else class="bg-surface-container-low rounded-xl p-8 text-center">
          <p class="text-on-surface-variant">尚未设定主目标，点击上方按钮创建你的第一个目标！</p>
        </section>

        <!-- 里程碑列表 -->
        <section class="bg-surface-container-low rounded-xl p-8">
          <div class="flex justify-between items-center mb-6">
            <h3 class="text-xl font-bold font-headline flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">format_list_bulleted</span>
              短期里程碑
            </h3>
            <span class="text-xs text-on-surface-variant font-medium">{{ overview.milestonesCompleted }} / {{ overview.milestonesTotal }} 已完成</span>
          </div>

          <div v-if="overview.milestones.length === 0" class="text-center py-8 text-on-surface-variant">
            <p>暂无里程碑</p>
          </div>

          <div v-else class="space-y-4">
            <div
              v-for="m in overview.milestones"
              :key="m.id"
              class="flex items-center gap-4 bg-surface-container-lowest p-4 rounded-xl group hover:shadow-md transition-shadow"
              :class="m.status !== 'DONE' ? 'border-2 border-transparent hover:border-primary-fixed transition-all' : ''"
            >
              <div
                class="w-6 h-6 rounded-md border-2 flex items-center justify-center cursor-pointer"
                :class="m.status === 'DONE' ? 'border-primary bg-primary text-white' : 'border-outline-variant'"
                @click="toggleMilestoneStatus(m)"
              >
                <span v-if="m.status === 'DONE'" class="material-symbols-outlined text-sm">check</span>
              </div>
              <div class="flex-grow">
                <p class="font-semibold text-on-surface">{{ m.title }}</p>
                <p class="text-xs text-on-surface-variant">{{ m.desc }}</p>
              </div>
              <span v-if="m.status === 'DONE'" class="text-xs font-bold text-tertiary px-2 py-1 bg-tertiary-fixed rounded">已完成</span>
              <div v-else-if="m.status === 'IN_PROGRESS'" class="flex items-center gap-2">
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
        <!-- 成功准则 -->
        <section class="bg-surface-container-lowest rounded-xl p-6 shadow-sm">
          <h3 class="text-lg font-bold font-headline mb-6 flex items-center gap-2">
            <span class="material-symbols-outlined text-tertiary">verified</span>
            成功准则
          </h3>
          <div class="space-y-6">
            <div>
              <p class="text-xs text-on-surface-variant mb-2 uppercase tracking-widest font-bold">薪资预期</p>
              <p class="text-xl font-bold text-tertiary">{{ overview.successCriteria?.salary || '未设定' }}</p>
            </div>
            <div class="pt-4 border-t border-surface-variant">
              <p class="text-xs text-on-surface-variant mb-2 uppercase tracking-widest font-bold">目标公司</p>
              <div v-if="overview.successCriteria?.companies?.length" class="flex flex-wrap gap-2">
                <span v-for="c in overview.successCriteria.companies" :key="c" class="px-3 py-1 bg-surface-container text-on-secondary-container text-sm rounded-lg">{{ c }}</span>
              </div>
              <p v-else class="text-on-surface-variant text-sm">未设定</p>
            </div>
            <div class="pt-4 border-t border-surface-variant">
              <p class="text-xs text-on-surface-variant mb-2 uppercase tracking-widest font-bold">核心城市</p>
              <p class="font-semibold">{{ citiesDisplay }}</p>
            </div>
          </div>
        </section>

        <!-- 长期愿景 -->
        <section class="bg-surface-variant/70 backdrop-blur-xl rounded-xl p-6">
          <h3 class="text-lg font-bold font-headline mb-4 flex items-center gap-2">
            <span class="material-symbols-outlined text-primary">auto_awesome</span>
            长期愿景
          </h3>
          <div v-if="overview.longTermAspirations?.length" class="space-y-4">
            <div v-for="a in overview.longTermAspirations" :key="a.title" class="p-4 bg-white/40 rounded-xl">
              <p class="text-sm font-bold text-primary mb-1">{{ a.title }}</p>
              <p class="text-xs text-on-surface-variant leading-relaxed">{{ a.desc }}</p>
            </div>
          </div>
          <p v-else class="text-on-surface-variant text-sm">暂无长期愿景</p>
        </section>

        <!-- AI建议 -->
        <div v-if="overview.aiAdvice?.content" class="bg-primary-fixed rounded-xl p-4 flex items-start gap-4">
          <span class="material-symbols-outlined text-primary mt-1" style="font-variation-settings: 'FILL' 1;">lightbulb</span>
          <div>
            <p class="text-xs font-bold text-on-primary-fixed-variant mb-1">AI 建议</p>
            <p class="text-xs text-on-primary-fixed-variant leading-relaxed">{{ overview.aiAdvice.content }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 并行目标 -->
    <section v-if="!loading && (overview.parallelGoals.length > 0 || overview.primaryGoal)" class="pt-2">
      <h3 class="text-2xl font-bold font-headline mb-8">其他并行目标</h3>
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div
          v-for="g in overview.parallelGoals"
          :key="g.id"
          class="bg-surface-container-low rounded-xl p-6 hover:translate-y-[-4px] transition-transform"
        >
          <div class="flex justify-between items-start mb-4">
            <div class="p-2 rounded-lg bg-secondary-container text-primary">
              <span class="material-symbols-outlined">flag</span>
            </div>
            <div class="flex items-center gap-2">
              <button 
                @click="handleCompleteGoal(g)"
                class="text-xs text-primary hover:text-primary/80 font-medium flex items-center gap-1"
              >
                <span class="material-symbols-outlined text-sm">check_circle</span>
                完成
              </button>
              <span class="text-xs font-bold text-on-surface-variant">{{ getStatusLabel(g.status) }}</span>
            </div>
          </div>
          <h4 class="font-bold mb-2">{{ g.title }}</h4>
          <p class="text-xs text-on-surface-variant mb-4">{{ g.desc }}</p>
          <div class="w-full bg-surface-variant h-1.5 rounded-full overflow-hidden">
            <div class="h-full bg-secondary" :style="{ width: g.progress + '%' }"></div>
          </div>
        </div>

        <!-- 添加新目标卡片 -->
        <div
          class="bg-surface-container-low rounded-xl p-6 border-2 border-dashed border-outline-variant flex flex-col items-center justify-center text-on-surface-variant cursor-pointer hover:bg-surface-container transition-colors"
          @click="handleAddGoal"
        >
          <span class="material-symbols-outlined text-4xl mb-2">add_circle</span>
          <span class="text-sm font-bold">设定新领域目标</span>
        </div>
      </div>
    </section>

    <!-- 完成目标确认弹窗 -->
    <div v-if="showCompleteConfirm" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div class="bg-surface-container-lowest rounded-2xl p-8 w-full max-w-md mx-4 shadow-2xl" @click.stop @mousedown.stop @mouseup.stop>
        <div class="text-center">
          <div class="w-16 h-16 mx-auto mb-4 rounded-full bg-tertiary-container flex items-center justify-center">
            <span class="material-symbols-outlined text-3xl text-tertiary">celebration</span>
          </div>
          <h3 class="text-xl font-bold font-headline mb-2">确认完成目标？</h3>
          <p class="text-on-surface-variant mb-6">
            你确定已完成目标「<span class="font-bold text-on-surface">{{ completingGoal?.title }}</span>」吗？<br>
            <span class="text-sm">完成后该目标将被删除。</span>
          </p>
          <div class="flex justify-center gap-3">
            <button 
              @click="closeCompleteConfirm"
              class="px-6 py-2 rounded-lg border border-outline-variant text-on-surface-variant hover:bg-surface-container transition-colors"
            >
              再想想
            </button>
            <button 
              @click="confirmCompleteGoal"
              class="px-6 py-2 rounded-lg bg-tertiary text-white font-medium hover:opacity-90 transition-opacity"
            >
              确认完成
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 完成庆祝弹窗 -->
    <div v-if="showCelebration" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div class="bg-surface-container-lowest rounded-2xl p-8 w-full max-w-md mx-4 shadow-2xl text-center" @click.stop @mousedown.stop @mouseup.stop>
        <div class="relative">
          <div class="absolute inset-0 flex items-center justify-center pointer-events-none">
            <span class="material-symbols-outlined text-8xl text-primary/10">celebration</span>
          </div>
          <span class="material-symbols-outlined text-6xl text-primary mb-4 relative">emoji_events</span>
        </div>
        <h3 class="text-2xl font-bold font-headline mb-2 text-primary">恭喜你！</h3>
        <p class="text-lg mb-2">目标「<span class="font-bold text-tertiary">{{ completedGoalTitle }}</span>」</p>
        <p class="text-on-surface-variant mb-6">已成功完成！你的努力得到了回报，继续加油！</p>
        <button 
          @click="closeCelebration"
          class="px-8 py-3 rounded-xl bg-gradient-to-r from-primary to-tertiary text-white font-bold hover:opacity-90 transition-opacity"
        >
          继续前进
        </button>
      </div>
    </div>

    <!-- 创建目标弹窗 -->
    <div v-if="showCreateModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 overflow-y-auto py-8">
      <div class="bg-surface-container-lowest rounded-2xl p-8 w-full max-w-2xl mx-4 shadow-2xl my-auto" @click.stop @mousedown.stop @mouseup.stop>
        <div class="flex justify-between items-center mb-6">
          <h3 class="text-xl font-bold font-headline">创建新目标</h3>
          <button @click="closeCreateModal" class="text-on-surface-variant hover:text-on-surface">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>

        <form @submit.prevent="submitCreateGoal" class="space-y-6">
          <!-- 基本信息 -->
          <div class="space-y-4">
            <h4 class="text-sm font-bold text-primary uppercase tracking-wider">基本信息</h4>
            <div>
              <label class="block text-sm font-medium mb-1">目标标题 <span class="text-error">*</span></label>
              <input 
                v-model="newGoal.title" 
                type="text" 
                required
                class="w-full px-4 py-2 rounded-lg border border-outline-variant bg-surface-container-low focus:border-primary focus:outline-none"
                placeholder="例如：成为高级前端工程师"
              />
            </div>

            <div>
              <label class="block text-sm font-medium mb-1">目标描述</label>
              <textarea 
                v-model="newGoal.desc"
                rows="3"
                class="w-full px-4 py-2 rounded-lg border border-outline-variant bg-surface-container-low focus:border-primary focus:outline-none resize-none"
                placeholder="详细描述你的目标..."
              ></textarea>
            </div>

            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="block text-sm font-medium mb-1">预计达成时间</label>
                <input 
                  v-model="newGoal.eta"
                  type="text"
                  class="w-full px-4 py-2 rounded-lg border border-outline-variant bg-surface-container-low focus:border-primary focus:outline-none"
                  placeholder="例如：2025年12月"
                />
              </div>
              <div>
                <label class="block text-sm font-medium mb-1">初始进度</label>
                <input 
                  v-model.number="newGoal.progress"
                  type="number"
                  min="0"
                  max="100"
                  class="w-full px-4 py-2 rounded-lg border border-outline-variant bg-surface-container-low focus:border-primary focus:outline-none"
                />
              </div>
            </div>

            <div class="flex items-center gap-3 p-4 bg-primary-container/10 rounded-lg">
              <input 
                type="checkbox" 
                id="isPrimary" 
                v-model="newGoal.isPrimary"
                class="w-5 h-5 rounded border-outline-variant text-primary focus:ring-primary"
              />
              <label for="isPrimary" class="text-sm font-medium">
                设为<span class="text-primary font-bold">主目标</span>（勾选后可设置成功准则、愿景、里程碑）
              </label>
            </div>
          </div>

          <!-- 主目标专属字段 -->
          <template v-if="newGoal.isPrimary">
            <!-- 成功准则 -->
            <div class="space-y-4 pt-4 border-t border-outline-variant">
              <h4 class="text-sm font-bold text-tertiary uppercase tracking-wider">成功准则</h4>
              <div>
                <label class="block text-sm font-medium mb-1">薪资预期</label>
                <input 
                  v-model="newGoal.successCriteria.salary"
                  type="text"
                  class="w-full px-4 py-2 rounded-lg border border-outline-variant bg-surface-container-low focus:border-primary focus:outline-none"
                  placeholder="例如：¥30k - ¥45k / 月"
                />
              </div>
              <div>
                <label class="block text-sm font-medium mb-1">目标公司（用逗号分隔）</label>
                <input 
                  v-model="companiesInput"
                  type="text"
                  class="w-full px-4 py-2 rounded-lg border border-outline-variant bg-surface-container-low focus:border-primary focus:outline-none"
                  placeholder="例如：腾讯, 字节跳动, 阿里巴巴"
                />
              </div>
              <div>
                <label class="block text-sm font-medium mb-1">目标城市（用逗号分隔）</label>
                <input 
                  v-model="citiesInput"
                  type="text"
                  class="w-full px-4 py-2 rounded-lg border border-outline-variant bg-surface-container-low focus:border-primary focus:outline-none"
                  placeholder="例如：北京, 上海, 深圳"
                />
              </div>
            </div>

            <!-- 长期愿景 -->
            <div class="space-y-4 pt-4 border-t border-outline-variant">
              <div class="flex justify-between items-center">
                <h4 class="text-sm font-bold text-primary uppercase tracking-wider">长期愿景</h4>
                <button type="button" @click="addAspiration" class="text-xs text-primary hover:text-primary/80 font-medium">
                  + 添加愿景
                </button>
              </div>
              <div v-for="(a, index) in newGoal.longTermAspirations" :key="index" class="flex gap-2">
                <input 
                  v-model="a.title"
                  type="text"
                  placeholder="愿景标题"
                  class="flex-1 px-3 py-2 rounded-lg border border-outline-variant bg-surface-container-low focus:border-primary focus:outline-none text-sm"
                />
                <input 
                  v-model="a.desc"
                  type="text"
                  placeholder="描述"
                  class="flex-1 px-3 py-2 rounded-lg border border-outline-variant bg-surface-container-low focus:border-primary focus:outline-none text-sm"
                />
                <button type="button" @click="removeAspiration(index)" class="text-error hover:text-error/80">
                  <span class="material-symbols-outlined">delete</span>
                </button>
              </div>
            </div>

            <!-- 短期里程碑 -->
            <div class="space-y-4 pt-4 border-t border-outline-variant">
              <div class="flex justify-between items-center">
                <h4 class="text-sm font-bold text-secondary uppercase tracking-wider">短期里程碑</h4>
                <button type="button" @click="addMilestone" class="text-xs text-secondary hover:text-secondary/80 font-medium">
                  + 添加里程碑
                </button>
              </div>
              <div v-for="(m, index) in newGoal.milestones" :key="index" class="flex gap-2">
                <input 
                  v-model="m.title"
                  type="text"
                  placeholder="里程碑标题"
                  class="flex-1 px-3 py-2 rounded-lg border border-outline-variant bg-surface-container-low focus:border-primary focus:outline-none text-sm"
                />
                <input 
                  v-model="m.desc"
                  type="text"
                  placeholder="描述"
                  class="flex-1 px-3 py-2 rounded-lg border border-outline-variant bg-surface-container-low focus:border-primary focus:outline-none text-sm"
                />
                <button type="button" @click="removeMilestone(index)" class="text-error hover:text-error/80">
                  <span class="material-symbols-outlined">delete</span>
                </button>
              </div>
            </div>
          </template>

          <div class="flex justify-end gap-3 pt-4">
            <button 
              type="button"
              @click="closeCreateModal"
              class="px-6 py-2 rounded-lg border border-outline-variant text-on-surface-variant hover:bg-surface-container transition-colors"
            >
              取消
            </button>
            <button 
              type="submit"
              :disabled="submitting"
              class="px-6 py-2 rounded-lg bg-primary text-white font-medium hover:opacity-90 transition-opacity disabled:opacity-50"
            >
              {{ submitting ? '创建中...' : '创建目标' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import {
  getGoalsOverview,
  createGoal,
  createMilestone,
  updateMilestone,
  updateGoal,
  deleteGoal,
  type GoalsOverview,
  type Milestone,
  type GoalCreateBody,
  type SuccessCriteria,
  type LongTermAspiration,
  type GoalSummary,
  isApiSuccess,
} from '@/api/goals'

// 表单内部类型
interface MilestoneInput {
  title: string
  desc: string
}

interface LongTermAspirationInput {
  title: string
  desc: string
}

interface NewGoalForm {
  title: string
  desc: string
  status: string
  progress: number
  eta: string
  isPrimary: boolean
  successCriteria: SuccessCriteria
  longTermAspirations: LongTermAspirationInput[]
  milestones: MilestoneInput[]
}

const loading = ref(true)
const submitting = ref(false)
const showCreateModal = ref(false)
const showCompleteConfirm = ref(false)
const showCelebration = ref(false)
const completingGoal = ref<GoalSummary | null>(null)
const completedGoalTitle = ref('')
const citiesInput = ref('')
const companiesInput = ref('')
const newGoal = ref<NewGoalForm>({
  title: '',
  desc: '',
  status: 'TODO',
  progress: 0,
  eta: '',
  isPrimary: false,
  successCriteria: { salary: '', companies: [], cities: [] },
  longTermAspirations: [],
  milestones: [],
})
const overview = ref<GoalsOverview>({
  primaryGoal: null,
  milestones: [],
  milestonesCompleted: 0,
  milestonesTotal: 0,
  successCriteria: { salary: '', companies: [], cities: [] },
  longTermAspirations: [],
  aiAdvice: { content: '' },
  parallelGoals: [],
})

const citiesDisplay = computed(() => {
  const cities = overview.value.successCriteria?.cities
  return cities && cities.length > 0 ? cities.join('、') : '未设定'
})

async function loadOverview() {
  loading.value = true
  try {
    const result = await getGoalsOverview()
    if (isApiSuccess(result.code)) {
      overview.value = result.data
    } else {
      console.error('获取目标总览失败:', result.msg)
    }
  } catch (e) {
    console.error('获取目标总览失败:', e)
  } finally {
    loading.value = false
  }
}

function getStatusLabel(status: string): string {
  const labels: Record<string, string> = {
    TODO: '待开始',
    IN_PROGRESS: '进行中',
    DONE: '已完成',
  }
  return labels[status] || status
}

async function toggleMilestoneStatus(m: Milestone) {
  const newStatus = m.status === 'DONE' ? 'TODO' : 'DONE'
  try {
    const result = await updateMilestone(m.goalId, m.id, {
      status: newStatus,
      progress: newStatus === 'DONE' ? 100 : 0,
    })
    if (isApiSuccess(result.code)) {
      await loadOverview()
    }
  } catch (e) {
    console.error('更新里程碑状态失败:', e)
  }
}

function handleAddGoal() {
  showCreateModal.value = true
}

function closeCreateModal() {
  showCreateModal.value = false
  newGoal.value = {
    title: '',
    desc: '',
    status: 'TODO',
    progress: 0,
    eta: '',
    isPrimary: false,
    successCriteria: { salary: '', companies: [], cities: [] },
    longTermAspirations: [],
    milestones: [],
  }
  citiesInput.value = ''
  companiesInput.value = ''
}

function addAspiration() {
  newGoal.value.longTermAspirations.push({ title: '', desc: '' })
}

function removeAspiration(index: number) {
  newGoal.value.longTermAspirations.splice(index, 1)
}

function addMilestone() {
  newGoal.value.milestones.push({ title: '', desc: '' })
}

function removeMilestone(index: number) {
  newGoal.value.milestones.splice(index, 1)
}

async function submitCreateGoal() {
  submitting.value = true
  try {
    // 1. 创建目标
    const goalBody: GoalCreateBody = {
      title: newGoal.value.title,
      desc: newGoal.value.desc,
      status: newGoal.value.status,
      progress: newGoal.value.progress,
      eta: newGoal.value.eta,
      isPrimary: newGoal.value.isPrimary,
    }
    const goalResult = await createGoal(goalBody)
    if (!isApiSuccess(goalResult.code)) {
      alert(goalResult.msg || '创建目标失败')
      return
    }

    const goalId = goalResult.data.id

    // 2. 解析并更新成功准则和长期愿景
    const cities = citiesInput.value.split(/[,，]/).map(s => s.trim()).filter(Boolean)
    const companies = companiesInput.value.split(/[,，]/).map(s => s.trim()).filter(Boolean)
    const validAspirations = newGoal.value.longTermAspirations.filter(a => a.title?.trim())

    if (cities.length > 0 || companies.length > 0 || newGoal.value.successCriteria.salary || validAspirations.length > 0) {
      await updateGoal(goalId, {
        successCriteria: {
          salary: newGoal.value.successCriteria.salary,
          companies,
          cities,
        },
        longTermAspirations: validAspirations,
      })
    }

    // 3. 创建里程碑
    const validMilestones = newGoal.value.milestones.filter(m => m.title?.trim())
    for (let i = 0; i < validMilestones.length; i++) {
      await createMilestone(goalId, {
        title: validMilestones[i].title,
        desc: validMilestones[i].desc,
        status: 'TODO',
        progress: 0,
        order: i + 1,
      })
    }

    closeCreateModal()
    await loadOverview()
  } catch (e) {
    console.error('创建目标失败:', e)
    alert('创建失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

// 点击完成目标
function handleCompleteGoal(goal: GoalSummary) {
  completingGoal.value = goal
  showCompleteConfirm.value = true
}

// 关闭确认弹窗
function closeCompleteConfirm() {
  showCompleteConfirm.value = false
  completingGoal.value = null
}

// 确认完成目标
async function confirmCompleteGoal() {
  if (!completingGoal.value) return

  try {
    const result = await deleteGoal(completingGoal.value.id)
    if (isApiSuccess(result.code)) {
      completedGoalTitle.value = completingGoal.value.title
      showCompleteConfirm.value = false
      showCelebration.value = true
      await loadOverview()
    } else {
      alert(result.msg || '删除目标失败')
    }
  } catch (e) {
    console.error('完成目标失败:', e)
    alert('操作失败，请稍后重试')
  }
}

// 关闭庆祝弹窗
function closeCelebration() {
  showCelebration.value = false
  completedGoalTitle.value = ''
}

onMounted(() => {
  loadOverview()
})
</script>
