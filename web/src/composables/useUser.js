import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import request, { apiData } from '../utils/request'
import { normalizeUserMedia } from '../utils/media'

const token = ref(localStorage.getItem('token') || '')
const userInfo = ref(normalizeUserMedia(parseStoredUser()))
const showAuthDialog = ref(false)
const isLoginMode = ref(true)
const authForm = ref({ username: '', password: '', nickname: '', email: '' })
const authLoading = ref(false)
const authRedirect = ref('')

let authExpiredListenerBound = false

function parseStoredUser() {
  try {
    return JSON.parse(localStorage.getItem('user') || 'null')
  } catch {
    localStorage.removeItem('user')
    return null
  }
}

function saveSession(rawToken, user) {
  const displayUser = normalizeUserMedia(user)
  token.value = rawToken
  userInfo.value = displayUser
  localStorage.setItem('token', rawToken)
  localStorage.setItem('user', JSON.stringify(displayUser))
}

function saveUser(user) {
  const displayUser = normalizeUserMedia(user)
  userInfo.value = displayUser
  localStorage.setItem('user', JSON.stringify(displayUser))
}

function clearSession() {
  token.value = ''
  userInfo.value = null
  localStorage.removeItem('token')
  localStorage.removeItem('user')
}

function bindAuthExpiredListener() {
  if (authExpiredListenerBound || typeof window === 'undefined') return
  window.addEventListener('yukiss:auth-expired', clearSession)
  authExpiredListenerBound = true
}

bindAuthExpiredListener()

export function useUser() {
  const openAuth = (mode = 'login', redirect = '') => {
    isLoginMode.value = mode !== 'register'
    authRedirect.value = redirect
    showAuthDialog.value = true
  }

  const closeAuth = () => {
    showAuthDialog.value = false
    authRedirect.value = ''
  }

  const consumeAuthRedirect = () => {
    const redirect = authRedirect.value
    authRedirect.value = ''
    return redirect
  }

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
      return data.user
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
    if (!token.value) return null
    const user = apiData(await request.get('/user/me'))
    saveUser(user)
    return user
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
    authRedirect,
    saveUser,
    clearSession,
    openAuth,
    closeAuth,
    consumeAuthRedirect,
    doLogin,
    doRegister,
    doLogout,
    refreshMe,
  }
}
