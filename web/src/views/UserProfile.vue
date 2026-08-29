<template>
  <main class="page-shell public-profile-page">
    <el-skeleton v-if="isLoading" animated :rows="10" class="glass-card profile-loading" />

    <el-empty v-else-if="errorMessage" class="glass-card empty-state" :description="errorMessage">
      <el-button type="primary" plain @click="loadProfile">重新加载</el-button>
    </el-empty>

    <template v-else-if="profile">
      <section class="glass-card profile-header">
        <el-avatar :size="104" :src="mediaUrl(profile.avatar)">
          {{ profileInitial }}
        </el-avatar>

        <div class="profile-info">
          <span class="section-kicker">个人主页</span>
          <h1>{{ profile.nickname || '匿名用户' }}</h1>
          <p>{{ profile.bio || '这个用户还没有填写个人简介。' }}</p>
          <div class="profile-meta">
            <span v-if="profile.location"><el-icon><Location /></el-icon>{{ profile.location }}</span>
            <span><el-icon><Calendar /></el-icon>{{ formatDate(profile.createTime) }} 加入</span>
            <a
              v-if="safeWebsite"
              :href="safeWebsite"
              target="_blank"
              rel="noopener noreferrer"
            >
              <el-icon><Link /></el-icon>个人网站
            </a>
          </div>
        </div>

        <div class="profile-actions">
          <div class="profile-stats">
            <div>
              <b>{{ profile.followerCount ?? 0 }}</b>
              <span>粉丝</span>
            </div>
            <div>
              <b>{{ profile.followingCount ?? 0 }}</b>
              <span>关注</span>
            </div>
          </div>

          <el-button v-if="profile.ownProfile" type="primary" plain @click="router.push('/profile')">
            编辑资料
          </el-button>
          <el-button
            v-else
            :type="profile.followedByMe ? 'default' : 'primary'"
            :plain="profile.followedByMe"
            :loading="followLoading"
            @click="toggleFollow"
          >
            {{ profile.followedByMe ? '取消关注' : '关注' }}
          </el-button>
        </div>
      </section>

      <section class="profile-articles">
        <div class="feed-toolbar glass-card">
          <div>
            <span class="section-kicker">公开文章</span>
            <h2 class="section-title">{{ profile.nickname || '该用户' }}的文章</h2>
            <p class="section-desc">共 {{ articles.length }} 篇。</p>
          </div>
        </div>

        <el-empty
          v-if="!articles.length"
          class="glass-card empty-state"
          description="该用户还没有发布文章"
        />

        <div v-else class="article-list">
          <ArticleCard
            v-for="article in articles"
            :key="article.id"
            :article="article"
            :show-author="false"
            @open="openArticle"
          />
        </div>
      </section>
    </template>
  </main>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { Calendar, Link, Location } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import ArticleCard from '../components/ArticleCard.vue'
import { useUser } from '../composables/useUser'
import { formatDate } from '../utils/date'
import { mediaUrl } from '../utils/media'
import request, { apiData } from '../utils/request'

const route = useRoute()
const router = useRouter()
const { token, openAuth, refreshMe } = useUser()
const profile = ref(null)
const articles = ref([])
const isLoading = ref(false)
const followLoading = ref(false)
const errorMessage = ref('')

const userId = computed(() => Number(route.params.id))
const profileInitial = computed(() => (profile.value?.nickname || '用').slice(0, 1))
const safeWebsite = computed(() => {
  const value = profile.value?.website?.trim()
  return value && /^https?:\/\//i.test(value) ? value : ''
})

async function loadProfile() {
  if (!Number.isInteger(userId.value) || userId.value < 1) {
    errorMessage.value = '用户不存在'
    return
  }
  isLoading.value = true
  errorMessage.value = ''
  try {
    const [profileResponse, articlesResponse] = await Promise.all([
      request.get(`/users/${userId.value}`),
      request.get(`/articles/user/${userId.value}`),
    ])
    profile.value = apiData(profileResponse)
    articles.value = apiData(articlesResponse) || []
  } catch {
    profile.value = null
    articles.value = []
    errorMessage.value = '用户不存在或主页暂时无法加载'
  } finally {
    isLoading.value = false
  }
}

async function toggleFollow() {
  if (!token.value) {
    openAuth('login', route.fullPath)
    return
  }
  followLoading.value = true
  try {
    const response = profile.value.followedByMe
      ? await request.delete(`/users/${userId.value}/follow`)
      : await request.post(`/users/${userId.value}/follow`)
    profile.value = apiData(response)
    await refreshMe().catch(() => {})
  } finally {
    followLoading.value = false
  }
}

const openArticle = (id) => router.push(`/articles/${id}`)

watch(() => [route.params.id, token.value], loadProfile, { immediate: true })
</script>

<style scoped>
.public-profile-page {
  width: min(980px, calc(100% - 40px));
}

.profile-loading,
.profile-header {
  padding: clamp(24px, 5vw, 42px);
}

.profile-header {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 26px;
}

.profile-info h1 {
  margin: 7px 0 10px;
  color: var(--text-strong);
  font-size: clamp(30px, 5vw, 42px);
}

.profile-info > p {
  max-width: 560px;
  margin: 0;
  color: var(--text-muted);
  line-height: 1.75;
  white-space: pre-wrap;
}

.profile-meta {
  margin-top: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px 18px;
  color: var(--text-faint);
  font-size: 13px;
}

.profile-meta span,
.profile-meta a {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.profile-meta a {
  color: var(--theme-pink);
  text-decoration: none;
}

.profile-actions {
  min-width: 170px;
  display: grid;
  gap: 16px;
}

.profile-actions > .el-button {
  width: 100%;
  margin: 0;
}

.profile-stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
}

.profile-stats div {
  padding: 2px 16px;
  text-align: center;
}

.profile-stats div + div {
  border-left: 1px solid var(--border-soft);
}

.profile-stats b,
.profile-stats span {
  display: block;
}

.profile-stats b {
  color: var(--text-strong);
  font-size: 23px;
}

.profile-stats span {
  margin-top: 3px;
  color: var(--text-faint);
  font-size: 12px;
}

.profile-articles {
  margin-top: 22px;
}

.profile-articles .feed-toolbar {
  margin-bottom: 16px;
}

.article-list {
  display: grid;
  gap: 16px;
}

@media (max-width: 720px) {
  .public-profile-page {
    width: min(100% - 28px, 980px);
  }

  .profile-header {
    grid-template-columns: 1fr;
    justify-items: center;
    text-align: center;
  }

  .profile-meta {
    justify-content: center;
  }

  .profile-actions {
    width: min(100%, 280px);
  }
}
</style>
