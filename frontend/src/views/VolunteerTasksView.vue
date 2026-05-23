<template>
  <section class="view page">
    <div class="section-head">
      <div>
        <h1>{{ t('volunteerTasks.title') }}</h1>
        <p>{{ t('volunteerTasks.subtitle') }}</p>
      </div>
      <el-button v-if="auth.isLoggedIn.value" :icon="Plus" type="primary" size="large" @click="dialogVisible = true">{{ t('volunteerTasks.publishTask') }}</el-button>
      <el-button v-else :icon="LogIn" size="large" @click="$router.push('/auth')">{{ t('volunteerTasks.loginToPublish') }}</el-button>
    </div>

    <div class="toolbar tool-panel" style="grid-template-columns: 1.5fr 1fr 1fr auto">
      <el-input v-model="filters.keyword" :placeholder="t('volunteerTasks.placeholderKeyword')" clearable @keyup.enter="load" />
      <el-input v-model="filters.region" :placeholder="t('volunteerTasks.placeholderRegion')" clearable @keyup.enter="load" />
      <el-select v-model="filters.status" :placeholder="t('volunteerTasks.placeholderStatus')" clearable>
        <el-option v-for="item in publicTaskStatuses" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-button :icon="Search" type="primary" @click="load">{{ t('volunteerTasks.filter') }}</el-button>
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
          <Link2 :size="14" /> {{ t('volunteerTasks.relatedRescue') }}{{ task.relatedRescueLocation }}
        </div>
        <div style="display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-top: 8px">
          <div class="meta-line"><User :size="14" /> {{ task.publisherNickname || '-' }}</div>
          <div style="display:flex;gap:6px">
            <el-button v-if="canEdit(task)" :icon="Pencil" text size="small" @click="openEdit(task)">{{ t('volunteerTasks.edit') }}</el-button>
            <el-button :icon="Eye" @click="openDetail(task)">{{ t('volunteerTasks.detail') }}</el-button>
            <el-button
              v-if="canApply(task)"
              :icon="UserPlus"
              type="primary"
              plain
              size="small"
              @click="openApply(task)"
            >{{ t('volunteerTasks.apply') }}</el-button>
          </div>
        </div>
      </article>
    </div>
    <EmptyState v-else :title="t('volunteerTasks.emptyTitle')" :description="t('volunteerTasks.emptyDescription')" />

    <el-dialog v-model="detailVisible" :title="t('volunteerTasks.taskDetail')" width="700px" append-to-body>
      <div v-if="current" class="form-shell">
        <StatusTag :value="current.status" :text="current.statusText" :options="volunteerTaskStatusOptions" />
        <h2>{{ current.title }}</h2>
        <p class="muted">{{ current.description }}</p>

        <div class="detail-info-grid">
          <div class="info-item"><MapPin :size="16" /><span><strong>{{ t('volunteerTasks.locationLabel') }}</strong>{{ current.location }}</span></div>
          <div class="info-item"><Users :size="16" /><span><strong>{{ t('volunteerTasks.peopleCountLabel') }}</strong>{{ current.currentVolunteers || 0 }} / {{ current.maxVolunteers }} 人</span></div>
          <div v-if="current.scheduledTime" class="info-item"><Calendar :size="16" /><span><strong>{{ t('volunteerTasks.timeLabel') }}</strong>{{ formatDate(current.scheduledTime) }}</span></div>
          <div class="info-item"><User :size="16" /><span><strong>{{ t('volunteerTasks.publisherLabel') }}</strong>{{ current.publisherNickname || '-' }}</span></div>
          <div v-if="current.relatedRescueLocation" class="info-item rescue-link"><Link2 :size="16" /><span><strong>{{ t('volunteerTasks.relatedRescueLabel') }}</strong>{{ current.relatedRescueLocation }}</span></div>
        </div>

        <div class="volunteer-progress">
          <span>{{ t('volunteerTasks.volunteerProgress') }}</span>
          <el-progress
            :percentage="Math.min(100, Math.round(((current.currentVolunteers || 0) / (current.maxVolunteers || 1)) * 100))"
            :stroke-width="10"
          />
        </div>

        <h4 style="margin-top: 20px; margin-bottom: 12px">{{ t('volunteerTasks.applicationList') }}</h4>
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
                >{{ t('volunteerTasks.approve') }}</el-button>
                <el-button
                  v-if="app.status === 'PENDING'"
                  :icon="X"
                  type="danger"
                  text
                  size="small"
                  @click="reviewApp(app, 'REJECTED')"
                >{{ t('volunteerTasks.reject') }}</el-button>
                <el-button
                  v-if="app.status === 'APPROVED'"
                  :icon="CheckCircle"
                  type="primary"
                  text
                  size="small"
                  @click="completeApp(app)"
                >{{ t('volunteerTasks.confirmComplete') }}</el-button>
              </template>
            </div>
          </div>
        </div>
        <EmptyState v-else :title="t('volunteerTasks.noApplications')" :description="t('volunteerTasks.noApplicationsDesc')" :compact="true" />

        <div style="display:flex;gap:8px;margin-top:20px">
          <template v-if="canEdit(current)">
            <el-button :icon="Pencil" type="primary" @click="detailVisible = false; openEdit(current)">{{ t('volunteerTasks.edit') }}</el-button>
            <el-button :icon="RefreshCw" @click="openStatus(current)">{{ t('volunteerTasks.updateStatus') }}</el-button>
            <el-button :icon="Archive" type="danger" @click="offlineTask(current)">{{ t('volunteerTasks.offline') }}</el-button>
          </template>
          <el-button v-else-if="canApply(current)" :icon="UserPlus" type="primary" @click="detailVisible = false; openApply(current)">{{ t('volunteerTasks.wantToApply') }}</el-button>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="dialogVisible" :title="t('volunteerTasks.publishDialogTitle')" width="720px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item :label="t('volunteerTasks.formTitle')" prop="title">
          <el-input v-model="form.title" placeholder="如：周六去某小区抓流浪猫绝育" />
        </el-form-item>
        <el-form-item :label="t('volunteerTasks.formDescription')" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="详细描述任务内容、注意事项、集合地点等" />
        </el-form-item>
        <el-form-item :label="t('volunteerTasks.formLocation')" prop="location">
          <el-input v-model="form.location" placeholder="具体地址或区域" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="t('volunteerTasks.formMaxVolunteers')" prop="maxVolunteers">
              <el-input-number v-model="form.maxVolunteers" :min="1" :max="100" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="t('volunteerTasks.formScheduledTime')">
              <el-date-picker v-model="form.scheduledTime" type="datetime" :placeholder="t('volunteerTasks.placeholderSelectTime')" style="width: 100%" value-format="YYYY-MM-DDTHH:mm:ss" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item :label="t('volunteerTasks.formRelatedRescue')">
          <el-input v-model="form.relatedRescueId" placeholder="关联的救助信息ID（选填）" />
        </el-form-item>
        <el-form-item :label="t('volunteerTasks.formCoverImage')">
          <ImageUploader v-model="form.imageUrls" usage="task" :limit="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button :loading="saving" :icon="Send" type="primary" @click="submit">{{ t('volunteerTasks.submitPublish') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editVisible" :title="t('volunteerTasks.editDialogTitle')" width="720px" append-to-body>
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-position="top">
        <el-form-item :label="t('volunteerTasks.formTitle')" prop="title">
          <el-input v-model="editForm.title" />
        </el-form-item>
        <el-form-item :label="t('volunteerTasks.formDescription')" prop="description">
          <el-input v-model="editForm.description" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item :label="t('volunteerTasks.formLocation')" prop="location">
          <el-input v-model="editForm.location" />
        </el-form-item>
        <el-form-item :label="t('volunteerTasks.formMaxVolunteers')" prop="maxVolunteers">
          <el-input-number v-model="editForm.maxVolunteers" :min="1" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="t('volunteerTasks.formScheduledTime')">
          <el-date-picker v-model="editForm.scheduledTime" type="datetime" style="width: 100%" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
        <el-form-item :label="t('volunteerTasks.formCoverImage')">
          <ImageUploader v-model="editForm.imageUrls" usage="task" :limit="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button :loading="saving" type="primary" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="statusVisible" :title="t('volunteerTasks.updateStatusDialogTitle')" width="460px" append-to-body>
      <el-form label-position="top">
        <el-form-item :label="t('volunteerTasks.newStatusLabel')">
          <el-select v-model="newStatus" style="width: 100%">
            <el-option v-for="item in editableStatuses" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusVisible = false">取消</el-button>
        <el-button :loading="saving" type="primary" @click="saveStatus">{{ t('volunteerTasks.update') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="applyVisible" :title="t('volunteerTasks.applyDialogTitle')" width="500px" append-to-body>
      <div v-if="current" class="form-shell">
        <h3>{{ current.title }}</h3>
        <p class="muted">{{ t('volunteerTasks.stillNeedVolunteers', { count: (current.maxVolunteers || 0) - (current.currentVolunteers || 0) }) }}</p>
        <el-form ref="applyFormRef" :model="applyForm" :rules="applyRules" label-position="top">
          <el-form-item :label="t('volunteerTasks.formSelfIntro')">
            <el-input v-model="applyForm.message" type="textarea" :rows="3" :placeholder="t('volunteerTasks.placeholderSelfIntro')" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button :loading="saving" :icon="UserPlus" type="primary" @click="submitApply">{{ t('volunteerTasks.confirmApply') }}</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Archive, Calendar, Check, CheckCircle, Eye, Link2, LogIn, MapPin, Pencil, Plus, RefreshCw, Search, Send, User, UserPlus, Users } from 'lucide-vue-next'
import { useI18n } from 'vue-i18n'
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

const { t } = useI18n()
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
  title: [{ required: true, message: () => t('volunteerTasks.ruleTitleRequired'), trigger: 'blur' }],
  description: [{ required: true, message: () => t('volunteerTasks.ruleDescriptionRequired'), trigger: 'blur' }],
  location: [{ required: true, message: () => t('volunteerTasks.ruleLocationRequired'), trigger: 'blur' }],
  maxVolunteers: [{ required: true, message: () => t('volunteerTasks.ruleMaxVolunteersRequired'), trigger: 'blur' }]
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
    ElMessage.success(t('volunteerTasks.msgTaskUpdated'))
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
    ElMessage.success(t('volunteerTasks.msgStatusUpdated'))
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
    await ElMessageBox.confirm(t('volunteerTasks.confirmOfflineText'), t('volunteerTasks.confirmHint'), { type: 'warning' })
    await volunteerTaskApi.offline(task.id)
    ElMessage.success(t('volunteerTasks.msgOfflineSuccess'))
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
    ElMessage.success(t('volunteerTasks.msgApplySuccess'))
    applyVisible.value = false
    load()
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

async function reviewApp(app, status) {
  const actionText = status === 'APPROVED' ? t('volunteerTasks.approve') : t('volunteerTasks.reject')
  try {
    await ElMessageBox.confirm(`${t('volunteerTasks.confirmReviewText', { action: actionText, name: app.volunteerNickname })}`, t('volunteerTasks.confirmHint'), { type: status === 'APPROVED' ? 'success' : 'warning' })
    await volunteerTaskApi.reviewApplication(app.id, { status, reviewComment: '' })
    ElMessage.success(t('volunteerTasks.msgActionDone', { action: actionText }))
    await loadApplications(current.value.id)
    load()
  } catch (error) {
    if (error !== 'cancel') notifyError(error)
  }
}

async function completeApp(app) {
  try {
    await ElMessageBox.confirm(`${t('volunteerTasks.confirmCompleteText', { name: app.volunteerNickname })}`, t('volunteerTasks.confirmHint'), { type: 'info' })
    await volunteerTaskApi.completeApplication(app.id)
    ElMessage.success(t('volunteerTasks.msgConfirmedComplete'))
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
    ElMessage.success(t('volunteerTasks.msgPublishSuccess'))
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