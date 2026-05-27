<template>
  <section class="view-narrow page">
    <el-skeleton v-if="loading" :rows="6" animated />
    <div v-else-if="profile" class="surface form-shell user-public-profile">
      <div class="user-profile-header">
        <el-avatar :src="getFullUrl(profile.avatarUrl)" :size="80">
          {{ profile.nickname?.slice(0, 1) }}
        </el-avatar>
        <div style="flex:1;min-width:0">
          <h1>{{ profile.nickname }}</h1>
          <StatusTag :value="profile.role" :text="profile.roleText" :options="roleOptions" />
          <p class="muted">{{ t('userProfile.joinedAt') }}{{ formatTime(profile.createdAt) }}</p>
          <div v-if="stationInfo && profile.role === 'RESCUER'" class="station-badge">
            🏠 {{ stationInfo.stationName }}
            <StatusTag v-if="stationInfo.certificationStatus === 'APPROVED'" value="APPROVED" :options="certificationOptions" size="small" />
            <StatusTag v-else-if="stationInfo.certificationStatus === 'PENDING'" value="PENDING" :options="certificationOptions" size="small" />
          </div>
        </div>
      </div>

      <!-- Station info card -->
      <div v-if="stationInfo && profile.role === 'RESCUER'" class="station-info-card">
        <p v-if="stationInfo.description" class="muted">{{ stationInfo.description }}</p>
        <div class="station-mini-meta">
          <span v-if="stationInfo.address"><MapPin :size="13" /> {{ stationInfo.address }}</span>
          <span><Users :size="13" /> {{ t('userProfile.followersCount', { count: stationInfo.followerCount }) }}</span>
        </div>
      </div>

      <div class="user-profile-stats">
        <div class="stat-item">
          <span class="stat-num">{{ profile.animalCount }}</span>
          <span class="stat-label">{{ t('userProfile.animalRecords') }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-num">{{ profile.rescueCount }}</span>
          <span class="stat-label">{{ t('userProfile.rescuePosts') }}</span>
        </div>
      </div>
      <div v-if="auth.isLoggedIn.value && auth.state.user?.id !== profile.id" class="profile-action-row">
        <FollowButton v-if="profile.role === 'RESCUER' && stationInfo" :station-user-id="Number(route.params.id)" />
        <el-button type="primary" @click="openChat(profile.id)">{{ t('userProfile.sendMessage') }}</el-button>
        <el-button type="danger" plain @click="reportVisible = true">{{ t('userProfile.report') }}</el-button>
      </div>
    </div>
    <EmptyState v-else :title="t('userProfile.notFoundTitle')" :description="t('userProfile.notFoundDesc')" />
    <ReportDialog v-model="reportVisible" target-type="USER" :target-id="route.params.id" />
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MapPin, Users } from 'lucide-vue-next'
import { useRoute, useRouter } from 'vue-router'
import EmptyState from '../components/EmptyState.vue'
import FollowButton from '../components/FollowButton.vue'
import ReportDialog from '../components/ReportDialog.vue'
import StatusTag from '../components/StatusTag.vue'
import { userApi, rescueStationApi } from '../api'
import { useAiAssistantPageContext } from '../composables/useAiAssistantPageContext'
import { notifyError } from '../api/http'
import { useAuth } from '../stores/auth'
import { certificationOptions, roleOptions } from '../utils/status'

const route = useRoute()
const router = useRouter()
const auth = useAuth()
const { locale, t } = useI18n()
const loading = ref(false)
const profile = ref(null)
const stationInfo = ref(null)
const reportVisible = ref(false)

useAiAssistantPageContext(() => ({
  pageTitle: profile.value?.nickname || t('userProfile.pageTitle'),
  pageSummary: t('userProfile.pageSummary'),
  entityType: 'USER_PROFILE',
  entityId: profile.value?.id || Number(route.params.id) || null,
  viewData: {
    profile: profile.value ? {
      id: profile.value.id,
      nickname: profile.value.nickname,
      roleText: profile.value.roleText,
      animalCount: profile.value.animalCount,
      rescueCount: profile.value.rescueCount
    } : null,
    stationInfo: stationInfo.value ? {
      stationName: stationInfo.value.stationName,
      certificationStatusText: stationInfo.value.certificationStatusText,
      followerCount: stationInfo.value.followerCount
    } : null
  }
}))

const API_BASE = window.location.origin

function getFullUrl(url) {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:')) return url
  return `${API_BASE}${url}`
}

function formatTime(value) {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString(locale.value === 'zh' ? 'zh-CN' : 'en-US')
}

function openChat(userId) {
  router.push({ path: '/messages', query: { userId: String(userId) } })
}

onMounted(async () => {
  loading.value = true
  try {
    profile.value = await userApi.publicProfile(route.params.id)
    if (profile.value?.role === 'RESCUER') {
      try {
        stationInfo.value = await rescueStationApi.publicStation(route.params.id)
      } catch (err) {
        if (err?.response?.status !== 404) stationInfo.value = null
      }
    }
  } catch (error) {
    notifyError(error)
    profile.value = null
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.station-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  font-size: 14px;
  font-weight: 500;
}
.station-info-card {
  margin: 12px 0;
  padding: 12px 16px;
  background: var(--panel-soft);
  border-radius: 10px;
}
.station-mini-meta {
  display: flex;
  gap: 16px;
  margin-top: 6px;
  font-size: 13px;
  color: var(--muted);
}
</style>
