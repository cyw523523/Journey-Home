<template>
  <section class="view page">
    <div class="section-head">
      <div>
        <h1>{{ t('donationsPage.title') }}</h1>
        <p>{{ t('donationsPage.description') }}</p>
      </div>
      <el-button v-if="auth.isLoggedIn.value" :icon="Plus" type="primary" size="large" @click="dialogVisible = true">{{ t('donationsPage.publishNeed') }}</el-button>
      <el-button v-else :icon="LogIn" size="large" @click="$router.push('/auth')">{{ t('donationsPage.loginToPublish') }}</el-button>
    </div>

    <div class="toolbar tool-panel" style="grid-template-columns: 1.5fr 1fr 1fr auto">
      <el-input v-model="filters.keyword" :placeholder="t('common.keyword')" clearable @keyup.enter="load" />
      <el-select v-model="filters.category" :placeholder="t('donationsPage.category')" clearable>
        <el-option v-for="item in supplyCategoryOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-select v-model="filters.status" :placeholder="t('common.status')" clearable>
        <el-option v-for="item in publicDonationStatuses" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-button :icon="Search" type="primary" @click="load">{{ t('common.filter') }}</el-button>
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
            <span><strong>{{ demand.currentQuantity }}</strong> / {{ demand.targetQuantity }} {{ quantityUnit(demand.categoryLabel) }}</span>
            <span>{{ progressPercent(demand) }}%</span>
          </div>
          <el-progress :percentage="progressPercent(demand)" :stroke-width="8" :color="progressColor(demand)" />
        </div>
        <div class="meta-line"><MapPin :size="14" /> {{ demand.shippingAddress || t('donationsPage.pendingAddress') }}</div>
        <div style="display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-top: 8px">
          <div class="meta-line"><User :size="14" /> {{ demand.publisherNickname || '-' }}</div>
          <div style="display:flex;gap:6px">
            <el-button v-if="canEdit(demand)" :icon="Pencil" text size="small" @click="openEdit(demand)">{{ t('common.edit') }}</el-button>
            <el-button :icon="Eye" @click="openDetail(demand)">{{ t('common.details') }}</el-button>
            <el-button v-if="canDonate(demand)" :icon="Heart" type="primary" plain size="small" @click="openDonate(demand)">{{ t('donationsPage.donate') }}</el-button>
          </div>
        </div>
      </article>
    </div>
    <EmptyState v-else :title="t('donationsPage.emptyTitle')" :description="t('donationsPage.emptyDesc')" />

    <el-dialog v-model="detailVisible" :title="t('donationsPage.detailTitle')" width="680px" append-to-body>
      <div v-if="current" class="form-shell">
        <StatusTag :value="current.status" :text="current.statusText" :options="donationStatusOptions" />
        <h2>{{ current.title }}</h2>
        <el-tag size="small" type="info" style="margin-bottom: 12px">{{ current.categoryLabel }}</el-tag>
        <p class="muted">{{ current.description }}</p>
        <div class="progress-section" style="margin: 16px 0">
          <div class="progress-info">
            <span><strong>{{ current.currentQuantity }}</strong> / {{ current.targetQuantity }} {{ t('donationsPage.itemUnit') }}</span>
            <span>{{ progressPercent(current) }}%</span>
          </div>
          <el-progress :percentage="progressPercent(current)" :stroke-width="10" :color="progressColor(current)" />
        </div>
        <div class="detail-meta">
          <p class="meta-line"><User :size="16" /> {{ t('donationsPage.publisher') }}{{ current.publisherNickname || '-' }}</p>
          <p class="meta-line"><Phone :size="16" /> {{ t('donationsPage.contactPerson') }}{{ current.contactName || '-' }} / {{ current.contactPhone || '-' }}</p>
          <p class="meta-line"><MapPin :size="16" /> {{ t('donationsPage.shippingAddress') }}{{ current.shippingAddress || t('donationsPage.notFilled') }}</p>
        </div>

        <h4 style="margin-top: 20px; margin-bottom: 12px">{{ t('donationsPage.recordsTitle') }}</h4>
        <el-skeleton v-if="recordsLoading" :rows="3" animated />
        <div v-else-if="records.length" class="record-list">
          <div v-for="record in records" :key="record.id" class="record-item">
            <div class="record-main">
              <strong>{{ record.donorDisplayName || t('donationsPage.anonymous') }}</strong>
              <span>{{ t('donationsPage.donatedQuantity', { quantity: record.quantity }) }}</span>
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
              >{{ t('donationsPage.confirmReceived') }}</el-button>
            </div>
          </div>
        </div>
        <EmptyState v-else :title="t('donationsPage.noRecordsTitle')" :description="t('donationsPage.noRecordsDesc')" :compact="true" />

        <div v-if="canEdit(current)" style="display:flex;gap:8px;margin-top:20px">
          <el-button :icon="Pencil" type="primary" @click="detailVisible = false; openEdit(current)">{{ t('common.edit') }}</el-button>
          <el-button :icon="Archive" type="danger" @click="offlineDemand(current)">{{ t('admin.offline') }}</el-button>
        </div>
        <div v-else-if="canDonate(current)" style="margin-top: 16px">
          <el-button :icon="Heart" type="primary" @click="detailVisible = false; openDonate(current)">{{ t('donationsPage.iWantDonate') }}</el-button>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="dialogVisible" :title="t('donationsPage.publishDialogTitle')" width="720px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item :label="t('donationsPage.needTitle')" prop="title">
          <el-input v-model="form.title" :placeholder="t('donationsPage.needTitlePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('donationsPage.category')" prop="category">
          <el-select v-model="form.category" :placeholder="t('donationsPage.selectCategory')" style="width: 100%">
            <el-option v-for="item in supplyCategoryOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="t('donationsPage.targetQuantity')" prop="targetQuantity">
              <el-input-number v-model="form.targetQuantity" :min="1" :max="10000" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item :label="t('common.description')" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" :placeholder="t('donationsPage.descriptionPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('donationsPage.contactName')">
          <el-input v-model="form.contactName" :placeholder="t('donationsPage.contactNamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('donationsPage.contactPhone')">
          <el-input v-model="form.contactPhone" :placeholder="t('donationsPage.contactPhonePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('donationsPage.shippingAddressLabel')">
          <el-input v-model="form.shippingAddress" :placeholder="t('donationsPage.shippingAddressPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('donationsPage.coverImage')">
          <ImageUploader v-model="form.imageUrls" usage="supply" :limit="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button :loading="saving" :icon="Send" type="primary" @click="submit">{{ t('donationsPage.submitPublish') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editVisible" :title="t('donationsPage.editDialogTitle')" width="720px" append-to-body>
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-position="top">
        <el-form-item :label="t('donationsPage.needTitle')" prop="title">
          <el-input v-model="editForm.title" />
        </el-form-item>
        <el-form-item :label="t('donationsPage.category')" prop="category">
          <el-select v-model="editForm.category" style="width: 100%">
            <el-option v-for="item in supplyCategoryOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('donationsPage.targetQuantity')" prop="targetQuantity">
          <el-input-number v-model="editForm.targetQuantity" :min="1" :max="10000" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="t('common.description')" prop="description">
          <el-input v-model="editForm.description" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item :label="t('donationsPage.contactName')">
          <el-input v-model="editForm.contactName" />
        </el-form-item>
        <el-form-item :label="t('donationsPage.contactPhone')">
          <el-input v-model="editForm.contactPhone" />
        </el-form-item>
        <el-form-item :label="t('donationsPage.shippingAddressLabel')">
          <el-input v-model="editForm.shippingAddress" />
        </el-form-item>
        <el-form-item :label="t('donationsPage.coverImage')">
          <ImageUploader v-model="editForm.imageUrls" usage="supply" :limit="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button :loading="saving" type="primary" @click="saveEdit">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="donateVisible" :title="t('donationsPage.donateDialogTitle')" width="520px" append-to-body>
      <div v-if="current" class="form-shell">
        <h3>{{ current.title }}</h3>
        <p class="muted">{{ t('donationsPage.remainingQuantity', { quantity: (current.targetQuantity || 0) - (current.currentQuantity || 0) }) }}</p>
        <el-form ref="donateFormRef" :model="donateForm" :rules="donateRules" label-position="top">
          <el-form-item :label="t('donationsPage.donationQuantity')" prop="quantity">
            <el-input-number v-model="donateForm.quantity" :min="1" :max="maxDonateQty" style="width: 100%" />
          </el-form-item>
          <el-form-item :label="t('donationsPage.deliveryMethod')">
            <el-select v-model="donateForm.deliveryMethod" :placeholder="t('donationsPage.selectMethod')" style="width: 100%">
              <el-option :label="t('donationsPage.onlineDelivery')" value="ONLINE" />
              <el-option :label="t('donationsPage.offlineDelivery')" value="OFFLINE" />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('donationsPage.trackingNumber')">
            <el-input v-model="donateForm.trackingNumber" :placeholder="t('donationsPage.trackingPlaceholder')" />
          </el-form-item>
          <el-form-item :label="t('donationsPage.message')">
            <el-input v-model="donateForm.message" type="textarea" :rows="2" :placeholder="t('donationsPage.messagePlaceholder')" />
          </el-form-item>
          <el-form-item :label="t('donationsPage.displayName')">
            <el-input v-model="donateForm.donorDisplayName" :placeholder="t('donationsPage.displayNamePlaceholder')" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="donateVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button :loading="saving" :icon="Heart" type="primary" @click="submitDonate">{{ t('donationsPage.confirmDonate') }}</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Archive, CheckCircle, Eye, Heart, LogIn, MapPin, Pencil, Phone, Plus, Search, Send, User } from 'lucide-vue-next'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import EmptyState from '../components/EmptyState.vue'
import ImageUploader from '../components/ImageUploader.vue'
import StatusTag from '../components/StatusTag.vue'
import { donationApi } from '../api'
import { useAiAssistantPageContext } from '../composables/useAiAssistantPageContext'
import { notifyError } from '../api/http'
import {
  donationStatusOptions,
  supplyCategoryOptions
} from '../utils/status'
import { useAuth } from '../stores/auth'

const auth = useAuth()
const route = useRoute()
const router = useRouter()
const { t } = useI18n()
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
  quantity: [{ required: true, message: t('donationsPage.quantityRequired'), trigger: 'blur' }]
}

