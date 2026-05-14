<template>
  <section class="view page">
    <div class="hero-board">
      <div class="hero-main">
        <div>
          <p class="eyebrow"><Sparkles :size="16" /> {{ $t('home.heroSubtitle') }}</p>
          <h1>{{ $t('home.heroTitle') }}</h1>
          <p>
            {{ $t('home.heroDescription') }}
          </p>
          <div class="hero-search">
            <el-input v-model="keyword" size="large" :placeholder="$t('home.searchPlaceholder')" @keyup.enter="goSearch" />
            <el-button :icon="Search" size="large" type="primary" @click="goSearch">{{ $t('common.search') }}</el-button>
          </div>
        </div>
        <div class="stat-row">
          <div class="stat-item">
            <strong>{{ overview.userCount }}</strong>
            <span>{{ $t('home.registeredUsers') }}</span>
          </div>
          <div class="stat-item">
            <strong>{{ overview.animalCount }}</strong>
            <span>{{ $t('home.animalRecords') }}</span>
          </div>
          <div class="stat-item">
            <strong>{{ overview.rescueCount }}</strong>
            <span>{{ $t('home.rescueInfo') }}</span>
          </div>
          <div class="stat-item">
            <strong>{{ overview.pendingAuditCount }}</strong>
            <span>{{ $t('home.pendingAudit') }}</span>
          </div>
        </div>
      </div>

      <aside class="hero-side">
        <div class="spotlight-photo">
          <img :src="animals[0]?.coverImageUrl || demoImages[0]" alt="待领养动物" />
          <div class="spotlight-badge">
            <Heart :size="18" />
            <span>{{ animals[0]?.description || $t('home.spotlightText') }}</span>
          </div>
        </div>
        <div class="side-list">
          <div class="side-list-head">
            <div>
              <h3>{{ $t('home.latestNotices') }}</h3>
              <p>{{ $t('home.latestNoticesDesc') }}</p>
            </div>
            <el-button text @click="$router.push('/notices')">{{ $t('common.viewAll') }}</el-button>
          </div>
          <RouterLink v-for="notice in notices" :key="notice.id" class="notice-link" :to="`/notices/${notice.id}`">
            <span>{{ notice.title }}</span>
            <ChevronRight :size="16" />
          </RouterLink>
          <EmptyState
            v-if="!notices.length"
            :title="$t('notices.noData')"
            :description="$t('notices.noDataDesc')"
          />
        </div>
      </aside>
    </div>

    <div class="section-head">
      <div>
        <h2>{{ $t('home.latestAnimals') }}</h2>
        <p>{{ $t('home.latestAnimalsDesc') }}</p>
      </div>
      <el-button :icon="ArrowRight" @click="$router.push('/animals')">{{ $t('common.viewAll') }}</el-button>
    </div>
    <div v-if="animals.length" class="grid animal-grid">
      <AnimalCard v-for="animal in animals" :key="animal.id" :animal="animal" />
    </div>
    <EmptyState v-else :title="$t('home.noAnimals')" :description="$t('home.noAnimalsDesc')" />

    <div class="section-head">
      <div>
        <h2>{{ $t('home.rescueDynamics') }}</h2>
        <p>{{ $t('home.rescueDynamicsDesc') }}</p>
      </div>
      <el-button :icon="Plus" type="primary" @click="$router.push('/rescues')">{{ $t('home.publishRescue') }}</el-button>
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
import { demoAnimals, demoImages, demoOverview, demoRescues } from '../data/demoData'
import { rescueStatusOptions } from '../utils/status'

const router = useRouter()
const keyword = ref('')
const overview = ref(demoOverview)
const animals = ref(demoAnimals)
const rescues = ref(demoRescues)
const notices = ref([])

function goSearch() {
  router.push({ name: 'animals', query: { keyword: keyword.value } })
}

onMounted(async () => {
  try {
    const data = await homeApi.overview()
    overview.value = data.overview || demoOverview
    animals.value = data.latestAnimals?.length ? data.latestAnimals : demoAnimals
    rescues.value = data.latestRescues?.length ? data.latestRescues : demoRescues
    notices.value = data.latestNotices || []
  } catch {
    overview.value = demoOverview
    animals.value = demoAnimals
    rescues.value = demoRescues
    notices.value = []
  }
})
</script>
