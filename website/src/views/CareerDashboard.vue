<template>
  <div class="space-y-10">
    <!-- Header Section -->
    <header class="flex flex-col md:flex-row md:items-center justify-between gap-6">
      <div class="flex items-center gap-6">
        <!-- 用户头像展示 -->
        <div class="flex-shrink-0">
          <img 
            :src="profileOverview?.avatar || '/default-avatar.png'" 
            class="w-20 h-20 rounded-2xl object-cover border-4 border-surface-container-high shadow-md"
            alt="User Avatar"
          />
        </div>
        <div>
          <h1 class="text-4xl font-extrabold headline-font tracking-tight text-on-surface mb-2">
            欢迎回来，{{ profileOverview?.name || '同学' }}。
          </h1>
          <p class="text-on-surface-variant body-md max-w-xl leading-relaxed">
            {{ headlineText }}
          </p>
        </div>
      </div>
      <div class="flex items-center gap-3">
        <button
          class="bg-surface-container-high text-on-secondary-container px-6 py-3 rounded-xl font-semibold hover:bg-surface-container-highest transition-colors disabled:opacity-60"
          :disabled="loadingOverview"
          @click="onReload"
        >
          刷新
        </button>
        <button
          class="bg-primary text-white px-6 py-3 rounded-xl font-semibold hover:opacity-90 transition-colors disabled:opacity-60"
          :disabled="loadingOverview"
          @click="openEdit"
        >
          编辑资料
        </button>
      </div>
    </header>

    <div v-if="pageError" class="bg-red-50 border border-red-200 text-red-700 rounded-xl p-4">
      {{ pageError }}
    </div>

    <div v-if="dashboardError" class="bg-red-50 border border-red-200 text-red-700 rounded-xl p-4">
      {{ dashboardError }}
    </div>

    <div v-if="roadmapError" class="bg-red-50 border border-red-200 text-red-700 rounded-xl p-4">
      {{ roadmapError }}
    </div>

    <!-- Bento Grid Layout -->
    <div class="grid grid-cols-1 md:grid-cols-12 gap-6">
      <!-- AI Match Summary Card (Span 8) -->
      <div class="md:col-span-8 bg-surface-container-lowest rounded-xl p-8 shadow-[0_20px_40px_rgba(25,28,30,0.04)] relative overflow-hidden group">
        <div class="absolute top-0 right-0 w-64 h-64 bg-primary-fixed/20 rounded-full -mr-20 -mt-20 blur-3xl group-hover:bg-primary-fixed/30 transition-colors"></div>
        <div class="flex flex-col md:flex-row gap-10 items-center relative z-10">
          <div class="flex-shrink-0 text-center relative group/score">
            <!-- 动态圆环 SVG -->
            <div class="w-32 h-32 relative flex items-center justify-center">
              <svg class="w-full h-full transform -rotate-90">
                <!-- 背景圆环 -->
                <circle
                  cx="64"
                  cy="64"
                  r="56"
                  stroke="currentColor"
                  stroke-width="8"
                  fill="transparent"
                  class="text-surface-container-high"
                />
                <!-- 进度圆环 -->
                <circle
                  cx="64"
                  cy="64"
                  r="56"
                  stroke="currentColor"
                  stroke-width="8"
                  fill="transparent"
                  stroke-linecap="round"
                  class="text-primary transition-all duration-1000 ease-out"
                  :style="{
                    strokeDasharray: '351.85',
                    strokeDashoffset: 351.85 - (351.85 * (dashboardSummary?.match_summary?.score || 0)) / 100
                  }"
                />
              </svg>
              <!-- 中间文字 -->
              <div class="absolute inset-0 flex items-center justify-center bg-white rounded-full m-2 shadow-inner">
                <span class="text-4xl font-extrabold headline-font text-primary">{{ matchScoreText }}</span>
                <span class="text-sm font-bold text-on-surface-variant self-end mb-4 ml-1">/100</span>
              </div>
            </div>
            <p class="mt-4 text-xs font-bold uppercase tracking-widest text-primary">匹配得分</p>
          </div>
          <div>
            <div class="flex items-center gap-2 mb-3">
              <span class="material-symbols-outlined text-tertiary-fixed-dim bg-tertiary-container p-1 rounded-md text-sm">auto_awesome</span>
              <h2 class="text-xl font-bold headline-font">{{ dashboardSummary?.job_profile?.name || '目标岗位' }}匹配度</h2>
            </div>
            <p class="text-on-surface-variant leading-relaxed text-lg italic">
              “{{ matchSummaryText }}”
            </p>
            <div class="mt-6 flex gap-4">
              <div class="bg-primary-fixed/50 px-3 py-1.5 rounded-lg text-xs font-bold text-primary flex items-center gap-2">
                <span class="material-symbols-outlined text-sm">location_on</span>
                {{ dashboardSummary?.job_profile?.city || profileOverview?.location || '未填写城市' }}
              </div>
              <div class="bg-primary-fixed/50 px-3 py-1.5 rounded-lg text-xs font-bold text-primary flex items-center gap-2">
                <span class="material-symbols-outlined text-sm">badge</span>
                {{ profileOverview?.current_role || '未填写当前岗位' }}
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
          <div v-if="(marketTrends?.length ?? 0) === 0" class="text-sm text-on-surface-variant">暂无市场趋势数据</div>
          <div v-for="(t, i) in marketTrends" :key="`${t.name || 'trend'}-${i}`" class="p-4 bg-surface-container-lowest rounded-lg">
            <div class="flex justify-between items-center mb-1">
              <span class="text-sm font-medium">{{ t.name || '—' }}</span>
              <span class="text-xs font-bold" :class="(t.growth ?? 0) >= 0 ? 'text-green-600' : 'text-red-600'">
                {{ formatGrowth(t.growth) }}
              </span>
            </div>
            <div class="h-1.5 w-full bg-surface-container-highest rounded-full overflow-hidden">
              <div class="h-full bg-primary-container" :style="{ width: `${Math.max(0, Math.min(100, t.value ?? 0))}%` }"></div>
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
          <div v-if="(actions?.length ?? 0) === 0" class="text-sm text-on-surface-variant">暂无行动建议</div>
          <div v-for="(item, index) in actions" :key="item.id || index" class="group flex items-center justify-between p-4 bg-surface rounded-xl hover:bg-primary-fixed/30 transition-all cursor-pointer" @click="onActionClick(item)">
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
            <div v-if="(roadmap?.length ?? 0) === 0" class="text-sm text-on-surface-variant">暂无职业路径数据</div>
            <div v-for="(node, index) in roadmap" :key="`${node.title || 'step'}-${index}`" class="flex flex-col items-center text-center max-w-[200px] cursor-pointer" @click="onSelectRoadmapStep(index)">
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

      <div class="md:col-span-12 bg-surface-container-lowest rounded-xl p-8 shadow-[0_20px_40px_rgba(25,28,30,0.04)]">
        <div class="flex items-center justify-between mb-6">
          <h3 class="text-lg font-bold headline-font">详细档案</h3>
          <span v-if="loadingDetail" class="text-xs text-on-surface-variant">加载中...</span>
        </div>

        <div v-if="detailError" class="bg-red-50 border border-red-200 text-red-700 rounded-xl p-4 mb-6">
          {{ detailError }}
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <section class="bg-surface rounded-xl p-6">
            <h4 class="font-bold mb-4">技能</h4>
            <div v-if="(profileDetail?.skills?.length ?? 0) === 0" class="text-sm text-on-surface-variant">暂无技能信息</div>
            <div v-else class="flex flex-wrap gap-2">
              <span
                v-for="(s, i) in profileDetail?.skills"
                :key="`${s}-${i}`"
                class="bg-primary-fixed/40 text-primary px-3 py-1 rounded-full text-xs font-bold"
              >
                {{ s }}
              </span>
            </div>
          </section>

          <section class="bg-surface rounded-xl p-6">
            <h4 class="font-bold mb-4">教育经历</h4>
            <div v-if="(profileDetail?.education?.length ?? 0) === 0" class="text-sm text-on-surface-variant">暂无教育经历</div>
            <div v-else class="space-y-4">
              <div v-for="(e, i) in profileDetail?.education" :key="i" class="p-4 bg-surface-container-low rounded-lg">
                <div class="flex items-center justify-between gap-4">
                  <div class="font-semibold truncate">{{ e.school || '未填写学校' }}</div>
                  <div class="text-xs text-on-surface-variant">{{ e.period || '' }}</div>
                </div>
                <div class="text-sm text-on-surface-variant mt-1">
                  {{ [e.degree, e.major].filter(Boolean).join(' · ') }}
                </div>
              </div>
            </div>
          </section>

          <section class="bg-surface rounded-xl p-6 md:col-span-2">
            <h4 class="font-bold mb-4">工作经历</h4>
            <div v-if="(profileDetail?.experience?.length ?? 0) === 0" class="text-sm text-on-surface-variant">暂无工作经历</div>
            <div v-else class="space-y-4">
              <div v-for="(x, i) in profileDetail?.experience" :key="i" class="p-4 bg-surface-container-low rounded-lg">
                <div class="flex items-center justify-between gap-4">
                  <div class="font-semibold truncate">{{ x.company || '未填写公司' }}</div>
                  <div class="text-xs text-on-surface-variant">{{ x.period || '' }}</div>
                </div>
                <div class="text-sm text-on-surface-variant mt-1">{{ x.position || '' }}</div>
                <div v-if="x.description" class="text-sm text-on-surface-variant mt-2 leading-relaxed">{{ x.description }}</div>
              </div>
            </div>
          </section>

          <section class="bg-surface rounded-xl p-6 md:col-span-2">
            <h4 class="font-bold mb-4">项目经历</h4>
            <div v-if="(profileDetail?.projects?.length ?? 0) === 0" class="text-sm text-on-surface-variant">暂无项目经历</div>
            <div v-else class="space-y-4">
              <div v-for="(p, i) in profileDetail?.projects" :key="i" class="p-4 bg-surface-container-low rounded-lg">
                <div class="flex items-center justify-between gap-4">
                  <div class="font-semibold truncate">{{ p.name || '未填写项目名' }}</div>
                  <a
                    v-if="p.link"
                    class="text-xs font-bold text-primary hover:underline"
                    :href="p.link"
                    target="_blank"
                    rel="noreferrer"
                  >
                    链接
                  </a>
                </div>
                <div v-if="(p.tech_stack?.length ?? 0) > 0" class="flex flex-wrap gap-2 mt-3">
                  <span
                    v-for="(t, j) in p.tech_stack"
                    :key="`${t}-${j}`"
                    class="bg-tertiary-fixed/30 text-on-surface px-3 py-1 rounded-full text-xs font-bold"
                  >
                    {{ t }}
                  </span>
                </div>
              </div>
            </div>
          </section>
        </div>
      </div>
    </div>

    <div v-if="editOpen" class="fixed inset-0 z-[60] flex items-center justify-center p-6">
      <div class="absolute inset-0 bg-black/40" @click="closeEdit"></div>
      <div class="relative w-full max-w-xl bg-white dark:bg-slate-950 rounded-2xl shadow-2xl p-6">
        <div class="flex items-center justify-between mb-6">
          <h3 class="text-lg font-bold">编辑资料</h3>
          <button class="p-2 rounded-lg hover:bg-surface-container-low" @click="closeEdit">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>

        <div class="space-y-5">
          <!-- 头像拖拽上传 -->
          <div>
            <label class="block text-sm font-semibold mb-2">个人头像</label>
            <div 
              class="relative group cursor-pointer border-2 border-dashed border-outline-variant rounded-2xl p-4 transition-all hover:border-primary hover:bg-primary/5"
              @click="triggerFileUpload"
              @dragover.prevent="isDragging = true"
              @dragleave.prevent="isDragging = false"
              @drop.prevent="handleDrop"
              :class="{ 'border-primary bg-primary/10': isDragging }"
            >
              <div class="flex flex-col items-center gap-2">
                <div class="relative">
                  <img 
                    :src="editAvatar || '/default-avatar.png'" 
                    class="w-20 h-20 rounded-full object-cover border-2 border-surface-container-high shadow-sm"
                    alt="Avatar Preview"
                  />
                  <div v-if="uploading" class="absolute inset-0 bg-black/40 rounded-full flex items-center justify-center">
                    <div class="w-6 h-6 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                  </div>
                </div>
                <div class="text-center">
                  <p class="text-sm font-medium text-on-surface">点击或拖拽图片上传</p>
                  <p class="text-[10px] text-on-surface-variant">支持 JPG, PNG，最大 2MB</p>
                </div>
              </div>
              <input 
                type="file" 
                ref="fileInput" 
                class="hidden" 
                accept="image/*" 
                @change="handleFileChange"
              />
            </div>
          </div>

          <div>
            <label class="block text-sm font-semibold mb-2">昵称</label>
            <input
              v-model="editName"
              type="text"
              class="w-full rounded-xl border border-surface-container-highest bg-surface px-4 py-3 outline-none focus:ring-2 focus:ring-primary"
              placeholder="请输入昵称"
            />
          </div>

          <div>
            <label class="block text-sm font-semibold mb-2">性别</label>
            <div class="flex gap-6 p-1">
              <label class="flex items-center gap-2 cursor-pointer group">
                <input type="radio" v-model="editSex" :value="1" class="w-4 h-4 text-primary focus:ring-primary" />
                <span class="text-sm font-medium group-hover:text-primary transition-colors">男</span>
              </label>
              <label class="flex items-center gap-2 cursor-pointer group">
                <input type="radio" v-model="editSex" :value="0" class="w-4 h-4 text-primary focus:ring-primary" />
                <span class="text-sm font-medium group-hover:text-primary transition-colors">女</span>
              </label>
            </div>
          </div>

          <div v-if="saveError" class="bg-red-50 border border-red-200 text-red-700 rounded-xl p-4">
            {{ saveError }}
          </div>
          <div v-if="saveOk" class="bg-green-50 border border-green-200 text-green-700 rounded-xl p-4">
            保存成功
          </div>

          <div class="flex items-center justify-end gap-3 pt-2">
            <button class="px-5 py-2.5 rounded-xl font-semibold hover:bg-surface-container-low" @click="closeEdit">取消</button>
            <button
              class="px-5 py-2.5 rounded-xl font-semibold bg-primary text-white hover:opacity-90 disabled:opacity-60"
              :disabled="saving"
              @click="onSave"
            >
              {{ saving ? '保存中...' : '保存' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { isApiSuccess } from '@/api/client'
import * as profileApi from '@/api/userProfile'
import { updateUserInfo, uploadFile, type UserEditBody } from '@/api/auth'
import * as dashboardApi from '@/api/dashboard'

const loadingOverview = ref(false)
const loadingDetail = ref(false)
const loadingDashboard = ref(false)
const loadingRoadmap = ref(false)
const pageError = ref<string | null>(null)
const detailError = ref<string | null>(null)
const dashboardError = ref<string | null>(null)
const roadmapError = ref<string | null>(null)

const router = useRouter()

const profileOverview = ref<profileApi.UserProfileOverview | null>(null)
const profileDetail = ref<profileApi.UserProfileDetail | null>(null)
const dashboardSummary = ref<dashboardApi.DashboardSummary | null>(null)
const dashboardRoadmap = ref<dashboardApi.DashboardRoadmap | null>(null)

const editOpen = ref(false)
const editName = ref('')
const editAvatar = ref('')
const editSex = ref(1)
const saving = ref(false)
const uploading = ref(false)
const isDragging = ref(false)
const saveError = ref<string | null>(null)
const saveOk = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)

const matchScoreText = computed(() => {
  const s = dashboardSummary.value?.match_summary?.score
  if (typeof s === 'number' && Number.isFinite(s)) return String(Math.round(s))
  return '--'
})

const headlineText = computed(() => {
  const target = dashboardSummary.value?.job_profile?.name
  const loc = dashboardSummary.value?.job_profile?.city || profileOverview.value?.location
  const current = profileOverview.value?.current_role
  const bits = [
    target ? `您的目标岗位是「${target}」。` : '请先上传简历获取 AI 分析结果。',
    current ? `当前岗位：${current}。` : null,
    loc ? `所在城市：${loc}。` : null,
  ].filter(Boolean)
  return bits.join('')
})

const matchSummaryText = computed(() => {
  const score = dashboardSummary.value?.match_summary?.score
  const target = dashboardSummary.value?.job_profile?.name
  if (typeof score !== 'number' || !Number.isFinite(score)) return '请先上传简历，生成匹配度分析。'
  if (!target) return `当前匹配分为 ${Math.round(score)}。`
  if (score >= 80) return `您与${target}职位的匹配度很高，建议继续完善项目与技能深度。`
  if (score >= 60) return `您与${target}职位具备一定匹配度，建议补齐关键技能与项目经验。`
  return `您与${target}职位匹配度偏低，建议先从核心技能和基础项目开始提升。`
})

const actions = computed(() => dashboardSummary.value?.actions ?? [])
const marketTrends = computed(() => dashboardSummary.value?.market_trends ?? [])
const roadmap = computed(() => dashboardRoadmap.value?.steps ?? [])

function formatGrowth(growth: number | undefined) {
  const n = typeof growth === 'number' && Number.isFinite(growth) ? growth : 0
  const sign = n >= 0 ? '+' : ''
  return `${sign}${Math.round(n * 100)}%`
}

async function loadOverview() {
  loadingOverview.value = true
  pageError.value = null
  try {
    const r = await profileApi.getUserProfile()
    if (!isApiSuccess(r.code)) {
      pageError.value = r.msg || '获取档案概览失败'
      profileOverview.value = null
      return
    }
    profileOverview.value = r.data ?? null
  } catch (e) {
    pageError.value = e instanceof Error ? e.message : '获取档案概览失败'
    profileOverview.value = null
  } finally {
    loadingOverview.value = false
  }
}

async function loadDashboardSummary() {
  loadingDashboard.value = true
  dashboardError.value = null
  try {
    const r = await dashboardApi.getDashboardSummary()
    if (r.code === 404) {
      dashboardSummary.value = null
      await router.replace({ name: 'resume', query: { from: 'dashboard' } })
      return
    }
    if (!isApiSuccess(r.code)) {
      dashboardError.value = r.msg || '获取仪表盘汇总失败'
      dashboardSummary.value = null
      return
    }
    dashboardSummary.value = r.data ?? null
  } catch (e) {
    dashboardError.value = e instanceof Error ? e.message : '获取仪表盘汇总失败'
    dashboardSummary.value = null
  } finally {
    loadingDashboard.value = false
  }
}

async function loadDashboardRoadmap() {
  loadingRoadmap.value = true
  roadmapError.value = null
  try {
    const r = await dashboardApi.getDashboardRoadmap()
    if (r.code === 404) {
      dashboardRoadmap.value = null
      await router.replace({ name: 'resume', query: { from: 'dashboard' } })
      return
    }
    if (!isApiSuccess(r.code)) {
      roadmapError.value = r.msg || '获取职业进化地图失败'
      dashboardRoadmap.value = null
      return
    }
    dashboardRoadmap.value = r.data ?? null
  } catch (e) {
    roadmapError.value = e instanceof Error ? e.message : '获取职业进化地图失败'
    dashboardRoadmap.value = null
  } finally {
    loadingRoadmap.value = false
  }
}

async function loadDetail() {
  loadingDetail.value = true
  detailError.value = null
  try {
    const r = await profileApi.getUserProfileDetail()
    if (!isApiSuccess(r.code)) {
      detailError.value = r.msg || '获取详细档案失败'
      profileDetail.value = null
      return
    }
    profileDetail.value = r.data ?? null
  } catch (e) {
    detailError.value = e instanceof Error ? e.message : '获取详细档案失败'
    profileDetail.value = null
  } finally {
    loadingDetail.value = false
  }
}

async function onReload() {
  await Promise.all([loadOverview(), loadDetail(), loadDashboardSummary(), loadDashboardRoadmap()])
}

async function onSelectRoadmapStep(index: number) {
  if (loadingRoadmap.value) return
  const steps = dashboardRoadmap.value?.steps
  if (!steps || index < 0 || index >= steps.length) return

  loadingRoadmap.value = true
  roadmapError.value = null
  try {
    const r = await dashboardApi.updateDashboardCurrentStep({ current_step_index: index })
    if (!isApiSuccess(r.code)) {
      roadmapError.value = r.msg || '更新当前阶段失败'
      return
    }
    const updatedSteps = r.data ?? []
    dashboardRoadmap.value = {
      ...(dashboardRoadmap.value || {}),
      current_step_index: index,
      steps: updatedSteps,
    }
  } catch (e) {
    roadmapError.value = e instanceof Error ? e.message : '更新当前阶段失败'
  } finally {
    loadingRoadmap.value = false
  }
}

function onActionClick(item: dashboardApi.DashboardAction) {
  const link = (item?.link || '').trim()
  if (!link) return
  void router.push(link)
}

function openEdit() {
  saveError.value = null
  saveOk.value = false
  editName.value = profileOverview.value?.name || ''
  editAvatar.value = profileOverview.value?.avatar || ''
  // 假设 profileOverview 暂时没有直接透出 sex，但可以从后端 Auth 相关信息获取，或默认为 1
  // 如果 profileOverview 已经包含 sex 字段，直接回显
  editSex.value = (profileOverview.value as any)?.sex ?? 1
  editOpen.value = true
}

function triggerFileUpload() {
  fileInput.value?.click()
}

async function processFile(file: File) {
  if (!file.type.startsWith('image/')) {
    saveError.value = '请上传图片文件'
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    saveError.value = '图片大小不能超过 2MB'
    return
  }

  uploading.value = true
  saveError.value = null
  try {
    const res = await uploadFile(file)
    if (isApiSuccess(res.code) && res.data) {
      editAvatar.value = res.data
    } else {
      saveError.value = res.msg || '上传失败'
    }
  } catch (error) {
    saveError.value = '上传过程中出现错误'
  } finally {
    uploading.value = false
  }
}

function handleFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (file) processFile(file)
}

function handleDrop(event: DragEvent) {
  isDragging.value = false
  const file = event.dataTransfer?.files[0]
  if (file) processFile(file)
}

function closeEdit() {
  editOpen.value = false
}

async function onSave() {
  if (saving.value || uploading.value) return
  saving.value = true
  saveError.value = null
  saveOk.value = false
  try {
    const body: UserEditBody = {
      name: editName.value.trim(),
      userImage: editAvatar.value,
      sex: editSex.value
    }
    const r = await updateUserInfo(body)
    if (!isApiSuccess(r.code)) {
      saveError.value = r.msg || '保存失败'
      return
    }
    saveOk.value = true
    await loadOverview()
    // 延迟关闭窗口
    setTimeout(() => {
      if (saveOk.value) closeEdit()
    }, 1000)
  } catch (e) {
    saveError.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  void onReload()
})
</script>
