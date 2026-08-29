<template>
  <main class="page-shell not-found">
    <div class="glass-card not-found__card">
      <span class="section-kicker">404</span>
      <h1>页面不存在</h1>
      <p>链接可能已失效，或者页面地址有误。</p>
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
