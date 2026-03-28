<template>
  <div class="flex gap-6">
    <aside class="hidden lg:flex w-72 bg-surface-container-low rounded-xl overflow-hidden flex-col">
      <div class="p-6">
        <h2 class="font-headline font-bold text-lg text-on-surface mb-4">对话历史</h2>
        <button
          class="w-full flex items-center justify-center gap-2 py-3 bg-primary-container text-white rounded-xl mb-6 hover:opacity-90 transition-all font-medium"
          type="button"
          @click="startNewConversation"
        >
          <span class="material-symbols-outlined text-sm">add</span>
          <span>开启新对话</span>
        </button>
      </div>

      <div class="flex-1 overflow-y-auto px-3 space-y-1">
        <button
          v-for="c in conversations"
          :key="c.id"
          type="button"
          class="w-full text-left p-4 rounded-xl flex items-center gap-3 cursor-pointer transition-colors"
          :class="c.id === activeConversationId ? 'bg-primary-fixed text-primary' : 'text-on-surface-variant hover:bg-surface-container-high'"
          @click="selectConversation(c.id)"
        >
          <span class="material-symbols-outlined" :class="c.id === activeConversationId ? 'text-primary' : ''">{{ c.icon }}</span>
          <div class="flex-1 min-w-0">
            <p class="text-sm font-semibold truncate">{{ c.title }}</p>
            <p class="text-xs opacity-70">{{ c.timeLabel }}</p>
          </div>
        </button>
      </div>

      <div class="p-4 bg-surface-container-high/50 m-4 rounded-xl">
        <p class="text-xs text-on-surface-variant mb-2">当前导师</p>
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-full bg-primary-fixed flex items-center justify-center">
            <span class="material-symbols-outlined text-primary">smart_toy</span>
          </div>
          <div>
            <p class="text-sm font-bold">AI 职业导师</p>
            <p class="text-[10px] text-tertiary font-medium bg-tertiary-fixed px-1.5 rounded uppercase">智尊版</p>
          </div>
        </div>
      </div>
    </aside>

    <section class="flex-1 flex flex-col bg-surface-container-lowest rounded-xl overflow-hidden min-h-[650px] relative">
      <div class="px-6 py-4 bg-white/50 backdrop-blur-md flex justify-between items-center">
        <div class="flex items-center gap-4">
          <div class="relative">
            <div class="w-3 h-3 bg-green-500 rounded-full absolute bottom-0 right-0 border-2 border-white"></div>
            <span class="material-symbols-outlined text-4xl text-primary">smart_toy</span>
          </div>
          <div>
            <h1 class="font-headline font-extrabold text-xl text-on-surface">与AI导师对话</h1>
            <p class="text-xs text-on-surface-variant">为您提供个性化的职业晋升方案</p>
          </div>
        </div>
        <div class="flex gap-2">
          <button class="p-2 rounded-full hover:bg-surface-container-high text-on-surface-variant material-symbols-outlined" type="button">search</button>
          <button class="p-2 rounded-full hover:bg-surface-container-high text-on-surface-variant material-symbols-outlined" type="button">more_vert</button>
        </div>
      </div>

      <div ref="messagesEl" class="flex-1 overflow-y-auto p-6 space-y-6 pb-40">
        <div
          v-for="m in activeMessages"
          :key="m.id"
          class="flex items-start gap-4 max-w-3xl"
          :class="m.role === 'user' ? 'ml-auto flex-row-reverse' : ''"
        >
          <div
            class="w-10 h-10 rounded-xl flex items-center justify-center text-white flex-shrink-0"
            :class="m.role === 'user' ? 'bg-secondary' : 'bg-primary'"
          >
            <span class="material-symbols-outlined text-2xl">{{ m.role === 'user' ? 'person' : 'bolt' }}</span>
          </div>

          <div
            class="p-5 rounded-2xl shadow-sm"
            :class="m.role === 'user' ? 'bg-primary text-white rounded-tr-none shadow-md' : 'bg-surface-container-lowest text-on-surface rounded-tl-none border border-outline-variant/10'"
          >
            <p class="leading-relaxed whitespace-pre-wrap">{{ m.content }}</p>

            <div v-if="m.role === 'assistant' && m.suggestions?.length" class="grid grid-cols-2 gap-3 mt-4">
              <button
                v-for="s in m.suggestions"
                :key="s.title"
                type="button"
                class="text-left p-3 rounded-lg bg-surface-container-low hover:bg-primary-fixed transition-colors text-xs font-medium border border-transparent hover:border-primary/20"
                @click="applySuggestion(s.text)"
              >
                <span class="block text-primary mb-1">{{ s.title }}</span>
                {{ s.text }}
              </button>
            </div>

            <div v-if="m.role === 'assistant' && m.tip" class="mt-4 flex items-center gap-2 p-3 bg-tertiary-fixed/30 rounded-lg border-l-4 border-tertiary">
              <span class="material-symbols-outlined text-tertiary text-sm">lightbulb</span>
              <p class="text-xs text-tertiary font-medium">{{ m.tip }}</p>
            </div>
          </div>
        </div>
      </div>

      <div class="absolute bottom-0 left-0 w-full p-6 bg-gradient-to-t from-background via-background/95 to-transparent">
        <div class="flex gap-2 mb-4 overflow-x-auto max-w-4xl mx-auto">
          <button
            v-for="q in quickQuestions"
            :key="q"
            type="button"
            class="whitespace-nowrap px-4 py-2 rounded-full bg-surface-container-highest/50 border border-outline-variant/20 text-xs font-medium hover:bg-white transition-all"
            @click="applySuggestion(q)"
          >
            {{ q }}
          </button>
        </div>

        <form class="max-w-4xl mx-auto bg-surface-container-lowest rounded-2xl shadow-xl p-2 flex items-center gap-2 border border-outline-variant/10" @submit.prevent="send">
          <button class="p-3 text-on-surface-variant hover:bg-surface-container-low rounded-xl transition-colors material-symbols-outlined" type="button">attach_file</button>
          <button class="p-3 text-on-surface-variant hover:bg-surface-container-low rounded-xl transition-colors material-symbols-outlined" type="button">mic</button>
          <input
            v-model="draft"
            class="flex-1 bg-transparent border-none focus:ring-0 text-sm font-medium px-2 py-3"
            placeholder="输入你的问题，或粘贴你的简历内容..."
            type="text"
          />
          <button class="bg-primary-container text-white px-6 py-3 rounded-xl flex items-center gap-2 font-bold transition-transform active:scale-95" type="submit">
            <span>发送</span>
            <span class="material-symbols-outlined text-sm">send</span>
          </button>
        </form>

        <p class="text-center text-[10px] text-on-surface-variant mt-3 opacity-60">AI可能会产生误差，请结合实际情况参考建议</p>
      </div>
    </section>

    <aside class="hidden xl:flex w-80 bg-surface-container-low rounded-xl p-6 flex-col gap-6">
      <div class="bg-white p-6 rounded-2xl shadow-sm">
        <h3 class="font-headline font-bold text-sm mb-4">你的今日建议</h3>
        <div class="space-y-4">
          <div class="flex items-start gap-3">
            <div class="w-8 h-8 rounded-lg bg-tertiary-fixed flex items-center justify-center text-tertiary">
              <span class="material-symbols-outlined text-lg">trending_up</span>
            </div>
            <div>
              <p class="text-xs font-bold">技能缺口发现</p>
              <p class="text-[11px] text-on-surface-variant">你当前简历缺少“SQL进阶”关键词，建议补充相关案例。</p>
            </div>
          </div>
          <div class="flex items-start gap-3">
            <div class="w-8 h-8 rounded-lg bg-primary-fixed flex items-center justify-center text-primary">
              <span class="material-symbols-outlined text-lg">verified</span>
            </div>
            <div class="flex-1">
              <p class="text-xs font-bold">面试准备度</p>
              <div class="w-full bg-surface-container-high h-1.5 rounded-full mt-1.5">
                <div class="bg-primary h-full w-[65%] rounded-full"></div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="flex-1 relative rounded-2xl overflow-hidden">
        <div class="absolute inset-0 bg-gradient-to-t from-primary/80 to-transparent"></div>
        <div class="absolute inset-0 bg-primary/10"></div>
        <div class="absolute bottom-0 left-0 right-0 p-4">
          <p class="text-white text-xs font-bold">职引AI 会员计划</p>
          <p class="text-white/80 text-[10px]">解锁无限次AI面试模拟与简历精修</p>
          <button class="mt-2 py-1.5 bg-white text-primary rounded-lg text-[10px] font-bold w-full" type="button">立即了解</button>
        </div>
      </div>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'

