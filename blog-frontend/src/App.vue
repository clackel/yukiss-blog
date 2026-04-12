<template>
  <div class="anime-bg">
    <div class="glass-container">
      <header class="header">
        <h1 class="title">✨ Yukiss の 博客空间 ✨</h1>
        <p class="subtitle">欢迎来到我的次元世界 (*´▽｀)ノノ</p>

        <div style="text-align: center; margin-bottom: 20px;">
          <button class="publish-btn" @click="showForm = !showForm">
            {{ showForm ? '🏃 算了不写了' : '✍️ 我要投稿' }}
          </button>
        </div>
      </header>

      <transition name="fade">
        <div v-if="showForm" class="publish-form article-card" style="border-left: 5px solid #42b983;">
          <h3 style="margin-top:0; color:#42b983;">📝 记录此刻的心情...</h3>
          <input v-model="newArticle.title" placeholder="给你的文章起个好听的名字..." class="input-field">
          <textarea v-model="newArticle.content" placeholder="在这里输入内容..." class="textarea-field"></textarea>
          <div style="text-align: right;">
            <button @click="submitArticle" class="submit-btn">发射！🚀</button>
          </div>
        </div>
      </transition>

      <main class="content">
        <div v-for="article in articles" :key="article.id" class="article-card">
          <div class="card-tag"># 术力口 / 日志</div>
          <h2 class="article-title">{{ article.title }}</h2>
          <p class="article-body">{{ article.content }}</p>
          <div class="article-footer">
            <span>📅 {{ formatDate(article.createTime) }}</span>
            <span class="read-more">阅读全文 →</span>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const articles = ref([])
const showForm = ref(false)
const newArticle = ref({ title: '', content: '' })

// 封装刷新列表的方法，方便重复调用
const fetchArticles = async () => {
  try {
    const res = await axios.get('http://localhost:4080/articles')
    articles.value = res.data
  } catch (err) {
    console.error("加载失败:", err)
  }
}

onMounted(() => {
  fetchArticles()
})

// 发布文章逻辑
const submitArticle = async () => {
  if (!newArticle.value.title || !newArticle.value.content) {
    alert('标题和内容都要写哦！(oﾟvﾟ)ノ')
    return
  }
  try {
    await axios.post('http://localhost:4080/articles', newArticle.value)
    alert('发布成功！数据已同步到异次元服务器！')
    newArticle.value = { title: '', content: '' } // 清空
    showForm.value = false // 隐藏表单
    fetchArticles() // 刷新列表，看到新文章
  } catch (err) {
    alert('发射失败，请检查后端是否开启。')
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return '未知时间'
  const date = new Date(dateStr)
  return date.toLocaleDateString()
}
</script>

<style>
/* ... 你之前的背景和玻璃容器样式保持不变 ... */
.anime-bg {
  min-height: 100vh;
  background: url('https://api.kdcc.cn/img/random') no-repeat center center fixed;
  background-size: cover;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 40px 20px;
}

.glass-container {
  width: 100%;
  max-width: 800px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  padding: 30px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.3);
}

/* ✨ 新增：表单和按钮的样式 */
.publish-btn {
  background: #ff6bb1;
  color: white;
  border: none;
  padding: 10px 25px;
  border-radius: 25px;
  cursor: pointer;
  font-weight: bold;
  box-shadow: 0 4px 15px rgba(255, 107, 177, 0.3);
  transition: 0.3s;
}
.publish-btn:hover { background: #ff85c0; transform: translateY(-2px); }

.input-field, .textarea-field {
  width: 100%;
  box-sizing: border-box;
  margin-bottom: 15px;
  padding: 12px;
  border: 1px solid rgba(255, 107, 177, 0.2);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.5);
  outline: none;
}
.textarea-field { height: 120px; resize: none; }

.submit-btn {
  background: #42b983;
  color: white;
  border: none;
  padding: 10px 30px;
  border-radius: 10px;
  cursor: pointer;
  font-weight: bold;
}
.submit-btn:hover { background: #3aa876; }

.fade-enter-active, .fade-leave-active { transition: opacity 0.5s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* 之前的卡片样式 */
.article-card {
  background: rgba(255, 255, 255, 0.9);
  margin-bottom: 25px;
  padding: 20px;
  border-radius: 15px;
  transition: transform 0.3s;
  border-left: 5px solid #ff6bb1;
}
.title { color: #ff6bb1; text-align: center; }
.article-title { margin: 10px 0; color: #444; }
.article-body { color: #666; line-height: 1.6; }
.article-footer { margin-top: 15px; display: flex; justify-content: space-between; color: #999; }
</style>