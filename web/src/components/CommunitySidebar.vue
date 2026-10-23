<template>
  <aside class="community-aside">
    <div
      v-if="token"
      class="glass-card aside-card profile-summary"
      role="link"
      tabindex="0"
      @click="openMyPublicProfile"
      @keydown.enter="openMyPublicProfile"
    >
      <div class="profile-summary__head">
        <el-avatar :size="52" :src="mediaUrl(userInfo?.avatar)">
          {{ userInitial }}
        </el-avatar>
        <div>
          <h3>{{ userInfo?.nickname || userInfo?.username || '我的主页' }}</h3>
          <span>查看个人主页</span>
        </div>
      </div>
      <p>{{ userInfo?.bio || '还没有填写个人简介。' }}</p>
      <div class="follow-stats">
        <div>
          <b>{{ userInfo?.followerCount ?? 0 }}</b>
          <span>粉丝</span>
        </div>
        <div>
          <b>{{ userInfo?.followingCount ?? 0 }}</b>
          <span>关注</span>
        </div>
      </div>
    </div>

    <div v-else class="glass-card aside-card profile-summary profile-summary--guest">
      <span class="section-kicker">个人主页</span>
      <h3>登录后查看个人资料</h3>
      <p>登录后可以关注作者，并在这里快速进入你的主页。</p>
      <el-button type="primary" plain @click="openAuth('login', route.fullPath)">登录</el-button>
    </div>

    <div v-if="popularTags.length" class="glass-card aside-card">
      <span class="section-kicker">热门标签</span>
      <h3>按标签浏览</h3>
      <div class="tag-cloud">
        <button
          v-for="item in popularTags"
          :key="item.name"
          type="button"
          :class="{ active: item.name === activeTag }"
          @click="$emit('select-tag', item.name === activeTag ? '' : item.name)"
        >
          #{{ item.name }} <small>{{ item.articleCount }}</small>
        </button>
      </div>
    </div>

    <MiniCalendar />

    <div class="glass-card aside-card">
      <span class="section-kicker">文章概览</span>
      <h3>当前列表</h3>
      <div class="stats">
        <div>
          <b>{{ total }}</b>
          <span>文章</span>
        </div>
        <div>
          <b>{{ sortLabel }}</b>
          <span>排序方式</span>
        </div>
      </div>
      <el-button v-if="token" text class="write-link" @click="router.push('/editor')">写文章 →</el-button>
      <el-button v-else text class="write-link" @click="openAuth('login', '/editor')">登录后写文章 →</el-button>
    </div>
  </aside>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MiniCalendar from './MiniCalendar.vue'
import { useUser } from '../composables/useUser'
import { mediaUrl } from '../utils/media'

defineProps({
  total: { type: Number, default: 0 },
  sortLabel: { type: String, default: '' },
  popularTags: { type: Array, default: () => [] },
  activeTag: { type: String, default: '' },
})

defineEmits(['select-tag'])

const route = useRoute()
const router = useRouter()
const { token, userInfo, openAuth } = useUser()

const userInitial = computed(() => (userInfo.value?.nickname || userInfo.value?.username || '我').slice(0, 1))

const openMyPublicProfile = () => {
  if (userInfo.value?.id) router.push(`/users/${userInfo.value.id}`)
}
</script>

<style scoped>
.community-aside {
  display: grid;
  align-content: start;
  gap: 18px;
}

.aside-card {
  padding: 24px;
}

.aside-card h3 {
  margin: 8px 0 16px;
  color: var(--text-strong);
}

.aside-card p {
  color: var(--text-muted);
  line-height: 1.75;
}

.profile-summary[role='link'] {
  cursor: pointer;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.profile-summary[role='link']:hover,
.profile-summary[role='link']:focus-visible {
  transform: translateY(-2px);
  box-shadow: var(--theme-shadow-hover) !important;
  outline: none;
}

.profile-summary__head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.profile-summary__head h3 {
  margin: 0 0 4px;
}

.profile-summary__head span {
  color: var(--text-faint);
  font-size: 12px;
}

.profile-summary > p {
  margin: 16px 0;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.profile-summary--guest h3 {
  margin-bottom: 10px;
}

.follow-stats {
  padding-top: 14px;
  border-top: 1px solid var(--border-soft);
  display: grid;
  grid-template-columns: 1fr 1fr;
}

.follow-stats div {
  text-align: center;
}

.follow-stats div + div {
  border-left: 1px solid var(--border-soft);
}

.follow-stats b,
.follow-stats span {
  display: block;
}

.follow-stats b {
  color: var(--text-strong);
  font-size: 18px;
}

.follow-stats span {
  margin-top: 3px;
  color: var(--text-faint);
  font-size: 12px;
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-cloud button {
  padding: 5px 11px;
  border: 0;
  border-radius: 999px;
  color: var(--theme-pink);
  background: var(--accent-soft);
  cursor: pointer;
  font-size: 13px;
  font-weight: 700;
}

.tag-cloud button small {
  color: var(--text-faint);
  font-weight: 500;
}

.tag-cloud button.active,
.tag-cloud button:hover {
  color: white;
  background: var(--theme-pink);
}

.tag-cloud button.active small,
.tag-cloud button:hover small {
  color: rgba(255, 255, 255, 0.8);
}

.stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.stats div {
  padding: 16px 10px;
  border-radius: 13px;
  text-align: center;
  background: var(--accent-soft);
}

.stats b,
.stats span {
  display: block;
}

.stats b {
  color: var(--theme-pink);
  font-size: 24px;
}

.stats div:nth-child(2) b {
  font-size: 14px;
  line-height: 34px;
}

.stats span {
  margin-top: 3px;
  color: var(--text-faint);
  font-size: 12px;
}

.write-link {
  padding-left: 0;
  color: var(--theme-pink);
  font-weight: 800;
}

@media (max-width: 900px) {
  .community-aside {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 620px) {
  .community-aside {
    grid-template-columns: 1fr;
  }
}
</style>
