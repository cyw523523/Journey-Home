import i18n from '../i18n'

export const animalTypeOptions = [
  { label: '猫', value: 'CAT' },
  { label: '狗', value: 'DOG' },
  { label: '其他', value: 'OTHER' }
]

export const genderOptions = [
  { label: '公', value: 'MALE' },
  { label: '母', value: 'FEMALE' },
  { label: '未知', value: 'UNKNOWN' }
]

export const animalStatusOptions = [
  { label: '待审核', value: 'PENDING_REVIEW', type: 'warning' },
  { label: '待救助', value: 'WAITING_RESCUE', type: 'danger' },
  { label: '救助中', value: 'RESCUING', type: 'primary' },
  { label: '待领养', value: 'WAITING_ADOPTION', type: 'success' },
  { label: '已领养', value: 'ADOPTED', type: 'info' },
  { label: '已下架', value: 'OFFLINE', type: 'info' },
  { label: '已驳回', value: 'REJECTED', type: 'danger' }
]

export const rescueStatusOptions = [
  { label: '待审核', value: 'PENDING_REVIEW', type: 'warning' },
  { label: '待处理', value: 'PENDING_PROCESS', type: 'danger' },
  { label: '处理中', value: 'PROCESSING', type: 'primary' },
  { label: '已完成', value: 'COMPLETED', type: 'success' },
  { label: '已下架', value: 'OFFLINE', type: 'info' },
  { label: '已驳回', value: 'REJECTED', type: 'danger' }
]

export const applyStatusOptions = [
  { label: '待审核', value: 'PENDING_REVIEW', type: 'warning' },
  { label: '已通过', value: 'APPROVED', type: 'success' },
  { label: '已驳回', value: 'REJECTED', type: 'danger' },
  { label: '已取消', value: 'CANCELED', type: 'info' }
]

export const agreementStatusOptions = [
  { label: '待领养人签署', value: 'PENDING_ADOPTER', type: 'warning' },
  { label: '待救助方签署', value: 'PENDING_COUNTERPART', type: 'primary' },
  { label: '已签署完成', value: 'COMPLETED', type: 'success' }
]

export const followUpStatusOptions = [
  { label: '待回访', value: 'PENDING', type: 'warning' },
  { label: '已回访', value: 'COMPLETED', type: 'success' }
]

export const operationTargetOptions = [
  { label: '动物档案', value: 'ANIMAL' },
  { label: '救助信息', value: 'RESCUE' },
  { label: '领养申请', value: 'ADOPT_APPLY' },
  { label: '领养协议', value: 'ADOPTION_AGREEMENT' },
  { label: '领养回访', value: 'ADOPTION_FOLLOW_UP' },
  { label: '社区帖子', value: 'COMMUNITY_POST' },
  { label: '社区评论', value: 'COMMUNITY_COMMENT' },
  { label: '公告', value: 'NOTICE' },
  { label: '用户', value: 'USER' }
]

export const operationTypeOptions = [
  { label: '创建', value: 'CREATE' },
  { label: '编辑', value: 'UPDATE' },
  { label: '下架', value: 'OFFLINE' },
  { label: '状态变更', value: 'STATUS_CHANGE' },
  { label: '提交申请', value: 'SUBMIT_APPLICATION' },
  { label: '取消申请', value: 'CANCEL_APPLICATION' },
  { label: '通过申请', value: 'APPROVE_APPLICATION' },
  { label: '驳回申请', value: 'REJECT_APPLICATION' },
  { label: '生成协议', value: 'CREATE_AGREEMENT' },
  { label: '签署协议', value: 'SIGN_AGREEMENT' },
  { label: '生成回访计划', value: 'CREATE_FOLLOW_UP_PLAN' },
  { label: '完成回访', value: 'COMPLETE_FOLLOW_UP' }
]

export const noticeStatusOptions = [
  { label: '草稿', value: 'DRAFT', type: 'info' },
  { label: '已发布', value: 'PUBLISHED', type: 'success' },
  { label: '已下架', value: 'OFFLINE', type: 'warning' }
]

export const communityPostStatusOptions = [
  { label: '待审核', value: 'PENDING_REVIEW', type: 'warning' },
  { label: '已发布', value: 'PUBLISHED', type: 'success' },
  { label: '已驳回', value: 'REJECTED', type: 'danger' },
  { label: '已下架', value: 'OFFLINE', type: 'warning' }
]

export const communityCommentStatusOptions = [
  { label: '待审核', value: 'PENDING_REVIEW', type: 'warning' },
  { label: '已发布', value: 'PUBLISHED', type: 'success' },
  { label: '已驳回', value: 'REJECTED', type: 'danger' },
  { label: '已下架', value: 'OFFLINE', type: 'warning' }
]

export const roleOptions = [
  { label: '普通用户', value: 'USER' },
  { label: '救助发布者', value: 'RESCUER' },
  { label: '管理员', value: 'ADMIN' }
]

