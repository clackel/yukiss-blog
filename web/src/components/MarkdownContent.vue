<template>
  <div class="markdown-body" v-html="html" />
</template>

<script setup>
import { computed } from 'vue'
import { renderMarkdown } from '../utils/markdown'

const props = defineProps({
  source: {
    type: String,
    default: '',
  },
})

// renderMarkdown 的输出已经过 DOMPurify 过滤，因此可以安全地交给 v-html（见 eslint.config.js）
const html = computed(() => renderMarkdown(props.source))
</script>
