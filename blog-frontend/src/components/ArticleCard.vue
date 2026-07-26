<template>
  <article
    class="article-card glass-card"
    tabindex="0"
    role="link"
    @click="$emit('open', article.id)"
    @keydown.enter="$emit('open', article.id)"
  >
    <div class="article-card__head">
      <div v-if="showAuthor" class="author-line">
        <el-avatar :size="38" :src="mediaUrl(article.authorAvatar)">
          {{ authorInitial }}
        </el-avatar>
        <div>
          <b>{{ article.authorNickname || '神秘旅人' }}</b>
          <span>{{ formatDate(article.createTime) }}</span>
        </div>
      </div>
      <span v-else class="article-date">{{ formatDate(article.createTime) }}</span>

      <div v-if="manageable" class="article-card__manage" @click.stop>
        <el-button text size="small" @click="$emit('edit', article.id)">
          <el-icon><Edit /></el-icon>
          编辑
        </el-button>
        <el-button text size="small" type="danger" @click="$emit('delete', article)">
          <el-icon><Delete /></el-icon>
          删除
        </el-button>
      </div>
    </div>

    <h3>{{ article.title }}</h3>
    <p>{{ excerpt }}</p>

    <div class="article-card__footer">
      <span><el-icon><Star /></el-icon>{{ article.likeCount || 0 }}</span>
      <span><el-icon><ChatDotRound /></el-icon>{{ article.commentCount || 0 }}</span>
      <span class="read-more">阅读全文 <el-icon><ArrowRight /></el-icon></span>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import { ArrowRight, ChatDotRound, Delete, Edit, Star } from '@element-plus/icons-vue'
import { formatDate } from '../utils/date'
import { markdownExcerpt } from '../utils/markdown'
import { mediaUrl } from '../utils/media'

const props = defineProps({
  article: {
    type: Object,
    required: true,
  },
  showAuthor: {
    type: Boolean,
    default: true,
  },
  manageable: {
    type: Boolean,
    default: false,
  },
})

defineEmits(['open', 'edit', 'delete'])

const excerpt = computed(() => markdownExcerpt(props.article.content, 190))
const authorInitial = computed(() => (props.article.authorNickname || '旅').slice(0, 1))
</script>

<style scoped>
.article-card {
  padding: 24px;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.article-card:hover,
.article-card:focus-visible {
  transform: translateY(-3px);
  border-color: color-mix(in srgb, var(--theme-pink) 36%, transparent) !important;
  box-shadow: var(--theme-shadow-hover) !important;
  outline: none;
}

.article-card__head {
  min-height: 40px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.author-line {
  display: flex;
  align-items: center;
  gap: 11px;
}

.author-line div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.author-line b {
  color: var(--text-strong);
  font-size: 14px;
}

.author-line span,
.article-date {
  color: var(--text-faint);
  font-size: 12px;
}

.article-card__manage {
  display: flex;
  flex: 0 0 auto;
}

.article-card h3 {
  margin: 18px 0 10px;
  color: var(--text-strong);
  font-size: clamp(20px, 2.2vw, 24px);
  line-height: 1.35;
}

.article-card > p {
  min-height: 52px;
  margin: 0;
  color: var(--text-muted);
  line-height: 1.75;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-card__footer {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 18px;
  color: var(--text-faint);
  font-size: 13px;
}

.article-card__footer span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.article-card__footer .read-more {
  margin-left: auto;
  color: var(--theme-pink);
  font-weight: 700;
}

@media (max-width: 560px) {
  .article-card {
    padding: 20px;
  }

  .article-card__head {
    align-items: stretch;
    flex-direction: column;
  }

  .article-card__manage {
    justify-content: flex-end;
  }
}
</style>
