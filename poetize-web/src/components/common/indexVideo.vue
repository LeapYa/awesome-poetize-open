<template>
  <div
    class="index-video-wrap"
    :class="{ 'is-ready': ready, 'is-stalled': stalled }"
  >
    <!-- 视频未就绪时的扫光骨架，避免慢网下封面区域镂空 -->
    <div class="index-video-skeleton" aria-hidden="true"></div>
    <video
      ref="video"
      class="index-video"
      autoplay
      muted
      loop
      playsinline
      webkit-playsinline
      x5-video-player-type="h5"
      x5-video-player-fullscreen="true"
      preload="auto"
      :src="src"
      @loadeddata="markReady"
      @canplay="markReady"
      @playing="markReady"
      @error="markStalled"
    ></video>
  </div>
</template>

<script>
import { useMainStore } from '@/stores/main'

// 骨架最多扫光这么久，之后停住做静态占位，避免一直闪
const STALL_TIMEOUT = 10000

export default {
  name: 'IndexVideo',

  data() {
    return {
      ready: false,
      stalled: false,
    }
  },

  computed: {
    mainStore() {
      return useMainStore()
    },
    src() {
      const sysConfig = this.mainStore.sysConfig || {}
      const prefix = sysConfig['webStaticResourcePrefix'] || ''
      return prefix + 'assets/backgroundVideo.mp4'
    },
  },

  watch: {
    // 站点静态资源前缀晚到时地址会变，重新走一次骨架到淡入，避免露出空帧
    src(next, prev) {
      if (next === prev) {
        return
      }
      this.ready = false
      this.startStallTimer()
    },
  },

  mounted() {
    const video = this.$refs.video
    // 缓存命中时可能在挂载前就已有首帧
    if (video && video.readyState >= 2) {
      this.markReady()
      return
    }
    this.startStallTimer()
  },

  beforeUnmount() {
    clearTimeout(this.stallTimer)
  },

  methods: {
    startStallTimer() {
      this.stalled = false
      clearTimeout(this.stallTimer)
      this.stallTimer = setTimeout(() => {
        this.stalled = true
      }, STALL_TIMEOUT)
    },
    markReady() {
      if (this.ready) {
        return
      }
      this.ready = true
      this.stalled = false
      clearTimeout(this.stallTimer)
    },
    markStalled() {
      clearTimeout(this.stallTimer)
      this.stalled = true
    },
  },
}
</script>

<style scoped>
.index-video-wrap {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}
.index-video-skeleton {
  position: absolute;
  inset: 0;
  /* 深色底：封面标题是白字，浅色骨架会让文字在加载期看不见 */
  background-color: #33373d;
  background-image: linear-gradient(
    100deg,
    rgba(255, 255, 255, 0) 25%,
    rgba(255, 255, 255, 0.14) 50%,
    rgba(255, 255, 255, 0) 75%
  );
  background-size: 220% 100%;
  animation: index-video-shimmer 1.6s ease-in-out infinite;
  /* 出现要快（重新加载时立刻补上占位），消失时才延迟到视频显影之后 */
  transition: opacity 0.32s ease;
}
.index-video {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0;
  filter: blur(24px);
  transform: scale(1.08);
  transition: opacity 0.9s ease, filter 0.9s ease,
    transform 1.4s cubic-bezier(0.22, 0.61, 0.36, 1);
}
.is-ready .index-video {
  opacity: 1;
  filter: blur(0);
  transform: scale(1);
}
/* 视频完全显影后再撤掉骨架，避免中途露出页面底色 */
.is-ready .index-video-skeleton {
  opacity: 0;
  animation: none;
  transition: opacity 0.32s ease 0.7s;
}
.is-stalled .index-video-skeleton {
  animation: none;
}
/* 深色主题下换成中性深灰，避免写死的偏蓝底色在 #1e1e1e 页面上发亮 */
:global(html.dark-mode .index-video-skeleton) {
  background-color: #2b2b2e;
  background-image: linear-gradient(
    100deg,
    rgba(255, 255, 255, 0) 25%,
    rgba(255, 255, 255, 0.1) 50%,
    rgba(255, 255, 255, 0) 75%
  );
}
@keyframes index-video-shimmer {
  0% {
    background-position: 150% 0;
  }
  100% {
    background-position: -50% 0;
  }
}
@media (prefers-reduced-motion: reduce) {
  .index-video {
    filter: none;
    transform: none;
    transition: opacity 0.2s ease;
  }
  .index-video-skeleton {
    animation: none;
  }
}
</style>
