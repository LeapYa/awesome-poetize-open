/**
 * KaTeX 按需加载模块
 * 只有当文章内容包含数学公式时才加载 katex
 */

// 缓存已加载的 katex 插件
let markdownItKatexPlugin = null

/**
 * 检测内容是否包含数学公式
 * 支持 $...$ 行内公式和 $$...$$ 块级公式
 * @param {string} content - Markdown 内容
 * @returns {boolean}
 */
export function hasMathFormula(content) {
  if (!content || typeof content !== 'string') {
    return false
  }
  
  // 检测 LaTeX 数学公式模式
  // $...$ 行内公式（排除 $$ 和转义的 \$）
  // $$...$$ 块级公式
  const mathPatterns = [
    /\$\$[\s\S]+?\$\$/,           // 块级公式 $$...$$
    /(?<!\$)\$(?!\$)[^\$\n]+?\$(?!\$)/,  // 行内公式 $...$（排除 $$）
    /\\begin\{(equation|align|gather|matrix|bmatrix|pmatrix|vmatrix|cases)\}/i,  // LaTeX 环境
    /\\frac\{/,                    // 分数
    /\\sum|\\int|\\prod|\\lim/,    // 常见数学符号
    /\\sqrt\{/,                    // 根号
    /\\alpha|\\beta|\\gamma|\\delta|\\epsilon|\\theta|\\lambda|\\mu|\\pi|\\sigma|\\omega/i,  // 希腊字母
  ]
  
  return mathPatterns.some(pattern => pattern.test(content))
}

/**
 * 动态加载 markdown-it-katex 插件
 * @returns {Promise<Function|null>} 返回插件函数，如果加载失败返回 null
 */
export async function loadMarkdownItKatex() {
  if (markdownItKatexPlugin) {
    return markdownItKatexPlugin
  }
  
  try {
    const module = await import(/* @vite-ignore */ '@iktakahiro/markdown-it-katex')
    markdownItKatexPlugin = module.default || module
    return markdownItKatexPlugin
  } catch (error) {
    console.warn('Failed to load markdown-it-katex:', error)
    return null
  }
}

/**
 * 创建 MarkdownIt 实例，根据内容决定是否加载 katex
 * @param {Object} MarkdownIt - MarkdownIt 类
 * @param {Object} multimdTable - markdown-it-multimd-table 插件
 * @param {string} content - 要渲染的内容（用于检测是否需要 katex）
 * @returns {Promise<Object>} 返回配置好的 md 实例
 */
export async function createMarkdownRenderer(MarkdownIt, multimdTable, content) {
  const md = new MarkdownIt({ breaks: true }).use(multimdTable)
  
  // 只有检测到数学公式时才加载 katex
  if (hasMathFormula(content)) {
    const katexPlugin = await loadMarkdownItKatex()
    if (katexPlugin) {
      md.use(katexPlugin)
    }
  }
  
  return md
}
