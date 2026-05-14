<template>
  <section class="view-narrow page">
    <el-skeleton v-if="loading" :rows="8" animated />
    <template v-else-if="detail">
      <article class="surface form-shell community-detail-shell">
        <div class="community-detail-head">
          <div>
            <p class="eyebrow"><MessagesSquare :size="16" /> {{ $t('community.title') }}</p>
            <h1>{{ detail.post.title }}</h1>
            <p class="muted">
              {{ $t('community.author') }}：{{ detail.post.authorNickname }} · {{ detail.post.authorRoleText }} · {{ formatTime(detail.post.createdAt) }}
            </p>
          </div>
          <StatusTag :value="detail.post.status" :text="detail.post.statusText" :options="communityPostStatusOptions" />
        </div>

        <div class="community-detail-content">{{ detail.post.content }}</div>

        <div class="community-comment-block">
          <div class="section-head" style="margin-top: 0">
            <div>
              <h2>{{ $t('community.comments') }}</h2>
              <p>{{ detail.comments.length }} {{ $t('community.commentCount') }}</p>
            </div>
          </div>

          <div v-if="auth.isLoggedIn.value" class="community-comment-editor">
            <el-input
              v-model="commentForm.content"
              type="textarea"
              :rows="4"
              maxlength="2000"
              show-word-limit
              :placeholder="$t('community.commentPlaceholder')"
            />
            <div style="display: flex; justify-content: flex-end; margin-top: 12px">
              <el-button :loading="commenting" type="primary" @click="submitComment">{{ $t('community.sendComment') }}</el-button>
            </div>
          </div>
          <el-alert v-else type="info" :closable="false" :title="$t('community.loginToComment')" style="margin-bottom: 16px" />

          <div v-if="detail.comments.length" class="community-comment-list">
            <article v-for="comment in detail.comments" :key="comment.id" class="community-comment-card">
              <div class="community-comment-head">
                <div>
                  <strong>{{ comment.authorNickname }}</strong>
                  <p class="muted">{{ comment.authorRoleText }} · {{ formatTime(comment.createdAt) }}</p>
                </div>
                <el-button v-if="canManageComment(comment)" text type="danger" @click="removeComment(comment.id)">
                  {{ $t('community.delete') }}
                </el-button>
              </div>
              <div class="community-comment-content">{{ comment.content }}</div>
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
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, MessagesSquare } from 'lucide-vue-next'
import { useRoute } from 'vue-router'
import EmptyState from '../components/EmptyState.vue'
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

const commentForm = reactive({
  content: ''
})

function formatTime(value) {
  return value ? new Date(value).toLocaleString() : '-'
}

function canManageComment(comment) {
  return auth.state.user && (auth.state.user.id === comment.authorId || auth.state.user.role === 'ADMIN')
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
    await communityApi.createComment(route.params.id, commentForm)
    commentForm.content = ''
    ElMessage.success('评论已发布')
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
