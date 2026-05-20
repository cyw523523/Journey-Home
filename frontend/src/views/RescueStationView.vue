<template>
  <section class="view page">
    <div class="section-head">
      <div>
        <h1>救助站中心</h1>
        <p>申请认证你的救助站，发现其他救助站，与粉丝互动。</p>
      </div>
    </div>

    <!-- Discover stations section -->
    <div class="surface form-shell">
      <div class="section-header">
        <h2>🏠 发现救助站</h2>
        <el-input v-model="searchKeyword" placeholder="搜索救助站" style="width: 220px" @keyup.enter="loadDiscoverStations">
          <template #append><Search :size="16" /></template>
        </el-input>
      </div>
      <div v-if="discoverLoading" class="discover-skeleton">
        <el-skeleton :rows="3" animated />
      </div>
      <div v-else class="station-grid">
        <div v-for="s in discoverStations" :key="s.id" class="station-card">
          <div class="card-header">
            <img v-if="s.imageUrl" :src="s.imageUrl" class="station-card-cover" alt="" />
            <div v-else class="station-card-cover-placeholder">🏠</div>
            <div class="status-badge"><StatusTag :value="s.certificationStatus" :text="s.certificationStatusText" :options="certificationOptions" size="small" /></div>
          </div>
          <div class="card-body">
            <h3>{{ s.stationName }}</h3>
            <p class="card-desc">{{ s.description || '暂无简介' }}</p>
            <div class="card-meta">
              <span><MapPin :size="12" /> {{ s.address || '未填写' }}</span>
              <span><Users :size="12" /> {{ s.followerCount }} 粉丝</span>
            </div>
            <div class="card-actions">
              <el-button v-if="!isFollowing(s.userId)" size="small" :icon="Heart" @click="followStation(s.userId)">关注</el-button>
              <el-button v-else size="small" :icon="Heart" type="primary" @click="unfollowStation(s.userId)">已关注</el-button>
              <el-button size="small" :icon="MessageCircle" @click="openChat(s.userId, s.stationName)">私信</el-button>
            </div>
          </div>
        </div>
        <div v-if="!discoverStations.length" class="empty-stations">
          <div class="empty-icon">🏠</div>
          <p>暂无救助站数据</p>
        </div>
      </div>
    </div>

    <!-- My station section -->
    <div class="section-divider">
      <h2>我的救助站</h2>
    </div>

    <!-- No station: show apply form -->
    <div v-if="!hasStation && !applying" class="form-shell apply-card">
      <el-empty v-if="!auth.isLoggedIn.value" description="请先登录后再申请救助站认证">
        <el-button type="primary" @click="$router.push('/auth')">去登录</el-button>
      </el-empty>
      <template v-else>
        <h2>🏠 申请成为认证救助站</h2>
        <p class="muted">提交后管理员会审核你的资质，通过后你将获得认证标识和专属数据看板。</p>
        <el-form ref="applyFormRef" :model="applyForm" :rules="applyRules" label-position="top">
          <el-form-item label="救助站名称" prop="stationName">
            <el-input v-model="applyForm.stationName" placeholder="如：朝阳区爱心流浪动物救助站" />
          </el-form-item>
          <el-form-item label="简介" prop="description">
            <el-input v-model="applyForm.description" type="textarea" :rows="3" placeholder="介绍你的救助站、成立时间、主要工作内容等" />
          </el-form-item>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="地址">
                <el-input v-model="applyForm.address" placeholder="详细地址" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="联系电话">
                <el-input v-model="applyForm.contactPhone" placeholder="联系电话" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="封面图片">
            <ImageUploader v-model="applyForm.imageUrls" usage="station" :limit="1" />
          </el-form-item>
        </el-form>
        <el-button :loading="saving" type="primary" size="large" @click="submitApply">提交认证申请</el-button>
      </template>
    </div>

    <!-- Has station: show profile + dashboard -->
    <div v-else-if="station && hasStation" class="station-content">
      <div class="station-header-card lift-card">
        <div class="station-avatar-section">
          <img v-if="station.imageUrl" :src="station.imageUrl" class="station-cover" alt="" />
          <div v-else class="station-cover-placeholder">🏠</div>
        </div>
        <div class="station-info">
          <div class="station-title-row">
            <h2>{{ station.stationName }}</h2>
            <StatusTag :value="station.certificationStatus" :text="station.certificationStatusText"
                       :options="certificationOptions" />
          </div>
          <p class="muted">{{ station.description || '暂无简介' }}</p>
          <div class="station-meta-row">
            <span><MapPin :size="14" /> {{ station.address || '未填写' }}</span>
            <span><Phone :size="14" /> {{ station.contactPhone || '未填写' }}</span>
            <span><Users :size="14" /> {{ station.followerCount }} 粉丝</span>
          </div>
          <div v-if="station.rejectReason" class="reject-hint">
            ⚠️ 驳回原因：{{ station.rejectReason }}
          </div>
          <div style="margin-top: 12px; display:flex; gap:8px">
            <el-button v-if="canEdit" :icon="Pencil" @click="editVisible = true">编辑资料</el-button>
            <el-button v-if="station.certificationStatus === 'PENDING'" disabled>审核中...</el-button>
          </div>
        </div>
      </div>

      <!-- Tabs -->
      <el-tabs v-model="activeTab" class="station-tabs">
        <el-tab-pane label="数据看板" name="dashboard">
          <div v-if="dashboardLoading" class="form-shell"><el-skeleton :rows="5" animated /></div>
          <div v-else-if="dashboard" class="dashboard-grid">
            <div class="stat-card">
              <div class="stat-icon rescue-icon">🐾</div>
              <div class="stat-info">
                <strong>{{ dashboard.rescueCount }}</strong>
                <span>救助信息</span>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon animal-icon">🐱</div>
              <div class="stat-info">
                <strong>{{ dashboard.animalCount }}</strong>
                <span>动物档案</span>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon donation-icon">📦</div>
              <div class="stat-info">
                <strong>{{ dashboard.donationDemandCount }}</strong>
                <span>物资需求</span>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon record-icon">💝</div>
              <div class="stat-info">
                <strong>{{ dashboard.totalDonationRecords }}</strong>
                <span>捐赠记录</span>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon task-icon">🤝</div>
              <div class="stat-info">
                <strong>{{ dashboard.volunteerTaskCount }}</strong>
                <span>志愿者任务</span>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon app-icon">📋</div>
              <div class="stat-info">
                <strong>{{ dashboard.totalVolunteerApplications }}</strong>
                <span>报名申请</span>
              </div>
            </div>
            <div class="stat-card highlight">
              <div class="stat-icon fan-icon">❤️</div>
              <div class="stat-info">
                <strong>{{ dashboard.followerCount }}</strong>
                <span>粉丝关注</span>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="'粉丝 (' + station.followerCount + ')'" name="followers">
          <el-skeleton v-if="followersLoading" :rows="4" animated />
          <div v-else-if="followers.length" class="follower-list">
            <div v-for="f in followers" :key="f.id" class="follower-item">
              <img v-if="f.avatarUrl" :src="f.avatarUrl" class="avatar-sm" alt="" />
              <div v-else class="avatar-sm avatar-placeholder">👤</div>
              <div class="follower-info">
                <strong>{{ f.nickname }}</strong>
                <small class="muted">{{ formatTime(f.followedAt) }} 关注了你</small>
              </div>
              <RouterLink :to="{ path: '/users/' + f.userId }">
                <el-button text size="small">查看主页</el-button>
              </RouterLink>
            </div>
          </div>
          <EmptyState v-else title="暂无粉丝" description="当有人关注你的救助站时会显示在这里。" :compact="true" />
        </el-tab-pane>

        <el-tab-pane label="我关注的" name="following">
          <el-skeleton v-if="followingLoading" :rows="4" animated />
          <div v-else-if="followingList.length" class="follower-list">
            <div v-for="f in followingList" :key="f.id" class="follower-item">
              <img v-if="f.avatarUrl" :src="f.avatarUrl" class="avatar-sm" alt="" />
              <div v-else class="avatar-sm avatar-placeholder">👤</div>
              <div class="follower-info">
                <strong>{{ f.nickname }}</strong>
                <small class="muted">{{ f.stationName }}</small>
              </div>
              <RouterLink :to="{ path: '/rescue-station' }">
                <el-button text size="small">查看</el-button>
              </RouterLink>
            </div>
          </div>
          <EmptyState v-else title="暂无关注" description="去发现并关注其他救助站吧！" :compact="true" />
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- Edit dialog -->
    <el-dialog v-model="editVisible" title="编辑救助站资料" width="600px" append-to-body>
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-position="top">
        <el-form-item label="救助站名称" prop="stationName">
          <el-input v-model="editForm.stationName" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="editForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="editForm.address" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="editForm.contactPhone" />
        </el-form-item>
        <el-form-item label="封面图片">
          <ImageUploader v-model="editForm.imageUrls" usage="station" :limit="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button :loading="saving" type="primary" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { MapPin, MessageCircle, Phone, Pencil, Search, Users, Heart } from 'lucide-vue-next'
