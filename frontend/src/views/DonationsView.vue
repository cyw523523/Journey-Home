<template>
  <section class="view page">
    <div class="section-head">
      <div>
        <h1>物资捐赠</h1>
        <p>发布物资需求，接受爱心捐赠，让每一份善意都能帮助到需要的小动物。</p>
      </div>
      <el-button v-if="auth.isLoggedIn.value" :icon="Plus" type="primary" size="large" @click="dialogVisible = true">发布需求</el-button>
      <el-button v-else :icon="LogIn" size="large" @click="$router.push('/auth')">登录后发布</el-button>
    </div>

    <div class="toolbar tool-panel" style="grid-template-columns: 1.5fr 1fr 1fr auto">
      <el-input v-model="filters.keyword" placeholder="关键词" clearable @keyup.enter="load" />
      <el-select v-model="filters.category" placeholder="物资类别" clearable>
        <el-option v-for="item in supplyCategoryOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-select v-model="filters.status" placeholder="状态" clearable>
        <el-option v-for="item in publicDonationStatuses" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-button :icon="Search" type="primary" @click="load">筛选</el-button>
    </div>

    <el-skeleton v-if="loading" :rows="6" animated />
    <div v-else-if="demands.length" class="grid donation-grid">
      <article v-for="demand in demands" :key="demand.id" class="donation-card lift-card">
        <StatusTag :value="demand.status" :text="demand.statusText" :options="donationStatusOptions" />
        <div class="donation-header">
          <h3>{{ demand.title }}</h3>
          <el-tag size="small" type="info">{{ demand.categoryLabel }}</el-tag>
        </div>
        <p class="muted">{{ demand.description }}</p>
        <div class="progress-section">
          <div class="progress-info">
            <span><strong>{{ demand.currentQuantity }}</strong> / {{ demand.targetQuantity }} {{ demand.categoryLabel.includes('粮') ? 'kg' : '件' }}</span>
            <span>{{ progressPercent(demand) }}%</span>
          </div>
          <el-progress :percentage="progressPercent(demand)" :stroke-width="8" :color="progressColor(demand)" />
        </div>
        <div class="meta-line"><MapPin :size="14" /> {{ demand.shippingAddress || '待填写收货地址' }}</div>
        <div style="display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-top: 8px">
          <div class="meta-line"><User :size="14" /> {{ demand.publisherNickname || '-' }}</div>
          <div style="display:flex;gap:6px">
            <el-button v-if="canEdit(demand)" :icon="Pencil" text size="small" @click="openEdit(demand)">编辑</el-button>
            <el-button :icon="Eye" @click="openDetail(demand)">详情</el-button>
            <el-button v-if="canDonate(demand)" :icon="Heart" type="primary" plain size="small" @click="openDonate(demand)">捐赠</el-button>
          </div>
        </div>
      </article>
    </div>
    <EmptyState v-else title="暂无物资需求" description="可以发布一条物资需求，等待爱心人士捐赠。" />

    <el-dialog v-model="detailVisible" title="物资需求详情" width="680px" append-to-body>
      <div v-if="current" class="form-shell">
        <StatusTag :value="current.status" :text="current.statusText" :options="donationStatusOptions" />
        <h2>{{ current.title }}</h2>
        <el-tag size="small" type="info" style="margin-bottom: 12px">{{ current.categoryLabel }}</el-tag>
        <p class="muted">{{ current.description }}</p>
        <div class="progress-section" style="margin: 16px 0">
          <div class="progress-info">
            <span><strong>{{ current.currentQuantity }}</strong> / {{ current.targetQuantity }} 件</span>
            <span>{{ progressPercent(current) }}%</span>
          </div>
          <el-progress :percentage="progressPercent(current)" :stroke-width="10" :color="progressColor(current)" />
        </div>
        <div class="detail-meta">
          <p class="meta-line"><User :size="16" /> 发布人：{{ current.publisherNickname || '-' }}</p>
          <p class="meta-line"><Phone :size="16" /> 联系人：{{ current.contactName || '-' }} / {{ current.contactPhone || '-' }}</p>
          <p class="meta-line"><MapPin :size="16" /> 收货地址：{{ current.shippingAddress || '未填写' }}</p>
        </div>

        <h4 style="margin-top: 20px; margin-bottom: 12px">捐赠记录</h4>
        <el-skeleton v-if="recordsLoading" :rows="3" animated />
        <div v-else-if="records.length" class="record-list">
          <div v-for="record in records" :key="record.id" class="record-item">
            <div class="record-main">
              <strong>{{ record.donorDisplayName || '匿名' }}</strong>
              <span>捐赠了 {{ record.quantity }} 件</span>
              <span class="muted">{{ record.message || '' }}</span>
            </div>
            <div class="record-right">
              <StatusTag :value="record.status" :text="record.statusText" :options="donationStatusOptions" size="small" />
              <el-button
                v-if="canConfirmRecord(record)"
                :icon="CheckCircle"
                type="success"
                text
                size="small"
                @click="confirmDonation(record)"
              >确认收到</el-button>
            </div>
          </div>
        </div>
        <EmptyState v-else title="暂无捐赠记录" description="成为第一个捐赠者吧！" :compact="true" />

        <div v-if="canEdit(current)" style="display:flex;gap:8px;margin-top:20px">
          <el-button :icon="Pencil" type="primary" @click="detailVisible = false; openEdit(current)">编辑</el-button>
          <el-button :icon="Archive" type="danger" @click="offlineDemand(current)">下架</el-button>
        </div>
        <div v-else-if="canDonate(current)" style="margin-top: 16px">
          <el-button :icon="Heart" type="primary" @click="detailVisible = false; openDonate(current)">我要捐赠</el-button>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="dialogVisible" title="发布物资需求" width="720px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="需求标题" prop="title">
          <el-input v-model="form.title" placeholder="如：急需猫粮50斤用于救助站" />
        </el-form-item>
        <el-form-item label="物资类别" prop="category">
          <el-select v-model="form.category" placeholder="选择类别" style="width: 100%">
            <el-option v-for="item in supplyCategoryOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="目标数量" prop="targetQuantity">
              <el-input-number v-model="form.targetQuantity" :min="1" :max="10000" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="详细说明" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="描述为什么需要这些物资、用途等" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.contactName" placeholder="联系人姓名（选填）" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.contactPhone" placeholder="联系电话（选填）" />
        </el-form-item>
        <el-form-item label="收货地址">
          <el-input v-model="form.shippingAddress" placeholder="邮寄或线下交接地址（选填）" />
        </el-form-item>
        <el-form-item label="封面图片">
          <ImageUploader v-model="form.imageUrls" usage="supply" :limit="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button :loading="saving" :icon="Send" type="primary" @click="submit">提交发布</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editVisible" title="编辑物资需求" width="720px" append-to-body>
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-position="top">
        <el-form-item label="需求标题" prop="title">
          <el-input v-model="editForm.title" />
        </el-form-item>
        <el-form-item label="物资类别" prop="category">
          <el-select v-model="editForm.category" style="width: 100%">
            <el-option v-for="item in supplyCategoryOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标数量" prop="targetQuantity">
          <el-input-number v-model="editForm.targetQuantity" :min="1" :max="10000" style="width: 100%" />
        </el-form-item>
        <el-form-item label="详细说明" prop="description">
          <el-input v-model="editForm.description" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="editForm.contactName" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="editForm.contactPhone" />
        </el-form-item>
        <el-form-item label="收货地址">
          <el-input v-model="editForm.shippingAddress" />
        </el-form-item>
        <el-form-item label="封面图片">
          <ImageUploader v-model="editForm.imageUrls" usage="supply" :limit="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button :loading="saving" type="primary" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="donateVisible" title="爱心捐赠" width="520px" append-to-body>
      <div v-if="current" class="form-shell">
        <h3>{{ current.title }}</h3>
        <p class="muted">还需 <strong>{{ (current.targetQuantity || 0) - (current.currentQuantity || 0) }}</strong> 件</p>
        <el-form ref="donateFormRef" :model="donateForm" :rules="donateRules" label-position="top">
          <el-form-item label="捐赠数量" prop="quantity">
            <el-input-number v-model="donateForm.quantity" :min="1" :max="maxDonateQty" style="width: 100%" />
          </el-form-item>
          <el-form-item label="配送方式">
            <el-select v-model="donateForm.deliveryMethod" placeholder="选择方式" style="width: 100%">
              <el-option label="在线捐赠/邮寄" value="ONLINE" />
              <el-option label="线下自送" value="OFFLINE" />
            </el-select>
          </el-form-item>
          <el-form-item label="快递单号（如有）">
            <el-input v-model="donateForm.trackingNumber" placeholder="填写后方便接收方追踪" />
          </el-form-item>
          <el-form-item label="留言">
            <el-input v-model="donateForm.message" type="textarea" :rows="2" placeholder="想对受助人说的话..." />
          </el-form-item>
          <el-form-item label="显示名称">
            <el-input v-model="donateForm.donorDisplayName" placeholder="留空则显示昵称" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="donateVisible = false">取消</el-button>
        <el-button :loading="saving" :icon="Heart" type="primary" @click="submitDonate">确认捐赠</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Archive, CheckCircle, Eye, Heart, LogIn, MapPin, Pencil, Phone, Plus, Search, Send, User } from 'lucide-vue-next'
