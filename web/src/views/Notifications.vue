<template>
  <main class="page-shell notifications-page">
    <div class="feed-toolbar glass-card">
      <div>
        <span class="section-kicker">消息</span>
        <h1 class="section-title">通知</h1>
        <p class="section-desc">评论、回复、点赞和关注都会出现在这里。</p>
      </div>
      <el-button plain round :disabled="!unreadCount" :loading="isMarkingAll" @click="markAllRead">
        全部标为已读
      </el-button>
    </div>

    <el-skeleton v-if="isLoading" animated :rows="8" class="glass-card list-card" />

    <el-empty v-else-if="errorMessage" class="glass-card empty-state" :description="errorMessage">
      <el-button type="primary" plain @click="loadPage">重新加载</el-button>
    </el-empty>

    <el-empty v-else-if="!pageData.items.length" class="glass-card empty-state" description="暂时没有新消息" />

    <ul v-else class="glass-card list-card notification-list">
      <li
        v-for="item in pageData.items"
        :key="item.id"
        :class="{ unread: !item.read, clickable: Boolean(notificationLink(item)) }"
        @click="openNotification(item)"
      >
        <el-avatar :size="40" :src="mediaUrl(item.actorAvatar)">
          {{ (item.actorNickname || '友').slice(0, 1) }}
        </el-avatar>
        <div class="notification-body">
          <p>
            <b>{{ item.actorNickname || '有人' }}</b>
            {{ notificationText(item) }}
            <span v-if="item.articleTitle" class="article-title">《{{ item.articleTitle }}》</span>
          </p>
          <blockquote v-if="item.commentContent">{{ item.commentContent }}</blockquote>
          <time>{{ formatDateTime(item.createTime) }}</time>
        </div>
        <span v-if="!item.read" class="unread-dot" aria-label="未读" />
      </li>
    </ul>

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
import { useRouter } from 'vue-router'
import { useNotifications } from '../composables/useNotifications'
import { formatDateTime } from '../utils/date'
import { mediaUrl } from '../utils/media'
import { notificationLink, notificationText } from '../utils/notification'
import request, { apiData } from '../utils/request'

const router = useRouter()
const { unreadCount, refreshUnreadCount } = useNotifications()
const isLoading = ref(false)
const isMarkingAll = ref(false)
const errorMessage = ref('')
const pageData = reactive({ items: [], total: 0, page: 1, pageSize: 20, totalPages: 0 })

const loadPage = async () => {
  isLoading.value = true
  errorMessage.value = ''
  try {
    const data = apiData(
      await request.get('/notifications', {
        params: { page: pageData.page, pageSize: pageData.pageSize },
      })
    )
    Object.assign(pageData, data)
  } catch {
    errorMessage.value = '通知加载失败'
  } finally {
    isLoading.value = false
  }
  refreshUnreadCount()
}

const changePage = (value) => {
  pageData.page = value
  loadPage()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const openNotification = async (item) => {
  if (!item.read) {
    item.read = true
    unreadCount.value = Math.max(unreadCount.value - 1, 0)
    request.post(`/notifications/${item.id}/read`).catch(() => {})
  }
  const link = notificationLink(item)
  if (link) await router.push(link)
}

const markAllRead = async () => {
  isMarkingAll.value = true
  try {
    await request.post('/notifications/read-all')
    pageData.items.forEach((item) => {
      item.read = true
    })
    unreadCount.value = 0
  } finally {
    isMarkingAll.value = false
  }
}

onMounted(loadPage)
</script>

<style scoped>
.notifications-page {
  width: min(860px, calc(100% - 40px));
}

.feed-toolbar {
  margin-bottom: 18px;
}

.list-card {
  padding: 8px 0;
}

.notification-list {
  margin: 0;
  list-style: none;
}

.notification-list li {
  padding: 16px 24px;
  display: flex;
  align-items: flex-start;
  gap: 14px;
  position: relative;
}

.notification-list li + li {
  border-top: 1px solid var(--border-soft);
}

.notification-list li.clickable {
  cursor: pointer;
}

.notification-list li.clickable:hover {
  background: var(--surface-soft);
}

.notification-list li.unread {
  background: color-mix(in srgb, var(--accent-soft) 60%, transparent);
}

.notification-body {
  min-width: 0;
  flex: 1;
}

.notification-body p {
  margin: 2px 0 6px;
  color: var(--text-main);
  line-height: 1.6;
}

.notification-body b {
  color: var(--text-strong);
}

.article-title {
  color: var(--theme-pink);
}

.notification-body blockquote {
  margin: 0 0 6px;
  padding: 8px 12px;
  border-left: 3px solid var(--border-soft);
  border-radius: 6px;
  color: var(--text-muted);
  background: var(--surface-soft);
  font-size: 13px;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notification-body time {
  color: var(--text-faint);
  font-size: 12px;
}

.unread-dot {
  width: 8px;
  height: 8px;
  margin-top: 8px;
  border-radius: 50%;
  flex: 0 0 auto;
  background: var(--theme-pink);
}

.pagination-wrap {
  padding: 24px 0 8px;
  display: flex;
  justify-content: center;
}

@media (max-width: 640px) {
  .notifications-page {
    width: min(100% - 24px, 860px);
  }

  .notification-list li {
    padding: 14px 16px;
  }
}
</style>
