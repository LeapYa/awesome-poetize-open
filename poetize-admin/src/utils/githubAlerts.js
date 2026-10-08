/**
 * GitHub Alerts 语法兼容（> [!NOTE] / [!TIP] / [!IMPORTANT] / [!WARNING] / [!CAUTION]）
 *
 * 背景：
 * GitHub 于 2023-05 上线的私有扩展 —— 在引用块首行写 `[!NOTE]` 等标签，github.com 会渲染成
 * 彩色提示框。它**不是 CommonMark，也不在 GFM 正式规范里**，纯靠规范的第三方渲染器没有理由支持它。
 * 但 AI 写文章非常爱用这套语法，于是需要在这里兜住。
 *
 * 为什么不能「直接不管」：
 * 本站在 `article-style-protection.css` 里用 `!important` 把 `.entry-content blockquote` 锁成了
 * 「居中 + 两侧橙色 FontAwesome 引号」（text-align:center / padding:15px 50px / :before \f10d）
 * —— 那是为防主题污染写的保护规则。于是 `> [!NOTE] xxx` 会渲染成一段居中的大字引言，
 * 且字面量 `[!NOTE]` 原样露在正文里（markdown-it 里 `[!NOTE]` 既非链接也非引用定义，只当普通文本）。
 * 即「忽略」在这个站上不是中性降级，而是最难看的渲染，故做兼容。
 *
 * 实现要点：
 * 1. **渲染成 div，不复用 blockquote** —— 直接绕开上面那套 `!important` 引用块保护规则，
 *    否则要再写一堆 `!important` 对冲，很脆。
 * 2. 规则挂在 core 的 `inline` 之前：先改掉段落原始文本，再交给 inline 解析器正常处理，
 *    这样提示框正文里的 `**加粗**`、`` `代码` ``、链接等全部照常生效。
 * 3. 标题由 renderer 规则在开标签后追加，避免在 core 阶段往 token 数组中间插 token。
 *
 * 多语言：
 * 标题文案跟随**内容语言**（不是站点 UI 语言）—— 英文文章显示 Note，日文文章显示 補足。
 * 用法：`md.use(githubAlerts, { lang: 'en' })`，不传则回退到 en。
 * 语言码集合与 `utils/languageUtils.js` 的 TOC_TITLE_MAP 保持一致（站点支持的那 14 种）。
 * ⚠️ 这些译法是我们自己的自然译法，**不是**照抄 GitHub 的 UI 文案（其界面本地化字符串并未公开；
 * GitHub Docs 的中文风格指南里 NOTE 同时出现「注释」和「注意」两种写法，自相矛盾，不值得照搬）。
 * 需要改成别的措辞时，只动下面的 ALERT_TITLES 表即可。
 *
 * 支持范围（有意保守，宁可可预测也不要乱认）：
 *   - 只认引用块的**第一个**块级元素是段落、且段落以 `[!TYPE]` 开头
 *   - 大小写不敏感；`> [!NOTE] 同行正文` 与 `> [!NOTE]\n> 换行正文` 都支持
 *   - 白名单只有上述五种，`[!DANGER]` 之类一律不动，保持原样
 *   - 想让标签字面显示，写成 `\[!NOTE]`（反斜杠让首行不匹配，转义后渲染为 `[!NOTE]`）
 *   - 嵌套引用（`> > [!NOTE]`）与列表内的 alert 同样会被转换；外层引用块的居中装饰样式照旧生效，
 *     提示框正文由 `.md-alert` 自身的 text-align 拉回左对齐，不会被带偏
 *
 * 注意：`poetize-web` 与 `poetize-admin` 各有一份同名副本（两个独立 app，无共享源码目录），
 * 修改时请同步两边。
 */

/** 五种类型，顺序与 ALERT_TITLES 每行的五个标题严格对应 */
const ALERT_TYPES = ['note', 'tip', 'important', 'warning', 'caution']

/** 缺省语言：既用于未传 lang，也用于语言码认不出来时的兜底（与 TOC_TITLE_MAP 的 auto→en 一致） */
const FALLBACK_LANG = 'en'

/**
 * 标题文案表：顺序 = note / tip / important / warning / caution
 * 语言码取自 utils/languageUtils.js 的 TOC_TITLE_MAP。
 * `auto`（自动检测）故意不列，走兜底逻辑落到 en。
 * zh 与 zh-TW 这五个词恰好同形（注意/提示/重要/警告/小心），故两行内容相同；
 * 想把 CAUTION 在繁体下换成「謹慎」之类，直接改 zh-TW 那一行即可。
 * 对外导出，方便做语言切换器或写测试。
 */
export const ALERT_TITLES = {
  'zh': ['注意', '提示', '重要', '警告', '小心'],
  'zh-TW': ['注意', '提示', '重要', '警告', '小心'],
  'en': ['Note', 'Tip', 'Important', 'Warning', 'Caution'],
  'ja': ['補足', 'ヒント', '重要', '警告', '注意'],
  'ko': ['참고', '팁', '중요', '경고', '주의'],
  'fr': ['Remarque', 'Astuce', 'Important', 'Avertissement', 'Attention'],
  'de': ['Hinweis', 'Tipp', 'Wichtig', 'Warnung', 'Vorsicht'],
  'es': ['Nota', 'Sugerencia', 'Importante', 'Advertencia', 'Precaución'],
  'ru': ['Примечание', 'Совет', 'Важно', 'Предупреждение', 'Осторожно'],
  'pt': ['Nota', 'Dica', 'Importante', 'Aviso', 'Cuidado'],
  'it': ['Nota', 'Suggerimento', 'Importante', 'Avviso', 'Attenzione'],
  'ar': ['ملاحظة', 'نصيحة', 'مهم', 'تحذير', 'تنبيه'],
  'th': ['หมายเหตุ', 'เคล็ดลับ', 'สำคัญ', 'คำเตือน', 'ข้อควรระวัง'],
  'vi': ['Ghi chú', 'Mẹo', 'Quan trọng', 'Cảnh báo', 'Thận trọng']
}

