<template>
  <div class="app-shell">
    <div class="shell-ambient shell-ambient-a"></div>
    <div class="shell-ambient shell-ambient-b"></div>

    <header class="topbar-shell">
      <div class="topbar">
        <RouterLink class="brand" to="/">
          <span class="brand-mark"><HeartHandshake :size="22" /></span>
          <span class="brand-copy">
            <strong>{{ $t('nav.brand') }}</strong>
            <small>{{ $t('nav.brandSub') }}</small>
          </span>
        </RouterLink>

        <div class="nav-cluster">
          <nav class="nav-links">
            <RouterLink to="/">{{ $t('nav.home') }}</RouterLink>
            <RouterLink to="/community">{{ $t('nav.community') }}</RouterLink>
            <RouterLink to="/notices">{{ $t('nav.notices') }}</RouterLink>
            <RouterLink to="/animals">{{ $t('nav.animals') }}</RouterLink>
            <RouterLink to="/rescues">{{ $t('nav.rescues') }}</RouterLink>
            <RouterLink v-if="auth.isLoggedIn.value" to="/profile">{{ $t('nav.profile') }}</RouterLink>
            <RouterLink v-if="auth.isAdmin.value" to="/admin">{{ $t('nav.admin') }}</RouterLink>
          </nav>
        </div>

        <div class="top-actions">
          <button v-if="auth.isLoggedIn.value" class="user-chip notification-chip" @click="$router.push('/messages')">
            <MessagesSquare :size="17" />
            <span>{{ messageLabel }}</span>
            <el-badge v-if="messageSummary.unreadCount" :value="messageSummary.unreadCount" />
          </button>

          <el-dropdown @command="changeLanguage">
            <button class="lang-switch">
              <Globe :size="17" />
              <span>{{ currentLanguageLabel }}</span>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="zh">{{ $t('common.chinese') }}</el-dropdown-item>
                <el-dropdown-item command="en">{{ $t('common.english') }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <el-dropdown v-if="auth.isLoggedIn.value" @visible-change="handleNotificationVisible">
            <button class="user-chip notification-chip">
              <Bell :size="17" />
              <span>通知</span>
              <el-badge v-if="notificationSummary.unreadCount" :value="notificationSummary.unreadCount" />
            </button>
            <template #dropdown>
              <div class="notification-menu">
                <div class="notification-head">
                  <strong>{{ t('notification.notifications') }}</strong>
                  <el-button text size="small" @click.stop="markAllRead">{{ t('notification.markAllRead') }}</el-button>
                </div>
                <div v-if="notifications.length" class="notification-list">
                  <button
                    v-for="item in notifications"
                    :key="item.id"
                    class="notification-item"
                    :class="{ unread: !item.readFlag }"
                    @click="openNotification(item)"
                  >
                    <strong>{{ t('notification.' + item.title, item.title) }}</strong>
                    <span>{{ formatNotificationContent(item) }}</span>
                    <small>{{ formatTime(item.createdAt) }}</small>
                  </button>
                </div>
                <div v-else class="notification-empty">{{ t('notification.noNotifications') }}</div>
              </div>
            </template>
          </el-dropdown>

          <el-button v-if="!auth.isLoggedIn.value" :icon="LogIn" type="primary" @click="$router.push('/auth')">
            {{ $t('nav.login') }}
          </el-button>
          <el-dropdown v-else>
            <button class="user-chip">
              <UserRound :size="17" />
              <span>{{ auth.state.user?.nickname || auth.state.user?.account }}</span>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="$router.push('/profile')">{{ $t('nav.profile') }}</el-dropdown-item>
                <el-dropdown-item v-if="auth.isAdmin.value" @click="$router.push('/admin')">{{ $t('nav.admin') }}</el-dropdown-item>
                <el-dropdown-item divided @click="logout">{{ $t('nav.logout') }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <main class="page-stage">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Bell, Globe, HeartHandshake, LogIn, MessagesSquare, UserRound } from 'lucide-vue-next'
import { messageApi, notificationApi } from '../api'
import { notifyError } from '../api/http'
import { connectMessageSocket, disconnectMessageSocket, subscribeMessageSocket } from '../services/messageSocket'
import { useAuth } from '../stores/auth'

const { locale, t } = useI18n()
const auth = useAuth()
const router = useRouter()
const notifications = ref([])
const notificationSummary = ref({ unreadCount: 0 })
const messageSummary = ref({ unreadCount: 0 })
let refreshTimer = null
let unsubscribeMessageSocket = null

const currentLanguageLabel = computed(() => {
  return locale.value === 'zh' ? t('common.chinese') : t('common.english')
})

const messageLabel = computed(() => {
  const translated = t('nav.messages')
  return translated === 'nav.messages' ? '私信' : translated
})

function changeLanguage(lang) {
  locale.value = lang
  localStorage.setItem('language', lang)
}

function logout() {
  auth.logout()
  router.push('/')
}

function formatTime(value) {
  return value ? new Date(value).toLocaleString() : '-'
}

function formatNotificationContent(item) {
  if (item.title === 'COMMENT_REPLY_COMMENT' || item.title === 'COMMENT_REPLY_POST') {
    const parts = (item.content || '').split('|')
    if (parts.length === 2) {
      return t('notification.' + item.title + '_CONTENT', { nickname: parts[0], snippet: parts[1] })
    }
  }
  return item.content || ''
}

async function loadNotifications() {
  if (!auth.isLoggedIn.value) return
  try {
    const [listData, summaryData] = await Promise.all([
      notificationApi.list({ page: 0, size: 6 }),
      notificationApi.summary()
    ])
    notifications.value = listData.content || []
    notificationSummary.value = summaryData || { unreadCount: 0 }
  } catch (error) {
    notifyError(error)
  }
}

async function loadMessageSummary() {
  if (!auth.isLoggedIn.value) return
  try {
    messageSummary.value = await messageApi.summary()
  } catch (error) {
    notifyError(error)
  }
}

async function handleNotificationVisible(visible) {
  if (visible) {
    await loadNotifications()
  }
}

async function markAllRead() {
  try {
    await notificationApi.markAllRead()
    await loadNotifications()
  } catch (error) {
    notifyError(error)
  }
}

async function openNotification(item) {
  try {
    if (!item.readFlag) {
      await notificationApi.markRead(item.id)
    }
    await loadNotifications()
    router.push('/profile?tab=notifications')
  } catch (error) {
    notifyError(error)
  }
}

function handleMessagesUpdated() {
  loadMessageSummary()
}

function handleMessageSocketEvent(event) {
  if (!event || event.type === 'socket-open' || event.type === 'socket-close') return
  loadMessageSummary()
  window.dispatchEvent(new CustomEvent('messages:socket', { detail: event }))
}

function startRefreshTimer() {
  stopRefreshTimer()
  refreshTimer = window.setInterval(async () => {
    await loadNotifications()
  }, 20000)
}

function stopRefreshTimer() {
  if (refreshTimer) {
    window.clearInterval(refreshTimer)
    refreshTimer = null
  }
}

watch(() => auth.isLoggedIn.value, (loggedIn) => {
  if (loggedIn) {
    connectMessageSocket(auth.state.token)
    loadNotifications()
    loadMessageSummary()
    startRefreshTimer()
  } else {
    disconnectMessageSocket()
    notifications.value = []
    notificationSummary.value = { unreadCount: 0 }
    messageSummary.value = { unreadCount: 0 }
    stopRefreshTimer()
  }
}, { immediate: true })

onMounted(() => {
  window.addEventListener('messages:updated', handleMessagesUpdated)
  unsubscribeMessageSocket = subscribeMessageSocket(handleMessageSocketEvent)
  if (auth.isLoggedIn.value) {
    connectMessageSocket(auth.state.token)
    loadNotifications()
    loadMessageSummary()
    startRefreshTimer()
  }
})

onUnmounted(() => {
  window.removeEventListener('messages:updated', handleMessagesUpdated)
  unsubscribeMessageSocket?.()
  unsubscribeMessageSocket = null
  disconnectMessageSocket()
  stopRefreshTimer()
})
</script>

<style scoped>
.notification-chip {
  position: relative;
}

.notification-menu {
  width: 320px;
  padding: 10px;
}

.notification-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.notification-list {
  display: grid;
  gap: 8px;
}

.notification-item {
  width: 100%;
  text-align: left;
  border: 1px solid rgba(20, 39, 52, 0.08);
  background: white;
  border-radius: 12px;
  padding: 10px;
  display: grid;
  gap: 4px;
  cursor: pointer;
}

.notification-item.unread {
  border-color: rgba(34, 110, 90, 0.28);
  background: rgba(242, 248, 246, 0.9);
}

.notification-empty {
  color: var(--el-text-color-secondary);
  text-align: center;
  padding: 16px 8px;
}
</style>
