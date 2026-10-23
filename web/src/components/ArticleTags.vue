<template>
  <ul v-if="tags.length" class="article-tags" aria-label="文章标签">
    <li v-for="tag in tags" :key="tag">
      <button type="button" @click.stop="$emit('select', tag)" @keydown.stop>#{{ tag }}</button>
    </li>
  </ul>
</template>

<script setup>
defineProps({
  tags: {
    type: Array,
    default: () => [],
  },
})

defineEmits(['select'])
</script>

<style scoped>
.article-tags {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.article-tags button {
  padding: 3px 10px;
  border: 0;
  border-radius: 999px;
  color: var(--theme-pink);
  background: var(--accent-soft);
  cursor: pointer;
  font-size: 12px;
  font-weight: 700;
}

.article-tags button:hover,
.article-tags button:focus-visible {
  color: white;
  background: var(--theme-pink);
  outline: none;
}
</style>
