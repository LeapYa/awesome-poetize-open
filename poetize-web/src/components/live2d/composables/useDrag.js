/**
 * 拖拽功能 Composable
 * Vue2.7 Composition API
 */
import { ref, onMounted, onUnmounted } from 'vue'
import { useLive2DStore } from '@/stores/live2d'

export function useDrag(elementRef) {
  const store = useLive2DStore()

  const isDragging = ref(false)
  const hasMoved = ref(false) // 是否真正移动了
  const startX = ref(0)
  const startY = ref(0)
  const currentX = ref(0)
  const currentY = ref(0)
  const DRAG_THRESHOLD = 5 // 拖拽阈值（像素），移动超过这个距离才算拖拽

  /**
   * 开始拖拽
   */
  const handleDragStart = (e) => {
    // 阻止默认行为，但不阻止冒泡，让点击事件能触发
    e.preventDefault()

    isDragging.value = true
    hasMoved.value = false // 重置移动标志
    store.startDragging()

    // 获取初始位置
    const touch = e.touches ? e.touches[0] : e
    startX.value = touch.clientX
    startY.value = touch.clientY

    // 获取父元素（waifu容器）的位置
    const parentElement = elementRef.value?.parentElement
    if (parentElement) {
      const rect = parentElement.getBoundingClientRect()
      currentX.value = rect.left
      currentY.value = rect.top
    }
  }

  /**
   * 拖拽中
   */
  const handleDragMove = (e) => {
    if (!isDragging.value) return

    const touch = e.touches ? e.touches[0] : e
    const deltaX = touch.clientX - startX.value
    const deltaY = touch.clientY - startY.value

    // 检查是否超过拖拽阈值（只检测横向距离）
    const distance = Math.abs(deltaX)
    if (distance > DRAG_THRESHOLD) {
      // 第一次超过阈值时显示消息
      if (!hasMoved.value) {
        const messages = [
          '是我挡路了吗？',
          '你想把我拖到哪里去呀？',
          '要把我移到哪里呢？',
          '好啦好啦，我这就让开～',
        ]
        const randomMessage =
          messages[Math.floor(Math.random() * messages.length)]
        store.showMessage(randomMessage, 3000, 8)
      }
      hasMoved.value = true
      e.preventDefault() // 只有真正拖拽时才阻止默认行为
    } else {
      // 未超过阈值，不算拖拽
      return
    }

    // 计算新位置（只允许横向移动）
    const newX = currentX.value + deltaX
    // Y 坐标不变，保持底部对齐

    // 获取父元素
    const parentElement = elementRef.value?.parentElement
    if (!parentElement) return

    // 限制在视口内（只限制X轴）
    const maxX = window.innerWidth - (parentElement.offsetWidth || 280)

    const boundedX = Math.max(0, Math.min(newX, maxX))

    // 只更新 X 坐标，Y 保持 null（底部对齐）
    store.updatePosition(boundedX, null)
  }

  /**
   * 结束拖拽
   */
  const handleDragEnd = (e) => {
    if (!isDragging.value) return

    const wasDragging = hasMoved.value
    isDragging.value = false
    hasMoved.value = false
    store.stopDragging()

    if (wasDragging) {
      // 如果确实拖拽了，阻止点击事件触发
      e.preventDefault()
      e.stopPropagation()
    } else {
      // 未移动，允许点击事件触发
    }
  }

  /**
   * 绑定事件
   */
  const bindEvents = () => {
    if (!elementRef.value) return

    const element = elementRef.value

    // 鼠标事件
    element.addEventListener('mousedown', handleDragStart)
    document.addEventListener('mousemove', handleDragMove)
    document.addEventListener('mouseup', handleDragEnd)

    // 触摸事件
    element.addEventListener('touchstart', handleDragStart, { passive: false })
    document.addEventListener('touchmove', handleDragMove, { passive: false })
    document.addEventListener('touchend', handleDragEnd)
  }

  /**
   * 解绑事件
   */
  const unbindEvents = () => {
    if (!elementRef.value) return

    const element = elementRef.value

    // 鼠标事件
    element.removeEventListener('mousedown', handleDragStart)
    document.removeEventListener('mousemove', handleDragMove)
    document.removeEventListener('mouseup', handleDragEnd)

    // 触摸事件
    element.removeEventListener('touchstart', handleDragStart)
    document.removeEventListener('touchmove', handleDragMove)
    document.removeEventListener('touchend', handleDragEnd)
  }

  // 挂载时绑定
  onMounted(() => {
    // 延迟绑定，确保DOM已渲染
    setTimeout(() => {
      bindEvents()
    }, 100)
  })

  // 卸载时解绑
  onUnmounted(() => {
    unbindEvents()
  })

  return {
    isDragging: hasMoved, // 只有真正移动时才算拖拽中，用于显示grabbing光标
  }
}
