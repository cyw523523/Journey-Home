<template>
  <section class="view-narrow page">
    <el-skeleton v-if="loading" :rows="6" animated />
    <article v-else class="surface form-shell">
      <p class="eyebrow"><Megaphone :size="16" /> 平台公告</p>
      <StatusTag :value="notice.status" :text="notice.statusText" :options="noticeStatusOptions" />
      <h1 style="margin: 14px 0">{{ notice.title }}</h1>
      <p class="muted">{{ notice.publishedAt ? new Date(notice.publishedAt).toLocaleString() : '' }}</p>
      <div style="margin-top: 22px; line-height: 1.9; white-space: pre-wrap">{{ notice.content }}</div>
      <div style="margin-top: 26px">
        <el-button :icon="ArrowLeft" @click="$router.back()">返回</el-button>
      </div>
    </article>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, Megaphone } from 'lucide-vue-next'
import StatusTag from '../components/StatusTag.vue'
import { noticeApi } from '../api'
import { demoNotices } from '../data/demoData'
import { noticeStatusOptions } from '../utils/status'

const route = useRoute()
const loading = ref(false)
const notice = ref({})

onMounted(async () => {
  loading.value = true
  try {
    notice.value = await noticeApi.detail(route.params.id)
  } catch {
    notice.value = demoNotices.find((item) => String(item.id) === String(route.params.id)) || demoNotices[0]
  } finally {
    loading.value = false
  }
})
</script>
