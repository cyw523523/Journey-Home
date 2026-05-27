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
          <el-menu-item index="operationLogs"><Archive />操作日志</el-menu-item>
          <el-menu-item index="reports"><ShieldAlert />举报处理</el-menu-item>
          <el-menu-item index="appeals"><FileCheck2 />申诉复核</el-menu-item>
          <el-menu-item index="stations"><Building2 />救助站认证</el-menu-item>
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
            <el-table-column label="操作" width="220">
              <template #default="{ row }">
                <el-button v-if="row.status === 'APPROVED'" size="small" text type="primary" @click="openAgreement(row)">协议</el-button>
                <el-button v-if="row.status === 'APPROVED'" size="small" text @click="openFollowUps(row)">回访</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-show="active === 'operationLogs'" class="surface form-shell">
          <div style="display:flex;gap:10px;flex-wrap:wrap;margin-bottom:12px">
            <el-select v-model="operationLogFilters.targetType" clearable style="width: 180px" @change="loadOperationLogs">
              <el-option v-for="item in operationTargetOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-select v-model="operationLogFilters.operationType" clearable style="width: 180px" @change="loadOperationLogs">
              <el-option v-for="item in operationTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-input v-model="operationLogFilters.operatorKeyword" placeholder="操作人" style="width: 160px" @keyup.enter="loadOperationLogs" />
            <el-input v-model="operationLogFilters.keyword" placeholder="对象/说明" style="width: 180px" @keyup.enter="loadOperationLogs" />
            <el-button :icon="RefreshCw" @click="loadOperationLogs">查询</el-button>
          </div>
          <el-table :data="operationLogs" stripe>
            <el-table-column prop="operatedAt" label="时间" width="170">
              <template #default="{ row }">{{ formatTime(row.operatedAt) }}</template>
            </el-table-column>
            <el-table-column prop="operatorNickname" label="操作人" width="130" />
            <el-table-column prop="operationTypeText" label="动作" width="120" />
            <el-table-column prop="targetTypeText" label="对象类型" width="120" />
            <el-table-column prop="targetName" label="对象" min-width="180" show-overflow-tooltip />
            <el-table-column prop="operatorIp" label="IP" width="120" />
            <el-table-column prop="detail" label="说明" min-width="180" show-overflow-tooltip />
            <el-table-column label="详情" width="100">
              <template #default="{ row }">
                <el-button size="small" text @click="openOperationLog(row)">快照</el-button>
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

        <div v-show="active === 'stations'" class="surface form-shell">
          <div style="display:flex;gap:10px;margin-bottom:12px">
            <el-select v-model="stationStatusFilter" clearable style="width: 180px" @change="loadStations">
              <el-option label="待审核" value="PENDING" />
              <el-option label="已认证" value="APPROVED" />
              <el-option label="未通过" value="REJECTED" />
            </el-select>
            <el-button :icon="RefreshCw" @click="loadStations">刷新</el-button>
          </div>
          <el-table :data="stations" stripe>
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="stationName" label="救助站名称" width="160" />
            <el-table-column prop="nickname" label="申请人" width="120" />
            <el-table-column prop="address" label="地址" min-width="140" show-overflow-tooltip />
            <el-table-column prop="contactPhone" label="联系电话" width="120" />
            <el-table-column label="认证状态" width="120">
              <template #default="{ row }">
                <StatusTag :value="row.certificationStatus" :text="row.certificationStatusText"
                           :options="certStatusOptions" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="粉丝" width="70">
              <template #default="{ row }">{{ row.followerCount }}</template>
            </el-table-column>
            <el-table-column label="申请时间" width="160">
              <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button v-if="row.certificationStatus === 'PENDING'" size="small" :icon="Check" type="primary"
                           text @click="openCertify(row, 'APPROVED')">通过</el-button>
                <el-button v-if="row.certificationStatus === 'PENDING'" size="small" :icon="X" type="danger"
                           text @click="openCertify(row, 'REJECTED')">驳回</el-button>
                <el-button v-if="row.certificationStatus !== 'PENDING'" size="small" :icon="Eye" text
                           @click="openStationDetail(row)">详情</el-button>
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

    <el-dialog v-model="detailDialog" :title="`详情 - ${detailTargetTypeText}`" width="760px" append-to-body>
      <el-skeleton v-if="detailLoading" :rows="8" animated />
      <template v-else-if="detailData">
        <div v-if="detailTargetType === 'ANIMAL'" class="audit-detail-grid">
          <div class="detail-item"><label>类型</label><span>{{ detailData.typeText }}</span></div>
          <div class="detail-item"><label>性别</label><span>{{ detailData.genderText }}</span></div>
          <div class="detail-item"><label>年龄</label><span>{{ detailData.age ?? '未知' }}</span></div>
          <div class="detail-item"><label>发现地区</label><span>{{ detailData.foundRegion }}</span></div>
          <div class="detail-item"><label>健康情况</label><span>{{ detailData.healthCondition || '-' }}</span></div>
          <div class="detail-item"><label>状态</label><StatusTag :value="detailData.status" :text="detailData.statusText" :options="animalStatusOptions" /></div>
          <div class="detail-item" v-if="parseExpectedStatus(detailData)"><label>预期操作</label><StatusTag :value="parseExpectedStatus(detailData)" :text="expectedStatusText(parseExpectedStatus(detailData))" :options="animalStatusOptions" /></div>
          <div class="detail-item"><label>发布者</label><span>{{ detailData.publisherNickname }}</span></div>
          <div class="detail-item full-width"><label>描述</label><p>{{ detailData.description || '-' }}</p></div>
          <div class="detail-item full-width" v-if="detailData.coverImageUrl"><label>封面图</label><img :src="detailData.coverImageUrl" style="max-width:200px;border-radius:8px" /></div>
          <div class="detail-item full-width" v-if="detailData.imageUrls?.length">
            <label>图片</label><div class="detail-thumb-row"><img v-for="u in detailData.imageUrls" :key="u" :src="u" style="width:80px;height:80px;object-fit:cover;border-radius:6px;margin-right:6px" /></div>
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
            <label>图片</label><div class="detail-thumb-row"><img v-for="u in detailData.imageUrls" :key="u" :src="u" style="width:80px;height:80px;object-fit:cover;border-radius:6px;margin-right:6px" /></div>
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
        <div v-else-if="detailTargetType === 'COMMUNITY_POST'" class="audit-detail-grid">
          <div class="detail-item full-width"><label>标题</label><p>{{ detailData.title }}</p></div>
          <div class="detail-item"><label>作者</label><span>{{ detailData.authorNickname }}</span></div>
          <div class="detail-item"><label>状态</label><StatusTag :value="detailData.status" :text="detailData.statusText" :options="communityPostStatusOptions" /></div>
          <div class="detail-item full-width"><label>内容</label><p>{{ detailData.content }}</p></div>
          <div class="detail-item full-width" v-if="detailData.imageUrls?.length">
            <label>图片</label><div class="detail-thumb-row"><img v-for="u in detailData.imageUrls" :key="u" :src="u" style="width:80px;height:80px;object-fit:cover;border-radius:6px;margin-right:6px" /></div>
          </div>
        </div>
        <div v-else-if="detailTargetType === 'COMMUNITY_COMMENT'" class="audit-detail-grid">
          <div class="detail-item"><label>作者</label><span>{{ detailData.authorNickname }}</span></div>
          <div class="detail-item"><label>帖子ID</label><span>{{ detailData.postId }}</span></div>
          <div class="detail-item"><label>状态</label><StatusTag :value="detailData.status" :text="detailData.statusText" :options="communityCommentStatusOptions" /></div>
          <div class="detail-item full-width"><label>内容</label><p>{{ detailData.content }}</p></div>
          <div class="detail-item full-width" v-if="detailData.imageUrls?.length">
            <label>图片</label><div class="detail-thumb-row"><img v-for="u in detailData.imageUrls" :key="u" :src="u" style="width:80px;height:80px;object-fit:cover;border-radius:6px;margin-right:6px" /></div>
          </div>
        </div>
      </template>
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

    <el-dialog v-model="certDialog" title="救助站认证" width="560px" append-to-body>
      <div v-if="certTarget" style="margin-bottom: 12px">
        <p><strong>{{ certTarget.stationName }}</strong></p>
        <p class="muted">申请人：{{ certTarget.nickname }}</p>
        <p class="muted">地址：{{ certTarget.address }}</p>
        <p class="muted">联系电话：{{ certTarget.contactPhone }}</p>
      </div>
      <el-form :model="certForm" label-position="top">
        <el-form-item label="认证结果">
          <el-select v-model="certForm.action" style="width: 100%">
            <el-option label="通过" value="APPROVED" />
            <el-option label="驳回" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理意见">
          <el-input v-model="certForm.opinion" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="certDialog = false">取消</el-button>
        <el-button :loading="saving" type="primary" @click="submitCertify">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="stationDetailDialog" title="救助站详情" width="680px" append-to-body>
      <div v-if="stationDetail" class="audit-detail-grid">
        <div class="detail-item"><label>救助站名称</label><span>{{ stationDetail.stationName }}</span></div>
        <div class="detail-item"><label>申请人</label><span>{{ stationDetail.nickname }}</span></div>
        <div class="detail-item"><label>联系电话</label><span>{{ stationDetail.contactPhone }}</span></div>
        <div class="detail-item"><label>地址</label><span>{{ stationDetail.address }}</span></div>
        <div class="detail-item"><label>认证状态</label><StatusTag :value="stationDetail.certificationStatus" :text="stationDetail.certificationStatusText" :options="certStatusOptions" /></div>
        <div class="detail-item"><label>粉丝数</label><span>{{ stationDetail.followerCount }}</span></div>
        <div class="detail-item"><label>申请时间</label><span>{{ formatTime(stationDetail.createdAt) }}</span></div>
        <div class="detail-item full-width"><label>简介</label><p>{{ stationDetail.description || '-' }}</p></div>
        <div v-if="stationDetail.rejectReason" class="detail-item full-width"><label>驳回理由</label><p>{{ stationDetail.rejectReason }}</p></div>
        <div v-if="stationDetail.imageUrl" class="detail-item full-width"><label>封面图片</label><img :src="stationDetail.imageUrl" style="max-width:200px;border-radius:8px" /></div>
      </div>
      <template #footer>
        <el-button @click="stationDetailDialog = false">关闭</el-button>
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
          <el-button v-if="agreementData.pdfUrl" text type="primary" @click="openAgreementPdf">下载 PDF</el-button>
        </div>
        <div class="agreement-content">{{ agreementData.content }}</div>
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
            <img v-for="url in item.imageUrls" :key="url" :src="url" style="width:88px;height:88px;object-fit:cover;border-radius:8px" />
          </div>
          <p class="muted">填写人：{{ item.creatorNickname || '-' }}，完成时间：{{ formatTime(item.completedAt) }}</p>
          <div v-if="item.status !== 'COMPLETED'" class="follow-up-actions">
            <el-button size="small" type="primary" plain @click="openFollowUpComplete(item)">填写回访</el-button>
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

    <el-dialog v-model="operationLogDialogVisible" title="操作日志详情" width="760px" append-to-body>
      <div v-if="operationLogDetail" class="operation-log-detail">
        <div class="audit-detail-grid">
          <div class="detail-item"><label>时间</label><span>{{ formatTime(operationLogDetail.operatedAt) }}</span></div>
          <div class="detail-item"><label>操作人</label><span>{{ operationLogDetail.operatorNickname }} / {{ operationLogDetail.operatorAccount }}</span></div>
          <div class="detail-item"><label>动作</label><span>{{ operationLogDetail.operationTypeText }}</span></div>
          <div class="detail-item"><label>IP</label><span>{{ operationLogDetail.operatorIp || '-' }}</span></div>
          <div class="detail-item"><label>对象类型</label><span>{{ operationLogDetail.targetTypeText }}</span></div>
          <div class="detail-item"><label>对象</label><span>{{ operationLogDetail.targetName }}</span></div>
          <div class="detail-item full-width"><label>说明</label><p>{{ operationLogDetail.detail || '-' }}</p></div>
          <div class="detail-item full-width"><label>变更前</label><pre class="log-json">{{ operationLogDetail.beforeSnapshot || '-' }}</pre></div>
          <div class="detail-item full-width"><label>变更后</label><pre class="log-json">{{ operationLogDetail.afterSnapshot || '-' }}</pre></div>
        </div>
      </div>
      <template #footer>
        <el-button @click="operationLogDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Archive,
  Building2,
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
import ImageUploader from '../components/ImageUploader.vue'
import { adoptionApi, adminApi } from '../api'
import { rescueStationApi } from '../api'
import { notifyError } from '../api/http'
import { useAuth } from '../stores/auth'
import {
  agreementStatusOptions,
  animalStatusOptions,
  appealActionOptions,
  appealStatusOptions,
  appealTargetOptions,
  applyStatusOptions,
  followUpStatusOptions,
  communityCommentStatusOptions,
  communityPostStatusOptions,
  noticeStatusOptions,
  operationTargetOptions,
  operationTypeOptions,
  reportActionOptions,
  reportStatusOptions,
  reportTargetOptions,
  rescueStatusOptions,
  roleOptions,
  userStatusOptions,
  certificationOptions as certStatusOptions
} from '../utils/status'

