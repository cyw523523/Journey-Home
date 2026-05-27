<template>
  <section class="view page">
    <div class="section-head">
      <div>
        <h1>{{ t('adminRescueStationsPage.title') }}</h1>
        <p>{{ t('adminRescueStationsPage.description') }}</p>
      </div>
      <el-button :icon="RefreshCw" @click="loadStations">{{ t('common.refresh') }}</el-button>
    </div>

    <div class="surface form-shell">
      <div style="display:flex;gap:10px;margin-bottom:12px">
        <el-select v-model="stationStatusFilter" clearable style="width: 180px" @change="loadStations">
          <el-option :label="t('adminRescueStationsPage.all')" value="" />
          <el-option v-for="item in statusFilterOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-input v-model="searchKeyword" :placeholder="t('adminRescueStationsPage.searchPlaceholder')" style="width: 220px" @keyup.enter="loadStations">
          <template #append><Search :size="16" /></template>
        </el-input>
        <el-button :icon="Search" text @click="loadStations">{{ t('common.search') }}</el-button>
      </div>

      <el-table :data="stations" stripe>
        <el-table-column prop="id" :label="t('admin.id')" width="70" />
        <el-table-column prop="stationName" :label="t('adminRescueStationsPage.stationName')" min-width="160" />
        <el-table-column prop="nickname" :label="t('admin.applicant')" width="120" />
        <el-table-column prop="address" :label="t('common.region')" min-width="140" show-overflow-tooltip />
        <el-table-column prop="contactPhone" :label="t('common.contact')" width="120" />
        <el-table-column :label="t('adminRescueStationsPage.certificationStatus')" width="120">
          <template #default="{ row }">
            <StatusTag :value="row.certificationStatus" :text="row.certificationStatusText"
                       :options="certStatusOptions" size="small" />
          </template>
        </el-table-column>
        <el-table-column :label="t('adminRescueStationsPage.followers')" width="70">
          <template #default="{ row }">{{ row.followerCount }}</template>
        </el-table-column>
        <el-table-column :label="t('adminRescueStationsPage.appliedAt')" width="160">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column :label="t('admin.action')" width="240">
          <template #default="{ row }">
            <el-button size="small" :icon="Eye" text @click="openDetail(row)">{{ t('common.details') }}</el-button>
            <el-button v-if="row.certificationStatus === 'PENDING'" size="small" :icon="Check" type="primary"
                       text @click="openCertify(row, 'APPROVED')">{{ t('admin.approve') }}</el-button>
            <el-button v-if="row.certificationStatus === 'PENDING'" size="small" :icon="X" type="danger"
                       text @click="openCertify(row, 'REJECTED')">{{ t('admin.reject') }}</el-button>
            <el-button v-if="row.certificationStatus === 'REJECTED'" size="small" :icon="RotateCcw"
                       text @click="openCertify(row, 'APPROVED')">{{ t('adminRescueStationsPage.reviewAgain') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="detailDialog" :title="t('adminRescueStationsPage.detailTitle')" width="680px" append-to-body>
      <div v-if="detailData" class="audit-detail-grid">
        <div class="detail-item"><label>{{ t('adminRescueStationsPage.stationName') }}</label><span>{{ detailData.stationName }}</span></div>
        <div class="detail-item"><label>{{ t('admin.applicant') }}</label><span>{{ detailData.nickname }}</span></div>
        <div class="detail-item"><label>{{ t('common.contact') }}</label><span>{{ detailData.contactPhone }}</span></div>
        <div class="detail-item"><label>{{ t('common.region') }}</label><span>{{ detailData.address }}</span></div>
        <div class="detail-item"><label>{{ t('adminRescueStationsPage.certificationStatus') }}</label><StatusTag :value="detailData.certificationStatus" :text="detailData.certificationStatusText" :options="certStatusOptions" /></div>
        <div class="detail-item"><label>{{ t('adminRescueStationsPage.followers') }}</label><span>{{ detailData.followerCount }}</span></div>
        <div class="detail-item"><label>{{ t('adminRescueStationsPage.appliedAt') }}</label><span>{{ formatTime(detailData.createdAt) }}</span></div>
        <div class="detail-item full-width"><label>{{ t('common.description') }}</label><p>{{ detailData.description || '-' }}</p></div>
        <div v-if="detailData.rejectReason" class="detail-item full-width"><label>{{ t('adminRescueStationsPage.rejectReason') }}</label><p>{{ detailData.rejectReason }}</p></div>
        <div v-if="detailData.imageUrl" class="detail-item full-width"><label>{{ t('adminRescueStationsPage.coverImage') }}</label><img :src="detailData.imageUrl" style="max-width:200px;border-radius:8px" /></div>
      </div>
      <template #footer>
        <el-button @click="detailDialog = false">{{ t('common.close') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="certDialog" :title="t('adminRescueStationsPage.certifyTitle')" width="560px" append-to-body>
      <div v-if="certTarget" style="margin-bottom: 12px">
        <p><strong>{{ certTarget.stationName }}</strong></p>
        <p class="muted">{{ t('admin.applicant') }}：{{ certTarget.nickname }}</p>
        <p class="muted">{{ t('common.region') }}：{{ certTarget.address }}</p>
        <p class="muted">{{ t('common.contact') }}：{{ certTarget.contactPhone }}</p>
      </div>
      <el-form :model="certForm" label-position="top">
        <el-form-item :label="t('adminRescueStationsPage.certifyResult')">
          <el-select v-model="certForm.action" style="width: 100%">
            <el-option v-for="item in certActionOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('admin.auditOpinion')">
          <el-input v-model="certForm.opinion" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="certDialog = false">{{ t('common.cancel') }}</el-button>
        <el-button :loading="saving" type="primary" @click="submitCertify">{{ t('common.submit') }}</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { Check, Eye, RefreshCw, RotateCcw, Search, X } from 'lucide-vue-next'
import StatusTag from '../components/StatusTag.vue'
import { rescueStationApi } from '../api'
import { notifyError } from '../api/http'
import { certificationOptions as certStatusOptions, optionText } from '../utils/status'

const { locale, t } = useI18n()
const stations = ref([])
const stationStatusFilter = ref('')
const searchKeyword = ref('')
const saving = ref(false)

const detailDialog = ref(false)
const detailData = ref(null)

const certDialog = ref(false)
const certTarget = ref(null)
const certForm = reactive({ action: 'APPROVED', opinion: '' })

const statusFilterOptions = computed(() => {
  void locale.value
  return ['PENDING', 'APPROVED', 'REJECTED'].map((value) => ({
    value,
    label: optionText(certStatusOptions, value)
  }))
})

const certActionOptions = computed(() => {
  void locale.value
  return [
    { value: 'APPROVED', label: t('admin.approve') },
    { value: 'REJECTED', label: t('admin.reject') }
  ]
})

async function loadStations() {
  try {
    const data = await rescueStationApi.adminList({
      status: stationStatusFilter.value || undefined,
      page: 0,
      size: 50
    })
    let result = data.content || []
    if (searchKeyword.value) {
      result = result.filter(item => item.stationName?.toLowerCase().includes(searchKeyword.value.toLowerCase()))
    }
    stations.value = result
  } catch (error) {
    notifyError(error)
  }
}

function openDetail(row) {
  detailData.value = row
  detailDialog.value = true
}

function openCertify(row, action) {
  certTarget.value = row
  certForm.action = action
  certForm.opinion = ''
  certDialog.value = true
}

async function submitCertify() {
  saving.value = true
  try {
    await rescueStationApi.certify(certTarget.value.userId, { status: certForm.action, reason: certForm.opinion })
    ElMessage.success(certForm.action === 'APPROVED' ? t('adminRescueStationsPage.approvedSuccess') : t('adminRescueStationsPage.rejectedSuccess'))
    certDialog.value = false
    await loadStations()
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

function formatTime(value) {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString(locale.value === 'zh' ? 'zh-CN' : 'en-US')
}

onMounted(loadStations)
</script>

<style scoped>
.audit-detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.detail-item.full-width {
  grid-column: 1 / -1;
}
.detail-item label {
  font-weight: 600;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.muted {
  color: var(--el-text-color-secondary);
}
</style>
