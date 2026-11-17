<template>
  <div>
    <div class="friend-links-container">
      <!-- 封面 -->
      <div class="friend-links-header my-animation-slide-top">
        <!-- 背景视频（含慢网骨架与淡入） -->
        <indexVideo></indexVideo>
        <div class="friend-links-header-content">
          <!-- 标题 -->
          <div style="color: var(--white); margin: 0 10px">
            <div style="font-size: 30px; font-weight: bold; line-height: 2">
              {{ friendsPageTitle }}
            </div>
            <div style="font-size: 16px; opacity: 0.8; margin-top: 10px">
              留下你的网站吧，让我们建立友谊的桥梁
            </div>
          </div>
        </div>
      </div>

      <!-- 内容 -->
      <div class="friend-links-content">
        <friend></friend>
      </div>
    </div>

    <!-- 页脚 -->
    <div style="background: var(--favoriteBg)">
      <myFooter></myFooter>
    </div>
  </div>
</template>

<script>
import { defineAsyncComponent } from 'vue'
import { useMainStore } from '@/stores/main'
import indexVideo from './common/indexVideo'


export default {
  components: {
    myFooter: defineAsyncComponent(() => import('./common/myFooter')),
    friend: defineAsyncComponent(() => import('./friend')),
    indexVideo,
  },

  data() {
    return {}
  },

  computed: {
    mainStore() {
      return useMainStore()
    },
    friendsPageTitle() {
      try {
        const navConfig = this.mainStore.webInfo.navConfig
        const navItems = navConfig ? JSON.parse(navConfig) : []
        const friendsNav = Array.isArray(navItems)
          ? navItems.find((item) => item.link === '/friends')
          : null
        return friendsNav && friendsNav.name && friendsNav.name.trim()
          ? friendsNav.name.trim()
          : '友人帐'
      } catch (error) {
        return '友人帐'
      }
    },
    pageTitle() {
      const siteTitle = this.mainStore.homePageTitle
      return siteTitle ? `${this.friendsPageTitle} - ${siteTitle}` : this.friendsPageTitle
    },
  },

  watch: {
    pageTitle: {
      immediate: true,
      handler(title) {
        document.title = title
        window.OriginTitile = title
      },
    },
  },

  created() {
    // 设置页面标题（注：setTitle 方法已移除，页面标题由路由或组件自行管理）
    // document.title = "友人帐 | " + this.mainStore.sysConfig.webName;
  },

  mounted() {},

  methods: {},
}
</script>

<style scoped>
.friend-links-container {
  padding: 25px;
  background: var(--favoriteBg);
}
.friend-links-header {
  margin: 60px auto 30px;
  height: 200px;
  position: relative;
  overflow: hidden;
  border-radius: 20px;
  max-width: 1200px;
}
.friend-links-header-content {
  position: absolute;
  left: 20px;
  top: 25px;
  padding: 5px 20px;
  margin: 10px;
}
.friend-links-content {
  margin: 0 auto;
  max-width: 1200px;
}
@media screen and (max-width: 906px) {
  .friend-links-header {
    height: 180px;
  }
  .friend-links-header-content {
    margin: 0;
  }
}
@media screen and (max-width: 636px) {
  .friend-links-header {
    height: 160px;
  }
  .friend-links-header-content {
    margin: 0;
  }
}
</style>
