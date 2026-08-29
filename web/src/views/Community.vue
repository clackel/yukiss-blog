<template>
  <main class="page-shell community-page">
    <section class="community-hero" :style="{ backgroundImage: `url(${cardBgImg})` }">
      <div class="community-hero__mask">
        <span class="section-kicker">Public feed</span>
        <h1>从朋友的文字里，<br>发现另一种日常</h1>
        <p>按时间阅读新故事，或看看最近最受欢迎的灵感。</p>
      </div>
    </section>

    <section class="community-layout">
      <div class="community-main">
        <div class="search-panel glass-card">
          <el-input
            ref="searchInput"
            v-model="draftKeyword"
            clearable
            maxlength="80"
            size="large"
            placeholder="搜索标题、正文或作者"
            @clear="submitSearch"
            @keyup.enter="submitSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button type="primary" size="large" class="anime-btn" @click="submitSearch">搜索</el-button>
        </div>

        <div class="feed-toolbar glass-card">
          <div>
            <span class="section-kicker">Discovery</span>
            <h2 class="section-title">{{ keyword ? `“${keyword}” 的结果` : '全部故事' }}</h2>
            <p class="section-desc">共 {{ pageData.total }} 篇，当前第 {{ pageData.page }} 页。</p>
          </div>
          <div class="sort-switch" role="group" aria-label="文章排序">
            <button :class="{ active: sort === 'latest' }" type="button" @click="changeSort('latest')">最新</button>
            <button :class="{ active: sort === 'popular' }" type="button" @click="changeSort('popular')">热门</button>
          </div>
        </div>

        <div v-if="isLoading" class="feed-list">
          <el-skeleton v-for="index in 4" :key="index" animated :rows="5" class="glass-card loading-card" />
        </div>

        <el-empty v-else-if="errorMessage" class="glass-card empty-state" description="频道暂时连接失败">
          <el-button type="primary" plain @click="loadPage">重新加载</el-button>
        </el-empty>

        <el-empty
          v-else-if="!pageData.items.length"
          class="glass-card empty-state"
          :description="keyword ? '没有找到匹配的文章，换个关键词试试' : '社区还很安静，等待第一篇文章'"
        >
          <el-button v-if="keyword" type="primary" plain @click="clearSearch">清除搜索</el-button>
          <el-button v-else-if="token" type="primary" class="anime-btn" @click="router.push('/editor')">写第一篇</el-button>
          <el-button v-else type="primary" class="anime-btn" @click="openAuth('register', '/editor')">注册并开始写作</el-button>
        </el-empty>

        <div v-else class="feed-list">
          <ArticleCard
            v-for="article in pageData.items"
            :key="article.id"
            :article="article"
            @open="openArticle"
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
      </div>

      <aside class="community-aside">
        <div class="glass-card aside-card">
          <span class="section-kicker">Channel pulse</span>
          <h3>频道状态</h3>
          <div class="stats">
            <div>
              <b>{{ pageData.total }}</b>
              <span>公开文章</span>
            </div>
            <div>
              <b>{{ sort === 'latest' ? '新' : '热' }}</b>
              <span>当前排序</span>
            </div>
          </div>
        </div>

        <div class="glass-card aside-card">
          <span class="section-kicker">Writing prompt</span>
          <h3>今天写什么？</h3>
          <p>一件刚学会的小事、一段想保存的心情，或者一个还没想完整的念头。</p>
          <el-button v-if="token" text class="write-link" @click="router.push('/editor')">写下它 →</el-button>
          <el-button v-else text class="write-link" @click="openAuth('login', '/editor')">登录后写作 →</el-button>
        </div>
      </aside>
    </section>
  </main>
</template>

<script setup>
import { nextTick, onMounted, reactive, ref, watch } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import ArticleCard from '../components/ArticleCard.vue'
import { useUser } from '../composables/useUser'
import { buildArticleQuery, normalizeArticleQuery } from '../utils/articleQuery'
import request, { apiData } from '../utils/request'
import cardBgImg from '../assets/card-bg.jpg'

const route = useRoute()
const router = useRouter()
const { token, openAuth } = useUser()
const searchInput = ref(null)
const draftKeyword = ref('')
const keyword = ref('')
const sort = ref('latest')
const isLoading = ref(false)
const errorMessage = ref('')
const pageData = reactive({
  items: [],
  total: 0,
  page: 1,
  pageSize: 10,
  totalPages: 0,
})

const syncFromRoute = () => {
  const normalized = normalizeArticleQuery(route.query)
  keyword.value = normalized.keyword
  draftKeyword.value = keyword.value
  sort.value = normalized.sort
  pageData.page = normalized.page
}

