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
        <el-menu :default-active="active" @select="val => active = val">
          <el-menu-item index="dashboard"><ChartNoAxesCombined />{{ $t('admin.dashboard') }}</el-menu-item>
          <el-menu-item index="audits"><ClipboardCheck />{{ $t('admin.auditManagement') }}</el-menu-item>
          <el-menu-item index="users"><Users />{{ $t('admin.userManagement') }}</el-menu-item>
          <el-menu-item index="notices"><Megaphone />{{ $t('admin.noticeManagement') }}</el-menu-item>
          <el-menu-item index="applications"><HeartHandshake />{{ $t('admin.adoptionApplications') }}</el-menu-item>
          <el-menu-item index="reports"><ShieldAlert />举报处理</el-menu-item>
          <el-menu-item index="appeals"><FileCheck2 />申诉复核</el-menu-item>
        </el-menu>
      </aside>

      <main class="admin-content">
        <div v-show="active === 'dashboard'" class="surface form-shell">
          <div class="metric-grid">
            <div class="metric"><span>{{ $t('admin.userCount') }}</span><strong>{{ overview.userCount }}</strong></div>
            <div class="metric"><span>{{ $t('admin.animalCount') }}</span><strong>{{ overview.animalCount }}</strong></div>
            <div class="metric"><span>{{ $t('admin.rescueCount') }}</span><strong>{{ overview.rescueCount }}</strong></div>
            <div class="metric"><span>{{ $t('admin.applyCount') }}</span><strong>{{ overview.applyCount }}</strong></div>
            <div class="metric"><span>{{ $t('admin.pendingAuditCount') }}</span><strong>{{ overview.pendingAuditCount }}</strong></div>
          </div>
          <el-row :gutter="14" style="margin-top: 16px">
            <el-col :md="8" :sm="24">
              <h3>动物状态</h3>
              <div v-for="item in animalStatus" :key="item.status" class="mini-row">
                <span>{{ item.statusText }}<strong>{{ item.count }}</strong></span>
              </div>
            </el-col>
            <el-col :md="8" :sm="24">
              <h3>救助状态</h3>
              <div v-for="item in rescueStatus" :key="item.status" class="mini-row">
                <span>{{ item.statusText }}<strong>{{ item.count }}</strong></span>
              </div>
            </el-col>
            <el-col :md="8" :sm="24">
              <h3>申请状态</h3>
              <div v-for="item in applyStatus" :key="item.status" class="mini-row">
                <span>{{ item.statusText }}<strong>{{ item.count }}</strong></span>
              </div>
            </el-col>
          </el-row>
        </div>

        <div v-show="active === 'audits'" class="surface form-shell">
          <div style="display:flex;gap:10px;margin-bottom:12px">
            <el-select v-model="auditType" clearable style="width: 220px" @change="loadPending">
              <el-option label="动物档案" value="ANIMAL" />
              <el-option label="救助信息" value="RESCUE" />
              <el-option label="领养申请" value="ADOPT_APPLY" />
              <el-option label="社区帖子" value="COMMUNITY_POST" />
              <el-option label="社区评论" value="COMMUNITY_COMMENT" />
            </el-select>
            <el-button :icon="RefreshCw" @click="loadPending">刷新</el-button>
          </div>
          <el-table :data="pending" stripe>
            <el-table-column prop="targetType" label="类型" width="160" />
            <el-table-column prop="targetId" label="业务ID" width="100" />
            <el-table-column prop="title" label="内容" />
            <el-table-column prop="publisherOrApplicant" label="发布者/申请人" width="180" />
            <el-table-column label="操作" width="320">
              <template #default="{ row }">
                <el-button size="small" :icon="Eye" text @click="openDetail(row)">详情</el-button>
                <el-button size="small" :icon="Check" type="primary" @click="openAudit(row, 'APPROVE')">通过</el-button>
                <el-button size="small" :icon="X" @click="openAudit(row, 'REJECT')">驳回</el-button>
                <el-button size="small" v-if="row.targetType !== 'ADOPT_APPLY'" @click="openAudit(row, 'OFFLINE')">下架</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-show="active === 'users'" class="surface form-shell">
          <el-table :data="usersList" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="account" label="账号" />
            <el-table-column prop="nickname" label="昵称" />
            <el-table-column prop="phone" label="手机号" />
            <el-table-column label="角色" width="150">
              <template #default="{ row }">
                <el-select v-model="row.role" size="small" @change="updateUser(row)">
                  <el-option v-for="item in roleOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="140">
              <template #default="{ row }">
                <el-select v-model="row.status" size="small" @change="updateUser(row)">
                  <el-option v-for="item in userStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-show="active === 'notices'" class="surface form-shell">
          <div style="display:flex;justify-content:flex-end;margin-bottom:12px">
            <el-button :icon="Plus" type="primary" @click="openNotice()">新增公告</el-button>
          </div>
          <el-table :data="notices" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="title" label="标题" />
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <StatusTag :value="row.status" :text="row.statusText" :options="noticeStatusOptions" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="220">
              <template #default="{ row }">
                <el-button size="small" :icon="Pencil" @click="openNotice(row)">编辑</el-button>
                <el-button size="small" :icon="Archive" @click="offlineNotice(row)">下架</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-show="active === 'applications'" class="surface form-shell">
          <el-table :data="applications" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="applicantName" label="申请人" width="130" />
            <el-table-column prop="animalTypeText" label="动物" width="100" />
            <el-table-column prop="reason" label="理由" />
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <StatusTag :value="row.status" :text="row.statusText" :options="applyStatusOptions" />
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-show="active === 'reports'" class="surface form-shell">
          <div style="display:flex;gap:10px;margin-bottom:12px">
            <el-select v-model="reportStatusFilter" clearable style="width: 180px" @change="loadReports">
              <el-option v-for="item in reportStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-select v-model="reportTypeFilter" clearable style="width: 180px" @change="loadReports">
              <el-option v-for="item in reportTargetOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </div>
          <el-table :data="reports" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="targetTypeText" label="对象" width="120" />
            <el-table-column prop="reasonTypeText" label="原因" width="120" />
            <el-table-column prop="description" label="说明" />
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <StatusTag :value="row.status" :text="row.statusText" :options="reportStatusOptions" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="220">
              <template #default="{ row }">
                <el-button size="small" :icon="Eye" text @click="openReportResolve(row)">处理</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-show="active === 'appeals'" class="surface form-shell">
          <div style="display:flex;gap:10px;margin-bottom:12px">
            <el-select v-model="appealStatusFilter" clearable style="width: 180px" @change="loadAppeals">
              <el-option v-for="item in appealStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-select v-model="appealTypeFilter" clearable style="width: 180px" @change="loadAppeals">
              <el-option v-for="item in appealTargetOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </div>
          <el-table :data="appeals" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="targetTypeText" label="对象" width="120" />
            <el-table-column prop="applicantNickname" label="申诉人" width="120" />
            <el-table-column prop="reason" label="申诉原因" />
            <el-table-column label="状态" width="150">
              <template #default="{ row }">
                <StatusTag :value="row.status" :text="row.statusText" :options="appealStatusOptions" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="220">
              <template #default="{ row }">
                <el-button size="small" :icon="Eye" text @click="openAppealReview(row)">复核</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </main>
    </div>

    <el-dialog v-model="auditDialog" title="审核处理" width="520px" append-to-body>
      <el-form :model="auditForm" label-position="top">
        <el-form-item label="审核结果">
          <el-select v-model="auditForm.action" style="width: 100%">
            <el-option label="通过" value="APPROVE" />
            <el-option label="驳回" value="REJECT" />
            <el-option v-if="auditForm.targetType !== 'ADOPT_APPLY'" label="下架" value="OFFLINE" />
          </el-select>
        </el-form-item>
        <el-form-item label="审核意见">
          <el-input v-model="auditForm.opinion" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditDialog = false">取消</el-button>
        <el-button :loading="saving" :icon="Send" type="primary" @click="submitAudit">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialog" :title="`详情 - ${detailTargetType}`" width="760px" append-to-body>
      <el-skeleton v-if="detailLoading" :rows="8" animated />
      <pre v-else class="admin-json-preview">{{ JSON.stringify(detailData, null, 2) }}</pre>
      <template #footer>
        <el-button @click="detailDialog = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="noticeDialog" title="公告编辑" width="680px" append-to-body>
      <el-form :model="noticeForm" label-position="top">
        <el-form-item label="标题">
          <el-input v-model="noticeForm.title" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="noticeForm.status" style="width: 100%">
            <el-option v-for="item in noticeStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="noticeForm.content" type="textarea" :rows="6" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="noticeDialog = false">取消</el-button>
        <el-button :loading="saving" :icon="Save" type="primary" @click="saveNotice">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reportDialog" title="处理举报" width="560px" append-to-body>
      <div v-if="reportTarget" style="margin-bottom: 12px">
        <el-tag>{{ reportTarget.targetTypeText }}</el-tag>
        <p style="margin: 12px 0 6px">{{ reportTarget.description }}</p>
        <p class="muted">举报原因：{{ reportTarget.reasonTypeText }}</p>
        <div v-if="reportTarget.targetContent" style="margin-top: 12px; padding: 12px; background: rgba(244,248,246,0.9); border-radius: 8px; white-space: pre-wrap; word-break: break-word; font-size: 13px; color: #30413b;">
          <strong>被举报内容：</strong><br>{{ reportTarget.targetContent }}
        </div>
      </div>
      <el-form :model="reportForm" label-position="top">
        <el-form-item label="处理动作">
          <el-select v-model="reportForm.action" style="width: 100%">
            <el-option v-for="item in reportActionOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理意见">
          <el-input v-model="reportForm.opinion" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportDialog = false">取消</el-button>
        <el-button :loading="saving" type="primary" @click="submitReportResolve">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="appealDialog" title="处理申诉" width="560px" append-to-body>
      <div v-if="appealTarget" style="margin-bottom: 12px">
        <el-tag>{{ appealTarget.targetTypeText }}</el-tag>
        <p style="margin: 12px 0 6px">{{ appealTarget.reason }}</p>
        <p class="muted">申诉人：{{ appealTarget.applicantNickname }}</p>
      </div>
      <el-form :model="appealForm" label-position="top">
        <el-form-item label="复核动作">
          <el-select v-model="appealForm.action" style="width: 100%">
            <el-option v-for="item in appealActionOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="复核意见">
          <el-input v-model="appealForm.opinion" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="appealDialog = false">取消</el-button>
        <el-button :loading="saving" type="primary" @click="submitAppealReview">提交</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Archive,
  ChartNoAxesCombined,
  Check,
  ClipboardCheck,
  Eye,
  FileCheck2,
  HeartHandshake,
  Megaphone,
  Pencil,
  Plus,
  RefreshCw,
  Save,
  Send,
  ShieldAlert,
  Users,
  X
} from 'lucide-vue-next'
import StatusTag from '../components/StatusTag.vue'
import { adminApi } from '../api'
import { notifyError } from '../api/http'
import {
  animalStatusOptions,
  appealActionOptions,
  appealStatusOptions,
  appealTargetOptions,
  applyStatusOptions,
  noticeStatusOptions,
  reportActionOptions,
  reportStatusOptions,
  reportTargetOptions,
  rescueStatusOptions,
  roleOptions,
  userStatusOptions
} from '../utils/status'

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
const reports = ref([])
const appeals = ref([])
const auditType = ref('')
const reportStatusFilter = ref('')
const reportTypeFilter = ref('')
const appealStatusFilter = ref('')
const appealTypeFilter = ref('')

