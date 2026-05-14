<template>
  <section class="view page">
    <div class="section-head">
      <div>
        <h1>{{ $t('community.title') }}</h1>
        <p>{{ $t('community.description') }}</p>
      </div>
      <el-button
        v-if="auth.isLoggedIn.value"
        :icon="SquarePen"
        type="primary"
        size="large"
        @click="openEditor()"
      >
        {{ $t('community.publish') }}
      </el-button>
      <el-button v-else :icon="LogIn" size="large" @click="$router.push('/auth')">
        {{ $t('community.loginToPost') }}
      </el-button>
    </div>

    <div class="toolbar tool-panel community-toolbar">
      <el-input v-model="keyword" :placeholder="$t('community.keyword')" clearable @keyup.enter="load" />
      <el-button :icon="Search" type="primary" @click="load">{{ $t('community.filter') }}</el-button>
    </div>

    <el-skeleton v-if="loading" :rows="7" animated />
    <div v-else-if="posts.length" class="community-feed">
      <article v-for="post in posts" :key="post.id" class="community-card lift-card">
        <div class="community-card-head">
          <div>
            <h3 class="post-title-link" @click="$router.push(`/community/${post.id}`)">{{ post.title }}</h3>
            <p class="muted author-line">
              <RouterLink :to="'/users/' + post.authorId" class="author-link">
                <el-avatar :src="getFullUrl(post.authorAvatarUrl)" :size="22" style="margin-right:4px;vertical-align:middle">
                  {{ post.authorNickname?.slice(0, 1) }}
                </el-avatar>
                {{ post.authorNickname }}
              </RouterLink>
               · {{ post.authorRoleText }} · {{ formatTime(post.createdAt) }}
            </p>
          </div>
          <StatusTag :value="post.status" :text="post.statusText" :options="communityPostStatusOptions" />
        </div>
        <p class="community-card-content">{{ excerpt(post.content) }}</p>
        <div v-if="post.imageUrls?.length" class="post-card-images">
          <img v-for="url in post.imageUrls.slice(0, 4)" :key="url" :src="getFullUrl(url)" class="post-card-thumb" />
          <span v-if="post.imageUrls.length > 4" class="more-images">+{{ post.imageUrls.length - 4 }}</span>
        </div>
        <div class="community-card-foot">
          <span class="muted">{{ post.commentCount }} {{ $t('community.commentCount') }}</span>
          <div class="community-actions">
            <el-button text @click="$router.push(`/community/${post.id}`)">{{ $t('notices.readMore') }}</el-button>
            <el-button v-if="canManage(post)" text @click="openEditor(post)">{{ $t('community.edit') }}</el-button>
            <el-button v-if="canManage(post)" text type="danger" @click="removePost(post)">{{ $t('community.delete') }}</el-button>
          </div>
        </div>
      </article>
    </div>
    <EmptyState
      v-else
      :title="$t('community.noData')"
      :description="$t('community.noDataDesc')"
    />

    <div v-if="total > pageSize" style="display: flex; justify-content: center; margin-top: 24px">
      <el-pagination v-model:current-page="page" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="load" />
    </div>

    <el-dialog v-model="editorVisible" :title="editingId ? $t('community.edit') : $t('community.publish')" width="760px" append-to-body>
      <el-form ref="formRef" :model="editor" :rules="rules" label-position="top">
        <el-form-item :label="$t('community.titleField')" prop="title">
          <el-input v-model="editor.title" maxlength="120" show-word-limit :placeholder="$t('community.titlePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('community.contentField')" prop="content">
          <div style="display:flex;align-items:flex-start;gap:8px">
            <el-input
              v-model="editor.content"
              type="textarea"
              :rows="10"
              maxlength="5000"
              show-word-limit
              :placeholder="$t('community.contentPlaceholder')"
              style="flex:1"
            />
            <EmojiPicker @select="(emoji) => editor.content += emoji" />
          </div>
        </el-form-item>
        <el-form-item label="图片">
          <ImageUploader v-model="editor.imageUrls" usage="community-post" :limit="6" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editorVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button :loading="saving" :icon="Send" type="primary" @click="submitPost">
          {{ editingId ? $t('community.update') : $t('community.submit') }}
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { LogIn, Search, Send, SquarePen } from 'lucide-vue-next'
import { RouterLink } from 'vue-router'
import EmptyState from '../components/EmptyState.vue'
import EmojiPicker from '../components/EmojiPicker.vue'
import ImageUploader from '../components/ImageUploader.vue'
import StatusTag from '../components/StatusTag.vue'
import { communityApi } from '../api'
import { notifyError } from '../api/http'
import { useAuth } from '../stores/auth'
import { communityPostStatusOptions } from '../utils/status'

const auth = useAuth()
const loading = ref(false)
const saving = ref(false)
const posts = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const keyword = ref('')
const editorVisible = ref(false)
const editingId = ref(null)
const formRef = ref()

const editor = reactive({
  title: '',
  content: '',
  imageUrls: []
})

const rules = {
  title: [{ required: true, message: '请输入帖子标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入帖子内容', trigger: 'blur' }]
}

const API_BASE = window.location.origin

function getFullUrl(url) {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:')) return url
  return `${API_BASE}${url}`
}

function formatTime(value) {
  return value ? new Date(value).toLocaleString() : '-'
}

function excerpt(content) {
  if (!content) return ''
  const normalized = content.replace(/\s+/g, ' ').trim()
  return normalized.length > 220 ? `${normalized.slice(0, 220)}...` : normalized
}

function canManage(post) {
  return auth.state.user && (auth.state.user.id === post.authorId || auth.state.user.role === 'ADMIN')
}

function openEditor(post = null) {
  editingId.value = post?.id || null
  editor.title = post?.title || ''
  editor.content = post?.content || ''
  editor.imageUrls = post?.imageUrls || []
  editorVisible.value = true
}

async function load() {
  loading.value = true
  try {
    const data = await communityApi.list({
      keyword: keyword.value,
      page: page.value - 1,
      size: pageSize
    })
    posts.value = data.content || []
    total.value = data.totalElements || 0
  } catch (error) {
    notifyError(error)
    posts.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function submitPost() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (editingId.value) {
      await communityApi.update(editingId.value, editor)
      ElMessage.success('帖子已更新')
    } else {
      await communityApi.create(editor)
      ElMessage.success('帖子已发布')
    }
    editorVisible.value = false
    editingId.value = null
    editor.title = ''
    editor.content = ''
    editor.imageUrls = []
    page.value = 1
    await load()
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

async function removePost(post) {
  try {
    await ElMessageBox.confirm('删除后帖子会从社区公开列表中下架，确认继续吗？', '提示', {
      type: 'warning'
    })
    await communityApi.delete(post.id)
    ElMessage.success('帖子已下架')
    await load()
  } catch (error) {
    if (error !== 'cancel') {
      notifyError(error)
    }
  }
}

onMounted(load)
</script>
