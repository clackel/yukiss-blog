<template>
  <div class="app-wrapper">
    <header class="top-nav">
      <div class="nav-content">
        <RouterLink class="logo" to="/" aria-label="返回 Yukiss 首页">
          <span class="logo-mark">Y</span>
          <span class="mizuki-text">Yukiss</span>
        </RouterLink>

        <nav class="desktop-nav" aria-label="主导航">
          <RouterLink to="/community">发现</RouterLink>
          <RouterLink v-if="token" to="/home">我的文章</RouterLink>
          <RouterLink v-if="token" to="/editor">写文章</RouterLink>
          <RouterLink v-if="token" to="/profile">个人资料</RouterLink>
        </nav>

        <div class="nav-tools">
          <button class="icon-button" type="button" title="搜索文章" aria-label="搜索文章" @click="goSearch">
            <el-icon><Search /></el-icon>
          </button>

          <el-dropdown trigger="click" @command="setTheme">
            <button
              class="icon-button"
              type="button"
              :title="`主题：${themeLabel}`"
              :aria-label="`主题：${themeLabel}`"
            >
              <el-icon>
                <Moon v-if="effectiveTheme === 'dark'" />
                <Sunny v-else />
              </el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="system" :disabled="themeMode === 'system'">跟随系统</el-dropdown-item>
                <el-dropdown-item command="light" :disabled="themeMode === 'light'">浅色模式</el-dropdown-item>
                <el-dropdown-item command="dark" :disabled="themeMode === 'dark'">深色模式</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <el-dropdown class="mobile-menu" trigger="click" @command="handleNavigation">
            <button class="icon-button" type="button" aria-label="打开导航菜单">
              <el-icon><Menu /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="/community">发现</el-dropdown-item>
                <el-dropdown-item v-if="token" command="/home">我的文章</el-dropdown-item>
                <el-dropdown-item v-if="token" command="/editor">写文章</el-dropdown-item>
                <el-dropdown-item v-if="token" command="/profile">个人资料</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <el-dropdown v-if="token" trigger="click" @command="handleAccountCommand">
            <button class="user-button" type="button">
              <el-avatar :size="32" :src="mediaUrl(userInfo?.avatar)">
                {{ userInitial }}
              </el-avatar>
              <span>{{ userInfo?.nickname || userInfo?.username || '我的' }}</span>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="public-profile">我的主页</el-dropdown-item>
                <el-dropdown-item command="profile">个人资料</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button v-else type="primary" round class="nav-login-btn" @click="openAuth('login', route.fullPath)">
            登录
          </el-button>
        </div>
      </div>
    </header>

    <RouterView v-slot="{ Component }">
      <Transition name="page-fade" mode="out-in">
        <component :is="Component" />
      </Transition>
    </RouterView>

    <AuthDialog />
  </div>
</template>

<script setup>
import { computed, onMounted, watch } from 'vue'
import { Menu, Moon, Search, Sunny } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import AuthDialog from './components/AuthDialog.vue'
import { useTheme } from './composables/useTheme'
import { useUser } from './composables/useUser'
import { mediaUrl } from './utils/media'

const route = useRoute()
const router = useRouter()
const { token, userInfo, openAuth, doLogout, refreshMe } = useUser()
const { themeMode, effectiveTheme, setTheme } = useTheme()

const userInitial = computed(() => {
  const label = userInfo.value?.nickname || userInfo.value?.username || '友'
  return label.slice(0, 1)
})

const themeLabel = computed(() => ({
  system: '跟随系统',
  light: '浅色模式',
  dark: '深色模式',
})[themeMode.value])

const goSearch = () => {
  router.push({ path: '/community', query: { focus: 'search' } })
}

const handleNavigation = (path) => {
  router.push(path)
}

const handleAccountCommand = (command) => {
  if (command === 'public-profile') {
    if (userInfo.value?.id) router.push(`/users/${userInfo.value.id}`)
    return
  }
  if (command === 'profile') {
    router.push('/profile')
    return
  }
  if (command === 'logout') {
    doLogout()
    router.push('/')
  }
}

watch(
  () => [route.query.login, route.query.redirect],
  ([login, redirect]) => {
    if (login === '1' && !token.value) {
      openAuth('login', typeof redirect === 'string' ? redirect : '')
    }
  },
  { immediate: true }
)

watch(token, (value) => {
  if (!value && route.meta.requiresAuth) {
    router.replace('/')
  }
})

onMounted(async () => {
  if (!token.value) return
  try {
    await refreshMe()
  } catch {
    if (route.meta.requiresAuth) {
      await router.replace('/')
    }
  }
})
</script>

<style scoped>
.top-nav {
  position: fixed;
  inset: 0 0 auto;
  height: var(--nav-height);
  z-index: 1000;
  border-bottom: 1px solid var(--border-soft);
  background: color-mix(in srgb, var(--surface) 82%, transparent);
  backdrop-filter: blur(18px) saturate(160%);
}

.nav-content {
  width: min(1180px, calc(100% - 32px));
  height: 100%;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 28px;
}

.logo {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  text-decoration: none;
}

.logo-mark {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  color: white;
  background: linear-gradient(135deg, var(--theme-pink), var(--theme-violet));
  box-shadow: 0 7px 16px var(--theme-glow);
  font-weight: 900;
}

.mizuki-text {
  color: var(--text-strong);
  font-size: 21px;
  font-weight: 900;
  letter-spacing: 1px;
}

.desktop-nav {
  display: flex;
  align-items: center;
  gap: 6px;
}

.desktop-nav a {
  padding: 9px 13px;
  border-radius: 11px;
  color: var(--text-muted);
  text-decoration: none;
  font-size: 14px;
  font-weight: 700;
}

.desktop-nav a:hover,
.desktop-nav a.router-link-active {
  color: var(--theme-pink);
  background: var(--accent-soft);
}

.nav-tools {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
}

.icon-button,
.user-button {
  border: 0;
  color: var(--text-muted);
  background: transparent;
  cursor: pointer;
}

.icon-button {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  font-size: 19px;
}

.icon-button:hover {
  color: var(--theme-pink);
  background: var(--accent-soft);
}

.user-button {
  padding: 3px 8px 3px 3px;
  border-radius: 18px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
}

.user-button:hover {
  background: var(--accent-soft);
}

.mobile-menu {
  display: none;
}

.nav-login-btn {
  min-width: 76px;
}

.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.page-fade-enter-from,
.page-fade-leave-to {
  opacity: 0;
  transform: translateY(4px);
}

@media (max-width: 820px) {
  .desktop-nav {
    display: none;
  }

  .mobile-menu {
    display: inline-flex;
  }

  .user-button span {
    display: none;
  }
}

@media (max-width: 480px) {
  .nav-content {
    width: min(100% - 20px, 1180px);
    gap: 8px;
  }

  .mizuki-text {
    display: none;
  }

  .nav-tools {
    gap: 3px;
  }
}
</style>