useAiAssistantPageContext(() => ({
  pageTitle: detailVisible.value && current.value ? t('donationsPage.pageTitleDetail', { id: current.value.id }) : t('donationsPage.title'),
  pageSummary: detailVisible.value && current.value
    ? t('donationsPage.pageSummaryDetail')
    : t('donationsPage.pageSummary'),
  entityType: detailVisible.value && current.value ? 'DONATION_DEMAND' : null,
  entityId: detailVisible.value && current.value ? current.value.id : null,
  viewData: {
    filters: { ...filters },
    visibleDemands: demands.value.slice(0, 6).map((item) => ({
      id: item.id,
      title: item.title,
      categoryLabel: item.categoryLabel,
      statusText: item.statusText,
      targetQuantity: item.targetQuantity,
      currentQuantity: item.currentQuantity
    })),
    detailOpen: detailVisible.value,
    currentDemand: current.value ? {
      id: current.value.id,
      title: current.value.title,
      categoryLabel: current.value.categoryLabel,
      statusText: current.value.statusText,
      targetQuantity: current.value.targetQuantity,
      currentQuantity: current.value.currentQuantity
    } : null,
    recordCount: records.value.length
  }
}))

function quantityUnit(label) {
  return String(label || '').includes('粮') ? t('donationsPage.kgUnit') : t('donationsPage.itemUnit')
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
    ElMessage.success(t('donationsPage.updated'))
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
    await ElMessageBox.confirm(t('donationsPage.offlineConfirm'), t('common.warning'), { type: 'warning' })
    await donationApi.offline(demand.id)
    ElMessage.success(t('donationsPage.offlined'))
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
    ElMessage.success(t('donationsPage.donateSuccess'))
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
    await ElMessageBox.confirm(t('donationsPage.confirmReceivePrompt'), t('common.warning'), { type: 'info' })
    await donationApi.completeDonation(record.id)
    ElMessage.success(t('donationsPage.received'))
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
    ElMessage.success(t('donationsPage.publishSuccess'))
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
