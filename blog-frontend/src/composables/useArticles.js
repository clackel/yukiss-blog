import { ref } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

export function useArticles() {
  const articles = ref([])
  const fetchArticles = async () => {
    try {
      const res = await axios.get('/articles')
      articles.value = res.data
    } catch (err) {
      ElMessage.error('无法连接到后端，请检查 4000 端口')
    }
  }

  const submitArticle = async (newArticle, onSuccess) => {
    if (!newArticle.title || !newArticle.content) {
      ElMessage.warning('标题和内容缺一不可哦！')
      return
    }
    try {
      await axios.post('/articles', newArticle)
      ElMessage.success('发布成功！')
      if (onSuccess) onSuccess()
      fetchArticles()
    } catch (err) {
      ElMessage.error('发布失败，请检查网络')
    }
  }

  const formatDate = (dateStr) => {
    if (!dateStr) return '未知时间'
    return new Date(dateStr).toLocaleDateString()
  }

  return { articles, fetchArticles, submitArticle, formatDate }
}