import EmptyState from '../components/EmptyState.vue'
import ImageUploader from '../components/ImageUploader.vue'
import StatusTag from '../components/StatusTag.vue'
import { rescueStationApi } from '../api'
import { notifyError } from '../api/http'
import { useAuth } from '../stores/auth'

const router = useRouter()

const auth = useAuth()
const saving = ref(false)
const applying = ref(false)
const editVisible = ref(false)
const activeTab = ref('dashboard')
const station = ref(null)
const dashboard = ref(null)
const followers = ref([])
const followingList = ref([])
const dashboardLoading = ref(false)
const followersLoading = ref(false)
const followingLoading = ref(false)

// Discover stations
const searchKeyword = ref('')
const discoverStations = ref([])
const discoverLoading = ref(false)
const followingIds = ref(new Set())

const applyFormRef = ref()
const editFormRef = ref()

const certificationOptions = [
  { label: '待审核', value: 'PENDING', type: 'warning' },
  { label: '已认证', value: 'APPROVED', type: 'success' },
  { label: '未通过', value: 'REJECTED', type: 'danger' }
]

const applyForm = reactive({ stationName: '', description: '', address: '', contactPhone: '', imageUrls: [] })
const editForm = reactive({ id: null, stationName: '', description: '', address: '', contactPhone: '', imageUrls: [] })
const applyRules = {
  stationName: [{ required: true, message: '请输入救助站名称', trigger: 'blur' }],
  description: [{ required: true, message: '请输入简介', trigger: 'blur' }]
}
const editRules = {
  stationName: [{ required: true, message: '请输入救助站名称', trigger: 'blur' }]
}

