/**
 * 字体动态加载器
 * 通过动态创建CSS来加载字体文件，使用系统配置的CDN地址
 */

// 默认Unicode范围定义
let baseRange, level1Range, level2Range, otherRange

/**
 * 从远程URL加载JSON
 * @param {string} url 远程文件URL
 * @returns {Promise<object>} JSON对象
 */
async function fetchJson(url) {
  try {
    const response = await fetch(url)
    if (!response.ok) {
      throw new Error(`HTTP error ${response.status}`)
    }
    return await response.json()
  } catch (error) {
    console.error(`获取JSON文件失败: ${url}`, error)
    return null
  }
}

/**
 * 加载字体
 * @param {Object} sysConfig 系统配置
 */
export async function loadFonts(sysConfig) {
  const fontCdnBaseUrl =
    sysConfig['font.cdn.base-url'] || '/static/assets/font_chunks/'
  // 是否使用单一字体文件
  const useSingleFont = sysConfig['font.use.single'] === 'true'
  // 单一字体文件名
  const singleFontName = sysConfig['font.single.filename'] || 'font.woff2'
  // 是否从远程加载Unicode范围
  const loadUnicodeFromRemote = sysConfig['font.unicode.remote'] === 'true'
  // Unicode范围文件路径
  const unicodeJsonPath =
    sysConfig['font.unicode.path'] ||
    '/static/assets/font_chunks/unicode_ranges.json'

  // 如果需要从远程加载Unicode范围
  if (loadUnicodeFromRemote && !useSingleFont) {
    try {
      // 加载JSON文件
      const unicodeRanges = await fetchJson(unicodeJsonPath)

      if (unicodeRanges) {
        // 更新Unicode范围变量
        if (unicodeRanges.base) baseRange = unicodeRanges.base.join(',')
        if (unicodeRanges.level1) level1Range = unicodeRanges.level1.join(',')
        if (unicodeRanges.level2) level2Range = unicodeRanges.level2.join(',')
        if (unicodeRanges.other) otherRange = unicodeRanges.other.join(',')
      } else {
      }
    } catch (error) {
      console.error('加载远程Unicode范围失败，使用默认值', error)
    }
  } else if (!loadUnicodeFromRemote && !useSingleFont) {
    // 从本地加载Unicode范围
    const localUnicodeJsonPath =
      '/static/assets/font_chunks/unicode_ranges.json'
    try {
      // 加载本地JSON文件
      const unicodeRanges = await fetchJson(localUnicodeJsonPath)

      if (unicodeRanges) {
        // 更新Unicode范围变量
        if (unicodeRanges.base) baseRange = unicodeRanges.base.join(',')
        if (unicodeRanges.level1) level1Range = unicodeRanges.level1.join(',')
        if (unicodeRanges.level2) level2Range = unicodeRanges.level2.join(',')
        if (unicodeRanges.other) otherRange = unicodeRanges.other.join(',')
      } else {
      }
    } catch (error) {
      console.error('加载本地Unicode范围失败，使用默认值', error)
    }
  }

  // 创建style元素
  const style = document.createElement('style')
  style.type = 'text/css'
  style.id = 'dynamic-font-style'

  // 移除旧的样式（如果存在）
  const oldStyle = document.getElementById('dynamic-font-style')
  if (oldStyle) {
    oldStyle.parentNode.removeChild(oldStyle)
  }

  // 构建字体CSS
  let css = ''

  if (useSingleFont) {
    // 使用单一字体文件
    css = `
      @font-face {
        font-family: 'MyAwesomeFont';
        src: url('${fontCdnBaseUrl}${singleFontName}') format('woff2');
        font-weight: normal;
        font-style: normal;
        font-display: swap;
      }
    `
  } else {
    // 使用分块字体文件
    css = `
      @font-face {
        font-family: 'MyAwesomeFont';
        src: url('${fontCdnBaseUrl}font.base.woff2') format('woff2');
        font-weight: normal;
        font-style: normal;
        font-display: swap;
        unicode-range: ${baseRange};
      }
      
      @font-face {
        font-family: 'MyAwesomeFont';
        src: url('${fontCdnBaseUrl}font.level1.woff2') format('woff2');
        font-weight: normal;
        font-style: normal;
        font-display: swap;
        unicode-range: ${level1Range};
      }
      
      @font-face {
        font-family: 'MyAwesomeFont';
        src: url('${fontCdnBaseUrl}font.level2.woff2') format('woff2');
        font-weight: normal;
        font-style: normal;
        font-display: swap;
        unicode-range: ${level2Range};
      }
      
      @font-face {
        font-family: 'MyAwesomeFont';
        src: url('${fontCdnBaseUrl}font.other.woff2') format('woff2');
        font-weight: normal;
        font-style: normal;
        font-display: swap;
        unicode-range: ${otherRange};
      }
    `
  }

  // 设置样式内容并添加到文档
  style.textContent = css

  // 安全地添加样式到head，避免appendChild在文本节点上的错误
  try {
    if (document.head && document.head.nodeType === Node.ELEMENT_NODE) {
      document.head.appendChild(style)
    } else {
    }
  } catch (error) {
    console.error('添加字体样式失败:', error)
    // 尝试备用方法
    try {
      const head = document.querySelector('head')
      if (head && head.nodeType === Node.ELEMENT_NODE) {
        head.appendChild(style)
      }
    } catch (fallbackError) {
      console.error('备用方法也失败了:', fallbackError)
    }
  }
}

// 导出默认方法
export default {
  loadFonts,
}
