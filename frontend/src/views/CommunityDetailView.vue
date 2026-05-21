<template>
  <section class="view-narrow page">
    <el-skeleton v-if="loading" :rows="8" animated />
    <template v-else-if="detail">
      <article class="surface form-shell community-detail-shell">
        <div class="community-detail-head">
          <div>
            <p class="eyebrow"><MessagesSquare :size="16" /> {{ $t('community.title') }}</p>
            <h1>{{ detail.post.title }}</h1>
            <p class="muted author-line">
              <RouterLink :to="avatarTarget(detail.post.authorId)" class="chat-avatar-link">
                <el-avatar :src="getFullUrl(detail.post.authorAvatarUrl)" :size="22">
                  {{ detail.post.authorNickname?.slice(0, 1) }}
                </el-avatar>
              </RouterLink>
              <RouterLink :to="`/users/${detail.post.authorId}`" class="author-link">{{ detail.post.authorNickname }}</RouterLink>
              <span> · {{ detail.post.authorRoleText }} · {{ formatTime(detail.post.createdAt) }}</span>
            </p>
          </div>
          <div style="display:flex;align-items:center;gap:8px">
            <StatusTag :value="detail.post.status" :text="detail.post.statusText" :options="communityPostStatusOptions" />
            <el-button
              v-if="auth.isLoggedIn.value && auth.state.user?.id !== detail.post.authorId"
              text
              type="danger"
              @click="openReport('COMMUNITY_POST', detail.post.id)"
            >
              举报
            </el-button>
          </div>
        </div>

        <div class="community-detail-content">{{ detail.post.content }}</div>
        <div v-if="detail.post.imageUrls?.length" class="community-detail-images">
          <img v-for="url in detail.post.imageUrls" :key="url" :src="getFullUrl(url)" class="community-detail-thumb" />
        </div>

        <!-- Post actions bar -->
        <div class="post-actions-bar">
          <LikeButton targetType="POST" :targetId="detail.post.id" :initialLiked="detail.post.liked" :initialCount="detail.post.likeCount || 0" />
          <FavoriteButton :postId="detail.post.id" :initialFavorited="detail.post.favorited" />
          <span class="muted" style="font-size:13px">阅读 {{ detail.post.viewCount || 0 }}</span>
          <FollowUserButton v-if="detail.post.authorId && detail.post.authorId !== auth.state.user?.id" :userId="detail.post.authorId" :initialFollowed="detail.post.authorFollowed" style="margin-left:auto" />
        </div>

        <!-- Comment editor -->
        <CommentEditor v-if="auth.isLoggedIn.value" :placeholder="replyTarget ? '回复 ' + replyTarget.replyToNickname + '...' : '写下你的评论...'" @submit="handleCommentSubmit" />
        <el-button v-if="replyTarget" text size="small" @click="replyTarget = null" style="margin-bottom:8px">取消回复</el-button>

        <!-- Floor list -->
        <div class="community-comment-block">
          <div class="section-head" style="margin-top: 0">
            <div>
              <h2>{{ $t('community.comments') }}</h2>
              <p>{{ detail.post.commentCount || 0 }} {{ $t('community.commentCount') }}</p>
            </div>
          </div>
          <FloorList ref="floorListRef" :postId="detail.post.id" :totalComments="detail.post.commentCount || 0"
            @reply="handleFloorReply" @reply-to="handleSubReply" />
        </div>

        <div style="margin-top: 26px">
          <el-button :icon="ArrowLeft" @click="$router.push('/community')">{{ $t('community.backToList') }}</el-button>
        </div>
      </article>
    </template>
    <EmptyState
      v-else
      :title="$t('community.notFound')"
      :description="$t('community.notFoundDesc')"
    />
    <ReportDialog
      v-model="reportVisible"
      :target-type="reportTarget.type"
      :target-id="reportTarget.id || 0"
      @submitted="load"
    />
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ArrowLeft, MessagesSquare } from 'lucide-vue-next'
import { RouterLink, useRoute } from 'vue-router'
import EmptyState from '../components/EmptyState.vue'
import ReportDialog from '../components/ReportDialog.vue'
import StatusTag from '../components/StatusTag.vue'
import CommentEditor from '../components/community/CommentEditor.vue'
import FloorList from '../components/community/FloorList.vue'
import LikeButton from '../components/community/LikeButton.vue'
import FavoriteButton from '../components/community/FavoriteButton.vue'
import FollowUserButton from '../components/community/FollowUserButton.vue'
import { communityApi } from '../api'
import { notifyError } from '../api/http'
import { useAuth } from '../stores/auth'
import { communityPostStatusOptions } from '../utils/status'

const route = useRoute()
const auth = useAuth()
const loading = ref(false)
const detail = ref(null)
const reportVisible = ref(false)
const reportTarget = ref({ type: 'COMMUNITY_POST', id: 0 })
const replyTarget = ref(null)
const floorListRef = ref(null)

const API_BASE = window.location.origin

function getFullUrl(url) {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:')) return url
  return `${API_BASE}${url}`
}

function formatTime(value) {
  return value ? new Date(value).toLocaleString() : '-'
}

function avatarTarget(userId) {
  if (auth.state.user?.id === userId) {
    return '/profile'
  }
  return { path: '/messages', query: { userId: String(userId) } }
}

function openReport(type, id) {
  reportTarget.value = { type, id }
  reportVisible.value = true
}

async function load() {
  loading.value = true
  try {
    detail.value = await communityApi.detail(route.params.id)
  } catch (error) {
    notifyError(error)
    detail.value = null
  } finally {
    loading.value = false
  }
}

function handleFloorReply({ floor, replyToId }) {
  replyTarget.value = { floorId: floor.id, replyToId, replyToNickname: floor.authorNickname }
}

function handleSubReply({ floor, replyToId, replyToNickname }) {
  replyTarget.value = { floorId: floor.id, replyToId, replyToNickname }
}

async function handleCommentSubmit({ content, imageUrls }) {
  try {
    if (replyTarget.value) {
      await communityApi.createReply(replyTarget.value.floorId, {
        replyToCommentId: replyTarget.value.replyToId,
        content,
        imageUrls
      })
    } else {
      await communityApi.createFloor(detail.value.post.id, { content, imageUrls })
    }
    replyTarget.value = null
    await load()
    if (floorListRef.value) { floorListRef.value.load() }
  } catch (e) {
    // error handled by interceptor
  }
}

onMounted(load)
</script>
