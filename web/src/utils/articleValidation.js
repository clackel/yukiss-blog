export const ARTICLE_TITLE_LIMIT = 80
export const ARTICLE_CONTENT_LIMIT = 50000
export const ARTICLE_TAG_LIMIT = 5
export const ARTICLE_TAG_LENGTH = 20

// 与后端 ArticleServiceImpl 的校验规则保持一致；草稿允许正文为空
export function validateArticleDraft({ title = '', content = '', tags = [], status = 'published' } = {}) {
  const normalizedTitle = title.trim()
  const normalizedContent = content.trim()
  if (!normalizedTitle) return '请填写文章标题'
  if (normalizedTitle.length > ARTICLE_TITLE_LIMIT) return `文章标题不能超过 ${ARTICLE_TITLE_LIMIT} 个字符`
  if (status === 'published' && !normalizedContent) return '请填写文章正文'
  if (normalizedContent.length > ARTICLE_CONTENT_LIMIT) return `文章正文不能超过 ${ARTICLE_CONTENT_LIMIT} 个字符`
  if (tags.length > ARTICLE_TAG_LIMIT) return `每篇文章最多 ${ARTICLE_TAG_LIMIT} 个标签`
  if (tags.some((tag) => tag.length > ARTICLE_TAG_LENGTH)) return `标签不能超过 ${ARTICLE_TAG_LENGTH} 个字符`
  if (tags.some((tag) => tag.includes(','))) return '标签不能包含逗号'
  return ''
}
