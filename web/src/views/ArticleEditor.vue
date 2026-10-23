<template>
  <main class="page-shell editor-page">
    <div class="editor-header">
      <div>
        <el-button text class="back-link" @click="router.back()">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <span class="section-kicker">{{ isEditMode ? 'Edit story' : 'New story' }}</span>
        <h1>{{ isEditMode ? '继续打磨这篇文章' : '写下此刻想保存的内容' }}</h1>
        <p>正文使用 Markdown，支持标题、列表、引用、链接、代码块和图片。</p>
      </div>
      <div class="save-actions">
        <el-button v-if="!isPublished" size="large" :loading="savingStatus === 'draft'" @click="saveArticle('draft')">
          保存草稿
        </el-button>
        <el-button
          type="primary"
          size="large"
          class="anime-btn"
          :loading="savingStatus === 'published'"
          @click="saveArticle('published')"
        >
          {{ isPublished ? '保存修改' : '发布文章' }}
        </el-button>
      </div>
    </div>

    <el-skeleton v-if="isLoading" animated :rows="12" class="glass-card editor-loading" />

    <el-empty v-else-if="loadError" class="glass-card empty-state" :description="loadError">
      <el-button type="primary" plain @click="router.push('/home')">返回我的文章</el-button>
    </el-empty>

    <section v-else class="editor-shell glass-card">
      <div class="title-row">
        <el-input
          v-model="form.title"
          maxlength="80"
          show-word-limit
          size="large"
          placeholder="给文章起一个清楚的标题"
        />
        <el-input-tag
          v-model="form.tags"
          class="tag-input"
          :max="ARTICLE_TAG_LIMIT"
          delimiter=","
          clearable
          placeholder="添加标签，回车确认（最多 5 个）"
          @add-tag="normalizeNewTag"
        />
      </div>

      <div class="mobile-tabs" role="tablist" aria-label="编辑与预览">
        <button type="button" :class="{ active: mobilePanel === 'write' }" @click="mobilePanel = 'write'">编辑</button>
        <button type="button" :class="{ active: mobilePanel === 'preview' }" @click="mobilePanel = 'preview'">
          预览
        </button>
      </div>

      <div class="editor-grid">
        <section class="write-panel" :class="{ 'mobile-hidden': mobilePanel !== 'write' }">
          <div class="panel-label">
            <span>Markdown</span>
            <span class="panel-tools">
              <el-button text size="small" :loading="isUploading" @click="pickImage">
                <el-icon><Picture /></el-icon>
                插入图片
              </el-button>
              {{ form.content.length.toLocaleString() }} / 50,000
            </span>
            <input
              ref="imageInput"
              type="file"
              accept="image/jpeg,image/png,image/gif,image/webp"
              hidden
              @change="uploadImage"
            />
          </div>
          <el-input
            ref="contentInput"
            v-model="form.content"
            type="textarea"
            :rows="25"
            maxlength="50000"
            resize="none"
            placeholder="# 从一个标题开始&#10;&#10;写下正文，使用 **粗体**、列表或代码块整理你的想法。"
          />
        </section>

        <section class="preview-panel" :class="{ 'mobile-hidden': mobilePanel !== 'preview' }">
          <div class="panel-label">
            <span>实时预览</span>
            <span>安全渲染</span>
          </div>
          <div v-if="!form.content.trim()" class="preview-empty">
            <span>预览会出现在这里</span>
            <p>开始输入 Markdown，右侧会实时呈现最终阅读效果。</p>
          </div>
          <MarkdownContent v-else :source="form.content" />
        </section>
      </div>
    </section>

    <div v-if="!isLoading && !loadError" class="editor-footer">
      <span>{{ isDirty ? '有尚未保存的修改' : '所有修改已保存' }}</span>
      <el-button v-if="!isPublished" :loading="savingStatus === 'draft'" @click="saveArticle('draft')">
        保存草稿
      </el-button>
      <el-button
        type="primary"
        class="anime-btn"
        :loading="savingStatus === 'published'"
        @click="saveArticle('published')"
      >
        {{ isPublished ? '保存修改' : '发布文章' }}
      </el-button>
    </div>
  </main>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ArrowLeft, Picture } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import MarkdownContent from '../components/MarkdownContent.vue'
import { useUser } from '../composables/useUser'
import { ARTICLE_TAG_LIMIT, validateArticleDraft } from '../utils/articleValidation'
import request, { apiData } from '../utils/request'

const IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
const IMAGE_MAX_BYTES = 5 * 1024 * 1024

const route = useRoute()
const router = useRouter()
const { userInfo, refreshMe } = useUser()
const form = reactive({ title: '', content: '', tags: [] })
const savedSnapshot = ref(JSON.stringify(form))
const status = ref('')
const isLoading = ref(false)
const savingStatus = ref('')
const isUploading = ref(false)
const loadError = ref('')
const mobilePanel = ref('write')
const allowNavigation = ref(false)
const imageInput = ref(null)
const contentInput = ref(null)

const isEditMode = computed(() => Boolean(route.params.id))
const articleId = computed(() => Number(route.params.id))
// 已发布文章只提供“保存修改”；新文章和草稿可以保存草稿或发布
const isPublished = computed(() => status.value === 'published')
const isDirty = computed(() => JSON.stringify(form) !== savedSnapshot.value)

const snapshot = () => {
  savedSnapshot.value = JSON.stringify(form)
}

const loadArticle = async () => {
  if (!isEditMode.value) {
    snapshot()
    return
  }

  isLoading.value = true
  loadError.value = ''
  try {
    if (!userInfo.value) {
      await refreshMe()
    }
    const article = apiData(await request.get(`/articles/${articleId.value}`))
    if (!userInfo.value || Number(article.authorId) !== Number(userInfo.value.id)) {
      loadError.value = '只能编辑自己发布的文章'
      return
    }
    form.title = article.title || ''
    form.content = article.content || ''
    form.tags = [...(article.tags || [])]
    status.value = article.status
    snapshot()
  } catch {
    loadError.value = '文章加载失败，可能已经被删除'
  } finally {
    isLoading.value = false
  }
}

const saveArticle = async (targetStatus) => {
  if (savingStatus.value) return
  const title = form.title.trim()
  const content = form.content.trim()
  const tags = form.tags.map((tag) => tag.trim()).filter(Boolean)
  const validationMessage = validateArticleDraft({ title, content, tags, status: targetStatus })
  if (validationMessage) {
    ElMessage.warning(validationMessage)
    return
  }

  const wasPublished = isPublished.value
  savingStatus.value = targetStatus
  try {
    const payload = { title, content, tags, status: targetStatus }
    const response = isEditMode.value
      ? await request.put(`/articles/${articleId.value}`, payload)
      : await request.post('/articles', payload)
    const article = apiData(response)
    Object.assign(form, { title: article.title, content: article.content, tags: [...(article.tags || [])] })
    status.value = article.status
    snapshot()

    if (targetStatus === 'draft') {
      ElMessage.success('草稿已保存')
      // 新建的草稿切换到编辑地址，之后的保存都会更新同一篇
      if (!isEditMode.value) {
        allowNavigation.value = true
        await router.replace(`/editor/${article.id}`)
        allowNavigation.value = false
      }
      return
    }

    allowNavigation.value = true
    ElMessage.success(wasPublished ? '修改已保存' : '文章已发布')
    await router.replace(`/articles/${article.id}`)
  } finally {
    savingStatus.value = ''
  }
}

// 新标签去掉开头的 #，空标签或重复标签直接丢弃
const normalizeNewTag = (tag) => {
  const normalized = tag.replace(/^#+/, '').trim()
  const others = form.tags.slice(0, -1)
  if (!normalized || others.some((item) => item.toLowerCase() === normalized.toLowerCase())) {
    form.tags.pop()
    return
  }
  form.tags[form.tags.length - 1] = normalized
}

const pickImage = () => {
  imageInput.value?.click()
}

const insertAtCursor = (text) => {
  const textarea = contentInput.value?.textarea
  const start = textarea ? textarea.selectionStart : form.content.length
  const end = textarea ? textarea.selectionEnd : form.content.length
  form.content = `${form.content.slice(0, start)}${text}${form.content.slice(end)}`
  nextTick(() => {
    if (!textarea) return
    const cursor = start + text.length
    textarea.focus()
    textarea.setSelectionRange(cursor, cursor)
  })
}

const uploadImage = async (event) => {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (!IMAGE_TYPES.includes(file.type)) {
    ElMessage.error('仅支持 JPG、PNG、GIF 或 WebP 图片')
    return
  }
  if (file.size > IMAGE_MAX_BYTES) {
    ElMessage.error('图片不能超过 5MB')
    return
  }

  isUploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const { url } = apiData(await request.post('/upload', formData))
    const alt = file.name.replace(/\.[^.]+$/, '').replace(/[[\]]/g, '') || '图片'
    insertAtCursor(`\n![${alt}](${url})\n`)
    ElMessage.success('图片已插入')
  } finally {
    isUploading.value = false
  }
}

