/**
 * Markdown-it 按需加载模块
 * 使用 npm 包而不是外部 JS 文件
 */

// 缓存已加载的 markdown-it 实例
let markdownItModule = null

/**
 * 检查 markdown-it 是否已加载
 * @returns {boolean}
 */
export function isMarkdownItLoaded() {
  return markdownItModule !== null
}

/**
 * 动态加载 markdown-it 库
 * @returns {Promise<Function|null>} 返回 MarkdownIt 构造函数
 */
export async function loadMarkdownIt() {
  if (markdownItModule) {
    return markdownItModule
  }
  
  try {
    const module = await import(/* @vite-ignore */ 'markdown-it')
    markdownItModule = module.default || module
    // 同时挂载到 window 上以兼容旧代码
    window.markdownit = markdownItModule
    return markdownItModule
  } catch (error) {
    console.warn('Failed to load markdown-it:', error)
    return null
  }
}

/**
 * 获取已加载的 markdown-it（同步方法，需要先调用 loadMarkdownIt）
 * @returns {Function|null}
 */
export function getMarkdownIt() {
  return markdownItModule
}

/**
 * 创建配置好的 markdown-it 实例
 * @param {Object} options - markdown-it 配置选项
 * @returns {Promise<Object|null>} markdown-it 实例
 */
export async function createMarkdownItInstance(options = {}) {
  const MarkdownIt = await loadMarkdownIt()
  if (!MarkdownIt) {
    return null
  }
  
  const defaultOptions = {
    html: false,
    linkify: true,
    breaks: true,
    typographer: true,
  }
  
  return new MarkdownIt({ ...defaultOptions, ...options })
}