import { useRoute, useRouter } from 'vue-router'
import EmptyState from '../components/EmptyState.vue'
import ImageUploader from '../components/ImageUploader.vue'
import StatusTag from '../components/StatusTag.vue'
import { donationApi } from '../api'
import { notifyError } from '../api/http'
import {
  donationStatusOptions,
  supplyCategoryOptions
} from '../utils/status'
import { useAuth } from '../stores/auth'

const auth = useAuth()
const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const recordsLoading = ref(false)
const detailVisible = ref(false)
const dialogVisible = ref(false)
const editVisible = ref(false)
const donateVisible = ref(false)
const current = ref(null)
const demands = ref([])
const records = ref([])
const formRef = ref()
const editFormRef = ref()
const donateFormRef = ref()
const publicDonationStatuses = donationStatusOptions.filter((item) => ['PENDING', 'CLAIMED', 'IN_TRANSIT', 'COMPLETED'].includes(item.value))
const filters = reactive({ keyword: '', category: '', status: '' })
const form = reactive({ title: '', category: '', targetQuantity: 10, description: '', contactName: '', contactPhone: '', shippingAddress: '', imageUrls: [] })
const editForm = reactive({ id: null, title: '', category: '', targetQuantity: 0, description: '', contactName: '', contactPhone: '', shippingAddress: '', imageUrls: [] })
const donateForm = reactive({ quantity: 1, deliveryMethod: 'ONLINE', trackingNumber: '', message: '', donorDisplayName: '' })
const rules = {
  title: [{ required: true, message: '请输入需求标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择物资类别', trigger: 'change' }],
  targetQuantity: [{ required: true, message: '请输入目标数量', trigger: 'blur' }],
  description: [{ required: true, message: '请输入详细说明', trigger: 'blur' }]
}
const editRules = { ...rules }
const donateRules = {
  quantity: [{ required: true, message: '请输入捐赠数量', trigger: 'blur' }]
}

