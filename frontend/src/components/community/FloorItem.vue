<template>
  <div :id="`L${floor.floorNo}`" class="floor-item" :class="{ highlight: showHighlight }">
    <div class="floor-head">
      <span class="floor-no">{{ floor.floorNo }}L</span>
      <RouterLink :to="`/users/${floor.authorId}`" class="floor-author">{{ floor.authorNickname }}</RouterLink>
      <span v-if="floor.isPostAuthor" class="floor-op-badge">楼主</span>
      <span class="muted floor-time">{{ formatTime(floor.createdAt) }}</span>
    </div>
    <p class="floor-content">{{ floor.content }}</p>
    <div v-if="floor.imageUrls?.length" class="floor-images">
      <img v-for="url in floor.imageUrls" :key="url" :src="getFullUrl(url)" class="floor-thumb" />
    </div>
    <div class="floor-actions">
      <LikeButton targetType="COMMENT" :targetId="floor.id" :initialLiked="floor.liked" :initialCount="floor.likeCount" />
      <el-button text size="small" @click="$emit('reply', floor)">回复</el-button>
    </div>

    <div v-if="showTopReplies?.length" class="sub-replies">
      <ReplyItem v-for="r in showTopReplies" :key="r.id" :reply="r" @reply-to="(reply) => $emit('reply-to', floor, reply)" />
      <el-button v-if="floor.replyCount > 3" text size="small" @click="$emit('expand-replies', floor)">
        展开剩余 {{ floor.replyCount - 3 }} 条回复
      </el-button>
    </div>

    <div v-if="floor._allReplies?.length" class="sub-replies expanded">
      <ReplyItem v-for="r in floor._allReplies" :key="r.id" :reply="r" @reply-to="(reply) => $emit('reply-to', floor, reply)" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import LikeButton from './LikeButton.vue'
import ReplyItem from './ReplyItem.vue'

const props = defineProps({ floor: Object, highlightFor: Number })
defineEmits(['reply', 'reply-to', 'expand-replies'])

const showHighlight = ref(false)
const API_BASE = window.location.origin

const showTopReplies = computed(() => {
  if (props.floor._allReplies?.length) return null
  return props.floor.topReplies?.length ? props.floor.topReplies : null
})

function getFullUrl(url) {
  if (!url) return ''
  if (url.startsWith('http') || url.startsWith('data:')) return url
  return API_BASE + url
}

function formatTime(v) { return v ? new Date(v).toLocaleString() : '-' }

onMounted(() => {
  if (props.highlightFor && props.highlightFor > 0) {
    showHighlight.value = true
    setTimeout(() => { showHighlight.value = false }, 1500)
  }
})
</script>

<style scoped>
.floor-item { padding: 16px 0; border-bottom: 1px solid var(--line); }
.floor-item.highlight { background: rgba(31,138,112,0.08); }
.floor-head { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.floor-no { font-weight: 700; color: var(--primary); font-size: 13px; }
.floor-author { font-weight: 600; color: var(--ink); text-decoration: none; }
.floor-op-badge { font-size: 11px; background: var(--primary); color: #fff; padding: 1px 6px; border-radius: 3px; }
.floor-time { font-size: 12px; }
.floor-content { line-height: 1.7; white-space: pre-wrap; margin: 0 0 8px; }
.floor-images { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 8px; }
.floor-thumb { max-width: 120px; max-height: 120px; border-radius: 6px; object-fit: cover; }
.floor-actions { display: flex; gap: 8px; }
.sub-replies { margin-top: 12px; padding-left: 24px; border-left: 2px solid var(--primary); }
</style>
