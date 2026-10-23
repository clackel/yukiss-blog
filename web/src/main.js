import { createApp } from 'vue'
import App from './App.vue'
// Element Plus 组件由 vite.config.js 中的 unplugin-vue-components 按需引入，这里只引入样式
import 'element-plus/dist/index.css'
import { router } from './router'
import './style.css'

const app = createApp(App)
app.use(router)
app.mount('#app')
