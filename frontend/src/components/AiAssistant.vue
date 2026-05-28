<template>
  <div class="ai-assistant-widget">
    <div
      class="ai-float-btn"
      :style="{ left: position.x + 'px', top: position.y + 'px' }"
      @mousedown="startDrag"
      @touchstart="startDrag"
      @click="handleClick"
    >
      <Bot :size="22" />
      <span class="ai-float-label">AI 助手</span>
    </div>

    <Teleport to="body">
      <Transition name="ai-dialog">
        <div v-if="isOpen" class="ai-dialog-overlay" @click.self="closeDialog">
          <div class="ai-dialog" :style="{ left: dialogPosition.x + 'px', top: dialogPosition.y + 'px' }">
            <div class="ai-dialog-header" @mousedown="startDialogDrag" @touchstart="startDialogDrag">
              <div class="ai-dialog-title">
                <div class="ai-dialog-title-icon">
                  <Bot :size="20" />
                </div>
                <div class="ai-dialog-title-text">
                  <strong>AI 智能助手</strong>
                  <span>{{ pageBadgeText }}</span>
                </div>
              </div>
              <div class="ai-dialog-actions">
                <button
                  class="ai-header-btn"
                  type="button"
                  title="新对话"
                  :disabled="loading"
                  @click.stop="resetConversation"
                >
                  <RefreshCcw :size="16" />
                </button>
                <button class="ai-header-btn" type="button" title="关闭" @click.stop="closeDialog">
                  <X :size="18" />
                </button>
              </div>
            </div>

            <div ref="messagesContainer" class="ai-dialog-messages">
              <div class="ai-context-bar">
                <span>{{ contextBarText }}</span>
              </div>

              <div v-if="messages.length === 0" class="ai-welcome">
                <div class="ai-welcome-mark">
                  <Bot :size="38" />
                </div>
                <div class="ai-welcome-copy">
                  <h3>我会结合站内内容继续和你聊</h3>
                  <p>
                    我会参考你当前页面、最近几轮对话和平台真实数据来回答。关闭浮窗不会清空本轮聊天，你也可以随时开始新对话。
                  </p>
                </div>

                <div class="ai-capability-grid">
                  <div v-for="item in capabilityCards" :key="item.title" class="ai-capability-card">
                    <strong>{{ item.title }}</strong>
                    <span>{{ item.description }}</span>
                  </div>
                </div>

                <div class="ai-section">
                  <div class="ai-section-label">从这里开始</div>
                  <div class="ai-chip-list">
                    <button
                      v-for="question in starterQuestions"
                      :key="question"
                      class="ai-chip"
                      type="button"
                      @click="sendQuickQuestion(question)"
                    >
                      {{ question }}
                    </button>
                  </div>
                </div>
              </div>

              <template v-else>
                <div v-for="message in messages" :key="message.id" class="ai-message" :class="message.role">
                  <div class="ai-message-avatar">
                    <User v-if="message.role === 'user'" :size="16" />
                    <Bot v-else :size="16" />
                  </div>
                  <div class="ai-message-bubble">
                    <div class="ai-message-content">{{ message.content }}</div>
                  </div>
                </div>
              </template>

              <div v-if="loading" class="ai-message assistant">
                <div class="ai-message-avatar">
                  <Bot :size="16" />
                </div>
                <div class="ai-message-bubble">
                  <div class="ai-message-content ai-typing">
                    <span></span><span></span><span></span>
                  </div>
                </div>
              </div>
            </div>

            <div class="ai-suggestion-bar">
              <button
                v-for="question in followUpSuggestions"
                :key="question"
                class="ai-suggestion-chip"
                type="button"
                :disabled="loading"
                @click="sendQuickQuestion(question)"
              >
                {{ question }}
              </button>
            </div>

            <div class="ai-dialog-input">
              <el-input
                v-model="inputMessage"
                :placeholder="inputPlaceholder"
                :disabled="loading"
                @keyup.enter="sendMessage"
              />
              <el-button type="primary" :loading="loading" circle @click="sendMessage">
                <Send :size="16" />
              </el-button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { Bot, RefreshCcw, Send, User, X } from 'lucide-vue-next'
