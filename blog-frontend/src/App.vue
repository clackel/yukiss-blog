<template>
  <div class="app-wrapper">
    <div class="top-nav">
      <div class="nav-content">
        <div class="logo">
          <span class="mizuki-text">Yukiss</span>
        </div>
        
        <el-menu 
          :default-active="$route.path" 
          class="glass-menu" 
          mode="horizontal" 
          router
          :ellipsis="false"
        >
          <el-menu-item index="/">
            <el-icon><HomeFilled /></el-icon> 主页 (我的)
          </el-menu-item>
          <el-menu-item index="/community">
            <el-icon><Menu /></el-icon> 社区 (发现)
          </el-menu-item>
          <el-menu-item index="/profile">
            <el-icon><User /></el-icon> 个人资料
          </el-menu-item>
        </el-menu>

        <div class="nav-tools">
          <el-icon><Search /></el-icon>
          <el-icon><Sunny /></el-icon>
        </div>
      </div>
    </div>

    <router-view v-slot="{ Component }">
      <transition name="fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </div>
</template>

<script setup>
import { HomeFilled, Menu, User, Search, Sunny } from '@element-plus/icons-vue'
</script>

<style>
/* 整个应用的底色 */
body { margin: 0; background-color: #f4f5f7; }

/* 导航栏外壳：固定在顶部，毛玻璃效果 */
.top-nav {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 60px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(12px) saturate(180%); /* 苹果风玻璃模糊 */
  border-bottom: 1px solid rgba(255, 255, 255, 0.4);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
  z-index: 999; /* 保证在最上层 */
  display: flex;
  justify-content: center;
}

/* 导航栏内容区约束宽度 */
.nav-content {
  width: 100%;
  max-width: 1300px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}

/* Logo 样式 */
.logo .mizuki-text {
  font-size: 24px;
  font-weight: 900;
  color: transparent;
  background: linear-gradient(120deg, #ff6bb1, #ffa8c9);
  background-clip: text;
  -webkit-background-clip: text;
  letter-spacing: 2px;
}

/* Element Menu 去除自带底色和边框 */
.glass-menu {
  background: transparent !important;
  border-bottom: none !important;
}
.glass-menu .el-menu-item {
  background: transparent !important;
  font-weight: bold;
  color: #555;
}
.glass-menu .el-menu-item:hover, .glass-menu .el-menu-item.is-active {
  color: #ff6bb1 !important;
  background: transparent !important;
}
.glass-menu .el-menu-item.is-active {
  border-bottom: 3px solid #ff6bb1 !important;
}

/* 右侧图标 */
.nav-tools {
  display: flex;
  gap: 15px;
  font-size: 20px;
  color: #666;
  cursor: pointer;
}
.nav-tools .el-icon:hover { color: #ff6bb1; }

/* 页面切换的淡入淡出动画 */
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>