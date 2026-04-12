import { createApp } from 'vue'
import App from './App.vue'
// 引入 Element Plus
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './style.css' // 保持你的全局样式

const app = createApp(App)
app.use(ElementPlus)
app.mount('#app')