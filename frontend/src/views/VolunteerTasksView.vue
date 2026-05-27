<template>
  <section class="view page">
    <div class="section-head">
      <div>
        <h1>{{ t('volunteer.title') }}</h1>
        <p>{{ t('volunteer.description') }}</p>
      </div>
      <el-button v-if="auth.isLoggedIn.value" :icon="Plus" type="primary" size="large" @click="dialogVisible = true">{{ t('volunteer.publish') }}</el-button>
      <el-button v-else :icon="LogIn" size="large" @click="$router.push('/auth')">{{ t('volunteer.loginToPublish') }}</el-button>
    </div>

    <div class="toolbar tool-panel" style="grid-template-columns: 1.5fr 1fr 1fr auto">
      <el-input v-model="filters.keyword" :placeholder="t('common.keyword')" clearable @keyup.enter="load" />
      <el-input v-model="filters.region" :placeholder="t('common.region')" clearable @keyup.enter="load" />
      <el-select v-model="filters.status" :placeholder="t('common.status')" clearable>
        <el-option v-for="item in publicTaskStatuses" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-button :icon="Search" type="primary" @click="load">{{ t('common.filter') }}</el-button>
    </div>

    <el-skeleton v-if="loading" :rows="6" animated />
    <div v-else-if="tasks.length" class="grid task-grid">
      <article v-for="task in tasks" :key="task.id" class="task-card lift-card">
        <StatusTag :value="task.status" :text="task.statusText" :options="volunteerTaskStatusOptions" />
        <h3>{{ task.title }}</h3>
        <p class="muted">{{ task.description }}</p>
        <div class="task-meta-row">
          <span class="meta-line"><MapPin :size="14" /> {{ task.location }}</span>
          <span class="meta-line"><Users :size="14" /> {{ task.currentVolunteers || 0 }}/{{ task.maxVolunteers }} {{ t('volunteer.volunteersCount') }}</span>
        </div>
        <div v-if="task.scheduledTime" class="meta-line"><Calendar :size="14" /> {{ formatDate(task.scheduledTime) }}</div>
        <div v-if="task.relatedRescueLocation" class="meta-line rescue-link">
          <Link2 :size="14" /> {{ t('volunteer.relatedRescueLocation') }}：{{ task.relatedRescueLocation }}
        </div>
        <div class="task-card-foot">
          <div class="meta-line"><User :size="14" /> {{ task.publisherNickname || '-' }}</div>
          <div class="task-card-actions">
            <el-button v-if="canEdit(task)" :icon="Pencil" text size="small" @click="openEdit(task)">{{ t('common.edit') }}</el-button>
            <el-button :icon="Eye" @click="openDetail(task)">{{ t('common.details') }}</el-button>
            <el-button
              v-if="canApply(task)"
              :icon="UserPlus"
              type="primary"
              plain
              size="small"
              @click="openApply(task)"
            >{{ t('volunteer.applyNow') }}</el-button>
          </div>
        </div>
      </article>
    </div>
    <EmptyState v-else :title="t('volunteer.emptyTitle')" :description="t('volunteer.emptyDesc')" />

    <el-dialog v-model="detailVisible" :title="t('volunteer.detailTitle')" width="700px" append-to-body>
      <div v-if="current" class="form-shell">
        <StatusTag :value="current.status" :text="current.statusText" :options="volunteerTaskStatusOptions" />
        <h2>{{ current.title }}</h2>
        <p class="muted">{{ current.description }}</p>

        <div class="detail-info-grid">
          <div class="info-item"><MapPin :size="16" /><span><strong>{{ t('volunteer.location') }}：</strong>{{ current.location }}</span></div>
          <div class="info-item"><Users :size="16" /><span><strong>{{ t('volunteer.volunteersCount') }}：</strong>{{ current.currentVolunteers || 0 }} / {{ current.maxVolunteers }} {{ t('volunteer.volunteersCount') }}</span></div>
          <div v-if="current.scheduledTime" class="info-item"><Calendar :size="16" /><span><strong>{{ t('volunteer.time') }}：</strong>{{ formatDate(current.scheduledTime) }}</span></div>
          <div class="info-item"><User :size="16" /><span><strong>{{ t('volunteer.publisher') }}：</strong>{{ current.publisherNickname || '-' }}</span></div>
          <div v-if="current.relatedRescueLocation" class="info-item rescue-link"><Link2 :size="16" /><span><strong>{{ t('volunteer.relatedRescueLocation') }}：</strong>{{ current.relatedRescueLocation }}</span></div>
        </div>

        <div class="volunteer-progress">
          <span>{{ t('volunteer.progress') }}</span>
          <el-progress
            :percentage="Math.min(100, Math.round(((current.currentVolunteers || 0) / (current.maxVolunteers || 1)) * 100))"
            :stroke-width="10"
          />
        </div>

        <h4 style="margin-top: 20px; margin-bottom: 12px">{{ t('volunteer.applications') }}</h4>
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
                >{{ t('volunteer.approve') }}</el-button>
                <el-button
                  v-if="app.status === 'PENDING'"
                  :icon="X"
                  type="danger"
                  text
                  size="small"
                  @click="reviewApp(app, 'REJECTED')"
                >{{ t('volunteer.reject') }}</el-button>
                <el-button
                  v-if="app.status === 'APPROVED'"
                  :icon="CheckCircle"
                  type="primary"
                  text
                  size="small"
                  @click="completeApp(app)"
                >{{ t('volunteer.complete') }}</el-button>
              </template>
            </div>
          </div>
        </div>
        <EmptyState v-else :title="t('volunteer.noApplications')" :description="t('volunteer.noApplicationsDesc')" :compact="true" />

        <div class="task-detail-actions">
          <template v-if="canEdit(current)">
            <el-button :icon="Pencil" type="primary" @click="detailVisible = false; openEdit(current)">{{ t('common.edit') }}</el-button>
            <el-button :icon="RefreshCw" @click="openStatus(current)">{{ t('common.status') }}</el-button>
            <el-button :icon="Archive" type="danger" @click="offlineTask(current)">{{ t('common.delete') }}</el-button>
          </template>
          <el-button v-else-if="canApply(current)" :icon="UserPlus" type="primary" @click="detailVisible = false; openApply(current)">{{ t('volunteer.applyMe') }}</el-button>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="dialogVisible" :title="t('volunteer.createTitle')" width="720px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item :label="t('volunteer.titleField')" prop="title">
          <el-input v-model="form.title" :placeholder="t('volunteer.titlePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('volunteer.descriptionField')" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" :placeholder="t('volunteer.descriptionPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('volunteer.location')" prop="location">
          <el-input v-model="form.location" :placeholder="t('volunteer.locationPlaceholder')" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="t('volunteer.maxVolunteers')" prop="maxVolunteers">
              <el-input-number v-model="form.maxVolunteers" :min="1" :max="100" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="t('volunteer.scheduledTime')">
              <el-date-picker v-model="form.scheduledTime" type="datetime" :placeholder="t('volunteer.scheduledTimePlaceholder')" style="width: 100%" value-format="YYYY-MM-DDTHH:mm:ss" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item :label="t('volunteer.relatedRescue')">
          <el-input v-model="form.relatedRescueId" :placeholder="t('volunteer.relatedRescuePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('volunteer.coverImage')">
          <ImageUploader v-model="form.imageUrls" usage="task" :limit="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button :loading="saving" :icon="Send" type="primary" @click="submit">{{ t('volunteer.submitPublish') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editVisible" :title="t('volunteer.editTitle')" width="720px" append-to-body>
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-position="top">
        <el-form-item :label="t('volunteer.titleField')" prop="title">
          <el-input v-model="editForm.title" />
        </el-form-item>
        <el-form-item :label="t('volunteer.descriptionField')" prop="description">
          <el-input v-model="editForm.description" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item :label="t('volunteer.location')" prop="location">
          <el-input v-model="editForm.location" />
        </el-form-item>
        <el-form-item :label="t('volunteer.maxVolunteers')" prop="maxVolunteers">
          <el-input-number v-model="editForm.maxVolunteers" :min="1" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="t('volunteer.scheduledTime')">
          <el-date-picker v-model="editForm.scheduledTime" type="datetime" style="width: 100%" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
        <el-form-item :label="t('volunteer.coverImage')">
          <ImageUploader v-model="editForm.imageUrls" usage="task" :limit="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button :loading="saving" type="primary" @click="saveEdit">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="statusVisible" :title="t('common.status')" width="460px" append-to-body>
      <el-form label-position="top">
        <el-form-item :label="t('common.status')">
          <el-select v-model="newStatus" style="width: 100%">
            <el-option v-for="item in editableStatuses" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button :loading="saving" type="primary" @click="saveStatus">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="applyVisible" :title="t('volunteer.applyTitle')" width="500px" append-to-body>
      <div v-if="current" class="form-shell">
        <h3>{{ current.title }}</h3>
        <p class="muted">{{ t('volunteer.remainingVolunteers', { count: (current.maxVolunteers || 0) - (current.currentVolunteers || 0) }) }}</p>
        <el-form ref="applyFormRef" :model="applyForm" :rules="applyRules" label-position="top">
          <el-form-item :label="t('volunteer.applyMessage')">
            <el-input v-model="applyForm.message" type="textarea" :rows="3" :placeholder="t('volunteer.applyMessagePlaceholder')" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="applyVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button :loading="saving" :icon="UserPlus" type="primary" @click="submitApply">{{ t('volunteer.applyConfirm') }}</el-button>
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
import { useAiAssistantPageContext } from '../composables/useAiAssistantPageContext'
import { notifyError } from '../api/http'
import {
  volunteerTaskStatusOptions,
  volunteerApplicationStatusOptions
} from '../utils/status'
import { useAuth } from '../stores/auth'

const auth = useAuth()
const { t, locale } = useI18n()
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
  title: [{ required: true, message: t('volunteer.titleRequired'), trigger: 'blur' }],
  description: [{ required: true, message: t('volunteer.descriptionRequired'), trigger: 'blur' }],
  location: [{ required: true, message: t('volunteer.locationRequired'), trigger: 'blur' }],
  maxVolunteers: [{ required: true, message: t('volunteer.maxVolunteersRequired'), trigger: 'blur' }]
}
const editRules = { ...rules }
const applyRules = {}

