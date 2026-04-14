<template>

  <div class="flex gap-10 w-full h-[calc(100vh-200px)] overflow-hidden">

    <aside class="hidden lg:flex w-80 bg-surface-container-low rounded-xl overflow-hidden flex-col">

      <div class="flex-1 overflow-y-auto overscroll-contain p-6 space-y-5">

        <h2 class="font-headline font-bold text-lg text-on-surface mb-4">对话历史</h2>

        <button

          class="w-full flex items-center justify-center gap-2 py-3 bg-primary-container text-white rounded-xl mb-6 hover:opacity-90 transition-all font-medium"

          type="button"

          @click="startNewConversation"

        >

          <span class="material-symbols-outlined text-sm">add</span>

          <span>开启新对话</span>

        </button>

        <div class="bg-white/70 backdrop-blur rounded-2xl p-5 shadow-sm border border-outline-variant/10">
          <h3 class="font-headline font-bold text-sm mb-4">你的今日建议</h3>
          <div class="space-y-4">
            <div v-for="(s, idx) in suggestionsForSidebar" :key="idx" class="flex items-start gap-3">
              <div class="w-8 h-8 rounded-lg bg-tertiary-fixed flex items-center justify-center text-tertiary">
                <span class="material-symbols-outlined text-lg">lightbulb</span>
              </div>
              <div>
                <p class="text-xs font-bold">{{ s.title }}</p>
                <p class="text-[11px] text-on-surface-variant">{{ s.text }}</p>
              </div>
            </div>
          </div>

        </div>

        <div class="space-y-1">
          <div
            v-for="c in conversations"
            :key="c.id"
            class="group relative w-full text-left p-4 rounded-xl flex items-center gap-3 cursor-pointer transition-colors"
            :class="c.id === activeConversationId ? 'bg-primary-fixed text-primary' : 'text-on-surface-variant hover:bg-surface-container-high'"
            @click="selectConversation(c.id)"
          >
            <span class="material-symbols-outlined" :class="c.id === activeConversationId ? 'text-primary' : ''">chat_bubble</span>
            <div class="flex-1 min-w-0">
              <p class="text-sm font-semibold truncate">{{ c.title }}</p>
              <p class="text-xs opacity-70">{{ formatTime(c.lastMessageAt) }}</p>
            </div>
            <button
              type="button"
              class="absolute right-2 top-1/2 -translate-y-1/2 p-1.5 rounded-lg bg-error/10 text-error opacity-0 group-hover:opacity-100 transition-opacity hover:bg-error/20"
              @click.stop="confirmDeleteConversation(c.id, c.title)"
              title="删除对话"
            >
              <span class="material-symbols-outlined text-sm">delete</span>
            </button>
          </div>
        </div>



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



    <section class="flex-1 flex flex-col bg-surface-container-lowest rounded-xl overflow-hidden min-h-[650px] relative min-w-0">

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

        <div class="flex gap-2 items-center">
          <!-- 简历选择器 -->
          <div class="relative">
            <select
              v-model="selectedResumeId"
              class="appearance-none bg-surface-container-low border border-outline-variant/20 rounded-lg px-3 py-1.5 pr-8 text-sm text-on-surface cursor-pointer hover:bg-surface-container focus:outline-none focus:ring-2 focus:ring-primary/20"
              @change="onResumeChange"
            >
              <option value="">选择简历</option>
              <option v-for="r in resumeList" :key="r.id" :value="r.id">{{ r.name }}</option>
            </select>
            <span class="material-symbols-outlined absolute right-1 top-1/2 -translate-y-1/2 text-on-surface-variant pointer-events-none text-sm">expand_more</span>
          </div>
          <button class="p-2 rounded-full hover:bg-surface-container-high text-on-surface-variant material-symbols-outlined" type="button">more_vert</button>
        </div>

      </div>



      <div ref="messagesEl" class="flex-1 overflow-y-auto p-8 space-y-8 pb-8">

        <div

          v-for="m in activeMessages"

          :key="m.id"

          class="flex items-start gap-4 max-w-6xl mx-auto w-full"

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

            <div v-if="m.role === 'assistant'" class="prose prose-sm max-w-none leading-relaxed message-content" v-html="renderMarkdown(m.content)"></div>
            <p v-else class="leading-relaxed whitespace-pre-wrap">{{ m.content }}</p>



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



      <div class="sticky bottom-0 left-0 w-full p-8 bg-gradient-to-t from-background via-background/95 to-transparent">

        <div class="flex gap-3 mb-5 overflow-x-auto max-w-6xl mx-auto">

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



        <form class="max-w-6xl mx-auto bg-surface-container-lowest rounded-2xl shadow-xl p-3 flex items-center gap-3 border border-outline-variant/10" @submit.prevent="send">
          <input ref="fileInput" type="file" class="hidden" @change="handleFileUpload" />
          <input ref="voiceInput" type="file" accept="audio/*" class="hidden" @change="handleVoiceUpload" />
          <button class="p-3 text-on-surface-variant hover:bg-surface-container-low rounded-xl transition-colors material-symbols-outlined" type="button" @click="fileInput?.click()">attach_file</button>
          <button class="p-3 text-on-surface-variant hover:bg-surface-container-low rounded-xl transition-colors material-symbols-outlined" type="button" @click="voiceInput?.click()">mic</button>
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

    <!-- 确认删除对话框 -->
    <div v-if="showDeleteConfirm" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click.self="showDeleteConfirm = false">
      <div class="bg-surface-container-lowest rounded-2xl p-6 max-w-sm w-full mx-4 shadow-2xl">
        <div class="flex items-center gap-3 mb-4">
          <div class="w-10 h-10 rounded-full bg-error/10 flex items-center justify-center">
            <span class="material-symbols-outlined text-error">warning</span>
          </div>
          <div>
            <h3 class="font-bold text-on-surface">确认删除</h3>
            <p class="text-xs text-on-surface-variant">此操作无法撤销</p>
          </div>
        </div>
        <p class="text-sm text-on-surface-variant mb-6">
          确定要删除对话「<span class="font-semibold text-on-surface">{{ deleteTargetTitle }}</span>」吗？
        </p>
        <div class="flex gap-3">
          <button
            type="button"
            class="flex-1 px-4 py-2.5 rounded-xl bg-surface-container-high text-on-surface font-medium hover:bg-surface-container-highest transition-colors"
            @click="showDeleteConfirm = false"
          >
            取消
          </button>
          <button
            type="button"
            class="flex-1 px-4 py-2.5 rounded-xl bg-error text-white font-medium hover:bg-error/90 transition-colors"
            @click="doDeleteConversation"
          >
            删除
          </button>
        </div>
      </div>
    </div>

  </div>

