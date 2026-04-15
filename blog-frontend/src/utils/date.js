// src/utils/date.js
export function formatDate(dateStr) {
  if (!dateStr) return '未知时间'
  return new Date(dateStr).toLocaleDateString()
}

export function getDaysSince(dateStr) {
  if (!dateStr) return 0
  const start = new Date(dateStr)
  const now = new Date()
  return Math.floor((now - start) / (1000 * 60 * 60 * 24))
}
