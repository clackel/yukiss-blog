export const ARTICLE_TITLE_LIMIT = 80
export const ARTICLE_CONTENT_LIMIT = 50000

export function validateArticleDraft({ title = '', content = '' } = {}) {
  const normalizedTitle = title.trim()
  const normalizedContent = content.trim()
  if (!normalizedTitle) return '请填写文章标题'
  if (normalizedTitle.length > ARTICLE_TITLE_LIMIT) return `文章标题不能超过 ${ARTICLE_TITLE_LIMIT} 个字符`
  if (!normalizedContent) return '请填写文章正文'
  if (normalizedContent.length > ARTICLE_CONTENT_LIMIT) return `文章正文不能超过 ${ARTICLE_CONTENT_LIMIT} 个字符`
  return ''
}
