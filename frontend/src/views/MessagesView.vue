<template>
  <section class="view page">
    <div class="section-head">
      <div>
        <h1>{{ tr('messages.title', '私聊消息') }}</h1>
        <p>{{ tr('messages.description', '和发布人、救助者或社区里的其他用户直接沟通。') }}</p>
      </div>
    </div>

    <div class="message-layout surface">
      <aside class="conversation-panel">
        <div class="conversation-panel-head">
          <div>
            <strong>{{ tr('messages.conversations', '会话列表') }}</strong>
            <p>{{ tr('messages.unread', `未读 ${totalUnread}`, { count: totalUnread }) }}</p>
          </div>
          <el-button text @click="refreshAll">{{ tr('common.refresh', '刷新') }}</el-button>
        </div>

        <div v-if="listLoading" class="conversation-loading">
          <el-skeleton :rows="6" animated />
        </div>
        <div v-else-if="conversations.length" class="conversation-list">
          <button
            v-for="item in conversations"
            :key="item.id"
            class="conversation-item"
            :class="{ active: activeConversation?.id === item.id }"
            @click="openConversation(item.id)"
          >
            <el-badge :value="item.unreadCount || undefined" :hidden="!item.unreadCount" class="conversation-badge">
              <el-avatar :src="getFullUrl(item.peer.avatarUrl)" :size="42">
                {{ item.peer.nickname?.slice(0, 1) }}
              </el-avatar>
            </el-badge>
            <div class="conversation-copy">
              <div class="conversation-title-row">
                <strong>{{ item.peer.nickname }}</strong>
                <span>{{ formatTime(item.lastMessageAt || item.updatedAt || item.createdAt) }}</span>
              </div>
              <p>{{ previewText(item) }}</p>
            </div>
          </button>
        </div>
        <EmptyState
          v-else
          class="conversation-empty-state"
          :title="tr('messages.emptyTitle', '还没有私聊会话')"
          :description="tr('messages.emptyDesc', '可以从用户头像或主页发起私聊。')"
        />
      </aside>

      <main class="chat-panel">
        <template v-if="activeConversation">
          <header class="chat-head">
            <div class="chat-peer">
              <el-avatar :src="getFullUrl(activeConversation.peer.avatarUrl)" :size="50">
                {{ activeConversation.peer.nickname?.slice(0, 1) }}
              </el-avatar>
              <div>
                <strong>{{ activeConversation.peer.nickname }}</strong>
                <p>{{ activeConversation.peer.roleText }}</p>
              </div>
            </div>
            <div class="chat-head-actions">
              <el-button text @click="openUserProfile(activeConversation.peer.id)">{{ tr('nav.profile', '主页') }}</el-button>
              <el-button text @click="refreshActive">{{ tr('common.refresh', '刷新') }}</el-button>
            </div>
          </header>

          <div ref="messageScrollRef" class="message-scroll">
            <div v-if="detailLoading" class="conversation-loading">
              <el-skeleton :rows="8" animated />
            </div>
            <div v-else-if="messages.length" class="message-thread">
              <article
                v-for="item in messages"
                :key="item.id"
                class="message-row"
                :class="{ mine: item.sender.id === auth.state.user?.id }"
              >
                <el-avatar class="message-avatar" :src="getFullUrl(item.sender.avatarUrl)" :size="34">
                  {{ item.sender.nickname?.slice(0, 1) }}
                </el-avatar>
                <div class="message-bubble-wrap">
                  <div class="message-meta">
                    <strong>{{ item.sender.nickname }}</strong>
                    <span>{{ formatTime(item.createdAt) }}</span>
                    <span
                      v-if="item.sender.id === auth.state.user?.id"
                      :class="item.readFlag ? 'read-label' : 'unread-dot'"
                    >
                      {{ item.readFlag ? '已读' : '未读' }}
                    </span>
                  </div>
                  <div v-if="item.content" class="message-bubble">{{ item.content }}</div>
                  <div v-if="item.imageUrls?.length" class="message-image-grid">
                    <el-image
                      v-for="(url, index) in item.imageUrls"
                      :key="url"
                      :src="getFullUrl(url)"
                      :preview-src-list="item.imageUrls.map(getFullUrl)"
                      :initial-index="index"
                      fit="cover"
                      preview-teleported
                      class="message-image"
                    />
                  </div>
                </div>
              </article>
            </div>
            <EmptyState
              v-else
              class="chat-empty-state"
              :title="tr('messages.startTitle', '还没有消息')"
              :description="tr('messages.startDesc', '发一条消息，开始这段对话。')"
            />
          </div>

          <div class="chat-composer">
            <div v-if="composer.imageUrls.length" class="composer-image-strip">
              <div v-for="(url, index) in composer.imageUrls" :key="url" class="composer-image-chip">
                <el-image
                  :src="getFullUrl(url)"
                  :preview-src-list="composer.imageUrls.map(getFullUrl)"
                  :initial-index="index"
                  fit="cover"
                  preview-teleported
                  class="composer-image-thumb"
                />
                <button class="composer-image-remove" type="button" @click="removeComposerImage(index)">
                  <X :size="12" />
                </button>
              </div>
            </div>

            <el-input
              v-model="composer.content"
              type="textarea"
              :rows="3"
              maxlength="2000"
              show-word-limit
              :placeholder="tr('messages.placeholder', '输入私聊内容...')"
            />

            <div class="chat-composer-foot">
              <div class="chat-composer-tools">
                <el-upload
                  class="composer-upload"
                  :action="uploadAction"
                  :headers="uploadHeaders"
                  :show-file-list="false"
                  :multiple="true"
                  accept=".jpg,.jpeg,.png,.webp"
                  :on-success="handleComposerUploadSuccess"
                  :on-error="handleComposerUploadError"
                  :on-exceed="handleComposerUploadExceed"
                  :limit="6"
                >
                  <button class="composer-tool-button" type="button" aria-label="上传图片">
                    <ImagePlus :size="18" />
                  </button>
                </el-upload>
                <span class="chat-composer-hint">
                  {{ tr('messages.composeHint', '可发送文字和图片，打开会话后会自动标记收到的消息为已读。') }}
                </span>
              </div>
              <el-button :loading="sending" type="primary" @click="sendMessage">
                {{ tr('messages.send', '发送') }}
              </el-button>
            </div>
          </div>
        </template>

        <EmptyState
          v-else
          class="chat-empty-state"
          :title="tr('messages.emptyTitle', '还没有私聊会话')"
          :description="tr('messages.entryHint', '先从左侧选择会话，或从任意用户头像进入私聊。')"
        />
      </main>
    </div>
  </section>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ImagePlus, X } from 'lucide-vue-next'
