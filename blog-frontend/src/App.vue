<template>
  <div>
    <h1>我的博客</h1>
    <ul>
      <li v-for="article in articles" :key="article.id">
        <h3>{{ article.title }}</h3>
        <p>{{ article.content }}</p>
        <small>{{ article.createTime }}</small>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const articles = ref([])

// 页面一加载就去找后端拿数据
onMounted(async () => {
  const res = await axios.get('http://localhost:4080/articles')
  articles.value = res.data
})
</script>

<style>
/* 随便加点样式，让它看着不像 90 年代的网页 */
li { border-bottom: 1px solid #ccc; padding: 10px; list-style: none; }
h3 { color: #42b983; }
</style>