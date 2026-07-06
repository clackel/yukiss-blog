import axios from 'axios'
import { ElMessage } from 'element-plus'

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:4000'

const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 8000,
})

request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = token
    }
    return config
  },
  error => Promise.reject(error)
)

request.interceptors.response.use(
  response => response,
  error => {
    const message = error.response?.data?.message || error.response?.data || '服务连接失败，请稍后再试'
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      ElMessage.error('登录状态已失效，请重新登录')
    } else if (message) {
      ElMessage.error(message)
    }
    return Promise.reject(error)
  }
)

export function apiData(response) {
  const body = response.data
  if (body && typeof body === 'object' && 'success' in body) {
    if (!body.success) {
      throw new Error(body.message || '操作失败')
    }
    return body.data
  }
  return body
}

export default request
