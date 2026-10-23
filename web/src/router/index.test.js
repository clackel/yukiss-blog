import { afterEach, describe, expect, it } from 'vitest'
import { authGuard, routes } from './index'

const protectedRoute = {
  fullPath: '/editor/12',
  matched: [{ meta: { requiresAuth: true } }],
}

const publicRoute = {
  fullPath: '/community',
  matched: [{ meta: { public: true } }],
}

afterEach(() => localStorage.clear())

describe('Route authentication guard', () => {
  it('keeps public reading available to visitors', () => {
    expect(authGuard(publicRoute)).toBeUndefined()
  })

  it('redirects anonymous writers to the global login flow', () => {
    expect(authGuard(protectedRoute)).toEqual({
      path: '/',
      query: {
        login: '1',
        redirect: '/editor/12',
      },
    })
  })

  it('keeps personal pages behind login', () => {
    const privateNames = routes.filter((route) => route.meta?.requiresAuth).map((route) => route.name)
    expect(privateNames).toEqual(
      expect.arrayContaining(['home', 'article-create', 'article-edit', 'profile', 'notifications'])
    )
  })

  it('allows authenticated users into private pages', () => {
    localStorage.setItem('token', 'valid-token')
    expect(authGuard(protectedRoute)).toBeUndefined()
  })
})
