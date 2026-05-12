<template>
  <div class="empty-state">
    <component :is="icon" :size="34" />
    <h3>{{ title }}</h3>
    <p>{{ description }}</p>
    <slot />
  </div>
</template>

<script setup>
import { SearchX } from 'lucide-vue-next'

defineProps({
  icon: { type: [Object, Function], default: SearchX },
  title: { type: String, default: '暂无数据' },
  description: { type: String, default: '换个条件试试，或稍后再来看看。' }
})
</script>
