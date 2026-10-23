<template>
  <main class="page-shell community-page">
    <section class="community-hero" :style="{ backgroundImage: `url(${cardBgImg})` }">
      <div class="community-hero__mask">
        <span class="section-kicker">文章发现</span>
        <h1>发现值得阅读的文章</h1>
        <p>搜索标题、正文或作者，按标签筛选，并按你关心的维度排序。</p>
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
            <template #prefix
              ><el-icon><Search /></el-icon
            ></template>
          </el-input>
          <el-button type="primary" size="large" class="anime-btn" @click="submitSearch">搜索</el-button>
        </div>

        <div class="feed-toolbar glass-card">
          <div>
            <span class="section-kicker">搜索结果</span>
            <h2 class="section-title">{{ resultTitle }}</h2>
            <p class="section-desc">
              共 {{ pageData.total }} 篇，当前第 {{ pageData.page }} 页。
              <button v-if="tag" class="active-tag" type="button" @click="selectTag('')">
                #{{ tag }} <el-icon><Close /></el-icon>
              </button>
            </p>
          </div>
          <div class="sort-switch" role="group" aria-label="文章排序">
            <button
              v-for="option in sortOptions"
              :key="option.value"
              :class="{ active: sort === option.value }"
              type="button"
              @click="changeSort(option.value)"
            >
              {{ option.label }}
            </button>
          </div>
        </div>

        <div v-if="isLoading" class="feed-list">
          <el-skeleton v-for="index in 4" :key="index" animated :rows="5" class="glass-card loading-card" />
        </div>

        <el-empty v-else-if="errorMessage" class="glass-card empty-state" description="文章列表暂时无法加载">
          <el-button type="primary" plain @click="loadPage">重新加载</el-button>
        </el-empty>

        <el-empty
          v-else-if="!pageData.items.length"
          class="glass-card empty-state"
          :description="keyword || tag ? '没有找到匹配的文章，换个条件试试' : '社区还很安静，等待第一篇文章'"
        >
          <el-button v-if="keyword || tag" type="primary" plain @click="clearFilters">清除筛选</el-button>
          <el-button v-else-if="token" type="primary" class="anime-btn" @click="router.push('/editor')"
            >写第一篇</el-button
          >
          <el-button v-else type="primary" class="anime-btn" @click="openAuth('register', '/editor')"
            >注册并开始写作</el-button
          >
        </el-empty>

        <div v-else class="feed-list">
          <ArticleCard
            v-for="article in pageData.items"
            :key="article.id"
            :article="article"
            @open="openArticle"
            @open-author="openAuthor"
            @select-tag="selectTag"
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

      <CommunitySidebar
        :total="pageData.total"
        :sort-label="currentSortLabel"
        :popular-tags="popularTags"
        :active-tag="tag"
        @select-tag="selectTag"
      />
    </section>
  </main>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { Close, Search } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import ArticleCard from '../components/ArticleCard.vue'
import CommunitySidebar from '../components/CommunitySidebar.vue'
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
const tag = ref('')
const sort = ref('published')
const popularTags = ref([])
const isLoading = ref(false)
const errorMessage = ref('')
const pageData = reactive({
  items: [],
  total: 0,
  page: 1,
  pageSize: 10,
  totalPages: 0,
})

const sortOptions = [
  { value: 'published', label: '发布时间' },
  { value: 'commented', label: '评论时间' },
  { value: 'likes', label: '点赞数' },
  { value: 'comments', label: '评论数' },
]
const currentSortLabel = computed(() => sortOptions.find((option) => option.value === sort.value)?.label || '发布时间')
const resultTitle = computed(() => {
  if (keyword.value) return `“${keyword.value}” 的结果`
  if (tag.value) return `#${tag.value}`
  return '全部文章'
})

const syncFromRoute = () => {
  const normalized = normalizeArticleQuery(route.query)
  keyword.value = normalized.keyword
  draftKeyword.value = keyword.value
  tag.value = normalized.tag
  sort.value = normalized.sort
  pageData.page = normalized.page
}

const loadPage = async () => {
  isLoading.value = true
  errorMessage.value = ''
  try {
    const data = apiData(
      await request.get('/articles/page', {
        params: {
          page: pageData.page,
          pageSize: pageData.pageSize,
          keyword: keyword.value || undefined,
          tag: tag.value || undefined,
          sort: sort.value,
        },
      })
    )
    Object.assign(pageData, data)
  } catch {
    errorMessage.value = '文章加载失败'
  } finally {
    isLoading.value = false
  }
}

const loadPopularTags = async () => {
  try {
    popularTags.value = apiData(await request.get('/articles/tags')) || []
  } catch {
    popularTags.value = []
  }
}

const updateRoute = (overrides = {}) => {
  const nextState = {
    keyword: keyword.value,
    tag: tag.value,
    sort: sort.value,
    page: pageData.page,
    ...overrides,
  }
  router.push({ path: '/community', query: buildArticleQuery(nextState) })
}

const submitSearch = () => {
  updateRoute({ keyword: draftKeyword.value.trim(), page: 1 })
}

const selectTag = (value) => {
  updateRoute({ tag: value, page: 1 })
}

const clearFilters = () => {
  draftKeyword.value = ''
  updateRoute({ keyword: '', tag: '', page: 1 })
}

const changeSort = (value) => {
  if (sort.value === value) return
  updateRoute({ sort: value, page: 1 })
}

const changePage = (value) => {
  updateRoute({ page: value })
  window.scrollTo({ top: 360, behavior: 'smooth' })
}

const openArticle = (id) => {
  router.push(`/articles/${id}`)
}

const openAuthor = (id) => {
  if (id) router.push(`/users/${id}`)
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

onMounted(loadPopularTags)
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

.active-tag {
  margin-left: 6px;
  padding: 2px 6px 2px 10px;
  border: 0;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  gap: 3px;
  color: white;
  background: var(--theme-pink);
  cursor: pointer;
  font-size: 12px;
  font-weight: 700;
  vertical-align: middle;
}

.sort-switch {
  padding: 4px;
  border-radius: 13px;
  display: flex;
  flex-wrap: wrap;
  background: var(--surface-soft);
}

.sort-switch button {
  min-width: 76px;
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

@media (max-width: 900px) {
  .community-layout {
    grid-template-columns: 1fr;
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
}
</style>
