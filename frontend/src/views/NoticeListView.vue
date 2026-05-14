<template>
  <section class="view page">
    <div class="section-head">
      <div>
        <h1>{{ $t('notices.title') }}</h1>
        <p>{{ $t('notices.description') }}</p>
      </div>
    </div>

    <div class="toolbar tool-panel notice-toolbar">
      <el-input v-model="keyword" :placeholder="$t('notices.keyword')" clearable @keyup.enter="load" />
      <el-button :icon="Search" type="primary" @click="load">{{ $t('notices.filter') }}</el-button>
    </div>

    <el-skeleton v-if="loading" :rows="6" animated />
    <div v-else-if="notices.length" class="notice-stack">
      <article v-for="notice in notices" :key="notice.id" class="notice-card lift-card">
        <div class="notice-card-head">
          <div>
            <p class="eyebrow"><Megaphone :size="16" /> {{ $t('notices.publishedAt') }}</p>
            <h3>{{ notice.title }}</h3>
          </div>
          <StatusTag :value="notice.status" :text="notice.statusText" :options="noticeStatusOptions" />
        </div>
        <p class="notice-card-content">{{ excerpt(notice.content) }}</p>
        <div class="notice-card-meta">
          <span>{{ $t('notices.publisher') }}：{{ notice.publisherNickname || '-' }}</span>
          <span>{{ notice.publishedAt ? new Date(notice.publishedAt).toLocaleString() : '-' }}</span>
        </div>
        <div class="notice-card-actions">
          <el-button type="primary" plain @click="$router.push(`/notices/${notice.id}`)">
            {{ $t('notices.readMore') }}
          </el-button>
        </div>
      </article>
    </div>
    <EmptyState
      v-else
      :title="$t('notices.noData')"
      :description="$t('notices.noDataDesc')"
    />

    <div v-if="total > pageSize" style="display: flex; justify-content: center; margin-top: 24px">
      <el-pagination v-model:current-page="page" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="load" />
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { Megaphone, Search } from 'lucide-vue-next'
import EmptyState from '../components/EmptyState.vue'
import StatusTag from '../components/StatusTag.vue'
import { noticeApi } from '../api'
import { notifyError } from '../api/http'
import { noticeStatusOptions } from '../utils/status'

const loading = ref(false)
const notices = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const keyword = ref('')

function excerpt(content) {
  if (!content) return ''
  const normalized = content.replace(/\s+/g, ' ').trim()
  return normalized.length > 160 ? `${normalized.slice(0, 160)}...` : normalized
}

async function load() {
  loading.value = true
  try {
    const data = await noticeApi.list({
      keyword: keyword.value,
      page: page.value - 1,
      size: pageSize
    })
    notices.value = data.content || []
    total.value = data.totalElements || 0
  } catch (error) {
    notifyError(error)
    notices.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