export const userStatusOptions = [
  { label: '正常', value: 'NORMAL', type: 'success' },
  { label: '禁用', value: 'DISABLED', type: 'danger' }
]

export const reportTargetOptions = [
  { label: '动物档案', value: 'ANIMAL' },
  { label: '救助信息', value: 'RESCUE' },
  { label: '社区帖子', value: 'COMMUNITY_POST' },
  { label: '社区评论', value: 'COMMUNITY_COMMENT' },
  { label: '用户', value: 'USER' }
]

export const reportReasonOptions = [
  { label: '虚假信息', value: 'FALSE_INFORMATION' },
  { label: '广告引流', value: 'ADVERTISEMENT' },
  { label: '辱骂骚扰', value: 'HARASSMENT' },
  { label: '不良内容', value: 'ABUSE' },
  { label: '敏感图片', value: 'SENSITIVE_IMAGE' },
  { label: '垃圾刷屏', value: 'SPAM' },
  { label: '其他', value: 'OTHER' }
]

export const reportStatusOptions = [
  { label: '待处理', value: 'PENDING_REVIEW', type: 'warning' },
  { label: '处理中', value: 'PROCESSING', type: 'primary' },
  { label: '举报属实', value: 'RESOLVED_VALID', type: 'success' },
  { label: '举报不属实', value: 'RESOLVED_INVALID', type: 'info' },
  { label: '重复举报', value: 'DISMISSED_DUPLICATE', type: 'info' }
]

export const reportActionOptions = [
  { label: '驳回举报', value: 'DISMISS' },
  { label: '仅警告', value: 'WARN_ONLY' },
  { label: '下架内容', value: 'OFFLINE_CONTENT' },
  { label: '禁用账号', value: 'BAN_USER' }
]

export const appealTargetOptions = [
  { label: '动物档案', value: 'ANIMAL' },
  { label: '救助信息', value: 'RESCUE' },
  { label: '领养申请', value: 'ADOPT_APPLY' },
  { label: '社区帖子', value: 'COMMUNITY_POST' },
  { label: '社区评论', value: 'COMMUNITY_COMMENT' }
]

export const appealStatusOptions = [
  { label: '待复核', value: 'PENDING_REVIEW', type: 'warning' },
  { label: '二次复核中', value: 'SECOND_REVIEW_PENDING', type: 'primary' },
  { label: '申诉通过', value: 'APPROVED', type: 'success' },
  { label: '申诉驳回', value: 'REJECTED', type: 'danger' }
]

export const appealActionOptions = [
  { label: '升级二次复核', value: 'ESCALATE' },
  { label: '通过申诉', value: 'APPROVE' },
  { label: '驳回申诉', value: 'REJECT' }
]

export const supplyCategoryOptions = [
  { label: '猫粮', value: 'CAT_FOOD' },
  { label: '狗粮', value: 'DOG_FOOD' },
  { label: '猫砂', value: 'CAT_LITTER' },
  { label: '药品', value: 'MEDICINE' },
  { label: '玩具', value: 'TOYS' },
  { label: '垫子/窝', value: 'BEDDING' },
  { label: '清洁用品', value: 'CLEANING' },
  { label: '其他', value: 'OTHER' }
]

export const donationStatusOptions = [
  { label: '待认领', value: 'PENDING', type: 'warning' },
  { label: '已认领', value: 'CLAIMED', type: 'primary' },
  { label: '运输中', value: 'IN_TRANSIT', type: 'primary' },
  { label: '已完成', value: 'COMPLETED', type: 'success' },
  { label: '已取消', value: 'CANCELLED', type: 'info' }
]

export const volunteerTaskStatusOptions = [
  { label: '待审核', value: 'PENDING_REVIEW', type: 'warning' },
  { label: '招募中', value: 'RECRUITING', type: 'success' },
  { label: '进行中', value: 'IN_PROGRESS', type: 'primary' },
  { label: '已完成', value: 'COMPLETED', type: 'success' },
  { label: '已取消', value: 'CANCELLED', type: 'info' }
]

export const volunteerApplicationStatusOptions = [
  { label: '待确认', value: 'PENDING', type: 'warning' },
  { label: '已通过', value: 'APPROVED', type: 'success' },
  { label: '已拒绝', value: 'REJECTED', type: 'danger' },
  { label: '已撤回', value: 'WITHDRAWN', type: 'info' },
  { label: '已完成', value: 'COMPLETED', type: 'success' }
]

export const certificationOptions = [
  { label: '待审核', value: 'PENDING', type: 'warning' },
  { label: '已认证', value: 'APPROVED', type: 'success' },
  { label: '未通过', value: 'REJECTED', type: 'danger' }
]

export const medicalRecordTypeOptions = [
  { label: '驱虫', value: 'DEWORMING' },
  { label: '疫苗', value: 'VACCINE' },
  { label: '绝育', value: 'NEUTERING' },
  { label: '诊疗', value: 'TREATMENT' },
  { label: '其他', value: 'OTHER' }
]