import { useRoute } from 'vue-router'
import { aiAssistantApi } from '../api'
import { notifyError } from '../api/http'
import { useAiAssistantContext } from '../stores/aiAssistantContext'
import { useAuth } from '../stores/auth'

const STORAGE_PREFIX = 'guitu_ai_assistant_session_v3:'
const MAX_HISTORY_MESSAGES = 24

const route = useRoute()
const auth = useAuth()
const { context: pageContext } = useAiAssistantContext()

const isOpen = ref(false)
const loading = ref(false)
const inputMessage = ref('')
const messages = ref([])
const assistantSuggestedQuestions = ref([])
const messagesContainer = ref(null)
const position = ref(getDefaultLauncherPosition())
const dialogPosition = ref(getDefaultDialogPosition())
const isDragging = ref(false)
const isDialogDragging = ref(false)
const dragOffset = ref({ x: 0, y: 0 })
const hasMoved = ref(false)

const routeMetaMap = {
  home: {
    label: '首页',
    assistantHint: '我会把首页概览和站内最新内容一起考虑进回答。',
    starters: ['平台现在有哪些待领养动物', '首页最近有哪些新内容', '我第一次使用这个网站先看什么'],
    followUps: ['我现在适合先看哪个板块', '怎么领养一只动物', '你还能帮我做什么']
  },
  animals: {
    label: '动物档案',
    assistantHint: '我能结合当前筛选条件和动物列表帮你查找或比较。',
    starters: ['现在有什么猫可以领养', '我适合领养什么类型的动物', '领养前要重点看哪些信息'],
    followUps: ['帮我推荐适合新手的动物', '当前有哪些待领养动物', '怎么领养申请']
  },
  'animal-detail': {
    label: '动物详情',
    assistantHint: '我会优先参考这只动物的状态、健康信息和所在地。',
    starters: ['这只动物适合新手吗', '领养这只动物前我还需要了解什么', '这只动物现在能申请领养吗'],
    followUps: ['这只动物有什么需要特别注意', '申请领养要准备什么', '和它匹配的家庭是什么样']
  },
  rescues: {
    label: '救助信息',
    assistantHint: '我可以结合当前救助列表和状态帮你判断下一步。',
    starters: ['当前有哪些待处理救助', '发布救助信息要写哪些内容', '我该怎么筛选紧急救助'],
    followUps: ['这页能做什么', '救助信息怎么发布', '我如何联系发起人']
  },
  notices: {
    label: '公告列表',
    assistantHint: '我能帮你快速理解平台公告和制度变更。',
    starters: ['最近公告主要在讲什么', '新用户最该先看哪条公告', '这些公告和领养流程有什么关系'],
    followUps: ['帮我概括重点公告', '平台最近有什么变化', '这页能做什么']
  },
  'notice-detail': {
    label: '公告详情',
    assistantHint: '我会优先解释当前这条公告的重点和影响。',
    starters: ['这条公告主要讲了什么', '这条公告对我有什么影响', '我需要特别注意哪些要求'],
    followUps: ['帮我总结这条公告', '这条公告和哪个功能相关', '我下一步该做什么']
  },
  community: {
    label: '社区',
    assistantHint: '我可以帮你理解社区内容、互动方式和发帖规则。',
    starters: ['社区里现在都在聊什么', '我怎么发布一篇帖子', '新人在社区先看什么'],
    followUps: ['社区里怎么互动', '发帖要注意什么', '这页能做什么']
  },
  'community-category': {
    label: '社区分类',
    assistantHint: '我会把当前分类下的内容和讨论方向考虑进去。',
    starters: ['这个分类主要发什么内容', '我适合在这里发什么帖子', '这个分类最近有什么热门内容'],
    followUps: ['这个分类适合问什么', '帮我总结这里的内容', '我怎么发帖']
  },
  'community-detail': {
    label: '帖子详情',
    assistantHint: '我会结合当前帖子内容和讨论氛围来回答。',
    starters: ['这篇帖子主要讲了什么', '这篇帖子的评论重点是什么', '我回复这篇帖子该注意什么'],
    followUps: ['帮我概括这篇帖子', '这篇帖子适合继续追问什么', '我可以怎么互动']
  },
  donations: {
    label: '物资捐赠',
    assistantHint: '我能结合捐赠需求和缺口帮你判断怎么参与。',
    starters: ['当前最缺什么物资', '我怎么参与捐赠', '这条物资还差多少'],
    followUps: ['哪些需求最紧急', '捐赠流程是什么', '这页能做什么']
  },
  'volunteer-tasks': {
    label: '志愿任务',
    assistantHint: '我可以帮你看任务要求、报名方式和当前缺口。',
    starters: ['现在有哪些志愿任务', '我适合报名什么任务', '参加志愿任务前要准备什么'],
    followUps: ['这页能做什么', '任务还能报名吗', '我该怎么选任务']
  },
  messages: {
    label: '私聊消息',
    assistantHint: '我能结合消息页状态帮你看未读、联系对象和沟通入口。',
    starters: ['我现在有多少未读消息', '这页主要怎么用', '我怎么继续联系对方'],
    followUps: ['帮我看未读情况', '这页能做什么', '我应该先回复谁']
  },
  profile: {
    label: '个人中心',
    assistantHint: '我会结合你的个人中心信息，帮你梳理待办和入口。',
    starters: ['我当前有哪些待处理事项', '个人中心里可以做什么', '怎么领养申请进度'],
    followUps: ['我下一步该点哪里', '这页能做什么', '帮我梳理一下待办']
  },
  'adoption-new': {
    label: '领养申请',
    assistantHint: '我能结合当前申请页说明你该怎么填写和准备。',
    starters: ['领养申请要填哪些内容', '这页我下一步该怎么做', '申请前还要准备什么'],
    followUps: ['帮我看申请流程', '这页能做什么', '领养申请要注意什么']
  },
  map: {
    label: '周边地图',
    assistantHint: '我会结合当前地图和附近点位帮你找位置。',
    starters: ['附近有什么点位', '我怎么找离我近的救助站', '地图上这些点分别是什么'],
    followUps: ['帮我看附近资源', '这页能做什么', '最近的救助站在哪']
  },
  'rescue-station': {
    label: '救助站中心',
    assistantHint: '我会结合救助站中心的数据帮你看运营情况和入口。',
    starters: ['救助站中心能做什么', '我当前站点还缺什么', '有哪些待处理事项'],
    followUps: ['帮我梳理站点待办', '这页能做什么', '我先处理哪件事']
  },
  'user-profile': {
    label: '用户主页',
    assistantHint: '我会结合当前用户主页内容帮你理解对方信息。',
    starters: ['这个用户主要发布了什么', '我可以怎么和这个用户互动', '这页能看到哪些信息'],
    followUps: ['帮我概括这个主页', '这页能做什么', '我可以怎么联系对方']
  }
}

