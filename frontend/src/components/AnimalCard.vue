<template>
  <article class="animal-card lift-card" @click="$router.push(`/animals/${animal.id}`)">
    <div class="animal-media">
      <img :src="animal.coverImageUrl || fallback" :alt="animal.typeText || '动物照片'" />
      <StatusTag class="float-tag" :value="animal.status" :text="animal.statusText" :options="animalStatusOptions" />
    </div>
    <div class="animal-body">
      <div class="animal-title">
        <h3>{{ animal.typeText || optionText(animalTypeOptions, animal.type) }}</h3>
        <span>{{ animal.genderText || optionText(genderOptions, animal.gender) }} · {{ animal.age ?? '未知' }}岁</span>
      </div>
      <p>{{ animal.description || animal.healthCondition || '暂无详细说明' }}</p>
      <div class="meta-line">
        <MapPin :size="16" />
        <span>{{ animal.foundRegion }}</span>
      </div>
    </div>
  </article>
</template>

<script setup>
import { MapPin } from 'lucide-vue-next'
import StatusTag from './StatusTag.vue'
import { animalStatusOptions, animalTypeOptions, genderOptions, optionText } from '../utils/status'
import { demoImages } from '../data/demoData'

defineProps({
  animal: { type: Object, required: true }
})

const fallback = demoImages[0]
</script>
