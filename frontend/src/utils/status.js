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

export function optionText(options, value) {
  return options.find((item) => item.value === value)?.label || value || '-'
}

export function optionType(options, value) {
  return options.find((item) => item.value === value)?.type || 'info'
}
