// 通知类型到文案和跳转地址的映射，与后端 Notification.TYPE_* 保持一致
const ACTION_TEXT = {
  comment: '评论了你的文章',
  reply: '回复了你的评论',
  article_like: '赞了你的文章',
  comment_like: '赞了你的评论',
  follow: '关注了你',
}

export function notificationText(notification) {
  return ACTION_TEXT[notification.type] || '与你有新的互动'
}

export function notificationLink(notification) {
  if (notification.type === 'follow') {
    return notification.actorDeleted ? null : `/users/${notification.actorId}`
  }
  if (!notification.articleId) return null
  const hash = notification.commentId ? `#comment-${notification.commentId}` : ''
  return `/articles/${notification.articleId}${hash}`
}