import EmptyState from '../components/EmptyState.vue'
import { messageApi } from '../api'
import { notifyError } from '../api/http'
import { useAuth } from '../stores/auth'

const auth = useAuth()
const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const conversations = ref([])
const activeConversation = ref(null)
const messages = ref([])
const listLoading = ref(false)
const detailLoading = ref(false)
const sending = ref(false)
const messageScrollRef = ref(null)
const composer = reactive({ content: '', imageUrls: [] })

const totalUnread = computed(() => conversations.value.reduce((sum, item) => sum + (item.unreadCount || 0), 0))

const API_BASE = window.location.origin
const uploadAction = '/api/files/upload?usage=direct-message'
const uploadHeaders = computed(() => {
  const token = localStorage.getItem('guitu_token')
  return token ? { Authorization: `Bearer ${token}` } : {}
})

function getFullUrl(url) {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:')) return url
  return `${API_BASE}${url}`
}

function formatTime(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString()
}

function previewText(item) {
  if (!item.lastMessageContent) return '...'
  const prefix = item.lastSenderId === auth.state.user?.id ? '我: ' : ''
  return prefix + item.lastMessageContent
}

function tr(key, fallback, params = undefined) {
  const translated = t(key, params)
  return translated === key ? fallback : translated
}

function avatarTargetUserId() {
  return Number(route.query.userId)
}

function routeConversationId() {
  return Number(route.query.conversationId)
}

function sortConversation(a, b) {
  const timeA = new Date(a.lastMessageAt || a.updatedAt || a.createdAt || 0).getTime()
  const timeB = new Date(b.lastMessageAt || b.updatedAt || b.createdAt || 0).getTime()
  return timeB - timeA
}

