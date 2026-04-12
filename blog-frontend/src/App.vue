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
            <el-avatar :size="90" :src="avatarImg" class="profile-avatar" />
            <h2 class="profile-name">MaSparkter</h2>
            <p class="profile-moto">何意味</p>
            <div class="social-icons">
              <span>GitHub</span> | <span>Twitter</span>
            </div>
          </el-card>

          <el-card class="glass-card side-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <el-icon style="vertical-align: middle; margin-right: 4px;"><InfoFilled /></el-icon> 公告
              </div>
            </template>
            <div class="stat-item" style="font-size: 14px; line-height: 1.8;">欢迎访问 Yukiss Blog！有问题请联系站长。</div>
          </el-card>
        </el-col>

        <el-col :xs="24" :md="12">
          <div class="action-bar">
            <el-button type="primary" round @click="openDialog" class="anime-btn">
              我要投稿喵
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
                <span class="dialog-desc">分享你的灵感与故事吧！</span>
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
                placeholder="今天有什么次元趣事？"
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

          <el-card v-for="article in articles" :key="article.id" class="glass-card article-post" shadow="hover">
            <div class="post-cover" :style="{ backgroundImage: `url(${cardBgImg})` }">
              <h2 class="post-title">{{ article.title }}</h2>
            </div>
            <div class="post-meta">
              <span><el-icon style="vertical-align: middle; margin-right: 2px;"><Calendar /></el-icon>{{ formatDate(article.createTime) }}</span>
              <span class="tag-pink"><el-icon style="vertical-align: middle; margin-right: 2px;"><EditPen /></el-icon>日志</span>
            </div>
            <p class="post-excerpt">{{ article.content }}</p>
            <div class="post-footer">
              <el-button text class="read-more">阅读全文<el-icon style="vertical-align: middle; margin-left: 2px;"><ArrowRight /></el-icon></el-button>
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
            <div class="stat-item">文章总数：<b>{{ articles.length }}</b></div>
            <div class="stat-item">运行天数：<b>1</b> 天</div>
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

    <footer class="footer">
      <p>© 2026 Yukiss Blog | Power by MaSparkter</p>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import coverImg from './assets/cover.png'
import avatarImg from './assets/avatar.jpg'
import cardBgImg from './assets/card-bg.jpg'
import { useArticles } from './composables/useArticles'
import { useForm } from './composables/useForm'
import { formatDate, getDaysSince } from './utils/date'
import { ElMessage, ElDialog } from 'element-plus'
import { InfoFilled, DataAnalysis, Calendar, EditPen, ArrowRight } from '@element-plus/icons-vue'

const calendarDate = ref(new Date())
const siteStartDate = '2024-04-01' // 建站日期

// 文章相关逻辑
const { articles, fetchArticles, submitArticle: submitArticleRaw } = useArticles()
// 表单相关逻辑
const { showDialog, newArticle, resetForm } = useForm()

const runDays = computed(() => getDaysSince(siteStartDate))

const openDialog = () => { showDialog.value = true }
const closeDialog = () => { showDialog.value = false; resetForm() }

