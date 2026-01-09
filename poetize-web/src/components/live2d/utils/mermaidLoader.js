/**
 * Mermaid 图表库加载器
 * 单独文件避免静态导入时污染其他模块
 */

/**
 * 检查Mermaid是否已加载
 */
export function isMermaidLoaded() {
  return typeof window.mermaid !== 'undefined'
}

/**
 * 加载Mermaid图表库
 */
export async function loadMermaidResources() {
  if (isMermaidLoaded()) {
    return true
  }

  try {
    // 使用动态导入加载 Mermaid
    // @vite-ignore 告诉 Vite 不要预分析此动态导入
    const mermaid = await import(/* @vite-ignore */ 'mermaid')

    // 将mermaid挂载到window对象，供其他地方使用
    window.mermaid = mermaid.default || mermaid

    // 检测是否为暗色模式
    const isDark =
      document.documentElement.classList.contains('dark-mode') ||
      document.body.classList.contains('dark-mode')

    // 初始化Mermaid配置
    // 始终使用 'default' 主题保持原始的节点颜色
    // 暗色模式下的文字颜色会通过 JavaScript 直接修改 SVG 元素来处理
    window.mermaid.initialize({
      startOnLoad: false,
      theme: 'default', // 始终使用 default 主题，保留原始节点颜色
      securityLevel: 'loose',
      fontFamily: 'Arial, sans-serif',
      themeVariables: {
        fontSize: '14px',
      },
    })

    return true
  } catch (error) {
    console.error('加载Mermaid失败:', error)
    return false
  }
}
