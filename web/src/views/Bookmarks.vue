<template>
  <main class="page-shell bookmarks-page">
    <div class="feed-toolbar glass-card">
      <div>
        <span class="section-kicker">收藏夹</span>
        <h1 class="section-title">我的收藏</h1>
        <p class="section-desc">共 {{ pageData.total }} 篇，按收藏时间排列。文章被作者退回草稿后会暂时从这里消失。</p>
      </div>
      <el-button plain round :loading="isLoading" @click="loadPage">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <div v-if="isLoading" class="article-list">
      <el-skeleton v-for="index in 3" :key="index" animated :rows="5" class="glass-card loading-card" />
    </div>

    <el-empty v-else-if="errorMessage" class="glass-card empty-state" :description="errorMessage">
      <el-button type="primary" plain @click="loadPage">重新加载</el-button>
    </el-empty>

    <el-empty v-else-if="!pageData.items.length" class="glass-card empty-state" description="还没有收藏任何文章">
      <el-button type="primary" class="anime-btn" @click="router.push('/community')">去发现文章</el-button>
    </el-empty>

    <div v-else class="article-list">
      <ArticleCard
        v-for="article in pageData.items"
        :key="article.id"
        :article="article"
        @open="openArticle"
        @open-author="openAuthor"
        @select-tag="openTag"
      />
    </div>

    <div v-if="pageData.totalPages > 1" class="pagination-wrap">
      <el-pagination
        background
        layout="prev, pager, next"
        :current-page="pageData.page"
        :page-count="pageData.totalPages"
        @current-change="changePage"
      />
    </div>
  </main>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import ArticleCard from '../components/ArticleCard.vue'
import request, { apiData } from '../utils/request'

const router = useRouter()
const isLoading = ref(false)
const errorMessage = ref('')
const pageData = reactive({ items: [], total: 0, page: 1, pageSize: 10, totalPages: 0 })

const loadPage = async () => {
  isLoading.value = true
  errorMessage.value = ''
  try {
    const data = apiData(
      await request.get('/bookmark/mine', {
        params: { page: pageData.page, pageSize: pageData.pageSize },
      })
    )
    Object.assign(pageData, data)
  } catch {
    errorMessage.value = '收藏列表加载失败'
  } finally {
    isLoading.value = false
  }
}

const changePage = (value) => {
  pageData.page = value
  loadPage()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const openArticle = (id) => router.push(`/articles/${id}`)
const openAuthor = (id) => {
  if (id) router.push(`/users/${id}`)
}
const openTag = (tag) => router.push({ path: '/community', query: { tag } })

onMounted(loadPage)
</script>

<style scoped>
.bookmarks-page {
  width: min(920px, calc(100% - 40px));
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

.pagination-wrap {
  padding: 24px 0 8px;
  display: flex;
  justify-content: center;
}

@media (max-width: 640px) {
  .bookmarks-page {
    width: min(100% - 24px, 920px);
  }
}
</style>
