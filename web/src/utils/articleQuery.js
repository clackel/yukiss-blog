export function normalizeArticleQuery(query = {}) {
  const parsedPage = Number(query.page)
  return {
    keyword: typeof query.q === 'string' ? query.q.trim().slice(0, 80) : '',
    sort: query.sort === 'popular' ? 'popular' : 'latest',
    page: Number.isInteger(parsedPage) && parsedPage > 0 ? parsedPage : 1,
  }
}

export function buildArticleQuery({ keyword = '', sort = 'latest', page = 1 } = {}) {
  const query = {}
  const normalizedKeyword = keyword.trim()
  if (normalizedKeyword) query.q = normalizedKeyword
  if (sort === 'popular') query.sort = 'popular'
  if (page > 1) query.page = String(page)
  return query
}