function upsertConversation(conversation) {
  const next = [...conversations.value]
  const index = next.findIndex((item) => item.id === conversation.id)
  if (index >= 0) {
    next[index] = { ...next[index], ...conversation }
  } else {
    next.unshift(conversation)
  }
  conversations.value = next.sort(sortConversation)
}

function syncRoute(conversation) {
  router.replace({
    path: '/messages',
    query: {
      conversationId: String(conversation.id),
      userId: String(conversation.peer.id)
    }
  })
}

function notifyMessageStateChanged() {
  window.dispatchEvent(new Event('messages:updated'))
}

function distanceToBottom() {
  const target = messageScrollRef.value
  if (!target) return 0
  return target.scrollHeight - target.scrollTop - target.clientHeight
}

function shouldStickToBottom() {
  return distanceToBottom() < 48
}

async function loadConversations(options = {}) {
  const { silent = false } = options
  if (!silent) {
    listLoading.value = true
  }
  try {
    const data = await messageApi.listConversations({ page: 0, size: 50 })
    conversations.value = (data.content || []).slice().sort(sortConversation)
  } catch (error) {
    notifyError(error)
    conversations.value = []
  } finally {
    if (!silent) {
      listLoading.value = false
    }
  }
}

async function applyDetail(detail, options = {}) {
  const { shouldSyncRoute = true, autoScroll = true } = options
  activeConversation.value = detail.conversation
  messages.value = detail.messages || []
  upsertConversation(detail.conversation)
  await nextTick()
  if (autoScroll) {
    scrollToBottom()
  }
  notifyMessageStateChanged()
  if (shouldSyncRoute) {
    syncRoute(detail.conversation)
  }
}

async function openConversation(conversationId, shouldSyncRoute = true, options = {}) {
  if (!conversationId) return
  const { silent = false, autoScroll = true } = options
  if (!silent) {
    detailLoading.value = true
  }
  try {
    const detail = await messageApi.detail(conversationId)
    await applyDetail(detail, { shouldSyncRoute, autoScroll })
  } catch (error) {
    notifyError(error)
  } finally {
    if (!silent) {
      detailLoading.value = false
    }
  }
}

async function openConversationWithUser(userId, shouldSyncRoute = true, options = {}) {
  if (!userId) return
  const { silent = false, autoScroll = true } = options
  if (!silent) {
    detailLoading.value = true
  }
  try {
    const detail = await messageApi.detailWithUser(userId)
    await applyDetail(detail, { shouldSyncRoute, autoScroll })
  } catch (error) {
    notifyError(error)
  } finally {
    if (!silent) {
      detailLoading.value = false
    }
  }
}

async function openFromRoute() {
  const targetUserId = avatarTargetUserId()
  const conversationId = routeConversationId()

  if (targetUserId && activeConversation.value?.peer.id === targetUserId) return
  if (conversationId && activeConversation.value?.id === conversationId) return

  if (targetUserId) {
    await openConversationWithUser(targetUserId)
    return
  }
  if (conversationId) {
    await openConversation(conversationId)
    return
  }
  if (conversations.value.length) {
    await openConversation(conversations.value[0].id)
  } else {
    activeConversation.value = null
    messages.value = []
  }
}

async function refreshActive() {
  if (activeConversation.value?.id) {
    await openConversation(activeConversation.value.id, false)
  }
  await loadConversations()
}

async function refreshAll() {
  await loadConversations()
  if (activeConversation.value?.id) {
    await openConversation(activeConversation.value.id, false)
  } else {
    await openFromRoute()
  }
}

async function sendMessage() {
  const content = composer.content.trim()
  const imageUrls = [...composer.imageUrls]
  if ((!content && !imageUrls.length) || !activeConversation.value?.id) return
  sending.value = true
  try {
    await messageApi.send(activeConversation.value.id, { content, imageUrls })
    composer.content = ''
    composer.imageUrls = []
    await openConversation(activeConversation.value.id, false, { silent: true, autoScroll: true })
    await loadConversations({ silent: true })
  } catch (error) {
    notifyError(error)
  } finally {
    sending.value = false
  }
}

