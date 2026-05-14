<template>
  <section class="view page">
    <div class="section-head">
      <div>
        <h1>{{ $t('admin.title') }}</h1>
        <p>{{ $t('admin.description') }}</p>
      </div>
      <el-button :icon="RefreshCw" @click="loadAll">{{ $t('common.refresh') }}</el-button>
    </div>

    <div class="admin-layout">
      <aside class="admin-menu surface">
        <el-menu v-model="active" @select="active = $event">
          <el-menu-item index="dashboard"><ChartNoAxesCombined />{{ $t('admin.dashboard') }}</el-menu-item>
          <el-menu-item index="audits"><ClipboardCheck />{{ $t('admin.auditManagement') }}</el-menu-item>
          <el-menu-item index="users"><Users />{{ $t('admin.userManagement') }}</el-menu-item>
          <el-menu-item index="notices"><Megaphone />{{ $t('admin.noticeManagement') }}</el-menu-item>
          <el-menu-item index="applications"><HeartHandshake />{{ $t('admin.adoptionApplications') }}</el-menu-item>
        </el-menu>
      </aside>

      <main class="admin-content">
        <div v-show="active === 'dashboard'" class="surface form-shell">
          <div class="metric-grid">
            <div class="metric">
              <span>{{ $t('admin.userCount') }}</span>
              <strong>{{ overview.userCount }}</strong>
            </div>
            <div class="metric">
              <span>{{ $t('admin.animalCount') }}</span>
              <strong>{{ overview.animalCount }}</strong>
            </div>
            <div class="metric">
              <span>{{ $t('admin.rescueCount') }}</span>
              <strong>{{ overview.rescueCount }}</strong>
            </div>
            <div class="metric">
              <span>{{ $t('admin.applyCount') }}</span>
              <strong>{{ overview.applyCount }}</strong>
            </div>
            <div class="metric">
              <span>{{ $t('admin.pendingAuditCount') }}</span>
              <strong>{{ overview.pendingAuditCount }}</strong>
            </div>
          </div>
          <el-row :gutter="14" style="margin-top: 16px">
            <el-col :md="8" :sm="24">
              <h3>{{ $t('admin.animalStatus') }}</h3>
              <div v-for="item in animalStatus" :key="item.status" class="mini-row">
                <span>{{ item.statusText }}<strong>{{ item.count }}</strong></span>
              </div>
            </el-col>
            <el-col :md="8" :sm="24">
              <h3>{{ $t('admin.rescueStatus') }}</h3>
              <div v-for="item in rescueStatus" :key="item.status" class="mini-row">
                <span>{{ item.statusText }}<strong>{{ item.count }}</strong></span>
              </div>
            </el-col>
            <el-col :md="8" :sm="24">
              <h3>{{ $t('admin.applyStatus') }}</h3>
              <div v-for="item in applyStatus" :key="item.status" class="mini-row">
                <span>{{ item.statusText }}<strong>{{ item.count }}</strong></span>
              </div>
            </el-col>
          </el-row>
        </div>

        <div v-show="active === 'audits'" class="surface form-shell">
          <div style="display: flex; gap: 10px; margin-bottom: 12px">
            <el-select v-model="auditType" :placeholder="$t('admin.auditType')" clearable style="width: 220px" @change="loadPending">
              <el-option :label="$t('admin.animalRecord')" value="ANIMAL" />
              <el-option :label="$t('admin.rescueInfo')" value="RESCUE" />
              <el-option :label="$t('admin.adoptApply')" value="ADOPT_APPLY" />
            </el-select>
            <el-button :icon="RefreshCw" @click="loadPending">{{ $t('common.refresh') }}</el-button>
          </div>
          <el-table :data="pending" stripe>
            <el-table-column prop="targetType" :label="$t('common.type')" width="140" />
            <el-table-column prop="targetId" :label="$t('admin.businessId')" width="100" />
            <el-table-column prop="title" :label="$t('admin.content')" />
            <el-table-column prop="publisherOrApplicant" :label="$t('admin.publisherOrApplicant')" width="150" />
            <el-table-column :label="$t('admin.action')" width="310">
              <template #default="{ row }">
                <el-button size="small" :icon="Eye" text type="info" @click="openDetail(row)">详情</el-button>
                <el-button size="small" :icon="Check" type="primary" @click="openAudit(row, 'APPROVE')">{{ $t('admin.approve') }}</el-button>
                <el-button size="small" :icon="X" @click="openAudit(row, 'REJECT')">{{ $t('admin.reject') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-show="active === 'users'" class="surface form-shell">
          <el-table :data="usersList" stripe>
            <el-table-column prop="id" :label="$t('admin.id')" width="80" />
            <el-table-column prop="account" :label="$t('auth.account')" />
            <el-table-column prop="nickname" :label="$t('admin.nickname')" />
            <el-table-column prop="phone" :label="$t('admin.phoneNumber')" />
            <el-table-column :label="$t('admin.role')" width="150">
              <template #default="{ row }">
                <el-select v-model="row.role" size="small" @change="updateUser(row)">
                  <el-option v-for="item in roleOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column :label="$t('admin.status')" width="140">
              <template #default="{ row }">
                <el-select v-model="row.status" size="small" @change="updateUser(row)">
                  <el-option v-for="item in userStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-show="active === 'notices'" class="surface form-shell">
          <div style="display: flex; justify-content: flex-end; margin-bottom: 12px">
            <el-button :icon="Plus" type="primary" @click="openNotice()">{{ $t('admin.addNotice') }}</el-button>
          </div>
          <el-table :data="notices" stripe>
            <el-table-column prop="id" :label="$t('admin.id')" width="80" />
            <el-table-column prop="title" :label="$t('admin.noticeTitle')" />
            <el-table-column :label="$t('admin.status')" width="120">
              <template #default="{ row }">
                <StatusTag :value="row.status" :text="row.statusText" :options="noticeStatusOptions" />
              </template>
            </el-table-column>
            <el-table-column :label="$t('admin.action')" width="220">
              <template #default="{ row }">
                <el-button size="small" :icon="Pencil" @click="openNotice(row)">{{ $t('common.edit') }}</el-button>
                <el-button size="small" :icon="Archive" @click="offlineNotice(row)">{{ $t('admin.offline') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-show="active === 'applications'" class="surface form-shell">
          <el-table :data="applications" stripe>
            <el-table-column prop="id" :label="$t('admin.id')" width="80" />
            <el-table-column prop="applicantName" :label="$t('admin.applicant')" width="130" />
            <el-table-column prop="animalTypeText" :label="$t('admin.animal')" width="100" />
            <el-table-column prop="reason" :label="$t('admin.reason')" />
            <el-table-column :label="$t('admin.status')" width="120">
              <template #default="{ row }">
                <StatusTag :value="row.status" :text="row.statusText" :options="applyStatusOptions" />
              </template>
            </el-table-column>
          </el-table>
        </div>
      </main>
    </div>

    <el-dialog v-model="auditDialog" :title="$t('admin.auditDialogTitle')" width="520px" append-to-body>
      <el-form :model="auditForm" label-position="top">
        <el-form-item :label="$t('admin.auditResult')">
          <el-select v-model="auditForm.action" style="width: 100%">
            <el-option :label="$t('admin.approve')" value="APPROVE" />
            <el-option :label="$t('admin.reject')" value="REJECT" />
            <el-option v-if="auditForm.targetType !== 'ADOPT_APPLY'" :label="$t('admin.offline')" value="OFFLINE" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('admin.auditOpinion')">
          <el-input v-model="auditForm.opinion" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditDialog = false">{{ $t('common.cancel') }}</el-button>
        <el-button :loading="saving" :icon="Send" type="primary" @click="submitAudit">{{ $t('common.submit') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialog" :title="'审核详情 - ' + detailTargetType" width="720px" append-to-body>
      <el-skeleton v-if="detailLoading" :rows="8" animated />
      <template v-else-if="detailData">
        <div v-if="detailTargetType === 'ANIMAL'" class="audit-detail-grid">
          <div class="detail-item"><label>类型</label><span>{{ detailData.typeText }}</span></div>
          <div class="detail-item"><label>性别</label><span>{{ detailData.genderText }}</span></div>
          <div class="detail-item"><label>年龄</label><span>{{ detailData.age ?? '未知' }}</span></div>
          <div class="detail-item"><label>发现地区</label><span>{{ detailData.foundRegion }}</span></div>
          <div class="detail-item"><label>健康情况</label><span>{{ detailData.healthCondition || '-' }}</span></div>
          <div class="detail-item"><label>状态</label><StatusTag :value="detailData.status" :text="detailData.statusText" :options="animalStatusOptions" /></div>
          <div class="detail-item"><label>发布者</label><span>{{ detailData.publisherNickname }}</span></div>
          <div class="detail-item full-width"><label>描述</label><p>{{ detailData.description || '-' }}</p></div>
          <div class="detail-item full-width" v-if="detailData.coverImageUrl">
            <label>封面图</label><img :src="getFullUrl(detailData.coverImageUrl)" style="max-width:200px;border-radius:8px" />
          </div>
          <div class="detail-item full-width" v-if="detailData.imageUrls?.length">
            <label>图片集</label>
            <div class="detail-thumb-row">
              <img v-for="url in detailData.imageUrls" :key="url" :src="getFullUrl(url)" style="width:80px;height:80px;object-fit:cover;border-radius:6px;margin-right:6px" />
            </div>
          </div>
        </div>
        <div v-else-if="detailTargetType === 'RESCUE'" class="audit-detail-grid">
          <div class="detail-item"><label>地点</label><span>{{ detailData.location }}</span></div>
          <div class="detail-item"><label>动物情况</label><span>{{ detailData.animalCondition }}</span></div>
          <div class="detail-item"><label>联系方式</label><span>{{ detailData.contact }}</span></div>
          <div class="detail-item"><label>状态</label><StatusTag :value="detailData.status" :text="detailData.statusText" :options="rescueStatusOptions" /></div>
          <div class="detail-item"><label>发布者</label><span>{{ detailData.publisherNickname }}</span></div>
          <div class="detail-item full-width"><label>描述</label><p>{{ detailData.description }}</p></div>
          <div class="detail-item full-width" v-if="detailData.imageUrls?.length">
            <label>图片集</label>
            <div class="detail-thumb-row">
              <img v-for="url in detailData.imageUrls" :key="url" :src="getFullUrl(url)" style="width:80px;height:80px;object-fit:cover;border-radius:6px;margin-right:6px" />
            </div>
          </div>
        </div>
        <div v-else-if="detailTargetType === 'ADOPT_APPLY'" class="audit-detail-grid">
          <div class="detail-item"><label>动物ID</label><span>{{ detailData.animalId }}</span></div>
          <div class="detail-item"><label>动物类型</label><span>{{ detailData.animalTypeText }}</span></div>
          <div class="detail-item"><label>申请人</label><span>{{ detailData.applicantName }}</span></div>
          <div class="detail-item"><label>联系方式</label><span>{{ detailData.contact }}</span></div>
          <div class="detail-item"><label>状态</label><StatusTag :value="detailData.status" :text="detailData.statusText" :options="applyStatusOptions" /></div>
          <div class="detail-item full-width"><label>领养理由</label><p>{{ detailData.reason }}</p></div>
          <div class="detail-item full-width"><label>居住条件</label><p>{{ detailData.livingCondition }}</p></div>
          <div class="detail-item full-width"><label>饲养经验</label><p>{{ detailData.experience }}</p></div>
          <div class="detail-item full-width" v-if="detailData.auditOpinion"><label>审核意见</label><p>{{ detailData.auditOpinion }}</p></div>
        </div>
      </template>
      <template #footer>
        <el-button @click="detailDialog = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="noticeDialog" :title="$t('admin.noticeDialogTitle')" width="680px" append-to-body>
      <el-form :model="noticeForm" label-position="top">
        <el-form-item :label="$t('admin.noticeTitle')">
          <el-input v-model="noticeForm.title" />
        </el-form-item>
        <el-form-item :label="$t('admin.status')">
          <el-select v-model="noticeForm.status" style="width: 100%">
            <el-option v-for="item in noticeStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('admin.noticeContent')">
          <el-input v-model="noticeForm.content" type="textarea" :rows="6" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="noticeDialog = false">{{ $t('common.cancel') }}</el-button>
        <el-button :loading="saving" :icon="Save" type="primary" @click="saveNotice">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import {
  Archive,
  ChartNoAxesCombined,
  Check,
  ClipboardCheck,
  Eye,
  HeartHandshake,
  Megaphone,
  Pencil,
  Plus,
  RefreshCw,
  Save,
  Send,
  Users,
  X
} from 'lucide-vue-next'
import StatusTag from '../components/StatusTag.vue'
import { adminApi } from '../api'
import { notifyError } from '../api/http'
import { animalStatusOptions, applyStatusOptions, noticeStatusOptions, rescueStatusOptions, roleOptions, userStatusOptions } from '../utils/status'

const { t } = useI18n()
const active = ref('dashboard')
const saving = ref(false)
const overview = ref({ userCount: 0, animalCount: 0, rescueCount: 0, applyCount: 0, pendingAuditCount: 0 })
const animalStatus = ref([])
const rescueStatus = ref([])
const applyStatus = ref([])
const pending = ref([])
const usersList = ref([])
const notices = ref([])
const applications = ref([])
const auditType = ref('')
const auditDialog = ref(false)
const noticeDialog = ref(false)
const auditForm = reactive({ targetType: '', targetId: null, action: 'APPROVE', opinion: '' })
const noticeForm = reactive({ id: null, title: '', content: '', status: 'DRAFT' })
const detailDialog = ref(false)
const detailLoading = ref(false)
const detailData = ref(null)
const detailTargetType = ref('')

async function loadAll() {
  await Promise.all([loadDashboard(), loadPending(), loadUsers(), loadNotices(), loadApplications()])
}

async function loadDashboard() {
  try {
    const [overviewData, animalData, rescueData, applyData] = await Promise.all([
      adminApi.overview(),
      adminApi.animalStatus(),
      adminApi.rescueStatus(),
      adminApi.applyStatus()
    ])
    overview.value = overviewData
    animalStatus.value = animalData
    rescueStatus.value = rescueData
    applyStatus.value = applyData
  } catch (error) {
    notifyError(error)
  }
}

async function loadPending() {
  try {
    pending.value = await adminApi.pending({ targetType: auditType.value || undefined, page: 0, size: 20 })
  } catch (error) {
    notifyError(error)
  }
}

async function loadUsers() {
  try {
    usersList.value = (await adminApi.users({ page: 0, size: 30 })).content || []
  } catch (error) {
    notifyError(error)
  }
}

async function loadNotices() {
  try {
    notices.value = (await adminApi.notices({ page: 0, size: 30 })).content || []
  } catch (error) {
    notifyError(error)
  }
}

async function loadApplications() {
  try {
    applications.value = (await adminApi.applications({ page: 0, size: 30 })).content || []
  } catch (error) {
    notifyError(error)
  }
}

const API_BASE = window.location.origin

function getFullUrl(url) {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:')) return url
  return `${API_BASE}${url}`
}

async function openDetail(row) {
  detailTargetType.value = row.targetType
  detailData.value = null
  detailLoading.value = true
  detailDialog.value = true
  try {
    detailData.value = await adminApi.auditDetail(row.targetType, row.targetId)
  } catch (error) {
    notifyError(error)
    detailDialog.value = false
  } finally {
    detailLoading.value = false
  }
}

function openAudit(row, action) {
  Object.assign(auditForm, {
    targetType: row.targetType,
    targetId: row.targetId,
    action,
    opinion: action === 'APPROVE' ? t('admin.defaultApproveOpinion') : t('admin.defaultRejectOpinion')
  })
  auditDialog.value = true
}

async function submitAudit() {
  saving.value = true
  try {
    await adminApi.audit(auditForm)
    ElMessage.success(t('admin.auditProcessed'))
    auditDialog.value = false
    await loadAll()
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

async function updateUser(row) {
  try {
    await adminApi.updateUser(row.id, { role: row.role, status: row.status })
    ElMessage.success(t('admin.userUpdated'))
  } catch (error) {
    notifyError(error)
  }
}

function openNotice(row) {
  Object.assign(noticeForm, row ? { id: row.id, title: row.title, content: row.content, status: row.status } : { id: null, title: '', content: '', status: 'DRAFT' })
  noticeDialog.value = true
}

async function saveNotice() {
  saving.value = true
  try {
    if (noticeForm.id) {
      await adminApi.updateNotice(noticeForm.id, noticeForm)
    } else {
      await adminApi.createNotice(noticeForm)
    }
    ElMessage.success(t('admin.noticeSaved'))
    noticeDialog.value = false
    loadNotices()
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

async function offlineNotice(row) {
  try {
    await adminApi.offlineNotice(row.id)
    ElMessage.success(t('admin.noticeOffline'))
    loadNotices()
  } catch (error) {
    notifyError(error)
  }
}

onMounted(loadAll)
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
.detail-thumb-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
</style>
