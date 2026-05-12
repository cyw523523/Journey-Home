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

export const roleOptions = [
  { label: '普通用户', value: 'USER' },
  { label: '救助发布者', value: 'RESCUER' },
  { label: '管理员', value: 'ADMIN' }
]

export const userStatusOptions = [
  { label: '正常', value: 'NORMAL', type: 'success' },
  { label: '禁用', value: 'DISABLED', type: 'danger' }
]

export function optionText(options, value) {
  return options.find((item) => item.value === value)?.label || value || '-'
}

export function optionType(options, value) {
  return options.find((item) => item.value === value)?.type || 'info'
}
