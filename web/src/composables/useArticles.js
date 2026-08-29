import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import request, { apiData } from '../utils/request'

export function useArticles() {
  const articles = ref([])
  const isLoading = ref(false)
  const errorMessage = ref('')
  const lastFetchOptions = ref({})

  const fetchArticles = async (options = lastFetchOptions.value) => {
    lastFetchOptions.value = options
    isLoading.value = true
    errorMessage.value = ''
    try {
      const endpoint = options.mine ? '/articles/mine' : '/articles'
      const res = await request.get(endpoint)
      articles.value = apiData(res) || []
    } catch (err) {
      errorMessage.value = '文章加载失败，请稍后重试'
    } finally {
      isLoading.value = false
    }
  }

  const submitArticle = async (newArticle, onSuccess) => {
    if (!newArticle.title || !newArticle.content) {
      ElMessage.warning('请填写标题和内容')
      return
    }
    try {
      await request.post('/articles', newArticle)
      ElMessage.success('发布成功')
      if (onSuccess) onSuccess()
      fetchArticles(lastFetchOptions.value)
    } catch (err) {
      ElMessage.error('发布失败，请稍后重试')
    }
  }

  const formatDate = (dateStr) => {
    if (!dateStr) return '未知时间'
    return new Date(dateStr).toLocaleDateString()
  }

  return { articles, isLoading, errorMessage, fetchArticles, submitArticle, formatDate }
}
