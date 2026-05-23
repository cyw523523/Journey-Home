<template>
  <section class="view page">
    <div class="section-head">
      <div>
        <h1>{{ t('profile.title') }}</h1>
        <p>{{ t('profile.subtitle') }}</p>
      </div>
    </div>

    <div class="profile-grid">
      <aside class="sidebar-panel surface">
        <div class="avatar-block">
          <el-avatar :src="getFullUrl(profile.avatarUrl)" :size="92">{{ profile.nickname?.slice(0, 1) }}</el-avatar>
          <h2>{{ profile.nickname }}</h2>
          <StatusTag :value="profile.status" :text="profile.statusText" :options="userStatusOptions" />
        </div>
        <el-form :model="profileForm" label-position="top">
          <el-form-item :label="t('profile.nickname')">
            <el-input v-model="profileForm.nickname" />
          </el-form-item>
          <el-form-item :label="t('profile.phone')">
            <el-input v-model="profileForm.phone" />
          </el-form-item>
          <el-form-item :label="t('profile.avatar')">
            <ImageUploader v-model="avatarUrls" usage="avatar" :limit="1" />
          </el-form-item>
          <el-button :loading="saving" :icon="Save" type="primary" style="width: 100%" @click="saveProfile">{{ t('profile.saveProfile') }}</el-button>
        </el-form>
      </aside>

      <main class="content-panel surface">
        <el-tabs v-model="tab">
          <el-tab-pane :label="t('profile.animalsTab')" name="animals">
            <el-table :data="animals" stripe>
              <el-table-column prop="id" :label="t('profile.idCol')" width="80" />
              <el-table-column prop="typeText" :label="t('profile.typeCol')" width="120" />
              <el-table-column prop="foundRegion" :label="t('profile.regionCol')" />
              <el-table-column :label="t('profile.statusCol')" width="140">
                <template #default="{ row }">
                  <StatusTag :value="row.status" :text="row.statusText" :options="animalStatusOptions" />
                </template>
              </el-table-column>
              <el-table-column :label="t('profile.actionCol')" width="280">
                <template #default="{ row }">
                  <el-button size="small" text @click="openAnimalEditor(row)">{{ t('profile.editBtn') }}</el-button>
                  <el-button size="small" text @click="openStatusDialog('animal', row)">{{ t('profile.statusBtn') }}</el-button>
                  <el-button size="small" text type="danger" @click="offlineRecord('animal', row)">{{ t('profile.offlineBtn') }}</el-button>
                  <el-button v-if="['REJECTED', 'OFFLINE'].includes(row.status)" size="small" text type="warning" @click="openAppeal('ANIMAL', row.id)">{{ t('profile.appealBtn') }}</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="t('profile.rescuesTab')" name="rescues">
            <el-table :data="rescues" stripe>
              <el-table-column prop="id" :label="t('profile.idCol')" width="80" />
              <el-table-column prop="location" :label="t('rescues.location')" />
              <el-table-column prop="animalCondition" :label="t('rescues.animalCondition')" />
              <el-table-column :label="t('profile.statusCol')" width="140">
                <template #default="{ row }">
                  <StatusTag :value="row.status" :text="row.statusText" :options="rescueStatusOptions" />
                </template>
              </el-table-column>
              <el-table-column :label="t('profile.actionCol')" width="280">
                <template #default="{ row }">
                  <el-button size="small" text @click="openRescueEditor(row)">{{ t('profile.editBtn') }}</el-button>
                  <el-button size="small" text @click="openStatusDialog('rescue', row)">{{ t('profile.statusBtn') }}</el-button>
                  <el-button size="small" text type="danger" @click="offlineRecord('rescue', row)">{{ t('profile.offlineBtn') }}</el-button>
                  <el-button v-if="['REJECTED', 'OFFLINE'].includes(row.status)" size="small" text type="warning" @click="openAppeal('RESCUE', row.id)">{{ t('profile.appealBtn') }}</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="t('profile.adoptionTab')" name="applications">
            <el-table :data="applications" stripe>
              <el-table-column prop="id" :label="t('profile.idCol')" width="80" />
              <el-table-column prop="animalTypeText" :label="t('admin.animal')" width="120" />
              <el-table-column prop="reason" :label="t('admin.reason')" />
              <el-table-column :label="t('profile.statusCol')" width="140">
                <template #default="{ row }">
                  <StatusTag :value="row.status" :text="row.statusText" :options="applyStatusOptions" />
                </template>
              </el-table-column>
              <el-table-column prop="auditOpinion" :label="t('admin.auditOpinion')" />
              <el-table-column :label="t('profile.actionCol')" width="220">
                <template #default="{ row }">
                  <el-button v-if="row.status === 'PENDING_REVIEW'" size="small" text type="danger" @click="cancelApplication(row)">{{ t('common.cancel') }}</el-button>
                  <el-button v-if="row.status === 'REJECTED'" size="small" text type="warning" @click="openAppeal('ADOPT_APPLY', row.id)">{{ t('profile.appealBtn') }}</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="t('profile.postsTab')" name="communityPosts">
            <el-table :data="communityPosts" stripe>
              <el-table-column prop="id" :label="t('profile.idCol')" width="80" />
              <el-table-column prop="title" :label="t('admin.noticeTitle')" />
              <el-table-column :label="t('profile.statusCol')" width="140">
                <template #default="{ row }">
                  <StatusTag :value="row.status" :text="row.statusText" :options="communityPostStatusOptions" />
                </template>
              </el-table-column>
              <el-table-column :label="t('profile.actionCol')" width="180">
                <template #default="{ row }">
                  <el-button v-if="['REJECTED', 'OFFLINE'].includes(row.status)" size="small" text type="warning" @click="openAppeal('COMMUNITY_POST', row.id)">{{ t('profile.appealBtn') }}</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="t('profile.commentsTab')" name="communityComments">
            <el-table :data="communityComments" stripe>
              <el-table-column prop="id" :label="t('profile.idCol')" width="80" />
              <el-table-column prop="content" :label="t('admin.content')" />
              <el-table-column :label="t('profile.statusCol')" width="140">
                <template #default="{ row }">
                  <StatusTag :value="row.status" :text="row.statusText" :options="communityCommentStatusOptions" />
                </template>
              </el-table-column>
              <el-table-column :label="t('profile.actionCol')" width="180">
                <template #default="{ row }">
                  <el-button v-if="['REJECTED', 'OFFLINE'].includes(row.status)" size="small" text type="warning" @click="openAppeal('COMMUNITY_COMMENT', row.id)">{{ t('profile.appealBtn') }}</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="`${t('notification.notifications')}${notificationSummary.unreadCount ? `(${notificationSummary.unreadCount})` : ''}`" name="notifications">
            <div class="toolbar" style="justify-content:flex-end;margin-bottom:12px">
              <el-button text @click="markAllRead">{{ t('notification.markAllRead') }}</el-button>
            </div>
            <el-table :data="notifications" stripe>
              <el-table-column :label="t('profile.noticeTitle')" width="220">
                <template #default="{ row }">{{ t('notification.' + row.title, row.title) }}</template>
              </el-table-column>
              <el-table-column :label="t('admin.content')">
                <template #default="{ row }">{{ formatNotificationContent(row) }}</template>
              </el-table-column>
              <el-table-column :label="t('profile.noticeStatus')" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.readFlag ? 'info' : 'success'">{{ row.readFlag ? t('notification.read') : t('notification.unread') }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createdAt" :label="t('profile.noticeTime')" width="180">
                <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
              </el-table-column>
              <el-table-column :label="t('profile.noticeAction')" width="100">
                <template #default="{ row }">
                  <el-button v-if="!row.readFlag" size="small" text @click="markRead(row.id)">{{ t('profile.markRead') }}</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="t('profile.reportsTab')" name="reports">
            <el-table :data="reports" stripe>
              <el-table-column prop="id" :label="t('profile.idCol')" width="80" />
              <el-table-column prop="targetTypeText" :label="t('admin.reportTarget')" width="120" />
              <el-table-column prop="reasonTypeText" :label="t('admin.reason')" width="120" />
              <el-table-column prop="description" :label="t('admin.content')" />
              <el-table-column :label="t('profile.statusCol')" width="140">
                <template #default="{ row }">
                  <StatusTag :value="row.status" :text="row.statusText" :options="reportStatusOptions" />
                </template>
              </el-table-column>
              <el-table-column prop="resolutionOpinion" :label="t('admin.auditOpinion')" />
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="t('profile.appealsTab')" name="appeals">
            <el-table :data="appeals" stripe>
              <el-table-column prop="id" :label="t('profile.idCol')" width="80" />
              <el-table-column prop="targetTypeText" :label="t('admin.appealTarget')" width="120" />
              <el-table-column prop="reason" :label="t('admin.appealReason')" />
              <el-table-column :label="t('profile.statusCol')" width="150">
                <template #default="{ row }">
                  <StatusTag :value="row.status" :text="row.statusText" :options="appealStatusOptions" />
                </template>
              </el-table-column>
              <el-table-column prop="finalReviewOpinion" :label="t('admin.reviewResult')" />
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="t('profile.changePassword')" name="password">
            <el-form ref="passwordRef" :model="passwordForm" :rules="passwordRules" label-position="top" style="max-width: 460px">
              <el-form-item :label="t('profile.currentPwd')" prop="oldPassword">
                <el-input v-model="passwordForm.oldPassword" type="password" show-password />
              </el-form-item>
              <el-form-item :label="t('profile.newPwd')" prop="newPassword">
                <el-input v-model="passwordForm.newPassword" type="password" show-password />
              </el-form-item>
              <el-form-item :label="t('profile.confirmPwd')" prop="confirmPassword">
                <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
              </el-form-item>
              <el-button :loading="saving" :icon="LockKeyhole" type="primary" @click="changePassword">{{ t('profile.changePassword') }}</el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </main>
    </div>

    <el-dialog v-model="animalEditorVisible" :title="t('profile.editAnimalTitle')" width="720px" append-to-body>
      <el-form ref="animalFormRef" :model="animalEditor" :rules="animalRules" label-position="top">
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item :label="t('animals.animalType')" prop="type">
          <el-select v-model="animalEditor.type" style="width: 100%">
            <el-option v-for="item in animalTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="t('animals.gender')" prop="gender">
              <el-select v-model="animalEditor.gender" style="width: 100%">
                <el-option v-for="item in genderOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="t('animals.age')" prop="age">
              <el-input-number v-model="animalEditor.age" :min="0" :max="30" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item :label="t('animals.foundRegion')" prop="foundRegion">
          <el-input v-model="animalEditor.foundRegion" />
        </el-form-item>
        <el-form-item :label="t('animals.healthCondition')">
          <el-input v-model="animalEditor.healthCondition" />
        </el-form-item>
        <el-form-item :label="t('animals.photos')">
          <ImageUploader v-model="animalEditor.imageUrls" usage="animal" />
        </el-form-item>
        <el-form-item :label="t('animals.description')">
          <el-input v-model="animalEditor.description" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="animalEditorVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button :loading="saving" type="primary" @click="saveAnimal">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rescueEditorVisible" :title="t('profile.editRescueTitle')" width="720px" append-to-body>
      <el-form ref="rescueFormRef" :model="rescueEditor" :rules="rescueRules" label-position="top">
        <el-form-item :label="t('rescues.location')" prop="location">
          <el-input v-model="rescueEditor.location" />
        </el-form-item>
        <el-form-item :label="t('rescues.animalCondition')" prop="animalCondition">
          <el-input v-model="rescueEditor.animalCondition" />
        </el-form-item>
        <el-form-item :label="t('rescues.contact')" prop="contact">
          <el-input v-model="rescueEditor.contact" />
        </el-form-item>
        <el-form-item :label="t('rescues.description')" prop="description">
          <el-input v-model="rescueEditor.description" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item :label="t('rescues.siteImages')">
          <ImageUploader v-model="rescueEditor.imageUrls" usage="rescue" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rescueEditorVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button :loading="saving" type="primary" @click="saveRescue">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="statusDialogVisible" :title="t('profile.updateStatusTitle')" width="460px" append-to-body>
      <el-form label-position="top">
        <el-form-item :label="t('profile.newStatus')">
          <el-select v-model="statusForm.newStatus" style="width: 100%">
            <el-option v-for="item in availableStatuses" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button :loading="saving" type="primary" @click="saveStatus">{{ t('profile.update') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="appealVisible" :title="t('profile.submitAppealTitle')" width="560px" append-to-body>
      <el-form ref="appealFormRef" :model="appealForm" :rules="appealRules" label-position="top">
        <el-form-item :label="t('profile.appealTargetLabel')">
          <el-input :model-value="optionText(appealTargetOptions, appealForm.targetType)" disabled />
        </el-form-item>
        <el-form-item :label="t('profile.appealReasonLabel')" prop="reason">
          <el-input v-model="appealForm.reason" type="textarea" :rows="5" maxlength="1000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="appealVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button :loading="saving" type="primary" @click="submitAppeal">{{ t('profile.submitAppealBtn') }}</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { useRoute } from 'vue-router'
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Archive, LockKeyhole, Save } from 'lucide-vue-next'
import StatusTag from '../components/StatusTag.vue'
import ImageUploader from '../components/ImageUploader.vue'
import { adoptionApi, animalApi, appealApi, communityApi, notificationApi, reportApi, rescueApi, userApi } from '../api'
import { notifyError } from '../api/http'
import { useAuth } from '../stores/auth'
import {
  animalStatusOptions,
  communityCommentStatusOptions,
  communityPostStatusOptions,
  animalTypeOptions,
  appealStatusOptions,
  appealTargetOptions,
  applyStatusOptions,
  genderOptions,
  optionText,
  reportStatusOptions,
  rescueStatusOptions,
  userStatusOptions
} from '../utils/status'

const auth = useAuth()
const route = useRoute()
const { t } = useI18n()
const tab = ref(route.query.tab || 'animals')
const saving = ref(false)
const profile = ref(auth.state.user || {})
const animals = ref([])
const rescues = ref([])
const applications = ref([])
const communityPosts = ref([])
const communityComments = ref([])
const notifications = ref([])
const notificationSummary = ref({ unreadCount: 0 })
const reports = ref([])
const appeals = ref([])
const passwordRef = ref()
const animalFormRef = ref()
const rescueFormRef = ref()
const appealFormRef = ref()
const avatarUrls = ref([])
const profileForm = reactive({ nickname: '', phone: '', avatarUrl: '' })
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const animalEditorVisible = ref(false)
const rescueEditorVisible = ref(false)
const statusDialogVisible = ref(false)
const appealVisible = ref(false)
const statusTargetType = ref('animal')
const statusTarget = ref(null)
const statusForm = reactive({ newStatus: '' })
const animalEditor = reactive({ id: null, type: 'CAT', gender: 'UNKNOWN', age: 0, foundRegion: '', healthCondition: '', imageUrls: [], description: '' })
const rescueEditor = reactive({ id: null, location: '', animalCondition: '', contact: '', description: '', imageUrls: [] })
const appealForm = reactive({ targetType: 'ANIMAL', targetId: null, reason: '' })

const animalRules = {
  type: [{ required: true, message: t('animals.selectAnimalType'), trigger: 'change' }],
  gender: [{ required: true, message: t('animals.selectGender'), trigger: 'change' }],
  foundRegion: [{ required: true, message: t('animals.inputRegion'), trigger: 'blur' }],
  imageUrls: [{ type: 'array', required: true, min: 1, message: t('animals.uploadPhoto'), trigger: 'change' }]
}
const rescueRules = {
  location: [{ required: true, message: t('rescues.rescueLocationRequired'), trigger: 'blur' }],
  animalCondition: [{ required: true, message: t('rescues.animalConditionRequired'), trigger: 'blur' }],
  contact: [{ required: true, message: t('rescues.contactRequired'), trigger: 'blur' }],
  description: [{ required: true, message: t('rescues.descriptionRequired'), trigger: 'blur' }]
}
const appealRules = {
  reason: [{ required: true, message: t('profile.appealReasonRequired'), trigger: 'blur' }]
}
const passwordRules = {
  oldPassword: [{ required: true, message: t('profile.oldPwdRequired'), trigger: 'blur' }],
  newPassword: [{ required: true, message: t('profile.newPwdRequired'), trigger: 'blur' }, { min: 6, max: 32, message: t('auth.passwordLength'), trigger: 'blur' }],
  confirmPassword: [{ required: true, message: t('profile.confirmPwdRequired'), trigger: 'blur' }, { validator: (rule, value, callback) => value !== passwordForm.newPassword ? callback(new Error(t('profile.pwdNotMatch'))) : callback(), trigger: 'blur' }]
}

const availableStatuses = computed(() => {
  if (statusTargetType.value === 'animal') {
    return animalStatusOptions.filter((item) => ['WAITING_RESCUE', 'RESCUING', 'WAITING_ADOPTION', 'ADOPTED', 'OFFLINE'].includes(item.value))
  }
  return rescueStatusOptions.filter((item) => item.value !== 'PENDING_REVIEW' && item.value !== 'REJECTED')
})

const API_BASE = window.location.origin
function getFullUrl(url) {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:')) return url
  return `${API_BASE}${url}`
}

function formatTime(value) {
  return value ? new Date(value).toLocaleString() : '-'
}

function formatNotificationContent(item) {
  if (item.title === 'COMMENT_REPLY_COMMENT' || item.title === 'COMMENT_REPLY_POST') {
    const parts = (item.content || '').split('|')
    if (parts.length === 2) {
      return t('notification.' + item.title + '_CONTENT', { nickname: parts[0], snippet: parts[1] })
    }
  }
  return item.content || ''
}

async function loadRecords() {
  try {
    const [myAnimals, myRescues, myApplications, myCommunityPosts, myCommunityComments, myNotifications, notificationSummaryData, myReports, myAppeals] = await Promise.all([
      userApi.animals({ page: 0, size: 20 }),
      userApi.rescues({ page: 0, size: 20 }),
      userApi.applications({ page: 0, size: 20 }),
      communityApi.myPosts({ page: 0, size: 20 }),
      communityApi.myComments({ page: 0, size: 20 }),
      notificationApi.list({ page: 0, size: 20 }),
      notificationApi.summary(),
      reportApi.list({ page: 0, size: 20 }),
      appealApi.list({ page: 0, size: 20 })
    ])
    animals.value = myAnimals.content || []
    rescues.value = myRescues.content || []
    applications.value = myApplications.content || []
    communityPosts.value = myCommunityPosts.content || []
    communityComments.value = myCommunityComments.content || []
    notifications.value = myNotifications.content || []
    notificationSummary.value = notificationSummaryData || { unreadCount: 0 }
    reports.value = myReports.content || []
    appeals.value = myAppeals.content || []
  } catch (error) {
    notifyError(error)
  }
}

async function load() {
  try {
    profile.value = await userApi.profile()
    Object.assign(profileForm, {
      nickname: profile.value.nickname,
      phone: profile.value.phone,
      avatarUrl: profile.value.avatarUrl || ''
    })
    avatarUrls.value = profile.value.avatarUrl ? [profile.value.avatarUrl] : []
  } catch (error) {
    notifyError(error)
  }
  await loadRecords()
}

async function saveProfile() {
  saving.value = true
  try {
    profileForm.avatarUrl = avatarUrls.value[0] || ''
    profile.value = await userApi.update(profileForm)
    auth.state.user = profile.value
    localStorage.setItem('guitu_user', JSON.stringify(profile.value))
    ElMessage.success(t('profile.profileSaved'))
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

async function changePassword() {
  await passwordRef.value.validate()
  saving.value = true
  try {
    await userApi.changePassword(passwordForm)
    ElMessage.success(t('profile.pwdChanged'))
    Object.assign(passwordForm, { oldPassword: '', newPassword: '', confirmPassword: '' })
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

function openAnimalEditor(row) {
  Object.assign(animalEditor, {
    id: row.id,
    type: row.type,
    gender: row.gender,
    age: row.age ?? 0,
    foundRegion: row.foundRegion || '',
    healthCondition: row.healthCondition || '',
    imageUrls: row.imageUrls || [],
    description: row.description || ''
  })
  animalEditorVisible.value = true
}

function openRescueEditor(row) {
  Object.assign(rescueEditor, {
    id: row.id,
    location: row.location || '',
    animalCondition: row.animalCondition || '',
    contact: row.contact || '',
    description: row.description || '',
    imageUrls: row.imageUrls || []
  })
  rescueEditorVisible.value = true
}

async function saveAnimal() {
  await animalFormRef.value.validate()
  saving.value = true
  try {
    await animalApi.update(animalEditor.id, animalEditor)
    ElMessage.success(t('profile.animalUpdated'))
    animalEditorVisible.value = false
    await loadRecords()
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

async function saveRescue() {
  await rescueFormRef.value.validate()
  saving.value = true
  try {
    await rescueApi.update(rescueEditor.id, rescueEditor)
    ElMessage.success(t('profile.rescueUpdated'))
    rescueEditorVisible.value = false
    await loadRecords()
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

function openStatusDialog(type, row) {
  statusTargetType.value = type
  statusTarget.value = row
  statusForm.newStatus = row.status
  statusDialogVisible.value = true
}

async function saveStatus() {
  saving.value = true
  try {
    if (statusTargetType.value === 'animal') {
      await animalApi.updateStatus(statusTarget.value.id, { status: statusForm.newStatus })
    } else {
      await rescueApi.updateStatus(statusTarget.value.id, { status: statusForm.newStatus })
    }
    ElMessage.success(t('profile.statusUpdated'))
    statusDialogVisible.value = false
    await loadRecords()
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

async function offlineRecord(type, row) {
  try {
    await ElMessageBox.confirm(t('profile.offlineConfirmMsg'), t('common.warning'), { type: 'warning' })
    if (type === 'animal') {
      await animalApi.offline(row.id)
    } else {
      await rescueApi.offline(row.id)
    }
    ElMessage.success(t('profile.offlineSuccess'))
    await loadRecords()
  } catch (error) {
    if (error !== 'cancel') notifyError(error)
  }
}

async function cancelApplication(row) {
  try {
    await adoptionApi.cancel(row.id)
    ElMessage.success(t('profile.applicationCancelled'))
    await loadRecords()
  } catch (error) {
    notifyError(error)
  }
}

async function markRead(id) {
  try {
    await notificationApi.markRead(id)
    await loadRecords()
  } catch (error) {
    notifyError(error)
  }
}

async function markAllRead() {
  try {
    await notificationApi.markAllRead()
    await loadRecords()
  } catch (error) {
    notifyError(error)
  }
}

function openAppeal(targetType, targetId) {
  Object.assign(appealForm, { targetType, targetId, reason: '' })
  appealVisible.value = true
}

async function submitAppeal() {
  await appealFormRef.value.validate()
  saving.value = true
  try {
    await appealApi.create(appealForm)
    ElMessage.success(t('profile.appealSubmitted'))
    appealVisible.value = false
    await loadRecords()
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>
