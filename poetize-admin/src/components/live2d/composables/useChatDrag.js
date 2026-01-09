/**
 * 聊天面板拖拽功能 Composable
 * Vue2.7 Composition API
 */
import { ref, computed, onMounted, onUnmounted } from 'vue'

export function useChatDrag(headerRef) {
  const isDragging = ref(false)
  const startX = ref(0)
  const startY = ref(0)
  const currentX = ref(0)
  const currentY = ref(0)
  const savedX = ref(parseInt(localStorage.getItem('chat_panel_x')) || null)
  const savedY = ref(parseInt(localStorage.getItem('chat_panel_y')) || null)
  
  // 缓存面板尺寸，避免每次都读取DOM
  const panelWidth = ref(0)
  const panelHeight = ref(0)
  
  /**
   * 面板样式
   */
  const panelStyle = computed(() => {
    const style = {}
    
    if (savedX.value !== null && savedY.value !== null) {
      style.left = `${savedX.value}px`
      style.top = `${savedY.value}px`
      style.right = 'auto'
      style.bottom = 'auto'
    }
    
    return style
  })
  
  /**
   * 开始拖拽
   */
  const handleDragStart = (e) => {
    if (!headerRef.value) return
    
    // 立即标记为拖拽状态
    isDragging.value = true
    
    // 获取初始位置
    const touch = e.touches ? e.touches[0] : e
    startX.value = touch.clientX
    startY.value = touch.clientY
    
    // 获取面板当前位置和尺寸（只在开始时获取一次）
    const panel = headerRef.value.parentElement
    if (panel) {
      const rect = panel.getBoundingClientRect()
      currentX.value = rect.left
      currentY.value = rect.top
      panelWidth.value = rect.width
      panelHeight.value = rect.height
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
    
    // 计算新位置
    const newX = currentX.value + deltaX
    const newY = currentY.value + deltaY
    
    // 限制在视口内（使用缓存的尺寸）
    const maxX = window.innerWidth - panelWidth.value
    const maxY = window.innerHeight - panelHeight.value
    
    const boundedX = Math.max(0, Math.min(newX, maxX))
    const boundedY = Math.max(0, Math.min(newY, maxY))
    
    // 更新位置
    savedX.value = boundedX
    savedY.value = boundedY
  }
  
  /**
   * 结束拖拽
   */
  const handleDragEnd = () => {
    if (!isDragging.value) return
    
    isDragging.value = false
    
    // 保存位置
    if (savedX.value !== null && savedY.value !== null) {
      localStorage.setItem('chat_panel_x', savedX.value)
      localStorage.setItem('chat_panel_y', savedY.value)
    }
  }
  
  // 挂载时绑定全局move/end事件
  onMounted(() => {
    document.addEventListener('mousemove', handleDragMove)
    document.addEventListener('mouseup', handleDragEnd)
    document.addEventListener('touchmove', handleDragMove)
    document.addEventListener('touchend', handleDragEnd)
  })
  
  // 卸载时解绑
  onUnmounted(() => {
    document.removeEventListener('mousemove', handleDragMove)
    document.removeEventListener('mouseup', handleDragEnd)
    document.removeEventListener('touchmove', handleDragMove)
    document.removeEventListener('touchend', handleDragEnd)
  })
  
  /**
   * 强制重置拖拽状态（用于清理）
   */
  const resetDragging = () => {
    isDragging.value = false
  }
  
  return {
    isDragging,
    panelStyle,
    handleDragStart,
    resetDragging
  }
}
