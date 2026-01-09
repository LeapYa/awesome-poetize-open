<template>
  <div v-if="enabled" class="live2d-widget-container">
    <transition name="slide-up">
      <div
        v-show="visible"
        id="waifu"
        class="live2d-widget"
        :style="widgetStyle"
      >
        <Live2DTips
          v-if="currentMessage"
          :message="currentMessage"
        />
        
        <Live2DCanvas
          :model-id="currentModelId"
          :texture-id="currentTextureId"
          @click="handleCanvasClick"
        />
        
        <Live2DToolbar
          @chat="toggleChat"
          @change-model="loadRandomModel"
          @change-texture="changeTexture"
          @toggle-mouse-animation="handleMouseAnimationToggle"
          @close="hide"
        />
      </div>
    </transition>
    
    <Live2DToggle
      v-show="!visible"
      @click="show"
    />
  </div>
</template>

<script>
import { computed, onMounted } from 'vue'
import { useLive2D } from './composables/useLive2D'
import { useEvents } from './composables/useEvents'
import { useMouseAnimation } from './composables/useMouseAnimation'
import { useLive2DStore } from '@/stores/live2d'

export default {
  name: 'Live2DWidget',
  
  components: {
    Live2DTips: () => import('./Live2DTips.vue'),
    Live2DCanvas: () => import('./Live2DCanvas.vue'),
    Live2DToolbar: () => import('./Live2DToolbar.vue'),
    Live2DToggle: () => import('./Live2DToggle.vue')
  },
  
  setup() {
    const store = useLive2DStore()
    const live2d = useLive2D()
    
    // 事件监听
    useEvents()
    
    // 鼠标动画
    const mouseAnimation = useMouseAnimation()
    
    const currentModelId = computed(() => store.currentModelId)
    const currentTextureId = computed(() => store.currentTextureId)
    const position = computed(() => store.position)
    
    const widgetStyle = computed(() => {
      const style = {}
      
      // 如果有保存的X位置，使用保存的位置
      if (position.value.x !== null) {
        style.left = `${position.value.x}px`
        style.right = 'auto'
      }
      // Y 坐标保持底部对齐（使用 CSS 默认的 bottom: 0）

      // 移动端整体缩放（检测触摸设备或小屏幕）
      const isMobile = window.matchMedia('(max-width: 768px)').matches || 
                       window.matchMedia('(hover: none) and (pointer: coarse)').matches
      if (isMobile) {
        style.transform = 'scale(0.7)'
        style.transformOrigin = 'bottom left'
      }
      
      return style
    })
    
    const handleCanvasClick = () => {
      const messages = [
        '好开心你注意到我了！',
        '感谢你的互动！',
        '你好呀！很高兴认识你',
        '哇，你点我了！'
      ]
      live2d.showMessage(messages, 5000, 8)
    }
    
    const handleMouseAnimationToggle = () => {
      const isEnabled = mouseAnimation.toggle()
      live2d.showMessage(
        isEnabled ? '哈哈，要牢记社会主义核心价值观哦！' : '今天你爱国了吗？',
        6000,
        9
      )
    }
    
    onMounted(async () => {
      await live2d.init()
    })
    
    return {
      enabled: live2d.enabled,
      visible: live2d.visible,
      currentMessage: live2d.currentMessage,
      currentModelId,
      currentTextureId,
      widgetStyle,
      handleCanvasClick,
      toggleChat: live2d.toggleChat,
      loadRandomModel: live2d.loadRandomModel,
      changeTexture: live2d.changeTexture,
      handleMouseAnimationToggle,
      hide: live2d.hide,
      show: live2d.show
    }
  }
}
</script>

<style scoped>
.live2d-widget-container {
  position: fixed;
  z-index: 999;
}

.live2d-widget {
  position: fixed;
  z-index: 999;
  /* 位置由live2d.css中的#waifu样式控制 (right: 0; bottom: 0;) */
}

.slide-up-enter-active, .slide-up-leave-active {
  transition: all 1s ease;
}

.slide-up-enter, .slide-up-leave-to {
  transform: translateY(500px);
}
</style>
