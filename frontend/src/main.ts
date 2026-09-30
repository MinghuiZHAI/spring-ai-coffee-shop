import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import './styles/tokens.css'
import './styles/element-plus.scss'
import App from './App.vue'
import router from './router'

// pinia 先于 router 注册：登录守卫内 useUserStore() 依赖激活的 pinia 实例
createApp(App).use(createPinia()).use(router).use(ElementPlus, { locale: zhCn }).mount('#app')
