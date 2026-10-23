<template>
  <section class="glass-card comments-panel">
    <div class="comments-head">
      <div>
        <span class="section-kicker">评论</span>
        <h2>评论区</h2>
      </div>
      <b>{{ totalCount }}</b>
    </div>

    <p v-if="!interactive" class="comments-disabled">草稿发布后才能评论和点赞。</p>

    <div v-else-if="token" class="comment-editor">
      <div v-if="replyingTo" class="replying-banner">
        正在回复 {{ replyingTo.nickname || '匿名用户' }}
        <button type="button" @click="replyingTo = null">取消</button>
      </div>
      <el-input
        ref="editorInput"
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
      <el-button type="primary" round class="anime-btn" @click="openAuth('login', route.fullPath)">
        登录参与讨论
      </el-button>
    </div>

    <el-skeleton v-if="isLoading" animated :rows="5" />

    <el-empty v-else-if="loadError" :description="loadError">
      <el-button type="primary" plain @click="loadFirstPage">重新加载</el-button>
    </el-empty>

    <el-empty v-else-if="!comments.length" description="还没有评论，来写第一条吧" />

    <div v-else class="comment-list">
      <article v-for="comment in comments" :key="comment.id" class="comment-thread">
        <CommentItem
          :comment="comment"
          :current-user-id="currentUserId"
          :article-author-id="articleAuthorId"
          :interactive="interactive"
          @like="toggleLike"
          @reply="startReply"
          @delete="deleteComment"
          @update="Object.assign(comment, $event)"
          @open-user="goUserProfile"
        />

        <div v-if="comment.replies?.length" class="reply-list">
          <CommentItem
            v-for="reply in comment.replies"
            :key="reply.id"
            :comment="reply"
            is-reply
            :parent-nickname="comment.nickname"
            :current-user-id="currentUserId"
            :article-author-id="articleAuthorId"
            :interactive="interactive"
            @like="toggleLike"
            @delete="deleteComment"
            @update="Object.assign(reply, $event)"
            @open-user="goUserProfile"
          />
        </div>
      </article>

      <div v-if="hasMore" class="load-more">
        <el-button round :loading="isLoadingMore" @click="loadNextPage">
          加载更多评论（还有 {{ remainingTopLevel }} 条）
        </el-button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import CommentItem from './CommentItem.vue'
import { useUser } from '../composables/useUser'
import request, { apiData } from '../utils/request'

const PAGE_SIZE = 10

const props = defineProps({
  articleId: { type: Number, required: true },
  articleAuthorId: { type: Number, default: null },
  // 文章评论总数（含回复），由父组件维护
  totalCount: { type: Number, default: 0 },
  interactive: { type: Boolean, default: true },
})

const emit = defineEmits(['count-change'])

const route = useRoute()
const router = useRouter()
const { token, userInfo, openAuth } = useUser()

const comments = ref([])
const page = ref(0)
const totalTopLevel = ref(0)
const isLoading = ref(false)
const isLoadingMore = ref(false)
const loadError = ref('')
const newComment = ref('')
const replyingTo = ref(null)
const isSubmitting = ref(false)
const editorInput = ref(null)

const currentUserId = computed(() => (userInfo.value?.id ? Number(userInfo.value.id) : null))
const hasMore = computed(() => comments.value.length < totalTopLevel.value)
const remainingTopLevel = computed(() => Math.max(totalTopLevel.value - comments.value.length, 0))

const fetchPage = async (pageNumber) =>
  apiData(
    await request.get('/comment/list', {
      params: { articleId: props.articleId, page: pageNumber, pageSize: PAGE_SIZE },
    })
  )

// 合并新一页时按 id 去重：刚发表的评论可能已经插在本地列表末尾
const mergeComments = (items) => {
  const existing = new Set(comments.value.map((comment) => comment.id))
  comments.value.push(...items.filter((comment) => !existing.has(comment.id)))
}

