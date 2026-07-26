import { describe, expect, it } from 'vitest'
import {
  ARTICLE_CONTENT_LIMIT,
  ARTICLE_TITLE_LIMIT,
  validateArticleDraft,
} from './articleValidation'

describe('Article draft validation', () => {
  it('requires a title and body', () => {
    expect(validateArticleDraft({ title: ' ', content: '正文' })).toBe('请填写文章标题')
    expect(validateArticleDraft({ title: '标题', content: ' ' })).toBe('请填写文章正文')
  })

  it('enforces the shared article limits', () => {
    expect(validateArticleDraft({ title: 'x'.repeat(ARTICLE_TITLE_LIMIT + 1), content: '正文' }))
      .toContain('80')
    expect(validateArticleDraft({ title: '标题', content: 'x'.repeat(ARTICLE_CONTENT_LIMIT + 1) }))
      .toContain('50000')
  })

  it('accepts a valid Markdown draft', () => {
    expect(validateArticleDraft({ title: '一篇文章', content: '# 标题\n\n正文' })).toBe('')
  })
})
