import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    // 按需引入 Element Plus 组件（模板里用到哪个才打包哪个）；
    // 样式仍在 main.js 中整体引入，保证 ElMessage 等函数式组件的样式可用
    Components({
      resolvers: [ElementPlusResolver({ importStyle: false })],
      dts: false,
    }),
  ],
  // 按需引入的组件来自 element-plus/es，提前预构建，避免开发时首次访问才发现依赖、重新打包并刷新页面
  optimizeDeps: {
    include: ['element-plus/es'],
  },
  server: {
    host: '127.0.0.1',
    port: 3000,
  },
  test: {
    environment: 'jsdom',
    globals: true,
  },
})
