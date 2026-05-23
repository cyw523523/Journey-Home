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
          <el-menu-item index="reports"><ShieldAlert />{{ t('admin.reportHandle') }}</el-menu-item>
          <el-menu-item index="appeals"><FileCheck2 />{{ t('admin.appealReview') }}</el-menu-item>
          <el-menu-item index="stations"><Building2 />{{ t('admin.stationCertify') }}</el-menu-item>
          <el-menu-item index="volunteers"><Handshake />{{ t('admin.volunteerTask') }}</el-menu-item>
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
              <h3>{{ t('admin.animalStatus') }}</h3>
              <div v-for="item in animalStatus" :key="item.status" class="mini-row">
                <span>{{ item.statusText }}<strong>{{ item.count }}</strong></span>
              </div>
            </el-col>
            <el-col :md="8" :sm="24">
              <h3>{{ t('admin.rescueStatus') }}</h3>
              <div v-for="item in rescueStatus" :key="item.status" class="mini-row">
                <span>{{ item.statusText }}<strong>{{ item.count }}</strong></span>
              </div>
            </el-col>
            <el-col :md="8" :sm="24">
              <h3>{{ t('admin.applyStatus') }}</h3>
              <div v-for="item in applyStatus" :key="item.status" class="mini-row">
                <span>{{ item.statusText }}<strong>{{ item.count }}</strong></span>
              </div>
            </el-col>
          </el-row>
        </div>

        <div v-show="active === 'audits'" class="surface form-shell">
          <div style="display:flex;gap:10px;margin-bottom:12px">
            <el-select v-model="auditType" clearable style="width: 220px" @change="loadPending">
              <el-option :label="t('admin.animalRecord')" value="ANIMAL" />
              <el-option :label="t('admin.rescueInfo')" value="RESCUE" />
              <el-option :label="t('admin.adoptApply')" value="ADOPT_APPLY" />
              <el-option :label="t('admin.communityPost')" value="COMMUNITY_POST" />
              <el-option :label="t('admin.communityComment')" value="COMMUNITY_COMMENT" />
              <el-option :label="t('admin.volunteerTaskLabel')" value="VOLUNTEER_TASK" />
            </el-select>
            <el-button :icon="RefreshCw" @click="loadPending">刷新</el-button>
          </div>
          <el-table :data="pending" stripe>
            <el-table-column :label="t('admin.targetType')" width="160">
              <template #default="{ row }">{{ auditTypeLabel(row.targetType) }}</template>
            </el-table-column>
            <el-table-column prop="targetId" :label="t('admin.businessId')" width="100" />
            <el-table-column prop="title" :label="t('admin.content')" />
            <el-table-column prop="publisherOrApplicant" :label="t('admin.publisherApplicant')" width="180" />
            <el-table-column :label="t('admin.actions')" width="320">
              <template #default="{ row }">
                <el-button size="small" :icon="Eye" text @click="openDetail(row)">{{ t('admin.detail') }}</el-button>
                <el-button size="small" :icon="Check" type="primary" @click="openAudit(row, 'APPROVE')">{{ t('admin.approve') }}</el-button>
                <el-button size="small" :icon="X" @click="openAudit(row, 'REJECT')">{{ t('admin.reject') }}</el-button>
                <el-button size="small" v-if="row.targetType !== 'ADOPT_APPLY'" @click="openAudit(row, 'OFFLINE')">{{ t('admin.offline') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-show="active === 'users'" class="surface form-shell">
          <el-table :data="usersList" stripe>
            <el-table-column prop="id" :label="t('admin.id')" width="80" />
            <el-table-column prop="account" :label="t('admin.account')" />
            <el-table-column prop="nickname" :label="t('admin.nickname')" />
            <el-table-column prop="phone" :label="t('admin.phoneNumber')" />
            <el-table-column :label="t('admin.role')" width="150">
              <template #default="{ row }">
                <el-select v-model="row.role" size="small" @change="updateUser(row)">
                  <el-option v-for="item in roleOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column :label="t('admin.status')" width="140">
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
            <el-button :icon="Plus" type="primary" @click="openNotice()">{{ t('admin.createAnnouncement') }}</el-button>
          </div>
          <el-table :data="notices" stripe>
            <el-table-column prop="id" :label="t('admin.id')" width="80" />
            <el-table-column prop="title" :label="t('admin.noticeTitle')" />
            <el-table-column :label="t('admin.status')" width="120">
              <template #default="{ row }">
                <StatusTag :value="row.status" :text="row.statusText" :options="noticeStatusOptions" />
              </template>
            </el-table-column>
            <el-table-column :label="t('admin.actions')" width="220">
              <template #default="{ row }">
                <el-button size="small" :icon="Pencil" @click="openNotice(row)">{{ t('common.edit') }}</el-button>
                <el-button size="small" :icon="Archive" @click="offlineNotice(row)">{{ t('admin.offline') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-show="active === 'applications'" class="surface form-shell">
          <el-table :data="applications" stripe>
            <el-table-column prop="id" :label="t('admin.id')" width="80" />
            <el-table-column prop="applicantName" :label="t('admin.applicant')" width="130" />
            <el-table-column prop="animalTypeText" :label="t('admin.animal')" width="100" />
            <el-table-column prop="reason" :label="t('admin.reason')" />
            <el-table-column :label="t('admin.status')" width="120">
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
            <el-table-column prop="id" :label="t('admin.id')" width="80" />
            <el-table-column :label="t('admin.target')" width="120">
              <template #default="{ row }">{{ reportTargetLabel(row.targetTypeText) }}</template>
            </el-table-column>
            <el-table-column prop="reasonTypeText" :label="t('admin.reason')" width="120" />
            <el-table-column prop="description" :label="t('admin.content')" />
            <el-table-column :label="t('admin.status')" width="120">
              <template #default="{ row }">
                <StatusTag :value="row.status" :text="row.statusText" :options="reportStatusOptions" />
              </template>
            </el-table-column>
            <el-table-column :label="t('admin.actions')" width="220">
              <template #default="{ row }">
                <el-button size="small" :icon="Eye" text @click="openReportResolve(row)">{{ t('admin.handle') }}</el-button>
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
            <el-table-column prop="id" :label="t('admin.id')" width="80" />
            <el-table-column :label="t('admin.target')" width="120">
              <template #default="{ row }">{{ appealTargetLabel(row.targetTypeText) }}</template>
            </el-table-column>
            <el-table-column prop="applicantNickname" :label="t('admin.appealPerson')" width="120" />
            <el-table-column prop="reason" :label="t('admin.appealReason')" />
            <el-table-column :label="t('admin.status')" width="150">
              <template #default="{ row }">
                <StatusTag :value="row.status" :text="row.statusText" :options="appealStatusOptions" />
              </template>
            </el-table-column>
            <el-table-column :label="t('admin.actions')" width="220">
              <template #default="{ row }">
                <el-button size="small" :icon="Eye" text @click="openAppealReview(row)">{{ t('admin.review') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-show="active === 'stations'" class="surface form-shell">
          <div style="display:flex;gap:10px;margin-bottom:12px">
            <el-select v-model="stationStatusFilter" clearable style="width: 180px" @change="loadStations">
              <el-option :label="t('admin.certPending')" value="PENDING" />
              <el-option :label="t('admin.certApproved')" value="APPROVED" />
              <el-option :label="t('admin.certRejected')" value="REJECTED" />
            </el-select>
            <el-button :icon="RefreshCw" @click="loadStations">刷新</el-button>
          </div>
          <el-table :data="stations" stripe>
            <el-table-column prop="id" :label="t('admin.id')" width="70" />
            <el-table-column prop="stationName" :label="t('admin.stationNameLabel')" width="160" />
            <el-table-column prop="nickname" :label="t('admin.applicant')" width="120" />
            <el-table-column prop="address" :label="t('admin.address')" min-width="140" show-overflow-tooltip />
            <el-table-column prop="contactPhone" :label="t('admin.contactPhone')" width="120" />
            <el-table-column :label="t('admin.certStatus')" width="120">
              <template #default="{ row }">
                <StatusTag :value="row.certificationStatus" :text="row.certificationStatusText"
                           :options="certStatusOptions" size="small" />
              </template>
            </el-table-column>
            <el-table-column :label="t('admin.followers')" width="70">
              <template #default="{ row }">{{ row.followerCount }}</template>
            </el-table-column>
            <el-table-column :label="t('admin.applyTime')" width="160">
              <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column :label="t('admin.actions')" width="200">
              <template #default="{ row }">
                <el-button v-if="row.certificationStatus === 'PENDING'" size="small" :icon="Check" type="primary"
                           text @click="openCertify(row, 'APPROVED')">{{ t('admin.approve') }}</el-button>
                <el-button v-if="row.certificationStatus === 'PENDING'" size="small" :icon="X" type="danger"
                           text @click="openCertify(row, 'REJECTED')">{{ t('admin.reject') }}</el-button>
                <el-button v-if="row.certificationStatus !== 'PENDING'" size="small" :icon="Eye" text
                           @click="openStationDetail(row)">{{ t('admin.detail') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-show="active === 'volunteers'" class="surface form-shell">
          <div style="display:flex;gap:10px;margin-bottom:12px">
            <el-select v-model="volunteerStatusFilter" clearable style="width: 180px" @change="loadVolunteerTasks">
              <el-option :label="t('admin.volPendingReview')" value="PENDING_REVIEW" />
              <el-option :label="t('admin.volRecruiting')" value="RECRUITING" />
              <el-option :label="t('admin.volInProgress')" value="IN_PROGRESS" />
              <el-option :label="t('admin.volCompleted')" value="COMPLETED" />
              <el-option :label="t('admin.volRejected')" value="REJECTED" />
              <el-option :label="t('admin.volCancelled')" value="CANCELLED" />
            </el-select>
            <el-button :icon="RefreshCw" @click="loadVolunteerTasks">刷新</el-button>
          </div>
          <el-table :data="volunteerTasks" stripe>
            <el-table-column prop="id" :label="t('admin.id')" width="70" />
            <el-table-column prop="title" :label="t('admin.taskTitle')" min-width="160" show-overflow-tooltip />
            <el-table-column prop="location" :label="t('admin.location')" width="140" show-overflow-tooltip />
            <el-table-column prop="publisherNickname" :label="t('admin.publisher')" width="110" />
            <el-table-column :label="t('admin.peopleCount')" width="90">
              <template #default="{ row }">{{ row.currentVolunteers || 0 }}/{{ row.maxVolunteers }}</template>
            </el-table-column>
            <el-table-column :label="t('admin.status')" width="120">
              <template #default="{ row }">
                <StatusTag :value="row.status" :text="row.statusText" :options="volunteerTaskStatusOptions" size="small" />
              </template>
            </el-table-column>
            <el-table-column :label="t('admin.applyTime')" width="160">
              <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column :label="t('admin.actions')" width="240">
              <template #default="{ row }">
                <el-button size="small" :icon="Eye" text @click="openVolunteerDetail(row)">{{ t('admin.detail') }}</el-button>
                <el-button v-if="row.status === 'PENDING_REVIEW'" size="small" :icon="Check" type="primary" text @click="openAudit({ targetType: 'VOLUNTEER_TASK', targetId: row.id }, 'APPROVE')">{{ t('admin.approve') }}</el-button>
                <el-button v-if="row.status === 'PENDING_REVIEW'" size="small" :icon="X" type="danger" text @click="openAudit({ targetType: 'VOLUNTEER_TASK', targetId: row.id }, 'REJECT')">{{ t('admin.reject') }}</el-button>
                <el-button v-if="row.status !== 'PENDING_REVIEW' && row.status !== 'REJECTED' && row.status !== 'CANCELLED'" size="small" :icon="X" text @click="openAudit({ targetType: 'VOLUNTEER_TASK', targetId: row.id }, 'OFFLINE')">{{ t('admin.offline') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </main>
    </div>

    <el-dialog v-model="auditDialog" :title="t('admin.auditDialogTitle')" width="520px" append-to-body>
      <el-form :model="auditForm" label-position="top">
        <el-form-item :label="t('admin.auditResult')">
          <el-select v-model="auditForm.action" style="width: 100%">
            <el-option :label="t('admin.approve')" value="APPROVE" />
            <el-option :label="t('admin.reject')" value="REJECT" />
            <el-option v-if="auditForm.targetType !== 'ADOPT_APPLY'" :label="t('admin.offline')" value="OFFLINE" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('admin.auditOpinion')">
          <el-input v-model="auditForm.opinion" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditDialog = false">{{ t('common.cancel') }}</el-button>
        <el-button :loading="saving" :icon="Send" type="primary" @click="submitAudit">{{ t('common.submit') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialog" :title="`详情 - ${detailTargetTypeText}`" width="760px" append-to-body>
      <el-skeleton v-if="detailLoading" :rows="8" animated />
      <template v-else-if="detailData">
        <div v-if="detailTargetType === 'ANIMAL'" class="audit-detail-grid">
          <div class="detail-item"><label>{{ t('admin.typeLabel') }}</label><span>{{ detailData.typeText }}</span></div>
          <div class="detail-item"><label>{{ t('admin.genderLabel') }}</label><span>{{ detailData.genderText }}</span></div>
          <div class="detail-item"><label>{{ t('admin.ageLabel') }}</label><span>{{ detailData.age ?? t('admin.unknown') }}</span></div>
          <div class="detail-item"><label>{{ t('animals.foundRegion') }}</label><span>{{ detailData.foundRegion }}</span></div>
          <div class="detail-item"><label>{{ t('animals.healthCondition') }}</label><span>{{ detailData.healthCondition || '-' }}</span></div>
          <div class="detail-item"><label>{{ t('admin.status') }}</label><StatusTag :value="detailData.status" :text="detailData.statusText" :options="animalStatusOptions" /></div>
          <div class="detail-item" v-if="parseExpectedStatus(detailData)"><label>{{ t('admin.expectedAction') }}</label><StatusTag :value="parseExpectedStatus(detailData)" :text="expectedStatusText(parseExpectedStatus(detailData))" :options="animalStatusOptions" /></div>
          <div class="detail-item"><label>{{ t('admin.publisher') }}</label><span>{{ detailData.publisherNickname }}</span></div>
          <div class="detail-item full-width"><label>{{ t('animals.description') }}</label><p>{{ detailData.description || '-' }}</p></div>
          <div class="detail-item full-width" v-if="detailData.coverImageUrl"><label>封面图</label><img :src="detailData.coverImageUrl" style="max-width:200px;border-radius:8px" /></div>
          <div class="detail-item full-width" v-if="detailData.imageUrls?.length">
            <label>图片</label><div class="detail-thumb-row"><img v-for="u in detailData.imageUrls" :key="u" :src="u" style="width:80px;height:80px;object-fit:cover;border-radius:6px;margin-right:6px" /></div>
          </div>
        </div>
        <div v-else-if="detailTargetType === 'RESCUE'" class="audit-detail-grid">
          <div class="detail-item"><label>{{ t('rescues.location') }}</label><span>{{ detailData.location }}</span></div>
          <div class="detail-item"><label>{{ t('rescues.animalCondition') }}</label><span>{{ detailData.animalCondition }}</span></div>
          <div class="detail-item"><label>{{ t('rescues.contact') }}</label><span>{{ detailData.contact }}</span></div>
          <div class="detail-item"><label>{{ t('admin.status') }}</label><StatusTag :value="detailData.status" :text="detailData.statusText" :options="rescueStatusOptions" /></div>
          <div class="detail-item"><label>{{ t('admin.publisher') }}</label><span>{{ detailData.publisherNickname }}</span></div>
          <div class="detail-item full-width"><label>{{ t('rescues.description') }}</label><p>{{ detailData.description }}</p></div>
          <div class="detail-item full-width" v-if="detailData.imageUrls?.length">
            <label>图片</label><div class="detail-thumb-row"><img v-for="u in detailData.imageUrls" :key="u" :src="u" style="width:80px;height:80px;object-fit:cover;border-radius:6px;margin-right:6px" /></div>
          </div>
        </div>
        <div v-else-if="detailTargetType === 'ADOPT_APPLY'" class="audit-detail-grid">
          <div class="detail-item"><label>{{ t('admin.animalId') }}</label><span>{{ detailData.animalId }}</span></div>
          <div class="detail-item"><label>{{ t('admin.animalType') }}</label><span>{{ detailData.animalTypeText }}</span></div>
          <div class="detail-item"><label>{{ t('admin.applicant') }}</label><span>{{ detailData.applicantName }}</span></div>
          <div class="detail-item"><label>{{ t('rescues.contact') }}</label><span>{{ detailData.contact }}</span></div>
          <div class="detail-item"><label>{{ t('admin.status') }}</label><StatusTag :value="detailData.status" :text="detailData.statusText" :options="applyStatusOptions" /></div>
          <div class="detail-item full-width"><label>{{ t('admin.adoptionReason') }}</label><p>{{ detailData.reason }}</p></div>
          <div class="detail-item full-width"><label>{{ t('admin.livingCondition') }}</label><p>{{ detailData.livingCondition }}</p></div>
          <div class="detail-item full-width"><label>{{ t('admin.experience') }}</label><p>{{ detailData.experience }}</p></div>
          <div class="detail-item full-width" v-if="detailData.auditOpinion"><label>审核意见</label><p>{{ detailData.auditOpinion }}</p></div>
        </div>
        <div v-else-if="detailTargetType === 'COMMUNITY_POST'" class="audit-detail-grid">
          <div class="detail-item full-width"><label>{{ t('admin.noticeTitle') }}</label><p>{{ detailData.title }}</p></div>
          <div class="detail-item"><label>{{ t('community.author') }}</label><span>{{ detailData.authorNickname }}</span></div>
          <div class="detail-item"><label>{{ t('admin.status') }}</label><StatusTag :value="detailData.status" :text="detailData.statusText" :options="communityPostStatusOptions" /></div>
          <div class="detail-item full-width"><label>内容</label><p>{{ detailData.content }}</p></div>
          <div class="detail-item full-width" v-if="detailData.imageUrls?.length">
            <label>图片</label><div class="detail-thumb-row"><img v-for="u in detailData.imageUrls" :key="u" :src="u" style="width:80px;height:80px;object-fit:cover;border-radius:6px;margin-right:6px" /></div>
          </div>
        </div>
        <div v-else-if="detailTargetType === 'COMMUNITY_COMMENT'" class="audit-detail-grid">
          <div class="detail-item"><label>{{ t('community.author') }}</label><span>{{ detailData.authorNickname }}</span></div>
          <div class="detail-item"><label>{{ t('admin.postId') }}</label><span>{{ detailData.postId }}</span></div>
          <div class="detail-item"><label>{{ t('admin.status') }}</label><StatusTag :value="detailData.status" :text="detailData.statusText" :options="communityCommentStatusOptions" /></div>
          <div class="detail-item full-width"><label>内容</label><p>{{ detailData.content }}</p></div>
          <div class="detail-item full-width" v-if="detailData.imageUrls?.length">
            <label>图片</label><div class="detail-thumb-row"><img v-for="u in detailData.imageUrls" :key="u" :src="u" style="width:80px;height:80px;object-fit:cover;border-radius:6px;margin-right:6px" /></div>
          </div>
        </div>
        <div v-else-if="detailTargetType === 'VOLUNTEER_TASK'" class="audit-detail-grid">
          <div class="detail-item"><label>{{ t('admin.noticeTitle') }}</label><span>{{ detailData.title }}</span></div>
          <div class="detail-item"><label>{{ t('admin.location') }}</label><span>{{ detailData.location }}</span></div>
          <div class="detail-item"><label>{{ t('admin.status') }}</label><StatusTag :value="detailData.status" :text="detailData.statusText" :options="volunteerTaskStatusOptions" /></div>
          <div class="detail-item"><label>{{ t('admin.peopleCount') }}</label><span>{{ detailData.currentVolunteers || 0 }} / {{ detailData.maxVolunteers }}</span></div>
          <div class="detail-item"><label>{{ t('admin.publisher') }}</label><span>{{ detailData.publisherNickname }}</span></div>
          <div class="detail-item" v-if="detailData.scheduledTime"><label>{{ t('admin.scheduledTime') }}</label><span>{{ formatTime(detailData.scheduledTime) }}</span></div>
          <div class="detail-item full-width"><label>{{ t('animals.description') }}</label><p>{{ detailData.description || '-' }}</p></div>
          <div class="detail-item full-width" v-if="detailData.reviewComment"><label>{{ t('admin.auditOpinion') }}</label><p>{{ detailData.reviewComment }}</p></div>
          <div class="detail-item full-width" v-if="detailData.imageUrl"><label>{{ t('admin.coverImage') }}</label><img :src="detailData.imageUrl" style="max-width:200px;border-radius:8px" /></div>
        </div>
      </template>
      <template #footer>
        <el-button @click="detailDialog = false">{{ t('admin.close') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="noticeDialog" :title="t('admin.noticeDialogTitle')" width="680px" append-to-body>
      <el-form :model="noticeForm" label-position="top">
        <el-form-item :label="t('admin.noticeTitle')">
          <el-input v-model="noticeForm.title" />
        </el-form-item>
        <el-form-item :label="t('admin.status')">
          <el-select v-model="noticeForm.status" style="width: 100%">
            <el-option v-for="item in noticeStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('admin.noticeContent')">
          <el-input v-model="noticeForm.content" type="textarea" :rows="6" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="noticeDialog = false">{{ t('common.cancel') }}</el-button>
        <el-button :loading="saving" :icon="Save" type="primary" @click="saveNotice">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reportDialog" :title="t('admin.handleReportTitle')" width="560px" append-to-body>
      <div v-if="reportTarget" style="margin-bottom: 12px">
        <el-tag>{{ reportTarget.targetTypeText }}</el-tag>
        <p style="margin: 12px 0 6px">{{ reportTarget.description }}</p>
        <p class="muted">{{ t('admin.reportReason') }}：{{ reportTarget.reasonTypeText }}</p>
        <div v-if="reportTarget.targetContent" style="margin-top: 12px; padding: 12px; background: rgba(244,248,246,0.9); border-radius: 8px; white-space: pre-wrap; word-break: break-word; font-size: 13px; color: #30413b;">
          <strong>{{ t('admin.reportedContent') }}</strong><br>{{ reportTarget.targetContent }}
        </div>
      </div>
      <el-form :model="reportForm" label-position="top">
        <el-form-item :label="t('admin.handleAction')">
          <el-select v-model="reportForm.action" style="width: 100%">
            <el-option v-for="item in reportActionOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('admin.handleOpinion')">
          <el-input v-model="reportForm.opinion" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportDialog = false">{{ t('common.cancel') }}</el-button>
        <el-button :loading="saving" type="primary" @click="submitReportResolve">{{ t('common.submit') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="appealDialog" :title="t('admin.handleAppealTitle')" width="560px" append-to-body>
      <div v-if="appealTarget" style="margin-bottom: 12px">
        <el-tag>{{ appealTarget.targetTypeText }}</el-tag>
        <p style="margin: 12px 0 6px">{{ appealTarget.reason }}</p>
        <p class="muted">{{ t('admin.appealPerson') }}：{{ appealTarget.applicantNickname }}</p>
      </div>
      <el-form :model="appealForm" label-position="top">
        <el-form-item :label="t('admin.reviewAction')">
          <el-select v-model="appealForm.action" style="width: 100%">
            <el-option v-for="item in appealActionOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('admin.reviewOpinion')">
          <el-input v-model="appealForm.opinion" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="appealDialog = false">{{ t('common.cancel') }}</el-button>
        <el-button :loading="saving" type="primary" @click="submitAppealReview">{{ t('common.submit') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="certDialog" :title="t('admin.certDialogTitle')" width="560px" append-to-body>
      <div v-if="certTarget" style="margin-bottom: 12px">
        <p><strong>{{ certTarget.stationName }}</strong></p>
        <p class="muted">{{ t('admin.applicant') }}：{{ certTarget.nickname }}</p>
        <p class="muted">{{ t('admin.address') }}：{{ certTarget.address }}</p>
        <p class="muted">{{ t('admin.contactPhone') }}：{{ certTarget.contactPhone }}</p>
      </div>
      <el-form :model="certForm" label-position="top">
        <el-form-item :label="t('admin.certResult')">
          <el-select v-model="certForm.action" style="width: 100%">
            <el-option :label="t('admin.approve')" value="APPROVED" />
            <el-option :label="t('admin.reject')" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('admin.handleOpinion')">
          <el-input v-model="certForm.opinion" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="certDialog = false">{{ t('common.cancel') }}</el-button>
        <el-button :loading="saving" type="primary" @click="submitCertify">{{ t('common.submit') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="stationDetailDialog" :title="t('admin.stationDetailTitle')" width="680px" append-to-body>
      <div v-if="stationDetail" class="audit-detail-grid">
        <div class="detail-item"><label>{{ t('admin.stationNameLabel') }}</label><span>{{ stationDetail.stationName }}</span></div>
        <div class="detail-item"><label>{{ t('admin.applicant') }}</label><span>{{ stationDetail.nickname }}</span></div>
        <div class="detail-item"><label>{{ t('admin.contactPhone') }}</label><span>{{ stationDetail.contactPhone }}</span></div>
        <div class="detail-item"><label>{{ t('admin.address') }}</label><span>{{ stationDetail.address }}</span></div>
        <div class="detail-item"><label>{{ t('admin.certStatus') }}</label><StatusTag :value="stationDetail.certificationStatus" :text="stationDetail.certificationStatusText" :options="certStatusOptions" /></div>
        <div class="detail-item"><label>{{ t('admin.followers') }}</label><span>{{ stationDetail.followerCount }}</span></div>
        <div class="detail-item"><label>{{ t('admin.applyTime') }}</label><span>{{ formatTime(stationDetail.createdAt) }}</span></div>
        <div class="detail-item full-width"><label>{{ t('station.intro') }}</label><p>{{ stationDetail.description || '-' }}</p></div>
        <div v-if="stationDetail.rejectReason" class="detail-item full-width"><label>{{ t('admin.rejectReasonLabel') }}</label><p>{{ stationDetail.rejectReason }}</p></div>
        <div v-if="stationDetail.imageUrl" class="detail-item full-width"><label>封面图片</label><img :src="stationDetail.imageUrl" style="max-width:200px;border-radius:8px" /></div>
      </div>
      <template #footer>
        <el-button @click="stationDetailDialog = false">{{ t('admin.close') }}</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import {
  Archive,
  Building2,
  ChartNoAxesCombined,
  Check,
  ClipboardCheck,
  Eye,
  FileCheck2,
  Handshake,
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
import { volunteerTaskApi } from '../api'
import { rescueStationApi } from '../api'
import { notifyError } from '../api/http'
import {
  animalStatusOptions,
  appealActionOptions,
  appealStatusOptions,
  appealTargetOptions,
  applyStatusOptions,
  communityCommentStatusOptions,
  communityPostStatusOptions,
  noticeStatusOptions,
  reportActionOptions,
  reportStatusOptions,
  reportTargetOptions,
  rescueStatusOptions,
  roleOptions,
  userStatusOptions,
  certificationOptions as certStatusOptions,
  volunteerTaskStatusOptions
} from '../utils/status'

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

// Rescue station management
const stations = ref([])
const stationDetailDialog = ref(false)
const stationDetail = ref(null)
const stationStatusFilter = ref('')
const volunteerTasks = ref([])
const volunteerStatusFilter = ref('')
const certDialog = ref(false)
const certTarget = ref(null)
const certForm = reactive({ action: 'APPROVED', opinion: '' })

const detailTypeLabels = { ANIMAL: t('admin.animalRecord'), RESCUE: t('admin.rescueInfo'), ADOPT_APPLY: t('admin.adoptApply'), COMMUNITY_POST: t('admin.communityPost'), COMMUNITY_COMMENT: t('admin.communityComment'), VOLUNTEER_TASK: t('admin.volunteerTaskLabel') }
const detailTargetTypeText = ref('')

function parseExpectedStatus(data) {
  if (!data || !data.reviewComment) return null
  const comment = data.reviewComment
  if (!comment.startsWith('STATUS_UPDATE|')) return null
  const parts = comment.split('|')
  if (parts.length === 3) return parts[2]
  return null
}
function expectedStatusText(status) {
  const opt = animalStatusOptions.find(o => o.value === status)
  return opt ? opt.label : status
}

const auditForm = reactive({ targetType: '', targetId: null, action: 'APPROVE', opinion: '' })
const noticeForm = reactive({ id: null, title: '', content: '', status: 'DRAFT' })
const reportForm = reactive({ action: 'DISMISS', opinion: '' })
const appealForm = reactive({ action: 'ESCALATE', opinion: '' })

async function loadAll() {
  await Promise.all([loadDashboard(), loadPending(), loadUsers(), loadNotices(), loadApplications(), loadReports(), loadAppeals(), loadStations(), loadVolunteerTasks()])
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
  detailTargetTypeText.value = detailTypeLabels[row.targetType] || row.targetType
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
    opinion: action === 'APPROVE' ? t('admin.defaultApproveOpinion') : action === 'OFFLINE' ? t('admin.defaultOfflineOpinion') : t('admin.defaultRejectOpinion')
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
    ElMessage.success(t('admin.noticeOffline'))
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
    ElMessage.success(t('admin.reportProcessed'))
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
    ElMessage.success(t('admin.appealProcessed'))
    appealDialog.value = false
    await loadAppeals()
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

// ========== Rescue station management ==========

async function loadStations() {
  try {
    const data = await rescueStationApi.adminList({ status: stationStatusFilter.value || undefined, page: 0, size: 30 })
    stations.value = data.content || []
  } catch (error) {
    notifyError(error)
  }
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
    ElMessage.success(certForm.action === 'APPROVED' ? t('admin.certApprovedMsg') : t('admin.certRejectedMsg'))
    certDialog.value = false
    await loadStations()
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

function openStationDetail(row) {
  stationDetail.value = row
  stationDetailDialog.value = true
}

async function loadVolunteerTasks() {
  try {
    const data = await volunteerTaskApi.adminList({ status: volunteerStatusFilter.value || undefined, page: 0, size: 30 })
    volunteerTasks.value = data.content || []
  } catch (error) {
    notifyError(error)
  }
}

function openVolunteerDetail(row) {
  detailTargetType.value = 'VOLUNTEER_TASK'
  detailTargetTypeText.value = t('admin.volunteerTaskLabel')
  detailData.value = row
  detailDialog.value = true
}

function formatTime(value) {
  return value ? new Date(value).toLocaleString('zh-CN') : '-'
}

const auditTypeMap = { ANIMAL: t('admin.animalRecord'), RESCUE: t('admin.rescueInfo'), ADOPT_APPLY: t('admin.adoptApply'), COMMUNITY_POST: t('admin.communityPost'), COMMUNITY_COMMENT: t('admin.communityComment'), VOLUNTEER_TASK: t('admin.volunteerTaskLabel') }
function auditTypeLabel(type) {
  return auditTypeMap[type] || type
}

const reportTargetLabelMap = { 'Animal profile': t('admin.animalRecord'), 'Rescue post': t('admin.rescueInfo'), 'Community post': t('admin.communityPost'), 'Community comment': t('admin.communityComment'), 'User': t('admin.userLabel') }
function reportTargetLabel(text) {
  return reportTargetLabelMap[text] || text
}

const appealTargetLabelMap = { 'Animal profile': t('admin.animalRecord'), 'Rescue post': t('admin.rescueInfo'), 'Adoption application': t('admin.adoptApply'), 'Community post': t('admin.communityPost'), 'Community comment': t('admin.communityComment') }
function appealTargetLabel(text) {
  return appealTargetLabelMap[text] || text
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
