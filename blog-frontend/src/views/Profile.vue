<template>
  <div class="profile-page">
    
    <el-card v-if="!userInfo" class="glass-card not-login-card">
      <el-empty description="你还没有登录喵，请先在主页登录哦~" />
    </el-card>

    <el-card v-else class="glass-card profile-main-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <el-icon style="vertical-align: middle; margin-right: 5px;"><User /></el-icon> 我的次元档案
        </div>
      </template>

      <div class="profile-content">
        <div class="avatar-section">
          <el-upload
            class="avatar-uploader"
            action="/upload"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleUploadSuccess"
            :before-upload="beforeUpload"
          >
            <div class="avatar-wrapper">
              <el-avatar :size="140" :src="userInfo.avatar" />
              <div class="avatar-mask">
                <el-icon size="24"><Camera /></el-icon>
                <span style="margin-top: 8px;">更换头像</span>
              </div>
            </div>
          </el-upload>
          <p class="avatar-tip">点击头像更换 (8MB以内)</p>
        </div>

        <div class="info-section">
          <el-descriptions :column="1" border class="glass-descriptions">
            <el-descriptions-item label="登录账号">
              <b>{{ userInfo.username }}</b>
            </el-descriptions-item>
            <el-descriptions-item label="对外昵称">
              {{ userInfo.nickname }}
            </el-descriptions-item>
            <el-descriptions-item label="身份权限">
              <el-tag :type="userInfo.role === 'ADMIN' ? 'danger' : 'success'" round>
                {{ userInfo.role === 'ADMIN' ? '管理员' : '普通用户' }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </div>
        <div style="margin-top: 30px; text-align: right;">
          <el-button type="danger" @click="handleDeleteAccount">
            <el-icon><Warning /></el-icon> 永久注销账号
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed } from 'vue'
// ✨ 确保引入了 Warning 图标
import { User, Camera, Warning } from '@element-plus/icons-vue' 
import { useUser } from '../composables/useUser'
// ✨ 确保引入了 ElMessageBox 和 ElMessage
import { ElMessage, ElMessageBox } from 'element-plus' 
import axios from 'axios'
// ✨ 确保引入了路由，不然注销后没法跳回主页
import { useRouter } from 'vue-router' 

const router = useRouter()
// ✨ 确保从 useUser 里解构出了 doLogout
const { token, userInfo, doLogout } = useUser()

// ======== 1. 头像上传逻辑 ========
const uploadHeaders = computed(() => {
  return { Authorization: token.value }
})

const beforeUpload = (file) => {
  const isImg = file.type === 'image/jpeg' || file.type === 'image/png' || file.type === 'image/gif'
  const isLt2M = file.size / 1024 / 1024 < 10 // 限制 10MB

  if (!isImg) ElMessage.error('头像只能是 JPG/PNG/GIF 格式喵！')
  if (!isLt2M) ElMessage.error('头像太大了，不能超过 10MB 喵！')
  return isImg && isLt2M
}

const handleUploadSuccess = async (res) => {
  const newAvatarUrl = res 
  try {
    await axios.post('/user/updateAvatar', null, {
      params: { avatarUrl: newAvatarUrl }
    })
    userInfo.value.avatar = newAvatarUrl
    localStorage.setItem('user', JSON.stringify(userInfo.value))
    ElMessage.success('头像更换成功！美美哒~')
  } catch (error) {
    ElMessage.error('头像更新失败，请检查网络...')
  }
}

// ======== 2. 账号注销逻辑 ========
const handleDeleteAccount = () => {
  // 如果之前没引入 ElMessageBox，代码执行到这里就会静默崩溃！
  ElMessageBox.confirm(
    '此操作将永久删除您的账号及所有相关数据，是否继续？',
    '危险操作警告',
    {
      confirmButtonText: '残忍注销',
      cancelButtonText: '点错了，不退了',
      type: 'warning',
    }
  ).then(async () => {
    try {
      await axios.delete('/user/delete')
      ElMessage.success('账号已注销，感谢你的陪伴！')
      
      // 清理现场
      doLogout()
      // 踢回主页
      router.push('/')
      
    } catch (error) {
      ElMessage.error('注销失败，请检查后端是否正常启动')
    }
  }).catch(() => {
    ElMessage.info('已取消注销')
  })
}
</script>

<style>
/* 整个页面的布局，留出顶部导航栏的空间 */
.profile-page {
  padding-top: 90px; 
  display: flex;
  justify-content: center;
  min-height: 100vh;
  box-sizing: border-box;
}

.profile-main-card, .not-login-card {
  width: 100%;
  max-width: 800px;
  height: fit-content;
}

.profile-content {
  display: flex;
  gap: 50px;
  padding: 30px;
  align-items: center;
}

/* 手机端改成上下排列 */
@media (max-width: 768px) {
  .profile-content {
    flex-direction: column;
    text-align: center;
  }
}

/* ======== 头像悬浮魔法效果 ======== */
.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.avatar-wrapper {
  position: relative;
  cursor: pointer;
  border-radius: 50%;
  overflow: hidden;
  border: 5px solid white;
  box-shadow: 0 8px 20px rgba(255, 107, 177, 0.2);
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275); /* 弹性动画 */
}

.avatar-wrapper:hover {
  transform: scale(1.05) rotate(5deg);
  box-shadow: 0 12px 25px rgba(255, 107, 177, 0.4);
  border-color: var(--theme-pink);
}

.avatar-mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0,0,0,0.5);
  color: white;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.avatar-wrapper:hover .avatar-mask {
  opacity: 1;
}

.avatar-tip {
  margin-top: 15px;
  font-size: 13px;
  color: #999;
}

/* ======== 描述列表透明化 ======== */
.info-section {
  flex: 1;
  width: 100%;
}

.glass-descriptions {
  --el-descriptions-table-border: 1px solid rgba(255, 107, 177, 0.2);
  --el-descriptions-item-bordered-label-background: rgba(255, 107, 177, 0.05);
}
</style>