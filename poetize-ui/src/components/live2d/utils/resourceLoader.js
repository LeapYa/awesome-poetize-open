/**
 * 资源加载工具
 * 用于异步加载外部CSS和JS资源
 */

/**
 * 检查资源是否已加载
 */
export function isResourceLoaded(url, type) {
  const selector = type === 'css'
    ? `link[href*="${url}"]`
    : `script[src*="${url}"]`

  return document.querySelector(selector) !== null
}

/**
 * 加载外部资源
 * @param {string} url - 资源URL
 * @param {string} type - 资源类型 'css' | 'js'
 * @returns {Promise<string>}
 */
export function loadExternalResource(url, type) {
  return new Promise((resolve, reject) => {
    // 检查是否已加载
    if (isResourceLoaded(url, type)) {
      return resolve(url)
    }

    let tag

    if (type === 'css') {
      tag = document.createElement('link')
      tag.rel = 'stylesheet'
      tag.href = url
    } else if (type === 'js') {
      tag = document.createElement('script')
      tag.src = url
      tag.async = true
    } else {
      return reject(new Error(`不支持的资源类型: ${type}`))
    }

    tag.onload = () => {
      resolve(url)
    }

    tag.onerror = () => {
      console.error(`资源加载失败: ${url}`)
      reject(new Error(`资源加载失败: ${url}`))
    }

    document.head.appendChild(tag)
  })
}

/**
 * 批量加载资源
 * @param {Array} resources - 资源数组 [{url, type}]
 * @returns {Promise<Array>}
 */
export function loadResources(resources) {
  return Promise.all(
    resources.map(({ url, type }) => loadExternalResource(url, type))
  )
}

/**
 * 预加载资源（使用link preload）
 * @param {string} url - 资源URL
 * @param {string} as - 资源类型 'script' | 'style' | 'fetch'
 */
export function preloadResource(url, as) {
  if (document.querySelector(`link[rel="preload"][href="${url}"]`)) {
    return // 已存在
  }

  const link = document.createElement('link')
  link.rel = 'preload'
  link.href = url
  link.as = as

  if (as === 'fetch') {
    link.crossOrigin = 'anonymous'
  }

  document.head.appendChild(link)
}

/**
 * 检查Live2D库是否已加载
 */
export function isLive2DLoaded() {
  return typeof window.loadlive2d === 'function'
}

/**
 * 检查KaTeX是否已加载
 */
export function isKatexLoaded() {
  return typeof window.katex !== 'undefined'
}

/**
 * 检查Markdown-it是否已加载
 */
export function isMarkdownItLoaded() {
  return typeof window.markdownit !== 'undefined'
}

/**
 * 检查Mermaid是否已加载
 */
export function isMermaidLoaded() {
  return typeof window.mermaid !== 'undefined'
}

/**
 * 加载Live2D相关资源
 * @param {string} live2dPath - Live2D资源路径
 */
export async function loadLive2DResources(live2dPath) {
  const resources = [
    { url: `${live2dPath}waifu.css`, type: 'css' },
    { url: `${live2dPath}live2d.min.js`, type: 'js' },
    { url: `${live2dPath}waifu-drag.css`, type: 'css' },
    { url: `${live2dPath}waifu-drag.js`, type: 'js' }
  ]

  try {
    await loadResources(resources)
    return true
  } catch (error) {
    console.error('Live2D资源加载失败:', error)
    return false
  }
}

/**
 * 加载Markdown渲染所需资源
 */
export async function loadMarkdownResources() {
  const resources = []

  // KaTeX
  if (!isKatexLoaded()) {
    resources.push(
      { url: '/libs/css/katex.min.css', type: 'css' },
      { url: '/libs/js/katex.min.js', type: 'js' }
    )
  }

  // Markdown-it
  if (!isMarkdownItLoaded()) {
    resources.push(
      { url: '/libs/js/markdown-it.min.js', type: 'js' }
    )
  }

  if (resources.length === 0) {
    return true
  }

  try {
    await loadResources(resources)
    return true
  } catch (error) {
    return false
  }
}

/**
 * 加载Mermaid图表库
 */
export async function loadMermaidResources() {
  if (isMermaidLoaded()) {
    return true
  }

  try {
    // 使用本地导入的Mermaid
    const mermaid = await import('mermaid')

    // 将mermaid挂载到window对象，供其他地方使用
    window.mermaid = mermaid.default || mermaid

    // 检测是否为暗色模式
    const isDark = document.documentElement.classList.contains('dark-mode') ||
      document.body.classList.contains('dark-mode')

    // 初始化Mermaid配置
    window.mermaid.initialize({
      startOnLoad: false,
      theme: isDark ? 'dark' : 'default',
      securityLevel: 'loose',
      fontFamily: 'Arial, sans-serif',
      themeVariables: isDark ? {
        // 深色模式的自定义主题变量
        darkMode: true,
        background: '#1e1e1e',
        primaryColor: '#4a9eff',
        primaryTextColor: '#ffffff',
        primaryBorderColor: '#4a9eff',
        lineColor: '#6b6b6b',
        secondaryColor: '#2d2d2d',
        tertiaryColor: '#3a3a3a',
        mainBkg: '#2d2d2d',
        secondBkg: '#383838',
        mainContrastColor: '#ffffff',
        darkTextColor: '#ffffff',
        textColor: '#e0e0e0',
        labelTextColor: '#e0e0e0',
        fontSize: '14px'
      } : {
        fontSize: '14px'
      }
    })

    return true
  } catch (error) {
    return false
  }
}

