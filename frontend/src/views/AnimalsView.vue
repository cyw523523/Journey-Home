<template>
  <section class="view page">
    <div class="section-head">
      <div>
        <h1>动物档案</h1>
        <p>这里展示的是审核通过且允许公开查看的动物档案。你自己提交但还没审核通过的记录，请到个人中心查看。</p>
      </div>
      <el-button v-if="auth.isLoggedIn.value" :icon="Plus" type="primary" size="large" @click="dialogVisible = true">
        发布档案
      </el-button>
      <el-button v-else :icon="LogIn" size="large" @click="$router.push('/auth')">登录后发布</el-button>
    </div>

    <div class="toolbar tool-panel">
      <el-input v-model="filters.keyword" placeholder="关键词" clearable @keyup.enter="load" />
      <el-select v-model="filters.type" placeholder="类型" clearable>
        <el-option v-for="item in animalTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-select v-model="filters.gender" placeholder="性别" clearable>
        <el-option v-for="item in genderOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-select v-model="filters.status" placeholder="状态" clearable>
        <el-option v-for="item in publicAnimalStatuses" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-input v-model="filters.region" placeholder="地区" clearable @keyup.enter="load" />
      <el-button :icon="Search" type="primary" @click="load">筛选</el-button>
    </div>

    <el-skeleton v-if="loading" :rows="6" animated />
    <div v-else-if="animals.length" class="grid animal-grid">
      <AnimalCard v-for="animal in animals" :key="animal.id" :animal="animal" />
    </div>
    <EmptyState
      v-else
      title="当前还没有可公开查看的动物档案"
      description="如果你刚提交了档案，它会先进入待审核状态；审核通过后才会出现在这里。"
    />

    <div v-if="total > pageSize" style="display: flex; justify-content: center; margin-top: 24px">
      <el-pagination v-model:current-page="page" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="load" />
    </div>

    <el-dialog v-model="dialogVisible" title="发布动物档案" width="720px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="动物类型" prop="type">
              <el-select v-model="form.type" style="width: 100%">
                <el-option v-for="item in animalTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="性别" prop="gender">
              <el-select v-model="form.gender" style="width: 100%">
                <el-option v-for="item in genderOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="年龄" prop="age">
              <el-input-number v-model="form.age" :min="0" :max="30" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="发现地区" prop="foundRegion">
          <el-input v-model="form.foundRegion" />
        </el-form-item>
        <el-form-item label="健康情况">
          <el-input v-model="form.healthCondition" />
        </el-form-item>
        <el-form-item label="照片" prop="imageUrls">
          <ImageUploader v-model="form.imageUrls" usage="animal" />
        </el-form-item>
        <el-form-item label="详细说明">
          <el-input v-model="form.description" type="textarea" :rows="4" />
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
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { LogIn, Plus, Search, Send } from 'lucide-vue-next'
import AnimalCard from '../components/AnimalCard.vue'
import EmptyState from '../components/EmptyState.vue'
import ImageUploader from '../components/ImageUploader.vue'
import { animalApi } from '../api'
import { notifyError } from '../api/http'
import { demoAnimals } from '../data/demoData'
import { animalStatusOptions, animalTypeOptions, genderOptions } from '../utils/status'
import { useAuth } from '../stores/auth'

const route = useRoute()
const auth = useAuth()
const loading = ref(false)
const saving = ref(false)
const animals = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 9
const dialogVisible = ref(false)
const formRef = ref()

const publicAnimalStatuses = animalStatusOptions.filter((item) => ['WAITING_RESCUE', 'RESCUING', 'WAITING_ADOPTION', 'ADOPTED'].includes(item.value))
const filters = reactive({
  keyword: route.query.keyword || '',
  type: '',
  gender: '',
  status: '',
  region: ''
})
const form = reactive({
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

async function load() {
  loading.value = true
  try {
    const data = await animalApi.list({
      ...filters,
      page: page.value - 1,
      size: pageSize
    })
    animals.value = data.content || []
    total.value = data.totalElements || 0
  } catch {
    animals.value = demoAnimals
    total.value = demoAnimals.length
  } finally {
    loading.value = false
  }
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    await animalApi.create(form)
    ElMessage.success('提交成功，等待管理员审核')
    dialogVisible.value = false
    Object.assign(form, { type: 'CAT', gender: 'UNKNOWN', age: 0, foundRegion: '', healthCondition: '', imageUrls: [], description: '' })
    load()
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>
