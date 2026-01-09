<template>
  <div id="app">
    <router-view />
    <!-- 全局验证码容器 -->
    <captcha-container />
    <!-- 全局异步通知组件 -->
    <async-notification ref="globalNotification" />
    <!-- 全局邮箱收集组件 -->
    <global-email-collection
      :visible="showGlobalEmailCollection"
      :userInfo="tempUserData"
      :provider="emailCollectionProvider"
      @complete="handleEmailCollectionComplete"
    />
    <!-- AI聊天（支持Live2D看板娘模式或简单按钮模式） -->
    <!-- mode从后台配置读取，默认为 'live2d' -->
    <Live2D :mode="waifuDisplayMode" />
  </div>
</template>

<script>
import { useMainStore } from '@/stores/main'
import globalEmailCollectionMixin from '@/mixins/globalEmailCollection.js'
import { initMouseClickEffect } from '@/composables/useMouseClickEffect'

import CaptchaContainer from '@/components/common/CaptchaContainer.vue'
import GlobalEmailCollection from '@/components/common/GlobalEmailCollection.vue'
import Live2D from '@/components/live2d/index.vue'

export default {
  name: 'App',
  mixins: [globalEmailCollectionMixin],
  components: {
    CaptchaContainer,
    GlobalEmailCollection,
    Live2D,
  },
  data() {
    return {
      currentLang: 'zh', // 默认中文
    }
  },

  computed: {
    mainStore() {
      return useMainStore()
    },
    waifuDisplayMode() {
      // 从 webInfo 中读取显示模式，默认为 'live2d'
      return this.mainStore?.webInfo?.waifuDisplayMode || 'live2d'
    },
  },

  watch: {
    '$route.path': function (newPath) {
      // 所有页面都使用网站标题
      // 添加安全检查，防止 store 未就绪时访问
      if (this.mainStore?.webInfo?.webTitle) {
        const webTitle = this.mainStore.webInfo.webTitle
        document.title = webTitle
        window.OriginTitile = webTitle
      }
    },
  },

  created() {
    // 获取用户语言偏好
    const savedLang = localStorage.getItem('preferredLanguage')
    if (savedLang === 'en' || savedLang === 'zh') {
      this.currentLang = savedLang
    } else {
      // 检测浏览器语言
      const browserLang = navigator.language || navigator.userLanguage
      if (browserLang.toLowerCase().startsWith('en')) {
        this.currentLang = 'en'
      }
    }

    // 初始化时设置网站标题
    if (this.mainStore.webInfo && this.mainStore.webInfo.webTitle) {
      // 直接使用webTitle字符串
      document.title = this.mainStore.webInfo.webTitle
      window.OriginTitile = this.mainStore.webInfo.webTitle
    }

    // 确保导航栏初始状态正确（修复首次访问时导航栏不显示的问题）
    this.mainStore.changeToolbarStatus({
      visible: true,
      enter: false,
    })
  },

  mounted() {
    // 确保字体加载
    document.body.style.fontFamily = 'var(--globalFont), serif'

    // 注册全局通知实例
    if (this.$refs.globalNotification) {
      this.$notify.setInstance(this.$refs.globalNotification)
    }

    // 初始化鼠标点击效果（根据后端配置自动选择效果类型）
    this.mouseClickEffectCleanup = initMouseClickEffect(this.mainStore)
  },

  beforeUnmount() {
    // 清理鼠标点击效果
    if (this.mouseClickEffectCleanup) {
      this.mouseClickEffectCleanup()
    }
  },

  methods: {
    handleLanguageChange(lang) {
      if (this.currentLang === lang) return

      this.currentLang = lang
      localStorage.setItem('preferredLanguage', lang)

      // 更新URL参数并刷新页面以应用语言更改
      const url = new URL(window.location)
      url.searchParams.set('lang', lang)
      window.location.href = url.toString()
    },
  },
}
</script>

<style>
* {
  font-family: 'MyAwesomeFont', serif;
}
/* 消息通知全局层级，始终最高优先级显示 */
.el-message {
  z-index: 10000 !important;
}

/* 验证码错误消息同样需要最高层级 */
.captcha-error-message {
  z-index: 10000 !important;
}
.global-language-switch {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 1000;
  background-color: rgba(255, 255, 255, 0.7);
  padding: 5px 10px;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}
.el-dropdown-link {
  cursor: pointer;
  color: #409eff;
}
</style>
