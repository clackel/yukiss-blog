<template>
  <el-dialog
    v-model="showAuthDialog"
    :title="dialogTitle"
    width="min(420px, calc(100vw - 28px))"
    align-center
    class="auth-dialog"
    @closed="resetRedirect"
  >
    <template v-if="recoveryMode">
      <div class="auth-intro">
        <span class="section-kicker">Recover account</span>
        <p>输入绑定邮箱和验证码，即可找回登录用户名。</p>
      </div>
      <el-form @submit.prevent="recoverAccount">
        <el-form-item>
          <el-input v-model="recoveryForm.email" maxlength="120" placeholder="绑定邮箱">
            <template #prefix><el-icon><Message /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <div class="code-row">
            <el-input v-model="recoveryForm.code" maxlength="6" placeholder="6 位验证码" />
            <el-button :loading="recoveryLoading" @click="sendRecoveryCode">获取验证码</el-button>
          </div>
          <div v-if="recoveryDevCode" class="dev-code">本地验证码：{{ recoveryDevCode }}</div>
        </el-form-item>
        <el-button native-type="submit" type="primary" class="anime-btn auth-submit">
          找回用户名
        </el-button>
      </el-form>
      <button class="mode-link" type="button" @click="recoveryMode = false">返回登录</button>
    </template>

    <template v-else>
    <div class="auth-intro">
      <span class="section-kicker">{{ isLoginMode ? '账号登录' : '账号注册' }}</span>
      <p>{{ isLoginMode ? '登录后可以写文章、评论和关注作者。' : '创建账号后即可发布文章和参与讨论。' }}</p>
    </div>

    <el-form @submit.prevent="submit">
      <el-form-item>
        <el-input
          v-model="authForm.username"
          autocomplete="username"
          maxlength="20"
          placeholder="用户名"
          @keyup.enter="submit"
        >
          <template #prefix><el-icon><User /></el-icon></template>
        </el-input>
      </el-form-item>

      <el-form-item>
        <el-input
          v-model="authForm.password"
          type="password"
          :autocomplete="isLoginMode ? 'current-password' : 'new-password'"
          maxlength="72"
          placeholder="密码"
          show-password
          @keyup.enter="submit"
        >
          <template #prefix><el-icon><Lock /></el-icon></template>
        </el-input>
      </el-form-item>

      <template v-if="!isLoginMode">
        <el-form-item>
          <el-input v-model="authForm.nickname" maxlength="30" placeholder="昵称（选填）">
            <template #prefix><el-icon><EditPen /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-input v-model="authForm.email" maxlength="120" placeholder="邮箱（用于找回账号）">
            <template #prefix><el-icon><Message /></el-icon></template>
          </el-input>
        </el-form-item>
      </template>

      <el-button
        native-type="submit"
        type="primary"
        class="anime-btn auth-submit"
        :loading="authLoading"
      >
        {{ isLoginMode ? '登录' : '立即注册' }}
      </el-button>
    </el-form>

    <button class="mode-link" type="button" @click="toggleMode">
      {{ isLoginMode ? '还没有账号？现在注册' : '已有账号？返回登录' }}
    </button>
    <button v-if="isLoginMode" class="recovery-link" type="button" @click="recoveryMode = true">
      忘记用户名？
    </button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { EditPen, Lock, Message, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUser } from '../composables/useUser'
import request, { apiData } from '../utils/request'

const router = useRouter()
const {
  showAuthDialog,
  isLoginMode,
  authForm,
  authLoading,
  authRedirect,
  consumeAuthRedirect,
  doLogin,
  doRegister,
} = useUser()
const recoveryMode = ref(false)
const recoveryLoading = ref(false)
const recoveryDevCode = ref('')
const recoveryForm = reactive({ email: '', code: '' })

const dialogTitle = computed(() => {
  if (recoveryMode.value) return '找回登录账号'
  return isLoginMode.value ? '欢迎回到 Yukiss' : '加入 Yukiss'
})

const submit = async () => {
  try {
    if (isLoginMode.value) {
      await doLogin()
      const redirect = consumeAuthRedirect()
      if (redirect) {
        await router.push(redirect)
      }
      return
    }
    await doRegister()
  } catch {
    // The shared request layer already displays the server message.
  }
}

const toggleMode = () => {
  isLoginMode.value = !isLoginMode.value
}

const sendRecoveryCode = async () => {
  recoveryLoading.value = true
  try {
    const data = apiData(await request.post('/user/recover/code', { email: recoveryForm.email }))
    recoveryDevCode.value = data?.devCode || ''
    ElMessage.success(data?.message || '验证码已生成')
  } finally {
    recoveryLoading.value = false
  }
}

const recoverAccount = async () => {
  const data = apiData(await request.post('/user/recover/account', recoveryForm))
  ElMessage.success(`你的登录用户名是：${data.username}`)
  recoveryMode.value = false
  authForm.value.username = data.username
  recoveryForm.code = ''
  recoveryDevCode.value = ''
}

const resetRedirect = () => {
  recoveryMode.value = false
  if (!authRedirect.value) return
  authRedirect.value = ''
}
</script>

<style scoped>
.auth-intro {
  margin-bottom: 22px;
}

.auth-intro p {
  margin: 7px 0 0;
  color: var(--text-muted);
  line-height: 1.7;
}

.auth-submit {
  width: 100%;
  height: 42px;
}

.mode-link {
  width: 100%;
  margin-top: 16px;
  border: 0;
  background: transparent;
  color: var(--theme-pink);
  cursor: pointer;
  font-weight: 700;
}

.recovery-link {
  width: 100%;
  margin-top: 10px;
  border: 0;
  color: var(--text-faint);
  background: transparent;
  cursor: pointer;
  font-size: 13px;
}

.code-row {
  width: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
}

.dev-code {
  margin-top: 8px;
  color: var(--theme-pink);
  font-size: 12px;
}
</style>