/**
 * 检查ECharts是否已加载
 */
export function isEChartsLoaded() {
  return typeof window.echarts !== 'undefined'
}

/**
 * 加载ECharts图表库（模块化导入优化）
 */
export async function loadEChartsResources() {
  if (isEChartsLoaded()) {
    return true
  }

  try {
    // 模块化导入 ECharts，只加载需要的组件（Tree Shaking 优化）
    const echarts = await import('echarts/core')

    // 导入需要的图表类型
    const { LineChart, BarChart, PieChart, ScatterChart, RadarChart, GaugeChart } = await import('echarts/charts')

    // 导入需要的组件
    const {
      TitleComponent,
      TooltipComponent,
      GridComponent,
      LegendComponent,
      DataZoomComponent,
      ToolboxComponent,
      MarkLineComponent,
      MarkPointComponent
    } = await import('echarts/components')

    // 导入渲染器
    const { CanvasRenderer } = await import('echarts/renderers')

    // 注册所有模块
    echarts.use([
      LineChart, BarChart, PieChart, ScatterChart, RadarChart, GaugeChart,
      TitleComponent, TooltipComponent, GridComponent,
      LegendComponent, DataZoomComponent, ToolboxComponent,
      MarkLineComponent, MarkPointComponent,
      CanvasRenderer
    ])

    // 将echarts挂载到window对象，供其他地方使用
    window.echarts = echarts

    return true
  } catch (error) {
    console.error('ECharts模块化加载失败:', error)
    return false
  }
}


/**
 * 检查代码高亮库是否已加载
 */
export function isHighlightJsLoaded() {
  return typeof window.hljs !== 'undefined'
}

/**
 * 加载代码高亮资源
 */
export async function loadHighlightResources() {
  if (isHighlightJsLoaded()) {
    return true
  }

  try {
    const resources = [
      { url: '/libs/css/highlight.min.css', type: 'css' },
      { url: '/libs/js/highlight.min.js', type: 'js' }
    ]

    await loadResources(resources)

    // 加载行号插件
    if (typeof window.hljs !== 'undefined') {
      await loadExternalResource('/libs/js/highlightjs-line-numbers.min.js', 'js')
    }

    return true
  } catch (error) {
    return false
  }
}

/**
 * 检查Clipboard.js是否已加载
 */
export function isClipboardLoaded() {
  return typeof window.ClipboardJS !== 'undefined'
}

/**
 * 加载Clipboard.js（代码复制功能）
 */
export async function loadClipboardResources() {
  if (isClipboardLoaded()) {
    return true
  }

  try {
    await loadExternalResource('/libs/js/clipboard.min.js', 'js')
    return true
  } catch (error) {
    return false
  }
}

/**
 * 检查KaTeX是否已加载（更新为检查全局window对象）
 */
export function isKatexLoadedGlobal() {
  return typeof window.katex !== 'undefined'
}

/**
 * 加载KaTeX数学公式库
 */
export async function loadKatexResources() {
  if (isKatexLoadedGlobal()) {
    return true
  }

  try {
    const resources = [
      { url: '/libs/css/katex.min.css', type: 'css' },
      { url: '/libs/js/katex.min.js', type: 'js' }
    ]

    await loadResources(resources)
    return true
  } catch (error) {
    return false
  }
}

/**
 * 检查Qiniu SDK是否已加载
 */
export function isQiniuLoaded() {
  return typeof window.qiniu !== 'undefined'
}

/**
 * 加载七牛云SDK（仅在上传时需要）
 */
export async function loadQiniuResources() {
  if (isQiniuLoaded()) {
    return true
  }

  try {
    await loadExternalResource('/libs/js/qiniu.min.js', 'js')
    return true
  } catch (error) {
    return false
  }
}

/**
 * 检查Markdown-it是否已加载（全局版本）
 */
export function isMarkdownItLoadedGlobal() {
  return typeof window.markdownit !== 'undefined'
}

/**
 * 加载Markdown-it库
 */
export async function loadMarkdownItResources() {
  if (isMarkdownItLoadedGlobal()) {
    return true
  }

  try {
    await loadExternalResource('/libs/js/markdown-it.min.js', 'js')
    return true
  } catch (error) {
    return false
  }
}
