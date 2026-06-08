<template>
  <section class="view page">
    <div class="section-head">
      <div>
        <h1>{{ t('profilePage.title') }}</h1>
        <p>{{ t('profilePage.description') }}</p>
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
          <el-form-item :label="t('auth.nickname')">
            <el-input v-model="profileForm.nickname" />
          </el-form-item>
          <el-form-item :label="t('auth.phone')">
            <el-input v-model="profileForm.phone" />
          </el-form-item>
          <el-form-item :label="t('profilePage.avatar')">
            <ImageUploader v-model="avatarUrls" usage="avatar" :limit="1" />
          </el-form-item>
          <el-button :loading="saving" :icon="Save" type="primary" style="width: 100%" @click="saveProfile">{{ t('profilePage.saveProfile') }}</el-button>
        </el-form>
      </aside>

      <main class="content-panel surface">
        <el-tabs v-model="tab">
          <el-tab-pane :label="t('nav.animals')" name="animals">
            <el-table :data="animals" stripe>
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="typeText" label="类型" width="120" />
              <el-table-column prop="foundRegion" label="发现地区" />
              <el-table-column label="状态" width="140">
                <template #default="{ row }">
                  <StatusTag :value="row.status" :text="row.statusText" :options="animalStatusOptions" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="280">
                <template #default="{ row }">
                  <el-button size="small" text @click="openAnimalEditor(row)">编辑</el-button>
                  <el-button size="small" text @click="openStatusDialog('animal', row)">状态</el-button>
                  <el-button size="small" text type="danger" @click="offlineRecord('animal', row)">下架</el-button>
                  <el-button v-if="['REJECTED', 'OFFLINE'].includes(row.status)" size="small" text type="warning" @click="openAppeal('ANIMAL', row.id)">申诉</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="t('nav.rescues')" name="rescues">
            <el-table :data="rescues" stripe>
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="location" label="地点" />
              <el-table-column prop="animalCondition" label="动物情况" />
              <el-table-column label="状态" width="140">
                <template #default="{ row }">
                  <StatusTag :value="row.status" :text="row.statusText" :options="rescueStatusOptions" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="280">
                <template #default="{ row }">
                  <el-button size="small" text @click="openRescueEditor(row)">编辑</el-button>
                  <el-button size="small" text @click="openStatusDialog('rescue', row)">状态</el-button>
                  <el-button size="small" text type="danger" @click="offlineRecord('rescue', row)">下架</el-button>
                  <el-button v-if="['REJECTED', 'OFFLINE'].includes(row.status)" size="small" text type="warning" @click="openAppeal('RESCUE', row.id)">申诉</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="t('profilePage.adoptionApplications')" name="applications">
            <el-table :data="applications" stripe>
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="animalTypeText" label="动物" width="120" />
              <el-table-column prop="reason" label="领养理由" />
              <el-table-column label="状态" width="140">
                <template #default="{ row }">
                  <StatusTag :value="row.status" :text="row.statusText" :options="applyStatusOptions" />
                </template>
              </el-table-column>
              <el-table-column prop="auditOpinion" label="审核意见" />
              <el-table-column label="操作" width="320">
                <template #default="{ row }">
                  <el-button v-if="row.status === 'APPROVED'" size="small" text type="primary" @click="openAgreement(row)">协议</el-button>
                  <el-button v-if="row.status === 'APPROVED'" size="small" text @click="openFollowUps(row)">回访</el-button>
                  <el-button v-if="row.status === 'PENDING_REVIEW'" size="small" text type="danger" @click="cancelApplication(row)">取消</el-button>
                  <el-button v-if="row.status === 'REJECTED'" size="small" text type="warning" @click="openAppeal('ADOPT_APPLY', row.id)">申诉</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="t('profilePage.adoptionFollowUp')" name="managedApplications">
            <el-table :data="managedApplications" stripe>
              <el-table-column prop="id" label="申请ID" width="90" />
              <el-table-column prop="animalTypeText" label="动物" width="100" />
              <el-table-column prop="applicantName" label="申请人" width="120" />
              <el-table-column prop="contact" label="联系方式" width="140" />
              <el-table-column label="状态" width="140">
                <template #default="{ row }">
                  <StatusTag :value="row.status" :text="row.statusText" :options="applyStatusOptions" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="240">
                <template #default="{ row }">
                  <el-button v-if="row.status === 'APPROVED'" size="small" text type="primary" @click="openAgreement(row)">协议</el-button>
                  <el-button v-if="row.status === 'APPROVED'" size="small" text @click="openFollowUps(row)">回访</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="t('statusLabel.communityPost')" name="communityPosts">
            <el-table :data="communityPosts" stripe>
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="title" label="标题" />
              <el-table-column label="状态" width="140">
                <template #default="{ row }">
                  <StatusTag :value="row.status" :text="row.statusText" :options="communityPostStatusOptions" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="180">
                <template #default="{ row }">
                  <el-button v-if="['REJECTED', 'OFFLINE'].includes(row.status)" size="small" text type="warning" @click="openAppeal('COMMUNITY_POST', row.id)">申诉</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="t('statusLabel.communityComment')" name="communityComments">
            <el-table :data="communityComments" stripe>
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="content" label="评论内容" />
              <el-table-column label="状态" width="140">
                <template #default="{ row }">
                  <StatusTag :value="row.status" :text="row.statusText" :options="communityCommentStatusOptions" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="180">
                <template #default="{ row }">
                  <el-button v-if="['REJECTED', 'OFFLINE'].includes(row.status)" size="small" text type="warning" @click="openAppeal('COMMUNITY_COMMENT', row.id)">申诉</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="`${t('notification.notifications')}${notificationSummary.unreadCount ? `(${notificationSummary.unreadCount})` : ''}`" name="notifications">
            <div class="toolbar" style="justify-content:flex-end;margin-bottom:12px">
              <el-button text @click="markAllRead">{{ t('notification.markAllRead') }}</el-button>
            </div>
              <el-table :data="notifications" stripe>
              <el-table-column :label="t('admin.noticeTitle')" width="220">
                <template #default="{ row }">{{ notificationTitle(row.title) }}</template>
              </el-table-column>
              <el-table-column :label="t('admin.content')">
                <template #default="{ row }">{{ formatNotificationContent(row) }}</template>
              </el-table-column>
              <el-table-column :label="t('admin.status')" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.readFlag ? 'info' : 'success'">{{ row.readFlag ? t('notification.read') : t('notification.unread') }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createdAt" :label="t('profilePage.time')" width="180">
                <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
              </el-table-column>
              <el-table-column :label="t('admin.action')" width="100">
                <template #default="{ row }">
                  <el-button v-if="!row.readFlag" size="small" text @click="markRead(row.id)">{{ t('profilePage.markRead') }}</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="t('profilePage.myReports')" name="reports">
            <el-table :data="reports" stripe>
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="targetTypeText" label="举报对象" width="120" />
              <el-table-column prop="reasonTypeText" label="原因" width="120" />
              <el-table-column prop="description" label="说明" />
              <el-table-column label="状态" width="140">
                <template #default="{ row }">
                  <StatusTag :value="row.status" :text="row.statusText" :options="reportStatusOptions" />
                </template>
              </el-table-column>
              <el-table-column prop="resolutionOpinion" label="处理结果" />
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="t('profilePage.myAppeals')" name="appeals">
            <el-table :data="appeals" stripe>
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="targetTypeText" label="申诉对象" width="120" />
              <el-table-column prop="reason" label="申诉原因" />
              <el-table-column label="状态" width="150">
                <template #default="{ row }">
                  <StatusTag :value="row.status" :text="row.statusText" :options="appealStatusOptions" />
                </template>
              </el-table-column>
              <el-table-column prop="finalReviewOpinion" label="复核结果" />
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="t('profilePage.changePassword')" name="password">
            <el-form ref="passwordRef" :model="passwordForm" :rules="passwordRules" label-position="top" style="max-width: 460px">
              <el-form-item :label="t('profilePage.oldPassword')" prop="oldPassword">
                <el-input v-model="passwordForm.oldPassword" type="password" show-password />
              </el-form-item>
              <el-form-item :label="t('profilePage.newPassword')" prop="newPassword">
                <el-input v-model="passwordForm.newPassword" type="password" show-password />
              </el-form-item>
              <el-form-item :label="t('auth.confirmPassword')" prop="confirmPassword">
                <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
              </el-form-item>
              <el-button :loading="saving" :icon="LockKeyhole" type="primary" @click="changePassword">{{ t('profilePage.changePassword') }}</el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </main>
    </div>

    <el-dialog v-model="animalEditorVisible" title="编辑动物档案" width="720px" append-to-body>
      <el-form ref="animalFormRef" :model="animalEditor" :rules="animalRules" label-position="top">
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="动物类型" prop="type">
              <el-select v-model="animalEditor.type" style="width: 100%">
                <el-option v-for="item in animalTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="性别" prop="gender">
              <el-select v-model="animalEditor.gender" style="width: 100%">
                <el-option v-for="item in genderOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="年龄" prop="age">
              <el-input-number v-model="animalEditor.age" :min="0" :max="30" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="发现地区" prop="foundRegion">
          <el-input v-model="animalEditor.foundRegion" />
        </el-form-item>
        <el-form-item label="健康情况">
          <el-input v-model="animalEditor.healthCondition" />
        </el-form-item>
        <el-form-item label="照片">
          <ImageUploader v-model="animalEditor.imageUrls" usage="animal" />
        </el-form-item>
        <el-form-item label="详细说明">
          <el-input v-model="animalEditor.description" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="animalEditorVisible = false">取消</el-button>
        <el-button :loading="saving" type="primary" @click="saveAnimal">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rescueEditorVisible" title="编辑救助信息" width="720px" append-to-body>
      <el-form ref="rescueFormRef" :model="rescueEditor" :rules="rescueRules" label-position="top">
        <el-form-item label="救助地点" prop="location">
          <el-input v-model="rescueEditor.location" />
        </el-form-item>
        <el-form-item label="动物情况" prop="animalCondition">
          <el-input v-model="rescueEditor.animalCondition" />
        </el-form-item>
        <el-form-item label="联系方式" prop="contact">
          <el-input v-model="rescueEditor.contact" />
        </el-form-item>
        <el-form-item label="求助说明" prop="description">
          <el-input v-model="rescueEditor.description" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="现场图片">
          <ImageUploader v-model="rescueEditor.imageUrls" usage="rescue" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rescueEditorVisible = false">取消</el-button>
        <el-button :loading="saving" type="primary" @click="saveRescue">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="statusDialogVisible" title="更新状态" width="460px" append-to-body>
      <el-form label-position="top">
        <el-form-item label="新状态">
          <el-select v-model="statusForm.newStatus" style="width: 100%">
            <el-option v-for="item in availableStatuses" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusDialogVisible = false">取消</el-button>
        <el-button :loading="saving" type="primary" @click="saveStatus">更新</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="appealVisible" title="提交申诉" width="560px" append-to-body>
      <el-form ref="appealFormRef" :model="appealForm" :rules="appealRules" label-position="top">
        <el-form-item label="申诉对象">
          <el-input :model-value="optionText(appealTargetOptions, appealForm.targetType)" disabled />
        </el-form-item>
        <el-form-item label="申诉理由" prop="reason">
          <el-input v-model="appealForm.reason" type="textarea" :rows="5" maxlength="1000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="appealVisible = false">取消</el-button>
        <el-button :loading="saving" type="primary" @click="submitAppeal">提交申诉</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="agreementDialogVisible" title="领养协议" width="780px" append-to-body>
      <div v-if="agreementData" class="agreement-shell">
        <div class="agreement-meta">
          <div>
            <strong>{{ agreementData.title }}</strong>
            <p class="muted">协议编号：{{ agreementData.agreementNo }}</p>
          </div>
          <StatusTag :value="agreementData.status" :text="agreementData.statusText" :options="agreementStatusOptions" />
        </div>
        <div class="agreement-actions">
          <template v-if="canEditAgreement">
            <el-button v-if="!agreementEditMode" text @click="startEditAgreement">编辑协议</el-button>
            <el-button v-else text @click="cancelEditAgreement">取消编辑</el-button>
            <el-button v-if="agreementEditMode" :loading="saving" text type="primary" @click="saveAgreement">保存协议</el-button>
          </template>
          <el-button v-if="agreementData.pdfUrl" text type="primary" @click="openAgreementPdf">下载 PDF</el-button>
        </div>
        <div v-if="agreementEditMode" class="agreement-edit-box">
          <el-form label-position="top">
            <el-form-item label="协议标题">
              <el-input v-model="agreementEditForm.title" maxlength="120" />
            </el-form-item>
            <el-form-item label="协议正文">
              <el-input v-model="agreementEditForm.content" type="textarea" :rows="14" maxlength="20000" show-word-limit />
            </el-form-item>
          </el-form>
        </div>
        <div v-else class="agreement-content">{{ agreementData.content }}</div>
        <div class="agreement-sign-grid">
          <div class="detail-item">
            <label>领养人签署</label>
            <span>{{ agreementData.adopterSignatureName || '未签署' }}</span>
            <img
              v-if="signatureImageUrl(agreementData, 'adopter')"
              :src="signatureImageUrl(agreementData, 'adopter')"
              alt="领养人签名"
              class="signature-preview"
            />
            <small>{{ formatTime(agreementData.adopterSignedAt) }}</small>
          </div>
          <div class="detail-item">
            <label>救助方签署</label>
            <span>{{ agreementData.counterpartSignatureName || '未签署' }}</span>
            <img
              v-if="signatureImageUrl(agreementData, 'counterpart')"
              :src="signatureImageUrl(agreementData, 'counterpart')"
              alt="救助方签名"
              class="signature-preview"
            />
            <small>{{ formatTime(agreementData.counterpartSignedAt) }}</small>
          </div>
        </div>
        <div v-if="canSignAgreement" class="agreement-sign-box">
          <el-form label-position="top">
            <el-form-item label="签署姓名">
              <el-input v-model="agreementSignatureForm.signatureName" maxlength="64" />
            </el-form-item>
            <el-form-item label="手写签名">
              <div class="signature-pad-shell">
                <canvas
                  ref="signatureCanvasRef"
                  class="signature-canvas"
                  @pointerdown="startSignature"
                  @pointermove="moveSignature"
                  @pointerup="finishSignature"
                  @pointerleave="finishSignature"
                />
                <div class="signature-pad-meta">
                  <span>{{ signatureHasStroke ? '已采集签名' : '请在框内完成签名，提交后将随协议一并保存' }}</span>
                  <el-button text @click="clearSignature">清空</el-button>
                </div>
              </div>
            </el-form-item>
          </el-form>
        </div>
      </div>
      <template #footer>
        <el-button @click="agreementDialogVisible = false">关闭</el-button>
        <el-button v-if="canSignAgreement" :loading="saving" type="primary" @click="signAgreement">确认签署</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="followUpDialogVisible" title="领养回访" width="760px" append-to-body>
      <div v-if="followUps.length" class="follow-up-list">
        <div v-for="item in followUps" :key="item.id" class="follow-up-card">
          <div class="follow-up-head">
            <div>
              <strong>{{ item.stageLabel }}</strong>
              <p class="muted">计划时间：{{ formatTime(item.plannedAt) }}</p>
            </div>
            <StatusTag :value="item.status" :text="item.statusText" :options="followUpStatusOptions" />
          </div>
          <p class="follow-up-note">{{ item.note || '暂未填写回访内容' }}</p>
          <div v-if="item.imageUrls?.length" class="detail-thumb-row">
            <img v-for="url in item.imageUrls" :key="url" :src="getFullUrl(url)" style="width:88px;height:88px;object-fit:cover;border-radius:8px" />
          </div>
          <p class="muted">填写人：{{ item.creatorNickname || '-' }}，完成时间：{{ formatTime(item.completedAt) }}</p>
          <p v-if="followUpManageMode && !canCompleteFollowUp(item) && item.status !== 'COMPLETED'" class="follow-up-waiting-tip">
            尚未到回访时间，暂不可填写
          </p>
          <div v-if="followUpManageMode && canCompleteFollowUp(item)" class="follow-up-actions">
            <el-button size="small" text @click="openFollowUpPlanEditor(item)">调整时间</el-button>
            <el-button size="small" type="primary" plain @click="openFollowUpComplete(item)">填写回访</el-button>
          </div>
          <div v-else-if="followUpManageMode && item.status !== 'COMPLETED'" class="follow-up-actions">
            <el-button size="small" text @click="openFollowUpPlanEditor(item)">调整时间</el-button>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无回访计划" />
      <template #footer>
        <el-button @click="followUpDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="followUpEditorVisible" title="填写回访记录" width="600px" append-to-body>
      <el-form label-position="top">
        <el-form-item label="回访内容">
          <el-input v-model="followUpEditor.note" type="textarea" :rows="5" maxlength="1000" show-word-limit />
        </el-form-item>
        <el-form-item label="回访图片">
          <ImageUploader v-model="followUpEditor.imageUrls" usage="adoption-follow-up" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="followUpEditorVisible = false">取消</el-button>
        <el-button :loading="saving" type="primary" @click="submitFollowUp">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="followUpPlanEditorVisible" title="调整回访时间" width="520px" append-to-body>
      <el-form label-position="top">
        <el-form-item label="新的计划时间">
          <el-date-picker
            v-model="followUpPlanEditor.plannedAt"
            type="datetime"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="followUpPlanEditorVisible = false">取消</el-button>
        <el-button :loading="saving" type="primary" @click="submitFollowUpPlan">保存时间</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { useRoute } from 'vue-router'
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Archive, LockKeyhole, Save } from 'lucide-vue-next'
import StatusTag from '../components/StatusTag.vue'
import ImageUploader from '../components/ImageUploader.vue'
import { adoptionApi, animalApi, appealApi, communityApi, notificationApi, reportApi, rescueApi, userApi } from '../api'
import { useAiAssistantPageContext } from '../composables/useAiAssistantPageContext'
import { notifyError } from '../api/http'
import { useAuth } from '../stores/auth'
import {
  agreementStatusOptions,
  animalStatusOptions,
  animalTypeOptions,
  appealStatusOptions,
  appealTargetOptions,
  applyStatusOptions,
  communityCommentStatusOptions,
  communityPostStatusOptions,
  followUpStatusOptions,
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
const managedApplications = ref([])
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
const agreementDialogVisible = ref(false)
const followUpDialogVisible = ref(false)
const followUpEditorVisible = ref(false)
const followUpPlanEditorVisible = ref(false)
const statusTargetType = ref('animal')
const statusTarget = ref(null)
const statusForm = reactive({ newStatus: '' })
const animalEditor = reactive({ id: null, type: 'CAT', gender: 'UNKNOWN', age: 0, foundRegion: '', healthCondition: '', imageUrls: [], description: '' })
const rescueEditor = reactive({ id: null, location: '', animalCondition: '', contact: '', description: '', imageUrls: [] })
const appealForm = reactive({ targetType: 'ANIMAL', targetId: null, reason: '' })
const agreementData = ref(null)
const agreementCurrentApplyId = ref(null)
const agreementSignatureForm = reactive({ signatureName: '', signatureDataUrl: '' })
const agreementEditMode = ref(false)
const agreementEditForm = reactive({ title: '', content: '' })
const signatureCanvasRef = ref()
const signatureHasStroke = ref(false)
const followUps = ref([])
const followUpManageMode = ref(false)
const followUpCurrentApplyId = ref(null)
const followUpEditor = reactive({ id: null, note: '', imageUrls: [] })
const followUpPlanEditor = reactive({ id: null, plannedAt: '' })
let signatureDrawing = false
let signatureLastPoint = { x: 0, y: 0 }

const animalRules = {
  type: [{ required: true, message: '请选择动物类型', trigger: 'change' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  foundRegion: [{ required: true, message: '请输入发现地区', trigger: 'blur' }],
  imageUrls: [{ type: 'array', required: true, min: 1, message: '至少上传一张照片', trigger: 'change' }]
}
const rescueRules = {
  location: [{ required: true, message: '请输入救助地点', trigger: 'blur' }],
  animalCondition: [{ required: true, message: '请输入动物情况', trigger: 'blur' }],
  contact: [{ required: true, message: '请输入联系方式', trigger: 'blur' }],
  description: [{ required: true, message: '请输入求助说明', trigger: 'blur' }]
}
const appealRules = {
  reason: [{ required: true, message: '请填写申诉理由', trigger: 'blur' }]
}
const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { min: 6, max: 32, message: '密码长度需为 6-32 位', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请确认密码', trigger: 'blur' }]
}

const availableStatuses = computed(() => {
  if (statusTargetType.value === 'animal') {
    return animalStatusOptions.filter((item) => ['WAITING_RESCUE', 'RESCUING', 'WAITING_ADOPTION', 'ADOPTED', 'OFFLINE'].includes(item.value))
  }
  return rescueStatusOptions.filter((item) => item.value !== 'PENDING_REVIEW' && item.value !== 'REJECTED')
})

const canManageAdoptions = computed(() => Boolean(profile.value.id))
const canSignAgreement = computed(() => {
  if (!agreementData.value || agreementData.value.status === 'COMPLETED' || !profile.value.id) {
    return false
  }
  const isAdopter = profile.value.id === agreementData.value.adopterId
  const isCounterpart = profile.value.id === agreementData.value.publisherId
  if (isAdopter) return !agreementData.value.adopterSignatureName
  if (isCounterpart) return !agreementData.value.counterpartSignatureName
  return false
})
const canEditAgreement = computed(() => {
  if (!agreementData.value || !profile.value.id) {
    return false
  }
  return profile.value.id === agreementData.value.publisherId &&
    !agreementData.value.adopterSignedAt &&
    !agreementData.value.counterpartSignedAt
})

useAiAssistantPageContext(() => ({
  pageTitle: t('profilePage.title'),
  pageSummary: t('profilePage.pageSummary'),
  viewData: {
    activeTab: tab.value,
    profile: profile.value ? {
      id: profile.value.id,
      nickname: profile.value.nickname,
      role: profile.value.role,
      roleText: profile.value.roleText,
      statusText: profile.value.statusText
    } : null,
    summary: {
      animalCount: animals.value.length,
      rescueCount: rescues.value.length,
      applicationCount: applications.value.length,
      pendingReviewApplicationCount: applications.value.filter((item) => item?.status === 'PENDING_REVIEW').length,
      approvedApplicationCount: applications.value.filter((item) => item?.status === 'APPROVED').length,
      managedApplicationCount: managedApplications.value.length,
      activeManagedApplicationCount: managedApplications.value.filter((item) => ['APPROVED', 'PENDING_ADOPTER', 'PENDING_COUNTERPART'].includes(item?.status)).length,
      communityPostCount: communityPosts.value.length,
      communityCommentCount: communityComments.value.length,
      unreadNotificationCount: notificationSummary.value.unreadCount || 0,
      reportCount: reports.value.length,
      appealCount: appeals.value.length
    },
    assistantHints: {
      userCanAuditApplications: false,
      managedApplicationsMeaning: '这里展示的是与自己发布动物相关的领养跟进、协议和回访，不是管理员审核台。',
      currentUserPerspective: 'personal-center'
    }
  }
}))

const API_BASE = window.location.origin
function getFullUrl(url) {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:')) return url
  return `${API_BASE}${url}`
}

function formatTime(value) {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString()
}

function canCompleteFollowUp(item) {
  if (!item || item.status === 'COMPLETED') {
    return false
  }
  if (!item.plannedAt) {
    return true
  }
  return new Date(item.plannedAt).getTime() <= Date.now()
}

function signatureImageUrl(data, side) {
  if (!data) {
    return ''
  }
  const keyMap = side === 'adopter'
    ? ['adopterSignatureImageUrl', 'adopterSignatureUrl', 'adopterSignatureImage', 'adopterSignature']
    : ['counterpartSignatureImageUrl', 'counterpartSignatureUrl', 'counterpartSignatureImage', 'counterpartSignature']
  const raw = keyMap.map((key) => data[key]).find((value) => typeof value === 'string' && value)
  return raw ? getFullUrl(raw) : ''
}

function initSignatureCanvas() {
  const canvas = signatureCanvasRef.value
  if (!canvas) {
    return
  }
  canvas.width = canvas.clientWidth || 640
  canvas.height = canvas.clientHeight || 220
  const context = canvas.getContext('2d')
  if (!context) {
    return
  }
  context.fillStyle = '#ffffff'
  context.fillRect(0, 0, canvas.width, canvas.height)
  context.lineCap = 'round'
  context.lineJoin = 'round'
  context.lineWidth = 2.8
  context.strokeStyle = '#1f5d4f'
  signatureHasStroke.value = false
  agreementSignatureForm.signatureDataUrl = ''
}

function clearSignature() {
  signatureDrawing = false
  initSignatureCanvas()
}

function getSignaturePoint(event) {
  const canvas = signatureCanvasRef.value
  if (!canvas) {
    return { x: 0, y: 0 }
  }
  const rect = canvas.getBoundingClientRect()
  const scaleX = canvas.width / rect.width
  const scaleY = canvas.height / rect.height
  return {
    x: (event.clientX - rect.left) * scaleX,
    y: (event.clientY - rect.top) * scaleY
  }
}

function syncSignatureData() {
  const canvas = signatureCanvasRef.value
  agreementSignatureForm.signatureDataUrl = canvas && signatureHasStroke.value ? canvas.toDataURL('image/png') : ''
}

function startSignature(event) {
  const canvas = signatureCanvasRef.value
  const context = canvas?.getContext('2d')
  if (!canvas || !context) {
    return
  }
  const point = getSignaturePoint(event)
  signatureDrawing = true
  signatureLastPoint = point
  signatureHasStroke.value = true
  context.beginPath()
  context.moveTo(point.x, point.y)
  context.lineTo(point.x + 0.01, point.y + 0.01)
  context.stroke()
  canvas.setPointerCapture?.(event.pointerId)
  syncSignatureData()
}

function moveSignature(event) {
  if (!signatureDrawing) {
    return
  }
  const canvas = signatureCanvasRef.value
  const context = canvas?.getContext('2d')
  if (!canvas || !context) {
    return
  }
  const point = getSignaturePoint(event)
  context.beginPath()
  context.moveTo(signatureLastPoint.x, signatureLastPoint.y)
  context.lineTo(point.x, point.y)
  context.stroke()
  signatureLastPoint = point
}

function finishSignature(event) {
  if (!signatureDrawing) {
    return
  }
  signatureDrawing = false
  signatureCanvasRef.value?.releasePointerCapture?.(event.pointerId)
  syncSignatureData()
}

function notificationTitle(title) {
  if (!title) {
    return t('appShell.systemNotification')
  }
  const key = 'notification.' + title
  const translated = t(key)
  return translated === key ? title : translated
}

function formatNotificationContent(item) {
  if (item.title === 'COMMENT_REPLY_COMMENT' || item.title === 'COMMENT_REPLY_POST') {
    const parts = (item.content || '').split('|')
    if (parts.length === 2) {
      const key = 'notification.' + item.title + '_CONTENT'
      const translated = t(key, { nickname: parts[0], snippet: parts[1] })
      return translated === key ? item.content || '' : translated
    }
  }
  return item.content || ''
}

async function loadRecords() {
  const results = await Promise.allSettled([
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

  const pageContent = (result) => result.status === 'fulfilled' ? (result.value.content || []) : []
  const singleValue = (result, fallback) => result.status === 'fulfilled' ? result.value : fallback
  const failures = results.filter((item) => item.status === 'rejected')

  animals.value = pageContent(results[0])
  rescues.value = pageContent(results[1])
  applications.value = pageContent(results[2])
  communityPosts.value = pageContent(results[3])
  communityComments.value = pageContent(results[4])
  notifications.value = pageContent(results[5])
  notificationSummary.value = singleValue(results[6], { unreadCount: 0 })
  reports.value = pageContent(results[7])
  appeals.value = pageContent(results[8])

  if (failures.length === results.length && failures[0]?.reason) {
    notifyError(failures[0].reason)
  }
}

async function loadManagedApplications() {
  if (!canManageAdoptions.value) {
    managedApplications.value = []
    return
  }
  try {
    managedApplications.value = (await userApi.managedApplications({ page: 0, size: 20 })).content || []
  } catch (error) {
    managedApplications.value = []
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
    ElMessage.success(t('profilePage.profileUpdated'))
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
    ElMessage.success(t('profilePage.passwordUpdated'))
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
    ElMessage.success('动物档案已更新')
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
    ElMessage.success('救助信息已更新')
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
    ElMessage.success('状态已更新')
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
    await ElMessageBox.confirm('确认下架这条记录吗？', '提示', { type: 'warning' })
    if (type === 'animal') {
      await animalApi.offline(row.id)
    } else {
      await rescueApi.offline(row.id)
    }
    ElMessage.success('已下架')
    await loadRecords()
  } catch (error) {
    if (error !== 'cancel') notifyError(error)
  }
}

async function cancelApplication(row) {
  try {
    await adoptionApi.cancel(row.id)
    ElMessage.success('申请已取消')
    await loadRecords()
  } catch (error) {
    notifyError(error)
  }
}

async function openAgreement(row) {
  try {
    agreementCurrentApplyId.value = row.id
    agreementData.value = await adoptionApi.agreement(row.id)
    agreementSignatureForm.signatureName = profile.value.nickname || row.applicantName || ''
    agreementSignatureForm.signatureDataUrl = ''
    agreementEditMode.value = false
    agreementEditForm.title = agreementData.value.title || ''
    agreementEditForm.content = agreementData.value.content || ''
    agreementDialogVisible.value = true
    await nextTick()
    initSignatureCanvas()
  } catch (error) {
    notifyError(error)
  }
}

function startEditAgreement() {
  agreementEditMode.value = true
  agreementEditForm.title = agreementData.value?.title || ''
  agreementEditForm.content = agreementData.value?.content || ''
}

function cancelEditAgreement() {
  agreementEditMode.value = false
  agreementEditForm.title = agreementData.value?.title || ''
  agreementEditForm.content = agreementData.value?.content || ''
}

async function saveAgreement() {
  if (!agreementCurrentApplyId.value) return
  if (!agreementEditForm.title.trim() || !agreementEditForm.content.trim()) {
    ElMessage.warning('请完善协议标题和正文')
    return
  }
  saving.value = true
  try {
    agreementData.value = await adoptionApi.updateAgreement(agreementCurrentApplyId.value, {
      title: agreementEditForm.title.trim(),
      content: agreementEditForm.content.trim()
    })
    agreementEditMode.value = false
    ElMessage.success('协议已更新')
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

function openAgreementPdf() {
  if (!agreementData.value?.pdfUrl) return
  window.open(getFullUrl(agreementData.value.pdfUrl), '_blank')
}

async function signAgreement() {
  if (!agreementCurrentApplyId.value) return
  if (!agreementSignatureForm.signatureName.trim()) {
    ElMessage.warning('请填写签署姓名')
    return
  }
  if (!signatureHasStroke.value) {
    ElMessage.warning('请先完成手写签名')
    return
  }
  syncSignatureData()
  saving.value = true
  try {
    agreementData.value = await adoptionApi.signAgreement(agreementCurrentApplyId.value, {
      signatureName: agreementSignatureForm.signatureName.trim(),
      signatureDataUrl: agreementSignatureForm.signatureDataUrl,
      signatureImageData: agreementSignatureForm.signatureDataUrl,
      signatureImage: agreementSignatureForm.signatureDataUrl,
      signatureImageUrl: agreementSignatureForm.signatureDataUrl,
      signatureMimeType: 'image/png'
    })
    ElMessage.success('协议已签署')
    await loadRecords()
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

async function openFollowUps(row) {
  try {
    followUpCurrentApplyId.value = row.id
    followUpManageMode.value = profile.value.id === row.publisherId
    followUps.value = await adoptionApi.followUps(row.id)
    followUpDialogVisible.value = true
  } catch (error) {
    notifyError(error)
  }
}

function openFollowUpComplete(item) {
  if (!canCompleteFollowUp(item)) {
    ElMessage.warning('尚未到回访时间，暂不可填写')
    return
  }
  followUpEditor.id = item.id
  followUpEditor.note = item.note || ''
  followUpEditor.imageUrls = item.imageUrls ? [...item.imageUrls] : []
  followUpEditorVisible.value = true
}

function openFollowUpPlanEditor(item) {
  followUpPlanEditor.id = item.id
  followUpPlanEditor.plannedAt = item.plannedAt ? item.plannedAt.slice(0, 19) : ''
  followUpPlanEditorVisible.value = true
}

async function submitFollowUp() {
  if (!followUpEditor.note.trim()) {
    ElMessage.warning('请填写回访内容')
    return
  }
  saving.value = true
  try {
    await adoptionApi.completeFollowUp(followUpEditor.id, {
      note: followUpEditor.note,
      imageUrls: followUpEditor.imageUrls
    })
    ElMessage.success('回访记录已保存')
    followUpEditorVisible.value = false
    if (followUpCurrentApplyId.value) {
      followUps.value = await adoptionApi.followUps(followUpCurrentApplyId.value)
    }
    await loadRecords()
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

async function submitFollowUpPlan() {
  if (!followUpPlanEditor.id || !followUpPlanEditor.plannedAt) {
    ElMessage.warning('请选择新的计划时间')
    return
  }
  saving.value = true
  try {
    await adoptionApi.updateFollowUpPlan(followUpPlanEditor.id, {
      plannedAt: followUpPlanEditor.plannedAt
    })
    ElMessage.success('回访时间已更新')
    followUpPlanEditorVisible.value = false
    if (followUpCurrentApplyId.value) {
      followUps.value = await adoptionApi.followUps(followUpCurrentApplyId.value)
    }
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
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
    ElMessage.success('申诉已提交')
    appealVisible.value = false
    await loadRecords()
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

onMounted(load)

watch(tab, async (value) => {
  if (value === 'managedApplications') {
    await loadManagedApplications()
  }
})

watch(agreementDialogVisible, async (visible) => {
  if (!visible) {
    clearSignature()
    return
  }
  if (canSignAgreement.value) {
    await nextTick()
    initSignatureCanvas()
  }
})
</script>

<style scoped>
.agreement-shell {
  display: grid;
  gap: 14px;
}

.agreement-meta,
.follow-up-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.agreement-actions {
  display: flex;
  justify-content: flex-end;
}

.agreement-content {
  padding: 14px;
  border-radius: 12px;
  background: rgba(244, 248, 246, 0.95);
  white-space: pre-wrap;
  line-height: 1.7;
  color: #30413b;
}

.agreement-edit-box {
  padding: 14px;
  border-radius: 12px;
  background: rgba(244, 248, 246, 0.95);
}

.agreement-sign-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.agreement-sign-box {
  padding-top: 8px;
  border-top: 1px solid rgba(74, 109, 96, 0.12);
}

.signature-preview {
  width: 100%;
  max-width: 220px;
  height: 88px;
  object-fit: contain;
  padding: 8px 10px;
  border-radius: 10px;
  border: 1px solid rgba(74, 109, 96, 0.14);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(246, 249, 247, 0.96));
}

.signature-pad-shell {
  display: grid;
  gap: 10px;
}

.signature-canvas {
  width: 100%;
  height: 220px;
  display: block;
  border-radius: 14px;
  border: 1px dashed rgba(74, 109, 96, 0.26);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 248, 0.98)),
    repeating-linear-gradient(
      0deg,
      transparent 0,
      transparent 35px,
      rgba(74, 109, 96, 0.06) 35px,
      rgba(74, 109, 96, 0.06) 36px
    );
  touch-action: none;
  cursor: crosshair;
}

.signature-pad-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #60756b;
  font-size: 13px;
}

.follow-up-list {
  display: grid;
  gap: 12px;
}

.follow-up-card {
  padding: 14px;
  border-radius: 12px;
  background: rgba(249, 251, 250, 0.96);
  border: 1px solid rgba(74, 109, 96, 0.1);
}

.follow-up-note {
  margin: 10px 0;
  line-height: 1.7;
  white-space: pre-wrap;
}

.follow-up-waiting-tip {
  margin: 8px 0 0;
  color: #b26a00;
  font-size: 13px;
}

.follow-up-actions {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 720px) {
  .agreement-sign-grid {
    grid-template-columns: 1fr;
  }
}
</style>
