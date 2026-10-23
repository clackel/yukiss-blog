import js from '@eslint/js'
import pluginVue from 'eslint-plugin-vue'
import prettierConfig from 'eslint-config-prettier/flat'
import globals from 'globals'

export default [
  { ignores: ['dist/**', 'coverage/**'] },
  js.configs.recommended,
  ...pluginVue.configs['flat/recommended'],
  {
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      globals: globals.browser,
    },
  },
  {
    files: ['*.config.js'],
    languageOptions: { globals: globals.node },
  },
  {
    // 页面组件由路由按文件名引用，单词名（Home、Profile…）不会与 HTML 元素冲突
    files: ['src/views/**/*.vue'],
    rules: { 'vue/multi-word-component-names': 'off' },
  },
  {
    // 唯一允许 v-html 的地方：内容来自 renderMarkdown，已经过 DOMPurify 过滤
    files: ['src/components/MarkdownContent.vue'],
    rules: { 'vue/no-v-html': 'off' },
  },
  // 关闭与 Prettier 冲突的格式类规则，格式统一交给 Prettier
  prettierConfig,
]