const capabilityCards = [
  {
    title: '查当前页',
    description: '像“现在有哪些猫可领养”“这条物资还差多少”这种，我会优先结合真实页面数据回答。'
  },
  {
    title: '接着聊',
    description: '像“比如呢”“那我适合哪个”“还有什么功能”这种追问，我会接着前面的上下文继续说。'
  },
  {
    title: '讲流程',
    description: '像领养、救助、发帖、捐赠这些整站使用问题，我会按平台真实流程讲清楚。'
  }
]

const routeMeta = computed(() => routeMetaMap[route.name] || {
  label: '当前页面',
  assistantHint: '我会优先参考你正在看的页面，再结合整站功能来回答。',
  starters: ['这个页面是做什么的', '我现在能做什么', '你能帮我做什么'],
  followUps: ['这页能做什么', '我下一步该怎么做', '帮我讲讲平台功能']
})

const pageBadgeText = computed(() => `当前参考：${routeMeta.value.label}`)
const contextBarText = computed(() => routeMeta.value.assistantHint)
const inputPlaceholder = computed(() => (
  messages.value.length
    ? '继续追问，我会记住刚才聊到的内容...'
    : '输入你的问题，我会结合页面和站内数据回答...'
))

const starterQuestions = computed(() => {
  const base = [
    '如何领养流浪动物',
    '领养需要什么条件',
    '如何发布救助信息'
  ]
  return uniqueQuestions([...routeMeta.value.starters, ...base]).slice(0, 6)
})

