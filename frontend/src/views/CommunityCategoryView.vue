<template>
  <section class="view page">
    <div class="section-head">
      <div>
        <el-button text :icon="ArrowLeft" @click="$router.push('/community')">
          {{ $t('community.backToList') }}
        </el-button>
        <h1 style="margin-top:4px">{{ category?.name || '...' }}</h1>
        <p v-if="category?.description">{{ category.description }}</p>
      </div>
    </div>

    <div class="toolbar tool-panel">
      <el-input v-model="keyword" :placeholder="$t('community.keyword')" clearable @keyup.enter="load" style="max-width:300px" />
      <el-button :icon="Search" type="primary" @click="load">{{ $t('community.filter') }}</el-button>
    </div>

    <el-skeleton v-if="loading" :rows="6" animated />
    <div v-else-if="posts.length" class="community-feed">
      <article v-for="post in posts" :key="post.id" class="community-card lift-card">
        <div class="community-card-head">
          <div>
            <h3 class="post-title-link" @click="$router.push(`/community/${post.id}`)">{{ post.title }}</h3>
            <p class="muted author-line">
              <RouterLink :to="`/users/${post.authorId}`" class="author-link">{{ post.authorNickname }}</RouterLink>
              <span> · {{ post.authorRoleText }} · {{ formatTime(post.createdAt) }}</span>
            </p>
          </div>
          <StatusTag v-if="post.status !== 'PUBLISHED'" :value="post.status" :text="post.statusText" :options="communityPostStatusOptions" />
        </div>
        <p class="community-card-content">{{ excerpt(post.content) }}</p>
        <div v-if="post.imageUrls?.length" class="post-card-images">
          <img v-for="url in post.imageUrls.slice(0, 4)" :key="url" :src="getFullUrl(url)" class="post-card-thumb" />
          <span v-if="post.imageUrls.length > 4" class="more-images">+{{ post.imageUrls.length - 4 }}</span>
        </div>
        <div class="community-card-foot">
          <span class="muted">{{ post.commentCount }} {{ $t('community.commentCount') }}</span>
          <el-button text @click="$router.push(`/community/posts/${post.id}`)">{{ $t('notices.readMore') }}</el-button>
        </div>
      </article>
    </div>
    <EmptyState
      v-else
      :title="$t('community.noData')"
      :description="$t('community.noDataDesc')"
    />

    <div v-if="total > 10" style="display:flex;justify-content:center;margin-top:24px">
      <el-pagination v-model:current-page="page" :page-size="10" :total="total" layout="prev, pager, next" @current-change="load" />
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { ArrowLeft, Search } from 'lucide-vue-next'
import EmptyState from '../components/EmptyState.vue'
import StatusTag from '../components/StatusTag.vue'
import { categoryApi, communityApi } from '../api'
import { communityPostStatusOptions } from '../utils/status'

const route = useRoute()
const category = ref(null)
const posts = ref([])
const keyword = ref('')
const page = ref(1)
const total = ref(0)
const loading = ref(false)

const API_BASE = window.location.origin

function getFullUrl(url) {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:')) return url
  return `${API_BASE}${url}`
}

function formatTime(value) {
  return value ? new Date(value).toLocaleString() : '-'
}

function excerpt(content) {
  if (!content) return ''
  const normalized = content.replace(/\s+/g, ' ').trim()
  return normalized.length > 220 ? `${normalized.slice(0, 220)}...` : normalized
}

async function loadCategory() {
  try {
    const cats = await categoryApi.list()
    category.value = cats.find(c => c.code === route.params.code)
  } catch {}
}

async function load() {
  loading.value = true
  try {
    const data = await communityApi.list({
      keyword: keyword.value,
      categoryId: category.value?.id,
      page: page.value - 1,
      size: 10
    })
    posts.value = data.content || []
    total.value = data.totalElements || 0
  } catch {
    posts.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

watch(() => route.params.code, () => { page.value = 1; loadCategory().then(load) })
onMounted(async () => { await loadCategory(); await load() })
</script>
