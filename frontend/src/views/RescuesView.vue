<template>
  <section class="view page">
    <div class="section-head">
      <div>
        <h1>救助信息</h1>
        <p>发布发现地点、动物情况和联系方式，审核通过后公开展示。</p>
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
          <el-button :icon="Eye" @click="openDetail(rescue)">详情</el-button>
        </div>
      </article>
    </div>
    <EmptyState v-else title="暂无救助信息" description="可以发布一条救助信息，等待管理员审核后展示。" />

    <el-dialog v-model="detailVisible" title="救助详情" width="680px">
      <div v-if="current" class="form-shell">
        <StatusTag :value="current.status" :text="current.statusText" :options="rescueStatusOptions" />
        <h2>{{ current.location }}</h2>
        <p>{{ current.animalCondition }}</p>
        <p class="muted">{{ current.description }}</p>
        <div class="thumb-grid" v-if="current.imageUrls?.length">
          <img v-for="url in current.imageUrls" :key="url" :src="url" alt="救助图片" />
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="dialogVisible" title="发布救助信息" width="720px">
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
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Eye, LogIn, Phone, Plus, Search, Send } from 'lucide-vue-next'
import EmptyState from '../components/EmptyState.vue'
import ImageUploader from '../components/ImageUploader.vue'
import StatusTag from '../components/StatusTag.vue'
import { rescueApi } from '../api'
import { notifyError } from '../api/http'
import { demoRescues } from '../data/demoData'
import { rescueStatusOptions } from '../utils/status'
import { useAuth } from '../stores/auth'

const auth = useAuth()
const loading = ref(false)
const saving = ref(false)
const detailVisible = ref(false)
const dialogVisible = ref(false)
const current = ref(null)
const rescues = ref([])
const formRef = ref()
const publicRescueStatuses = rescueStatusOptions.filter((item) => ['PENDING_PROCESS', 'PROCESSING', 'COMPLETED'].includes(item.value))
const filters = reactive({ keyword: '', region: '', status: '' })
const form = reactive({ location: '', animalCondition: '', contact: '', description: '', imageUrls: [] })
const rules = {
  location: [{ required: true, message: '请输入救助地点', trigger: 'blur' }],
  animalCondition: [{ required: true, message: '请输入动物情况', trigger: 'blur' }],
  contact: [{ required: true, message: '请输入联系方式', trigger: 'blur' }],
  description: [{ required: true, message: '请输入求助说明', trigger: 'blur' }]
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

onMounted(load)
</script>
