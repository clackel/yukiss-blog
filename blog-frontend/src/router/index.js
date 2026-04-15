import { createRouter, createWebHistory } from 'vue-router'
// 这三个页面组件我们马上就去创建
import Home from '../views/Home.vue' 
import Community from '../views/Community.vue'
import Profile from '../views/Profile.vue'

const routes = [
  { path: '/', component: Home },               // 默认主页：看自己的博客
  { path: '/community', component: Community }, // 社区：看所有人的博客
  { path: '/profile', component: Profile }      // 个人主页：改头像和资料
]

export const router = createRouter({
  history: createWebHistory(),
  routes
})