<template>
  <main class="page-shell community-page">
    <section class="community-hero" :style="{ backgroundImage: `url(${cardBgImg})` }">
      <div class="community-hero__mask">
        <span class="section-kicker">文章发现</span>
        <h1>发现值得阅读的文章</h1>
        <p>搜索标题、正文或作者，并按你关心的维度排序。</p>
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
            <span class="section-kicker">搜索结果</span>
            <h2 class="section-title">{{ keyword ? `“${keyword}” 的结果` : '全部文章' }}</h2>
            <p class="section-desc">共 {{ pageData.total }} 篇，当前第 {{ pageData.page }} 页。</p>
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
            @open-author="openAuthor"
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
        <div
          v-if="token"
          class="glass-card aside-card profile-summary"
          role="link"
          tabindex="0"
          @click="openMyPublicProfile"
          @keydown.enter="openMyPublicProfile"
        >
          <div class="profile-summary__head">
            <el-avatar :size="52" :src="mediaUrl(userInfo?.avatar)">
              {{ userInitial }}
            </el-avatar>
            <div>
              <h3>{{ userInfo?.nickname || userInfo?.username || '我的主页' }}</h3>
              <span>查看个人主页</span>
            </div>
          </div>
          <p>{{ userInfo?.bio || '还没有填写个人简介。' }}</p>
          <div class="follow-stats">
            <div>
              <b>{{ userInfo?.followerCount ?? 0 }}</b>
              <span>粉丝</span>
            </div>
            <div>
              <b>{{ userInfo?.followingCount ?? 0 }}</b>
              <span>关注</span>
            </div>
          </div>
        </div>

        <div v-else class="glass-card aside-card profile-summary profile-summary--guest">
          <span class="section-kicker">个人主页</span>
          <h3>登录后查看个人资料</h3>
          <p>登录后可以关注作者，并在这里快速进入你的主页。</p>
          <el-button type="primary" plain @click="openAuth('login', route.fullPath)">登录</el-button>
        </div>

        <div class="glass-card aside-card calendar-card">
          <div class="calendar-head">
            <button type="button" aria-label="上个月" @click="changeCalendarMonth(-1)">‹</button>
            <h3>{{ calendarTitle }}</h3>
            <button type="button" aria-label="下个月" @click="changeCalendarMonth(1)">›</button>
          </div>
          <div class="calendar-grid calendar-weekdays" aria-hidden="true">
            <span v-for="weekday in weekDays" :key="weekday">{{ weekday }}</span>
          </div>
          <div class="calendar-grid">
            <span
              v-for="day in calendarDays"
              :key="day.key"
              :class="{ blank: !day.value, today: day.isToday }"
            >
              {{ day.value }}
            </span>
          </div>
        </div>

        <div class="glass-card aside-card">
          <span class="section-kicker">文章概览</span>
          <h3>当前列表</h3>
          <div class="stats">
            <div>
              <b>{{ pageData.total }}</b>
              <span>文章</span>
            </div>
            <div>
              <b>{{ currentSortLabel }}</b>
              <span>排序方式</span>
            </div>
          </div>
          <el-button v-if="token" text class="write-link" @click="router.push('/editor')">写文章 →</el-button>
          <el-button v-else text class="write-link" @click="openAuth('login', '/editor')">登录后写文章 →</el-button>
        </div>
      </aside>
    </section>
  </main>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import ArticleCard from '../components/ArticleCard.vue'
import { useUser } from '../composables/useUser'
import { buildArticleQuery, normalizeArticleQuery } from '../utils/articleQuery'
import { mediaUrl } from '../utils/media'
import request, { apiData } from '../utils/request'
import cardBgImg from '../assets/card-bg.jpg'

