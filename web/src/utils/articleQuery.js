export function normalizeArticleQuery(query = {}) {
  const parsedPage = Number(query.page)
  const sortAliases = {
    latest: 'published',
    popular: 'likes',
  }
  const requestedSort = sortAliases[query.sort] || query.sort
  const allowedSorts = ['published', 'commented', 'likes', 'comments']
  return {
    keyword: typeof query.q === 'string' ? query.q.trim().slice(0, 80) : '',
    tag: typeof query.tag === 'string' ? query.tag.trim().slice(0, 20) : '',
    sort: allowedSorts.includes(requestedSort) ? requestedSort : 'published',
    page: Number.isInteger(parsedPage) && parsedPage > 0 ? parsedPage : 1,
  }
}

export function buildArticleQuery({ keyword = '', tag = '', sort = 'published', page = 1 } = {}) {
  const query = {}
  const normalizedKeyword = keyword.trim()
  const normalizedTag = tag.trim()
  if (normalizedKeyword) query.q = normalizedKeyword
  if (normalizedTag) query.tag = normalizedTag
  if (sort !== 'published') query.sort = sort
  if (page > 1) query.page = String(page)
  return query
}
