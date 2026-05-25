<template>
  <section class="view page">
    <div class="section-head">
      <div>
        <h1>志愿者任务</h1>
        <p>发布志愿者招募任务，将线上信息转化为线下行动，让爱心落地。</p>
      </div>
      <el-button v-if="auth.isLoggedIn.value" :icon="Plus" type="primary" size="large" @click="dialogVisible = true">发布任务</el-button>
      <el-button v-else :icon="LogIn" size="large" @click="$router.push('/auth')">登录后发布</el-button>
    </div>

    <div class="toolbar tool-panel" style="grid-template-columns: 1.5fr 1fr 1fr auto">
      <el-input v-model="filters.keyword" placeholder="关键词" clearable @keyup.enter="load" />
      <el-input v-model="filters.region" placeholder="地区" clearable @keyup.enter="load" />
      <el-select v-model="filters.status" placeholder="状态" clearable>
        <el-option v-for="item in publicTaskStatuses" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-button :icon="Search" type="primary" @click="load">筛选</el-button>
    </div>

    <el-skeleton v-if="loading" :rows="6" animated />
    <div v-else-if="tasks.length" class="grid task-grid">
      <article v-for="task in tasks" :key="task.id" class="task-card lift-card">
        <StatusTag :value="task.status" :text="task.statusText" :options="volunteerTaskStatusOptions" />
        <h3>{{ task.title }}</h3>
        <p class="muted">{{ task.description }}</p>
        <div class="task-meta-row">
          <span class="meta-line"><MapPin :size="14" /> {{ task.location }}</span>
          <span class="meta-line"><Users :size="14" /> {{ task.currentVolunteers || 0 }}/{{ task.maxVolunteers }} 人</span>
        </div>
        <div v-if="task.scheduledTime" class="meta-line"><Calendar :size="14" /> {{ formatDate(task.scheduledTime) }}</div>
        <div v-if="task.relatedRescueLocation" class="meta-line rescue-link">
          <Link2 :size="14" /> 关联救助：{{ task.relatedRescueLocation }}
        </div>
        <div style="display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-top: 8px">
          <div class="meta-line"><User :size="14" /> {{ task.publisherNickname || '-' }}</div>
          <div style="display:flex;gap:6px">
            <el-button v-if="canEdit(task)" :icon="Pencil" text size="small" @click="openEdit(task)">编辑</el-button>
            <el-button :icon="Eye" @click="openDetail(task)">详情</el-button>
            <el-button
              v-if="canApply(task)"
              :icon="UserPlus"
              type="primary"
              plain
              size="small"
              @click="openApply(task)"
            >报名</el-button>
          </div>
        </div>
      </article>
    </div>
    <EmptyState v-else title="暂无志愿者任务" description="可以发布一个志愿者任务，召集大家一起行动。" />

    <el-dialog v-model="detailVisible" title="任务详情" width="700px" append-to-body>
      <div v-if="current" class="form-shell">
        <StatusTag :value="current.status" :text="current.statusText" :options="volunteerTaskStatusOptions" />
        <h2>{{ current.title }}</h2>
        <p class="muted">{{ current.description }}</p>

        <div class="detail-info-grid">
          <div class="info-item"><MapPin :size="16" /><span><strong>地点：</strong>{{ current.location }}</span></div>
          <div class="info-item"><Users :size="16" /><span><strong>人数：</strong>{{ current.currentVolunteers || 0 }} / {{ current.maxVolunteers }} 人</span></div>
          <div v-if="current.scheduledTime" class="info-item"><Calendar :size="16" /><span><strong>时间：</strong>{{ formatDate(current.scheduledTime) }}</span></div>
          <div class="info-item"><User :size="16" /><span><strong>发布人：</strong>{{ current.publisherNickname || '-' }}</span></div>
          <div v-if="current.relatedRescueLocation" class="info-item rescue-link"><Link2 :size="16" /><span><strong>关联救助：</strong>{{ current.relatedRescueLocation }}</span></div>
        </div>

        <div class="volunteer-progress">
          <span>志愿者进度</span>
          <el-progress
            :percentage="Math.min(100, Math.round(((current.currentVolunteers || 0) / (current.maxVolunteers || 1)) * 100))"
            :stroke-width="10"
          />
        </div>

        <h4 style="margin-top: 20px; margin-bottom: 12px">报名列表</h4>
        <el-skeleton v-if="appsLoading" :rows="3" animated />
        <div v-else-if="applications.length" class="app-list">
          <div v-for="app in applications" :key="app.id" class="app-item">
            <div class="app-main">
              <strong>{{ app.volunteerNickname }}</strong>
              <span class="muted">{{ app.message || '' }}</span>
              <small class="muted">{{ formatTime(app.createdAt) }}</small>
            </div>
            <div class="app-right">
              <StatusTag :value="app.status" :text="app.statusText" :options="volunteerApplicationStatusOptions" size="small" />
              <template v-if="canManageApp(app)">
                <el-button
                  v-if="app.status === 'PENDING'"
                  :icon="Check"
                  type="success"
                  text
                  size="small"
                  @click="reviewApp(app, 'APPROVED')"
                >通过</el-button>
                <el-button
                  v-if="app.status === 'PENDING'"
                  :icon="X"
                  type="danger"
                  text
                  size="small"
                  @click="reviewApp(app, 'REJECTED')"
                >拒绝</el-button>
                <el-button
                  v-if="app.status === 'APPROVED'"
                  :icon="CheckCircle"
                  type="primary"
                  text
                  size="small"
                  @click="completeApp(app)"
                >确认完成</el-button>
              </template>
            </div>
          </div>
        </div>
        <EmptyState v-else title="暂无报名" description="等待志愿者报名参与..." :compact="true" />

        <div style="display:flex;gap:8px;margin-top:20px">
          <template v-if="canEdit(current)">
            <el-button :icon="Pencil" type="primary" @click="detailVisible = false; openEdit(current)">编辑</el-button>
            <el-button :icon="RefreshCw" @click="openStatus(current)">更新状态</el-button>
            <el-button :icon="Archive" type="danger" @click="offlineTask(current)">下架</el-button>
          </template>
          <el-button v-else-if="canApply(current)" :icon="UserPlus" type="primary" @click="detailVisible = false; openApply(current)">我要报名</el-button>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="dialogVisible" title="发布志愿者任务" width="720px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="任务标题" prop="title">
          <el-input v-model="form.title" placeholder="如：周六去某小区抓流浪猫绝育" />
        </el-form-item>
        <el-form-item label="任务描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="详细描述任务内容、注意事项、集合地点等" />
        </el-form-item>
        <el-form-item label="活动地点" prop="location">
          <el-input v-model="form.location" placeholder="具体地址或区域" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="需要人数" prop="maxVolunteers">
              <el-input-number v-model="form.maxVolunteers" :min="1" :max="100" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计划时间">
              <el-date-picker v-model="form.scheduledTime" type="datetime" placeholder="选择时间" style="width: 100%" value-format="YYYY-MM-DDTHH:mm:ss" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="关联救助（可选）">
          <el-input v-model="form.relatedRescueId" placeholder="关联的救助信息ID（选填）" />
        </el-form-item>
        <el-form-item label="封面图片">
          <ImageUploader v-model="form.imageUrls" usage="task" :limit="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button :loading="saving" :icon="Send" type="primary" @click="submit">提交发布</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editVisible" title="编辑志愿者任务" width="720px" append-to-body>
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-position="top">
        <el-form-item label="任务标题" prop="title">
          <el-input v-model="editForm.title" />
        </el-form-item>
        <el-form-item label="任务描述" prop="description">
          <el-input v-model="editForm.description" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="活动地点" prop="location">
          <el-input v-model="editForm.location" />
        </el-form-item>
        <el-form-item label="需要人数" prop="maxVolunteers">
          <el-input-number v-model="editForm.maxVolunteers" :min="1" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="计划时间">
          <el-date-picker v-model="editForm.scheduledTime" type="datetime" style="width: 100%" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
        <el-form-item label="封面图片">
          <ImageUploader v-model="editForm.imageUrls" usage="task" :limit="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button :loading="saving" type="primary" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="statusVisible" title="更新状态" width="460px" append-to-body>
      <el-form label-position="top">
        <el-form-item label="新状态">
          <el-select v-model="newStatus" style="width: 100%">
            <el-option v-for="item in editableStatuses" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusVisible = false">取消</el-button>
        <el-button :loading="saving" type="primary" @click="saveStatus">更新</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="applyVisible" title="报名参加" width="500px" append-to-body>
      <div v-if="current" class="form-shell">
        <h3>{{ current.title }}</h3>
        <p class="muted">还需 {{ (current.maxVolunteers || 0) - (current.currentVolunteers || 0) }} 名志愿者</p>
        <el-form ref="applyFormRef" :model="applyForm" :rules="applyRules" label-position="top">
          <el-form-item label="自我介绍/留言（可选）">
            <el-input v-model="applyForm.message" type="textarea" :rows="3" placeholder="简单介绍一下自己，或说明你能做什么..." />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button :loading="saving" :icon="UserPlus" type="primary" @click="submitApply">确认报名</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Archive, Calendar, Check, CheckCircle, Eye, Link2, LogIn, MapPin, Pencil, Plus, RefreshCw, Search, Send, User, UserPlus, Users } from 'lucide-vue-next'
