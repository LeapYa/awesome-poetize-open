/**
 * 鼠标动画 Composable（用于 Live2D 工具栏）
 * 注意：此文件已废弃，鼠标点击效果已统一由后端 mouseClickEffect 配置控制
 * 保留此文件仅为兼容 Live2D 工具栏的 toggle 功能
 * 
 * Vue2.7 Composition API
 */
import { ref, onMounted, onUnmounted } from 'vue'

export function useMouseAnimation() {
  // 注意：此功能已由后端 mouseClickEffect 统一控制
  // 这里仅保留空的 toggle 方法用于兼容 Live2D 工具栏
  const enabled = ref(false)

  /**
   * 切换动画（已废弃）
   * 鼠标点击效果现在由后端 mouseClickEffect 配置控制
   */
  const toggle = () => {
    // 返回当前后端配置状态，不再支持本地切换
    return false
  }

  return {
    enabled,
    toggle,
  }
}
