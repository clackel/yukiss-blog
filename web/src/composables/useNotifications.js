import { ref } from 'vue'
import request, { apiData } from '../utils/request'

const POLL_INTERVAL_MS = 60 * 1000

// 模块级单例：导航栏角标和通知页共享同一个未读数
const unreadCount = ref(0)
let pollTimer = null

async function refreshUnreadCount() {
  if (!localStorage.getItem('token')) {
    unreadCount.value = 0
    return
  }
  try {
    const data = apiData(await request.get('/notifications/unread-count'))
    unreadCount.value = data?.count || 0
  } catch {
    // 轮询失败不打扰用户，下次再试
  }
}

function handleVisibilityChange() {
  if (document.visibilityState === 'visible') refreshUnreadCount()
}

// 登录后开始轮询；页面重新可见时立即刷新一次
function startPolling() {
  stopPolling()
  refreshUnreadCount()
  pollTimer = window.setInterval(refreshUnreadCount, POLL_INTERVAL_MS)
  document.addEventListener('visibilitychange', handleVisibilityChange)
}

function stopPolling() {
  if (pollTimer) window.clearInterval(pollTimer)
  pollTimer = null
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  unreadCount.value = 0
}

export function useNotifications() {
  return {
    unreadCount,
    refreshUnreadCount,
    startPolling,
    stopPolling,
  }
}