function progressPercent(item) {
  if (!item.targetQuantity) return 0
  return Math.min(100, Math.round((item.currentQuantity || 0) / item.targetQuantity * 100))
}

function progressColor(item) {
  const pct = progressPercent(item)
  if (pct >= 100) return '#1f8a70'
  if (pct >= 50) return '#f2a93b'
  return '#e86f51'
}

const maxDonateQty = computed(() => {
  if (!current.value) return 1
  return Math.max(1, (current.value.targetQuantity || 0) - (current.value.currentQuantity || 0))
})

function canEdit(record) {
  if (!auth.state.user) return false
  return auth.state.user.id === record.publisherId || auth.isAdmin.value
}

function canDonate(demand) {
  if (!auth.isLoggedIn.value) return false
  if (!demand) return false
  return demand.status !== 'COMPLETED' && demand.status !== 'CANCELLED' && (demand.currentQuantity || 0) < (demand.targetQuantity || 0)
}

function canConfirmRecord(record) {
  if (!auth.state.user || !current.value) return false
  return auth.state.user.id === current.value.publisherId && record.status === 'CLAIMED'
}

async function load() {
  loading.value = true
  try {
    const data = await donationApi.list({ ...filters, page: 0, size: 12 })
    demands.value = data.content || []
  } catch (error) {
    notifyError(error)
  } finally {
    loading.value = false
  }
}

