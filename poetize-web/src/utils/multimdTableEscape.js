/**
 * markdown-it-multimd-table 表格转义补齐（`\|` 在单元格内的还原）
 *
 * 背景：
 * 原生 markdown-it 的表格切分函数 `escapedSplit` 在遇到 `\|` 时会顺手丢掉反斜杠，
 * 于是单元格文本 `\|` 变成 `|` —— 等价于「表格单元格内天然吃转义」。
 * multimd-table 接管表格解析后，只记录管道符位置（scan_bound_indices），把单元格
 * 原始切片直接交给 inline 解析器，反斜杠被原样留下。
 *
 * 后果（单元格里写 `\|`，反引号包住转义管道）：
 *   - 原生 markdown-it：反斜杠在切分阶段就被吃掉 → 代码片段内容是 `|` → 显示 `|`
 *   - multimd-table：代码片段内容是 `\|`，而 CommonMark 规定代码片段内不处理反斜杠
 *     转义（inline 的 escape 规则根本不进代码片段）→ 屏幕上多出一个反斜杠
 *
 * 修复：
 * inline 解析完成后，把表格单元格（th/td）内 code_inline 的内容按原生表格切分的
 * 行为还原，即 `\|` → `|`。
 *
 * 作用域严格限制：
 *   - 只处理 th_open / td_open 之内的 inline token
 *   - 只处理 code_inline（行内代码）的 content
 *   - 围栏代码块、缩进代码块、表格外的段落一律不动（那里 `\|` 保持 CommonMark 原义）
 *
 * 注意：poetize-web 与 poetize-admin 各有一份同名副本（两个独立 app，无共享源码目录），
 * 修改时请同步两边。
 */

const ESCAPED_PIPE = /\\\|/g

export default function multimdTableEscape(md) {
  md.core.ruler.push('multimd_table_escape', (state) => {
    const tokens = state.tokens
    let cellDepth = 0

    for (let i = 0; i < tokens.length; i++) {
      const token = tokens[i]

      if (token.type === 'th_open' || token.type === 'td_open') {
        cellDepth++
        continue
      }
      if (token.type === 'th_close' || token.type === 'td_close') {
        if (cellDepth > 0) cellDepth--
        continue
      }
      if (cellDepth === 0 || token.type !== 'inline' || !token.children) continue

      for (const child of token.children) {
        if (child.type === 'code_inline' && child.content.indexOf('\\|') !== -1) {
          child.content = child.content.replace(ESCAPED_PIPE, '|')
        }
      }
    }

    return true
  })
}
