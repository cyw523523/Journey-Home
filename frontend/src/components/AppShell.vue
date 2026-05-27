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

        <div class="topbar-center">
          <nav class="nav-links nav-links-desktop" :aria-label="navigationLabel">
            <RouterLink
              v-for="item in desktopPrimaryNavItems"
              :key="item.key"
              class="nav-link"
              :to="item.to"
            >
              {{ item.label }}
            </RouterLink>
          </nav>

          <el-dropdown class="more-menu more-menu-desktop" trigger="click">
            <button class="nav-more-button" type="button">
              <span>{{ moreLabel }}</span>
              <ChevronDown :size="16" />
            </button>
            <template #dropdown>
              <el-dropdown-menu class="header-dropdown-menu">
                <el-dropdown-item
                  v-for="item in desktopSecondaryNavItems"
                  :key="item.key"
                  @click="goTo(item.to)"
                >
                  {{ item.label }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>

        <div class="topbar-actions">
          <button class="mobile-nav-toggle" type="button" @click="mobileNavOpen = true">
            <Menu :size="18" />
            <span>{{ navigationLabel }}</span>
          </button>

          <span v-if="auth.isLoggedIn.value" class="identity-badge" :class="{ 'is-admin': auth.isAdmin.value }">
            {{ identityLabel }}
          </span>

          <RouterLink
            v-if="auth.isAdmin.value"
            class="user-chip admin-entry desktop-auth-action"
            :title="$t('nav.admin')"
            to="/admin"
          >
            <ShieldCheck :size="16" />
            <span>{{ adminQuickLabel }}</span>
          </RouterLink>

          <button
            v-if="auth.isLoggedIn.value"
            class="user-chip action-pill icon-action desktop-auth-action notification-chip"
            type="button"
            :aria-label="messageLabel"
            :title="messageLabel"
            @click="goTo('/messages')"
          >
            <el-badge class="action-badge" :hidden="!messageSummary.unreadCount" :max="99" :value="messageSummary.unreadCount">
              <MessagesSquare :size="17" />
            </el-badge>
          </button>

          <el-dropdown v-if="auth.isLoggedIn.value" trigger="click" @visible-change="handleNotificationVisible">
            <button
              class="user-chip action-pill icon-action desktop-auth-action notification-chip"
              type="button"
              :aria-label="notificationLabel"
              :title="notificationLabel"
            >
              <el-badge class="action-badge" :hidden="!notificationSummary.unreadCount" :max="99" :value="notificationSummary.unreadCount">
                <Bell :size="17" />
              </el-badge>
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
                    <strong>{{ notificationTitle(item.title) }}</strong>
                    <span>{{ formatNotificationContent(item) }}</span>
                    <small>{{ formatTime(item.createdAt) }}</small>
                  </button>
                </div>
                <div v-else class="notification-empty">{{ t('notification.noNotifications') }}</div>
              </div>
            </template>
          </el-dropdown>

          <el-dropdown trigger="click" @command="changeLanguage">
            <button class="lang-switch" :title="currentLanguageCode" type="button">
              <Globe :size="17" />
              <span>{{ currentLanguageCode }}</span>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="zh">{{ $t('common.chinese') }}</el-dropdown-item>
                <el-dropdown-item command="en">{{ $t('common.english') }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <el-button v-if="!auth.isLoggedIn.value" :icon="LogIn" type="primary" @click="$router.push('/auth')">
            {{ $t('nav.login') }}
          </el-button>

          <el-dropdown v-else trigger="click">
            <button class="user-chip user-entry" type="button">
              <UserRound :size="17" />
              <span>{{ userDisplayName }}</span>
              <ChevronDown :size="16" />
            </button>
            <template #dropdown>
              <el-dropdown-menu class="header-dropdown-menu">
                <el-dropdown-item @click="goTo('/profile')">{{ $t('nav.profile') }}</el-dropdown-item>
                <el-dropdown-item v-if="auth.isAdmin.value" @click="goTo('/admin')">{{ $t('nav.admin') }}</el-dropdown-item>
                <el-dropdown-item divided @click="logout">{{ $t('nav.logout') }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <el-drawer
        v-model="mobileNavOpen"
        class="mobile-nav-drawer"
        direction="rtl"
        size="min(92vw, 360px)"
        :with-header="false"
      >
        <div class="mobile-nav-shell">
          <div class="mobile-nav-head">
            <strong>{{ $t('nav.brand') }}</strong>
            <span>{{ $t('nav.brandSub') }}</span>
          </div>

          <section class="mobile-nav-group">
            <p class="mobile-nav-label">{{ navigationLabel }}</p>
            <button
              v-for="item in primaryNavItems"
              :key="item.key"
              class="mobile-nav-link"
              :class="{ 'is-active': isRouteActive(item.to) }"
              type="button"
              @click="goTo(item.to)"
            >
              {{ item.label }}
            </button>
          </section>

          <section class="mobile-nav-group" v-if="secondaryNavItems.length">
            <p class="mobile-nav-label">{{ moreLabel }}</p>
            <button
              v-for="item in secondaryNavItems"
              :key="item.key"
              class="mobile-nav-link"
              :class="{ 'is-active': isRouteActive(item.to) }"
              type="button"
              @click="goTo(item.to)"
            >
              {{ item.label }}
            </button>
          </section>

          <section class="mobile-nav-group" v-if="mobileAccountItems.length">
            <p class="mobile-nav-label">{{ accountLabel }}</p>
            <button
              v-for="item in mobileAccountItems"
              :key="item.key"
              class="mobile-nav-link"
              :class="{ 'is-active': isRouteActive(item.to) }"
              type="button"
              @click="goTo(item.to)"
            >
              <span>{{ item.label }}</span>
              <small v-if="item.badge">{{ item.badge }}</small>
            </button>
          </section>

          <section class="mobile-nav-group" v-if="auth.isAdmin.value">
            <p class="mobile-nav-label">{{ adminAreaLabel }}</p>
            <button
              class="mobile-nav-link"
              :class="{ 'is-active': isRouteActive('/admin') }"
              type="button"
              @click="goTo('/admin')"
            >
              {{ $t('nav.admin') }}
            </button>
          </section>
        </div>
      </el-drawer>
    </header>

    <main class="page-stage">
      <RouterView />
    </main>

    <AiAssistant v-if="showAiAssistant" />
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  Bell,
  ChevronDown,
  Globe,
  HeartHandshake,
  LogIn,
  Menu,
  MessagesSquare,
  ShieldCheck,
  UserRound
} from 'lucide-vue-next'
import { messageApi, notificationApi } from '../api'
import { notifyError } from '../api/http'
import { connectMessageSocket, disconnectMessageSocket, subscribeMessageSocket } from '../services/messageSocket'
import { useAuth } from '../stores/auth'
import AiAssistant from './AiAssistant.vue'

const { locale, t } = useI18n()
const auth = useAuth()
const route = useRoute()
const router = useRouter()
const notifications = ref([])
const notificationSummary = ref({ unreadCount: 0 })
const messageSummary = ref({ unreadCount: 0 })
const mobileNavOpen = ref(false)
let refreshTimer = null
let unsubscribeMessageSocket = null

const currentLanguageCode = computed(() => locale.value.toUpperCase())
const navigationLabel = computed(() => t('appShell.navigation'))
const moreLabel = computed(() => t('appShell.more'))
const accountLabel = computed(() => t('appShell.account'))
const adminAreaLabel = computed(() => t('appShell.adminArea'))
const adminQuickLabel = computed(() => t('appShell.adminQuick'))
const mapLabel = computed(() => t('nav.map'))
const identityLabel = computed(() => {
  if (auth.isAdmin.value) {
    return t('statusLabel.admin')
  }
  return t('appShell.member')
})
const notificationLabel = computed(() => {
  const translated = t('notification.notifications')
  return translated === 'notification.notifications' ? t('appShell.notifications') : translated
})
const messageLabel = computed(() => {
  const translated = t('nav.messages')
  return translated === 'nav.messages' ? t('nav.messages') : translated
})
const userDisplayName = computed(() => {
  return auth.state.user?.nickname || auth.state.user?.account || t('appShell.myAccount')
})
const rescueStationPath = computed(() => {
  return auth.isAdmin.value ? '/admin/rescue-stations' : '/rescue-station'
})
const notificationCenterLocation = computed(() => ({
  path: '/profile',
  query: { tab: 'notifications' }
}))

const allPrimaryNavItems = computed(() => ([
  { key: 'home', label: t('nav.home'), to: '/' },
  { key: 'community', label: t('nav.community'), to: '/community' },
  { key: 'notices', label: t('nav.notices'), to: '/notices' },
  { key: 'animals', label: t('nav.animals'), to: '/animals' },
  { key: 'rescues', label: t('nav.rescues'), to: '/rescues' },
  { key: 'map', label: mapLabel.value, to: '/map' },
  { key: 'volunteer', label: t('nav.volunteerTasks'), to: '/volunteer-tasks' }
]))

const primaryNavItems = computed(() => allPrimaryNavItems.value)
const desktopPrimaryNavItems = computed(() => allPrimaryNavItems.value)

const secondaryNavItems = computed(() => {
  const items = [
    { key: 'donations', label: t('nav.donations'), to: '/donations' },
    { key: 'station', label: t('nav.rescueStation'), to: rescueStationPath.value }
  ]

  if (auth.isLoggedIn.value) {
    items.push({ key: 'profile', label: t('nav.profile'), to: '/profile' })
  }

  return items
})

const desktopSecondaryNavItems = computed(() => secondaryNavItems.value)

const mobileAccountItems = computed(() => {
  if (!auth.isLoggedIn.value) {
    return []
  }

  return [
    {
      key: 'messages',
      label: messageLabel.value,
      to: '/messages',
      badge: messageSummary.value.unreadCount ? `${messageSummary.value.unreadCount}` : ''
    },
    {
      key: 'notifications',
      label: notificationLabel.value,
      to: notificationCenterLocation.value,
      badge: notificationSummary.value.unreadCount ? `${notificationSummary.value.unreadCount}` : ''
    },
    { key: 'profile', label: t('nav.profile'), to: '/profile', badge: '' }
  ]
})

const showAiAssistant = computed(() => {
  return auth.isLoggedIn.value && !auth.isAdmin.value
})

function changeLanguage(lang) {
  locale.value = lang
  localStorage.setItem('language', lang)
}

function logout() {
  mobileNavOpen.value = false
  auth.logout()
  router.push('/')
}

function goTo(location) {
  mobileNavOpen.value = false
  router.push(location)
}

function isRouteActive(location) {
  const resolved = router.resolve(location).path
  if (resolved === '/') {
    return route.path === '/'
  }
  return route.path === resolved || route.path.startsWith(`${resolved}/`)
}

function formatTime(value) {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString(locale.value === 'zh' ? 'zh-CN' : 'en-US')
}

function notificationTitle(title) {
  if (!title) {
    return t('appShell.systemNotification')
  }
  const key = 'notification.' + title
  const translated = t(key)
  return translated === key ? title : translated
}

function formatNotificationContent(item) {
  if (item.title === 'COMMENT_REPLY_COMMENT' || item.title === 'COMMENT_REPLY_POST') {
    const parts = (item.content || '').split('|')
    if (parts.length === 2) {
      const key = 'notification.' + item.title + '_CONTENT'
      const translated = t(key, { nickname: parts[0], snippet: parts[1] })
      return translated === key ? item.content || '' : translated
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
    goTo(notificationCenterLocation.value)
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

watch(() => route.fullPath, () => {
  mobileNavOpen.value = false
})

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

.action-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 0;
}

.notification-menu {
  width: 340px;
  padding: 12px;
}

.notification-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}

.notification-list {
  display: grid;
  gap: 8px;
}

.notification-item {
  width: 100%;
  text-align: left;
  border: 1px solid rgba(58, 77, 67, 0.1);
  background: rgba(255, 255, 255, 0.94);
  border-radius: 18px;
  padding: 12px;
  display: grid;
  gap: 6px;
  cursor: pointer;
}

.notification-item.unread {
  border-color: rgba(52, 98, 84, 0.22);
  background: rgba(244, 249, 245, 0.98);
}

.notification-empty {
  color: var(--el-text-color-secondary);
  text-align: center;
  padding: 18px 8px;
}

:deep(.action-badge .el-badge__content.is-fixed) {
  top: 3px;
  right: 3px;
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.96);
}
</style>
