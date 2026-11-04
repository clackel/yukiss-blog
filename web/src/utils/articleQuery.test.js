import { describe, expect, it } from 'vitest'
import { buildArticleQuery, normalizeArticleQuery } from './articleQuery'

describe('Article query helpers', () => {
  it('normalizes invalid route values to safe defaults', () => {
    expect(normalizeArticleQuery({ q: '  Vue  ', sort: 'oldest', page: '-2', scope: 'mine' })).toEqual({
      keyword: 'Vue',
      tag: '',
      sort: 'published',
      scope: 'all',
      page: 1,
    })
  })

  it('round-trips the following scope and the views sort', () => {
    const query = buildArticleQuery({ scope: 'following', sort: 'views' })

    expect(query).toEqual({ scope: 'following', sort: 'views' })
    expect(normalizeArticleQuery(query)).toMatchObject({ scope: 'following', sort: 'views' })
    expect(buildArticleQuery({ scope: 'all' })).toEqual({})
  })

  it('omits default values from shareable URLs', () => {
    expect(buildArticleQuery({ keyword: '', sort: 'published', page: 1 })).toEqual({})
    expect(buildArticleQuery({ keyword: 'Spring', sort: 'comments', page: 3 })).toEqual({
      q: 'Spring',
      sort: 'comments',
      page: '3',
    })
  })

  it('round-trips the tag filter', () => {
    const query = buildArticleQuery({ keyword: '', tag: ' 前端 ', sort: 'published', page: 1 })

    expect(query).toEqual({ tag: '前端' })
    expect(normalizeArticleQuery(query).tag).toBe('前端')
  })

  it('keeps old shared links working', () => {
    expect(normalizeArticleQuery({ sort: 'latest' }).sort).toBe('published')
    expect(normalizeArticleQuery({ sort: 'popular' }).sort).toBe('likes')
  })
})