const auditDialog = ref(false)
const detailDialog = ref(false)
const noticeDialog = ref(false)
const reportDialog = ref(false)
const appealDialog = ref(false)
const detailLoading = ref(false)
const detailData = ref(null)
const detailTargetType = ref('')
const reportTarget = ref(null)
const appealTarget = ref(null)

const auditForm = reactive({ targetType: '', targetId: null, action: 'APPROVE', opinion: '' })
const noticeForm = reactive({ id: null, title: '', content: '', status: 'DRAFT' })
const reportForm = reactive({ action: 'DISMISS', opinion: '' })
const appealForm = reactive({ action: 'ESCALATE', opinion: '' })

async function loadAll() {
  await Promise.all([loadDashboard(), loadPending(), loadUsers(), loadNotices(), loadApplications(), loadReports(), loadAppeals()])
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

async function loadReports() {
  try {
    reports.value = (await adminApi.reports({ status: reportStatusFilter.value || undefined, targetType: reportTypeFilter.value || undefined, page: 0, size: 30 })).content || []
  } catch (error) {
    notifyError(error)
  }
}

async function loadAppeals() {
  try {
    appeals.value = (await adminApi.appeals({ status: appealStatusFilter.value || undefined, targetType: appealTypeFilter.value || undefined, page: 0, size: 30 })).content || []
  } catch (error) {
    notifyError(error)
  }
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
    opinion: action === 'APPROVE' ? '审核通过' : action === 'OFFLINE' ? '内容已下架' : '审核不通过'
  })
  auditDialog.value = true
}

