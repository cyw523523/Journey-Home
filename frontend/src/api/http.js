import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: '/api',
  timeout: 12000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('guitu_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && Object.prototype.hasOwnProperty.call(body, 'success')) {
      if (!body.success) {
        throw new Error(body.message || '请求失败')
      }
      return body.data
    }
    return body
  },
  (error) => {
    const message = error.response?.data?.message || error.message || '网络异常，请稍后重试'
    if (error.response?.status === 401) {
      localStorage.removeItem('guitu_token')
      localStorage.removeItem('guitu_user')
    }
    return Promise.reject(new Error(message))
  }
)

export function notifyError(error) {
  ElMessage.error(error?.message || '操作失败')
}

export default http
