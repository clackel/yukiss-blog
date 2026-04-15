import { ref } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

export function useUser() {
  // 1. 从浏览器本地存储恢复状态（刷新网页才不会掉线）
  const token = ref(localStorage.getItem('token') || '')
  // 如果本地有存用户数据，就解析出来，否则就是 null
  const userInfo = ref(JSON.parse(localStorage.getItem('user') || 'null'))
  
  // 2. 弹窗控制
  const showAuthDialog = ref(false)
  const isLoginMode = ref(true) // true 登录模式，false 注册模式
  const authForm = ref({ username: '', password: '', nickname: '' })

  // 3. 登录动作
  const doLogin = async () => {
    try {
      const res = await axios.post('/user/login', authForm.value)
      
      // 注意：现在后端返回的是一个对象，里面有 token 和 user
      if (res.data && res.data.token) {
        const { token: rawToken, user } = res.data
        
        // 1. 保存手环
        token.value = rawToken
        localStorage.setItem('token', rawToken)
        
        // 2.保存后端传过来的真实用户信息 (包含数据库里的 avatar)
        userInfo.value = user
        localStorage.setItem('user', JSON.stringify(user))

        ElMessage.success('欢迎回来喵！')
        showAuthDialog.value = false
        authForm.value = { username: '', password: '', nickname: '' }
      } else {
        // 如果返回的是字符串错误信息
        ElMessage.error(typeof res.data === 'string' ? res.data : '登录失败')
      }
    } catch (err) {
      ElMessage.error('服务器连接失败...')
    }
  }

  // 4. 注册动作
  const doRegister = async () => {
    try {
      const res = await axios.post('/user/register', authForm.value)
      if (res.data === '注册成功！') {
        ElMessage.success('注册成功！快去登录吧~')
        isLoginMode.value = true // 切回登录模式
      } else {
        ElMessage.error(res.data)
      }
    } catch (err) {
      ElMessage.error('注册失败...')
    }
  }

  // 5. 退出动作
  const doLogout = () => {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    ElMessage.success('已安全退出！')
  }

  return { 
    token, userInfo, showAuthDialog, isLoginMode, authForm, 
    doLogin, doRegister, doLogout 
  }
}