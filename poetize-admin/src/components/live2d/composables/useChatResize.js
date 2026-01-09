/**
 * 聊天面板缩放功能 Composable
 * Vue2.7 Composition API
 */
import { ref, onMounted, onUnmounted } from 'vue'

export function useChatResize(panelRef) {
  const isResizing = ref(false)
  const resizeDirection = ref('')
  const startX = ref(0)
  const startY = ref(0)
  const startWidth = ref(0)
  const startHeight = ref(0)
  const currentWidth = ref(parseInt(localStorage.getItem('chat_panel_width')) || 380)
  const currentHeight = ref(parseInt(localStorage.getItem('chat_panel_height')) || 500)
  
  // 动态获取最小尺寸（移动端更小）
  const getMinWidth = () => window.innerWidth <= 768 ? 280 : 300
  const getMinHeight = () => window.innerWidth <= 768 ? 350 : 400
  
  // PC端建议的最大尺寸
  const SUGGESTED_MAX_WIDTH = 800
  const SUGGESTED_MAX_HEIGHT = 800
  
  // 当前面板位置（用于计算最大尺寸）
  const panelLeft = ref(0)
  const panelTop = ref(0)
  
  /**
   * 开始缩放
   */
  const handleResizeStart = (e, direction) => {
    e.preventDefault()
    e.stopPropagation()
    
    isResizing.value = true
    resizeDirection.value = direction
    
    const touch = e.touches ? e.touches[0] : e
    startX.value = touch.clientX
    startY.value = touch.clientY
    startWidth.value = currentWidth.value
    startHeight.value = currentHeight.value
    
    // 获取面板当前位置
    if (panelRef.value) {
      const rect = panelRef.value.getBoundingClientRect()
      panelLeft.value = rect.left
      panelTop.value = rect.top
    }
  }
  
  /**
   * 缩放中
   */
  const handleResizeMove = (e) => {
    if (!isResizing.value) return
    
    const touch = e.touches ? e.touches[0] : e
    const deltaX = touch.clientX - startX.value
    const deltaY = touch.clientY - startY.value
    
    let newWidth = startWidth.value
    let newHeight = startHeight.value
    
    // 根据方向计算新尺寸
    if (resizeDirection.value.includes('e')) {
      newWidth = startWidth.value + deltaX
    }
    if (resizeDirection.value.includes('w')) {
      newWidth = startWidth.value - deltaX
    }
    if (resizeDirection.value.includes('s')) {
      newHeight = startHeight.value + deltaY
    }
    if (resizeDirection.value.includes('n')) {
      newHeight = startHeight.value - deltaY
    }
    
    // 动态计算最大尺寸，确保不超出屏幕
    // 根据面板位置计算可用的最大宽度和高度
    const maxWidthByScreen = window.innerWidth - panelLeft.value
    const maxHeightByScreen = window.innerHeight - panelTop.value
    
    // 取建议最大值和屏幕限制的较小值
    const maxWidth = Math.min(SUGGESTED_MAX_WIDTH, maxWidthByScreen)
    const maxHeight = Math.min(SUGGESTED_MAX_HEIGHT, maxHeightByScreen)
    
    // 获取当前最小尺寸
    const minWidth = getMinWidth()
    const minHeight = getMinHeight()
    
    // 限制尺寸
    newWidth = Math.max(minWidth, Math.min(newWidth, maxWidth))
    newHeight = Math.max(minHeight, Math.min(newHeight, maxHeight))
    
    currentWidth.value = newWidth
    currentHeight.value = newHeight
  }
  
  /**
   * 结束缩放
   */
  const handleResizeEnd = () => {
    if (!isResizing.value) return
    
    isResizing.value = false
    resizeDirection.value = ''
    
    // 保存尺寸
    localStorage.setItem('chat_panel_width', currentWidth.value)
    localStorage.setItem('chat_panel_height', currentHeight.value)
  }
  
  // 挂载时绑定全局事件
  onMounted(() => {
    document.addEventListener('mousemove', handleResizeMove)
    document.addEventListener('mouseup', handleResizeEnd)
    document.addEventListener('touchmove', handleResizeMove)
    document.addEventListener('touchend', handleResizeEnd)
  })
  
  // 卸载时解绑
  onUnmounted(() => {
    document.removeEventListener('mousemove', handleResizeMove)
    document.removeEventListener('mouseup', handleResizeEnd)
    document.removeEventListener('touchmove', handleResizeMove)
    document.removeEventListener('touchend', handleResizeEnd)
  })
  
  /**
   * 强制重置缩放状态（用于清理）
   */
  const resetResizing = () => {
    isResizing.value = false
  }
  
  return {
    isResizing,
    currentWidth,
    currentHeight,
    handleResizeStart,
    resetResizing
  }
}
