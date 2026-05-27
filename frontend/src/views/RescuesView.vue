<template>
  <section class="view page">
    <div class="section-head">
      <div>
        <h1>{{ t('rescue.title') }}</h1>
        <p>{{ t('rescue.description') }}</p>
      </div>
      <el-button v-if="auth.isLoggedIn.value" :icon="Plus" type="primary" size="large" @click="dialogVisible = true">{{ t('rescue.publish') }}</el-button>
      <el-button v-else :icon="LogIn" size="large" @click="$router.push('/auth')">{{ t('rescue.loginToPublish') }}</el-button>
    </div>

    <div class="toolbar tool-panel" style="grid-template-columns: 1.5fr 1fr 1fr auto">
      <el-input v-model="filters.keyword" :placeholder="t('common.keyword')" clearable @keyup.enter="load" />
      <el-input v-model="filters.region" :placeholder="t('common.region')" clearable @keyup.enter="load" />
      <el-select v-model="filters.status" :placeholder="t('common.status')" clearable>
        <el-option v-for="item in publicRescueStatuses" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-button :icon="Search" type="primary" @click="load">{{ t('common.filter') }}</el-button>
    </div>

    <el-skeleton v-if="loading" :rows="6" animated />
    <div v-else-if="rescues.length" class="grid rescue-grid">
      <article v-for="rescue in rescues" :key="rescue.id" class="rescue-card lift-card">
        <StatusTag :value="rescue.status" :text="rescue.statusText" :options="rescueStatusOptions" />
        <h3>{{ rescue.location }}</h3>
        <p>{{ rescue.animalCondition }}</p>
        <p class="muted">{{ rescue.description }}</p>
        <div class="rescue-card-foot">
          <div class="meta-line"><Phone :size="16" /> {{ rescue.contact }}</div>
          <div class="rescue-card-actions">
            <el-button v-if="canEdit(rescue)" :icon="Pencil" text size="small" @click="openEdit(rescue)">{{ t('common.edit') }}</el-button>
            <el-button :icon="Eye" @click="openDetail(rescue)">{{ t('common.details') }}</el-button>
          </div>
        </div>
      </article>
    </div>
    <EmptyState v-else :title="t('rescue.emptyTitle')" :description="t('rescue.emptyDesc')" />

    <el-dialog v-model="detailVisible" :title="t('rescue.detailTitle')" width="680px" append-to-body>
      <div v-if="current" class="form-shell">
        <StatusTag :value="current.status" :text="current.statusText" :options="rescueStatusOptions" />
        <h2>{{ current.location }}</h2>
        <p>{{ current.animalCondition }}</p>
        <p class="muted">{{ current.description }}</p>
        <p class="meta-line" style="margin:8px 0"><Phone :size="16" /> {{ current.contact }}</p>
        <p class="muted" style="margin-top:8px">{{ t('rescue.publisher') }}：{{ current.publisherNickname || '-' }}</p>
        <div class="thumb-grid rescue-preview-grid" v-if="current.imageUrls?.length">
          <el-image
            v-for="(url, index) in current.imageUrls"
            :key="`${url}-${index}`"
            class="rescue-preview-image"
            :src="url"
            fit="cover"
            :preview-src-list="current.imageUrls"
            :initial-index="index"
            preview-teleported
            hide-on-click-modal
          />
        </div>
        <div v-if="canEdit(current)" class="rescue-detail-actions">
          <el-button :icon="Pencil" type="primary" @click="detailVisible = false; openEdit(current)">{{ t('common.edit') }}</el-button>
          <el-button :icon="RefreshCw" @click="openStatus(current)">{{ t('rescue.updateStatus') }}</el-button>
          <el-button :icon="Archive" type="danger" @click="offlineRescue(current)">{{ t('common.delete') }}</el-button>
        </div>
        <div v-else style="margin-top:16px">
          <el-button type="danger" plain @click="reportVisible = true">{{ t('rescue.reportThis') }}</el-button>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="dialogVisible" :title="t('rescue.createTitle')" width="720px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item :label="t('rescue.location')" prop="location">
          <el-input v-model="form.location" />
        </el-form-item>
        <el-form-item :label="t('rescue.animalCondition')" prop="animalCondition">
          <el-input v-model="form.animalCondition" />
        </el-form-item>
        <el-form-item :label="t('common.contact')" prop="contact">
          <el-input v-model="form.contact" />
        </el-form-item>
        <el-form-item :label="t('rescue.helpDescription')" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item :label="t('rescue.sceneImages')">
          <ImageUploader v-model="form.imageUrls" usage="rescue" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button :loading="saving" :icon="Send" type="primary" @click="submit">{{ t('rescue.submitReview') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editVisible" :title="t('rescue.editTitle')" width="720px" append-to-body>
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-position="top">
        <el-form-item :label="t('rescue.location')" prop="location">
          <el-input v-model="editForm.location" />
        </el-form-item>
        <el-form-item :label="t('rescue.animalCondition')" prop="animalCondition">
          <el-input v-model="editForm.animalCondition" />
        </el-form-item>
        <el-form-item :label="t('common.contact')" prop="contact">
          <el-input v-model="editForm.contact" />
        </el-form-item>
        <el-form-item :label="t('rescue.helpDescription')" prop="description">
          <el-input v-model="editForm.description" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item :label="t('rescue.sceneImages')">
          <ImageUploader v-model="editForm.imageUrls" usage="rescue" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button :loading="saving" type="primary" @click="saveEdit">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="statusVisible" :title="t('rescue.updateStatus')" width="460px" append-to-body>
      <el-form label-position="top">
        <el-form-item :label="t('rescue.newStatus')">
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

    <ReportDialog v-model="reportVisible" target-type="RESCUE" :target-id="current?.id || 0" />
  </section>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Archive, Eye, LogIn, Pencil, Phone, Plus, RefreshCw, Search, Send } from 'lucide-vue-next'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import EmptyState from '../components/EmptyState.vue'
import ImageUploader from '../components/ImageUploader.vue'
import ReportDialog from '../components/ReportDialog.vue'
import StatusTag from '../components/StatusTag.vue'
import { rescueApi } from '../api'
import { useAiAssistantPageContext } from '../composables/useAiAssistantPageContext'
import { notifyError } from '../api/http'
import { demoRescues } from '../data/demoData'
import { rescueStatusOptions } from '../utils/status'
import { useAuth } from '../stores/auth'

const auth = useAuth()
const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const loading = ref(false)
const saving = ref(false)
const detailVisible = ref(false)
const dialogVisible = ref(false)
const editVisible = ref(false)
const statusVisible = ref(false)
const reportVisible = ref(false)
const current = ref(null)
const statusTarget = ref(null)
const newStatus = ref('')
const rescues = ref([])
const formRef = ref()
const editFormRef = ref()
const publicRescueStatuses = rescueStatusOptions.filter((item) => ['PENDING_PROCESS', 'PROCESSING', 'COMPLETED'].includes(item.value))
const editableStatuses = rescueStatusOptions.filter((item) => item.value !== 'PENDING_REVIEW' && item.value !== 'REJECTED')
const filters = reactive({ keyword: '', region: '', status: '' })
const form = reactive({ location: '', animalCondition: '', contact: '', description: '', imageUrls: [] })
const editForm = reactive({ id: null, location: '', animalCondition: '', contact: '', description: '', imageUrls: [] })
const rules = {
  location: [{ required: true, message: t('rescue.locationRequired'), trigger: 'blur' }],
  animalCondition: [{ required: true, message: t('rescue.animalConditionRequired'), trigger: 'blur' }],
  contact: [{ required: true, message: t('rescue.contactRequired'), trigger: 'blur' }],
  description: [{ required: true, message: t('rescue.descriptionRequired'), trigger: 'blur' }]
}
const editRules = { ...rules }

useAiAssistantPageContext(() => ({
  pageTitle: detailVisible.value && current.value ? `救助详情 #${current.value.id}` : t('rescue.title'),
  pageSummary: detailVisible.value && current.value
    ? '当前正在查看一条救助信息详情。'
    : t('rescue.description'),
  entityType: detailVisible.value && current.value ? 'RESCUE' : null,
  entityId: detailVisible.value && current.value ? current.value.id : null,
  viewData: {
    filters: { ...filters },
    visibleRescues: rescues.value.slice(0, 6).map((item) => ({
      id: item.id,
      location: item.location,
      statusText: item.statusText,
      animalCondition: item.animalCondition
    })),
    detailOpen: detailVisible.value
  }
}))

function canEdit(record) {
  if (!auth.state.user) return false
  return auth.state.user.id === record.publisherId || auth.isAdmin.value
}

async function load() {
  loading.value = true
  try {
    const data = await rescueApi.list({ ...filters, page: 0, size: 12 })
    rescues.value = data.content || []
  } catch {
    rescues.value = demoRescues
  } finally {
    loading.value = false
  }
}

async function openDetail(rescue) {
  try {
    current.value = await rescueApi.detail(rescue.id)
  } catch {
    current.value = rescue
  }
  detailVisible.value = true
  syncDetailQuery(current.value?.id || rescue.id)
}

function syncDetailQuery(rescueId) {
  const nextId = String(rescueId)
  if (route.query.detail === nextId) return
  router.replace({ query: { ...route.query, detail: nextId } })
}

function clearDetailQuery() {
  if (!route.query.detail) return
  const nextQuery = { ...route.query }
  delete nextQuery.detail
  router.replace({ query: nextQuery })
}

async function openDetailFromQuery() {
  const rescueId = Number(route.query.detail)
  if (!rescueId || Number.isNaN(rescueId)) return
  if (detailVisible.value && current.value?.id === rescueId) return
  await openDetail({ id: rescueId })
}

function openEdit(rescue) {
  Object.assign(editForm, {
    id: rescue.id,
    location: rescue.location || '',
    animalCondition: rescue.animalCondition || '',
    contact: rescue.contact || '',
    description: rescue.description || '',
    imageUrls: rescue.imageUrls || []
  })
  editVisible.value = true
}

async function saveEdit() {
  await editFormRef.value.validate()
  saving.value = true
  try {
    await rescueApi.update(editForm.id, editForm)
    ElMessage.success(t('rescue.saveSuccess'))
    editVisible.value = false
    load()
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

function openStatus(rescue) {
  statusTarget.value = rescue
  newStatus.value = rescue.status
  statusVisible.value = true
}

async function saveStatus() {
  saving.value = true
  try {
    await rescueApi.updateStatus(statusTarget.value.id, { status: newStatus.value })
    ElMessage.success(t('rescue.updateSuccess'))
    statusVisible.value = false
    load()
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

async function offlineRescue(rescue) {
  try {
    await ElMessageBox.confirm(t('rescue.offlineConfirm'), t('common.warning'), { type: 'warning' })
    await rescueApi.offline(rescue.id)
    ElMessage.success(t('rescue.offlineSuccess'))
    detailVisible.value = false
    load()
  } catch (error) {
    if (error !== 'cancel') notifyError(error)
  }
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    await rescueApi.create(form)
    ElMessage.success(t('rescue.createSuccess'))
    Object.assign(form, { location: '', animalCondition: '', contact: '', description: '', imageUrls: [] })
    dialogVisible.value = false
    load()
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

watch(() => route.query.detail, () => {
  openDetailFromQuery()
})

watch(detailVisible, (visible) => {
  if (!visible) clearDetailQuery()
})

onMounted(async () => {
  await load()
  await openDetailFromQuery()
})
</script>

<style scoped>
.rescue-card-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.rescue-card-actions,
.rescue-detail-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.rescue-detail-actions {
  margin-top: 16px;
}
</style>
