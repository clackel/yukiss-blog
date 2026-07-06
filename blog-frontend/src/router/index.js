import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', component: () => import('../views/Home.vue') },
  { path: '/community', component: () => import('../views/Community.vue') },
  { path: '/profile', component: () => import('../views/Profile.vue') },
]

export const router = createRouter({
  history: createWebHistory(),
  routes,
})
