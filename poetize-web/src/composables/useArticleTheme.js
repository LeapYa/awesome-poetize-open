/**
 * 文章主题管理器
 * 从插件系统加载激活的文章主题，将用户视角的 H1~H5 配置
 * 映射到实际 CSS 变量 --heading-h2 ~ --heading-h6
 *
 * 用户视角 → 实际 HTML 映射：
 *   H1 → h2, H2 → h3, H3 → h4, H4 → h5, H5 → h6
 */
import request from '@/utils/request'
import constant from '@/utils/constant'

// 用户配置的 h1~h5 → 实际 CSS 变量的 h2~h6
const USER_TO_CSS_MAP = {
  h1: 'h2',
  h2: 'h3',
  h3: 'h4',
  h4: 'h5',
  h5: 'h6',
}

// 每级 CSS 标题有装饰时的默认 padding-left
const DEFAULT_PADDING = {
  h2: '40px',
  h3: '25px',
  h4: '20px',
  h5: '28px',
  h6: '20px',
}

// localStorage 缓存 key（仅作为离线 fallback）
const CACHE_KEY = 'article_theme_cache'

/**
 * 从后端获取当前激活的文章主题配置
 * 每次都请求后端以保证最新，localStorage 仅作为离线 fallback
 * @returns {Object|null} 主题配置对象，或 null 表示使用 CSS 默认值
 */
export async function fetchActiveTheme() {
  // 1. 先尝试从后端获取（保证最新）
  try {
    const res = await request.get(
      constant.baseURL + '/sysPlugin/getActiveArticleTheme'
    )
    if (res && res.code === 200 && res.data && res.data.pluginConfig) {
      const config = JSON.parse(res.data.pluginConfig)
      // 写入缓存（作为 fallback）
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
    console.warn('[ArticleTheme] 获取主题失败，尝试使用缓存', e)
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
 * 将主题配置应用到 document.documentElement 的 CSS 变量上
 * @param {Object} config - 主题配置，格式如 { headings: { h1: { emoji, color, show }, ... }, toc: { emoji, show } }
 */
export function applyTheme(config) {
  if (!config || !config.headings) return

  const root = document.documentElement

  Object.entries(config.headings).forEach(([userLevel, settings]) => {
    const cssLevel = USER_TO_CSS_MAP[userLevel]
    if (!cssLevel) return

    // emoji / content
    const emoji = settings.show ? (settings.emoji || '') : ''
    root.style.setProperty(`--heading-${cssLevel}-emoji`, `"${emoji}"`)

    // color（null 或空 → inherit）
    const color = settings.show && settings.color ? settings.color : 'inherit'
    root.style.setProperty(`--heading-${cssLevel}-color`, color)

    // display（show=false → none，隐藏 ::before）
    root.style.setProperty(
      `--heading-${cssLevel}-display`,
      settings.show ? 'inline' : 'none'
    )

    // padding（优先用户配置 → 有装饰用默认值 → 无装饰为 0）
    let padding = '0'
    if (settings.show && settings.emoji) {
      // 有装饰：优先用用户配置的 paddingLeft，否则用默认值
      padding = settings.paddingLeft || DEFAULT_PADDING[cssLevel] || '0'
    } else if (settings.paddingLeft) {
      // 无装饰但用户手动指定了 paddingLeft（可能想留空白）
      padding = settings.paddingLeft
    }
    root.style.setProperty(
      `--heading-${cssLevel}-padding`,
      padding
    )
  })

  // TOC emoji
  if (config.toc) {
    const tocEmoji = config.toc.show ? (config.toc.emoji || '') : ''
    root.style.setProperty('--toc-emoji', `"${tocEmoji}"`)
  }
}

/**
 * 获取 TOC emoji 字符串，用于 article.vue 中动态设置 data-toc-title
 * @param {Object} config - 主题配置
 * @returns {string|null} emoji 字符串（例如 "🏖️"），空字符串表示不显示，null 表示未配置（使用默认）
 */
export function getTocEmoji(config) {
  if (!config || !config.toc) return null // 未配置，使用默认
  if (!config.toc.show) return '' // 明确关闭
  return config.toc.emoji || '' // show=true 但没填 emoji → 不显示
}

/**
 * 移除自定义 CSS 变量，恢复 :root 中的默认值
 */
export function resetTheme() {
  const root = document.documentElement
  const props = [
    '--heading-h2-emoji',
    '--heading-h2-color',
    '--heading-h2-display',
    '--heading-h2-padding',
    '--heading-h3-emoji',
    '--heading-h3-color',
    '--heading-h3-display',
    '--heading-h3-padding',
    '--heading-h4-emoji',
    '--heading-h4-color',
    '--heading-h4-display',
    '--heading-h4-padding',
    '--heading-h5-emoji',
    '--heading-h5-color',
    '--heading-h5-display',
    '--heading-h5-padding',
    '--heading-h6-emoji',
    '--heading-h6-color',
    '--heading-h6-display',
    '--heading-h6-padding',
    '--toc-emoji',
  ]
  props.forEach((p) => root.style.removeProperty(p))
}

/**
 * 清除 localStorage 缓存，下次加载时强制从后端获取
 */
export function clearThemeCache() {
  try {
    localStorage.removeItem(CACHE_KEY)
  } catch (e) {
    // ignore
  }
}

/**
 * 从文章接口响应中提取并应用主题配置（同步，无闪烁）
 * 文章接口已合并返回 articleThemeConfig 字段，无需额外请求
 * @param {string|null} articleThemeConfigJson - 文章接口返回的 articleThemeConfig JSON 字符串
 * @returns {Object|null} 解析后的主题配置对象
 */
export function applyThemeFromArticle(articleThemeConfigJson) {
  if (!articleThemeConfigJson) return null

  try {
    const config = JSON.parse(articleThemeConfigJson)
    // 同步应用主题 —— 在渲染文章内容之前调用，彻底避免闪烁
    applyTheme(config)
    // 更新缓存（供其他场景使用）
    try {
      localStorage.setItem(
        CACHE_KEY,
        JSON.stringify({ data: config, timestamp: Date.now() })
      )
    } catch (e) {
      // 缓存写入失败，不影响使用
    }
    return config
  } catch (e) {
    console.warn('[ArticleTheme] 解析文章主题配置失败', e)
    return null
  }
}
