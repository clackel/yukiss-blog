<template>
  <div class="shoka-container">
    <div class="big-cover" :style="{ backgroundImage: `url(${coverImg})` }">
      <div class="cover-mask">
        <h1 class="site-title">yukiss~</h1>
        <p class="site-subtitle">No Code No Life</p>
      </div>
    </div>

    <div class="main-wrapper">
      <el-row :gutter="25" class="main-content">

        <el-col :xs="24" :md="6">
          <el-card class="glass-card profile-card" shadow="hover">
            <div v-if="userInfo">
              <el-avatar :size="90" :src="mediaUrl(userInfo.avatar)" class="profile-avatar" />
              <h2 class="profile-name">{{ userInfo.nickname }}</h2>
              <p class="profile-moto">你好，异次元旅人</p>
              <el-button type="danger" plain size="small" @click="doLogout" style="margin-top: 10px;">
                <el-icon><SwitchButton /></el-icon> 退出登录
              </el-button>
            </div>
            <div v-else>
              <el-avatar :size="90" src="https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png" class="profile-avatar" style="opacity: 0.5;"/>
              <h2 class="profile-name" style="color: #999;">未登录</h2>
              <p class="profile-moto">登录后解锁全部功能</p>
              <el-button type="primary" round class="anime-btn" @click="showAuthDialog = true" style="margin-top: 10px;">
                点击登录
              </el-button>
            </div>
          </el-card>

          <el-card class="glass-card side-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <el-icon style="vertical-align: middle; margin-right: 4px;"><InfoFilled /></el-icon> 公告
              </div>
            </template>
            <div class="stat-item" style="font-size: 14px; line-height: 1.8;">欢迎访问 Yukiss Blog！开启多用户时代喵~</div>
          </el-card>
        </el-col>

        <el-col :xs="24" :md="12">
          <div class="feed-toolbar glass-card">
            <div>
              <span class="section-kicker">Timeline</span>
              <h2 class="section-title">最新投稿</h2>
              <p class="section-desc">这里只收纳你自己投递到 Yukiss 的灵感碎片。</p>
            </div>
            <el-button type="primary" round @click="handlePublishClick" class="anime-btn">
              <el-icon><EditPen /></el-icon> 写一篇
            </el-button>
          </div>

          <el-dialog
            v-model="showDialog"
            title="发布新博客"
            width="600px"
            :modal="true"
            :close-on-click-modal="false"
            class="publish-dialog"
            @close="closeDialog"
          >
            <div class="dialog-glass-bg">
              <div class="dialog-header">
                <span class="dialog-title"><el-icon style="vertical-align: middle; margin-right: 4px;"><EditPen /></el-icon>发布新博客</span>
                <span class="dialog-desc">把今天的灵感投进 Yukiss 的星轨里。</span>
              </div>
              <el-input
                v-model="newArticle.title"
                placeholder="给文章起个标题..."
                class="m-b-16 dialog-input"
                maxlength="40"
                show-word-limit
                clearable
              />
              <el-input
                v-model="newArticle.content"
                type="textarea"
                :rows="7"
                placeholder="今天有什么想记录的故事？"
                class="m-b-16 dialog-input"
                maxlength="500"
                show-word-limit
                resize="none"
                clearable
              />
              <div class="dialog-btns">
                <el-button @click="closeDialog">取消</el-button>
                <el-button type="success" @click="submitArticle">确认发布</el-button>
              </div>
            </div>
          </el-dialog>

          <template v-if="isLoading">
            <el-card v-for="index in 3" :key="index" class="glass-card article-skeleton-card" shadow="never">
              <el-skeleton animated :rows="4" />
            </el-card>
          </template>

          <el-card v-else-if="errorMessage" class="glass-card empty-feed-card" shadow="hover">
            <el-empty description="星轨暂时连接失败">
              <el-button type="primary" round class="anime-btn" @click="fetchArticles">重新加载</el-button>
            </el-empty>
          </el-card>

          <el-card v-else-if="!articles.length" class="glass-card empty-feed-card" shadow="hover">
            <el-empty description="还没有投稿，第一束灵感正在等待被点亮">
              <el-button type="primary" round class="anime-btn" @click="handlePublishClick">成为第一位投稿者</el-button>
            </el-empty>
          </el-card>

          <el-card v-else v-for="article in articles" :key="article.id" class="glass-card new-article-card" shadow="hover" @click="openArticle(article.id)">
            <div class="card-content-wrapper">
              
              <div class="main-content-left">
                <h2 class="new-post-title">{{ article.title }}</h2>
                
                <div class="new-post-meta">
                  <span class="meta-item">
                    <div class="icon-bg"><el-icon><Calendar /></el-icon></div>
                    {{ formatDate(article.createTime) }}
                  </span>
                  <span class="meta-item">
                    <div class="icon-bg"><el-icon><User /></el-icon></div>
                    {{ article.authorNickname || '神秘旅人' }}
                  </span>
                </div>
                
                <p class="new-post-excerpt">{{ article.content }}</p>
                
                <div class="new-post-tags">
                  <span class="tag-item"># 日志</span>
                  <span class="tag-item"># Yukiss</span>
                  <span class="tag-item">♥ {{ article.likeCount || 0 }}</span>
                  <span class="tag-item">评论 {{ article.commentCount || 0 }}</span>
                </div>
              </div>
              
              <div class="arrow-right-block">
                <el-icon><ArrowRight /></el-icon>
              </div>
              
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :md="6">
          <el-card class="glass-card side-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <el-icon style="vertical-align: middle; margin-right: 4px;"><DataAnalysis /></el-icon> 站点统计
              </div>
            </template>
            <div class="stat-grid">
              <div class="stat-box">
                <span>文章</span>
                <b>{{ articles.length }}</b>
              </div>
              <div class="stat-box">
                <span>运行</span>
                <b>{{ runDays }}</b>
              </div>
            </div>
            <div class="stat-item subtle-stat">最近投稿：{{ latestAuthor }}</div>
          </el-card>

          <el-card class="glass-card side-card vibe-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <el-icon style="vertical-align: middle; margin-right: 4px;"><Star /></el-icon> 创作信号
              </div>
            </template>
            <div class="vibe-list">
              <span>随笔</span>
              <span>代码笔记</span>
              <span>观测日志</span>
            </div>
          </el-card>

          <el-card class="glass-card side-card" shadow="hover">
            <el-calendar v-model="calendarDate" class="mini-calendar">
              <template #header="{ date }">
                <div class="custom-calendar-header">
                  <div class="calendar-title">{{ date }}</div>
                  <div class="calendar-arrows">
                    <span class="arrow" @click="prevMonth">&lt;</span>
                    <span class="arrow" @click="nextMonth">&gt;</span>
                  </div>
                </div>
              </template>
            </el-calendar>
          </el-card>
        </el-col>

      </el-row>
    </div>

    <el-dialog v-model="showAuthDialog" :title="isLoginMode ? '次元通行证' : '注册新账号'" width="400px" center class="auth-dialog">
      <el-input v-model="authForm.username" placeholder="请输入用户名" class="m-b-16 dialog-input">
        <template #prefix><el-icon><User /></el-icon></template>
      </el-input>
      
      <el-input v-model="authForm.password" type="password" placeholder="请输入密码" show-password class="m-b-16 dialog-input">
        <template #prefix><el-icon><Lock /></el-icon></template>
      </el-input>

      <el-input v-if="!isLoginMode" v-model="authForm.nickname" placeholder="请输入昵称 (选填)" class="m-b-16 dialog-input">
        <template #prefix><el-icon><EditPen /></el-icon></template>
      </el-input>

      <el-input v-if="!isLoginMode" v-model="authForm.email" placeholder="请输入邮箱 (用于找回账号)" class="m-b-16 dialog-input">
        <template #prefix><el-icon><Message /></el-icon></template>
      </el-input>

      <div style="text-align: center; margin-top: 20px;">
        <el-button type="primary" class="anime-btn" style="width: 100%;" :loading="authLoading" @click="isLoginMode ? doLogin() : doRegister()">
          {{ isLoginMode ? '登录' : '立即注册' }}
        </el-button>
        <div style="margin-top: 15px; font-size: 13px; color: #888;">
          {{ isLoginMode ? '还没有账号？' : '已有账号？' }}
          <span style="color: var(--theme-pink); cursor: pointer; font-weight: bold;" @click="isLoginMode = !isLoginMode">
            {{ isLoginMode ? '马上注册' : '返回登录' }}
          </span>
        </div>
      </div>
    </el-dialog>

    <footer class="footer">
      <p>© 2026 Yukiss Blog | Power by MaSparkter</p>
    </footer>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import coverImg from '../assets/cover.png'