useAiAssistantPageContext(() => ({
  pageTitle: detailVisible.value && current.value ? `志愿任务 #${current.value.id}` : t('volunteer.title'),
  pageSummary: detailVisible.value && current.value
    ? '当前正在查看一个志愿任务详情和报名列表。'
    : t('volunteer.description'),
  entityType: detailVisible.value && current.value ? 'VOLUNTEER_TASK' : null,
  entityId: detailVisible.value && current.value ? current.value.id : null,
  viewData: {
    filters: { ...filters },
    visibleTasks: tasks.value.slice(0, 6).map((item) => ({
      id: item.id,
      title: item.title,
      location: item.location,
      statusText: item.statusText,
      currentVolunteers: item.currentVolunteers,
      maxVolunteers: item.maxVolunteers
    })),
    detailOpen: detailVisible.value,
    currentTask: current.value ? {
      id: current.value.id,
      title: current.value.title,
      location: current.value.location,
      statusText: current.value.statusText,
      currentVolunteers: current.value.currentVolunteers,
      maxVolunteers: current.value.maxVolunteers
    } : null,
    applicationCount: applications.value.length
  }
}))

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString(locale.value === 'zh' ? 'zh-CN' : 'en-US')
}

function formatTime(value) {
  return value ? new Date(value).toLocaleString(locale.value === 'zh' ? 'zh-CN' : 'en-US') : '-'
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
    ElMessage.success(t('volunteer.saveSuccess'))
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
    ElMessage.success(t('volunteer.updateSuccess'))
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
    await ElMessageBox.confirm(t('volunteer.offlineConfirm'), t('common.warning'), { type: 'warning' })
    await volunteerTaskApi.offline(task.id)
    ElMessage.success(t('volunteer.offlineSuccess'))
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
    ElMessage.success(t('volunteer.applySuccess'))
    applyVisible.value = false
    load()
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

async function reviewApp(app, status) {
  const actionText = status === 'APPROVED' ? t('volunteer.approve') : t('volunteer.reject')
  try {
    await ElMessageBox.confirm(
      status === 'APPROVED'
        ? t('volunteer.approveConfirm', { name: app.volunteerNickname })
        : t('volunteer.rejectConfirm', { name: app.volunteerNickname }),
      t('common.warning'),
      { type: status === 'APPROVED' ? 'success' : 'warning' }
    )
    await volunteerTaskApi.reviewApplication(app.id, { status, reviewComment: '' })
    ElMessage.success(status === 'APPROVED' ? t('volunteer.approvedSuccess') : t('volunteer.rejectedSuccess'))
    await loadApplications(current.value.id)
    load()
  } catch (error) {
    if (error !== 'cancel') notifyError(error)
  }
}

async function completeApp(app) {
  try {
    await ElMessageBox.confirm(t('volunteer.completeConfirm', { name: app.volunteerNickname }), t('common.warning'), { type: 'info' })
    await volunteerTaskApi.completeApplication(app.id)
    ElMessage.success(t('volunteer.completedSuccess'))
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
    ElMessage.success(t('volunteer.publishSuccess'))
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
.task-card-foot,
.task-card-actions,
.task-detail-actions,
.app-right {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.task-card-foot {
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
  gap: 12px;
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
  align-items: center;
  flex-shrink: 0;
}
.task-detail-actions {
  margin-top: 20px;
}
</style>