const auth = useAuth()
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
const operationLogs = ref([])
const reports = ref([])
const appeals = ref([])
const auditType = ref('')
const reportStatusFilter = ref('')
const reportTypeFilter = ref('')
const appealStatusFilter = ref('')
const appealTypeFilter = ref('')
const operationLogFilters = reactive({
  targetType: '',
  operationType: '',
  operatorKeyword: '',
  keyword: ''
})

const auditDialog = ref(false)
const detailDialog = ref(false)
const noticeDialog = ref(false)
const reportDialog = ref(false)
const appealDialog = ref(false)
const agreementDialogVisible = ref(false)
const followUpDialogVisible = ref(false)
const followUpEditorVisible = ref(false)
const operationLogDialogVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref(null)
const detailTargetType = ref('')
const reportTarget = ref(null)
const appealTarget = ref(null)
const agreementData = ref(null)
const agreementCurrentApplyId = ref(null)
const agreementSignatureForm = reactive({ signatureName: '', signatureDataUrl: '' })
const signatureCanvasRef = ref()
const signatureHasStroke = ref(false)
const followUps = ref([])
const followUpCurrentApplyId = ref(null)
const followUpEditor = reactive({ id: null, note: '', imageUrls: [] })
const operationLogDetail = ref(null)
let signatureDrawing = false
let signatureLastPoint = { x: 0, y: 0 }

