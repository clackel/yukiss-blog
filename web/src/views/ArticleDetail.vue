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
          <button class="article-author" type="button" @click="goUserProfile(article.authorId)">
            <el-avatar :size="46" :src="mediaUrl(article.authorAvatar)">
              {{ authorInitial }}
            </el-avatar>
            <div>
              <b>{{ article.authorNickname || '匿名用户' }}</b>
              <span>发布于 {{ formatDate(article.createTime) }}</span>
            </div>
          </button>

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

        <h1>{{ article.title }}</h1>
        <MarkdownContent :source="article.content" />

        <footer class="article-actions">
          <el-button
            round
            :type="article.likedByMe ? 'primary' : 'default'"
            :loading="likeLoading"
            @click="toggleArticleLike"
          >
            <el-icon><StarFilled v-if="article.likedByMe" /><Star v-else /></el-icon>
            {{ article.likedByMe ? '已点赞' : '点赞' }} {{ article.likeCount || 0 }}
          </el-button>
          <span><el-icon><ChatDotRound /></el-icon> 评论 {{ comments.length }}</span>
        </footer>
      </article>

      <section class="glass-card comments-panel">
        <div class="comments-head">
          <div>
            <span class="section-kicker">评论</span>
            <h2>评论区</h2>
          </div>
          <b>{{ comments.length }}</b>
        </div>

        <div v-if="token" class="comment-editor">
          <div v-if="replyingTo" class="replying-banner">
            正在回复 {{ replyingTo.nickname || '匿名用户' }}
            <button type="button" @click="cancelReply">取消</button>
          </div>
          <el-input
            v-model="newComment"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            resize="none"
            :placeholder="replyingTo ? `回复 ${replyingTo.nickname || '这位朋友'}…` : '写下你的想法…'"
            @keydown.ctrl.enter="submitComment"
          />
          <div class="editor-actions">
            <span>Ctrl + Enter 发送</span>
            <el-button type="primary" round class="anime-btn" :loading="isSubmitting" @click="submitComment">
              发送
            </el-button>
          </div>
        </div>

        <div v-else class="login-to-comment">
          <p>登录后可以评论、回复和点赞。</p>
          <el-button type="primary" round class="anime-btn" @click="openAuth('login', route.fullPath)">登录参与讨论</el-button>
        </div>

        <el-skeleton v-if="commentsLoading" animated :rows="5" />

        <el-empty v-else-if="!comments.length" description="还没有评论，来写第一条吧" />

        <div v-else class="comment-list">
          <article v-for="comment in topLevelComments" :key="comment.id" class="comment-thread">
            <div class="comment-item">
              <el-avatar :size="38" :src="mediaUrl(comment.avatar)">
                {{ commentInitial(comment) }}
              </el-avatar>
              <div class="comment-main">
                <div class="comment-top">
                  <button class="comment-name" type="button" @click="goUserProfile(comment.userId)">
                    {{ comment.nickname || '匿名用户' }}
                  </button>
                  <span>{{ formatDateTime(comment.createTime) }}</span>
                </div>
                <p>{{ comment.content }}</p>
                <div class="comment-actions">
                  <button type="button" @click="toggleCommentLike(comment)">
                    <el-icon><StarFilled v-if="comment.likedByMe" /><Star v-else /></el-icon>
                    {{ comment.likeCount || 0 }}
                  </button>
                  <button type="button" @click="startReply(comment)">回复</button>
                </div>
              </div>
            </div>

            <div v-if="repliesFor(comment.id).length" class="reply-list">
              <div v-for="reply in repliesFor(comment.id)" :key="reply.id" class="comment-item comment-item--reply">
                <el-avatar :size="32" :src="mediaUrl(reply.avatar)">
                  {{ commentInitial(reply) }}
                </el-avatar>
                <div class="comment-main">
                  <div class="comment-top">
                    <button class="comment-name" type="button" @click="goUserProfile(reply.userId)">
                      {{ reply.nickname || '匿名用户' }}
                    </button>
                    <span>{{ formatDateTime(reply.createTime) }}</span>
                  </div>
                  <p>
                    <span class="reply-label">回复 {{ reply.parentNickname || comment.nickname || '朋友' }}</span>
                    {{ reply.content }}
                  </p>
                  <div class="comment-actions">
                    <button type="button" @click="toggleCommentLike(reply)">
                      <el-icon><StarFilled v-if="reply.likedByMe" /><Star v-else /></el-icon>
                      {{ reply.likeCount || 0 }}
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </article>
        </div>
      </section>
    </template>
  </main>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import {
  ArrowLeft,
  ChatDotRound,
  Delete,
  Edit,
  Star,
  StarFilled,
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import MarkdownContent from '../components/MarkdownContent.vue'
import { useUser } from '../composables/useUser'
import request, { apiData } from '../utils/request'
import { formatDate, formatDateTime } from '../utils/date'
import { mediaUrl } from '../utils/media'

const route = useRoute()
const router = useRouter()
const { token, userInfo, openAuth } = useUser()
const article = ref(null)
const comments = ref([])
const newComment = ref('')
const replyingTo = ref(null)
const isLoading = ref(false)
const commentsLoading = ref(false)
const isSubmitting = ref(false)
const likeLoading = ref(false)
const errorMessage = ref('')

const articleId = () => Number(route.params.id)
const authorInitial = computed(() => (article.value?.authorNickname || '用').slice(0, 1))
const isOwner = computed(() => (
  article.value
  && userInfo.value
  && Number(article.value.authorId) === Number(userInfo.value.id)
))

const topLevelComments = computed(() => comments.value.filter(comment => !comment.parentId))
const repliesFor = (parentId) => comments.value.filter(comment => Number(comment.parentId) === Number(parentId))

const loadComments = async () => {
  commentsLoading.value = true
  try {
    comments.value = apiData(await request.get('/comment/list', {
      params: { articleId: articleId() },
    })) || []
  } finally {
    commentsLoading.value = false
  }
}

const loadArticle = async () => {
  isLoading.value = true
  errorMessage.value = ''
  try {
    const [articleResponse] = await Promise.all([
      request.get(`/articles/${articleId()}`),
      loadComments(),
    ])
    article.value = apiData(articleResponse)
  } catch {
    article.value = null
    errorMessage.value = '文章不存在或暂时无法加载'
  } finally {
    isLoading.value = false
  }
}

const requireLogin = () => {
  if (token.value) return true
  openAuth('login', route.fullPath)
  return false
}

const submitComment = async () => {
  if (!requireLogin()) return
  const content = newComment.value.trim()
  if (!content) {
    ElMessage.warning('请先填写评论内容')
    return
  }

  isSubmitting.value = true
  try {
    const created = apiData(await request.post('/comment/add', {
      articleId: articleId(),
      parentId: replyingTo.value?.id || null,
      content,
    }))
    comments.value.push(created)
    newComment.value = ''
    replyingTo.value = null
    article.value.commentCount = comments.value.length
    ElMessage.success('评论成功')
  } finally {
    isSubmitting.value = false
  }
}

const startReply = async (comment) => {
  if (!requireLogin()) return
  replyingTo.value = comment
  await nextTick()
  document.querySelector('.comment-editor textarea')?.focus()
}

const cancelReply = () => {
  replyingTo.value = null
}

const toggleArticleLike = async () => {
  if (!requireLogin() || likeLoading.value) return
  likeLoading.value = true
  try {
    const data = apiData(await request.post('/like', null, {
      params: { articleId: articleId() },
    }))
    article.value.likedByMe = data.liked
    article.value.likeCount = data.likeCount
  } finally {
    likeLoading.value = false
  }
}

const toggleCommentLike = async (comment) => {
  if (!requireLogin()) return
  const data = apiData(await request.post(`/comment/${comment.id}/like`))
  comment.likedByMe = data.liked
  comment.likeCount = data.likeCount
}

const deleteArticle = async () => {
  try {
    await ElMessageBox.confirm(
      '删除后文章、评论和点赞都无法恢复，确认继续吗？',
      '删除文章',
      {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
      }
    )
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

const commentInitial = (comment) => (comment.nickname || '评').slice(0, 1)

watch(
  () => route.params.id,
  () => {
    article.value = null
    comments.value = []
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
.article-body,
.comments-panel {
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

.article-body > h1 {
  max-width: 780px;
  margin: 38px 0 30px;
  color: var(--text-strong);
  font-size: clamp(34px, 6vw, 54px);
  line-height: 1.16;
  letter-spacing: -0.03em;
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

.comments-panel {
  margin-top: 22px;
}

.comments-head {
  margin-bottom: 24px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
}

.comments-head h2 {
  margin: 7px 0 0;
  color: var(--text-strong);
}

.comments-head > b {
  color: var(--theme-pink);
  font-size: 24px;
}

.comment-editor {
  margin-bottom: 30px;
  padding: 18px;
  border-radius: 16px;
  background: var(--surface-soft);
}

.replying-banner {
  margin-bottom: 10px;
  color: var(--text-muted);
  font-size: 13px;
}

.replying-banner button {
  margin-left: 7px;
  border: 0;
  color: var(--theme-pink);
  background: transparent;
  cursor: pointer;
}

.editor-actions {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 14px;
}

.editor-actions span {
  color: var(--text-faint);
  font-size: 12px;
}

.login-to-comment {
  margin-bottom: 28px;
  padding: 22px;
  border-radius: 15px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: var(--accent-soft);
}

.login-to-comment p {
  margin: 0;
  color: var(--text-muted);
}

.comment-list {
  display: grid;
  gap: 24px;
}

.comment-thread {
  padding-top: 22px;
  border-top: 1px solid var(--border-soft);
}

.comment-item {
  display: flex;
  gap: 12px;
}

.comment-main {
  min-width: 0;
  flex: 1;
}

.comment-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.comment-name {
  padding: 0;
  border: 0;
  color: var(--text-strong);
  background: transparent;
  cursor: pointer;
  font-weight: 700;
}

.comment-name:hover,
.comment-name:focus-visible {
  color: var(--theme-pink);
}

.comment-top span {
  color: var(--text-faint);
  font-size: 12px;
}

.comment-main p {
  margin: 9px 0;
  color: var(--text-main);
  line-height: 1.75;
  white-space: pre-wrap;
}

.comment-actions {
  display: flex;
  gap: 12px;
}

.comment-actions button {
  padding: 3px 0;
  border: 0;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--text-faint);
  background: transparent;
  cursor: pointer;
  font-size: 12px;
}

.comment-actions button:hover {
  color: var(--theme-pink);
}

.reply-list {
  margin: 16px 0 0 50px;
  padding: 16px;
  border-radius: 14px;
  display: grid;
  gap: 18px;
  background: var(--surface-soft);
}

.reply-label {
  margin-right: 5px;
  color: var(--theme-pink);
  font-size: 12px;
  font-weight: 700;
}

@media (max-width: 640px) {
  .article-detail-page {
    width: min(100% - 24px, 920px);
  }

  .article-header,
  .login-to-comment {
    align-items: flex-start;
    flex-direction: column;
  }

  .owner-actions,
  .login-to-comment .el-button {
    width: 100%;
  }

  .owner-actions .el-button {
    flex: 1;
  }

  .comment-top {
    flex-direction: column;
    gap: 3px;
  }

  .reply-list {
    margin-left: 18px;
  }
}
</style>
