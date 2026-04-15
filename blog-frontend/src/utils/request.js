// src/utils/request.js
import axios from 'axios'

const service = axios.create({
  baseURL: 'http://localhost:5050',
  timeout: 5000
})

// 可扩展拦截器
service.interceptors.response.use(
  response => response,
  error => {
    return Promise.reject(error)
  }
)

export default service
