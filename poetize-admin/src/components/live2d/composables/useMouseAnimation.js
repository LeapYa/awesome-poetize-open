/**
 * 鼠标动画 Composable
 * Vue2.7 Composition API
 */
import { ref, onMounted, onUnmounted } from 'vue'

export function useMouseAnimation() {
  const enabled = ref(localStorage.getItem('showMouseAnimation') === '1')
  let idx = 0 // 文字索引
  
  /**
   * 创建动画元素（社会主义核心价值观）
   */
  const createAnimationElement = (x, y) => {
    const list = ["富强", "民主", "文明", "和谐", "自由", "平等", "公正", "法治", "爱国", "敬业", "诚信", "友善"]
    
    const span = document.createElement('span')
    span.textContent = list[idx]
    idx = (idx + 1) % list.length
    
    // 设置样式
    Object.assign(span.style, {
      "z-index": "1000",
      "top": (y - 20) + "px",
      "left": x + "px",
      "position": "absolute",
      "pointer-events": "none",
      "font-weight": "bold",
      "color": "#ff6651",
      "transition": "all 1.5s ease-out"
    })
    
    // 安全地添加到DOM
    if (document.body && typeof document.body.appendChild === 'function' && span && span.nodeType === Node.ELEMENT_NODE) {
      document.body.appendChild(span)
    } else {
      return
    }
    
    // 动画：向上飘并淡出
    setTimeout(() => {
      span.style.top = (y - 180) + "px"
      span.style.opacity = "0"
    }, 10)
    
    // 1.5秒后移除元素
    setTimeout(() => {
      if (span.parentNode) {
        span.parentNode.removeChild(span)
      }
    }, 1500)
  }
  
  /**
   * 点击事件处理
   */
  const handleClick = (e) => {
    if (!enabled.value) return
    
    // 忽略特定元素上的点击
    if (e.target.closest('#waifu') || 
        e.target.closest('#waifu-chat') ||
        e.target.closest('button') ||
        e.target.closest('a')) {
      return
    }
    
    createAnimationElement(e.clientX, e.clientY)
  }
  
  /**
   * 切换动画
   */
  const toggle = () => {
    enabled.value = !enabled.value
    localStorage.setItem('showMouseAnimation', enabled.value ? '1' : '0')
    return enabled.value
  }
  
  // 挂载时绑定
  onMounted(() => {
    document.body.addEventListener('click', handleClick)
  })
  
  // 卸载时解绑
  onUnmounted(() => {
    document.body.removeEventListener('click', handleClick)
  })
  
  return {
    enabled,
    toggle
  }
}
