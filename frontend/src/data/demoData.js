export const demoImages = [
  'https://images.unsplash.com/photo-1573865526739-10659fec78a5?auto=format&fit=crop&w=900&q=80',
  'https://images.unsplash.com/photo-1552053831-71594a27632d?auto=format&fit=crop&w=900&q=80',
  'https://images.unsplash.com/photo-1519052537078-e6302a4968d4?auto=format&fit=crop&w=900&q=80',
  'https://images.unsplash.com/photo-1543466835-00a7907e9de1?auto=format&fit=crop&w=900&q=80'
]

export const demoAnimals = [
  {
    id: 101,
    type: 'CAT',
    typeText: '猫',
    gender: 'FEMALE',
    genderText: '母',
    age: 1,
    foundRegion: '武汉大学信息学部',
    healthCondition: '已驱虫，性格亲人',
    coverImageUrl: demoImages[0],
    imageUrls: [demoImages[0]],
    description: '常在宿舍楼附近活动，适合有耐心的领养家庭。',
    status: 'WAITING_ADOPTION',
    statusText: '待领养',
    publisherNickname: '归途志愿者'
  },
  {
    id: 102,
    type: 'DOG',
    typeText: '狗',
    gender: 'MALE',
    genderText: '公',
    age: 2,
    foundRegion: '东湖绿道',
    healthCondition: '轻微皮肤病治疗中',
    coverImageUrl: demoImages[1],
    imageUrls: [demoImages[1]],
    description: '已临时安置，需要后续救助与观察。',
    status: 'RESCUING',
    statusText: '救助中',
    publisherNickname: '救助发布者'
  },
  {
    id: 103,
    type: 'CAT',
    typeText: '猫',
    gender: 'UNKNOWN',
    genderText: '未知',
    age: 0,
    foundRegion: '街道口',
    healthCondition: '待检查',
    coverImageUrl: demoImages[2],
    imageUrls: [demoImages[2]],
    description: '幼猫，需要奶猫照护经验。',
    status: 'WAITING_RESCUE',
    statusText: '待救助',
    publisherNickname: '热心用户'
  }
]

export const demoRescues = [
  {
    id: 201,
    location: '珞珈山路附近',
    animalCondition: '橘猫疑似受伤，行动缓慢',
    contact: '19900000000',
    description: '需要附近志愿者协助观察并送医。',
    imageUrls: [demoImages[3]],
    status: 'PENDING_PROCESS',
    statusText: '待处理',
    publisherNickname: '热心用户'
  },
  {
    id: 202,
    location: '光谷广场',
    animalCondition: '小型犬走失，已临时喂食',
    contact: '19900000001',
    description: '希望联系救助组织或原主人。',
    imageUrls: [demoImages[1]],
    status: 'PROCESSING',
    statusText: '处理中',
    publisherNickname: '归途志愿者'
  }
]

export const demoNotices = [
  { id: 301, title: '周末领养开放日安排', content: '本周六下午开放线下领养咨询。', status: 'PUBLISHED', statusText: '已发布' },
  { id: 302, title: '图片上传规范', content: '请上传清晰正面照和环境照，便于审核。', status: 'PUBLISHED', statusText: '已发布' }
]

export const demoOverview = {
  userCount: 128,
  animalCount: 36,
  rescueCount: 18,
  applyCount: 42,
  pendingAuditCount: 7
}
