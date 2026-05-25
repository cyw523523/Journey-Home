<template>
  <article class="animal-card lift-card" @click="$router.push(`/animals/${animal.id}`)">
    <div class="animal-media">
      <img :src="animal.coverImageUrl || fallback" :alt="animal.typeText || 'animal'" />
      <div class="animal-media-overlay"></div>
      <StatusTag class="float-tag" :value="animal.status" :text="animal.statusText" :options="animalStatusOptions" />
      <div v-if="distanceLabel" class="distance-badge">
        <Navigation :size="14" />
        <span>{{ distanceLabel }}</span>
      </div>
      <div class="animal-media-note">
        <MapPin :size="14" />
        <span>{{ animal.foundRegion || 'Unknown region' }}</span>
      </div>
    </div>
    <div class="animal-body">
      <div class="animal-title">
        <div>
          <h3>{{ animal.typeText || optionText(animalTypeOptions, animal.type) }}</h3>
          <span class="animal-subline">{{ animal.genderText || optionText(genderOptions, animal.gender) }} / {{ ageLabel }}</span>
        </div>
        <span class="animal-arrow">
          <ArrowUpRight :size="18" />
        </span>
      </div>
      <p>{{ animal.description || animal.healthCondition || 'No detailed description yet.' }}</p>
      <div class="animal-footer">
        <div v-if="distanceLabel" class="meta-line distance-line">
          <Navigation :size="16" />
          <span>{{ distanceLabel }}</span>
        </div>
        <div class="meta-line">
          <HeartPulse :size="16" />
          <span>{{ animal.healthCondition || 'Needs gentle care and attention' }}</span>
        </div>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import { ArrowUpRight, HeartPulse, MapPin, Navigation } from 'lucide-vue-next'
import StatusTag from './StatusTag.vue'
import { animalStatusOptions, animalTypeOptions, genderOptions, optionText } from '../utils/status'
import { demoImages } from '../data/demoData'

const props = defineProps({
  animal: { type: Object, required: true }
})

const fallback = demoImages[0]

const ageLabel = computed(() => {
  if (props.animal.age === null || props.animal.age === undefined || props.animal.age === '') {
    return 'Age unknown'
  }

  return `${props.animal.age} yrs`
})

const distanceLabel = computed(() => {
  const distance = Number(props.animal.distanceKm)
  if (!Number.isFinite(distance)) return ''
  if (distance < 1) return `距你 ${Math.round(distance * 1000)} m`
  return `距你 ${distance.toFixed(distance < 10 ? 1 : 0)} km`
})
</script>

<style scoped>
.distance-badge {
  position: absolute;
  right: 16px;
  top: 16px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 11px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: #245c50;
  font-size: 13px;
  font-weight: 800;
  box-shadow: 0 10px 24px rgba(15, 44, 36, 0.16);
  backdrop-filter: blur(12px);
}

.distance-line {
  width: 100%;
  margin-bottom: 8px;
  color: #1f8a70;
  font-weight: 800;
}
</style>
