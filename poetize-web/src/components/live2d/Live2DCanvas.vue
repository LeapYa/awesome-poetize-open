<template>
  <canvas
    id="live2d"
    ref="canvasRef"
    class="live2d-canvas"
    :class="{ dragging: isDragging }"
    width="800"
    height="800"
    :style="canvasStyle"
    @click="handleClick"
  />
</template>

<script>
import { ref, watch, onMounted, computed, nextTick } from 'vue'
import { useLive2DStore } from '@/stores/live2d'
import { useDrag } from './composables/useDrag'
import constant from '@/utils/constant'

export default {
  name: 'Live2DCanvas',

  props: {
    modelId: {
      type: Number,
      required: true,
    },
    textureId: {
      type: Number,
      default: 0,
    },
  },

  emits: ['click'],

  setup(props, { emit }) {
    const store = useLive2DStore()
    const canvasRef = ref(null)
    const modelLoaded = ref(false)

    // 拖拽功能
    const { isDragging } = useDrag(canvasRef)

    /**
     * 加载Live2D模型
     */
    const loadModel = async () => {
      try {
        // 检查Live2D库是否已加载
        if (typeof window.loadlive2d !== 'function') {
          await waitForLive2D()
        }

        // 确保模型列表已加载
        if (!store.modelList) {
          await store.loadModelList()
        }

        // 获取模型路径
        const modelList = store.modelList
        if (
          !modelList ||
          !modelList.models ||
          !modelList.models[props.modelId]
        ) {
          console.error('模型列表无效')
          return
        }

        const models = modelList.models[props.modelId]
        // 兼容两种格式：字符串或数组
        let modelPath
        if (Array.isArray(models)) {
          modelPath = models[props.textureId] || models[0]
        } else {
          modelPath = models
        }

        // 构建完整URL
        const cdnPath = constant.cdnPath
        const modelUrl = `${cdnPath}model/${modelPath}/index.json`

        // 调用Live2D加载函数
        window.loadlive2d('live2d', modelUrl)

        modelLoaded.value = true
      } catch (error) {
        console.error('模型加载失败:', error)
      }
    }

    /**
     * 等待Live2D库加载
     */
    const waitForLive2D = () => {
      return new Promise((resolve, reject) => {
        // 如果已经加载，立即返回
        if (typeof window.loadlive2d === 'function') {
          resolve()
          return
        }

        let attempts = 0
        const maxAttempts = 100 // 最多等待10秒

        const checkInterval = setInterval(() => {
          attempts++

          if (typeof window.loadlive2d === 'function') {
            clearInterval(checkInterval)
            resolve()
          } else if (attempts >= maxAttempts) {
            clearInterval(checkInterval)
            reject(new Error('Live2D库加载超时，请刷新页面重试'))
          }
        }, 100)
      })
    }

    /**
     * Canvas点击事件
     */
    const handleClick = () => {
      emit('click')
    }

    // 监听模型ID变化
    watch(
      () => props.modelId,
      () => {
        loadModel()
      }
    )

    // 监听材质ID变化
    watch(
      () => props.textureId,
      () => {
        loadModel()
      }
    )

    // 计算缩放样式（用户配置的scale，移动端整体缩放已在widgetStyle处理）
    const canvasStyle = computed(() => {
      const scale = store.currentModelScale || 1.0
      return {
        transform: `scale(${scale})`,
        transformOrigin: 'bottom left'
      }
    })

    // 组件挂载
    onMounted(() => {
      // 延迟加载，确保DOM已渲染
      nextTick(() => {
        loadModel()
      })
    })

    return {
      canvasRef,
      isDragging,
      handleClick,
      canvasStyle
    }
  },
}
</script>

<style scoped>
.live2d-canvas {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 280px;
  height: 280px;
  cursor: grab;
  user-select: none;
  -webkit-tap-highlight-color: transparent;
  pointer-events: auto;
}
.live2d-canvas.dragging {
  cursor: grabbing;
}
@media screen and (max-width: 768px) {
  .live2d-canvas {
    width: 200px;
    height: 200px;
  }
}
</style>
