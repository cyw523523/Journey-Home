import http from './http'

export const authApi = {
  register: (data) => http.post('/auth/register', data),
  login: (data) => http.post('/auth/login', data),
  logout: () => http.post('/auth/logout'),
  me: () => http.get('/auth/me')
}

export const userApi = {
  profile: () => http.get('/users/me'),
  update: (data) => http.put('/users/me', data),
  changePassword: (data) => http.put('/users/me/password', data),
  animals: (params) => http.get('/users/me/animals', { params }),
  rescues: (params) => http.get('/users/me/rescues', { params }),
  applications: (params) => http.get('/users/me/applications', { params }),
  publicProfile: (id) => http.get(`/users/${id}`)
}

export const animalApi = {
  list: (params) => http.get('/animals', { params }),
  detail: (id) => http.get(`/animals/${id}`),
  create: (data) => http.post('/animals', data),
  update: (id, data) => http.put(`/animals/${id}`, data),
  updateStatus: (id, data) => http.patch(`/animals/${id}/status`, data),
  offline: (id) => http.delete(`/animals/${id}`)
}

export const rescueApi = {
  list: (params) => http.get('/rescues', { params }),
  detail: (id) => http.get(`/rescues/${id}`),
  create: (data) => http.post('/rescues', data),
  update: (id, data) => http.put(`/rescues/${id}`, data),
  updateStatus: (id, data) => http.patch(`/rescues/${id}/status`, data),
  offline: (id) => http.delete(`/rescues/${id}`)
}

export const adoptionApi = {
  create: (data) => http.post('/adoptions', data),
  smartMatch: (data) => http.post('/adoptions/smart-match', data, { timeout: 60000 }),
  list: (params) => http.get('/adoptions', { params }),
  detail: (id) => http.get(`/adoptions/${id}`),
  cancel: (id) => http.patch(`/adoptions/${id}/cancel`)
}

export const noticeApi = {
  list: (params) => http.get('/notices', { params }),
  detail: (id) => http.get(`/notices/${id}`)
}

export const communityApi = {
  list: (params) => http.get('/community/posts', { params }),
  myPosts: (params) => http.get('/community/mine/posts', { params }),
  myComments: (params) => http.get('/community/mine/comments', { params }),
  detail: (id) => http.get(`/community/posts/${id}`),
  create: (data) => http.post('/community/posts', data),
  update: (id, data) => http.put(`/community/posts/${id}`, data),
  delete: (id) => http.delete(`/community/posts/${id}`),
  createComment: (id, data) => http.post(`/community/posts/${id}/comments`, data),
  deleteComment: (id) => http.delete(`/community/comments/${id}`)
}

export const reportApi = {
  create: (data) => http.post('/reports', data),
  list: (params) => http.get('/reports', { params }),
  detail: (id) => http.get(`/reports/${id}`)
}

export const appealApi = {
  create: (data) => http.post('/appeals', data),
  list: (params) => http.get('/appeals', { params }),
  detail: (id) => http.get(`/appeals/${id}`)
}

export const notificationApi = {
  list: (params) => http.get('/notifications', { params }),
  summary: () => http.get('/notifications/summary'),
  markRead: (id) => http.patch(`/notifications/${id}/read`),
  markAllRead: () => http.patch('/notifications/read-all')
}

export const messageApi = {
  listConversations: (params) => http.get('/messages/conversations', { params }),
  detail: (id) => http.get(`/messages/conversations/${id}`),
  detailWithUser: (userId) => http.get(`/messages/conversations/with/${userId}`),
  send: (conversationId, data) => http.post(`/messages/conversations/${conversationId}/messages`, data),
  summary: () => http.get('/messages/summary')
}

export const homeApi = {
  overview: () => http.get('/home/overview')
}

export const adminApi = {
  overview: () => http.get('/admin/stats/overview'),
  animalStatus: () => http.get('/admin/stats/animals/status'),
  rescueStatus: () => http.get('/admin/stats/rescues/status'),
  applyStatus: () => http.get('/admin/stats/applications/status'),
  pending: (params) => http.get('/admin/audits/pending', { params }),
  auditDetail: (targetType, targetId) => http.get(`/admin/audits/${targetType}/${targetId}`),
  audit: (data) => http.post('/admin/audits', data),
  logs: (params) => http.get('/admin/audit-logs', { params }),
  applications: (params) => http.get('/admin/applications', { params }),
  users: (params) => http.get('/admin/users', { params }),
  updateUser: (id, data) => http.patch(`/admin/users/${id}`, data),
  notices: (params) => http.get('/admin/notices', { params }),
  createNotice: (data) => http.post('/admin/notices', data),
  updateNotice: (id, data) => http.put(`/admin/notices/${id}`, data),
  offlineNotice: (id) => http.patch(`/admin/notices/${id}/offline`),
  deleteNotice: (id) => http.delete(`/admin/notices/${id}`),
  reports: (params) => http.get('/admin/reports', { params }),
  reportDetail: (id) => http.get(`/admin/reports/${id}`),
  resolveReport: (id, data) => http.patch(`/admin/reports/${id}`, data),
  appeals: (params) => http.get('/admin/appeals', { params }),
  appealDetail: (id) => http.get(`/admin/appeals/${id}`),
  reviewAppeal: (id, data) => http.patch(`/admin/appeals/${id}`, data)
}
