<template>
  <section class="view page">
    <div class="section-head">
      <div>
        <h1>{{ $t('animals.title') }}</h1>
        <p>{{ $t('animals.description') }}</p>
      </div>
      <el-button v-if="auth.isLoggedIn.value" :icon="Plus" type="primary" size="large" @click="dialogVisible = true">
        {{ $t('animals.publishRecord') }}
      </el-button>
      <el-button v-else :icon="LogIn" size="large" @click="$router.push('/auth')">{{ $t('animals.loginToPublish') }}</el-button>
    </div>

    <div class="toolbar tool-panel">
      <el-input v-model="filters.keyword" :placeholder="$t('animals.keyword')" clearable @keyup.enter="load" />
      <el-select v-model="filters.type" :placeholder="$t('animals.type')" clearable>
        <el-option v-for="item in animalTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-select v-model="filters.gender" :placeholder="$t('animals.gender')" clearable>
        <el-option v-for="item in genderOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-select v-model="filters.status" :placeholder="$t('animals.status')" clearable>
        <el-option v-for="item in publicAnimalStatuses" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-input v-model="filters.region" :placeholder="$t('animals.region')" clearable @keyup.enter="load" />
      <el-button :icon="Search" type="primary" @click="load">{{ $t('animals.filter') }}</el-button>
    </div>

    <el-skeleton v-if="loading" :rows="6" animated />
    <div v-else-if="animals.length" class="grid animal-grid">
      <AnimalCard v-for="animal in animals" :key="animal.id" :animal="animal" />
    </div>
    <EmptyState
      v-else
      :title="$t('animals.noAnimals')"
      :description="$t('animals.noAnimalsDesc')"
    />

    <div v-if="total > pageSize" style="display: flex; justify-content: center; margin-top: 24px">
      <el-pagination v-model:current-page="page" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="load" />
    </div>

    <el-dialog v-model="dialogVisible" :title="$t('animals.publishDialogTitle')" width="720px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item :label="$t('animals.animalType')" prop="type">
              <el-select v-model="form.type" style="width: 100%">
                <el-option v-for="item in animalTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('animals.gender')" prop="gender">
              <el-select v-model="form.gender" style="width: 100%">
                <el-option v-for="item in genderOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('animals.age')" prop="age">
              <el-input-number v-model="form.age" :min="0" :max="30" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item :label="$t('animals.foundRegion')" prop="foundRegion">
          <el-input v-model="form.foundRegion" />
        </el-form-item>
        <el-form-item :label="$t('animals.healthCondition')">
          <el-input v-model="form.healthCondition" />
        </el-form-item>
        <el-form-item :label="$t('animals.photos')" prop="imageUrls">
          <ImageUploader v-model="form.imageUrls" usage="animal" />
        </el-form-item>
        <el-form-item :label="$t('animals.description')">
          <el-input v-model="form.description" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button :loading="saving" :icon="Send" type="primary" @click="submit">{{ $t('animals.submitForReview') }}</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
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
const { t } = useI18n()
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
  type: [{ required: true, message: () => t('animals.selectAnimalType'), trigger: 'change' }],
  gender: [{ required: true, message: () => t('animals.selectGender'), trigger: 'change' }],
  foundRegion: [{ required: true, message: () => t('animals.inputRegion'), trigger: 'blur' }],
  imageUrls: [{ type: 'array', required: true, min: 1, message: () => t('animals.uploadPhoto'), trigger: 'change' }]
}

async function load() {
  loading.value = true
  try {
    const data = await animalApi.list({
      ...filters,
      page: page.value - 1,
      size: pageSize
    })
    if (data.content && data.content.length > 0) {
      animals.value = data.content
      total.value = data.totalElements || 0
    } else {
      animals.value = demoAnimals
      total.value = demoAnimals.length
    }
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
    ElMessage.success(t('animals.submitSuccess'))
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
