<template>
  <transition name="bounce">
    <div
      id="waifu-toggle"
      class="waifu-toggle"
      :class="{ 'waifu-toggle-active': isActive }"
      @click="handleClick"
    >
      <span class="waifu-toggle-text">看板娘</span>
    </div>
  </transition>
</template>

<script>
import { defineComponent, ref, onMounted, watch } from 'vue'
import { useLive2DStore } from '@/stores/live2d'

export default defineComponent({
  name: 'Live2DToggle',

  emits: ['click'],

  setup(props, { emit }) {
    const store = useLive2DStore()
    const isActive = ref(true) // 默认显示

    const handleClick = () => {
      isActive.value = false
      emit('click')
    }

    // 监听 visible 状态变化，当 toggle 需要显示时重置 isActive
    watch(() => store.visible, (newVisible) => {
      if (!newVisible) {
        // 当看板娘隐藏时（toggle 将要显示），延迟重置 isActive
        setTimeout(() => {
          isActive.value = true
        }, 100)
      }
    })

    // 组件挂载时，先隐藏然后滑入显示（实现刷新时的动画效果）
    onMounted(() => {
      // 先设置为 false，让元素处于隐藏状态
      isActive.value = false
      // 延迟设置为 true，触发 CSS transition 动画
      setTimeout(() => {
        isActive.value = true
      }, 100)
    })

    return {
      isActive,
      handleClick,
    }
  },
})
</script>

<style scoped>
.waifu-toggle {
  background-color: #fa0;
  border-radius: 5px;
  bottom: 66px;
  color: #fff;
  cursor: pointer;
  font-size: 12px;
  left: 0;
  margin-left: -100px;
  padding: 5px 2px 5px 5px;
  position: fixed;
  transition: margin-left 1s;
  width: 60px;
  writing-mode: vertical-rl;
  z-index: 998;
}

.waifu-toggle:hover {
  margin-left: -20px;
}

.waifu-toggle-text {
  user-select: none;
}

.waifu-toggle-active {
  margin-left: -40px;
}

.waifu-toggle-active:hover {
  margin-left: -30px;
}

/* 弹跳动画 */
.bounce-enter-active {
  animation: slideIn 1s ease;
}

.bounce-leave-active {
  animation: slideOut 0.5s ease;
}

@keyframes slideIn {
  0% {
    margin-left: -100px;
  }
  100% {
    margin-left: -40px;
  }
}

@keyframes slideOut {
  0% {
    margin-left: -40px;
  }
  100% {
    margin-left: -100px;
  }
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .waifu-toggle {
    bottom: 50px;
    font-size: 12px;
    width: 60px;
    padding: 10px 5px 10px 10px;
  }
  
  .waifu-toggle-active {
    margin-left: -50px;
  }
  
  .waifu-toggle-active:hover {
    margin-left: -40px;
  }
}

/* 小屏幕 - 已移除隐藏规则，允许移动端显示 */
</style>