const followUpSuggestions = computed(() => {
  if (assistantSuggestedQuestions.value.length) {
    return uniqueQuestions(assistantSuggestedQuestions.value).slice(0, 3)
  }
  if (!messages.value.length) {
    return starterQuestions.value.slice(0, 3)
  }
  return uniqueQuestions(routeMeta.value.followUps).slice(0, 3)
})

const sessionStorageKey = computed(() => {
  const userId = auth.state.user?.id || 'guest'
  return `${STORAGE_PREFIX}${userId}`
})

function handleClick() {
  if (hasMoved.value) {
    hasMoved.value = false
    return
  }
  if (!isOpen.value) {
    openDialog()
  }
}

function openDialog() {
  isOpen.value = true
  if (!dialogPosition.value?.x && !dialogPosition.value?.y) {
    dialogPosition.value = getDefaultDialogPosition()
  }
  nextTick(scrollToBottom)
}

function closeDialog() {
  isOpen.value = false
}

function startDrag(event) {
  if (isOpen.value) {
    return
  }
  isDragging.value = true
  hasMoved.value = false
  const point = extractPoint(event)
  dragOffset.value = {
    x: point.x - position.value.x,
    y: point.y - position.value.y
  }
  event.preventDefault()
}

function startDialogDrag(event) {
  isDialogDragging.value = true
  const point = extractPoint(event)
  dragOffset.value = {
    x: point.x - dialogPosition.value.x,
    y: point.y - dialogPosition.value.y
  }
  event.preventDefault()
}

function handleMove(event) {
  if (isDragging.value) {
    const point = extractPoint(event)
    const nextX = point.x - dragOffset.value.x
    const nextY = point.y - dragOffset.value.y
    if (Math.abs(nextX - position.value.x) > 5 || Math.abs(nextY - position.value.y) > 5) {
      hasMoved.value = true
    }
    position.value = clampLauncherPosition(nextX, nextY)
  }

  if (isDialogDragging.value) {
    const point = extractPoint(event)
    dialogPosition.value = clampDialogPosition(
      point.x - dragOffset.value.x,
      point.y - dragOffset.value.y
    )
  }
}

function handleMoveEnd() {
  isDragging.value = false
  isDialogDragging.value = false
}

async function sendMessage() {
  const message = inputMessage.value.trim()
  if (!message || loading.value) {
    return
  }

  pushMessage('user', message)
  const history = buildRequestHistory()
  inputMessage.value = ''
  loading.value = true

  await nextTick()
  scrollToBottom()

  try {
    const response = await aiAssistantApi.chat({
      message,
      history,
      pageContext: requestContext.value
    })
    assistantSuggestedQuestions.value = Array.isArray(response.suggestions) ? response.suggestions : []
    pushMessage('assistant', response.reply || '我这次没有拿到有效回复，你可以换个问法再试一次。')
  } catch (error) {
    notifyError(error)
    assistantSuggestedQuestions.value = []
    pushMessage('assistant', '抱歉，我刚刚没能顺利回答。你可以稍后再试，或者换一种说法继续问我。')
  } finally {
    loading.value = false
    await nextTick()
    scrollToBottom()
  }
}

function sendQuickQuestion(question) {
  inputMessage.value = question
  sendMessage()
}

function resetConversation() {
  if (loading.value) {
    return
  }
  messages.value = []
  inputMessage.value = ''
  assistantSuggestedQuestions.value = []
}

function pushMessage(role, content) {
  messages.value.push({
    id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
    role,
    content
  })
}