const labelTranslationKeys = {
  猫: 'statusLabel.cat',
  狗: 'statusLabel.dog',
  其他: 'statusLabel.other',
  公: 'statusLabel.male',
  母: 'statusLabel.female',
  未知: 'statusLabel.unknown',
  待审核: 'statusLabel.pendingReview',
  待救助: 'statusLabel.waitingRescue',
  救助中: 'statusLabel.rescuing',
  待领养: 'statusLabel.waitingAdoption',
  已领养: 'statusLabel.adopted',
  已下架: 'statusLabel.offline',
  已驳回: 'statusLabel.rejected',
  待处理: 'statusLabel.pendingProcess',
  处理中: 'statusLabel.processing',
  已完成: 'statusLabel.completed',
  已通过: 'statusLabel.approved',
  已取消: 'statusLabel.canceled',
  待领养人签署: 'statusLabel.pendingAdopter',
  待救助方签署: 'statusLabel.pendingCounterpart',
  已签署完成: 'statusLabel.signedCompleted',
  待回访: 'statusLabel.pendingFollowUp',
  动物档案: 'statusLabel.animalRecord',
  救助信息: 'statusLabel.rescueInfo',
  领养申请: 'statusLabel.adoptionApply',
  领养协议: 'statusLabel.adoptionAgreement',
  领养回访: 'statusLabel.adoptionFollowUp',
  社区帖子: 'statusLabel.communityPost',
  社区评论: 'statusLabel.communityComment',
  公告: 'statusLabel.notice',
  用户: 'statusLabel.user',
  创建: 'statusLabel.create',
  编辑: 'statusLabel.update',
  状态变更: 'statusLabel.statusChange',
  提交申请: 'statusLabel.submitApplication',
  取消申请: 'statusLabel.cancelApplication',
  通过申请: 'statusLabel.approveApplication',
  驳回申请: 'statusLabel.rejectApplication',
  生成协议: 'statusLabel.createAgreement',
  签署协议: 'statusLabel.signAgreement',
  生成回访计划: 'statusLabel.createFollowUpPlan',
  完成回访: 'statusLabel.completeFollowUp',
  草稿: 'statusLabel.draft',
  已发布: 'statusLabel.published',
  普通用户: 'statusLabel.member',
  救助发布者: 'statusLabel.rescuer',
  管理员: 'statusLabel.admin',
  正常: 'statusLabel.normal',
  禁用: 'statusLabel.disabled',
  虚假信息: 'statusLabel.falseInformation',
  广告引流: 'statusLabel.advertisement',
  辱骂骚扰: 'statusLabel.harassment',
  不良内容: 'statusLabel.abuse',
  敏感图片: 'statusLabel.sensitiveImage',
  垃圾刷屏: 'statusLabel.spam',
  举报属实: 'statusLabel.resolvedValid',
  举报不属实: 'statusLabel.resolvedInvalid',
  重复举报: 'statusLabel.dismissedDuplicate',
  驳回举报: 'statusLabel.dismiss',
  仅警告: 'statusLabel.warnOnly',
  下架内容: 'statusLabel.offlineContent',
  禁用账号: 'statusLabel.banUser',
  待复核: 'statusLabel.pendingAppealReview',
  二次复核中: 'statusLabel.secondReviewPending',
  申诉通过: 'statusLabel.appealApproved',
  申诉驳回: 'statusLabel.appealRejected',
  升级二次复核: 'statusLabel.escalate',
  通过申诉: 'statusLabel.approveAppeal',
  驳回申诉: 'statusLabel.rejectAppeal',
  猫粮: 'statusLabel.catFood',
  狗粮: 'statusLabel.dogFood',
  猫砂: 'statusLabel.catLitter',
  药品: 'statusLabel.medicine',
  玩具: 'statusLabel.toys',
  '垫子/窝': 'statusLabel.bedding',
  清洁用品: 'statusLabel.cleaning',
  待认领: 'statusLabel.unclaimed',
  已认领: 'statusLabel.claimed',
  运输中: 'statusLabel.inTransit',
  招募中: 'statusLabel.recruiting',
  进行中: 'statusLabel.inProgress',
  待确认: 'statusLabel.pendingConfirmation',
  已拒绝: 'statusLabel.rejectedConfirmed',
  已撤回: 'statusLabel.withdrawn',
  未通过: 'statusLabel.notPassed',
  驱虫: 'statusLabel.deworming',
  疫苗: 'statusLabel.vaccine',
  绝育: 'statusLabel.neutering',
  诊疗: 'statusLabel.treatment'
}

export function translateLabel(label) {
  if (!label || i18n.global.locale.value === 'zh') {
    return label || '-'
  }

  const key = labelTranslationKeys[label]
  if (!key) {
    return label
  }

  const translated = i18n.global.t(key)
  return translated === key ? label : translated
}

export function optionText(options, value) {
  const label = options.find((item) => item.value === value)?.label
  return translateLabel(label || value || '-')
}

export function optionType(options, value) {
  return options.find((item) => item.value === value)?.type || 'info'
}