const confirmLeave = async () => {
  if (allowNavigation.value || !isDirty.value) return true
  try {
    await ElMessageBox.confirm('当前修改还没有保存，确定离开编辑页吗？', '未保存的修改', {
      type: 'warning',
      confirmButtonText: '离开',
      cancelButtonText: '继续编辑',
    })
    return true
  } catch {
    return false
  }
}

const handleBeforeUnload = (event) => {
  if (!isDirty.value) return
  event.preventDefault()
  event.returnValue = ''
}

onBeforeRouteLeave(confirmLeave)
onMounted(() => {
  window.addEventListener('beforeunload', handleBeforeUnload)
  loadArticle()
})
onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
})
</script>

<style scoped>
.editor-page {
  width: min(1380px, calc(100% - 40px));
}

.editor-header {
  margin-bottom: 24px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 28px;
}

.editor-header h1 {
  margin: 8px 0;
  color: var(--text-strong);
  font-size: clamp(30px, 4.5vw, 46px);
}

.editor-header p {
  margin: 0;
  color: var(--text-muted);
}

.back-link {
  display: flex;
  margin: 0 0 14px -12px;
  color: var(--text-muted);
}

.editor-loading {
  padding: 30px;
}

.editor-shell {
  overflow: hidden;
}

.title-row {
  padding: 20px;
  border-bottom: 1px solid var(--border-soft);
  display: grid;
  gap: 12px;
}

.save-actions {
  display: flex;
  gap: 10px;
}

.tag-input {
  padding: 0 14px;
}

.panel-tools {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.title-row :deep(.el-input__wrapper) {
  padding: 4px 14px;
  box-shadow: none;
}

.title-row :deep(.el-input__inner) {
  color: var(--text-strong);
  font-size: 22px;
  font-weight: 750;
}

.editor-grid {
  min-height: 620px;
  display: grid;
  grid-template-columns: 1fr 1fr;
}

.write-panel,
.preview-panel {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.write-panel {
  border-right: 1px solid var(--border-soft);
}

.panel-label {
  min-height: 44px;
  padding: 0 18px;
  border-bottom: 1px solid var(--border-soft);
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--text-faint);
  font-size: 12px;
  font-weight: 750;
  letter-spacing: 0.04em;
}

.write-panel :deep(.el-textarea),
.write-panel :deep(.el-textarea__inner) {
  height: 100%;
}

.write-panel :deep(.el-textarea__inner) {
  min-height: 620px !important;
  padding: 22px;
  border: 0;
  border-radius: 0;
  box-shadow: none;
  color: var(--text-main);
  background: transparent !important;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', monospace;
  font-size: 14px;
  line-height: 1.75;
}

.preview-panel .markdown-body,
.preview-empty {
  padding: 28px 32px 48px;
}

.preview-empty {
  color: var(--text-faint);
}

.preview-empty span {
  color: var(--text-strong);
  font-size: 20px;
  font-weight: 750;
}

.preview-empty p {
  line-height: 1.8;
}

.mobile-tabs {
  display: none;
}

.editor-footer {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 16px;
  color: var(--text-faint);
  font-size: 13px;
}

@media (max-width: 820px) {
  .editor-page {
    width: min(100% - 24px, 1380px);
  }

  .editor-header {
    align-items: stretch;
    flex-direction: column;
  }

  .save-actions .el-button {
    flex: 1;
  }

  .mobile-tabs {
    padding: 8px;
    border-bottom: 1px solid var(--border-soft);
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 8px;
  }

  .mobile-tabs button {
    padding: 9px;
    border: 0;
    border-radius: 10px;
    color: var(--text-muted);
    background: transparent;
    font-weight: 700;
  }

  .mobile-tabs button.active {
    color: var(--theme-pink);
    background: var(--accent-soft);
  }

  .editor-grid {
    grid-template-columns: 1fr;
  }

  .write-panel {
    border-right: 0;
  }

  .mobile-hidden {
    display: none;
  }
}
</style>