import EmptyState from '../components/EmptyState.vue'
import ImageUploader from '../components/ImageUploader.vue'
import StatusTag from '../components/StatusTag.vue'
import { volunteerTaskApi } from '../api'
import { notifyError } from '../api/http'
import {
  volunteerTaskStatusOptions,
  volunteerApplicationStatusOptions
} from '../utils/status'
import { useAuth } from '../stores/auth'

const auth = useAuth()
const loading = ref(false)
const saving = ref(false)
const appsLoading = ref(false)
const detailVisible = ref(false)
const dialogVisible = ref(false)
const editVisible = ref(false)
const statusVisible = ref(false)
const applyVisible = ref(false)
const current = ref(null)
const tasks = ref([])
const applications = ref([])
const formRef = ref()
const editFormRef = ref()
const applyFormRef = ref()
const newStatus = ref('')
const publicTaskStatuses = volunteerTaskStatusOptions.filter((item) => ['RECRUITING', 'IN_PROGRESS', 'COMPLETED'].includes(item.value))
const editableStatuses = volunteerTaskStatusOptions.filter((item) => item.value !== 'PENDING_REVIEW' && item.value !== 'CANCELLED')
const filters = reactive({ keyword: '', region: '', status: '' })
const form = reactive({ title: '', description: '', location: '', maxVolunteers: 3, scheduledTime: null, imageUrls: [], relatedRescueId: null })
const editForm = reactive({ id: null, title: '', description: '', location: '', maxVolunteers: 0, scheduledTime: null, imageUrls: [] })
const applyForm = reactive({ message: '' })
const rules = {
  title: [{ required: true, message: '请输入任务标题', trigger: 'blur' }],
  description: [{ required: true, message: '请输入任务描述', trigger: 'blur' }],
  location: [{ required: true, message: '请输入活动地点', trigger: 'blur' }],
  maxVolunteers: [{ required: true, message: '请填写需要人数', trigger: 'blur' }]
}
const editRules = { ...rules }
const applyRules = {}

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN')
}