function buildRequestHistory() {
  return messages.value
    .slice(0, -1)
    .filter((item) => item?.role && item?.content)
    .slice(-MAX_HISTORY_MESSAGES)
    .map((item) => ({
      role: item.role,
      content: item.content
    }))
}

function scrollToBottom() {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

function persistSession() {
  if (typeof window === 'undefined') {
    return
  }
  const payload = {
    messages: messages.value.slice(-MAX_HISTORY_MESSAGES * 2).map((item) => ({
      id: item.id,
      role: item.role,
      content: item.content
    })),
    suggestions: assistantSuggestedQuestions.value.slice(0, 6),
    position: position.value,
    dialogPosition: dialogPosition.value
  }
  window.localStorage.setItem(sessionStorageKey.value, JSON.stringify(payload))
}

function restoreSession() {
  if (typeof window === 'undefined') {
    return
  }
  const raw = window.localStorage.getItem(sessionStorageKey.value)
  if (!raw) {
    messages.value = []
    assistantSuggestedQuestions.value = []
    position.value = getDefaultLauncherPosition()
    dialogPosition.value = getDefaultDialogPosition()
    return
  }
  try {
    const parsed = JSON.parse(raw)
    messages.value = Array.isArray(parsed.messages)
      ? parsed.messages
        .filter((item) => item?.role && item?.content)
        .slice(-MAX_HISTORY_MESSAGES * 2)
      : []
    assistantSuggestedQuestions.value = Array.isArray(parsed.suggestions)
      ? parsed.suggestions.filter(Boolean).slice(0, 6)
      : []
    position.value = isValidPoint(parsed.position) ? clampLauncherPosition(parsed.position.x, parsed.position.y) : getDefaultLauncherPosition()
    dialogPosition.value = isValidPoint(parsed.dialogPosition) ? clampDialogPosition(parsed.dialogPosition.x, parsed.dialogPosition.y) : getDefaultDialogPosition()
  } catch {
    messages.value = []
    assistantSuggestedQuestions.value = []
    position.value = getDefaultLauncherPosition()
    dialogPosition.value = getDefaultDialogPosition()
  }
}

function uniqueQuestions(list) {
  return [...new Set((list || []).filter(Boolean))]
}

function extractPoint(event) {
  if (event.type.startsWith('touch')) {
    return {
      x: event.touches[0].clientX,
      y: event.touches[0].clientY
    }
  }
  return {
    x: event.clientX,
    y: event.clientY
  }
}

function getViewport() {
  if (typeof window === 'undefined') {
    return { width: 1440, height: 900 }
  }
  return {
    width: window.innerWidth,
    height: window.innerHeight
  }
}

function getDefaultLauncherPosition() {
  const viewport = getViewport()
  return {
    x: Math.max(20, viewport.width - 96),
    y: Math.max(100, viewport.height - 128)
  }
}

function getDefaultDialogPosition() {
  const viewport = getViewport()
  return clampDialogPosition(viewport.width - 448, viewport.height - 700)
}

function clampLauncherPosition(x, y) {
  const viewport = getViewport()
  return {
    x: Math.max(16, Math.min(viewport.width - 84, x)),
    y: Math.max(92, Math.min(viewport.height - 84, y))
  }
}

function clampDialogPosition(x, y) {
  const viewport = getViewport()
  const dialogWidth = Math.min(420, viewport.width - 24)
  const dialogHeight = Math.min(680, viewport.height - 24)
  return {
    x: Math.max(12, Math.min(viewport.width - dialogWidth - 12, x)),
    y: Math.max(12, Math.min(viewport.height - dialogHeight - 12, y))
  }
}

function isValidPoint(value) {
  return value && Number.isFinite(value.x) && Number.isFinite(value.y)
}

const requestContext = computed(() => ({
  routeName: typeof route.name === 'string' ? route.name : '',
  routePath: route.fullPath,
  ...(pageContext.value || {})
}))

watch(messages, async () => {
  persistSession()
  if (isOpen.value) {
    await nextTick()
    scrollToBottom()
  }
}, { deep: true })

watch([position, dialogPosition], () => {
  persistSession()
}, { deep: true })

watch(sessionStorageKey, () => {
  restoreSession()
}, { immediate: true })

watch(isOpen, async (open) => {
  if (open) {
    await nextTick()
    scrollToBottom()
  }
})

function handleResize() {
  position.value = clampLauncherPosition(position.value.x, position.value.y)
  dialogPosition.value = clampDialogPosition(dialogPosition.value.x, dialogPosition.value.y)
}

onMounted(() => {
  window.addEventListener('mousemove', handleMove)
  window.addEventListener('mouseup', handleMoveEnd)
  window.addEventListener('touchmove', handleMove)
  window.addEventListener('touchend', handleMoveEnd)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('mousemove', handleMove)
  window.removeEventListener('mouseup', handleMoveEnd)
  window.removeEventListener('touchmove', handleMove)
  window.removeEventListener('touchend', handleMoveEnd)
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.ai-float-btn {
  position: fixed;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 11px 15px;
  border: 1px solid rgba(32, 95, 80, 0.22);
  border-radius: 999px;
  background:
    radial-gradient(circle at top, rgba(255, 255, 255, 0.2), transparent 52%),
    linear-gradient(135deg, #1f5f52, #2f7a69 70%, #3f9681);
  color: #fffdf7;
  cursor: pointer;
  box-shadow: 0 16px 36px rgba(31, 95, 82, 0.28);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  user-select: none;
}

.ai-float-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 20px 42px rgba(31, 95, 82, 0.34);
}

.ai-float-label {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.ai-dialog-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
}

.ai-dialog {
  position: absolute;
  display: flex;
  flex-direction: column;
  width: min(420px, calc(100vw - 24px));
  height: min(680px, calc(100vh - 24px));
  border: 1px solid rgba(107, 83, 56, 0.12);
  border-radius: 28px;
  background:
    radial-gradient(circle at top right, rgba(214, 239, 226, 0.8), transparent 30%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(249, 245, 239, 0.98));
  box-shadow: 0 28px 90px rgba(49, 37, 21, 0.22);
  overflow: hidden;
}

.ai-dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 18px;
  background:
    radial-gradient(circle at top left, rgba(255, 255, 255, 0.14), transparent 26%),
    linear-gradient(135deg, #1f5f52, #2f7a69);
  color: #fff;
  cursor: move;
  user-select: none;
}

.ai-dialog-title {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.ai-dialog-title-icon {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.16);
}

.ai-dialog-title-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.ai-dialog-title-text strong {
  font-size: 18px;
  font-weight: 800;
}

.ai-dialog-title-text span {
  overflow: hidden;
  color: rgba(255, 255, 255, 0.82);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-dialog-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ai-header-btn {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border: none;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
  cursor: pointer;
  transition: background 0.2s ease, transform 0.2s ease;
}

.ai-header-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.24);
  transform: translateY(-1px);
}

.ai-header-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.ai-dialog-messages {
  flex: 1;
  padding: 16px 16px 10px;
  overflow-y: auto;
  background:
    linear-gradient(180deg, rgba(251, 248, 243, 0.55), rgba(247, 242, 235, 0.72)),
    linear-gradient(135deg, rgba(234, 245, 238, 0.15), transparent 40%);
}

.ai-context-bar {
  margin-bottom: 14px;
  padding: 10px 12px;
  border: 1px solid rgba(31, 95, 82, 0.1);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.72);
  color: #4a5e58;
  font-size: 12px;
  line-height: 1.5;
}

.ai-welcome {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.ai-welcome-mark {
  display: grid;
  width: 72px;
  height: 72px;
  place-items: center;
  border-radius: 24px;
  background:
    radial-gradient(circle at top, rgba(255, 255, 255, 0.92), rgba(236, 247, 241, 0.9)),
    linear-gradient(135deg, rgba(31, 95, 82, 0.06), rgba(63, 150, 129, 0.14));
  color: #1f5f52;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.7);
}

.ai-welcome-copy h3 {
  margin: 0 0 8px;
  color: #19211d;
  font-size: 26px;
  line-height: 1.2;
}

.ai-welcome-copy p {
  margin: 0;
  color: #5e5a52;
  font-size: 14px;
  line-height: 1.75;
}

.ai-capability-grid {
  display: grid;
  gap: 10px;
}

.ai-capability-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  border: 1px solid rgba(31, 95, 82, 0.1);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.82);
}

