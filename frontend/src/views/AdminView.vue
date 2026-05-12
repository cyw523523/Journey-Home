<template>
  <section class="view page">
    <div class="section-head">
      <div>
        <h1>管理员后台</h1>
        <p>统一审核、用户管理、公告发布和数据统计。</p>
      </div>
      <el-button :icon="RefreshCw" @click="loadAll">刷新</el-button>
    </div>

    <div class="admin-layout">
      <aside class="admin-menu surface">
        <el-menu v-model="active" @select="active = $event">
          <el-menu-item index="dashboard"><ChartNoAxesCombined />统计概览</el-menu-item>
          <el-menu-item index="audits"><ClipboardCheck />审核管理</el-menu-item>
          <el-menu-item index="users"><Users />用户管理</el-menu-item>
          <el-menu-item index="notices"><Megaphone />公告管理</el-menu-item>
          <el-menu-item index="applications"><HeartHandshake />领养申请</el-menu-item>
        </el-menu>
      </aside>

      <main class="admin-content">
        <div v-show="active === 'dashboard'" class="surface form-shell">
          <div class="metric-grid">
            <div class="metric">
              <span>用户数</span>
              <strong>{{ overview.userCount }}</strong>
            </div>
            <div class="metric">
              <span>动物档案</span>
              <strong>{{ overview.animalCount }}</strong>
            </div>
            <div class="metric">
              <span>救助信息</span>
              <strong>{{ overview.rescueCount }}</strong>
            </div>
            <div class="metric">
              <span>领养申请</span>
              <strong>{{ overview.applyCount }}</strong>
            </div>
            <div class="metric">
              <span>待审核</span>
              <strong>{{ overview.pendingAuditCount }}</strong>
            </div>
          </div>
          <el-row :gutter="14" style="margin-top: 16px">
            <el-col :md="8" :sm="24">
              <h3>动物状态</h3>
              <div v-for="item in animalStatus" :key="item.status" class="mini-row">
                <span>{{ item.statusText }}</span><strong>{{ item.count }}</strong>
              </div>
            </el-col>
            <el-col :md="8" :sm="24">
              <h3>救助状态</h3>
              <div v-for="item in rescueStatus" :key="item.status" class="mini-row">
                <span>{{ item.statusText }}</span><strong>{{ item.count }}</strong>
              </div>
            </el-col>
            <el-col :md="8" :sm="24">
              <h3>申请状态</h3>
              <div v-for="item in applyStatus" :key="item.status" class="mini-row">
                <span>{{ item.statusText }}</span><strong>{{ item.count }}</strong>
              </div>
            </el-col>
          </el-row>
        </div>

        <div v-show="active === 'audits'" class="surface form-shell">
          <div style="display: flex; gap: 10px; margin-bottom: 12px">
            <el-select v-model="auditType" placeholder="审核类型" clearable style="width: 220px" @change="loadPending">
              <el-option label="动物档案" value="ANIMAL" />
              <el-option label="救助信息" value="RESCUE" />
              <el-option label="领养申请" value="ADOPT_APPLY" />
            </el-select>
            <el-button :icon="RefreshCw" @click="loadPending">刷新</el-button>
          </div>
          <el-table :data="pending" stripe>
            <el-table-column prop="targetType" label="类型" width="140" />
            <el-table-column prop="targetId" label="业务ID" width="100" />
            <el-table-column prop="title" label="内容" />
            <el-table-column prop="publisherOrApplicant" label="发布/申请人" width="150" />
            <el-table-column label="操作" width="210">
              <template #default="{ row }">
                <el-button size="small" :icon="Check" type="primary" @click="openAudit(row, 'APPROVE')">通过</el-button>
                <el-button size="small" :icon="X" @click="openAudit(row, 'REJECT')">驳回</el-button>
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
          <div style="display: flex; justify-content: flex-end; margin-bottom: 12px">
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
      </main>
    </div>

    <el-dialog v-model="auditDialog" title="审核处理" width="520px">
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

    <el-dialog v-model="noticeDialog" title="公告管理" width="680px">
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
import { applyStatusOptions, noticeStatusOptions, roleOptions, userStatusOptions } from '../utils/status'

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

function openAudit(row, action) {
  Object.assign(auditForm, {
    targetType: row.targetType,
    targetId: row.targetId,
    action,
    opinion: action === 'APPROVE' ? '内容完整，审核通过。' : '信息不完整，请修改后重新提交。'
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
    ElMessage.success('公告已下架')
    loadNotices()
  } catch (error) {
    notifyError(error)
  }
}

onMounted(loadAll)
</script>