</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'

/**
 * 简单的 Markdown 解析函数（安全，只转换有限标记）
 */
function renderMarkdown(text: string): string {
  if (!text) return ''
  
  // 1. HTML 转义（防 XSS）
  let html = text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
  
  // 2. 代码块 ```code```
  html = html.replace(/```([\s\S]*?)```/g, '<pre class="code-block"><code>$1</code></pre>')
  
  // 3. 行内代码 `code`
  html = html.replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>')
  
  // 4. 标题 ### ## #（不处理 #### 及以上）
  html = html.replace(/^### (.+)$/gm, '<h3 class="msg-h3">$1</h3>')
  html = html.replace(/^## (.+)$/gm, '<h2 class="msg-h2">$1</h2>')
  html = html.replace(/^# (.+)$/gm, '<h1 class="msg-h1">$1</h1>')
  // #### 及以上保持原样，不做处理
  
  // 5. 粗体 **text**
  html = html.replace(/\*\*([^*]+)\*\*/g, '<strong class="msg-strong">$1</strong>')
  
  // 6. 斜体 *text*
  html = html.replace(/\*([^*]+)\*/g, '<em class="msg-em">$1</em>')

  // 7. URL 自动转可点击链接（在列表之前，避免破坏已生成的标签）
  // 匹配 https?:// 后的所有非空白字符，末尾去除标点
  html = html.replace(/(https?:\/\/\S+)/g, (match) => {
    const cleanUrl = match.replace(/[.,;:!?'")\]>]+$/, '')
    const hrefUrl = cleanUrl.replace(/&amp;/g, '&')
    return `<a href="${hrefUrl}" target="_blank" rel="noopener noreferrer" class="msg-link">${cleanUrl}</a>`
  })

  // 8. 无序列表 - item
  html = html.replace(/^- (.+)$/gm, '<li class="msg-li">$1</li>')
  // 包裹连续的 li
  html = html.replace(/(<li class="msg-li">.*<\/li>\n?)+/g, '<ul class="msg-ul">$&</ul>')
  
  // 9. 有序列表 1. item
  html = html.replace(/^\d+\. (.+)$/gm, '<li class="msg-li-ol">$1</li>')
  html = html.replace(/(<li class="msg-li-ol">.*<\/li>\n?)+/g, '<ol class="msg-ol">$&</ol>')
  
  // 10. 段落：连续非标签行包裹为 <p>
  const lines = html.split('\n')
  const result: string[] = []
  let paragraph: string[] = []
  
  for (const line of lines) {
    const trimmed = line.trim()
    const isBlockElement = /^<(pre|h[1-3]|ul|ol|li|p)/.test(trimmed)
    
    if (trimmed === '') {
      if (paragraph.length > 0) {
        result.push(`<p class="msg-p">${paragraph.join('<br>')}</p>`)
        paragraph = []
      }
    } else if (isBlockElement) {
      if (paragraph.length > 0) {
        result.push(`<p class="msg-p">${paragraph.join('<br>')}</p>`)
        paragraph = []
      }
      result.push(line)
    } else {
      paragraph.push(trimmed)
    }
  }
  
  if (paragraph.length > 0) {
    result.push(`<p class="msg-p">${paragraph.join('<br>')}</p>`)
  }
  
  return result.join('\n')
}
import {
  getConversations,
  createConversation,
  getMessages,
  sendMessage,
  deleteConversation,
  getDailySuggestions,
  uploadAttachment,
  voiceToText,
  type Conversation,
  type Message,
  type DailySuggestions,
  type QuickQuestion
} from '../api/chat'
import { listResumeAnalysis, type ResumeAnalysisListItem } from '../api/resume'

type ResumeItem = {
  id: number
  name: string
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

const conversations = ref<Conversation[]>([])
const activeConversationId = ref<number | null>(null)
const messagesMap = ref<Record<number, ChatMessage[]>>({})
const dailySuggestions = ref<DailySuggestions | null>(null)
const draft = ref('')
const messagesEl = ref<HTMLElement | null>(null)
const isLoading = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
const voiceInput = ref<HTMLInputElement | null>(null)
const showDeleteConfirm = ref(false)
const deleteTargetId = ref<number | null>(null)
const deleteTargetTitle = ref('')
const resumeList = ref<ResumeItem[]>([])
const selectedResumeId = ref<number | null>(null)

const activeMessages = computed(() => {
  if (!activeConversationId.value) return []
  return messagesMap.value[activeConversationId.value] ?? []
})

const quickQuestions = computed(() => {
  if (!dailySuggestions.value?.quickQuestions?.length) {
    return ['帮我润色这段简历', '模拟大厂面试', '如何谈更高薪水？', '推荐适合我的岗位']
  }
  return dailySuggestions.value.quickQuestions.map(q => q.text)
})

const suggestionsForSidebar = computed(() => {
  return dailySuggestions.value?.suggestions ?? [
    { title: '上传简历', text: '上传您的简历，获取个性化职业建议' },
    { title: '职业规划', text: '与 AI 讨论您的职业发展方向' }
  ]
})

onMounted(async () => {
  await loadConversations()
  await loadResumeList()
  await loadDailySuggestions()
})

async function loadConversations() {
  try {
    const list = await getConversations()
    conversations.value = list
    if (list.length > 0 && !activeConversationId.value) {
      await selectConversation(list[0].id)
    }
  } catch (e) {
    console.error('加载会话列表失败', e)
  }
}

async function loadDailySuggestions(resumeId?: number) {
  try {
    dailySuggestions.value = await getDailySuggestions(resumeId)
  } catch (e) {
    console.error('加载每日建议失败', e)
  }
}

async function loadResumeList() {
  try {
    const res = await listResumeAnalysis({ limit: 10 })
    if (res.data?.items) {
      resumeList.value = res.data.items
        .filter((item: ResumeAnalysisListItem) => item.analysis_id && item.original_file_name)
        .map((item: ResumeAnalysisListItem) => ({
          id: item.analysis_id!,
          name: item.original_file_name!
        }))
      // 默认选中第一个
      if (resumeList.value.length > 0 && !selectedResumeId.value) {
        selectedResumeId.value = resumeList.value[0].id
      }
    }
  } catch (e) {
    console.error('加载简历列表失败', e)
  }
}

async function onResumeChange() {
  // 切换简历后重新加载建议
  await loadDailySuggestions(selectedResumeId.value ?? undefined)
}

async function selectConversation(id: number) {
  activeConversationId.value = id
  if (!messagesMap.value[id]) {
    await loadMessages(id)
  }
  void nextTick(() => scrollToBottom())
}

async function loadMessages(conversationId: number) {
  try {
    const list = await getMessages(conversationId)
    messagesMap.value[conversationId] = list.map(m => ({
      id: String(m.id),
      role: m.role,
      content: m.content
    }))
  } catch (e) {
    console.error('加载消息失败', e)
  }
}

async function startNewConversation() {
  try {
    const conv = await createConversation('新对话')
    conversations.value = [conv, ...conversations.value]
    messagesMap.value[conv.id] = [{
      id: `m${Date.now()}`,
      role: 'assistant',
      content: '你好！告诉我你现在最想解决的职业问题。'
    }]
    await selectConversation(conv.id)
  } catch (e) {
    console.error('创建会话失败', e)
  }
}

function applySuggestion(text: string) {
  draft.value = text
}

function scrollToBottom() {
  if (!messagesEl.value) return
  messagesEl.value.scrollTop = messagesEl.value.scrollHeight
}

async function send() {
  const content = draft.value.trim()
  if (!content) return

  // 如果没有当前会话，先创建一个
  if (!activeConversationId.value) {
    try {
      const conv = await createConversation('新对话')
      conversations.value = [conv, ...conversations.value]
      activeConversationId.value = conv.id
      messagesMap.value[conv.id] = []
    } catch (e) {
      console.error('自动创建会话失败', e)
      return
    }
  }

  const conversationId = activeConversationId.value!
  const list = messagesMap.value[conversationId] ?? (messagesMap.value[conversationId] = [])

  // 添加用户消息
  list.push({ id: `u${Date.now()}`, role: 'user', content })
  draft.value = ''

  // 添加占位 AI 消息
  const aiMsgId = `a${Date.now()}`
  list.push({ id: aiMsgId, role: 'assistant', content: '' })
  void nextTick(() => scrollToBottom())

  try {
    const response = await sendMessage(conversationId, content, selectedResumeId.value ?? undefined)
    if (!response.body) {
      list[list.length - 1].content = '抱歉，发生了错误，请重试。'
      return
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let aiContent = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      const chunk = decoder.decode(value, { stream: true })
      aiContent += chunk
      // 更新消息内容
      const msg = list.find(m => m.id === aiMsgId)
      if (msg) msg.content = aiContent
      void nextTick(() => scrollToBottom())
    }
    
    // 流结束后刷新会话列表以显示新标题
    await loadConversations()
  } catch (e) {
    console.error('发送消息失败', e)
    const msg = list.find(m => m.id === aiMsgId)
    if (msg) msg.content = '抱歉，发生了错误，请重试。'
  }
}

async function handleFileUpload(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  try {
    const url = await uploadAttachment(file)
    draft.value += ` [附件: ${file.name}](${url}) `
  } catch (e) {
    console.error('上传附件失败', e)
    alert('上传附件失败')
  }
  target.value = ''
}

async function handleVoiceUpload(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  try {
    const text = await voiceToText(file)
    draft.value += text
  } catch (e) {
    console.error('语音转写失败', e)
    alert('语音转写失败')
  }
  target.value = ''
}

/**
 * 显示删除确认对话框
 */
function confirmDeleteConversation(id: number, title: string) {
  deleteTargetId.value = id
  deleteTargetTitle.value = title
  showDeleteConfirm.value = true
}

/**
 * 执行删除对话
 */
async function doDeleteConversation() {
  if (!deleteTargetId.value) return
  
  const id = deleteTargetId.value
  showDeleteConfirm.value = false
  
  try {
    await deleteConversation(id)
    conversations.value = conversations.value.filter(c => c.id !== id)
    delete messagesMap.value[id]
    
    // 如果删除的是当前会话，切换到其他会话
    if (activeConversationId.value === id) {
      if (conversations.value.length > 0) {
        await selectConversation(conversations.value[0].id)
      } else {
        activeConversationId.value = null
      }
    }
  } catch (e) {
    console.error('删除会话失败', e)
    alert('删除失败，请重试')
  }
}

function formatTime(dateStr: string | null): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(hours / 24)
  if (days < 7) return `${days}天前`
  return dateStr.slice(0, 10)
}
</script>

<style scoped>
/* AI 消息 Markdown 样式 */
.message-content {
  line-height: 1.7;
}

.message-content :deep(.msg-p) {
  margin-bottom: 0.75rem;
  line-height: 1.7;
}

.message-content :deep(.msg-p:last-child) {
  margin-bottom: 0;
}

.message-content :deep(.msg-h1) {
  font-size: 1.25rem;
  font-weight: 700;
  margin: 1rem 0 0.5rem;
  color: theme('colors.primary');
}

.message-content :deep(.msg-h2) {
  font-size: 1.125rem;
  font-weight: 600;
  margin: 0.875rem 0 0.375rem;
  color: theme('colors.primary');
}

.message-content :deep(.msg-h3) {
  font-size: 1rem;
  font-weight: 600;
  margin: 0.75rem 0 0.25rem;
  color: theme('colors.on-surface');
}

.message-content :deep(.msg-strong) {
  font-weight: 600;
  color: theme('colors.primary');
}

.message-content :deep(.msg-em) {
  font-style: italic;
  color: theme('colors.on-surface-variant');
}

.message-content :deep(.msg-link) {
  color: theme('colors.primary');
  text-decoration: underline;
  word-break: break-all;
}

.message-content :deep(.msg-link:hover) {
  color: theme('colors.on-primary-container');
}

.message-content :deep(.msg-ul),
.message-content :deep(.msg-ol) {
  margin: 0.5rem 0 0.75rem;
  padding-left: 1.25rem;
}

.message-content :deep(.msg-li),
.message-content :deep(.msg-li-ol) {
  margin-bottom: 0.25rem;
  line-height: 1.6;
}

.message-content :deep(.msg-ul) {
  list-style-type: disc;
}

.message-content :deep(.msg-ol) {
  list-style-type: decimal;
}

.message-content :deep(.code-block) {
  background: theme('colors.surface-container-high');
  border-radius: 0.5rem;
  padding: 0.75rem 1rem;
  margin: 0.75rem 0;
  overflow-x: auto;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 0.875rem;
  line-height: 1.5;
}

.message-content :deep(.code-block code) {
  background: transparent;
  padding: 0;
}

.message-content :deep(.inline-code) {
  background: theme('colors.surface-container-high');
  padding: 0.125rem 0.375rem;
  border-radius: 0.25rem;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 0.875em;
  color: theme('colors.primary');
}
</style>