function formatTime(value) {
  return value ? new Date(value).toLocaleString('zh-CN') : '-'
}

function canEdit(record) {
  if (!auth.state.user) return false
  return auth.state.user.id === record.publisherId || auth.isAdmin.value
}

function canApply(task) {
  if (!auth.isLoggedIn.value) return false
  if (!task) return false
  return task.status === 'RECRUITING' && (task.currentVolunteers || 0) < (task.maxVolunteers || 0)
}

function canManageApp(app) {
  if (!auth.state.user || !current.value) return false
  return auth.state.user.id === current.value.publisherId
}

async function load() {
  loading.value = true
  try {
    const data = await volunteerTaskApi.list({ ...filters, page: 0, size: 12 })
    tasks.value = data.content || []
  } catch (error) {
    notifyError(error)
  } finally {
    loading.value = false
  }
}

async function loadApplications(id) {
  appsLoading.value = true
  try {
    applications.value = await volunteerTaskApi.applications(id, { page: 0, size: 50 })
  } catch {
    applications.value = []
  } finally {
    appsLoading.value = false
  }
}

async function openDetail(task) {
  try {
    current.value = await volunteerTaskApi.detail(task.id)
  } catch {
    current.value = task
  }
  detailVisible.value = true
  await loadApplications(current.value?.id || task.id)
}

