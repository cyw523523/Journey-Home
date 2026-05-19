<template>
  <section class="view page">
    <el-skeleton v-if="loading" :rows="8" animated />
    <div v-else class="detail-layout">
      <div class="detail-photo surface">
        <img :src="animal.coverImageUrl || demoImages[0]" alt="动物详情照片" />
      </div>
      <div class="detail-panel surface">
        <p class="eyebrow"><PawPrint :size="16" /> 动物档案 #{{ animal.id }}</p>
        <StatusTag :value="animal.status" :text="animal.statusText" :options="animalStatusOptions" />
        <h1>{{ animal.typeText || optionText(animalTypeOptions, animal.type) }}</h1>
        <p class="muted">{{ animal.description || '暂无详细说明' }}</p>

        <div class="info-grid">
          <div class="info-box">
            <small>性别</small>
            <strong>{{ animal.genderText || optionText(genderOptions, animal.gender) }}</strong>
          </div>
          <div class="info-box">
            <small>年龄</small>
            <strong>{{ animal.age ?? '未知' }} 岁</strong>
          </div>
          <div class="info-box">
            <small>发现地区</small>
            <strong>{{ animal.foundRegion }}</strong>
          </div>
          <div class="info-box">
            <small>健康情况</small>
            <strong>{{ animal.healthCondition || '待补充' }}</strong>
          </div>
        </div>

        <div class="medical-section" style="margin-top:18px;border-top:1px solid var(--el-border-color-light);padding-top:16px">
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:10px">
            <h3 style="margin:0;font-size:16px;font-weight:600">医疗记录</h3>
            <el-button v-if="isOwnerOrAdmin" :icon="Plus" size="small" type="primary" @click="openMedicalEditor(null)">添加记录</el-button>
          </div>
          <div v-if="!medicalRecords.length" style="padding:16px;text-align:center;color:var(--el-text-color-secondary);font-size:13px">暂无医疗记录</div>
          <div v-else class="medical-list">
            <div v-for="record in medicalRecords" :key="record.id" class="medical-card">
              <div class="medical-head">
                <el-tag :type="medicalTypeStyle(record.type)" size="small">{{ record.typeText }}</el-tag>
                <span class="medical-date">{{ record.recordDate }}</span>
                <div v-if="isOwnerOrAdmin" style="margin-left:auto;display:flex;gap:4px">
                  <el-button :icon="Pencil" size="small" text @click="openMedicalEditor(record)">编辑</el-button>
                  <el-button :icon="Trash2" size="small" text type="danger" @click="deleteMedicalRecord(record.id)">删除</el-button>
                </div>
              </div>
              <p v-if="record.veterinarianName || record.institution" style="margin:6px 0 4px;font-size:13px;color:var(--el-text-color-regular)">{{ [record.veterinarianName, record.institution].filter(Boolean).join(' / ') }}</p>
              <p v-if="record.description" style="margin:4px 0;font-size:13px;color:var(--el-text-color-secondary);line-height:1.5">{{ record.description }}</p>
              <div v-if="record.imageUrls?.length" style="display:flex;gap:6px;margin-top:6px;flex-wrap:wrap">
                <img v-for="(url,idx) in record.imageUrls" :key="idx" :src="getFullUrl(url)" style="width:80px;height:80px;object-fit:cover;border-radius:6px;cursor:pointer;border:1px solid var(--el-border-color-lighter)" @click="previewMedicalImage(url)" />
              </div>
            </div>
          </div>
        </div>

        <div class="mini-row">
          <span>发布人：{{ animal.publisherNickname || '-' }}</span>
          <span>{{ animal.createdAt ? new Date(animal.createdAt).toLocaleDateString() : '' }}</span>
        </div>

        <div style="display: flex; gap: 10px; margin-top: 22px; flex-wrap: wrap">
          <el-button :icon="ArrowLeft" @click="$router.back()">返回</el-button>
          <template v-if="isOwnerOrAdmin">
            <el-button :icon="Pencil" type="primary" @click="openEditor">编辑</el-button>
            <el-button :icon="RefreshCw" @click="openStatus">更新状态</el-button>
            <el-button :icon="Archive" type="danger" @click="offline">下架</el-button>
          </template>
          <el-button v-else type="danger" plain @click="reportVisible = true">举报</el-button>
          <el-button
            v-if="animal.status === 'WAITING_ADOPTION'"
            :icon="HeartHandshake"
            type="primary"
            @click="apply"
          >
            提交领养申请
          </el-button>
        </div>
      </div>
    </div>

    <el-dialog v-model="editorVisible" title="编辑动物档案" width="720px" append-to-body>
      <el-form ref="formRef" :model="editor" :rules="rules" label-position="top">
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="动物类型" prop="type">
              <el-select v-model="editor.type" style="width: 100%">
                <el-option v-for="item in animalTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="性别" prop="gender">
              <el-select v-model="editor.gender" style="width: 100%">
                <el-option v-for="item in genderOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="年龄" prop="age">
              <el-input-number v-model="editor.age" :min="0" :max="30" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="发现地区" prop="foundRegion">
          <el-input v-model="editor.foundRegion" />
        </el-form-item>
        <el-form-item label="健康情况">
          <el-input v-model="editor.healthCondition" />
        </el-form-item>
        <el-form-item label="照片">
          <ImageUploader v-model="editor.imageUrls" usage="animal" />
        </el-form-item>
        <el-form-item label="详细说明">
          <el-input v-model="editor.description" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editorVisible = false">取消</el-button>
        <el-button :loading="saving" type="primary" @click="saveEditor">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="statusVisible" title="更新状态" width="460px" append-to-body>
      <el-form label-position="top">
        <el-form-item label="新状态">
          <el-select v-model="newStatus" style="width: 100%">
            <el-option v-for="item in availableStatuses" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusVisible = false">取消</el-button>
        <el-button :loading="saving" type="primary" @click="saveStatus">更新</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="medicalEditorVisible" :title="medicalEditingId ? '编辑医疗记录' : '添加医疗记录'" width="600px" append-to-body>
      <el-form ref="medicalFormRef" :model="medicalEditor" :rules="medicalRules" label-position="top">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="类型" prop="type">
              <el-select v-model="medicalEditor.type" style="width: 100%">
                <el-option v-for="item in medicalRecordTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="日期" prop="recordDate">
              <el-date-picker v-model="medicalEditor.recordDate" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="兽医姓名">
              <el-input v-model="medicalEditor.veterinarianName" placeholder="选填" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="机构名称">
              <el-input v-model="medicalEditor.institution" placeholder="选填" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述">
          <el-input v-model="medicalEditor.description" type="textarea" :rows="3" placeholder="选填" />
        </el-form-item>
        <el-form-item label="佐证图片">
          <ImageUploader v-model="medicalEditor.imageUrls" usage="medical" :limit="6" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="medicalEditorVisible = false">取消</el-button>
        <el-button :loading="medicalSaving" type="primary" @click="saveMedicalRecord">保存</el-button>
      </template>
    </el-dialog>

    <ReportDialog v-model="reportVisible" target-type="ANIMAL" :target-id="animal.id || route.params.id" />
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { Archive, ArrowLeft, HeartHandshake, PawPrint, Pencil, Plus, RefreshCw, Trash2 } from 'lucide-vue-next'
import ReportDialog from '../components/ReportDialog.vue'
import StatusTag from '../components/StatusTag.vue'
import ImageUploader from '../components/ImageUploader.vue'
import { animalApi, medicalRecordApi } from '../api'
import { notifyError } from '../api/http'
import { useAuth } from '../stores/auth'
import { demoAnimals, demoImages } from '../data/demoData'
import { animalStatusOptions, animalTypeOptions, genderOptions, medicalRecordTypeOptions, optionText } from '../utils/status'