function scrollToBottom() {
  const target = messageScrollRef.value
  if (!target) return
  target.scrollTop = target.scrollHeight
}

function openUserProfile(userId) {
  router.push(`/users/${userId}`)
}

function handleComposerUploadSuccess(response) {
  if (!response?.success) {
    ElMessage.error(response?.message || '图片上传失败')
    return
  }
  composer.imageUrls = [...composer.imageUrls, response.data.url]
}

function handleComposerUploadError() {
  ElMessage.error('图片上传失败，请稍后重试')
}

function handleComposerUploadExceed() {
  ElMessage.warning('最多上传 6 张图片')
}

function removeComposerImage(index) {
  composer.imageUrls = composer.imageUrls.filter((_, currentIndex) => currentIndex !== index)
}

async function handleSocketMessage(event) {
  const payload = event.detail
  if (!payload?.conversationId) return

  await loadConversations({ silent: true })

  if (activeConversation.value?.id === payload.conversationId) {
    await openConversation(payload.conversationId, false, {
      silent: true,
      autoScroll: payload.type === 'message-created' ? shouldStickToBottom() : false
    })
  }
}

watch(() => [route.query.userId, route.query.conversationId], async () => {
  await openFromRoute()
})

onMounted(async () => {
  await loadConversations()
  await openFromRoute()
  window.addEventListener('messages:socket', handleSocketMessage)
})

onUnmounted(() => {
  window.removeEventListener('messages:socket', handleSocketMessage)
})
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  width: min(1880px, calc(100vw - 8px));
  max-width: none;
  height: calc(100dvh - 8px);
  max-height: calc(100dvh - 8px);
  min-height: 0;
  overflow: hidden;
}

.page > .section-head:first-of-type {
  flex: 0 0 auto;
  margin: 0 0 8px;
  padding: 10px 14px;
  border-radius: 18px;
}

.page > .section-head:first-of-type h1 {
  font-size: clamp(28px, 2.4vw, 42px);
}

.page > .section-head:first-of-type p {
  margin-top: 4px;
  font-size: 13px;
}

.message-layout {
  display: grid;
  flex: 1 1 auto;
  grid-template-columns: 420px minmax(0, 1fr);
  gap: 0;
  overflow: hidden;
  height: auto;
  min-height: 0;
}

.conversation-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  border-right: 1px solid rgba(36, 92, 80, 0.08);
  background: rgba(255, 255, 255, 0.7);
  min-height: 0;
}

.conversation-panel-head,
.chat-head,
.chat-composer {
  padding: 18px 20px;
}

.conversation-panel-head,
.chat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid rgba(36, 92, 80, 0.08);
}

.conversation-panel-head strong,
.chat-peer strong {
  display: block;
  font-size: 18px;
}

.conversation-panel-head p,
.chat-peer p,
.chat-composer-hint,
.conversation-title-row span,
.message-meta span,
.conversation-copy p {
  margin: 4px 0 0;
  color: var(--muted);
  font-size: 13px;
}

.conversation-list {
  display: grid;
  align-content: start;
  grid-auto-rows: max-content;
  gap: 10px;
  padding: 14px;
  overflow: auto;
  min-height: 0;
}

.conversation-item {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 12px;
  align-items: center;
  padding: 14px;
  border: 1px solid rgba(36, 92, 80, 0.08);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.88);
  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.conversation-item:hover,
.conversation-item.active {
  border-color: rgba(36, 92, 80, 0.22);
  transform: translateY(-1px);
  box-shadow: 0 18px 28px rgba(67, 54, 37, 0.08);
}

.conversation-copy {
  min-width: 0;
}

.conversation-title-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.conversation-title-row strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conversation-copy p {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  min-width: 0;
  min-height: 0;
}

.chat-peer {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chat-head-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.message-scroll {
  overflow: auto;
  min-height: 0;
  padding: 22px;
  background:
    radial-gradient(circle at top right, rgba(36, 92, 80, 0.08), transparent 28%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.82), rgba(250, 245, 239, 0.7));
}

.message-thread {
  display: grid;
  align-content: start;
  gap: 16px;
}

.message-row {
  display: flex;
  gap: 10px;
  align-items: flex-end;
}

