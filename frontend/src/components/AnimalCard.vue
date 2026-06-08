<template>
  <article class="animal-card lift-card" @click="$router.push(`/animals/${animal.id}`)">
    <div class="animal-media">
      <img :src="animal.coverImageUrl || fallback" :alt="animalTypeLabel || 'animal'" />
      <div class="animal-media-overlay"></div>
      <StatusTag class="float-tag" :value="animal.status" :text="animal.statusText" :options="animalStatusOptions" />
      <div v-if="distanceLabel" class="distance-badge">
        <Navigation :size="14" />
        <span>{{ distanceLabel }}</span>
      </div>
      <div class="animal-media-note">
        <MapPin :size="14" />
        <span>{{ animal.foundRegion || t('animalCard.unknownRegion') }}</span>
      </div>
    </div>
    <div class="animal-body">
      <div class="animal-title">
        <div>
          <h3>{{ animalTypeLabel }}</h3>
          <span class="animal-subline">{{ genderLabel }} / {{ ageLabel }}</span>
        </div>
        <span class="animal-arrow">
          <ArrowUpRight :size="18" />
        </span>
      </div>
      <p>{{ animal.description || animal.healthCondition || t('animalCard.noDescription') }}</p>
      <div class="animal-footer">
        <div v-if="distanceLabel" class="meta-line distance-line">
          <Navigation :size="16" />
          <span>{{ distanceLabel }}</span>
        </div>
        <div class="meta-line">
          <HeartPulse :size="16" />
          <span>{{ animal.healthCondition || t('animalCard.defaultHealth') }}</span>
        </div>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ArrowUpRight, HeartPulse, MapPin, Navigation } from 'lucide-vue-next'
import StatusTag from './StatusTag.vue'
import { animalStatusOptions, animalTypeOptions, genderOptions, optionText, translateLabel } from '../utils/status'
import { demoImages } from '../data/demoData'

const props = defineProps({
  animal: { type: Object, required: true }
})

const { t } = useI18n()
const fallback = demoImages[0]

const animalTypeLabel = computed(() => translateLabel(props.animal.typeText) || optionText(animalTypeOptions, props.animal.type))
const genderLabel = computed(() => translateLabel(props.animal.genderText) || optionText(genderOptions, props.animal.gender))

const ageLabel = computed(() => {
  if (props.animal.age === null || props.animal.age === undefined || props.animal.age === '') {
    return t('animalCard.ageUnknown')
  }

  return t('animalCard.ageValue', { age: props.animal.age })
})

const distanceLabel = computed(() => {
  const distance = Number(props.animal.distanceKm)
  if (!Number.isFinite(distance)) return ''
  if (distance < 1) return t('animalCard.distanceMeters', { value: Math.round(distance * 1000) })
  return t('animalCard.distanceKilometers', { value: distance.toFixed(distance < 10 ? 1 : 0) })
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
