/**
 * @file 编辑器主题管理器
 * @description
 * 从插件系统加载激活的文章主题，将配置映射到编辑器 CSS 变量，
 * 使编辑器预览效果与前台文章页保持一致。
 *
 * 映射关系（编辑器中标题未降级，直接对应）：
 *   用户 H1 → --editor-h1-icon / --editor-h1-color / --editor-h1-display
 *   用户 H2 → --editor-h2-icon / --editor-h2-color / --editor-h2-display
 *   用户 H3 → --editor-h3-icon / --editor-h3-color / --editor-h3-display
 *   用户 H4 → --editor-h4-icon / --editor-h4-color / --editor-h4-display
 *   用户 H5 → --editor-h5-icon / --editor-h5-color / --editor-h5-display
 */
import request from '@/utils/request'
import constant from '@/utils/constant'

// 编辑器中用户配置 key → 编辑器 CSS 变量中的 heading 级别
const USER_TO_EDITOR_MAP = {
  h1: 'h1',
  h2: 'h2',
  h3: 'h3',
  h4: 'h4',
  h5: 'h5',
}

// 每级标题有装饰时的默认 padding-left
const DEFAULT_PADDING = {
  h1: '40px',
  h2: '25px',
  h3: '20px',
  h4: '28px',
  h5: '0',
}

// localStorage 缓存（仅作为离线 fallback）
const CACHE_KEY = 'editor_theme_cache'

/**
 * 从后端获取当前激活的文章主题配置
 * 每次都请求后端以保证最新，localStorage 仅作为离线 fallback
 * @returns {Object|null}
 */
export async function fetchActiveTheme() {
  // 1. 先尝试从后端获取（保证最新）
  try {
    const res = await request.get(
      constant.baseURL + '/sysPlugin/getActiveArticleTheme',
      {},
      true
    )
    if (res && res.code === 200 && res.data && res.data.pluginConfig) {
      const config = JSON.parse(res.data.pluginConfig)
      try {
        localStorage.setItem(
          CACHE_KEY,
          JSON.stringify({ data: config, timestamp: Date.now() })
        )
      } catch (e) {
        // 缓存写入失败，不影响使用
      }
      return config
    }
  } catch (e) {
    console.warn('[EditorTheme] 获取主题失败，尝试使用缓存', e)
  }

  // 2. 后端请求失败时，回退到 localStorage 缓存
  try {
    const cached = localStorage.getItem(CACHE_KEY)
    if (cached) {
      const { data } = JSON.parse(cached)
      return data
    }
  } catch (e) {
    // 缓存也读取失败
  }

  return null
}

/**
 * 将主题配置应用到编辑器 CSS 变量
 * @param {Object} config - 主题配置
 */
export function applyEditorTheme(config) {
  if (!config || !config.headings) return

  const root = document.documentElement

  Object.entries(config.headings).forEach(([userLevel, settings]) => {
    const editorLevel = USER_TO_EDITOR_MAP[userLevel]
    if (!editorLevel) return

    // icon（emoji / 符号）
    const emoji = settings.show ? (settings.emoji || '') : ''
    root.style.setProperty(`--editor-${editorLevel}-icon`, `"${emoji}"`)

    // color（null 或空 → inherit）
    const color = settings.show && settings.color ? settings.color : 'inherit'
    root.style.setProperty(`--editor-${editorLevel}-color`, color)

    // display（show=false → none，隐藏 ::before）
    root.style.setProperty(
      `--editor-${editorLevel}-display`,
      settings.show ? 'inline' : 'none'
    )

    // padding（优先用户配置 → 有装饰用默认值 → 无装饰为 0）
    let padding = '0'
    if (settings.show && settings.emoji) {
      padding = settings.paddingLeft || DEFAULT_PADDING[editorLevel] || '0'
    } else if (settings.paddingLeft) {
      padding = settings.paddingLeft
    }
    root.style.setProperty(
      `--editor-${editorLevel}-padding`,
      padding
    )
  })
}

/**
 * 移除自定义编辑器 CSS 变量，恢复 :root 中的默认值
 */
export function resetEditorTheme() {
  const root = document.documentElement
  const props = [
    '--editor-h1-icon',
    '--editor-h1-color',
    '--editor-h1-display',
    '--editor-h1-padding',
    '--editor-h2-icon',
    '--editor-h2-color',
    '--editor-h2-display',
    '--editor-h2-padding',
    '--editor-h3-icon',
    '--editor-h3-color',
    '--editor-h3-display',
    '--editor-h3-padding',
    '--editor-h4-icon',
    '--editor-h4-color',
    '--editor-h4-display',
    '--editor-h4-padding',
    '--editor-h5-icon',
    '--editor-h5-color',
    '--editor-h5-display',
    '--editor-h5-padding',
  ]
  props.forEach((p) => root.style.removeProperty(p))
}

/**
 * 清除 localStorage 缓存，下次加载时强制从后端获取
 */
export function clearEditorThemeCache() {
  try {
    localStorage.removeItem(CACHE_KEY)
  } catch (e) {
    // ignore
  }
}

/**
 * 一键初始化：获取主题并应用到编辑器
 * @returns {Object|null} 主题配置
 */
export async function initEditorTheme() {
  const config = await fetchActiveTheme()
  if (config) {
    applyEditorTheme(config)
  }
  return config
}
