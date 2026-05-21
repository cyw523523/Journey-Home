<template>
  <el-button text size="small" :class="{ 'is-favorited': favorited }" @click="toggle">
    <Star :size="15" :fill="favorited ? 'var(--amber)' : 'none'" :stroke="favorited ? 'var(--amber)' : undefined" />
    {{ $t('community.post.favorite') }}
  </el-button>
</template>

<script setup>
import { ref } from 'vue'
import { Star } from 'lucide-vue-next'
import { communityApi } from '../../api'

const props = defineProps({
  postId: { type: Number, required: true },
  initialFavorited: { type: Boolean, default: false }
})

const favorited = ref(props.initialFavorited)

async function toggle() {
  favorited.value = !favorited.value
  try {
    await communityApi.toggleFavorite(props.postId)
  } catch {
    favorited.value = !favorited.value
  }
}
</script>

<style scoped>
.is-favorited { color: var(--amber); }
</style>
