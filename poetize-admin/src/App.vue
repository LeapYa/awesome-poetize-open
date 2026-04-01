<template>
  <div id="app">
    <router-view/>
    <!-- 全局验证码容器 -->
    <component
      :is="captchaContainerComponent"
      v-if="captchaVisible && captchaContainerComponent"
    />
    <!-- 全局异步通知组件 -->
    <async-notification ref="globalNotification" />
    <!-- 全局邮箱收集组件 -->
    <GlobalEmailCollectionAsync
      v-if="showGlobalEmailCollection"
      :visible="showGlobalEmailCollection"
      :userInfo="tempUserData"
      :provider="emailCollectionProvider"
      @complete="handleEmailCollectionComplete"
    />
    <!-- AI聊天（支持Live2D看板娘模式或简单按钮模式） -->
    <!-- mode从后台配置读取，默认为 'live2d' -->
    <Live2DAsync v-if="showLive2D" :mode="waifuDisplayMode" />
  </div>
</template>

<script>
  import { defineAsyncComponent } from 'vue';
  import { useMainStore } from '@/stores/main';
  import globalEmailCollectionMixin from '@/mixins/globalEmailCollection.js';

export default {
  name: "App",
  mixins: [globalEmailCollectionMixin],
  components: {
    GlobalEmailCollectionAsync: defineAsyncComponent(() => import('@/components/common/GlobalEmailCollection.vue')),
    Live2DAsync: defineAsyncComponent(() => import('@/components/live2d/index.vue'))
  },
  data() {
    return {
      currentLang: 'zh', // 默认中文
      captchaContainerComponent: null,
      captchaContainerLoadingPromise: null,
      markdownWarmupRequested: false,
      showLive2D: false
    };
  },

  computed: {
      mainStore() {
        return useMainStore();
      },
    captchaVisible() {
      return this.mainStore.captcha.show;
    },
    waifuDisplayMode() {
      // 从 webInfo 中读取显示模式，默认为 'auto'，交由组件兜底
      return this.mainStore.webInfo?.waifuDisplayMode || 'auto';
    }
  },

  watch: {
    captchaVisible(visible) {
      if (visible) {
        this.ensureCaptchaContainerLoaded();
      }
    },
    '$route.path': function(newPath) {
      // 所有页面都使用网站标题
      if (this.mainStore.webInfo && this.mainStore.webInfo.webTitle) {
        // 直接使用webTitle字符串，不再需要从localStorage获取
        const webTitle = this.mainStore.webInfo.webTitle;
        document.title = webTitle;
        window.OriginTitile = webTitle;
      }

      this.scheduleMarkdownWarmup(newPath);
    }
  },

  created() {
    // 获取用户语言偏好
    const savedLang = localStorage.getItem('preferredLanguage');
    if (savedLang === 'en' || savedLang === 'zh') {
      this.currentLang = savedLang;
    } else {
      // 检测浏览器语言
      const browserLang = navigator.language || navigator.userLanguage;
      if (browserLang.toLowerCase().startsWith('en')) {
        this.currentLang = 'en';
      }
    }
    
    // 初始化时设置网站标题
    if (this.mainStore.webInfo && this.mainStore.webInfo.webTitle) {
      // 直接使用webTitle字符串
      document.title = this.mainStore.webInfo.webTitle;
      window.OriginTitile = this.mainStore.webInfo.webTitle;
    }
    
    // 确保导航栏初始状态正确（修复首次访问时导航栏不显示的问题）
    this.mainStore.changeToolbarStatus( {
      visible: true,
      enter: false
    });
  },

  mounted() {
    // 确保字体加载
    document.body.style.fontFamily = "var(--globalFont), serif";

    if (this.captchaVisible) {
      this.ensureCaptchaContainerLoaded();
    }
    
    // 注册全局通知实例
    if (this.$refs.globalNotification) {
      this.$notify.setInstance(this.$refs.globalNotification);
    }

    this.scheduleNonCriticalUi();
    this.scheduleMarkdownWarmup(this.$route.path);
  },

  beforeDestroy() {
    // 清理鼠标点击特效
    if (this.mouseClickEffectCleanup) {
      this.mouseClickEffectCleanup();
    }
  },

  methods: {
    runWhenIdle(callback, options = {}) {
      const { delay = 0, timeout = 1500 } = options;

      const invoke = () => {
        if (typeof window !== 'undefined' && typeof window.requestIdleCallback === 'function') {
          window.requestIdleCallback(() => callback(), { timeout });
        } else {
          window.setTimeout(callback, timeout);
        }
      };

      if (delay > 0) {
        window.setTimeout(invoke, delay);
      } else {
        invoke();
      }
    },
    shouldWarmupMarkdown(path = this.$route.path) {
      return path === '/postEdit';
    },
    scheduleMarkdownWarmup(path = this.$route.path) {
      if (this.markdownWarmupRequested || !this.shouldWarmupMarkdown(path)) {
        return;
      }

      this.markdownWarmupRequested = true;
      this.runWhenIdle(() => {
        import('@/utils/markdownLazyRenderer')
          .then(m => m.warmupMarkdown())
          .catch(error => {
            console.error('Markdown 核心预热失败:', error);
          });
      }, { delay: 600, timeout: 2500 });
    },
    scheduleNonCriticalUi() {
      this.runWhenIdle(() => {
        this.showLive2D = true;
        import('@/utils/mouseClickEffect')
          .then(({ initMouseClickEffect }) => {
            this.mouseClickEffectCleanup = initMouseClickEffect(this.mainStore);
          })
          .catch(error => {
            console.error('初始化鼠标点击特效失败:', error);
          });
      }, { delay: 1500, timeout: 2500 });
    },
    ensureCaptchaContainerLoaded() {
      if (this.captchaContainerComponent) {
        return Promise.resolve(this.captchaContainerComponent);
      }

      if (!this.captchaContainerLoadingPromise) {
        this.captchaContainerLoadingPromise = import('@/components/common/CaptchaContainer.vue')
          .then(module => {
            this.captchaContainerComponent = module.default || module;
            return this.captchaContainerComponent;
          })
          .finally(() => {
            this.captchaContainerLoadingPromise = null;
          });
      }

      return this.captchaContainerLoadingPromise;
    },
    handleLanguageChange(lang) {
      if (this.currentLang === lang) return;
      
      this.currentLang = lang;
      localStorage.setItem('preferredLanguage', lang);
      
      // 更新URL参数并刷新页面以应用语言更改
      const url = new URL(window.location);
      url.searchParams.set('lang', lang);
      window.location.href = url.toString();
    }
  }
}
</script>

<style>
/* 全局样式 */
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
  color: #409EFF;
}
</style>
