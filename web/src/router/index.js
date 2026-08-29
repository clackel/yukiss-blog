import { createRouter, createWebHistory } from 'vue-router'

export const routes = [
  { path: '/', name: 'landing', component: () => import('../views/Landing.vue'), meta: { public: true } },
  { path: '/community', name: 'community', component: () => import('../views/Community.vue'), meta: { public: true } },
  { path: '/articles/:id', name: 'article-detail', component: () => import('../views/ArticleDetail.vue'), meta: { public: true } },
  { path: '/home', name: 'home', component: () => import('../views/Home.vue'), meta: { requiresAuth: true } },
  { path: '/editor', name: 'article-create', component: () => import('../views/ArticleEditor.vue'), meta: { requiresAuth: true } },
  { path: '/editor/:id', name: 'article-edit', component: () => import('../views/ArticleEditor.vue'), meta: { requiresAuth: true } },
  { path: '/profile', name: 'profile', component: () => import('../views/Profile.vue'), meta: { requiresAuth: true } },
  { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('../views/NotFound.vue'), meta: { public: true } },
]

export function authGuard(to) {
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth)
  const hasToken = Boolean(localStorage.getItem('token'))
  if (requiresAuth && !hasToken) {
    return {
      path: '/',
      query: {
        login: '1',
        redirect: to.fullPath,
      },
    }
  }
}

export function createAppRouter(history = createWebHistory()) {
  const appRouter = createRouter({ history, routes })
  appRouter.beforeEach(authGuard)

  return appRouter
}

export const router = createAppRouter()
