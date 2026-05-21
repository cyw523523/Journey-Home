<template>
  <el-button text size="small" :class="{ 'is-liked': liked }" @click="toggle">
    <Heart :size="15" :fill="liked ? 'var(--coral)' : 'none'" :stroke="liked ? 'var(--coral)' : undefined" />
    {{ count || '' }}
  </el-button>
</template>

<script setup>
import { ref } from 'vue'
import { Heart } from 'lucide-vue-next'
import { communityApi } from '../../api'

const props = defineProps({
  targetType: { type: String, required: true },
  targetId: { type: Number, required: true },
  initialLiked: { type: Boolean, default: false },
  initialCount: { type: Number, default: 0 }
})

const liked = ref(props.initialLiked)
const count = ref(props.initialCount)

async function toggle() {
  const prevLiked = liked.value
  const prevCount = count.value
  // Optimistic update
  liked.value = !liked.value
  count.value += liked.value ? 1 : -1
  try {
    const result = await communityApi.toggleLike({ targetType: props.targetType, targetId: props.targetId })
    // result is the new state: true = liked, false = unliked
    liked.value = result
    count.value = prevCount + (result ? 1 : -1)
  } catch {
    // Rollback on failure
    liked.value = prevLiked
    count.value = prevCount
  }
}
</script>

<style scoped>
.is-liked { color: var(--coral); }
</style>