async function submitAudit() {
  saving.value = true
  try {
    await adminApi.audit(auditForm)
    ElMessage.success('审核已处理')
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
    ElMessage.success('用户已更新')
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
    ElMessage.success('公告已保存')
    noticeDialog.value = false
    await loadNotices()
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

async function offlineNotice(row) {
  try {
    await adminApi.offlineNotice(row.id)
    ElMessage.success('公告已下架')
    await loadNotices()
  } catch (error) {
    notifyError(error)
  }
}

async function openReportResolve(row) {
  try {
    reportTarget.value = await adminApi.reportDetail(row.id)
    reportForm.action = 'DISMISS'
    reportForm.opinion = ''
    reportDialog.value = true
  } catch (error) {
    notifyError(error)
  }
}

async function submitReportResolve() {
  saving.value = true
  try {
    await adminApi.resolveReport(reportTarget.value.id, reportForm)
    ElMessage.success('举报已处理')
    reportDialog.value = false
    await loadReports()
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

async function openAppealReview(row) {
  try {
    appealTarget.value = await adminApi.appealDetail(row.id)
    appealForm.action = 'ESCALATE'
    appealForm.opinion = ''
    appealDialog.value = true
  } catch (error) {
    notifyError(error)
  }
}

async function submitAppealReview() {
  saving.value = true
  try {
    await adminApi.reviewAppeal(appealTarget.value.id, appealForm)
    ElMessage.success('申诉已处理')
    appealDialog.value = false
    await loadAppeals()
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

onMounted(loadAll)
</script>

<style scoped>
.admin-json-preview {
  white-space: pre-wrap;
  word-break: break-word;
  background: rgba(244, 248, 246, 0.9);
  border-radius: 14px;
  padding: 14px;
  max-height: 62vh;
  overflow: auto;
}
</style>