function openEdit(task) {
  Object.assign(editForm, {
    id: task.id,
    title: task.title || '',
    description: task.description || '',
    location: task.location || '',
    maxVolunteers: task.maxVolunteers || 1,
    scheduledTime: task.scheduledTime || null,
    imageUrls: task.imageUrl ? [task.imageUrl] : []
  })
  editVisible.value = true
}

function openStatus(task) {
  newStatus.value = task.status
  statusVisible.value = true
}

async function saveEdit() {
  await editFormRef.value.validate()
  saving.value = true
  try {
    const payload = { ...editForm, imageUrl: editForm.imageUrls?.[0] || null }
    await volunteerTaskApi.update(editForm.id, payload)
    ElMessage.success('任务已更新')
    editVisible.value = false
    load()
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

async function saveStatus() {
  saving.value = true
  try {
    await volunteerTaskApi.updateStatus(current.value.id, { status: newStatus.value })
    ElMessage.success('状态已更新')
    statusVisible.value = false
    load()
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

async function offlineTask(task) {
  try {
    await ElMessageBox.confirm('下架后该任务将从公开列表中移除，确认继续吗？', '提示', { type: 'warning' })
    await volunteerTaskApi.offline(task.id)
    ElMessage.success('已下架')
    detailVisible.value = false
    load()
  } catch (error) {
    if (error !== 'cancel') notifyError(error)
  }
}

function openApply(task) {
  current.value = task
  applyForm.message = ''
  applyVisible.value = true
}

async function submitApply() {
  saving.value = true
  try {
    await volunteerTaskApi.apply(current.value.id, applyForm)
    ElMessage.success('报名成功，请等待审核')
    applyVisible.value = false
    load()
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

async function reviewApp(app, status) {
  const actionText = status === 'APPROVED' ? '通过' : '拒绝'
  try {
    await ElMessageBox.confirm(`确认${actionText} ${app.volunteerNickname} 的报名？`, '提示', { type: status === 'APPROVED' ? 'success' : 'warning' })
    await volunteerTaskApi.reviewApplication(app.id, { status, reviewComment: '' })
    ElMessage.success(`已${actionText}`)
    await loadApplications(current.value.id)
    load()
  } catch (error) {
    if (error !== 'cancel') notifyError(error)
  }
}

async function completeApp(app) {
  try {
    await ElMessageBox.confirm(`确认 ${app.volunteerNickname} 已完成任务？`, '提示', { type: 'info' })
    await volunteerTaskApi.completeApplication(app.id)
    ElMessage.success('已确认为完成')
    await loadApplications(current.value.id)
    load()
  } catch (error) {
    if (error !== 'cancel') notifyError(error)
  }
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { ...form, imageUrl: form.imageUrls?.[0] || null }
    if (payload.relatedRescueId) {
      payload.relatedRescueId = Number(payload.relatedRescueId) || null
    }
    await volunteerTaskApi.create(payload)
    ElMessage.success('发布成功，等待管理员审核')
    Object.assign(form, { title: '', description: '', location: '', maxVolunteers: 3, scheduledTime: null, imageUrls: [], relatedRescueId: null })
    dialogVisible.value = false
    load()
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await load()
})
</script>

<style scoped>
.task-grid {
  grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
}
.task-card {
  padding: 20px;
}
.task-meta-row {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}
.rescue-link {
  color: var(--blue);
  font-size: 13px;
}
.detail-info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 24px;
  padding: 12px 0;
  border-top: 1px solid var(--line);
}
.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}
.info-item svg {
  color: var(--muted);
  flex-shrink: 0;
}
.volunteer-progress {
  margin-top: 12px;
}
.volunteer-progress span {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  color: var(--muted);
}
.app-list {
  display: grid;
  gap: 8px;
}
.app-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  background: var(--panel-soft);
  border-radius: 10px;
}
.app-main {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  min-width: 0;
}
.app-right {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}
</style>
