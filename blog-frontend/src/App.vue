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
            <template #header><div class="card-header">📢 公告</div></template>
            <p style="font-size: 14px; line-height: 1.8;">你是凑企鹅</p>
          </el-card>
        </el-col>

        <el-col :xs="24" :md="12">
          <div class="action-bar">
            <el-button type="primary" round @click="showForm = !showForm" class="anime-btn">
              {{ showForm ? '算了不写了' : '我要投稿喵' }}
            </el-button>
          </div>

          <el-collapse-transition>
            <div v-if="showForm" class="glass-card publish-form">
              <el-input v-model="newArticle.title" placeholder="给文章起个标题..." class="m-b-10" />
              <el-input v-model="newArticle.content" type="textarea" :rows="4" placeholder="今天有什么次元趣事？" class="m-b-10" />
              <div style="text-align: right;">
                <el-button type="success" @click="submitArticle">确认发布</el-button>
              </div>
            </div>
          </el-collapse-transition>

          <el-card v-for="article in articles" :key="article.id" class="glass-card article-post" shadow="hover">
            <div class="post-cover" :style="{ backgroundImage: `url(${cardBgImg})` }">
              <h2 class="post-title">{{ article.title }}</h2>
            </div>
            <div class="post-meta">
              <span>📅 {{ formatDate(article.createTime) }}</span>
              <span class="tag-pink"># 日志</span>
            </div>
            <p class="post-excerpt">{{ article.content }}</p>
            <div class="post-footer">
              <el-button text class="read-more">阅读全文 →</el-button>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :md="6">
          <el-card class="glass-card side-card" shadow="hover">
            <template #header><div class="card-header">📊 站点统计</div></template>
            <div class="stat-item">文章总数：<b>{{ articles.length }}</b></div>
            <div class="stat-item">运行天数：<b>1</b> 天</div>
          </el-card>

          <el-card class="glass-card side-card" shadow="hover">
            <el-calendar v-model="calendarDate" class="mini-calendar" />
          </el-card>
        </el-col>

      </el-row>
    </div>

    <footer class="footer">
      <p>© 2024 Yukiss Blog | Mizuki Style</p>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

// 图片路径统一使用相对路径
import coverImg from './assets/cover.png'
import avatarImg from './assets/avatar.jpg'
import cardBgImg from './assets/card-bg.jpg'

const articles = ref([])
const showForm = ref(false)
const newArticle = ref({ title: '', content: '' })
const calendarDate = ref(new Date())

const fetchArticles = async () => {
  try {
    const res = await axios.get('http://localhost:4080/articles')
    articles.value = res.data
  } catch (err) {
    ElMessage.error('无法连接到后端，请检查 4080 端口')
  }
}

onMounted(() => fetchArticles())

const submitArticle = async () => {
  if (!newArticle.value.title || !newArticle.value.content) {
    ElMessage.warning('标题和内容缺一不可哦！')
    return
  }
  try {
    await axios.post('http://localhost:4080/articles', newArticle.value)
    ElMessage.success('发布成功！')
    newArticle.value = { title: '', content: '' }
    showForm.value = false
    fetchArticles()
  } catch (err) {
    ElMessage.error('发布失败，请检查网络')
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return '未知时间'
  return new Date(dateStr).toLocaleDateString()
}
</script>

<style>
/* 1. 全局暴力重置，消除所有边距黑盒 */
html, body {
  margin: 0 !important;
  padding: 0 !important;
  width: 100%;
  background-color: #f4f5f7;
  overflow-x: hidden;
}

#app {
  width: 100%;
  margin: 0;
  padding: 0;
}

/* 2. 封面图容器：确保绝对全屏 */
.shoka-container {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center; /* 强行让所有子元素横向居中 */
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

/* 3. 主体布局：决定居中的关键 */
.main-wrapper {
  width: 100%;
  max-width: 1300px; /* 控制整个博客在大屏下的最大宽度 */
  padding: 0 20px;
  box-sizing: border-box;
}

.main-content {
  margin-top: -50px !important; /* 压在封面上的距离 */
  position: relative;
  z-index: 100;
  width: 100%;
}

/* 4. 磨砂玻璃卡片通用样式 */
.glass-card {
  background: rgba(255, 255, 255, 0.8) !important;
  backdrop-filter: blur(10px);
  border-radius: 16px !important;
  border: 1px solid rgba(255, 255, 255, 0.5) !important;
  margin-bottom: 20px;
  box-shadow: 0 8px 20px rgba(0,0,0,0.05) !important;
}

.card-header { font-weight: bold; color: #ff6bb1; }

/* 5. 侧边栏内容 */
.profile-card { text-align: center; padding: 20px 0; }
.profile-name { color: #ff6bb1; margin: 15px 0 5px; }
.profile-moto { color: #888; font-size: 14px; }

/* 6. 文章列表内容 */
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
  content: ''; position: absolute; left: 0; bottom: 0; width: 100%; height: 50%;
  background: linear-gradient(to top, rgba(0,0,0,0.6), transparent);
}
.post-title { color: white; z-index: 1; margin: 0; text-shadow: 1px 1px 5px rgba(0,0,0,0.5); }

.post-meta { padding: 12px 20px; font-size: 13px; color: #999; border-bottom: 1px solid #f0f0f0; }
.tag-pink { color: #ff6bb1; margin-left: 10px; font-weight: bold; }
.post-excerpt { padding: 20px; color: #666; line-height: 1.7; }
.post-footer { padding: 0 20px 20px; text-align: right; }

/* 7. 其他 UI 细节 */
.action-bar { margin-bottom: 20px; text-align: center; }
.anime-btn { background: #ff6bb1 !important; border: none !important; box-shadow: 0 4px 12px rgba(255,107,177,0.3); }
.m-b-10 { margin-bottom: 12px; }

.mini-calendar { background: transparent !important; border: none !important; }
.el-calendar-table .el-calendar-day { height: 40px !important; }

.footer { padding: 50px 0; text-align: center; color: #aaa; font-size: 14px; }

/* 响应式适配：手机端自动排成一列 */
@media (max-width: 768px) {
  .main-content { margin-top: 20px !important; }
  .big-cover { height: 40vh; }
  .site-title { font-size: 2rem; }
}
</style>