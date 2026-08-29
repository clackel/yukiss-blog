<template>
  <main class="landing-page">
    <section class="landing-hero" :style="{ backgroundImage: `url(${coverImg})` }">
      <div class="landing-mask">
        <div class="landing-copy">
          <span class="section-kicker">Yukiss Blog</span>
          <h1>写作、阅读，<br>分享你的观点</h1>
          <p>一个用于发布长文和交流想法的博客社区。无需登录即可阅读，登录后可以写文章、点赞和评论。</p>
          <div class="landing-actions">
            <el-button type="primary" round size="large" class="anime-btn" @click="router.push('/community')">
              浏览社区
              <el-icon><ArrowRight /></el-icon>
            </el-button>
            <el-button v-if="token" round size="large" class="hero-ghost" @click="router.push('/editor')">
              写一篇
            </el-button>
            <el-button v-else round size="large" class="hero-ghost" @click="openAuth('register')">
              注册账号
            </el-button>
          </div>
        </div>

        <div class="landing-orbit" aria-hidden="true">
          <div class="orbit-card orbit-card--one">
            <span>开始写作</span>
            <b>记录值得保留的内容</b>
          </div>
          <div class="orbit-card orbit-card--two">
            <span>Markdown</span>
            <b>让表达保持清爽</b>
          </div>
        </div>
      </div>
    </section>

    <section class="landing-features" aria-label="Yukiss 特色">
      <div>
        <span>01</span>
        <b>安静写作</b>
        <p>用 Markdown 写下长文、代码和日常片段。</p>
      </div>
      <div>
        <span>02</span>
        <b>朋友社区</b>
        <p>公开阅读，登录后点赞、评论和回复。</p>
      </div>
      <div>
        <span>03</span>
        <b>保持简单</b>
        <p>没有复杂推荐算法，只按时间与真实互动发现内容。</p>
      </div>
    </section>

    <section class="latest-section">
      <div class="latest-heading">
        <div>
          <span class="section-kicker">最新文章</span>
          <h2 class="section-title">最近发布</h2>
          <p class="section-desc">查看社区最近更新的文章。</p>
        </div>
        <el-button text class="more-link" @click="router.push('/community')">
          查看全部 <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>

      <div v-if="isLoading" class="latest-grid">
        <el-skeleton v-for="index in 3" :key="index" animated :rows="5" class="glass-card skeleton-card" />
      </div>

      <el-empty v-else-if="errorMessage" class="glass-card empty-state" description="暂时无法加载文章">
        <el-button type="primary" plain @click="loadLatest">重新加载</el-button>
      </el-empty>

      <el-empty v-else-if="!latestArticles.length" class="glass-card empty-state" description="还没有文章">
        <el-button v-if="token" type="primary" class="anime-btn" @click="router.push('/editor')">写第一篇</el-button>
        <el-button v-else type="primary" class="anime-btn" @click="openAuth('register', '/editor')">注册并开始写作</el-button>
      </el-empty>

      <div v-else class="latest-grid">
        <ArticleCard
          v-for="article in latestArticles"
          :key="article.id"
          :article="article"
          @open="openArticle"
          @open-author="openAuthor"
        />
      </div>
    </section>

    <footer class="landing-footer">
      <b>Yukiss</b>
      <span>记录与分享。</span>
    </footer>
  </main>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ArrowRight } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import ArticleCard from '../components/ArticleCard.vue'
import { useUser } from '../composables/useUser'
import request, { apiData } from '../utils/request'
import coverImg from '../assets/cover.png'

const router = useRouter()
const { token, openAuth } = useUser()
const latestArticles = ref([])
const isLoading = ref(false)
const errorMessage = ref('')

const loadLatest = async () => {
  isLoading.value = true
  errorMessage.value = ''
  try {
    const data = apiData(await request.get('/articles/page', {
      params: { page: 1, pageSize: 3, sort: 'published' },
    }))
    latestArticles.value = data?.items || []
  } catch {
    errorMessage.value = '文章加载失败'
  } finally {
    isLoading.value = false
  }
}

const openArticle = (id) => {
  router.push(`/articles/${id}`)
}

const openAuthor = (id) => {
  if (id) router.push(`/users/${id}`)
}

onMounted(loadLatest)
</script>

