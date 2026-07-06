<template>
  <div class="profile-page">
    <el-card v-if="!userInfo" class="glass-card not-login-card">
      <el-empty description="登录后可以维护你的个人档案">
        <el-button type="primary" round class="anime-btn" @click="openLogin">前往登录</el-button>
      </el-empty>

      <el-divider>找回账号</el-divider>
      <el-form label-position="top" class="recovery-form">
        <el-form-item label="绑定邮箱">
          <el-input v-model="recoverForm.email" placeholder="请输入绑定过的邮箱" />
        </el-form-item>
        <el-form-item label="验证码">
          <div class="inline-control">
            <el-input v-model="recoverForm.code" placeholder="6 位验证码" />
            <el-button :loading="recoverLoading" @click="sendRecoverCode">获取验证码</el-button>
          </div>
          <div v-if="recoverDevCode" class="dev-code">开发验证码：{{ recoverDevCode }}</div>
        </el-form-item>
        <el-button type="primary" plain @click="recoverAccount">找回账号</el-button>
      </el-form>
    </el-card>

    <el-card v-else class="glass-card profile-main-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <el-icon><User /></el-icon>
          <span>账号与资料</span>
        </div>
      </template>

      <div class="profile-grid">
        <section class="avatar-section">
          <el-upload
            class="avatar-uploader"
            :action="uploadAction"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :before-upload="beforeUpload"
          >
            <div class="avatar-wrapper">
              <el-avatar :size="132" :src="mediaUrl(userInfo.avatar)" />
              <div class="avatar-mask">
                <el-icon size="24"><Camera /></el-icon>
                <span>更换头像</span>
              </div>
            </div>
          </el-upload>
          <p class="avatar-tip">支持 JPG、PNG、GIF、WebP，最大 5MB</p>
          <el-tag :type="userInfo.emailVerified ? 'success' : 'warning'" round>
            {{ userInfo.emailVerified ? '邮箱已绑定' : '邮箱未绑定' }}
          </el-tag>
        </section>

        <section class="panel-section">
          <el-tabs v-model="activeTab">
            <el-tab-pane label="基础资料" name="profile">
              <el-form label-position="top" class="profile-form">
                <el-form-item label="登录账号">
                  <el-input :model-value="userInfo.username" disabled />
                </el-form-item>
                <el-form-item label="昵称">
                  <el-input v-model="profileForm.nickname" maxlength="30" show-word-limit />
                </el-form-item>
                <el-form-item label="简介">
                  <el-input v-model="profileForm.bio" type="textarea" maxlength="300" show-word-limit />
                </el-form-item>
                <div class="two-cols">
                  <el-form-item label="性别">
                    <el-select v-model="profileForm.gender" placeholder="请选择" clearable>
                      <el-option label="保密" value="private" />
                      <el-option label="女" value="female" />
                      <el-option label="男" value="male" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="生日">
                    <el-date-picker v-model="profileForm.birthday" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
                  </el-form-item>
                </div>
                <div class="two-cols">
                  <el-form-item label="所在地">
                    <el-input v-model="profileForm.location" maxlength="80" />
                  </el-form-item>
                  <el-form-item label="个人网站">
                    <el-input v-model="profileForm.website" maxlength="180" placeholder="https://example.com" />
                  </el-form-item>
                </div>
                <el-button type="primary" :loading="profileLoading" @click="saveProfile">保存资料</el-button>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="邮箱绑定" name="email">
              <el-alert title="绑定邮箱后，可以通过邮箱找回账号。" type="info" show-icon :closable="false" />
              <el-form label-position="top" class="profile-form">
                <el-form-item label="当前邮箱">
                  <el-input :model-value="userInfo.email || '暂未绑定'" disabled />
                </el-form-item>
                <el-form-item label="新邮箱">
                  <div class="inline-control">
                    <el-input v-model="emailForm.email" placeholder="name@example.com" />
                    <el-button :loading="emailCodeLoading" @click="sendBindCode">获取验证码</el-button>
                  </div>
                  <div v-if="emailDevCode" class="dev-code">开发验证码：{{ emailDevCode }}</div>
                </el-form-item>
                <el-form-item label="验证码">
                  <el-input v-model="emailForm.code" maxlength="6" />
                </el-form-item>
                <el-button type="primary" @click="bindEmail">绑定邮箱</el-button>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="安全设置" name="security">
              <el-form label-position="top" class="profile-form">
                <el-form-item label="当前密码">
                  <el-input v-model="passwordForm.oldPassword" type="password" show-password />
                </el-form-item>
                <el-form-item label="新密码">
                  <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="至少 8 位，包含字母和数字" />
                </el-form-item>
                <el-button type="primary" @click="changePassword">更新密码</el-button>
              </el-form>

              <el-divider />
              <el-alert title="注销后账号将无法登录，邮箱会解除绑定。此操作需要再次输入密码确认。" type="warning" show-icon :closable="false" />
              <el-button type="danger" class="delete-btn" @click="openDeleteDialog">
                <el-icon><Warning /></el-icon>
                注销账号
              </el-button>
            </el-tab-pane>
          </el-tabs>
        </section>
      </div>
    </el-card>

    <el-dialog v-model="deleteDialogVisible" title="确认注销账号" width="420px">
      <p class="danger-copy">注销后，你将退出登录；账号、邮箱绑定和后续登录能力会被停用。</p>
      <el-input v-model="deleteForm.password" type="password" show-password placeholder="请输入当前密码确认" />
      <template #footer>
        <el-button @click="deleteDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="deleteAccount">确认注销</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Camera, User, Warning } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request, { API_BASE_URL, apiData } from '../utils/request'
import { mediaUrl } from '../utils/media'
import { useUser } from '../composables/useUser'

