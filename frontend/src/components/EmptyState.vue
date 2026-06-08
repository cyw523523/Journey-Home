<template>
  <div class="empty-state" :class="{ compact }">
    <component :is="icon" :size="34" />
    <h3>{{ displayTitle }}</h3>
    <p>{{ displayDescription }}</p>
    <slot />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { SearchX } from 'lucide-vue-next'

const props = defineProps({
  icon: { type: [Object, Function], default: () => SearchX },
  title: { type: String, default: '' },
  description: { type: String, default: '' },
  compact: { type: Boolean, default: false }
})

const { t } = useI18n()

const displayTitle = computed(() => props.title || t('common.noData'))
const displayDescription = computed(() => props.description || t('common.tryAnother'))
</script>

<style scoped>
.empty-state.compact {
  padding: 24px 16px;
}
</style>
