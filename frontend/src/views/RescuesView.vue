<template>
  <section class="view page">
    <div class="section-head">
      <div>
        <h1>{{ t('rescues.title') }}</h1>
        <p>{{ t('rescues.subtitle') }}</p>
      </div>
      <el-button v-if="auth.isLoggedIn.value" :icon="Plus" type="primary" size="large" @click="dialogVisible = true">{{ t('rescues.publish') }}</el-button>
      <el-button v-else :icon="LogIn" size="large" @click="$router.push('/auth')">{{ t('rescues.loginToPublish') }}</el-button>
    </div>

    <div class="toolbar tool-panel" style="grid-template-columns: 1.5fr 1fr 1fr auto">
      <el-input v-model="filters.keyword" :placeholder="t('rescues.placeholderKeyword')" clearable @keyup.enter="load" />
      <el-input v-model="filters.region" :placeholder="t('rescues.placeholderRegion')" clearable @keyup.enter="load" />
      <el-select v-model="filters.status" :placeholder="t('rescues.placeholderStatus')" clearable>
        <el-option v-for="item in publicRescueStatuses" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-button :icon="Search" type="primary" @click="load">{{ t('rescues.filter') }}</el-button>
    </div>

    <el-skeleton v-if="loading" :rows="6" animated />
    <div v-else-if="rescues.length" class="grid rescue-grid">
      <article v-for="rescue in rescues" :key="rescue.id" class="rescue-card lift-card">
        <StatusTag :value="rescue.status" :text="rescue.statusText" :options="rescueStatusOptions" />
        <h3>{{ rescue.location }}</h3>
        <p>{{ rescue.animalCondition }}</p>
        <p class="muted">{{ rescue.description }}</p>
        <div style="display: flex; justify-content: space-between; align-items: center; gap: 12px">
          <div class="meta-line"><Phone :size="16" /> {{ rescue.contact }}</div>
          <div style="display:flex;gap:6px">
            <el-button v-if="canEdit(rescue)" :icon="Pencil" text size="small" @click="openEdit(rescue)">{{ t('rescues.edit') }}</el-button>
            <el-button :icon="Eye" @click="openDetail(rescue)">{{ t('rescues.detail') }}</el-button>
          </div>
        </div>
      </article>
    </div>
    <EmptyState v-else :title="t('rescues.emptyTitle')" :description="t('rescues.emptyDescription')" />

    <el-dialog v-model="detailVisible" :title="t('rescues.detailTitle')" width="680px" append-to-body>
      <div v-if="current" class="form-shell">
        <StatusTag :value="current.status" :text="current.statusText" :options="rescueStatusOptions" />
        <h2>{{ current.location }}</h2>
        <p>{{ current.animalCondition }}</p>
        <p class="muted">{{ current.description }}</p>
        <p class="meta-line" style="margin:8px 0"><Phone :size="16" /> {{ current.contact }}</p>
        <p class="muted" style="margin-top:8px">{{ t('rescues.publisher') }}{{ current.publisherNickname || '-' }}</p>
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
        <div v-if="canEdit(current)" style="display:flex;gap:8px;margin-top:16px">
          <el-button :icon="Pencil" type="primary" @click="detailVisible = false; openEdit(current)">{{ t('rescues.edit') }}</el-button>
          <el-button :icon="RefreshCw" @click="openStatus(current)">{{ t('rescues.updateStatus') }}</el-button>
          <el-button :icon="Archive" type="danger" @click="offlineRescue(current)">{{ t('rescues.offline') }}</el-button>
        </div>
        <div v-else style="margin-top:16px">
          <el-button type="danger" plain @click="reportVisible = true">{{ t('rescues.report') }}</el-button>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="dialogVisible" :title="t('rescues.publishDialogTitle')" width="720px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item :label="t('rescues.formLocation')" prop="location">
          <el-input v-model="form.location" />
        </el-form-item>
        <el-form-item :label="t('rescues.formAnimalCondition')" prop="animalCondition">
          <el-input v-model="form.animalCondition" />
        </el-form-item>
        <el-form-item :label="t('rescues.formContact')" prop="contact">
          <el-input v-model="form.contact" />
        </el-form-item>
        <el-form-item :label="t('rescues.formDescription')" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item :label="t('rescues.formImages')">
          <ImageUploader v-model="form.imageUrls" usage="rescue" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button :loading="saving" :icon="Send" type="primary" @click="submit">{{ t('rescues.submitReview') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editVisible" :title="t('rescues.editDialogTitle')" width="720px" append-to-body>
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-position="top">
        <el-form-item :label="t('rescues.formLocation')" prop="location">
          <el-input v-model="editForm.location" />
        </el-form-item>
        <el-form-item :label="t('rescues.formAnimalCondition')" prop="animalCondition">
          <el-input v-model="editForm.animalCondition" />
        </el-form-item>
        <el-form-item :label="t('rescues.formContact')" prop="contact">
          <el-input v-model="editForm.contact" />
        </el-form-item>
        <el-form-item :label="t('rescues.formDescription')" prop="description">
          <el-input v-model="editForm.description" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item :label="t('rescues.formImages')">
          <ImageUploader v-model="editForm.imageUrls" usage="rescue" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button :loading="saving" type="primary" @click="saveEdit">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="statusVisible" :title="t('rescues.updateStatusDialogTitle')" width="460px" append-to-body>
      <el-form label-position="top">
        <el-form-item :label="t('rescues.newStatusLabel')">
          <el-select v-model="newStatus" style="width: 100%">
            <el-option v-for="item in editableStatuses" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button :loading="saving" type="primary" @click="saveStatus">{{ t('rescues.update') }}</el-button>
      </template>
    </el-dialog>

    <ReportDialog v-model="reportVisible" target-type="RESCUE" :target-id="current?.id || 0" />
  </section>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Archive, Eye, LogIn, Pencil, Phone, Plus, RefreshCw, Search, Send } from 'lucide-vue-next'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import EmptyState from '../components/EmptyState.vue'
import ImageUploader from '../components/ImageUploader.vue'
import ReportDialog from '../components/ReportDialog.vue'
import StatusTag from '../components/StatusTag.vue'
import { rescueApi } from '../api'
import { notifyError } from '../api/http'
import { demoRescues } from '../data/demoData'
import { rescueStatusOptions } from '../utils/status'
import { useAuth } from '../stores/auth'

const { t } = useI18n()
const auth = useAuth()
const route = useRoute()
const router = useRouter()
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
  location: [{ required: true, message: t('rescues.ruleLocation'), trigger: 'blur' }],
  animalCondition: [{ required: true, message: t('rescues.ruleAnimalCondition'), trigger: 'blur' }],
  contact: [{ required: true, message: t('rescues.ruleContact'), trigger: 'blur' }],
  description: [{ required: true, message: t('rescues.ruleDescription'), trigger: 'blur' }]
}
const editRules = { ...rules }

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
    ElMessage.success(t('rescues.msgUpdated'))
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
    ElMessage.success(t('rescues.msgStatusUpdated'))
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
    await ElMessageBox.confirm(t('rescues.confirmOffline'), t('common.tip'), { type: 'warning' })
    await rescueApi.offline(rescue.id)
    ElMessage.success(t('rescues.msgOfflined'))
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
    ElMessage.success(t('rescues.msgSubmitSuccess'))
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
