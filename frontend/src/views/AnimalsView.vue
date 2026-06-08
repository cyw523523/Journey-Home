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
      <el-button :icon="Navigation" :loading="distanceLoading" @click="refreshDistance">
        {{ currentLocation ? '刷新距离' : '显示距离' }}
      </el-button>
    </div>
    <div v-if="currentLocation" class="distance-tip">
      <span>已根据你的位置计算送养距离</span>
      <span>经度 {{ currentLocation.longitude.toFixed(4) }}，纬度 {{ currentLocation.latitude.toFixed(4) }}</span>
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
          <el-input v-model="form.foundRegion" placeholder="例如：武汉市洪山区珞喻路附近" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="发现地经度">
              <el-input-number v-model="form.foundLongitude" :precision="6" :step="0.001" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="发现地纬度">
              <el-input-number v-model="form.foundLatitude" :precision="6" :step="0.001" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8" class="animal-location-action">
            <el-button :icon="LocateFixed" @click="fillCurrentLocation">使用当前位置</el-button>
          </el-col>
        </el-row>
        <el-form-item :label="$t('animals.healthCondition')">
          <el-input v-model="form.healthCondition" />
        </el-form-item>
        <el-form-item :label="$t('animals.photos')" prop="imageUrls">
          <ImageUploader v-model="form.imageUrls" usage="animal" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="item in creatableStatuses" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
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
import { LocateFixed, LogIn, Navigation, Plus, Search, Send } from 'lucide-vue-next'
import AnimalCard from '../components/AnimalCard.vue'
import EmptyState from '../components/EmptyState.vue'
import ImageUploader from '../components/ImageUploader.vue'
import { animalApi } from '../api'
import { useAiAssistantPageContext } from '../composables/useAiAssistantPageContext'
import { getBrowserLocation } from '../utils/amap'
import { notifyError } from '../api/http'
import { demoAnimals } from '../data/demoData'
import { animalStatusOptions, animalTypeOptions, genderOptions } from '../utils/status'
import { useAuth } from '../stores/auth'

const route = useRoute()
const { t } = useI18n()
const auth = useAuth()
const loading = ref(false)
const saving = ref(false)
const distanceLoading = ref(false)
const currentLocation = ref(loadCachedLocation())
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
  foundLongitude: null,
  foundLatitude: null,
  healthCondition: '', 
  imageUrls: [],
  description: '',
  status: null
})
const creatableStatuses = animalStatusOptions.filter(item =>
  ['WAITING_RESCUE', 'RESCUING', 'WAITING_ADOPTION', 'PENDING_REVIEW'].includes(item.value)
)
const rules = {
  type: [{ required: true, message: () => t('animals.selectAnimalType'), trigger: 'change' }],
  gender: [{ required: true, message: () => t('animals.selectGender'), trigger: 'change' }],
  foundRegion: [{ required: true, message: () => t('animals.inputRegion'), trigger: 'blur' }],
  imageUrls: [{ type: 'array', required: true, min: 1, message: () => t('animals.uploadPhoto'), trigger: 'change' }]
}

useAiAssistantPageContext(() => ({
  pageTitle: t('animals.title'),
  pageSummary: t('animals.description'),
  viewData: {
    filters: { ...filters },
    total: total.value,
    currentLocation: currentLocation.value,
    visibleAnimals: animals.value.slice(0, 6).map((item) => ({
      id: item.id,
      typeText: item.typeText,
      genderText: item.genderText,
      statusText: item.statusText,
      foundRegion: item.foundRegion,
      distanceKm: item.distanceKm ?? null
    }))
  }
}))

async function load() {
  loading.value = true
  try {
    const data = await animalApi.list({
      ...filters,
      page: page.value - 1,
      size: pageSize
    })
    if (data.content && data.content.length > 0) {
      animals.value = withDistance(data.content)
      total.value = data.totalElements || 0
    } else {
      animals.value = withDistance(demoAnimals)
      total.value = demoAnimals.length
    }
  } catch {
    animals.value = withDistance(demoAnimals)
    total.value = demoAnimals.length
  } finally {
    loading.value = false
  }
}

function loadCachedLocation() {
  try {
    const raw = localStorage.getItem('guitu:lastLocation')
    if (!raw) return null
    const parsed = JSON.parse(raw)
    if (Number.isFinite(parsed.latitude) && Number.isFinite(parsed.longitude)) {
      return parsed
    }
  } catch {
    localStorage.removeItem('guitu:lastLocation')
  }
  return null
}

function cacheLocation(location) {
  localStorage.setItem('guitu:lastLocation', JSON.stringify(location))
}

function withDistance(list) {
  return list.map((animal) => ({
    ...animal,
    distanceKm: calcDistanceKm(animal)
  }))
}

function calcDistanceKm(animal) {
  if (!currentLocation.value) return null
  const lat = Number(animal.foundLatitude)
  const lng = Number(animal.foundLongitude)
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) return null

  const toRad = (value) => (value * Math.PI) / 180
  const earthRadiusKm = 6371.0088
  const dLat = toRad(lat - currentLocation.value.latitude)
  const dLng = toRad(lng - currentLocation.value.longitude)
  const a = Math.sin(dLat / 2) ** 2
    + Math.cos(toRad(currentLocation.value.latitude)) * Math.cos(toRad(lat)) * Math.sin(dLng / 2) ** 2
  return Number((earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))).toFixed(2))
}

async function refreshDistance() {
  distanceLoading.value = true
  try {
    const location = await getBrowserLocation()
    currentLocation.value = location
    cacheLocation(location)
    animals.value = withDistance(animals.value)
    ElMessage.success('已计算你与送养动物的距离')
  } catch (error) {
    notifyError(error)
  } finally {
    distanceLoading.value = false
  }
}

async function fillCurrentLocation() {
  try {
    const location = await getBrowserLocation()
    form.foundLongitude = Number(location.longitude.toFixed(6))
    form.foundLatitude = Number(location.latitude.toFixed(6))
    ElMessage.success('已填入当前位置坐标')
  } catch (error) {
    notifyError(error)
  }
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    await animalApi.create(form)
    ElMessage.success(t('animals.submitSuccess'))
    dialogVisible.value = false
    Object.assign(form, { type: 'CAT', gender: 'UNKNOWN', age: 0, foundRegion: '', foundLongitude: null, foundLatitude: null, healthCondition: '', imageUrls: [], description: '', status: null })
    load()
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  load()
})
</script>


<style scoped>
.animal-location-action {
  display: flex;
  align-items: end;
  padding-bottom: 18px;
}

.animal-location-action .el-button {
  width: 100%;
}

.distance-tip {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  align-items: center;
  margin: -8px 0 18px;
  padding: 10px 14px;
  border: 1px solid rgba(31, 138, 112, 0.16);
  border-radius: 999px;
  background: rgba(31, 138, 112, 0.08);
  color: #245c50;
  font-size: 13px;
  font-weight: 700;
}

@media (max-width: 768px) {
  .animal-location-action {
    padding-bottom: 0;
  }
}
</style>