const route = useRoute()
const router = useRouter()
const { token, userInfo, openAuth } = useUser()
const searchInput = ref(null)
const draftKeyword = ref('')
const keyword = ref('')
const sort = ref('published')
const calendarDate = ref(new Date())
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
const currentSortLabel = computed(() => (
  sortOptions.find(option => option.value === sort.value)?.label || '发布时间'
))
const userInitial = computed(() => (
  userInfo.value?.nickname || userInfo.value?.username || '我'
).slice(0, 1))
const weekDays = ['日', '一', '二', '三', '四', '五', '六']
const calendarTitle = computed(() => (
  `${calendarDate.value.getFullYear()} 年 ${calendarDate.value.getMonth() + 1} 月`
))
const calendarDays = computed(() => {
  const year = calendarDate.value.getFullYear()
  const month = calendarDate.value.getMonth()
  const firstWeekday = new Date(year, month, 1).getDay()
  const dayCount = new Date(year, month + 1, 0).getDate()
  const now = new Date()
  const days = Array.from({ length: firstWeekday }, (_, index) => ({
    key: `blank-${index}`,
    value: '',
    isToday: false,
  }))
  for (let day = 1; day <= dayCount; day += 1) {
    days.push({
      key: `${year}-${month}-${day}`,
      value: day,
      isToday: year === now.getFullYear() && month === now.getMonth() && day === now.getDate(),
    })
  }
  return days
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

const openAuthor = (id) => {
  if (id) router.push(`/users/${id}`)
}

const openMyPublicProfile = () => {
  if (userInfo.value?.id) router.push(`/users/${userInfo.value.id}`)
}

const changeCalendarMonth = (offset) => {
  calendarDate.value = new Date(
    calendarDate.value.getFullYear(),
    calendarDate.value.getMonth() + offset,
    1
  )
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

.profile-summary[role='link'] {
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.profile-summary[role='link']:hover,
.profile-summary[role='link']:focus-visible {
  transform: translateY(-2px);
  box-shadow: var(--theme-shadow-hover) !important;
  outline: none;
}

.profile-summary__head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.profile-summary__head h3 {
  margin: 0 0 4px;
}

.profile-summary__head span {
  color: var(--text-faint);
  font-size: 12px;
}

.profile-summary > p {
  margin: 16px 0;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.profile-summary--guest h3 {
  margin-bottom: 10px;
}

.follow-stats {
  padding-top: 14px;
  border-top: 1px solid var(--border-soft);
  display: grid;
  grid-template-columns: 1fr 1fr;
}

.follow-stats div {
  text-align: center;
}

.follow-stats div + div {
  border-left: 1px solid var(--border-soft);
}

.follow-stats b,
.follow-stats span {
  display: block;
}

.follow-stats b {
  color: var(--text-strong);
  font-size: 18px;
}

.follow-stats span {
  margin-top: 3px;
  color: var(--text-faint);
  font-size: 12px;
}

.calendar-card {
  padding: 18px;
}

.calendar-head {
  margin-bottom: 12px;
  display: grid;
  grid-template-columns: 32px 1fr 32px;
  align-items: center;
  gap: 5px;
}

.calendar-head h3 {
  margin: 0;
  text-align: center;
  font-size: 15px;
}

.calendar-head button {
  width: 32px;
  height: 32px;
  border: 0;
  border-radius: 9px;
  color: var(--text-muted);
  background: transparent;
  cursor: pointer;
  font-size: 22px;
}

.calendar-head button:hover {
  color: var(--theme-pink);
  background: var(--accent-soft);
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 3px;
}

.calendar-grid span {
  aspect-ratio: 1;
  border-radius: 8px;
  display: grid;
  place-items: center;
  color: var(--text-muted);
  font-size: 12px;
}

.calendar-weekdays span {
  aspect-ratio: auto;
  margin-bottom: 3px;
  color: var(--text-faint);
  font-size: 11px;
}

.calendar-grid span.today {
  color: white;
  background: var(--theme-pink);
  font-weight: 800;
}

.calendar-grid span.blank {
  visibility: hidden;
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

.stats div:nth-child(2) b {
  font-size: 14px;
  line-height: 34px;
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