const route = useRoute()
const router = useRouter()
const auth = useAuth()
const loading = ref(false)
const saving = ref(false)
const animal = ref({})
const editorVisible = ref(false)
const statusVisible = ref(false)
const reportVisible = ref(false)
const newStatus = ref('')
const formRef = ref()
const medicalFormRef = ref()
const medicalRecords = ref([])
const medicalEditorVisible = ref(false)
const medicalEditingId = ref(null)
const medicalSaving = ref(false)

const medicalEditor = reactive({
  type: 'VACCINE',
  recordDate: '',
  veterinarianName: '',
  institution: '',
  description: '',
  imageUrls: []
})

const medicalRules = {
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  recordDate: [{ required: true, message: '请选择日期', trigger: 'change' }]
}

const editor = reactive({
  type: 'CAT',
  gender: 'UNKNOWN',
  age: 0,
  foundRegion: '',
  healthCondition: '',
  imageUrls: [],
  description: ''
})

const rules = {
  type: [{ required: true, message: '请选择动物类型', trigger: 'change' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  foundRegion: [{ required: true, message: '请输入发现地区', trigger: 'blur' }],
  imageUrls: [{ type: 'array', required: true, min: 1, message: '至少上传一张照片', trigger: 'change' }]
}

const isOwnerOrAdmin = computed(() => {
  if (!auth.state.user) return false
  return auth.state.user.id === animal.value.publisherId || auth.isAdmin.value
})

const availableStatuses = computed(() => {
  return animalStatusOptions.filter((item) => ['WAITING_RESCUE', 'RESCUING', 'WAITING_ADOPTION', 'ADOPTED', 'OFFLINE'].includes(item.value))
})

function apply() {
  if (!auth.isLoggedIn.value) {
    router.push({ name: 'auth', query: { redirect: route.fullPath } })
    return
  }
  router.push(`/adoptions/new/${animal.value.id}`)
}

function openEditor() {
  Object.assign(editor, {
    type: animal.value.type || 'CAT',
    gender: animal.value.gender || 'UNKNOWN',
    age: animal.value.age ?? 0,
    foundRegion: animal.value.foundRegion || '',
    healthCondition: animal.value.healthCondition || '',
    imageUrls: animal.value.imageUrls || [],
    description: animal.value.description || ''
  })
  editorVisible.value = true
}

async function saveEditor() {
  await formRef.value.validate()
  saving.value = true
  try {
    animal.value = await animalApi.update(animal.value.id, editor)
    ElMessage.success('动物档案已更新')
    editorVisible.value = false
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

function openStatus() {
  newStatus.value = animal.value.status
  statusVisible.value = true
}

async function saveStatus() {
  saving.value = true
  try {
    animal.value = await animalApi.updateStatus(animal.value.id, { status: newStatus.value })
    ElMessage.success('状态已更新')
    statusVisible.value = false
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

const API_BASE = window.location.origin

function getFullUrl(url) {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:')) return url
  return `${API_BASE}${url}`
}

function medicalTypeStyle(type) {
  const map = { DEWORMING: 'warning', VACCINE: 'primary', NEUTERING: 'danger', TREATMENT: 'success', OTHER: 'info' }
  return map[type] || 'info'
}

function previewMedicalImage(url) {
  window.open(getFullUrl(url), '_blank')
}

async function fetchMedicalRecords() {
  try {
    medicalRecords.value = await medicalRecordApi.list(route.params.id) || []
  } catch {
    medicalRecords.value = []
  }
}

function openMedicalEditor(record) {
  if (record) {
    medicalEditingId.value = record.id
    Object.assign(medicalEditor, {
      type: record.type,
      recordDate: record.recordDate,
      veterinarianName: record.veterinarianName || '',
      institution: record.institution || '',
      description: record.description || '',
      imageUrls: record.imageUrls || []
    })
  } else {
    medicalEditingId.value = null
    Object.assign(medicalEditor, {
      type: 'VACCINE',
      recordDate: '',
      veterinarianName: '',
      institution: '',
      description: '',
      imageUrls: []
    })
  }
  medicalEditorVisible.value = true
}

async function saveMedicalRecord() {
  await medicalFormRef.value.validate()
  medicalSaving.value = true
  try {
    if (medicalEditingId.value) {
      await medicalRecordApi.update(route.params.id, medicalEditingId.value, { ...medicalEditor })
      ElMessage.success('医疗记录已更新')
    } else {
      await medicalRecordApi.create(route.params.id, { ...medicalEditor })
      ElMessage.success('医疗记录已添加')
    }
    medicalEditorVisible.value = false
    await fetchMedicalRecords()
  } catch (error) {
    notifyError(error)
  } finally {
    medicalSaving.value = false
  }
}

async function deleteMedicalRecord(recordId) {
  try {
    await ElMessageBox.confirm('确认删除这条医疗记录？', '提示', { type: 'warning' })
    await medicalRecordApi.delete(route.params.id, recordId)
    ElMessage.success('已删除')
    await fetchMedicalRecords()
  } catch (error) {
    if (error !== 'cancel') notifyError(error)
  }
}

async function offline() {
  try {
    await ElMessageBox.confirm('下架后该档案将从公开列表中移除，确认继续吗？', '提示', { type: 'warning' })
    await animalApi.offline(animal.value.id)
    ElMessage.success('已下架')
    router.back()
  } catch (error) {
    if (error !== 'cancel') notifyError(error)
  }
}

onMounted(async () => {
  loading.value = true
  try {
    const data = await animalApi.detail(route.params.id)
    if (data && data.id) {
      animal.value = data
    } else {
      animal.value = demoAnimals.find((item) => String(item.id) === String(route.params.id)) || demoAnimals[0]
    }
  } catch {
    animal.value = demoAnimals.find((item) => String(item.id) === String(route.params.id)) || demoAnimals[0]
  } finally {
    loading.value = false
  }
  fetchMedicalRecords()
})
</script>

<style scoped>
.medical-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.medical-card {
  padding: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-light);
}
.medical-head {
  display: flex;
  align-items: center;
  gap: 10px;
}
.medical-date {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
</style>