const submitArticle = async () => {
  if (!newArticle.value.title || !newArticle.value.content) {
    ElMessage.warning('标题和内容缺一不可哦！')
    return
  }
  if (newArticle.value.title.length > 40) {
    ElMessage.warning('标题不能超过40字！')
    return
  }
  if (newArticle.value.content.length > 500) {
    ElMessage.warning('内容不能超过500字！')
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

onMounted(() => fetchArticles())
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
.article-post { overflow: hidden; padding: 0 !important; }
.post-cover {
  height: 240px;
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: flex-end;
  padding: 20px;
  position: relative;
}
.post-cover::after {
  content: '';
  position: absolute; left: 0; bottom: 0; width: 100%; height: 50%;
  background: linear-gradient(to top, rgba(0,0,0,0.6), transparent);
}
.post-title { color: white; z-index: 1; margin: 0; text-shadow: 1px 1px 5px rgba(0,0,0,0.5); }
.post-meta { padding: 12px 20px; font-size: 13px; color: #999; border-bottom: 1px solid #f0f0f0; }
.tag-pink { color: var(--theme-pink); margin-left: 10px; font-weight: bold; }
.post-excerpt { padding: 20px; color: #666; line-height: 1.7; }
.post-footer { padding: 0 20px 20px; text-align: right; }
.action-bar { margin-bottom: 20px; text-align: center; }
.anime-btn { background: var(--theme-pink) !important; border: none !important; box-shadow: 0 4px 12px rgba(255,107,177,0.3); }
.m-b-10 { margin-bottom: 12px; }
.mini-calendar { background: transparent !important; border: none !important; }
.el-calendar-table .el-calendar-day { height: 40px !important; }
.footer { padding: 50px 0; text-align: center; color: #aaa; font-size: 14px; }
@media (max-width: 768px) {
  .main-content { margin-top: 20px !important; }
  .big-cover { height: 40vh; }
  .site-title { font-size: 2rem; }
}

.publish-dialog .el-dialog__body {
  padding: 0;
}
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
.dialog-header {
  text-align: center;
  margin-bottom: 18px;
}
.dialog-title {
  font-size: 1.6rem;
  font-weight: bold;
  color: var(--theme-pink);
  letter-spacing: 1px;
}
.dialog-desc {
  display: block;
  color: #888;
  font-size: 1rem;
  margin-top: 6px;
}
.dialog-input {
  margin-bottom: 18px !important;
  border-radius: 10px;
  background: rgba(255,255,255,0.7);
  box-shadow: 0 2px 8px rgba(255,107,177,0.08);
}
.dialog-btns {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
}
@media (max-width: 600px) {
  .publish-dialog .el-dialog {
    width: 98vw !important;
    min-width: unset;
  }
  .dialog-glass-bg {
    padding: 16px 6px 12px 6px;
  }
  .dialog-title { font-size: 1.1rem; }
}

/* ====================
   极简日历样式
==================== */
.mini-calendar {
  background: transparent !important;
  padding: 0 !important;
}

/* 隐藏日历自带的各种表格边框 */
.mini-calendar .el-calendar__header {
  border-bottom: none !important;
  padding: 5px 0 15px 0 !important;
}
.mini-calendar .el-calendar-table td,
.mini-calendar .el-calendar-table tr,
.mini-calendar .el-calendar-table th {
  border: none !important; 
}

/* 自定义头部排版 */
.custom-calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.calendar-title {
  font-weight: bold;
  font-size: 1.1rem;
  color: #333;
  /* 左侧粉色小竖线 */
  border-left: 4px solid var(--theme-pink);
  padding-left: 10px;
  line-height: 1.2;
}

.calendar-arrows {
  display: flex;
  gap: 18px;
  font-size: 1.2rem;
  color: #666;
  font-family: monospace; /* 让箭头看起来更纯粹 */
  padding-right: 5px;
}

.calendar-arrows .arrow {
  cursor: pointer;
  transition: color 0.3s;
}

.calendar-arrows .arrow:hover {
  color: var(--theme-pink);
}

/* 星期表头颜色淡化 */
.mini-calendar .el-calendar-table th {
  font-weight: normal;
  color: #999;
  padding-bottom: 15px;
}

/* 日期数字方块的基础样式 */
.mini-calendar .el-calendar-table .el-calendar-day {
  height: 32px !important;
  width: 32px !important;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 0 !important;
  margin: 2px auto; /* 居中显示 */
  border-radius: 8px; /* 圆角矩形 */
  transition: all 0.2s ease;
}

/* 鼠标悬浮的淡淡粉色背景 */
.mini-calendar .el-calendar-table .el-calendar-day:hover {
  background: rgba(255, 107, 177, 0.1) !important;
  color: var(--theme-pink);
}

/* 选中/今天 日期的框框样式 (核心复刻点) */
.mini-calendar .el-calendar-table td.is-selected .el-calendar-day {
  background: #fff9fc !important; /* 非常淡的粉底 */
  color: var(--theme-pink) !important;
  border: 1.5px solid var(--theme-pink);
  font-weight: bold;
}

/* 不是当前月份的日期置灰 */
.mini-calendar .el-calendar-table .is-prev .el-calendar-day,
.mini-calendar .el-calendar-table .is-next .el-calendar-day {
  color: #dcdcdc !important;
}
.mini-calendar .el-calendar__body {
  padding-bottom: 10px;
}
</style>