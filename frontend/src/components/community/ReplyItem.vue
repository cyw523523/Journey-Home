<template>
  <div class="reply-item">
    <RouterLink :to="`/users/${reply.authorId}`" class="reply-author">{{ reply.authorNickname }}</RouterLink>
    <span v-if="reply.replyToUserId" class="reply-to">
      回复 <RouterLink :to="`/users/${reply.replyToUserId}`">@{{ reply.replyToUserNickname }}</RouterLink>
    </span>
    <span class="reply-colon">: </span>
    <span class="reply-content">{{ reply.content }}</span>
    <div class="reply-meta">
      <span class="muted">{{ formatTime(reply.createdAt) }}</span>
      <LikeButton targetType="COMMENT" :targetId="reply.id" :initialLiked="reply.liked" :initialCount="reply.likeCount" />
      <el-button text size="small" @click="$emit('reply-to', reply)">回复</el-button>
    </div>
  </div>
</template>

<script setup>
import { RouterLink } from 'vue-router'
import LikeButton from './LikeButton.vue'

defineProps({ reply: Object })
defineEmits(['reply-to'])

function formatTime(v) { return v ? new Date(v).toLocaleString() : '-' }
</script>

<style scoped>
.reply-item { padding: 8px 0; font-size: 14px; }
.reply-item + .reply-item { border-top: 1px solid var(--line); }
.reply-author { font-weight: 600; color: var(--ink); text-decoration: none; }
.reply-to { color: var(--muted); font-size: 13px; }
.reply-colon { color: var(--muted); }
.reply-content { white-space: pre-wrap; }
.reply-meta { display: flex; align-items: center; gap: 8px; margin-top: 4px; font-size: 12px; }
</style>
