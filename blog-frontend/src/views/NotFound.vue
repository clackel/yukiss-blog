<template>
  <main class="page-shell not-found">
    <div class="glass-card not-found__card">
      <span class="section-kicker">404 · Lost signal</span>
      <h1>这颗星球上没有页面</h1>
      <p>链接可能已经失效，也可能只是走进了一条还没被记录的支线。</p>
      <el-button type="primary" round class="anime-btn" @click="router.push('/community')">
        返回社区
      </el-button>
    </div>
  </main>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<style scoped>
.not-found {
  display: grid;
  place-items: center;
}

.not-found__card {
  max-width: 620px;
  padding: 54px;
  text-align: center;
}

.not-found h1 {
  margin: 12px 0;
  color: var(--text-strong);
}

.not-found p {
  margin: 0 0 24px;
  color: var(--text-muted);
  line-height: 1.8;
}
</style>
