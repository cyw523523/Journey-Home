<template>
  <section class="view-narrow page">
    <el-skeleton v-if="loading" :rows="6" animated />
    <article v-else-if="notice" class="surface form-shell">
      <p class="eyebrow"><Megaphone :size="16" /> 平台公告</p>
      <StatusTag :value="notice.status" :text="notice.statusText" :options="noticeStatusOptions" />
      <h1 style="margin: 14px 0">{{ notice.title }}</h1>
      <p class="muted">{{ notice.publishedAt ? new Date(notice.publishedAt).toLocaleString() : '' }}</p>
      <div style="margin-top: 22px; line-height: 1.9; white-space: pre-wrap">{{ notice.content }}</div>
      <div style="margin-top: 26px">
        <el-button :icon="ArrowLeft" @click="$router.push('/notices')">{{ $t('notices.backToList') }}</el-button>
      </div>
    </article>
    <EmptyState
      v-else
      :title="$t('notices.notFound')"
      :description="$t('notices.notFoundDesc')"
    />
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, Megaphone } from 'lucide-vue-next'
import EmptyState from '../components/EmptyState.vue'
import StatusTag from '../components/StatusTag.vue'
import { noticeApi } from '../api'
import { notifyError } from '../api/http'
import { noticeStatusOptions } from '../utils/status'

const route = useRoute()
const loading = ref(false)
const notice = ref(null)

onMounted(async () => {
  loading.value = true
  try {
    notice.value = await noticeApi.detail(route.params.id)
  } catch (error) {
    notifyError(error)
    notice.value = null
  } finally {
    loading.value = false
  }
})
</script>
