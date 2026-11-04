// 与后端 utils/Mentions 一致：@ 前后不能紧挨字母数字，用户名 3-20 位字母、数字、下划线
const MENTION_PATTERN = /(?<![A-Za-z0-9_])@([A-Za-z0-9_]{3,20})(?![A-Za-z0-9_])/g

/**
 * 把评论正文按 @username 切成片段，只有后端确认存在的用户（mentions）才渲染成链接。
 * 返回 [{ type: 'text', text }, { type: 'mention', text, user }]
 */
export function splitMentions(content, mentions = []) {
  const text = content || ''
  const byUsername = new Map((mentions || []).map((user) => [String(user.username).toLowerCase(), user]))
  if (!byUsername.size) return text ? [{ type: 'text', text }] : []

  const segments = []
  let cursor = 0
  for (const match of text.matchAll(MENTION_PATTERN)) {
    const user = byUsername.get(match[1].toLowerCase())
    if (!user) continue
    if (match.index > cursor) segments.push({ type: 'text', text: text.slice(cursor, match.index) })
    segments.push({ type: 'mention', text: match[0], user })
    cursor = match.index + match[0].length
  }
  if (cursor < text.length) segments.push({ type: 'text', text: text.slice(cursor) })
  return segments
}

/** 评论区 @ 自动补全的候选：去重、去掉自己和已注销用户 */
export function mentionCandidates(users, currentUsername = '') {
  const seen = new Set()
  const result = []
  for (const user of users || []) {
    const username = user?.username
    if (!username || username === currentUsername || seen.has(username)) continue
    seen.add(username)
    result.push({ value: username, label: user.nickname || username })
  }
  return result
}
