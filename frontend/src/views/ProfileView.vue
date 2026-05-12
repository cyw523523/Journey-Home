<template>
  <section class="view page">
    <div class="section-head">
      <div>
        <h1>个人中心</h1>
        <p>维护资料，查看当前账号自己的发布记录和领养申请进度。</p>
      </div>
    </div>

    <el-alert
      v-if="auth.isAdmin.value"
      title="当前登录的是管理员账号。这里显示的是管理员账号自己发布过的动物、救助和申请记录；审核、用户管理、公告和统计请到管理员后台查看。"
      type="warning"
      :closable="false"
      style="margin-bottom: 16px"
    />

    <div class="profile-grid">
      <aside class="sidebar-panel surface">
        <div class="avatar-block">
          <el-avatar :src="getFullUrl(profile.avatarUrl)" :size="92">{{ profile.nickname?.slice(0, 1) }}</el-avatar>
          <h2>{{ profile.nickname }}</h2>
          <StatusTag :value="profile.status" :text="profile.statusText" :options="userStatusOptions" />
        </div>
        <el-form ref="profileRef" :model="profileForm" label-position="top">
          <el-form-item label="昵称">
            <el-input v-model="profileForm.nickname" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="profileForm.phone" />
          </el-form-item>
          <el-form-item label="头像">
            <ImageUploader v-model="avatarUrls" usage="avatar" :limit="1" />
          </el-form-item>
          <el-button :loading="saving" :icon="Save" type="primary" style="width: 100%" @click="saveProfile">保存资料</el-button>
        </el-form>
      </aside>

      <main class="content-panel surface">
        <el-tabs v-model="tab">
          <el-tab-pane label="动物档案" name="animals">
            <el-alert
              title="这里显示的是当前账号自己发布的动物档案，包括待审核、已驳回和已通过状态。"
              type="info"
              :closable="false"
              style="margin-bottom: 14px"
            />
            <el-table :data="animals" stripe>
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="typeText" label="类型" width="100" />
              <el-table-column prop="foundRegion" label="发现地区" />
              <el-table-column label="状态" width="120">
                <template #default="{ row }">
                  <StatusTag :value="row.status" :text="row.statusText" :options="animalStatusOptions" />
                </template>
              </el-table-column>
              <template #empty>
                <EmptyState
                  :title="auth.isAdmin.value ? '管理员账号还没有发布过动物档案' : '你还没有发布过动物档案'"
                  :description="auth.isAdmin.value ? '这是正常的。管理员主要在后台做审核和管理；只有管理员账号自己发布过档案，这里才会显示。' : '去“动物档案”页点击“发布档案”，提交后就会在这里看到记录。'"
                />
              </template>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="救助信息" name="rescues">
            <el-alert
              title="这里显示的是当前账号自己发布的救助信息。"
              type="info"
              :closable="false"
              style="margin-bottom: 14px"
            />
            <el-table :data="rescues" stripe>
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="location" label="地点" />
              <el-table-column prop="animalCondition" label="动物情况" />
              <el-table-column label="状态" width="120">
                <template #default="{ row }">
                  <StatusTag :value="row.status" :text="row.statusText" :options="rescueStatusOptions" />
                </template>
              </el-table-column>
              <template #empty>
                <EmptyState
                  :title="auth.isAdmin.value ? '管理员账号还没有发布过救助信息' : '你还没有发布过救助信息'"
                  :description="auth.isAdmin.value ? '管理员后台负责审核和管理；只有管理员账号自己发过救助信息，这里才会显示。' : '去“救助信息”页发布后，这里会显示你的记录和审核状态。'"
                />
              </template>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="领养申请" name="applications">
            <el-alert
              title="这里显示的是当前账号提交过的领养申请和审核结果。"
              type="info"
              :closable="false"
              style="margin-bottom: 14px"
            />
            <el-table :data="applications" stripe>
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="animalTypeText" label="动物" width="120" />
              <el-table-column prop="reason" label="领养理由" />
              <el-table-column label="状态" width="120">
                <template #default="{ row }">
                  <StatusTag :value="row.status" :text="row.statusText" :options="applyStatusOptions" />
                </template>
              </el-table-column>
              <el-table-column prop="auditOpinion" label="审核意见" />
              <template #empty>
                <EmptyState
                  :title="auth.isAdmin.value ? '管理员账号还没有提交过领养申请' : '你还没有提交过领养申请'"
                  :description="auth.isAdmin.value ? '管理员一般不会用管理员账号去提交领养申请，所以这里为空是正常的。' : '在动物详情页点击“提交领养申请”后，这里会看到进度。'"
                />
              </template>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="修改密码" name="password">
            <el-form ref="passwordRef" :model="passwordForm" :rules="passwordRules" label-position="top" style="max-width: 460px">
              <el-form-item label="原密码" prop="oldPassword">
                <el-input v-model="passwordForm.oldPassword" type="password" show-password />
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input v-model="passwordForm.newPassword" type="password" show-password />
              </el-form-item>
              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
              </el-form-item>
              <el-button :loading="saving" :icon="LockKeyhole" type="primary" @click="changePassword">修改密码</el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </main>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { LockKeyhole, Save } from 'lucide-vue-next'
import StatusTag from '../components/StatusTag.vue'
import ImageUploader from '../components/ImageUploader.vue'
import EmptyState from '../components/EmptyState.vue'
import { userApi } from '../api'
import { notifyError } from '../api/http'
import { useAuth } from '../stores/auth'
import { animalStatusOptions, applyStatusOptions, rescueStatusOptions, userStatusOptions } from '../utils/status'

const auth = useAuth()
const tab = ref('animals')
const saving = ref(false)
const profile = ref(auth.state.user || {})
const animals = ref([])
const rescues = ref([])
const applications = ref([])
const passwordRef = ref()
const avatarUrls = ref([])
const profileForm = reactive({ nickname: '', phone: '', avatarUrl: '' })
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const API_BASE = window.location.origin
function getFullUrl(url) {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:')) {
    return url
  }
  return `${API_BASE}${url}`
}
const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { min: 6, max: 32, message: '密码长度需为6-32位', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请确认密码', trigger: 'blur' }]
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
    const [myAnimals, myRescues, myApplications] = await Promise.all([
      userApi.animals({ page: 0, size: 20 }),
      userApi.rescues({ page: 0, size: 20 }),
      userApi.applications({ page: 0, size: 20 })
    ])
    animals.value = myAnimals.content || []
    rescues.value = myRescues.content || []
    applications.value = myApplications.content || []
  } catch (error) {
    notifyError(error)
  }
}

async function saveProfile() {
  saving.value = true
  try {
    profileForm.avatarUrl = avatarUrls.value[0] || ''
    const updatedProfile = await userApi.update(profileForm)
    profile.value = updatedProfile
    auth.state.user = updatedProfile
    localStorage.setItem('guitu_user', JSON.stringify(updatedProfile))
    ElMessage.success('资料已更新')
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

async function changePassword() {
  await passwordRef.value.validate()
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  saving.value = true
  try {
    await userApi.changePassword(passwordForm)
    ElMessage.success('密码已更新')
    Object.assign(passwordForm, { oldPassword: '', newPassword: '', confirmPassword: '' })
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>
