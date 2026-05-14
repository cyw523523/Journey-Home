<template>
  <article class="animal-card lift-card" @click="$router.push(`/animals/${animal.id}`)">
    <div class="animal-media">
      <img :src="animal.coverImageUrl || fallback" :alt="animal.typeText || 'animal'" />
      <div class="animal-media-overlay"></div>
      <StatusTag class="float-tag" :value="animal.status" :text="animal.statusText" :options="animalStatusOptions" />
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
import { ArrowUpRight, HeartPulse, MapPin } from 'lucide-vue-next'
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
</script>