type Conversation = {
  id: string
  title: string
  timeLabel: string
  icon: string
}

type Suggestion = {
  title: string
  text: string
}

type ChatMessage = {
  id: string
  role: 'assistant' | 'user'
  content: string
  suggestions?: Suggestion[]
  tip?: string
}

const conversations = ref<Conversation[]>([
  { id: 'c1', title: '简历深度优化 - 互联网', timeLabel: '2分钟前', icon: 'chat_bubble' },
  { id: 'c2', title: '模拟面试：字节跳动 PM', timeLabel: '1小时前', icon: 'psychology' },
  { id: 'c3', title: '职业规划咨询：转行AI', timeLabel: '昨天', icon: 'explore' },
  { id: 'c4', title: '薪资谈判策略建议', timeLabel: '3天前', icon: 'history' }
])

const activeConversationId = ref(conversations.value[0]?.id ?? 'c1')

const messagesByConversation = ref<Record<string, ChatMessage[]>>({
  c1: [
    {
      id: 'm1',
      role: 'assistant',
      content:
        '你好！我是你的专属AI职业导师。我已经分析了你的背景：拥有3年互联网产品经理经验。今天你想从哪方面开始提升？',
      suggestions: [
        { title: '简历门诊', text: '帮我针对“高级产品经理”岗位润色简历' },
        { title: '面试通关', text: '开启一场关于“产品架构”的模拟面试' }
      ]
    },
    { id: 'm2', role: 'user', content: '我想润色一下我简历中关于“数据驱动增长”的那段描述，总觉得不够专业。' },
    {
      id: 'm3',
      role: 'assistant',
      content:
        '明白。优秀的“数据驱动”描述应该包含：原始痛点、具体分析手段、最终量化结果。请把那段文字发送给我，或者上传你的简历附件。',
      tip: '提示：使用“STAR法则”能让你的描述更有说服力。'
    }
  ],
  c2: [{ id: 'm1', role: 'assistant', content: '已为你准备好模拟面试环境。先从自我介绍开始吧。' }],
  c3: [{ id: 'm1', role: 'assistant', content: '我们可以先从你的现有技能盘点开始，确定转行AI的最短路径。' }],
  c4: [{ id: 'm1', role: 'assistant', content: '薪资谈判我建议用“价值锚点 + 市场数据 + 备选方案”三步走。' }]
})

