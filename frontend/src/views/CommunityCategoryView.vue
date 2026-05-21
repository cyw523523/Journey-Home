<template>
  <section class="view page">
    <div class="section-head"><h1>{{ category?.name || '...' }}</h1><p>{{ category?.description }}</p></div>
    <div class="toolbar tool-panel">
      <el-input v-model="keyword" placeholder="搜索帖子..." clearable @keyup.enter="load" />
      <el-button :icon="Search" type="primary" @click="load">搜索</el-button>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Search } from 'lucide-vue-next'
import { categoryApi, communityApi } from '../api'

const route = useRoute()
const category = ref(null)
const posts = ref([])
const keyword = ref('')
const page = ref(1)
const total = ref(0)

async function loadCategory() {
  try { const cats = await categoryApi.list(); category.value = cats.find(c => c.code === route.params.code) } catch {}
}
async function load() {
  try { const data = await communityApi.list({ keyword: keyword.value, categoryId: category.value?.id, page: page.value - 1, size: 10 }); posts.value = data.content || []; total.value = data.totalElements || 0 }
  catch { posts.value = []; total.value = 0 }
}

watch(() => route.params.code, () => { loadCategory(); load() })
onMounted(async () => { await loadCategory(); await load() })
</script>