.ai-capability-card strong {
  color: #1f5f52;
  font-size: 14px;
}

.ai-capability-card span {
  color: #5c5951;
  font-size: 13px;
  line-height: 1.65;
}

.ai-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.ai-section-label {
  color: #48655c;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.ai-chip-list,
.ai-suggestion-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.ai-chip,
.ai-suggestion-chip {
  border: 1px solid rgba(31, 95, 82, 0.14);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  color: #1f5f52;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease, transform 0.2s ease;
}

.ai-chip {
  padding: 10px 14px;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.4;
}

.ai-suggestion-bar {
  padding: 0 16px 12px;
}

.ai-suggestion-chip {
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 600;
}

.ai-chip:hover,
.ai-suggestion-chip:hover:not(:disabled) {
  background: #1f5f52;
  color: #fff;
  transform: translateY(-1px);
}

.ai-suggestion-chip:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.ai-message {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}

.ai-message.user {
  flex-direction: row-reverse;
}

.ai-message-avatar {
  display: grid;
  width: 32px;
  height: 32px;
  flex: 0 0 32px;
  place-items: center;
  border-radius: 50%;
  background: #1f5f52;
  color: #fff;
  box-shadow: 0 8px 18px rgba(31, 95, 82, 0.18);
}

.ai-message.user .ai-message-avatar {
  background: #e76e52;
}

.ai-message-bubble {
  max-width: min(80%, 300px);
}

.ai-message-content {
  padding: 12px 15px;
  border-radius: 20px;
  background: #fff;
  color: #1e231f;
  font-size: 14px;
  line-height: 1.75;
  white-space: pre-wrap;
  word-break: break-word;
  box-shadow: 0 8px 24px rgba(39, 28, 17, 0.06);
}

.ai-message.user .ai-message-content {
  background: linear-gradient(135deg, #1f5f52, #2e7565);
  color: #fff;
}

.ai-typing {
  display: flex;
  gap: 4px;
  align-items: center;
  min-width: 52px;
}

.ai-typing span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #1f5f52;
  animation: typing 1.4s infinite ease-in-out;
}

.ai-typing span:nth-child(2) {
  animation-delay: 0.2s;
}

.ai-typing span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.4;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

.ai-dialog-input {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 14px 16px 18px;
  background: rgba(255, 255, 255, 0.92);
  border-top: 1px solid rgba(107, 83, 56, 0.08);
}

.ai-dialog-input .el-input {
  flex: 1;
}

.ai-dialog-input :deep(.el-input__wrapper) {
  min-height: 48px;
  border-radius: 999px;
  box-shadow: inset 0 0 0 1px rgba(31, 95, 82, 0.12);
}

.ai-dialog-input :deep(.el-button) {
  width: 46px;
  height: 46px;
  border: none;
  background: linear-gradient(135deg, #1f5f52, #2f7a69);
  box-shadow: 0 12px 28px rgba(31, 95, 82, 0.2);
}

.ai-dialog-enter-active,
.ai-dialog-leave-active {
  transition: opacity 0.24s ease, transform 0.24s ease;
}

.ai-dialog-enter-from,
.ai-dialog-leave-to {
  opacity: 0;
  transform: scale(0.97);
}

@media (max-width: 640px) {
  .ai-dialog {
    width: calc(100vw - 12px);
    height: calc(100vh - 12px);
    border-radius: 26px;
  }

  .ai-dialog-header {
    padding: 14px 14px 12px;
  }

  .ai-dialog-title-text strong {
    font-size: 17px;
  }

  .ai-welcome-copy h3 {
    font-size: 22px;
  }

  .ai-message-bubble {
    max-width: 84%;
  }
}
</style>