async function loadRecords(id) {
  recordsLoading.value = true
  try {
    records.value = await donationApi.records(id, { page: 0, size: 50 })
  } catch {
    records.value = []
  } finally {
    recordsLoading.value = false
  }
}

async function openDetail(demand) {
  try {
    current.value = await donationApi.detail(demand.id)
  } catch {
    current.value = demand
  }
  detailVisible.value = true
  await loadRecords(current.value?.id || demand.id)
}

function openEdit(demand) {
  Object.assign(editForm, {
    id: demand.id,
    title: demand.title || '',
    category: demand.category || '',
    targetQuantity: demand.targetQuantity || 0,
    description: demand.description || '',
    contactName: demand.contactName || '',
    contactPhone: demand.contactPhone || '',
    shippingAddress: demand.shippingAddress || '',
    imageUrls: demand.imageUrl ? [demand.imageUrl] : []
  })
  editVisible.value = true
}

async function saveEdit() {
  await editFormRef.value.validate()
  saving.value = true
  try {
    const payload = { ...editForm, imageUrl: editForm.imageUrls?.[0] || null }
    await donationApi.update(editForm.id, payload)
    ElMessage.success('物资需求已更新')
    editVisible.value = false
    load()
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

async function offlineDemand(demand) {
  try {
    await ElMessageBox.confirm('下架后该需求将从公开列表中移除，确认继续吗？', '提示', { type: 'warning' })
    await donationApi.offline(demand.id)
    ElMessage.success('已下架')
    detailVisible.value = false
    load()
  } catch (error) {
    if (error !== 'cancel') notifyError(error)
  }
}

function openDonate(demand) {
  current.value = demand
  donateForm.quantity = 1
  donateForm.deliveryMethod = 'ONLINE'
  donateForm.trackingNumber = ''
  donateForm.message = ''
  donateForm.donorDisplayName = ''
  donateVisible.value = true
}

async function submitDonate() {
  await donateFormRef.value.validate()
  saving.value = true
  try {
    await donationApi.donate(current.value.id, donateForm)
    ElMessage.success('感谢你的爱心捐赠！')
    donateVisible.value = false
    detailVisible.value = false
    load()
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

async function confirmDonation(record) {
  try {
    await ElMessageBox.confirm('确认已收到该捐赠？', '提示', { type: 'info' })
    await donationApi.completeDonation(record.id)
    ElMessage.success('已确认收到')
    await loadRecords(current.value.id)
    load()
  } catch (error) {
    if (error !== 'cancel') notifyError(error)
  }
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { ...form, imageUrl: form.imageUrls?.[0] || null }
    await donationApi.create(payload)
    ElMessage.success('发布成功，等待审核')
    Object.assign(form, { title: '', category: '', targetQuantity: 10, description: '', contactName: '', contactPhone: '', shippingAddress: '', imageUrls: [] })
    dialogVisible.value = false
    load()
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await load()
})
</script>

<style scoped>
.donation-grid {
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
}
.donation-card {
  padding: 20px;
}
.donation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.donation-header h3 {
  margin: 0;
  font-size: 17px;
}
.progress-section {
  margin: 12px 0 8px;
}
.progress-info {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  margin-bottom: 6px;
}
.detail-meta {
  display: grid;
  gap: 6px;
  padding: 12px 0;
  border-top: 1px solid var(--line);
}
.record-list {
  display: grid;
  gap: 8px;
}
.record-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  background: var(--panel-soft);
  border-radius: 10px;
}
.record-main {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  min-width: 0;
}
.record-right {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}
</style>
