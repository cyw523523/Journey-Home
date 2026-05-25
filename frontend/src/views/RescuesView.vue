<template>
  <section class="view page">
    <div class="section-head">
      <div>
        <h1>救助信息</h1>
        <p>发布发现地点、动物情况和联系方式，审核通过后会公开展示给更多人查看。</p>
      </div>
      <el-button v-if="auth.isLoggedIn.value" :icon="Plus" type="primary" size="large" @click="dialogVisible = true">发布救助</el-button>
      <el-button v-else :icon="LogIn" size="large" @click="$router.push('/auth')">登录后发布</el-button>
    </div>

    <div class="toolbar tool-panel" style="grid-template-columns: 1.5fr 1fr 1fr auto">
      <el-input v-model="filters.keyword" placeholder="关键词" clearable @keyup.enter="load" />
      <el-input v-model="filters.region" placeholder="地区" clearable @keyup.enter="load" />
      <el-select v-model="filters.status" placeholder="状态" clearable>
        <el-option v-for="item in publicRescueStatuses" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-button :icon="Search" type="primary" @click="load">筛选</el-button>
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
            <el-button v-if="canEdit(rescue)" :icon="Pencil" text size="small" @click="openEdit(rescue)">编辑</el-button>
            <el-button :icon="Eye" @click="openDetail(rescue)">详情</el-button>
          </div>
        </div>
      </article>
    </div>
    <EmptyState v-else title="暂无救助信息" description="可以发布一条救助信息，等待管理员审核后展示。" />

    <el-dialog v-model="detailVisible" title="救助详情" width="680px" append-to-body>
      <div v-if="current" class="form-shell">
        <StatusTag :value="current.status" :text="current.statusText" :options="rescueStatusOptions" />
        <h2>{{ current.location }}</h2>
        <p>{{ current.animalCondition }}</p>
        <p class="muted">{{ current.description }}</p>
        <p class="meta-line" style="margin:8px 0"><Phone :size="16" /> {{ current.contact }}</p>
        <p class="muted" style="margin-top:8px">发布人：{{ current.publisherNickname || '-' }}</p>
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
          <el-button :icon="Pencil" type="primary" @click="detailVisible = false; openEdit(current)">编辑</el-button>
          <el-button :icon="RefreshCw" @click="openStatus(current)">更新状态</el-button>
          <el-button :icon="Archive" type="danger" @click="offlineRescue(current)">下架</el-button>
        </div>
        <div v-else style="margin-top:16px">
          <el-button type="danger" plain @click="reportVisible = true">举报该救助信息</el-button>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="dialogVisible" title="发布救助信息" width="720px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="救助地点" prop="location">
          <el-input v-model="form.location" />
        </el-form-item>
        <el-form-item label="动物情况" prop="animalCondition">
          <el-input v-model="form.animalCondition" />
        </el-form-item>
        <el-form-item label="联系方式" prop="contact">
          <el-input v-model="form.contact" />
        </el-form-item>
        <el-form-item label="求助说明" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="现场图片">
          <ImageUploader v-model="form.imageUrls" usage="rescue" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button :loading="saving" :icon="Send" type="primary" @click="submit">提交审核</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editVisible" title="编辑救助信息" width="720px" append-to-body>
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-position="top">
        <el-form-item label="救助地点" prop="location">
          <el-input v-model="editForm.location" />
        </el-form-item>
        <el-form-item label="动物情况" prop="animalCondition">
          <el-input v-model="editForm.animalCondition" />
        </el-form-item>
        <el-form-item label="联系方式" prop="contact">
          <el-input v-model="editForm.contact" />
        </el-form-item>
        <el-form-item label="求助说明" prop="description">
          <el-input v-model="editForm.description" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="现场图片">
          <ImageUploader v-model="editForm.imageUrls" usage="rescue" />
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

    <ReportDialog v-model="reportVisible" target-type="RESCUE" :target-id="current?.id || 0" />
  </section>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Archive, Eye, LogIn, Pencil, Phone, Plus, RefreshCw, Search, Send } from 'lucide-vue-next'
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
  location: [{ required: true, message: '请输入救助地点', trigger: 'blur' }],
  animalCondition: [{ required: true, message: '请输入动物情况', trigger: 'blur' }],
  contact: [{ required: true, message: '请输入联系方式', trigger: 'blur' }],
  description: [{ required: true, message: '请输入求助说明', trigger: 'blur' }]
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
    ElMessage.success('救助信息已更新')
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
    ElMessage.success('状态已更新')
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
    await ElMessageBox.confirm('下架后该救助信息将从公开列表中移除，确认继续吗？', '提示', { type: 'warning' })
    await rescueApi.offline(rescue.id)
    ElMessage.success('已下架')
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
    ElMessage.success('提交成功，等待管理员审核')
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
