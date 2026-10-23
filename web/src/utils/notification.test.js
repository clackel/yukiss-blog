import { describe, expect, it } from 'vitest'
import { notificationLink, notificationText } from './notification'

describe('Notification helpers', () => {
  it('links comment notifications to the comment anchor', () => {
    expect(notificationLink({ type: 'reply', articleId: 3, commentId: 12 })).toBe('/articles/3#comment-12')
    expect(notificationLink({ type: 'article_like', articleId: 3, commentId: null })).toBe('/articles/3')
  })

  it('links follow notifications to the follower unless the account is gone', () => {
    expect(notificationLink({ type: 'follow', actorId: 9, actorDeleted: false })).toBe('/users/9')
    expect(notificationLink({ type: 'follow', actorId: 9, actorDeleted: true })).toBeNull()
  })

  it('has a readable fallback for unknown types', () => {
    expect(notificationText({ type: 'comment' })).toBe('评论了你的文章')
    expect(notificationText({ type: 'something-new' })).toBe('与你有新的互动')
  })
})