import { useArticles } from '../composables/useArticles'
import { useForm } from '../composables/useForm'
import { formatDate, getDaysSince } from '../utils/date'
import { mediaUrl } from '../utils/media'
import { ElMessage } from 'element-plus'
import { InfoFilled, DataAnalysis, Calendar, EditPen, ArrowRight, User, Lock, SwitchButton, Star, Message } from '@element-plus/icons-vue'
import { useUser } from '../composables/useUser'

// 引入用户状态
const { 
  token, userInfo, showAuthDialog, isLoginMode, authForm, authLoading,
  doLogin, doRegister, doLogout 
} = useUser()
const router = useRouter()

// 拦截原来的投稿按钮逻辑
const handlePublishClick = () => {
  if (!token.value) {
    ElMessage.warning('请先登录后再投稿')
    showAuthDialog.value = true
    return
  }
  openDialog()
}

const calendarDate = ref(new Date())
const siteStartDate = '2024-04-01'
const { articles, isLoading, errorMessage, fetchArticles, submitArticle: submitArticleRaw } = useArticles()
const { showDialog, newArticle, resetForm } = useForm()

const runDays = computed(() => getDaysSince(siteStartDate))
const latestAuthor = computed(() => articles.value[0] ? '你自己' : '等待第一篇投稿')

