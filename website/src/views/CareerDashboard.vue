<template>
  <div class="space-y-10">
    <!-- Header Section -->
    <header class="flex flex-col md:flex-row md:items-end justify-between gap-6">
      <div>
        <h1 class="text-4xl font-extrabold headline-font tracking-tight text-on-surface mb-2">
          欢迎回来，{{ profileOverview?.name || '同学' }}。
        </h1>
        <p class="text-on-surface-variant body-md max-w-xl leading-relaxed">
          {{ headlineText }}
        </p>
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

    <!-- Bento Grid Layout -->
    <div class="grid grid-cols-1 md:grid-cols-12 gap-6">
      <!-- AI Match Summary Card (Span 8) -->
      <div class="md:col-span-8 bg-surface-container-lowest rounded-xl p-8 shadow-[0_20px_40px_rgba(25,28,30,0.04)] relative overflow-hidden group">
        <div class="absolute top-0 right-0 w-64 h-64 bg-primary-fixed/20 rounded-full -mr-20 -mt-20 blur-3xl group-hover:bg-primary-fixed/30 transition-colors"></div>
        <div class="flex flex-col md:flex-row gap-10 items-center relative z-10">
          <div class="flex-shrink-0 text-center">
            <div class="w-32 h-32 rounded-full border-8 border-primary-fixed flex items-center justify-center bg-white shadow-inner">
              <span class="text-4xl font-extrabold headline-font text-primary">{{ matchScoreText }}</span>
              <span class="text-sm font-bold text-on-surface-variant self-end mb-4 ml-1">/100</span>
            </div>
            <p class="mt-4 text-xs font-bold uppercase tracking-widest text-primary">匹配得分</p>
          </div>
          <div>
            <div class="flex items-center gap-2 mb-3">
              <span class="material-symbols-outlined text-tertiary-fixed-dim bg-tertiary-container p-1 rounded-md text-sm">auto_awesome</span>
              <h2 class="text-xl font-bold headline-font">{{ profileOverview?.target_role || '目标岗位' }}匹配度</h2>
            </div>
            <p class="text-on-surface-variant leading-relaxed text-lg italic">
              “{{ matchSummaryText }}”
            </p>
            <div class="mt-6 flex gap-4">
              <div class="bg-primary-fixed/50 px-3 py-1.5 rounded-lg text-xs font-bold text-primary flex items-center gap-2">
                <span class="material-symbols-outlined text-sm">location_on</span>
                {{ profileOverview?.location || '未填写城市' }}
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
            <label class="block text-sm font-semibold mb-2">头像 URL</label>
            <input
              v-model="editAvatar"
              type="text"
              class="w-full rounded-xl border border-surface-container-highest bg-surface px-4 py-3 outline-none focus:ring-2 focus:ring-primary"
              placeholder="https://..."
            />
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
import { isApiSuccess } from '@/api/client'
import * as profileApi from '@/api/userProfile'

const loadingOverview = ref(false)
const loadingDetail = ref(false)
const pageError = ref<string | null>(null)
const detailError = ref<string | null>(null)

const profileOverview = ref<profileApi.UserProfileOverview | null>(null)
const profileDetail = ref<profileApi.UserProfileDetail | null>(null)

const editOpen = ref(false)
const editName = ref('')
const editAvatar = ref('')
const saving = ref(false)
const saveError = ref<string | null>(null)
const saveOk = ref(false)

const matchScoreText = computed(() => {
  const s = profileOverview.value?.match_score
  if (typeof s === 'number' && Number.isFinite(s)) return String(Math.round(s))
  return '--'
})

const headlineText = computed(() => {
  const target = profileOverview.value?.target_role
  const loc = profileOverview.value?.location
  const current = profileOverview.value?.current_role
  const bits = [
    target ? `您的目标岗位是「${target}」。` : '请先上传简历获取 AI 分析结果。',
    current ? `当前岗位：${current}。` : null,
    loc ? `所在城市：${loc}。` : null,
  ].filter(Boolean)
  return bits.join('')
})

const matchSummaryText = computed(() => {
  const score = profileOverview.value?.match_score
  const target = profileOverview.value?.target_role
  if (typeof score !== 'number' || !Number.isFinite(score)) return '请先上传简历，生成匹配度分析。'
  if (!target) return `当前匹配分为 ${Math.round(score)}。`
  if (score >= 80) return `您与${target}职位的匹配度很高，建议继续完善项目与技能深度。`
  if (score >= 60) return `您与${target}职位具备一定匹配度，建议补齐关键技能与项目经验。`
  return `您与${target}职位匹配度偏低，建议先从核心技能和基础项目开始提升。`
})

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
  await Promise.all([loadOverview(), loadDetail()])
}

function openEdit() {
  saveError.value = null
  saveOk.value = false
  editName.value = profileOverview.value?.name || ''
  editAvatar.value = profileOverview.value?.avatar || ''
  editOpen.value = true
}

function closeEdit() {
  editOpen.value = false
}

async function onSave() {
  if (saving.value) return
  saving.value = true
  saveError.value = null
  saveOk.value = false
  try {
    const body: profileApi.UpdateUserProfileBody = {
      ...(editName.value.trim() !== '' ? { name: editName.value.trim() } : {}),
      ...(editAvatar.value.trim() !== '' ? { avatar: editAvatar.value.trim() } : {}),
    }
    if (Object.keys(body).length === 0) {
      saveError.value = '请至少修改一项再保存'
      return
    }
    const r = await profileApi.updateUserProfile(body)
    if (!isApiSuccess(r.code)) {
      saveError.value = r.msg || '保存失败'
      return
    }
    saveOk.value = true
    await loadOverview()
  } catch (e) {
    saveError.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  void onReload()
})

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
