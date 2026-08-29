import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', component: () => import('../views/Landing.vue'), meta: { public: true } },
  { path: '/home', component: () => import('../views/Home.vue'), meta: { requiresAuth: true } },
  { path: '/community', component: () => import('../views/Community.vue'), meta: { requiresAuth: true } },
  { path: '/articles/:id', component: () => import('../views/ArticleDetail.vue'), meta: { requiresAuth: true } },
  { path: '/profile', component: () => import('../views/Profile.vue'), meta: { requiresAuth: true } },
]

export const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const hasToken = Boolean(localStorage.getItem('token'))
  if (to.meta.requiresAuth && !hasToken) {
    return '/'
  }
  if (to.path === '/' && hasToken) {
    return '/home'
  }
})
