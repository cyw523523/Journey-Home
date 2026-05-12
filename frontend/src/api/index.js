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
  applications: (params) => http.get('/users/me/applications', { params })
}

export const animalApi = {
  list: (params) => http.get('/animals', { params }),
  detail: (id) => http.get(`/animals/${id}`),
  create: (data) => http.post('/animals', data),
  update: (id, data) => http.put(`/animals/${id}`, data),
  offline: (id) => http.delete(`/animals/${id}`)
}

export const rescueApi = {
  list: (params) => http.get('/rescues', { params }),
  detail: (id) => http.get(`/rescues/${id}`),
  create: (data) => http.post('/rescues', data),
  updateStatus: (id, data) => http.patch(`/rescues/${id}/status`, data),
  offline: (id) => http.delete(`/rescues/${id}`)
}

export const adoptionApi = {
  create: (data) => http.post('/adoptions', data),
  list: (params) => http.get('/adoptions', { params }),
  detail: (id) => http.get(`/adoptions/${id}`),
  cancel: (id) => http.patch(`/adoptions/${id}/cancel`)
}

export const noticeApi = {
  list: (params) => http.get('/notices', { params }),
  detail: (id) => http.get(`/notices/${id}`)
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
  deleteNotice: (id) => http.delete(`/admin/notices/${id}`)
}
