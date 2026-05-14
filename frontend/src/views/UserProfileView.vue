<template>
  <section class="view-narrow page">
    <el-skeleton v-if="loading" :rows="6" animated />
    <div v-else-if="profile" class="surface form-shell user-public-profile">
      <div class="user-profile-header">
        <el-avatar :src="getFullUrl(profile.avatarUrl)" :size="80">
          {{ profile.nickname?.slice(0, 1) }}
        </el-avatar>
        <div>
          <h1>{{ profile.nickname }}</h1>
          <StatusTag :value="profile.role" :text="profile.roleText" :options="roleOptions" />
          <p class="muted">加入时间：{{ formatTime(profile.createdAt) }}</p>
        </div>
      </div>
      <div class="user-profile-stats">
        <div class="stat-item">
          <span class="stat-num">{{ profile.animalCount }}</span>
          <span class="stat-label">发布的动物档案</span>
        </div>
        <div class="stat-item">
          <span class="stat-num">{{ profile.rescueCount }}</span>
          <span class="stat-label">发布的救助信息</span>
        </div>
      </div>
    </div>
    <EmptyState v-else title="用户不存在" description="该用户不存在或已注销" />
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import EmptyState from '../components/EmptyState.vue'
import StatusTag from '../components/StatusTag.vue'
import { userApi } from '../api'
import { notifyError } from '../api/http'
import { roleOptions } from '../utils/status'

const route = useRoute()
const loading = ref(false)
const profile = ref(null)

const API_BASE = window.location.origin

function getFullUrl(url) {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:')) return url
  return `${API_BASE}${url}`
}

function formatTime(value) {
  return value ? new Date(value).toLocaleString() : '-'
}

onMounted(async () => {
  loading.value = true
  try {
    profile.value = await userApi.publicProfile(route.params.id)
  } catch (error) {
    notifyError(error)
    profile.value = null
  } finally {
    loading.value = false
  }
})
</script>
