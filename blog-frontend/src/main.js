import { createApp } from 'vue'
import App from './App.vue'
import ElementPlus, { ElMessage } from 'element-plus'
import 'element-plus/dist/index.css'
import axios from 'axios' // 引入 axios
import { router } from './router' // 引入路由
import './style.css'

axios.defaults.baseURL = 'http://localhost:4000'

// ==========================================
// Axios 全局拦截器
// ==========================================

// 1. 请求拦截器：每次发请求前，去 localStorage 里找手环 (Token)
axios.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    // 只要有手环，就自动戴在请求头 (Authorization) 上
    config.headers.Authorization = token
  }
  return config
}, error => {
  return Promise.reject(error)
})

// 2. 响应拦截器：专门处理保安 (后端) 拦截的情况
axios.interceptors.response.use(res => {
  return res
}, error => {
  // 如果后端返回 401 (未授权)，说明手环没带，或者过期了
  if (error.response && error.response.status === 401) {
    ElMessage.error('身份验证失败或已过期，请重新登录喵！')
    localStorage.removeItem('token') // 销毁假手环
    localStorage.removeItem('user')  // 清除用户信息
    // 这里可以加一个事件总线或者重载页面，暂时先让它报错
  }
  return Promise.reject(error)
})

// ==========================================

const app = createApp(App)
app.use(ElementPlus)
app.use(router)
app.mount('#app')
