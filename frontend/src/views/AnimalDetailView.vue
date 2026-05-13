<template>
  <section class="view page">
    <el-skeleton v-if="loading" :rows="8" animated />
    <div v-else class="detail-layout">
      <div class="detail-photo surface">
        <img :src="animal.coverImageUrl || demoImages[0]" alt="动物详情照片" />
      </div>
      <div class="detail-panel surface">
        <p class="eyebrow"><PawPrint :size="16" /> 动物档案 #{{ animal.id }}</p>
        <StatusTag :value="animal.status" :text="animal.statusText" :options="animalStatusOptions" />
        <h1>{{ animal.typeText || optionText(animalTypeOptions, animal.type) }}</h1>
        <p class="muted">{{ animal.description || '暂无详细说明' }}</p>

        <div class="info-grid">
          <div class="info-box">
            <small>性别</small>
            <strong>{{ animal.genderText || optionText(genderOptions, animal.gender) }}</strong>
          </div>
          <div class="info-box">
            <small>年龄</small>
            <strong>{{ animal.age ?? '未知' }} 岁</strong>
          </div>
          <div class="info-box">
            <small>发现地区</small>
            <strong>{{ animal.foundRegion }}</strong>
          </div>
          <div class="info-box">
            <small>健康情况</small>
            <strong>{{ animal.healthCondition || '待补充' }}</strong>
          </div>
        </div>

        <div class="mini-row">
          <span>发布人：{{ animal.publisherNickname || '-' }}</span>
          <span>{{ animal.createdAt ? new Date(animal.createdAt).toLocaleDateString() : '' }}</span>
        </div>

        <div style="display: flex; gap: 10px; margin-top: 22px">
          <el-button :icon="ArrowLeft" @click="$router.back()">返回</el-button>
          <el-button
            v-if="animal.status === 'WAITING_ADOPTION'"
            :icon="HeartHandshake"
            type="primary"
            @click="apply"
          >
            提交领养申请
          </el-button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, HeartHandshake, PawPrint } from 'lucide-vue-next'
import StatusTag from '../components/StatusTag.vue'
import { animalApi } from '../api'
import { notifyError } from '../api/http'
import { useAuth } from '../stores/auth'
import { demoAnimals, demoImages } from '../data/demoData'
import { animalStatusOptions, animalTypeOptions, genderOptions, optionText } from '../utils/status'

const route = useRoute()
const router = useRouter()
const auth = useAuth()
const loading = ref(false)
const animal = ref({})

function apply() {
  if (!auth.isLoggedIn.value) {
    router.push({ name: 'auth', query: { redirect: route.fullPath } })
    return
  }
  router.push(`/adoptions/new/${animal.value.id}`)
}

onMounted(async () => {
  loading.value = true
  try {
    const data = await animalApi.detail(route.params.id)
    if (data && data.id) {
      animal.value = data
    } else {
      animal.value = demoAnimals.find((item) => String(item.id) === String(route.params.id)) || demoAnimals[0]
    }
  } catch {
    animal.value = demoAnimals.find((item) => String(item.id) === String(route.params.id)) || demoAnimals[0]
  } finally {
    loading.value = false
  }
})
</script>
