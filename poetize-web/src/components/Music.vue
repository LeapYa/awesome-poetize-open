<template>
  <div>
    <div class="music-container">
      <!-- 封面 -->
      <div class="music-header my-animation-slide-top">
        <!-- 背景图片 -->
        <video
          class="index-video"
          autoplay="autoplay"
          muted="muted"
          loop="loop"
          preload="auto"
          playsinline="true"
          webkit-playsinline="true"
          x5-video-player-type="h5"
          x5-video-player-fullscreen="true"
          style="pointer-events: none;"
          :src="
            mainStore.sysConfig['webStaticResourcePrefix'] +
            'assets/backgroundVideo.mp4'
          "
        ></video>
        <div class="music-header-content">
          <!-- 标题 -->
          <div style="color: var(--white); margin: 0 10px">
            <div style="font-size: 30px; font-weight: bold; line-height: 2">
              曲乐
            </div>
            <div style="font-size: 16px; opacity: 0.8; margin-top: 10px">
              一曲肝肠断，天涯何处觅知音
            </div>
          </div>
        </div>
      </div>

      <!-- 内容 -->
      <div class="music-content">
        <funny></funny>
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


export default {
  components: {
    myFooter: defineAsyncComponent(() => import('./common/myFooter')),
    funny: defineAsyncComponent(() => import('./funny')),
  },

  data() {
    return {}
  },

  computed: {
    mainStore() {
      return useMainStore()
    },
  },

  watch: {},

  created() {
    // 设置页面标题（注：setTitle 方法已移除，页面标题由路由或组件自行管理）
    // document.title = "曲乐 | " + this.mainStore.sysConfig.webName;
  },

  mounted() {},

  methods: {},
}
</script>

<style scoped>
.music-container {
  padding: 25px;
  background: var(--favoriteBg);
}
.music-header {
  margin: 60px auto 30px;
  height: 200px;
  position: relative;
  overflow: hidden;
  border-radius: 20px;
  max-width: 1200px;
}
.index-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.music-header-content {
  position: absolute;
  left: 20px;
  top: 25px;
  padding: 5px 20px;
  margin: 10px;
}
.music-content {
  margin: 0 auto;
  max-width: 1200px;
}
@media screen and (max-width: 906px) {
  .music-header {
    height: 180px;
  }
  .music-header-content {
    margin: 0;
  }
}
@media screen and (max-width: 636px) {
  .music-header {
    height: 160px;
  }
  .music-header-content {
    margin: 0;
  }
}
</style>