const loadFirstPage = async () => {
  isLoading.value = true
  loadError.value = ''
  try {
    const data = await fetchPage(1)
    comments.value = data.items || []
    totalTopLevel.value = data.total
    page.value = 1
  } catch {
    loadError.value = '评论加载失败'
  } finally {
    isLoading.value = false
  }
  scrollToHashComment()
}

// 从通知跳转过来时（#comment-123）定位到对应评论；不在第一页时停在评论区
const scrollToHashComment = async () => {
  if (!route.hash.startsWith('#comment-')) return
  await nextTick()
  const target = document.querySelector(route.hash) || document.querySelector('.comments-panel')
  target?.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

const loadNextPage = async () => {
  isLoadingMore.value = true
  try {
    const data = await fetchPage(page.value + 1)
    mergeComments(data.items || [])
    totalTopLevel.value = data.total
    page.value += 1
  } finally {
    isLoadingMore.value = false
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
    const created = apiData(
      await request.post('/comment/add', {
        articleId: props.articleId,
        parentId: replyingTo.value?.id || null,
        content,
      })
    )
    if (created.parentId) {
      const parent = comments.value.find((comment) => comment.id === created.parentId)
      parent?.replies.push(created)
    } else {
      comments.value.push({ ...created, replies: created.replies || [] })
      totalTopLevel.value += 1
    }
    newComment.value = ''
    replyingTo.value = null
    emit('count-change', 1)
    ElMessage.success('评论成功')
  } finally {
    isSubmitting.value = false
  }
}

const startReply = async (comment) => {
  if (!requireLogin()) return
  replyingTo.value = comment
  await nextTick()
  editorInput.value?.focus()
}

const toggleLike = async (comment) => {
  if (!requireLogin()) return
  const data = apiData(await request.post(`/comment/${comment.id}/like`))
  comment.likedByMe = data.liked
  comment.likeCount = data.likeCount
}

const deleteComment = async (comment) => {
  const hasReplies = !comment.parentId && comment.replies?.length
  try {
    await ElMessageBox.confirm(
      hasReplies ? '删除后这条评论下的所有回复也会一起删除，确认继续吗？' : '确认删除这条评论吗？',
      '删除评论',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }

  const data = apiData(await request.delete(`/comment/${comment.id}`))
  if (comment.parentId) {
    const parent = comments.value.find((item) => item.id === comment.parentId)
    if (parent) parent.replies = parent.replies.filter((reply) => reply.id !== comment.id)
  } else {
    comments.value = comments.value.filter((item) => item.id !== comment.id)
    totalTopLevel.value = Math.max(totalTopLevel.value - 1, 0)
  }
  if (replyingTo.value?.id === comment.id) replyingTo.value = null
  emit('count-change', -(data?.deletedCount || 1))
  ElMessage.success('评论已删除')
}

const goUserProfile = (userId) => {
  if (userId) router.push(`/users/${userId}`)
}

watch(
  () => props.articleId,
  () => {
    comments.value = []
    replyingTo.value = null
    loadFirstPage()
  },
  { immediate: true }
)
</script>

<style scoped>
.comments-panel {
  margin-top: 22px;
  padding: clamp(24px, 5vw, 48px);
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

.comments-disabled {
  margin: 0 0 24px;
  padding: 16px 18px;
  border-radius: 14px;
  color: var(--text-muted);
  background: var(--surface-soft);
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

.reply-list {
  margin: 16px 0 0 50px;
  padding: 16px;
  border-radius: 14px;
  display: grid;
  gap: 18px;
  background: var(--surface-soft);
}

.load-more {
  display: flex;
  justify-content: center;
}

@media (max-width: 640px) {
  .login-to-comment {
    align-items: flex-start;
    flex-direction: column;
  }

  .login-to-comment .el-button {
    width: 100%;
  }

  .reply-list {
    margin-left: 18px;
  }
}
</style>
