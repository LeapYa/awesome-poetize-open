/**
 * 应用入口文件
 * 负责 Vue 应用初始化、插件注册和全局配置
 */

import Vue from 'vue'
import App from './App.vue'
import router from './router'
import ElementUI from 'element-ui-ce'
import { createPinia, PiniaVuePlugin } from 'pinia'

// 工具函数
import http from './utils/request'
import common from './utils/common'
import constant from './utils/constant'
import initAntiDebug from './utils/anti-debug'
import { loadFonts } from './utils/font-loader'
import { getDefaultAvatar, getAvatarUrl } from './utils/default-avatar'
import animateDirective from './utils/animateDirective'

// 业务模块
import { notificationManager } from './utils/notification'
import { initVueErrorHandler, initPromiseErrorHandler } from './utils/error-handler'
import { initGrayMode } from './utils/gray-mode'
import { initImageLoader } from './utils/image-loader'
import { registerServiceWorker } from './utils/pwa-manager'

// Stores
import { useMainStore } from './stores/main'

// 组件
import AsyncNotification from './components/common/AsyncNotification.vue'

// 样式文件
import './utils/title'
import './assets/css/animation.css'
import './assets/css/index.css'
import './assets/css/tocbot.css'
import './assets/css/color.css'
import './assets/css/markdown-highlight.css'
import './assets/css/font-awesome.min.css'
import './assets/css/admin-dark-mode.css'
import './assets/css/centered-dialog.css'
import './assets/css/article-style-protection.css'

// ==================== Vue 配置 ====================
Vue.config.productionTip = false

// 插件注册
Vue.use(PiniaVuePlugin)
Vue.use(ElementUI)
Vue.component('AsyncNotification', AsyncNotification)

// 注册全局指令
Vue.directive('animate', animateDirective)

// 全局属性挂载
Vue.prototype.$http = http
Vue.prototype.$common = common
Vue.prototype.$constant = constant
Vue.prototype.$bus = new Vue()
Vue.prototype.$notify = notificationManager
Vue.prototype.$getDefaultAvatar = getDefaultAvatar
Vue.prototype.$getAvatarUrl = getAvatarUrl

// ==================== 初始化模块 ====================

// 错误处理
initVueErrorHandler(Vue)
initPromiseErrorHandler()

// 初始化 Pinia
const pinia = createPinia()

// 创建主 store 实例
const mainStore = useMainStore(pinia)

// 灰度模式 - 使用 Pinia store
initGrayMode(mainStore)

// 监听 sysConfig 变化
mainStore.$subscribe((mutation, state) => {
  if (state.sysConfig) {
    loadFonts(state.sysConfig).catch(err => console.error('加载字体失败:', err))
  }
})

// 反调试（生产环境）
const disposeAntiDebug = initAntiDebug({
  enableInDev: process.env.VUE_APP_PRODUCTION_MODE === 'true'
})
if (disposeAntiDebug) {
  window.__disableAntiDebug = () => {
    disposeAntiDebug()
    delete window.__disableAntiDebug
  }
}

// ==================== 创建 Vue 实例 ====================

const app = new Vue({
  router,
  pinia,
  render: h => h(App)
})

// ==================== 挂载应用 ====================

// 判断是否为预渲染页面（SSR 注水）
if (window.PRERENDER_DATA) {
  app.$mount('#app', true)
} else {
  app.$mount('#app')
}

// ==================== 应用挂载后初始化 ====================

app.$nextTick(() => {
  // 触发应用挂载完成事件
  window.dispatchEvent(new CustomEvent('app-mounted', {
    detail: {
      timestamp: Date.now(),
      prerendered: !!window.PRERENDER_DATA,
      mountType: window.PRERENDER_DATA ? 'hydration' : 'normal'
    }
  }))

  // 添加挂载完成标记
  const appElement = document.getElementById('app')
  if (appElement) {
    appElement.classList.add('vue-mounted')
  }

  // 初始化图片懒加载
  initImageLoader()

  // 注册 PWA Service Worker
  registerServiceWorker(Vue.prototype.$notify.info)

  // 字体加载 - 在应用挂载后执行，确保 store 已完全初始化
  if (mainStore.sysConfig) {
    loadFonts(mainStore.sysConfig).catch(err => {
      console.error('加载字体失败:', err)
    })
  }
})
