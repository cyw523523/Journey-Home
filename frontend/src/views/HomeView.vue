<template>
  <section class="view page">
    <div class="hero-board">
      <div class="hero-main">
        <div>
          <p class="eyebrow"><Sparkles :size="16" /> 救助、登记、审核、领养，一条清晰的归途</p>
          <h1>归途</h1>
          <p>
            集中管理流浪动物档案、救助信息和领养申请，让爱心人士、救助发布者与管理员在同一个流程里协作。
          </p>
          <div class="hero-search">
            <el-input v-model="keyword" size="large" placeholder="搜索动物、地区或健康情况" @keyup.enter="goSearch" />
            <el-button :icon="Search" size="large" type="primary" @click="goSearch">搜索</el-button>
          </div>
        </div>
        <div class="stat-row">
          <div class="stat-item">
            <strong>{{ overview.userCount }}</strong>
            <span>注册用户</span>
          </div>
          <div class="stat-item">
            <strong>{{ overview.animalCount }}</strong>
            <span>动物档案</span>
          </div>
          <div class="stat-item">
            <strong>{{ overview.rescueCount }}</strong>
            <span>救助信息</span>
          </div>
          <div class="stat-item">
            <strong>{{ overview.pendingAuditCount }}</strong>
            <span>待审核</span>
          </div>
        </div>
      </div>

      <aside class="hero-side">
        <div class="spotlight-photo">
          <img :src="animals[0]?.coverImageUrl || demoImages[0]" alt="待领养动物" />
          <div class="spotlight-badge">
            <Heart :size="18" />
            <span>{{ animals[0]?.description || '每一条档案，都应该能被及时看见。' }}</span>
          </div>
        </div>
        <div class="side-list">
          <RouterLink v-for="notice in notices" :key="notice.id" class="notice-link" :to="`/notices/${notice.id}`">
            <span>{{ notice.title }}</span>
            <ChevronRight :size="16" />
          </RouterLink>
        </div>
      </aside>
    </div>

    <div class="section-head">
      <div>
        <h2>最新动物档案</h2>
        <p>只展示审核通过、可公开查看的动物信息。</p>
      </div>
      <el-button :icon="ArrowRight" @click="$router.push('/animals')">查看全部</el-button>
    </div>
    <div v-if="animals.length" class="grid animal-grid">
      <AnimalCard v-for="animal in animals" :key="animal.id" :animal="animal" />
    </div>
    <EmptyState v-else title="暂无动物档案" description="后端连接后会显示最新审核通过的动物。" />

    <div class="section-head">
      <div>
        <h2>救助动态</h2>
        <p>公开展示待处理、处理中和已完成的救助信息。</p>
      </div>
      <el-button :icon="Plus" type="primary" @click="$router.push('/rescues')">发布救助</el-button>
    </div>
    <div class="grid rescue-grid">
      <article v-for="rescue in rescues" :key="rescue.id" class="rescue-card lift-card">
        <StatusTag :value="rescue.status" :text="rescue.statusText" :options="rescueStatusOptions" />
        <h3>{{ rescue.location }}</h3>
        <p>{{ rescue.animalCondition }}</p>
        <div class="meta-line">
          <Phone :size="16" />
          <span>{{ rescue.contact }}</span>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { ArrowRight, ChevronRight, Heart, Phone, Plus, Search, Sparkles } from 'lucide-vue-next'
import AnimalCard from '../components/AnimalCard.vue'
import EmptyState from '../components/EmptyState.vue'
import StatusTag from '../components/StatusTag.vue'
import { homeApi } from '../api'
import { demoAnimals, demoImages, demoNotices, demoOverview, demoRescues } from '../data/demoData'
import { rescueStatusOptions } from '../utils/status'

const router = useRouter()
const keyword = ref('')
const overview = ref(demoOverview)
const animals = ref(demoAnimals)
const rescues = ref(demoRescues)
const notices = ref(demoNotices)

function goSearch() {
  router.push({ name: 'animals', query: { keyword: keyword.value } })
}

onMounted(async () => {
  try {
    const data = await homeApi.overview()
    overview.value = data.overview || demoOverview
    animals.value = data.latestAnimals?.length ? data.latestAnimals : demoAnimals
    rescues.value = data.latestRescues?.length ? data.latestRescues : demoRescues
    notices.value = data.latestNotices?.length ? data.latestNotices : demoNotices
  } catch {
    overview.value = demoOverview
    animals.value = demoAnimals
    rescues.value = demoRescues
    notices.value = demoNotices
  }
})
</script>