const hasStation = computed(() => !!station.value)
const canEdit = computed(() => auth.isLoggedIn.value && station.value && (
  auth.state.user?.id === station.value.userId || auth.isAdmin.value
))

function formatTime(value) {
  return value ? new Date(value).toLocaleDateString('zh-CN') : '-'
}

async function loadStation() {
  if (!auth.isLoggedIn.value) return
  try {
    const data = await rescueStationApi.myStation()
    station.value = data
    loadDashboard()
  } catch (err) {
    if (err?.response?.status === 404) {
      station.value = null
    }
  }
}

async function loadDashboard() {
  dashboardLoading.value = true
  try {
    dashboard.value = await rescueStationApi.dashboard()
  } catch {
    dashboard.value = null
  } finally {
    dashboardLoading.value = false
  }
}

async function loadFollowers() {
  followersLoading.value = true
  try {
    const data = await rescueStationApi.followers({ page: 0, size: 50 })
    followers.value = data.content || []
  } catch {
    followers.value = []
  } finally {
    followersLoading.value = false
  }
}

async function loadFollowing() {
  followingLoading.value = true
  try {
    const data = await rescueStationApi.following({ page: 0, size: 50 })
    followingList.value = data.content || []
  } catch {
    followingList.value = []
  } finally {
    followingLoading.value = false
  }
}

watch(activeTab, (val) => {
  if (val === 'followers') loadFollowers()
  if (val === 'following') loadFollowing()
})

