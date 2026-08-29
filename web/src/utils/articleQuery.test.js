import { describe, expect, it } from 'vitest'
import { buildArticleQuery, normalizeArticleQuery } from './articleQuery'

describe('Article query helpers', () => {
  it('normalizes invalid route values to safe defaults', () => {
    expect(normalizeArticleQuery({ q: '  Vue  ', sort: 'oldest', page: '-2' })).toEqual({
      keyword: 'Vue',
      sort: 'published',
      page: 1,
    })
  })

  it('omits default values from shareable URLs', () => {
    expect(buildArticleQuery({ keyword: '', sort: 'published', page: 1 })).toEqual({})
    expect(buildArticleQuery({ keyword: 'Spring', sort: 'comments', page: 3 })).toEqual({
      q: 'Spring',
      sort: 'comments',
      page: '3',
    })
  })

  it('keeps old shared links working', () => {
    expect(normalizeArticleQuery({ sort: 'latest' }).sort).toBe('published')
    expect(normalizeArticleQuery({ sort: 'popular' }).sort).toBe('likes')
  })
})