const openDialog = () => { showDialog.value = true }
const closeDialog = () => { showDialog.value = false; resetForm() }

const submitArticle = async () => {
  if (!newArticle.value.title || !newArticle.value.content) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  if (newArticle.value.title.length > 40) {
    ElMessage.warning('标题不能超过 40 字')
    return
  }
  if (newArticle.value.content.length > 500) {
    ElMessage.warning('内容不能超过 500 字')
    return
  }
  await submitArticleRaw(newArticle.value, () => { resetForm(); closeDialog() })
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const prevMonth = () => {
  const d = new Date(calendarDate.value)
  d.setMonth(d.getMonth() - 1)
  calendarDate.value = d
}
const nextMonth = () => {
  const d = new Date(calendarDate.value)
  d.setMonth(d.getMonth() + 1)
  calendarDate.value = d
}

const openArticle = (id) => {
  router.push(`/articles/${id}`)
}

onMounted(() => fetchArticles({ mine: true }))
</script>

<style>
/* 1. CSS 变量主题色 */
:root {
  --theme-pink: #ff6bb1;
  --theme-bg: #f4f5f7;
  --theme-card-bg: rgba(255,255,255,0.8);
  --theme-shadow: 0 8px 20px rgba(0,0,0,0.05);
  --theme-radius: 16px;
}
html, body {
  margin: 0 !important;
  padding: 0 !important;
  width: 100%;
  background-color: var(--theme-bg);
  overflow-x: hidden;
}
#app {
  width: 100%;
  margin: 0;
  padding: 0;
}
.shoka-container {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.big-cover {
  width: 100vw;
  height: 60vh;
  background-size: cover;
  background-position: center;
  display: flex;
  justify-content: center;
  align-items: center;
  color: white;
  margin-bottom: 0;
}
.cover-mask {
  background: rgba(0, 0, 0, 0.25);
  backdrop-filter: blur(4px);
  padding: 40px 80px;
  border-radius: 20px;
  text-align: center;
}
.site-title { font-size: 3.5rem; margin: 0; text-shadow: 2px 2px 10px rgba(0,0,0,0.3); }
.main-wrapper {
  width: 100%;
  max-width: 1300px;
  padding: 0 20px;
  box-sizing: border-box;
}
.main-content {
  margin-top: -50px !important;
  position: relative;
  z-index: 100;
  width: 100%;
}
.glass-card {
  background: var(--theme-card-bg) !important;
  backdrop-filter: blur(10px);
  border-radius: var(--theme-radius) !important;
  border: 1px solid rgba(255, 255, 255, 0.5) !important;
  margin-bottom: 20px;
  box-shadow: var(--theme-shadow) !important;
}
.card-header { font-weight: bold; color: var(--theme-pink); }
.profile-card { text-align: center; padding: 20px 0; }
.profile-name { color: var(--theme-pink); margin: 15px 0 5px; }
.profile-moto { color: #888; font-size: 14px; }
.post-cover::after {
  content: '';
  position: absolute; left: 0; bottom: 0; width: 100%; height: 50%;
  background: linear-gradient(to top, rgba(0,0,0,0.6), transparent);
}
.post-title { color: white; z-index: 1; margin: 0; text-shadow: 1px 1px 5px rgba(0,0,0,0.5); }

/* ✨ 新增：作者与日期排版样式 */
.author-info { display: flex; align-items: center; gap: 8px; }
.author-name { color: var(--theme-pink); font-weight: bold; font-size: 14px; }
.post-tags span { display: inline-flex; align-items: center; margin-left: 15px; }
.tag-pink { color: var(--theme-pink); font-weight: bold; }

.post-excerpt { padding: 20px; color: #666; line-height: 1.7; }
.post-footer { padding: 0 20px 20px; text-align: right; }
.action-bar { margin-bottom: 20px; text-align: center; }
.feed-toolbar {
  padding: 22px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.section-kicker {
  color: var(--theme-pink);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0;
  text-transform: uppercase;
}
.section-title {
  margin: 5px 0 4px;
  color: #333;
  font-size: 24px;
  line-height: 1.2;
}
.section-desc {
  margin: 0;
  color: #888;
  font-size: 14px;
}
.anime-btn { background: var(--theme-pink) !important; border: none !important; box-shadow: 0 4px 12px rgba(255,107,177,0.3); }
.m-b-10 { margin-bottom: 12px; }

.footer { padding: 50px 0; text-align: center; color: #aaa; font-size: 14px; }

/* 弹窗通用玻璃态 */
.publish-dialog .el-dialog__body, .auth-dialog .el-dialog__body { padding: 0; }
.dialog-glass-bg {
  background: rgba(255,255,255,0.85);
  backdrop-filter: blur(18px) saturate(1.2);
  border-radius: 18px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.12);
  padding: 32px 32px 24px 32px;
  display: flex;
  flex-direction: column;
  align-items: stretch;
}
.dialog-header { text-align: center; margin-bottom: 18px; }
.dialog-title { font-size: 1.6rem; font-weight: bold; color: var(--theme-pink); letter-spacing: 1px; }
.dialog-desc { display: block; color: #888; font-size: 1rem; margin-top: 6px; }
.dialog-input {
  margin-bottom: 18px !important;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(255,107,177,0.08);
}
.dialog-input .el-input__wrapper, .dialog-input .el-textarea__inner {
  background: rgba(255,255,255,0.7) !important;
}

/* ====================
   极简日历样式
==================== */
.mini-calendar { background: transparent !important; padding: 0 !important; }
.mini-calendar .el-calendar__header { border-bottom: none !important; padding: 5px 0 15px 0 !important; }
.mini-calendar .el-calendar-table td, .mini-calendar .el-calendar-table tr, .mini-calendar .el-calendar-table th { border: none !important; }
.custom-calendar-header { display: flex; justify-content: space-between; align-items: center; width: 100%; }
.calendar-title { font-weight: bold; font-size: 1.1rem; color: #333; border-left: 4px solid var(--theme-pink); padding-left: 10px; line-height: 1.2; }
.calendar-arrows { display: flex; gap: 18px; font-size: 1.2rem; color: #666; font-family: monospace; padding-right: 5px; }
.calendar-arrows .arrow { cursor: pointer; transition: color 0.3s; }
.calendar-arrows .arrow:hover { color: var(--theme-pink); }
.mini-calendar .el-calendar-table th { font-weight: normal; color: #999; padding-bottom: 15px; }
.mini-calendar .el-calendar-table .el-calendar-day {
  height: 32px !important; width: 32px !important; display: flex; justify-content: center; align-items: center; padding: 0 !important; margin: 2px auto; border-radius: 8px; transition: all 0.2s ease;
}
.mini-calendar .el-calendar-table .el-calendar-day:hover { background: rgba(255, 107, 177, 0.1) !important; color: var(--theme-pink); }
.mini-calendar .el-calendar-table td.is-selected .el-calendar-day { background: #fff9fc !important; color: var(--theme-pink) !important; border: 1.5px solid var(--theme-pink); font-weight: bold; }
.mini-calendar .el-calendar-table .is-prev .el-calendar-day, .mini-calendar .el-calendar-table .is-next .el-calendar-day { color: #dcdcdc !important; }
.mini-calendar .el-calendar__body { padding-bottom: 10px; }

@media (max-width: 768px) {
  .main-content { margin-top: 20px !important; }
  .big-cover { height: 40vh; }
  .site-title { font-size: 2rem; }
  .feed-toolbar {
    align-items: stretch;
    flex-direction: column;
  }
  .feed-toolbar .el-button {
    width: 100%;
  }
}

/* ====================
   新版文章卡片样式
==================== */
.new-article-card {
  padding: 0 !important;
  border-radius: 12px !important;
  overflow: hidden; /* 保证右侧色块贴边不超出圆角 */
  margin-bottom: 20px;
}
.new-article-card .el-card__body {
  padding: 0 !important;
}

.card-content-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: stretch; /* 让右侧粉色色块自动撑满整个卡片高度 */
  min-height: 160px;
}

/* 左侧主体内容 */
.main-content-left {
  flex: 1;
  padding: 25px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

/* 标题样式：带左侧粉色竖线 */
.new-post-title {
  font-size: 1.6rem;
  font-weight: bold;
  color: #333;
  margin: 0 0 15px 0;
  position: relative;
  padding-left: 15px;
  line-height: 1.3;
}
.new-post-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 5px;
  height: 80%;
  background: var(--theme-pink);
  border-radius: 4px;
}

/* 元数据排版 (时间、作者) */
.new-post-meta {
  display: flex;
  gap: 20px;
  margin-bottom: 15px;
}
.meta-item {
  display: flex;
  align-items: center;
  font-size: 13px;
  color: #777;
  gap: 8px;
}

/* 惊艳细节：图标的淡粉色背景方块 */
.meta-item .icon-bg {
  background: rgba(255, 107, 177, 0.15);
  color: var(--theme-pink);
  width: 24px;
  height: 24px;
  display: flex;
  justify-content: center;
  align-items: center;
  border-radius: 6px;
}

/* 摘要文字，超出两行自动显示省略号 */
.new-post-excerpt {
  color: #555;
  line-height: 1.6;
  font-size: 14.5px;
  margin: 0 0 18px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 底部小标签 */
.new-post-tags {
  display: flex;
  gap: 12px;
}
.tag-item {
  background: rgba(255, 107, 177, 0.1);
  color: var(--theme-pink);
  padding: 4px 12px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: bold;
}
.article-skeleton-card {
  padding: 8px 4px;
}
.empty-feed-card {
  padding: 24px 0;
}
.empty-feed-card .el-empty {
  --el-empty-description-margin-top: 12px;
}
.stat-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}
.stat-box {
  padding: 14px 12px;
  border-radius: 12px;
  background: rgba(255, 107, 177, 0.08);
  text-align: center;
}
.stat-box span {
  display: block;
  color: #888;
  font-size: 12px;
  margin-bottom: 4px;
}
.stat-box b {
  color: var(--theme-pink);
  font-size: 24px;
}
.subtle-stat {
  color: #777;
  font-size: 13px;
}
.vibe-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.vibe-list span {
  padding: 7px 10px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.7);
  color: #666;
  font-size: 13px;
  border: 1px solid rgba(255, 107, 177, 0.16);
}

/* 右侧的粉色大箭头区域 */
.arrow-right-block {
  width: 65px;
  background: rgba(255, 107, 177, 0.1);
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 24px;
  color: var(--theme-pink);
  cursor: pointer;
  transition: all 0.3s ease;
}
.arrow-right-block:hover {
  background: var(--theme-pink);
  color: #fff;
}

@media (max-width: 768px) {
  .new-post-meta {
    flex-direction: column;
    gap: 8px;
  }
  .arrow-right-block {
    width: 48px;
  }
}
</style>
