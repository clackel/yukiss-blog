<template>
  <div class="community-page">
    <section class="community-hero" :style="{ backgroundImage: `url(${cardBgImg})` }">
      <div class="community-hero-mask">
        <span class="section-kicker">Discovery</span>
        <h1>社区发现</h1>
        <p>在这里翻阅来自不同旅人的公开记录，找到同频的灵感坐标。</p>
      </div>
    </section>

    <div class="community-layout">
      <main class="community-feed">
        <div class="feed-toolbar glass-card">
          <div>
            <span class="section-kicker">Public Feed</span>
            <h2 class="section-title">全站投稿</h2>
            <p class="section-desc">按发布时间排序，越新的灵感越靠前。</p>
          </div>
          <el-button type="primary" round class="anime-btn" @click="fetchArticles">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>

        <template v-if="isLoading">
          <el-card v-for="index in 4" :key="index" class="glass-card community-card" shadow="never">
            <el-skeleton animated :rows="3" />
          </el-card>
        </template>

        <el-card v-else-if="errorMessage" class="glass-card community-empty" shadow="hover">
          <el-empty description="频道暂时连接失败">
            <el-button type="primary" round class="anime-btn" @click="fetchArticles">重新加载</el-button>
          </el-empty>
        </el-card>

        <el-card v-else-if="!articles.length" class="glass-card community-empty" shadow="hover">
          <el-empty description="社区频道还很安静，等待第一篇公开投稿" />
        </el-card>

        <el-card v-else v-for="article in articles" :key="article.id" class="glass-card community-card" shadow="hover" @click="openArticle(article.id)">
          <div class="community-card-main">
            <el-avatar :size="46" :src="article.authorAvatar" class="community-avatar">
              {{ getAuthorInitial(article) }}
            </el-avatar>
            <div class="community-card-content">
              <div class="community-card-top">
                <h3>{{ article.title }}</h3>
                <span>{{ formatDate(article.createTime) }}</span>
              </div>
              <p>{{ article.content }}</p>
              <div class="community-tags">
                <span>#公开频道</span>
                <span>#{{ article.authorNickname || '神秘旅人' }}</span>
                <span>♥ {{ article.likeCount || 0 }}</span>
                <span>评论 {{ article.commentCount || 0 }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </main>

      <aside class="community-aside">
        <el-card class="glass-card side-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <el-icon style="vertical-align: middle; margin-right: 4px;"><Connection /></el-icon> 频道状态
            </div>
          </template>
          <div class="stat-grid">
            <div class="stat-box">
              <span>投稿</span>
              <b>{{ articles.length }}</b>
            </div>
            <div class="stat-box">
              <span>作者</span>
              <b>{{ authorCount }}</b>
            </div>
          </div>
        </el-card>

        <el-card class="glass-card side-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <el-icon style="vertical-align: middle; margin-right: 4px;"><Compass /></el-icon> 推荐探索
            </div>
          </template>
          <div class="vibe-list">
            <span>新番碎碎念</span>
            <span>技术笔记</span>
            <span>生活记录</span>
            <span>灵感短打</span>
          </div>
        </el-card>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Compass, Connection, Refresh } from '@element-plus/icons-vue'
import cardBgImg from '../assets/card-bg.jpg'
import { useArticles } from '../composables/useArticles'
import { formatDate } from '../utils/date'

const { articles, isLoading, errorMessage, fetchArticles } = useArticles()
const router = useRouter()

const authorCount = computed(() => {
  const authors = articles.value.map(article => article.authorNickname || article.authorId).filter(Boolean)
  return new Set(authors).size
})

const getAuthorInitial = (article) => {
  const name = article.authorNickname || '旅'
  return name.slice(0, 1)
}

const openArticle = (id) => {
  router.push(`/articles/${id}`)
}

onMounted(() => fetchArticles())
</script>

<style scoped>
.community-page {
  min-height: 100vh;
  padding: 90px 20px 60px;
  background:
    linear-gradient(180deg, rgba(255, 248, 252, 0.9), rgba(244, 245, 247, 1) 340px);
}
.community-hero {
  max-width: 1180px;
  min-height: 220px;
  margin: 0 auto 24px;
  border-radius: 16px;
  overflow: hidden;
  background-size: cover;
  background-position: center;
  box-shadow: var(--theme-shadow);
}
.community-hero-mask {
  min-height: 220px;
  padding: 42px;
  color: #fff;
  background: linear-gradient(90deg, rgba(40, 34, 48, 0.72), rgba(40, 34, 48, 0.18));
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.community-hero h1 {
  margin: 8px 0 10px;
  font-size: 34px;
  line-height: 1.2;
}
.community-hero p {
  max-width: 520px;
  margin: 0;
  line-height: 1.8;
}
.community-layout {
  max-width: 1180px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 24px;
}
.community-feed {
  min-width: 0;
}
.community-card {
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}
.community-card:hover {
  transform: translateY(-2px);
}
.community-card .el-card__body {
  padding: 0;
}
.community-card-main {
  display: flex;
  gap: 16px;
  padding: 22px;
}
.community-avatar {
  flex: 0 0 auto;
  background: rgba(255, 107, 177, 0.16);
  color: var(--theme-pink);
  font-weight: 800;
}
.community-card-content {
  min-width: 0;
  flex: 1;
}
.community-card-top {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: flex-start;
}
.community-card-top h3 {
  margin: 0;
  color: #333;
  font-size: 21px;
  line-height: 1.35;
}
.community-card-top span {
  flex: 0 0 auto;
  color: #999;
  font-size: 13px;
}
.community-card-content p {
  margin: 12px 0 16px;
  color: #555;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.community-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.community-tags span {
  padding: 5px 10px;
  border-radius: 8px;
  background: rgba(255, 107, 177, 0.1);
  color: var(--theme-pink);
  font-size: 12px;
  font-weight: 700;
}
.community-empty {
  padding: 28px 0;
}
.community-aside {
  min-width: 0;
}

@media (max-width: 900px) {
  .community-layout {
    grid-template-columns: 1fr;
  }
  .community-hero-mask {
    padding: 30px 24px;
  }
}

@media (max-width: 640px) {
  .community-page {
    padding-left: 14px;
    padding-right: 14px;
  }
  .community-card-main,
  .community-card-top {
    flex-direction: column;
  }
  .community-card-top span {
    flex: auto;
  }
}
</style>