const activeMessages = computed(() => messagesByConversation.value[activeConversationId.value] ?? [])

const quickQuestions = ref<string[]>(['帮我润色这段简历', '模拟大厂面试', '如何谈更高薪水？', '推荐适合我的岗位'])
const draft = ref('')
const messagesEl = ref<HTMLElement | null>(null)

function selectConversation(id: string) {
  activeConversationId.value = id
  void nextTick(() => scrollToBottom())
}

function startNewConversation() {
  const id = `c${Date.now()}`
  conversations.value = [
    { id, title: '新对话', timeLabel: '刚刚', icon: 'chat_bubble' },
    ...conversations.value
  ]
  messagesByConversation.value[id] = [
    { id: `m${Date.now()}`, role: 'assistant', content: '你好！告诉我你现在最想解决的职业问题。' }
  ]
  selectConversation(id)
}

function applySuggestion(text: string) {
  draft.value = text
}

function scrollToBottom() {
  if (!messagesEl.value) return
  messagesEl.value.scrollTop = messagesEl.value.scrollHeight
}

function send() {
  const content = draft.value.trim()
  if (!content) return

  const conversationId = activeConversationId.value
  const list = messagesByConversation.value[conversationId] ?? (messagesByConversation.value[conversationId] = [])
  list.push({ id: `u${Date.now()}`, role: 'user', content })
  draft.value = ''

  list.push({
    id: `a${Date.now() + 1}`,
    role: 'assistant',
    content: '收到。我会从“目标岗位要求 - 你当前经历 - 量化成果”三个维度给你一版更专业的表述。'
  })

  void nextTick(() => scrollToBottom())
}
</script>