/** 图形只保留本体：无外圈、无底盘，描边统一 currentColor（暗色模式下才不会变成亮斑） */
const ICONS = {
  note: '<circle cx="8" cy="8" r="6.4"/><path d="M8 7.5v4"/><path d="M8 4.7h.01"/>',
  tip: '<path d="M8 1.9a4.1 4.1 0 0 0-2.4 7.5v1.5h4.8V9.4A4.1 4.1 0 0 0 8 1.9Z"/><path d="M6.4 12.8h3.2"/><path d="M7 14.4h2"/>',
  important:
    '<rect x="1.9" y="1.9" width="12.2" height="12.2" rx="3.2"/><path d="M8 5.2v3.6"/><path d="M8 10.8h.01"/>',
  warning:
    '<path d="M8 2.1 14.5 13.2H1.5Z"/><path d="M8 6.3v3.3"/><path d="M8 11.5h.01"/>',
  caution:
    '<path d="M5.7 1.9h4.6l3.8 3.8v4.6l-3.8 3.8H5.7L1.9 10.3V5.7Z"/><path d="M8 4.9v3.4"/><path d="M8 10.9h.01"/>'
}

/** 匹配引用块首行标记，并顺手吃掉紧随其后的换行，使剩余文本直接成为正文 */
const MARKER_RE = /^\[!(NOTE|TIP|IMPORTANT|WARNING|CAUTION)\][ \t]*(?:\r?\n[ \t]*)?/i

/**
 * 语言码 → 标题数组。依次尝试：精确匹配 → 忽略大小写 → 取主语言子标签（zh-CN→zh、pt-BR→pt），
 * 都不中则兜底 en。这样带地区码的写法（en-US / zh-Hans / ja-JP）也能落到正确那行。
 */
function resolveTitles(lang) {
  const keys = Object.keys(ALERT_TITLES)
  const fallback = ALERT_TITLES[FALLBACK_LANG]
  if (!lang) return fallback

  const raw = String(lang).trim()
  if (ALERT_TITLES[raw]) return ALERT_TITLES[raw]

  const lower = raw.toLowerCase()
  const exact = keys.find((key) => key.toLowerCase() === lower)
  if (exact) return ALERT_TITLES[exact]

  const base = lower.split(/[-_]/)[0]
  const baseHit = keys.find((key) => key.toLowerCase() === base)
  return baseHit ? ALERT_TITLES[baseHit] : fallback
}

function buildTitle(type, label) {
  return (
    '<p class="md-alert-title">' +
    `<svg class="md-alert-icon" viewBox="0 0 16 16" width="16" height="16"` +
    ' aria-hidden="true" focusable="false" fill="none" stroke="currentColor"' +
    ' stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round">' +
    ICONS[type] +
    '</svg>' +
    label +
    '</p>\n'
  )
}

export default function githubAlerts(md, options = {}) {
  const titles = resolveTitles(options.lang)

  md.core.ruler.before('inline', 'github_alerts', (state) => {
    const tokens = state.tokens

    for (let i = 0; i < tokens.length; i++) {
      const open = tokens[i]
      if (open.type !== 'blockquote_open') continue

      const pOpen = tokens[i + 1]
      const inline = tokens[i + 2]
      const pClose = tokens[i + 3]

      // 只认「引用块第一个块级元素就是普通段落」这种形状
      if (
        !pOpen ||
        pOpen.type !== 'paragraph_open' ||
        !inline ||
        inline.type !== 'inline' ||
        !pClose ||
        pClose.type !== 'paragraph_close'
      ) {
        continue
      }

      const match = MARKER_RE.exec(inline.content)
      if (!match) continue

      const type = match[1].toLowerCase()
      const body = inline.content.slice(match[0].length)

      // 引用块 → div.md-alert（tag 改名让两个配对 token 仍保持平衡）
      const closeIndex = findBlockquoteClose(tokens, i)
      if (closeIndex === -1) continue

      open.tag = 'div'
      open.attrSet('class', `md-alert md-alert-${type}`)
      // 标题在 core 阶段就连同语言解析好挂到 token 上，renderer 规则因此不依赖闭包语言
      open.meta = { githubAlert: { type, label: titles[ALERT_TYPES.indexOf(type)] || type } }
      tokens[closeIndex].tag = 'div'

      if (body.trim() === '') {
        // 只有标签没有正文：整段删掉，否则会多出一个占一行的空 <p>
        tokens.splice(i + 1, 3)
      } else {
        inline.content = body
      }
    }

    return true
  })

  // 在开标签后紧接标题；其余 blockquote 行为完全不变
  const defaultOpen = md.renderer.rules.blockquote_open
  md.renderer.rules.blockquote_open = (tokens, idx, options, env, self) => {
    const rendered = defaultOpen
      ? defaultOpen(tokens, idx, options, env, self)
      : self.renderToken(tokens, idx, options)

    const alert = tokens[idx].meta && tokens[idx].meta.githubAlert
    return alert ? rendered + buildTitle(alert.type, alert.label) : rendered
  }
}

function findBlockquoteClose(tokens, startIndex) {
  let depth = 0
  for (let i = startIndex; i < tokens.length; i++) {
    if (tokens[i].type === 'blockquote_open') depth++
    else if (tokens[i].type === 'blockquote_close') {
      depth--
      if (depth === 0) return i
    }
  }
  return -1
}