.message-row.mine {
  flex-direction: row-reverse;
}

.message-bubble-wrap {
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: flex-start;
  flex: 0 1 auto;
  max-width: min(520px, 78%);
}

.message-meta {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.message-row.mine .message-meta {
  justify-content: flex-end;
}

.message-row.mine .message-bubble-wrap {
  align-items: flex-end;
}

.message-bubble {
  display: inline-block;
  width: fit-content;
  max-width: 100%;
  padding: 12px 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(36, 92, 80, 0.08);
  color: #30413b;
  line-height: 1.75;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-image-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  max-width: min(420px, 100%);
}

.message-image {
  width: 132px;
  height: 132px;
  border-radius: 16px;
  overflow: hidden;
  cursor: zoom-in;
  border: 1px solid rgba(36, 92, 80, 0.08);
  background: rgba(255, 255, 255, 0.9);
}

.message-image :deep(.el-image__inner) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.message-row.mine .message-bubble {
  background: rgba(36, 92, 80, 0.94);
  color: #fff;
  border-color: transparent;
}

.unread-dot {
  display: inline-flex;
  align-items: center;
  padding: 1px 8px;
  border-radius: 10px;
  background: rgba(218, 91, 74, 0.12);
  color: #c0392b;
  font-size: 11px;
  font-weight: 600;
}

.read-label {
  font-size: 11px;
  color: var(--muted);
}

.chat-composer {
  display: grid;
  gap: 10px;
  border-top: 1px solid rgba(36, 92, 80, 0.08);
  background: rgba(255, 255, 255, 0.82);
}

.chat-composer :deep(.el-textarea__inner) {
  min-height: 88px;
}

.composer-image-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.composer-image-chip {
  position: relative;
}

.composer-image-thumb {
  width: 64px;
  height: 64px;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid rgba(36, 92, 80, 0.12);
}

.composer-image-thumb :deep(.el-image__inner) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.composer-image-remove {
  position: absolute;
  top: -6px;
  right: -6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border: none;
  border-radius: 999px;
  background: rgba(23, 33, 30, 0.86);
  color: #fff;
  cursor: pointer;
}

.chat-composer-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.chat-composer-tools {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  flex: 1;
}

.chat-composer-hint {
  min-width: 0;
}

.composer-upload {
  display: inline-flex;
}

.composer-upload :deep(.el-upload) {
  display: inline-flex;
}

.composer-tool-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border: 1px solid rgba(36, 92, 80, 0.16);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.94);
  color: #245c50;
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease, background-color 0.18s ease;
}

.composer-tool-button:hover {
  border-color: rgba(36, 92, 80, 0.32);
  background: rgba(236, 245, 242, 0.96);
  transform: translateY(-1px);
}

.conversation-loading {
  padding: 18px;
}

.conversation-empty-state {
  margin: 14px;
  min-height: calc(100% - 28px);
  place-items: start center;
  align-content: start;
  padding-top: 24px;
}

.chat-empty-state {
  height: 100%;
  min-height: 0;
  place-items: start center;
  align-content: start;
  padding-top: 40px;
}

@media (max-width: 980px) {
  .page {
    width: 100%;
    height: calc(100dvh - 16px);
    max-height: calc(100dvh - 16px);
  }

  .message-layout {
    grid-template-columns: 1fr;
    height: auto;
  }

  .conversation-panel {
    border-right: none;
    border-bottom: 1px solid rgba(36, 92, 80, 0.08);
    max-height: 34vh;
  }

  .chat-panel {
    min-height: 0;
  }
}

@media (max-width: 680px) {
  .page {
    width: 100%;
    height: calc(100dvh - 12px);
    max-height: calc(100dvh - 12px);
  }

  .conversation-panel-head,
  .chat-head,
  .chat-composer {
    padding: 16px;
  }

  .message-scroll {
    padding: 16px;
  }

  .message-bubble-wrap {
    max-width: 100%;
  }

  .message-image {
    width: 110px;
    height: 110px;
  }

  .chat-composer-foot {
    align-items: stretch;
    flex-direction: column;
  }

  .chat-composer-tools {
    width: 100%;
  }
}
</style>
