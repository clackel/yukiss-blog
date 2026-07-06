import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import request, { apiData } from '../utils/request'

const token = ref(localStorage.getItem('token') || '')
const userInfo = ref(JSON.parse(localStorage.getItem('user') || 'null'))
const showAuthDialog = ref(false)
const isLoginMode = ref(true)
const authForm = ref({ username: '', password: '', nickname: '', email: '' })
const authLoading = ref(false)

function saveSession(rawToken, user) {
  token.value = rawToken
  userInfo.value = user
  localStorage.setItem('token', rawToken)
  localStorage.setItem('user', JSON.stringify(user))
}

function saveUser(user) {
  userInfo.value = user
  localStorage.setItem('user', JSON.stringify(user))
}

function clearSession() {
  token.value = ''
  userInfo.value = null
  localStorage.removeItem('token')
  localStorage.removeItem('user')
}

export function useUser() {
  const doLogin = async () => {
    authLoading.value = true
    try {
      const data = apiData(await request.post('/user/login', {
        username: authForm.value.username,
        password: authForm.value.password,
      }))
      saveSession(data.token, data.user)
      ElMessage.success('登录成功')
      showAuthDialog.value = false
      authForm.value = { username: '', password: '', nickname: '', email: '' }
    } finally {
      authLoading.value = false
    }
  }

  const doRegister = async () => {
    authLoading.value = true
    try {
      await request.post('/user/register', authForm.value)
      ElMessage.success('注册成功，现在可以登录了')
      isLoginMode.value = true
      authForm.value.password = ''
    } finally {
      authLoading.value = false
    }
  }

  const refreshMe = async () => {
    if (!token.value) return
    const user = apiData(await request.get('/user/me'))
    saveUser(user)
  }

  const doLogout = () => {
    clearSession()
    ElMessage.success('已退出登录')
  }

  return {
    token,
    userInfo,
    showAuthDialog,
    isLoginMode,
    authForm,
    authLoading,
    saveUser,
    clearSession,
    doLogin,
    doRegister,
    doLogout,
    refreshMe,
  }
}
