<template>
  <section class="view page">
    <div class="section-head">
      <div>
        <h1>救助站管理</h1>
        <p>管理所有救助站认证申请，查看认证状态和审核记录</p>
      </div>
      <el-button :icon="RefreshCw" @click="loadStations">刷新</el-button>
    </div>

    <div class="surface form-shell">
      <div style="display:flex;gap:10px;margin-bottom:12px">
        <el-select v-model="stationStatusFilter" clearable style="width: 180px" @change="loadStations">
          <el-option label="全部" value="" />
          <el-option label="待审核" value="PENDING" />
          <el-option label="已认证" value="APPROVED" />
          <el-option label="未通过" value="REJECTED" />
        </el-select>
        <el-input v-model="searchKeyword" placeholder="搜索救助站名称" style="width: 220px" @keyup.enter="loadStations">
          <template #append><Search :size="16" /></template>
        </el-input>
        <el-button :icon="Search" text @click="loadStations">搜索</el-button>
      </div>

      <el-table :data="stations" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="stationName" label="救助站名称" min-width="160" />
        <el-table-column prop="nickname" label="申请人" width="120" />
        <el-table-column prop="address" label="地址" min-width="140" show-overflow-tooltip />
        <el-table-column prop="contactPhone" label="联系电话" width="120" />
        <el-table-column label="认证状态" width="120">
          <template #default="{ row }">
            <StatusTag :value="row.certificationStatus" :text="row.certificationStatusText"
                       :options="certStatusOptions" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="粉丝" width="70">
          <template #default="{ row }">{{ row.followerCount }}</template>
        </el-table-column>
        <el-table-column label="申请时间" width="160">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240">
          <template #default="{ row }">
            <el-button size="small" :icon="Eye" text @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.certificationStatus === 'PENDING'" size="small" :icon="Check" type="primary"
                       text @click="openCertify(row, 'APPROVED')">通过</el-button>
            <el-button v-if="row.certificationStatus === 'PENDING'" size="small" :icon="X" type="danger"
                       text @click="openCertify(row, 'REJECTED')">驳回</el-button>
            <el-button v-if="row.certificationStatus === 'REJECTED'" size="small" :icon="RotateCcw"
                       text @click="openCertify(row, 'APPROVED')">重新审核</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="detailDialog" title="救助站详情" width="680px" append-to-body>
      <div v-if="detailData" class="audit-detail-grid">
        <div class="detail-item"><label>救助站名称</label><span>{{ detailData.stationName }}</span></div>
        <div class="detail-item"><label>申请人</label><span>{{ detailData.nickname }}</span></div>
        <div class="detail-item"><label>联系电话</label><span>{{ detailData.contactPhone }}</span></div>
        <div class="detail-item"><label>地址</label><span>{{ detailData.address }}</span></div>
        <div class="detail-item"><label>认证状态</label><StatusTag :value="detailData.certificationStatus" :text="detailData.certificationStatusText" :options="certStatusOptions" /></div>
        <div class="detail-item"><label>粉丝数</label><span>{{ detailData.followerCount }}</span></div>
        <div class="detail-item"><label>申请时间</label><span>{{ formatTime(detailData.createdAt) }}</span></div>
        <div class="detail-item full-width"><label>简介</label><p>{{ detailData.description || '-' }}</p></div>
        <div v-if="detailData.rejectReason" class="detail-item full-width"><label>驳回理由</label><p>{{ detailData.rejectReason }}</p></div>
        <div v-if="detailData.imageUrl" class="detail-item full-width"><label>封面图片</label><img :src="detailData.imageUrl" style="max-width:200px;border-radius:8px" /></div>
      </div>
      <template #footer>
        <el-button @click="detailDialog = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="certDialog" title="救助站认证" width="560px" append-to-body>
      <div v-if="certTarget" style="margin-bottom: 12px">
        <p><strong>{{ certTarget.stationName }}</strong></p>
        <p class="muted">申请人：{{ certTarget.nickname }}</p>
        <p class="muted">地址：{{ certTarget.address }}</p>
        <p class="muted">联系电话：{{ certTarget.contactPhone }}</p>
      </div>
      <el-form :model="certForm" label-position="top">
        <el-form-item label="认证结果">
          <el-select v-model="certForm.action" style="width: 100%">
            <el-option label="通过" value="APPROVED" />
            <el-option label="驳回" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理意见">
          <el-input v-model="certForm.opinion" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="certDialog = false">取消</el-button>
        <el-button :loading="saving" type="primary" @click="submitCertify">提交</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Eye, RefreshCw, RotateCcw, Search, X } from 'lucide-vue-next'
import StatusTag from '../components/StatusTag.vue'
import { rescueStationApi } from '../api'
import { notifyError } from '../api/http'
import { certificationOptions as certStatusOptions } from '../utils/status'

const stations = ref([])
const stationStatusFilter = ref('')
const searchKeyword = ref('')
const saving = ref(false)

const detailDialog = ref(false)
const detailData = ref(null)

const certDialog = ref(false)
const certTarget = ref(null)
const certForm = reactive({ action: 'APPROVED', opinion: '' })

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
    ElMessage.success(certForm.action === 'APPROVED' ? '已通过认证' : '已驳回')
    certDialog.value = false
    await loadStations()
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

function formatTime(value) {
  return value ? new Date(value).toLocaleString('zh-CN') : '-'
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