const loadPage = async () => {
  isLoading.value = true
  errorMessage.value = ''
  try {
    const data = apiData(await request.get('/articles/page', {
      params: {
        page: pageData.page,
        pageSize: pageData.pageSize,
        keyword: keyword.value || undefined,
        sort: sort.value,
      },
    }))
    Object.assign(pageData, data)
  } catch {
    errorMessage.value = '文章加载失败'
  } finally {
    isLoading.value = false
  }
}

const updateRoute = (overrides = {}) => {
  const nextState = {
    keyword: keyword.value,
    sort: sort.value,
    page: pageData.page,
    ...overrides,
  }
  router.push({ path: '/community', query: buildArticleQuery(nextState) })
}

const submitSearch = () => {
  keyword.value = draftKeyword.value.trim()
  pageData.page = 1
  updateRoute({ keyword: keyword.value, page: 1 })
}

const clearSearch = () => {
  draftKeyword.value = ''
  keyword.value = ''
  pageData.page = 1
  updateRoute({ keyword: '', page: 1 })
}

const changeSort = (value) => {
  if (sort.value === value) return
  sort.value = value
  pageData.page = 1
  updateRoute({ sort: value, page: 1 })
}

const changePage = (value) => {
  pageData.page = value
  updateRoute({ page: value })
  window.scrollTo({ top: 360, behavior: 'smooth' })
}

const openArticle = (id) => {
  router.push(`/articles/${id}`)
}

watch(
  () => route.fullPath,
  async () => {
    syncFromRoute()
    await loadPage()
    if (route.query.focus === 'search') {
      await nextTick()
      searchInput.value?.focus()
    }
  },
  { immediate: true }
)

onMounted(async () => {
  if (route.query.focus === 'search') {
    await nextTick()
    searchInput.value?.focus()
  }
})
</script>

<style scoped>
.community-hero {
  min-height: 290px;
  margin-bottom: 24px;
  overflow: hidden;
  border-radius: 24px;
  background-size: cover;
  background-position: center;
  box-shadow: var(--theme-shadow);
}

.community-hero__mask {
  min-height: inherit;
  padding: 48px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  color: white;
  background: linear-gradient(90deg, rgba(30, 24, 34, 0.82), rgba(30, 24, 34, 0.26));
}

.community-hero h1 {
  margin: 12px 0;
  font-size: clamp(34px, 5vw, 52px);
  line-height: 1.15;
}

.community-hero p {
  margin: 0;
  color: rgba(255, 255, 255, 0.82);
  font-size: 16px;
}

.community-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 290px;
  gap: 24px;
}

.community-main {
  min-width: 0;
}

.search-panel {
  margin-bottom: 18px;
  padding: 15px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
}

.feed-toolbar {
  margin-bottom: 18px;
}

.sort-switch {
  padding: 4px;
  border-radius: 13px;
  display: flex;
  background: var(--surface-soft);
}

.sort-switch button {
  min-width: 64px;
  padding: 9px 14px;
  border: 0;
  border-radius: 10px;
  color: var(--text-muted);
  background: transparent;
  cursor: pointer;
  font-weight: 700;
}

.sort-switch button.active {
  color: white;
  background: var(--theme-pink);
  box-shadow: 0 5px 12px var(--theme-glow);
}

.feed-list {
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

.community-aside {
  display: grid;
  align-content: start;
  gap: 18px;
}

.aside-card {
  padding: 24px;
}

.aside-card h3 {
  margin: 8px 0 16px;
  color: var(--text-strong);
}

.aside-card p {
  color: var(--text-muted);
  line-height: 1.75;
}

.stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.stats div {
  padding: 16px 10px;
  border-radius: 13px;
  text-align: center;
  background: var(--accent-soft);
}

.stats b,
.stats span {
  display: block;
}

.stats b {
  color: var(--theme-pink);
  font-size: 24px;
}

.stats span {
  margin-top: 3px;
  color: var(--text-faint);
  font-size: 12px;
}

.write-link {
  padding-left: 0;
  color: var(--theme-pink);
  font-weight: 800;
}

@media (max-width: 900px) {
  .community-layout {
    grid-template-columns: 1fr;
  }

  .community-aside {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 620px) {
  .community-hero__mask {
    padding: 34px 24px;
  }

  .search-panel {
    grid-template-columns: 1fr;
  }

  .feed-toolbar {
    align-items: flex-start;
  }

  .community-aside {
    grid-template-columns: 1fr;
  }
}
</style>
