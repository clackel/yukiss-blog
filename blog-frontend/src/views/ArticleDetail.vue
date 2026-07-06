<template>
  <div class="article-detail-page">
    <main class="article-shell">
      <el-button class="back-btn" text @click="router.back()">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>

      <el-card v-if="isLoading" class="glass-card detail-card" shadow="never">
        <el-skeleton animated :rows="8" />
      </el-card>

      <el-card v-else-if="errorMessage" class="glass-card detail-card" shadow="hover">
        <el-empty :description="errorMessage">
          <el-button type="primary" round class="anime-btn" @click="loadArticle">重新加载</el-button>
        </el-empty>
      </el-card>

      <template v-else-if="article">
        <article class="glass-card detail-card article-body">
          <div class="article-meta">
            <el-avatar :size="44" :src="article.authorAvatar">
              {{ getAuthorInitial(article) }}
            </el-avatar>
            <div>
              <b>{{ article.authorNickname || '神秘旅人' }}</b>
              <span>{{ formatDate(article.createTime) }}</span>
            </div>
          </div>

          <h1>{{ article.title }}</h1>
          <p class="article-content">{{ article.content }}</p>

          <div class="article-actions">
            <el-button round :type="article.likedByMe ? 'primary' : 'default'" @click="toggleArticleLike">
              <el-icon><Star /></el-icon>
              {{ article.likedByMe ? '已点赞' : '点赞' }} {{ article.likeCount || 0 }}
            </el-button>
            <span>评论 {{ comments.length }}</span>
          </div>
        </article>

        <section class="glass-card comments-panel">
          <div class="comments-head">
            <h2>评论</h2>
            <span>{{ comments.length }}</span>
          </div>

          <div class="comment-editor">
            <el-input
              v-model="newComment"
              type="textarea"
              :rows="4"
              maxlength="300"
              show-word-limit
              resize="none"
              placeholder="写下你的想法..."
            />
            <el-button type="primary" round class="anime-btn" :loading="isSubmitting" @click="submitComment">
              发送
            </el-button>
          </div>

          <el-empty v-if="!comments.length" description="还没有评论，来写第一条吧" />

          <div v-else class="comment-list">
            <div v-for="comment in comments" :key="comment.id" class="comment-item">
              <el-avatar :size="36" :src="comment.avatar">
                {{ getCommentInitial(comment) }}
              </el-avatar>
              <div class="comment-main">
                <div class="comment-top">
                  <b>{{ comment.nickname || '神秘旅人' }}</b>
                  <span>{{ formatDate(comment.createTime) }}</span>
                </div>
                <p>{{ comment.content }}</p>
                <el-button text size="small" @click="toggleCommentLike(comment)">
                  <el-icon><Star /></el-icon>
                  {{ comment.likedByMe ? '已点赞' : '点赞' }} {{ comment.likeCount || 0 }}
                </el-button>
              </div>
            </div>
          </div>
        </section>
      </template>
    </main>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Star } from '@element-plus/icons-vue'
import request, { apiData } from '../utils/request'
import { formatDate } from '../utils/date'

const route = useRoute()
const router = useRouter()
const article = ref(null)
const comments = ref([])
const newComment = ref('')
const isLoading = ref(false)
const isSubmitting = ref(false)
const errorMessage = ref('')

const articleId = () => Number(route.params.id)

const loadComments = async () => {
  comments.value = apiData(await request.get('/comment/list', {
    params: { articleId: articleId() },
  })) || []
}

const loadArticle = async () => {
  isLoading.value = true
  errorMessage.value = ''
  try {
    article.value = apiData(await request.get(`/articles/${articleId()}`))
    await loadComments()
  } catch (err) {
    errorMessage.value = '文章加载失败，请稍后重试'
  } finally {
    isLoading.value = false
  }
}

const submitComment = async () => {
  const content = newComment.value.trim()
  if (!content) {
    ElMessage.warning('请先填写评论内容')
    return
  }
  isSubmitting.value = true
  try {
    await request.post('/comment/add', {
      articleId: articleId(),
      content,
    })
    newComment.value = ''
    ElMessage.success('评论成功')
    await loadComments()
    if (article.value) {
      article.value.commentCount = comments.value.length
    }
  } finally {
    isSubmitting.value = false
  }
}

const toggleArticleLike = async () => {
  const data = apiData(await request.post('/like', null, {
    params: { articleId: articleId() },
  }))
  article.value = {
    ...article.value,
    likedByMe: data.liked,
    likeCount: data.likeCount,
  }
}

const toggleCommentLike = async (comment) => {
  const data = apiData(await request.post(`/comment/${comment.id}/like`))
  comment.likedByMe = data.liked
  comment.likeCount += data.liked ? 1 : -1
}

const getAuthorInitial = (item) => (item.authorNickname || '旅').slice(0, 1)
const getCommentInitial = (item) => (item.nickname || '评').slice(0, 1)

onMounted(loadArticle)
</script>

<style scoped>
.article-detail-page {
  min-height: 100vh;
  padding: 92px 20px 64px;
  background:
    linear-gradient(180deg, rgba(255, 248, 252, 0.92), rgba(244, 245, 247, 1) 360px);
}
.article-shell {
  max-width: 860px;
  margin: 0 auto;
}
.glass-card {
  background: rgba(255, 255, 255, 0.84);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.58);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.05);
}
.anime-btn {
  background: var(--theme-pink) !important;
  border: none !important;
  box-shadow: 0 4px 12px rgba(255, 107, 177, 0.3);
}
.back-btn {
  margin-bottom: 14px;
  color: #666;
}
.detail-card,
.comments-panel {
  padding: 30px;
  border-radius: 12px !important;
}
.article-body {
  display: block;
}
.article-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #333;
}
.article-meta div {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.article-meta span,
.comment-top span,
.article-actions span {
  color: #999;
  font-size: 13px;
}
.article-body h1 {
  margin: 24px 0 18px;
  color: #2f2f33;
  font-size: 34px;
  line-height: 1.28;
  letter-spacing: 0;
}
.article-content {
  white-space: pre-wrap;
  color: #4f4f56;
  font-size: 16px;
  line-height: 1.9;
}
.article-actions {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 28px;
}
.comments-panel {
  margin-top: 20px;
}
.comments-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}
.comments-head h2 {
  margin: 0;
  color: #333;
  font-size: 22px;
}
.comments-head span {
  color: var(--theme-pink);
  font-weight: 800;
}
.comment-editor {
  display: grid;
  gap: 12px;
  margin-bottom: 22px;
}
.comment-editor .el-button {
  justify-self: end;
}
.comment-list {
  display: grid;
  gap: 18px;
}
.comment-item {
  display: flex;
  gap: 12px;
  padding-top: 18px;
  border-top: 1px solid rgba(255, 107, 177, 0.13);
}
.comment-main {
  flex: 1;
  min-width: 0;
}
.comment-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}
.comment-main p {
  margin: 8px 0 6px;
  color: #555;
  line-height: 1.7;
}

@media (max-width: 640px) {
  .detail-card,
  .comments-panel {
    padding: 22px;
  }
  .article-body h1 {
    font-size: 28px;
  }
  .comment-top {
    flex-direction: column;
    gap: 4px;
  }
}
</style>
