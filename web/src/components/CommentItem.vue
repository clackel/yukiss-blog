<template>
  <div :id="`comment-${comment.id}`" class="comment-item" :class="{ 'comment-item--reply': isReply }">
    <el-avatar :size="isReply ? 32 : 38" :src="mediaUrl(comment.avatar)">
      {{ initial }}
    </el-avatar>
    <div class="comment-main">
      <div class="comment-top">
        <span v-if="comment.userDeleted" class="comment-name comment-name--deleted">{{ comment.nickname }}</span>
        <button v-else class="comment-name" type="button" @click="$emit('open-user', comment.userId)">
          {{ comment.nickname || '匿名用户' }}
        </button>
        <span>
          {{ formatDateTime(comment.createTime) }}
          <em v-if="comment.updateTime" :title="`编辑于 ${formatDateTime(comment.updateTime)}`">（已编辑）</em>
        </span>
      </div>

      <div v-if="isEditing" class="comment-edit">
        <el-input
          v-model="editContent"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          resize="none"
          @keydown.ctrl.enter="saveEdit"
        />
        <div class="comment-edit__actions">
          <el-button size="small" @click="cancelEdit">取消</el-button>
          <el-button size="small" type="primary" :loading="isSaving" @click="saveEdit">保存</el-button>
        </div>
      </div>
      <p v-else>
        <span v-if="isReply" class="reply-label">回复 {{ comment.parentNickname || parentNickname || '朋友' }}</span>
        {{ comment.content }}
      </p>

      <div v-if="!isEditing" class="comment-actions">
        <button type="button" :disabled="!interactive" @click="$emit('like', comment)">
          <el-icon><StarFilled v-if="comment.likedByMe" /><Star v-else /></el-icon>
          {{ comment.likeCount || 0 }}
        </button>
        <button v-if="!isReply && interactive" type="button" @click="$emit('reply', comment)">回复</button>
        <button v-if="canEdit" type="button" @click="startEdit">编辑</button>
        <button v-if="canDelete" type="button" class="danger" @click="$emit('delete', comment)">删除</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { Star, StarFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request, { apiData } from '../utils/request'
import { formatDateTime } from '../utils/date'
import { mediaUrl } from '../utils/media'

const props = defineProps({
  comment: { type: Object, required: true },
  isReply: { type: Boolean, default: false },
  parentNickname: { type: String, default: '' },
  currentUserId: { type: Number, default: null },
  articleAuthorId: { type: Number, default: null },
  // 草稿下的评论只展示、不允许互动
  interactive: { type: Boolean, default: true },
})

const emit = defineEmits(['like', 'reply', 'delete', 'update', 'open-user'])

const isEditing = ref(false)
const isSaving = ref(false)
const editContent = ref('')

const initial = computed(() => (props.comment.nickname || '评').slice(0, 1))
const isMine = computed(() => props.currentUserId !== null && Number(props.comment.userId) === props.currentUserId)
const canEdit = computed(() => isMine.value)
// 评论作者本人或文章作者可以删除
const canDelete = computed(
  () => isMine.value || (props.currentUserId !== null && props.articleAuthorId === props.currentUserId)
)

const startEdit = () => {
  editContent.value = props.comment.content
  isEditing.value = true
}

const cancelEdit = () => {
  isEditing.value = false
}

const saveEdit = async () => {
  const content = editContent.value.trim()
  if (!content) {
    ElMessage.warning('评论内容不能为空')
    return
  }
  if (content === props.comment.content) {
    isEditing.value = false
    return
  }
  isSaving.value = true
  try {
    const updated = apiData(await request.put(`/comment/${props.comment.id}`, { content }))
    emit('update', { content: updated.content, updateTime: updated.updateTime })
    isEditing.value = false
    ElMessage.success('评论已更新')
  } finally {
    isSaving.value = false
  }
}
</script>

<style scoped>
.comment-item {
  display: flex;
  gap: 12px;
}

.comment-main {
  min-width: 0;
  flex: 1;
}

.comment-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.comment-name {
  padding: 0;
  border: 0;
  color: var(--text-strong);
  background: transparent;
  cursor: pointer;
  font-weight: 700;
}

.comment-name:hover,
.comment-name:focus-visible {
  color: var(--theme-pink);
}

.comment-name--deleted,
.comment-name--deleted:hover {
  color: var(--text-faint);
  cursor: default;
}

.comment-top > span {
  color: var(--text-faint);
  font-size: 12px;
}

.comment-top em {
  font-style: normal;
}

.comment-main p {
  margin: 9px 0;
  color: var(--text-main);
  line-height: 1.75;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.comment-edit {
  margin: 9px 0;
}

.comment-edit__actions {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.comment-actions {
  display: flex;
  gap: 12px;
}

.comment-actions button {
  padding: 3px 0;
  border: 0;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--text-faint);
  background: transparent;
  cursor: pointer;
  font-size: 12px;
}

.comment-actions button:hover:not(:disabled) {
  color: var(--theme-pink);
}

.comment-actions button.danger:hover {
  color: var(--el-color-danger);
}

.comment-actions button:disabled {
  cursor: default;
}

.reply-label {
  margin-right: 5px;
  color: var(--theme-pink);
  font-size: 12px;
  font-weight: 700;
}

@media (max-width: 640px) {
  .comment-top {
    flex-direction: column;
    gap: 3px;
  }
}
</style>
