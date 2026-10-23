<template>
  <main class="page-shell article-detail-page">
    <el-button text class="back-btn" @click="router.push('/community')">
      <el-icon><ArrowLeft /></el-icon>
      返回社区
    </el-button>

    <el-skeleton v-if="isLoading" animated :rows="12" class="glass-card detail-loading" />

    <el-empty v-else-if="errorMessage" class="glass-card empty-state" :description="errorMessage">
      <el-button type="primary" plain @click="loadArticle">重新加载</el-button>
    </el-empty>

    <template v-else-if="article">
      <article class="glass-card article-body">
        <header class="article-header">
          <component
            :is="article.authorDeleted ? 'div' : 'button'"
            class="article-author"
            :class="{ 'article-author--deleted': article.authorDeleted }"
            :type="article.authorDeleted ? undefined : 'button'"
            @click="!article.authorDeleted && goUserProfile(article.authorId)"
          >
            <el-avatar :size="46" :src="mediaUrl(article.authorAvatar)">
              {{ authorInitial }}
            </el-avatar>
            <div>
              <b>{{ article.authorNickname || '匿名用户' }}</b>
              <span v-if="isDraft">草稿 · 最后编辑于 {{ formatDate(article.updateTime) }}</span>
              <span v-else>发布于 {{ formatDate(article.createTime) }}</span>
            </div>
          </component>

          <div v-if="isOwner" class="owner-actions">
            <el-button plain @click="router.push(`/editor/${article.id}`)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button plain type="danger" @click="deleteArticle">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </div>
        </header>

        <div v-if="isDraft" class="draft-banner">这是一篇草稿，只有你自己能看到。编辑后选择“发布文章”即可公开。</div>

        <h1>{{ article.title }}</h1>
        <ArticleTags class="article-tags" :tags="article.tags || []" @select="openTag" />
        <MarkdownContent :source="article.content" />

        <footer class="article-actions">
          <el-button
            round
            :type="article.likedByMe ? 'primary' : 'default'"
            :loading="likeLoading"
            :disabled="isDraft"
            @click="toggleArticleLike"
          >
            <el-icon><StarFilled v-if="article.likedByMe" /><Star v-else /></el-icon>
            {{ article.likedByMe ? '已点赞' : '点赞' }} {{ article.likeCount || 0 }}
          </el-button>
          <span
            ><el-icon><ChatDotRound /></el-icon> 评论 {{ article.commentCount || 0 }}</span
          >
        </footer>
      </article>

      <CommentSection
        :article-id="article.id"
        :article-author-id="Number(article.authorId)"
        :total-count="article.commentCount || 0"
        :interactive="!isDraft"
        @count-change="article.commentCount = Math.max((article.commentCount || 0) + $event, 0)"
      />
    </template>
  </main>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ArrowLeft, ChatDotRound, Delete, Edit, Star, StarFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import ArticleTags from '../components/ArticleTags.vue'
import CommentSection from '../components/CommentSection.vue'
import MarkdownContent from '../components/MarkdownContent.vue'
import { useUser } from '../composables/useUser'
import request, { apiData } from '../utils/request'
import { formatDate } from '../utils/date'
import { mediaUrl } from '../utils/media'

const route = useRoute()
const router = useRouter()
const { token, userInfo, openAuth } = useUser()
const article = ref(null)
const isLoading = ref(false)
const likeLoading = ref(false)
const errorMessage = ref('')

const articleId = () => Number(route.params.id)
const authorInitial = computed(() => (article.value?.authorNickname || '用').slice(0, 1))
const isDraft = computed(() => article.value?.status === 'draft')
const isOwner = computed(
  () => article.value && userInfo.value && Number(article.value.authorId) === Number(userInfo.value.id)
)

const loadArticle = async () => {
  isLoading.value = true
  errorMessage.value = ''
  try {
    article.value = apiData(await request.get(`/articles/${articleId()}`))
  } catch {
    article.value = null
    errorMessage.value = '文章不存在或暂时无法加载'
  } finally {
    isLoading.value = false
  }
}

const toggleArticleLike = async () => {
  if (!token.value) {
    openAuth('login', route.fullPath)
    return
  }
  if (likeLoading.value) return
  likeLoading.value = true
  try {
    const data = apiData(
      await request.post('/like', null, {
        params: { articleId: articleId() },
      })
    )
    article.value.likedByMe = data.liked
    article.value.likeCount = data.likeCount
  } finally {
    likeLoading.value = false
  }
}

const deleteArticle = async () => {
  try {
    await ElMessageBox.confirm('删除后文章、评论和点赞都无法恢复，确认继续吗？', '删除文章', {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
    })
    await request.delete(`/articles/${article.value.id}`)
    ElMessage.success('文章已删除')
    await router.replace('/home')
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
  }
}

const goUserProfile = (userId) => {
  if (userId) router.push(`/users/${userId}`)
}

const openTag = (tag) => {
  router.push({ path: '/community', query: { tag } })
}

watch(
  () => route.params.id,
  () => {
    article.value = null
    loadArticle()
  },
  { immediate: true }
)
</script>

<style scoped>
.article-detail-page {
  width: min(920px, calc(100% - 40px));
}

.back-btn {
  margin: 0 0 14px -12px;
  color: var(--text-muted);
}

.detail-loading,
.article-body {
  padding: clamp(24px, 5vw, 48px);
}

.article-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.article-author {
  padding: 0;
  border: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  text-align: left;
  background: transparent;
  cursor: pointer;
}

.article-author:hover b,
.article-author:focus-visible b {
  color: var(--theme-pink);
}

.article-author--deleted {
  cursor: default;
}

.article-author--deleted b,
.article-author--deleted:hover b {
  color: var(--text-faint);
}

.article-author div {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.article-author b {
  color: var(--text-strong);
}

.article-author span {
  color: var(--text-faint);
  font-size: 13px;
}

.owner-actions {
  display: flex;
  gap: 8px;
}

.draft-banner {
  margin-top: 24px;
  padding: 12px 16px;
  border-radius: 12px;
  color: var(--text-muted);
  background: var(--surface-soft);
  font-size: 14px;
}

.article-body > h1 {
  max-width: 780px;
  margin: 38px 0 16px;
  color: var(--text-strong);
  font-size: clamp(34px, 6vw, 54px);
  line-height: 1.16;
  letter-spacing: -0.03em;
}

.article-tags {
  margin-bottom: 30px;
}

.article-actions {
  margin-top: 44px;
  padding-top: 24px;
  border-top: 1px solid var(--border-soft);
  display: flex;
  align-items: center;
  gap: 18px;
}

.article-actions span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--text-faint);
  font-size: 13px;
}

@media (max-width: 640px) {
  .article-detail-page {
    width: min(100% - 24px, 920px);
  }

  .article-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .owner-actions {
    width: 100%;
  }

  .owner-actions .el-button {
    flex: 1;
  }
}
</style>
