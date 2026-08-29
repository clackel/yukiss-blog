<template>
  <div class="landing-page">
    <section class="landing-hero" :style="{ backgroundImage: `url(${coverImg})` }">
      <div class="landing-mask">
        <div class="landing-copy">
          <span class="section-kicker">Yukiss Blog</span>
          <h1>记录灵感，也遇见同频的人</h1>
          <p>写下你的日常、代码笔记和闪念，把它们收进一个安静又明亮的个人空间。</p>
          <div class="landing-actions">
            <el-button type="primary" round class="anime-btn" @click="showLogin">
              <el-icon><User /></el-icon>
              登录
            </el-button>
            <el-button round class="ghost-btn" @click="showRegister">注册</el-button>
          </div>
        </div>
      </div>
    </section>

    <section class="landing-preview">
      <div class="preview-item">
        <b>写作</b>
        <span>沉淀自己的文章和碎片</span>
      </div>
      <div class="preview-item">
        <b>社区</b>
        <span>浏览公开投稿和灵感</span>
      </div>
      <div class="preview-item">
        <b>资料</b>
        <span>维护个人头像与主页信息</span>
      </div>
    </section>

    <el-dialog v-model="showAuthDialog" :title="isLoginMode ? '登录 Yukiss' : '注册 Yukiss'" width="400px" center class="auth-dialog">
      <el-input v-model="authForm.username" placeholder="请输入用户名" class="m-b-16 dialog-input">
        <template #prefix><el-icon><User /></el-icon></template>
      </el-input>

      <el-input v-model="authForm.password" type="password" placeholder="请输入密码" show-password class="m-b-16 dialog-input">
        <template #prefix><el-icon><Lock /></el-icon></template>
      </el-input>

      <el-input v-if="!isLoginMode" v-model="authForm.nickname" placeholder="请输入昵称（选填）" class="m-b-16 dialog-input">
        <template #prefix><el-icon><EditPen /></el-icon></template>
      </el-input>

      <el-input v-if="!isLoginMode" v-model="authForm.email" placeholder="请输入邮箱（用于找回账号）" class="m-b-16 dialog-input">
        <template #prefix><el-icon><Message /></el-icon></template>
      </el-input>

      <div class="auth-actions">
        <el-button type="primary" class="anime-btn" :loading="authLoading" @click="isLoginMode ? doLogin() : doRegister()">
          {{ isLoginMode ? '登录' : '立即注册' }}
        </el-button>
        <button class="mode-link" type="button" @click="isLoginMode = !isLoginMode">
          {{ isLoginMode ? '还没有账号？马上注册' : '已有账号？返回登录' }}
        </button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { watch } from 'vue'
import { useRouter } from 'vue-router'
import { EditPen, Lock, Message, User } from '@element-plus/icons-vue'
import coverImg from '../assets/cover.png'
import { useUser } from '../composables/useUser'

const router = useRouter()
const {
  token,
  showAuthDialog,
  isLoginMode,
  authForm,
  authLoading,
  doLogin,
  doRegister,
} = useUser()

const showLogin = () => {
  isLoginMode.value = true
  showAuthDialog.value = true
}

const showRegister = () => {
  isLoginMode.value = false
  showAuthDialog.value = true
}

watch(token, (value) => {
  if (value) {
    router.push('/home')
  }
}, { immediate: true })
</script>

<style scoped>
.landing-page {
  min-height: 100vh;
  padding-top: 60px;
  background: #f4f5f7;
}
.landing-hero {
  min-height: calc(100vh - 190px);
  background-size: cover;
  background-position: center;
}
.landing-mask {
  min-height: calc(100vh - 190px);
  padding: 72px 24px;
  box-sizing: border-box;
  background: linear-gradient(90deg, rgba(29, 27, 35, 0.74), rgba(29, 27, 35, 0.26));
  display: flex;
  align-items: center;
}
.landing-copy {
  width: min(680px, 100%);
  margin-left: max(24px, calc((100vw - 1180px) / 2));
  color: #fff;
}
.section-kicker {
  color: #ff9bc9;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0;
  text-transform: uppercase;
}
.landing-copy h1 {
  margin: 12px 0 16px;
  font-size: 46px;
  line-height: 1.18;
  letter-spacing: 0;
}
.landing-copy p {
  max-width: 560px;
  margin: 0;
  color: rgba(255, 255, 255, 0.88);
  font-size: 17px;
  line-height: 1.8;
}
.landing-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 28px;
}
.anime-btn {
  background: var(--theme-pink) !important;
  border: none !important;
  box-shadow: 0 4px 12px rgba(255, 107, 177, 0.3);
}
.ghost-btn {
  color: #fff !important;
  border-color: rgba(255, 255, 255, 0.62) !important;
  background: rgba(255, 255, 255, 0.08) !important;
}
.landing-preview {
  max-width: 1180px;
  margin: -34px auto 0;
  padding: 0 24px 54px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  position: relative;
  z-index: 2;
}
.preview-item {
  min-height: 92px;
  padding: 20px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(255, 255, 255, 0.62);
  box-shadow: var(--theme-shadow);
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
}
.preview-item b {
  color: var(--theme-pink);
  font-size: 18px;
}
.preview-item span {
  color: #666;
  font-size: 14px;
}
.auth-actions {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 18px;
}
.dialog-input {
  margin-bottom: 18px !important;
}
.auth-actions .el-button {
  width: 100%;
}
.mode-link {
  border: 0;
  background: transparent;
  color: var(--theme-pink);
  cursor: pointer;
  font-weight: 700;
}

@media (max-width: 720px) {
  .landing-copy {
    margin-left: 0;
  }
  .landing-copy h1 {
    font-size: 34px;
  }
  .landing-preview {
    grid-template-columns: 1fr;
  }
}
</style>
