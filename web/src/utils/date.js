// src/utils/date.js
export function formatDate(dateStr) {
  if (!dateStr) return '未知时间'
  const date = new Date(dateStr)
  if (Number.isNaN(date.getTime())) return '未知时间'
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}

export function formatDateTime(dateStr) {
  if (!dateStr) return '未知时间'
  const normalized = typeof dateStr === 'string' && !dateStr.includes('T')
    ? dateStr.replace(' ', 'T')
    : dateStr
  const date = new Date(normalized)
  if (Number.isNaN(date.getTime())) return '未知时间'
  return date.toLocaleString('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function getDaysSince(dateStr) {
  if (!dateStr) return 0
  const start = new Date(dateStr)
  const now = new Date()
  return Math.floor((now - start) / (1000 * 60 * 60 * 24))
}