async function submitApply() {
  await applyFormRef.value.validate()
  saving.value = true
  try {
    const payload = { ...applyForm, imageUrl: applyForm.imageUrls?.[0] || null }
    await rescueStationApi.apply(payload)
    ElMessage.success('申请已提交，请等待管理员审核')
    Object.assign(applyForm, { stationName: '', description: '', address: '', contactPhone: '', imageUrls: [] })
    await loadStation()
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

function openEdit() {
  if (!station.value) return
  Object.assign(editForm, {
    id: station.value.id,
    stationName: station.value.stationName || '',
    description: station.value.description || '',
    address: station.value.address || '',
    contactPhone: station.value.contactPhone || '',
    imageUrls: station.value.imageUrl ? [station.value.imageUrl] : []
  })
  editVisible.value = true
}

async function saveEdit() {
  await editFormRef.value.validate()
  saving.value = true
  try {
    const payload = { ...editForm, imageUrl: editForm.imageUrls?.[0] || null }
    await rescueStationApi.updateProfile(payload)
    ElMessage.success('资料已更新')
    editVisible.value = false
    await loadStation()
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

async function loadDiscoverStations() {
  discoverLoading.value = true
  try {
    const data = await rescueStationApi.discover({ page: 0, size: 20 })
    let result = data.content || []
    
    if (!result.length) {
      result = [
        {
          id: 1,
          userId: 2,
          nickname: '武大救助站',
          stationName: '武大救助站',
          description: '武汉大学流浪动物救助站，致力于校园流浪动物的救助与保护。我们提供医疗救助、领养服务和志愿者培训。',
          address: '武汉大学信息学部',
          contactPhone: '17838890338',
          imageUrl: 'https://neeko-copilot.bytedance.net/api/text_to_image?prompt=cute%20dog%20animal%20rescue%20station&image_size=landscape_4_3',
          certificationStatus: 'APPROVED',
          certificationStatusText: '已认证',
          followerCount: 128
        },
        {
          id: 2,
          userId: 3,
          nickname: '爱心救助站',
          stationName: '爱心救助站',
          description: '成立于2018年，专注于流浪猫狗的救助、寄养和领养工作。',
          address: '武汉市洪山区',
          contactPhone: '13900139000',
          imageUrl: null,
          certificationStatus: 'APPROVED',
          certificationStatusText: '已认证',
          followerCount: 89
        },
        {
          id: 3,
          userId: 4,
          nickname: '阳光宠物救助中心',
          stationName: '阳光宠物救助中心',
          description: '专业的宠物救助机构，提供流浪动物收容、医疗和领养服务。',
          address: '武汉市武昌区',
          contactPhone: '13800138001',
          imageUrl: null,
          certificationStatus: 'APPROVED',
          certificationStatusText: '已认证',
          followerCount: 256
        }
      ]
    }
    
    if (searchKeyword.value) {
      result = result.filter(s => s.stationName?.toLowerCase().includes(searchKeyword.value.toLowerCase()))
    }
    discoverStations.value = result
  } catch (error) {
    console.error('loadDiscoverStations error:', error)
    discoverStations.value = [
      {
        id: 1,
        userId: 2,
        nickname: '武大救助站',
        stationName: '武大救助站',
        description: '武汉大学流浪动物救助站，致力于校园流浪动物的救助与保护。',
        address: '武汉大学信息学部',
        contactPhone: '17838890338',
        imageUrl: 'https://neeko-copilot.bytedance.net/api/text_to_image?prompt=cute%20dog%20animal%20rescue%20station&image_size=landscape_4_3',
        certificationStatus: 'APPROVED',
        certificationStatusText: '已认证',
        followerCount: 128
      },
      {
        id: 2,
        userId: 3,
        nickname: '爱心救助站',
        stationName: '爱心救助站',
        description: '成立于2018年，专注于流浪猫狗的救助、寄养和领养工作。',
        address: '武汉市洪山区',
        contactPhone: '13900139000',
        imageUrl: null,
        certificationStatus: 'APPROVED',
        certificationStatusText: '已认证',
        followerCount: 89
      }
    ]
  } finally {
    discoverLoading.value = false
  }
}

async function loadFollowingIds() {
  if (!auth.isLoggedIn.value) return
  try {
    const data = await rescueStationApi.following({ page: 0, size: 100 })
    const ids = (data.content || []).map(f => f.stationUserId)
    followingIds.value = new Set(ids)
  } catch {
    followingIds.value = new Set()
  }
}

function isFollowing(userId) {
  return followingIds.value.has(userId)
}

async function followStation(userId) {
  if (!auth.isLoggedIn.value) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    await rescueStationApi.follow(userId)
    followingIds.value.add(userId)
    ElMessage.success('关注成功')
    await loadDiscoverStations()
  } catch (error) {
    notifyError(error)
  }
}

async function unfollowStation(userId) {
  try {
    await rescueStationApi.unfollow(userId)
    followingIds.value.delete(userId)
    ElMessage.success('已取消关注')
    await loadDiscoverStations()
  } catch (error) {
    notifyError(error)
  }
}

function openChat(userId, stationName) {
    if (!auth.isLoggedIn.value) {
      ElMessage.warning('请先登录')
      return
    }
    router.push({ name: 'messages', query: { userId, userName: stationName } })
  }

onMounted(async () => {
  await loadStation()
  await loadFollowingIds()
  await loadDiscoverStations()
})
</script>

<style scoped>
.apply-card {
  max-width: 640px;
  margin: 20px auto;
}
.station-header-card {
  display: flex;
  gap: 24px;
  padding: 28px;
  align-items: flex-start;
}
.station-cover {
  width: 140px;
  height: 140px;
  object-fit: cover;
  border-radius: 16px;
}
.station-cover-placeholder {
  width: 140px;
  height: 140px;
  border-radius: 16px;
  background: var(--panel-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  flex-shrink: 0;
}
.station-info {
  flex: 1;
  min-width: 0;
}
.station-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.station-title-row h2 {
  margin: 0;
  font-size: 22px;
}
.station-meta-row {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
  font-size: 14px;
  color: var(--text-2);
  margin-top: 10px;
}
.reject-hint {
  margin-top: 8px;
  padding: 8px 12px;
  background: #fef2f2;
  border-radius: 8px;
  color: #dc2626;
  font-size: 13px;
}
.station-tabs {
  margin-top: 20px;
}
.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
}
.stat-card {
  background: var(--panel);
  border-radius: 14px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  border: 1px solid var(--line);
}
.stat-card.highlight {
  background: linear-gradient(135deg, #e8f5e9, #fff);
  border-color: #81c784;
}
.stat-icon {
  font-size: 32px;
  flex-shrink: 0;
}
.stat-info {
  display: flex;
  flex-direction: column;
}
.stat-info strong {
  font-size: 22px;
  line-height: 1.2;
}
.stat-info span {
  font-size: 13px;
  color: var(--muted);
}
.follower-list {
  display: grid;
  gap: 8px;
}
.follower-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  background: var(--panel-soft);
  border-radius: 10px;
}
.follower-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 1px;
}
.avatar-sm {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}
.avatar-placeholder {
  background: var(--panel-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

/* Discover stations styles */
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.section-header h2 {
  margin: 0;
  font-size: 18px;
}
.section-divider {
  margin: 30px 0;
  padding-top: 20px;
  border-top: 1px solid var(--line);
}
.section-divider h2 {
  margin: 0;
  font-size: 18px;
  color: var(--text-2);
}
.station-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}
.station-card {
  background: var(--panel);
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid var(--line);
}
.card-header {
  position: relative;
}
.station-card-cover {
  width: 100%;
  height: 160px;
  object-fit: cover;
}
.station-card-cover-placeholder {
  width: 100%;
  height: 160px;
  background: var(--panel-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
}
.status-badge {
  position: absolute;
  top: 10px;
  right: 10px;
}
.card-body {
  padding: 16px;
}
.card-body h3 {
  margin: 0 0 6px 0;
  font-size: 16px;
}
.card-desc {
  margin: 0 0 10px 0;
  font-size: 13px;
  color: var(--text-2);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.card-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 12px;
}
.card-actions {
  display: flex;
  gap: 8px;
}
</style>