<style scoped>
.landing-page {
  min-height: 100vh;
  padding-top: var(--nav-height);
}

.landing-hero {
  min-height: min(720px, calc(100vh - var(--nav-height)));
  background-size: cover;
  background-position: center;
}

.landing-mask {
  min-height: inherit;
  padding: 72px max(24px, calc((100vw - 1180px) / 2));
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(280px, 0.75fr);
  align-items: center;
  gap: 56px;
  background:
    linear-gradient(90deg, rgba(25, 21, 30, 0.82), rgba(25, 21, 30, 0.4)),
    linear-gradient(180deg, transparent 70%, rgba(25, 21, 30, 0.25));
}

.landing-copy {
  max-width: 720px;
  color: white;
}

.landing-copy h1 {
  margin: 16px 0 20px;
  font-size: clamp(46px, 7vw, 78px);
  line-height: 1.08;
  letter-spacing: -0.04em;
}

.landing-copy p {
  max-width: 610px;
  margin: 0;
  color: rgba(255, 255, 255, 0.84);
  font-size: clamp(16px, 2vw, 19px);
  line-height: 1.85;
}

.landing-actions {
  margin-top: 32px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.hero-ghost {
  color: white !important;
  border-color: rgba(255, 255, 255, 0.55) !important;
  background: rgba(255, 255, 255, 0.08) !important;
}

.landing-orbit {
  position: relative;
  height: 360px;
}

.landing-orbit::before,
.landing-orbit::after {
  content: '';
  position: absolute;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 50%;
}

.landing-orbit::before {
  inset: 25px 15px;
}

.landing-orbit::after {
  inset: 72px 58px;
}

.orbit-card {
  position: absolute;
  z-index: 2;
  width: 220px;
  padding: 20px;
  border: 1px solid rgba(255, 255, 255, 0.26);
  border-radius: 18px;
  color: white;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(16px);
  box-shadow: 0 18px 44px rgba(0, 0, 0, 0.2);
}

.orbit-card span,
.orbit-card b {
  display: block;
}

.orbit-card span {
  margin-bottom: 7px;
  color: #ffadd1;
  font-size: 12px;
  font-weight: 800;
}

.orbit-card--one {
  top: 56px;
  left: 0;
}

.orbit-card--two {
  right: 0;
  bottom: 54px;
}

.landing-features {
  width: min(1120px, calc(100% - 40px));
  margin: -54px auto 0;
  position: relative;
  z-index: 2;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  border: 1px solid var(--border-soft);
  border-radius: 22px;
  overflow: hidden;
  background: var(--surface-raised);
  box-shadow: var(--theme-shadow);
  backdrop-filter: blur(18px);
}

.landing-features > div {
  padding: 28px;
}

.landing-features > div + div {
  border-left: 1px solid var(--border-soft);
}

.landing-features span {
  color: var(--theme-pink);
  font-size: 12px;
  font-weight: 900;
}

.landing-features b {
  display: block;
  margin: 8px 0;
  color: var(--text-strong);
  font-size: 19px;
}

.landing-features p {
  margin: 0;
  color: var(--text-muted);
  line-height: 1.65;
}

.latest-section {
  width: min(1180px, calc(100% - 40px));
  margin: 0 auto;
  padding: 92px 0 70px;
}

.latest-heading {
  margin-bottom: 26px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
}

.more-link {
  color: var(--theme-pink);
  font-weight: 800;
}

.latest-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

.skeleton-card {
  padding: 24px;
}

.landing-footer {
  padding: 32px 20px 42px;
  border-top: 1px solid var(--border-soft);
  display: flex;
  justify-content: center;
  gap: 10px;
  color: var(--text-faint);
  font-size: 13px;
}

.landing-footer b {
  color: var(--theme-pink);
}

@media (max-width: 900px) {
  .landing-mask {
    grid-template-columns: 1fr;
  }

  .landing-orbit {
    display: none;
  }

  .latest-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .landing-mask {
    min-height: 620px;
    padding: 54px 22px 94px;
  }

  .landing-copy h1 {
    font-size: 46px;
  }

  .landing-features {
    grid-template-columns: 1fr;
  }

  .landing-features > div + div {
    border-top: 1px solid var(--border-soft);
    border-left: 0;
  }

  .latest-heading {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