const router = useRouter()
const { token, userInfo, showAuthDialog, saveUser, clearSession, refreshMe } = useUser()

const activeTab = ref('profile')
const profileLoading = ref(false)
const emailCodeLoading = ref(false)
const recoverLoading = ref(false)
const deleteDialogVisible = ref(false)
const emailDevCode = ref('')
const recoverDevCode = ref('')

const profileForm = reactive({
  nickname: '',
  bio: '',
  gender: '',
  birthday: '',
  location: '',
  website: '',
})
const emailForm = reactive({ email: '', code: '' })
const passwordForm = reactive({ oldPassword: '', newPassword: '' })
const deleteForm = reactive({ password: '' })
const recoverForm = reactive({ email: '', code: '' })

const uploadHeaders = computed(() => ({ Authorization: token.value }))
const uploadAction = `${API_BASE_URL}/upload`

function syncProfileForm() {
  if (!userInfo.value) return
  Object.assign(profileForm, {
    nickname: userInfo.value.nickname || '',
    bio: userInfo.value.bio || '',
    gender: userInfo.value.gender || '',
    birthday: userInfo.value.birthday ? String(userInfo.value.birthday).slice(0, 10) : '',
    location: userInfo.value.location || '',
    website: userInfo.value.website || '',
  })
  emailForm.email = userInfo.value.email || ''
}

watch(userInfo, syncProfileForm, { immediate: true })
onMounted(() => refreshMe().catch(() => {}))

async function saveProfile() {
  profileLoading.value = true
  try {
    const user = apiData(await request.put('/user/profile', profileForm))
    saveUser(user)
    ElMessage.success('资料已保存')
  } finally {
    profileLoading.value = false
  }
}

async function sendBindCode() {
  emailCodeLoading.value = true
  try {
    const data = apiData(await request.post('/user/email/code', { email: emailForm.email }))
    emailDevCode.value = data?.devCode || ''
    ElMessage.success(data?.message || '验证码已发送')
  } finally {
    emailCodeLoading.value = false
  }
}

async function bindEmail() {
  const user = apiData(await request.post('/user/email/bind', emailForm))
  saveUser(user)
  emailForm.code = ''
  emailDevCode.value = ''
  ElMessage.success('邮箱绑定成功')
}

async function changePassword() {
  await request.post('/user/change-password', passwordForm)
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  ElMessage.success('密码已更新')
}

function beforeUpload(file) {
  const allowed = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
  if (!allowed.includes(file.type)) {
    ElMessage.error('头像仅支持 JPG、PNG、GIF 或 WebP')
    return false
  }
  if (file.size / 1024 / 1024 > 5) {
    ElMessage.error('头像不能超过 5MB')
    return false
  }
  return true
}

async function handleUploadSuccess(res) {
  const url = res?.data?.url
  if (!url) {
    ElMessage.error(res?.message || '头像上传失败')
    return
  }
  const user = apiData(await request.post('/user/updateAvatar', null, { params: { avatarUrl: url } }))
  saveUser(user)
  ElMessage.success('头像已更新')
}

function handleUploadError() {
  ElMessage.error('头像上传失败，请稍后再试')
}

function openDeleteDialog() {
  deleteForm.password = ''
  deleteDialogVisible.value = true
}

async function deleteAccount() {
  await ElMessageBox.confirm('这是不可恢复操作，确认注销当前账号吗？', '再次确认', { type: 'warning' })
  await request.delete('/user/delete', { data: { password: deleteForm.password } })
  clearSession()
  deleteDialogVisible.value = false
  ElMessage.success('账号已注销')
  router.push('/')
}

async function sendRecoverCode() {
  recoverLoading.value = true
  try {
    const data = apiData(await request.post('/user/recover/code', { email: recoverForm.email }))
    recoverDevCode.value = data?.devCode || ''
    ElMessage.success(data?.message || '验证码已发送')
  } finally {
    recoverLoading.value = false
  }
}

async function recoverAccount() {
  const data = apiData(await request.post('/user/recover/account', recoverForm))
  ElMessage.success(`你的登录账号是：${data.username}`)
}

function openLogin() {
  showAuthDialog.value = true
  router.push('/')
}
</script>

<style>
.profile-page {
  padding: 90px 20px 40px;
  min-height: 100vh;
  box-sizing: border-box;
  display: flex;
  justify-content: center;
}

.profile-main-card,
.not-login-card {
  width: 100%;
  max-width: 980px;
  height: fit-content;
}

.not-login-card {
  max-width: 760px;
}

.not-login-card .el-card__body {
  padding: 34px 42px;
}

.not-login-card .el-empty {
  padding: 12px 0 24px;
}

.not-login-card .el-empty__image {
  width: 128px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.profile-grid {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 32px;
  padding: 18px;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
}

.avatar-wrapper {
  position: relative;
  cursor: pointer;
  border-radius: 50%;
  overflow: hidden;
  border: 5px solid white;
  box-shadow: 0 8px 20px rgba(255, 107, 177, 0.2);
}

.avatar-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.52);
  color: white;
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}

.avatar-wrapper:hover .avatar-mask {
  opacity: 1;
}

.avatar-tip,
.dev-code,
.danger-copy {
  color: #888;
  font-size: 13px;
}

.panel-section {
  min-width: 0;
}

.profile-form {
  max-width: 620px;
}

.two-cols {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.inline-control {
  width: 100%;
  display: flex;
  gap: 10px;
}

.inline-control .el-input {
  flex: 1;
}

.delete-btn {
  margin-top: 18px;
}

.recovery-form {
  max-width: 520px;
  margin: 0 auto;
}

@media (max-width: 768px) {
  .profile-grid,
  .two-cols {
    grid-template-columns: 1fr;
  }

  .inline-control {
    flex-direction: column;
  }
}
</style>
