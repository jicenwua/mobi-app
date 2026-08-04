import { createApp } from 'vue'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import './assets/main.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import pinia from './store'
import directives from './directives'
import Pagination from '@/components/Pagination/index.vue'
import DictTag from '@/components/DictTag/index.vue'
import '@/permission'
import { useThemeStore } from './store/modules/theme'

const app = createApp(App)

// 按需自动引入 Element Plus 组件；图标仍全局注册（el-button icon="Search" 等）
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.component('Pagination', Pagination)
app.component('DictTag', DictTag)

app.use(router)
app.use(pinia)
app.use(directives)

const themeStore = useThemeStore()
themeStore.initTheme()

app.mount('#app')