// Rescue station management
const stations = ref([])
const stationDetailDialog = ref(false)
const stationDetail = ref(null)
const stationStatusFilter = ref('')
const certDialog = ref(false)
const certTarget = ref(null)
const certForm = reactive({ action: 'APPROVED', opinion: '' })

const canSignAgreement = computed(() => {
  if (!agreementData.value || agreementData.value.status === 'COMPLETED') {
    return false
  }
  const currentUserId = auth.state.user?.id
  if (!currentUserId) return false
  return !agreementData.value.counterpartSignatureName || currentUserId === agreementData.value.adopterId
})

const detailTypeLabels = { ANIMAL: '动物档案', RESCUE: '救助信息', ADOPT_APPLY: '领养申请', COMMUNITY_POST: '社区帖子', COMMUNITY_COMMENT: '社区评论' }
const detailTargetTypeText = ref('')
const API_BASE = window.location.origin

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

function getFullUrl(url) {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:')) return url
  return `${API_BASE}${url}`
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

const auditForm = reactive({ targetType: '', targetId: null, action: 'APPROVE', opinion: '' })
const noticeForm = reactive({ id: null, title: '', content: '', status: 'DRAFT' })
const reportForm = reactive({ action: 'DISMISS', opinion: '' })
const appealForm = reactive({ action: 'ESCALATE', opinion: '' })

async function loadAll() {
  await Promise.allSettled([
    loadDashboard(),
    loadPending(),
    loadUsers(),
    loadNotices(),
    loadApplications(),
    loadReports(),
    loadAppeals(),
    loadStations()
  ])
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

async function loadOperationLogs() {
  try {
    operationLogs.value = (await adminApi.operationLogs({
      targetType: operationLogFilters.targetType || undefined,
      operationType: operationLogFilters.operationType || undefined,
      operatorKeyword: operationLogFilters.operatorKeyword || undefined,
      keyword: operationLogFilters.keyword || undefined,
      page: 0,
      size: 50
    })).content || []
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
    ElMessage.success(certForm.action === 'APPROVED' ? '已通过认证' : '已驳回')
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

async function openAgreement(row) {
  try {
    agreementCurrentApplyId.value = row.id
    agreementData.value = await adoptionApi.agreement(row.id)
    agreementSignatureForm.signatureName = auth.state.user?.nickname || '平台管理员'
    agreementSignatureForm.signatureDataUrl = ''
    agreementDialogVisible.value = true
    await nextTick()
    initSignatureCanvas()
  } catch (error) {
    notifyError(error)
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
    await loadApplications()
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

async function openFollowUps(row) {
  try {
    followUpCurrentApplyId.value = row.id
    followUps.value = await adoptionApi.followUps(row.id)
    followUpDialogVisible.value = true
  } catch (error) {
    notifyError(error)
  }
}

function openFollowUpComplete(item) {
  followUpEditor.id = item.id
  followUpEditor.note = item.note || ''
  followUpEditor.imageUrls = item.imageUrls ? [...item.imageUrls] : []
  followUpEditorVisible.value = true
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
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

function openOperationLog(row) {
  operationLogDetail.value = row
  operationLogDialogVisible.value = true
}

function formatTime(value) {
  return value ? new Date(value).toLocaleString('zh-CN') : '-'
}

onMounted(loadAll)

watch(active, async (value) => {
  if (value === 'operationLogs') {
    await loadOperationLogs()
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
.agreement-actions,
.follow-up-actions {
  display: flex;
  justify-content: flex-end;
}
.agreement-content,
.log-json {
  padding: 14px;
  border-radius: 12px;
  background: rgba(244, 248, 246, 0.95);
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.7;
  color: #30413b;
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
</style>
