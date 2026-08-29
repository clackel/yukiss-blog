<template>
  <main class="page-shell home-page">
    <section class="home-hero glass-card">
      <div class="home-identity">
        <el-avatar :size="72" :src="mediaUrl(userInfo?.avatar)">
          {{ userInitial }}
        </el-avatar>
        <div>
          <span class="section-kicker">个人工作台</span>
          <h1>{{ greeting }}，{{ userInfo?.nickname || userInfo?.username }}</h1>
          <p>这里收纳你写下的每一篇文章，也记录朋友们留下的回应。</p>
        </div>
      </div>
      <el-button type="primary" size="large" round class="anime-btn" @click="router.push('/editor')">
        <el-icon><EditPen /></el-icon>
        写一篇
      </el-button>
    </section>

    <section class="dashboard-grid">
      <aside class="dashboard-aside">
        <div class="glass-card stats-card">
          <span class="section-kicker">数据概览</span>
          <h2>创作概览</h2>
          <div class="stats-grid">
            <div>
              <b>{{ articles.length }}</b>
              <span>文章</span>
            </div>
            <div>
              <b>{{ totalLikes }}</b>
              <span>获赞</span>
            </div>
            <div>
              <b>{{ totalComments }}</b>
              <span>评论</span>
            </div>
          </div>
        </div>

        <div class="glass-card prompt-card">
          <span class="section-kicker">写作建议</span>
          <h3>下一篇可以写</h3>
          <p>{{ writingPrompt }}</p>
        </div>
      </aside>

      <section class="article-column">
        <div class="feed-toolbar glass-card">
          <div>
            <span class="section-kicker">文章管理</span>
            <h2 class="section-title">我的文章</h2>
            <p class="section-desc">编辑、整理，或者回到某篇文章继续对话。</p>
          </div>
          <el-button plain round :loading="isLoading" @click="loadArticles">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>

        <div v-if="isLoading" class="article-list">
          <el-skeleton v-for="index in 3" :key="index" animated :rows="5" class="glass-card loading-card" />
        </div>

        <el-empty v-else-if="errorMessage" class="glass-card empty-state" description="暂时无法加载你的文章">
          <el-button type="primary" plain @click="loadArticles">重新加载</el-button>
        </el-empty>

        <el-empty v-else-if="!articles.length" class="glass-card empty-state" description="还没有文章，写下第一篇吧">
          <el-button type="primary" class="anime-btn" @click="router.push('/editor')">开始写作</el-button>
        </el-empty>

        <div v-else class="article-list">
          <ArticleCard
            v-for="article in articles"
            :key="article.id"
            :article="article"
            :show-author="false"
            manageable
            @open="openArticle"
            @edit="editArticle"
            @delete="deleteArticle"
          />
        </div>
      </section>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { EditPen, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import ArticleCard from '../components/ArticleCard.vue'
import { useUser } from '../composables/useUser'
import request, { apiData } from '../utils/request'
import { mediaUrl } from '../utils/media'

const router = useRouter()
const { userInfo } = useUser()
const articles = ref([])
const isLoading = ref(false)
const errorMessage = ref('')

const prompts = [
  '记录一件最近让你改变看法的小事。',
  '写下一个刚解决的问题，以及真正有效的思路。',
  '给未来的自己留一封只有三段话的信。',
  '分享一部最近喜欢的作品，以及它打动你的地方。',
]

const userInitial = computed(() => (
  userInfo.value?.nickname || userInfo.value?.username || '我'
).slice(0, 1))

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 11) return '早上好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const writingPrompt = prompts[new Date().getDate() % prompts.length]
const totalLikes = computed(() => articles.value.reduce((sum, article) => sum + (article.likeCount || 0), 0))
const totalComments = computed(() => articles.value.reduce((sum, article) => sum + (article.commentCount || 0), 0))

const loadArticles = async () => {
  isLoading.value = true
  errorMessage.value = ''
  try {
    articles.value = apiData(await request.get('/articles/mine')) || []
  } catch {
    errorMessage.value = '文章加载失败'
  } finally {
    isLoading.value = false
  }
}

const openArticle = (id) => router.push(`/articles/${id}`)
const editArticle = (id) => router.push(`/editor/${id}`)

const deleteArticle = async (article) => {
  try {
    await ElMessageBox.confirm(
      `确认删除《${article.title}》吗？文章下的评论和点赞也会一并删除。`,
      '删除文章',
      {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
      }
    )
    await request.delete(`/articles/${article.id}`)
    articles.value = articles.value.filter(item => item.id !== article.id)
    ElMessage.success('文章已删除')
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
  }
}

onMounted(loadArticles)
</script>

<style scoped>
.home-hero {
  margin-bottom: 24px;
  padding: 30px 34px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  overflow: hidden;
  position: relative;
}

.home-hero::after {
  content: '';
  width: 220px;
  height: 220px;
  position: absolute;
  right: -70px;
  bottom: -110px;
  border-radius: 50%;
  background: radial-gradient(circle, var(--theme-glow), transparent 68%);
  pointer-events: none;
}

.home-identity {
  display: flex;
  align-items: center;
  gap: 20px;
}

.home-identity h1 {
  margin: 7px 0 5px;
  color: var(--text-strong);
  font-size: clamp(27px, 4vw, 38px);
}

.home-identity p {
  margin: 0;
  color: var(--text-muted);
  line-height: 1.7;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: 290px minmax(0, 1fr);
  gap: 24px;
}

.dashboard-aside {
  display: grid;
  align-content: start;
  gap: 18px;
}

.stats-card,
.prompt-card {
  padding: 24px;
}

.stats-card h2,
.prompt-card h3 {
  margin: 8px 0 18px;
  color: var(--text-strong);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.stats-grid div {
  padding: 14px 6px;
  border-radius: 12px;
  text-align: center;
  background: var(--accent-soft);
}

.stats-grid b,
.stats-grid span {
  display: block;
}

.stats-grid b {
  color: var(--theme-pink);
  font-size: 22px;
}

.stats-grid span {
  margin-top: 3px;
  color: var(--text-faint);
  font-size: 11px;
}

.prompt-card p {
  margin: 0;
  color: var(--text-muted);
  line-height: 1.8;
}

.article-column {
  min-width: 0;
}

.feed-toolbar {
  margin-bottom: 18px;
}

.article-list {
  display: grid;
  gap: 16px;
}

.loading-card {
  padding: 24px;
}

@media (max-width: 900px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .dashboard-aside {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .home-hero,
  .home-identity {
    align-items: flex-start;
    flex-direction: column;
  }

  .home-hero {
    padding: 25px;
  }

  .home-hero > .el-button {
    width: 100%;
  }

  .dashboard-aside {
    grid-template-columns: 1fr;
  }
}
</style>
