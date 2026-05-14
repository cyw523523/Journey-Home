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
              <RouterLink :to="'/users/' + detail.post.authorId" class="author-link">
                <el-avatar :src="getFullUrl(detail.post.authorAvatarUrl)" :size="22" style="margin-right:4px;vertical-align:middle">
                  {{ detail.post.authorNickname?.slice(0, 1) }}
                </el-avatar>
                {{ detail.post.authorNickname }}
              </RouterLink>
              · {{ detail.post.authorRoleText }} · {{ formatTime(detail.post.createdAt) }}
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

        <div class="community-comment-block">
          <div class="section-head" style="margin-top: 0">
            <div>
              <h2>{{ $t('community.comments') }}</h2>
              <p>{{ detail.comments.length }} {{ $t('community.commentCount') }}</p>
            </div>
          </div>

          <div v-if="auth.isLoggedIn.value" class="community-comment-editor">
            <el-tag v-if="replyingTo" closable type="info" @close="cancelReply" style="margin-bottom:8px">
              回复 @{{ replyingTo.nickname }}
            </el-tag>
            <el-input
              v-model="commentForm.content"
              type="textarea"
              :rows="4"
              maxlength="2000"
              show-word-limit
              :placeholder="$t('community.commentPlaceholder')"
            />
            <div style="display:flex;align-items:center;gap:8px;margin-top:8px">
              <ImageUploader v-model="commentForm.imageUrls" usage="community-comment" :limit="3" />
              <EmojiPicker @select="(emoji) => commentForm.content += emoji" />
            </div>
            <div style="display: flex; justify-content: flex-end; margin-top: 12px">
              <el-button :loading="commenting" type="primary" @click="submitComment">{{ $t('community.sendComment') }}</el-button>
            </div>
          </div>
          <el-alert v-else type="info" :closable="false" :title="$t('community.loginToComment')" style="margin-bottom: 16px" />

          <div v-if="detail.comments.length" class="community-comment-list">
            <article
              v-for="comment in commentsWithDepth"
              :key="comment.id"
              class="community-comment-card"
              :style="{ marginLeft: comment.depth * 28 + 'px' }"
            >
              <div class="community-comment-head">
                <div style="display:flex;align-items:center;gap:8px">
                  <RouterLink :to="'/users/' + comment.authorId" class="author-link">
                    <el-avatar :src="getFullUrl(comment.authorAvatarUrl)" :size="24">
                      {{ comment.authorNickname?.slice(0, 1) }}
                    </el-avatar>
                  </RouterLink>
                  <div>
                    <div>
                      <RouterLink :to="'/users/' + comment.authorId" class="author-link">
                        <strong>{{ comment.authorNickname }}</strong>
                      </RouterLink>
                      <span v-if="comment.replyToAuthorNickname" class="muted" style="font-size:12px">
                        回复 <RouterLink :to="'/users/' + comment.replyToAuthorId" style="color:var(--primary)">{{ comment.replyToAuthorNickname }}</RouterLink>
                      </span>
                    </div>
                    <p class="muted" style="font-size:12px">{{ comment.authorRoleText }} · {{ formatTime(comment.createdAt) }}</p>
                  </div>
                </div>
                <div style="display:flex;gap:4px">
                  <el-button v-if="auth.isLoggedIn.value" text size="small" @click="replyTo(comment)">回复</el-button>
                  <el-button
                    v-if="auth.isLoggedIn.value && auth.state.user?.id !== comment.authorId"
                    text
                    size="small"
                    type="danger"
                    @click="openReport('COMMUNITY_COMMENT', comment.id)"
                  >
                    举报
                  </el-button>
                  <el-button v-if="canManageComment(comment)" text size="small" type="danger" @click="removeComment(comment.id)">
                    {{ $t('community.delete') }}
                  </el-button>
                </div>
              </div>
              <div class="community-comment-content">{{ comment.content }}</div>
              <div v-if="comment.imageUrls?.length" class="comment-images-row">
                <img v-for="url in comment.imageUrls" :key="url" :src="getFullUrl(url)" class="comment-thumb" />
              </div>
            </article>
          </div>
          <EmptyState
            v-else
            :title="$t('community.comments')"
            :description="$t('community.emptyComments')"
          />
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, MessagesSquare } from 'lucide-vue-next'
import { RouterLink, useRoute } from 'vue-router'
import EmptyState from '../components/EmptyState.vue'
import EmojiPicker from '../components/EmojiPicker.vue'
import ImageUploader from '../components/ImageUploader.vue'
import ReportDialog from '../components/ReportDialog.vue'
import StatusTag from '../components/StatusTag.vue'
import { communityApi } from '../api'
import { notifyError } from '../api/http'
import { useAuth } from '../stores/auth'
import { communityPostStatusOptions } from '../utils/status'

const route = useRoute()
const auth = useAuth()
const loading = ref(false)
const commenting = ref(false)
const detail = ref(null)
const replyingTo = ref(null)
const reportVisible = ref(false)
const reportTarget = ref({ type: 'COMMUNITY_POST', id: 0 })

const commentForm = reactive({
  content: '',
  imageUrls: [],
  parentCommentId: null
})

const commentsWithDepth = computed(() => {
  const comments = detail.value?.comments || []
  if (!comments.length) return []
  const map = {}
  const depths = {}
  for (const c of comments) map[c.id] = c
  function getDepth(comment) {
    if (depths[comment.id] !== undefined) return depths[comment.id]
    if (!comment.parentCommentId || !map[comment.parentCommentId]) {
      depths[comment.id] = 0
    } else {
      depths[comment.id] = getDepth(map[comment.parentCommentId]) + 1
    }
    return depths[comment.id]
  }
  return comments.map((c) => ({ ...c, depth: Math.min(getDepth(c), 3) }))
})

const API_BASE = window.location.origin

function getFullUrl(url) {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:')) return url
  return `${API_BASE}${url}`
}

function formatTime(value) {
  return value ? new Date(value).toLocaleString() : '-'
}

function canManageComment(comment) {
  return auth.state.user && (auth.state.user.id === comment.authorId || auth.state.user.role === 'ADMIN')
}

function replyTo(comment) {
  replyingTo.value = { id: comment.id, nickname: comment.authorNickname }
  commentForm.parentCommentId = comment.id
  commentForm.content = `@${comment.authorNickname} `
}

function cancelReply() {
  replyingTo.value = null
  commentForm.content = ''
  commentForm.parentCommentId = null
  commentForm.imageUrls = []
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

async function submitComment() {
  if (!commentForm.content.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  commenting.value = true
  try {
    await communityApi.createComment(route.params.id, {
      content: commentForm.content,
      parentCommentId: commentForm.parentCommentId,
      imageUrls: commentForm.imageUrls
    })
    commentForm.content = ''
    commentForm.imageUrls = []
    commentForm.parentCommentId = null
    replyingTo.value = null
    ElMessage.success('评论已提交')
    await load()
  } catch (error) {
    notifyError(error)
  } finally {
    commenting.value = false
  }
}

async function removeComment(id) {
  try {
    await ElMessageBox.confirm('确认删除这条评论吗？', '提示', { type: 'warning' })
    await communityApi.deleteComment(id)
    ElMessage.success('评论已删除')
    await load()
  } catch (error) {
    if (error !== 'cancel') {
      notifyError(error)
    }
  }
}

onMounted(load)
</script>
