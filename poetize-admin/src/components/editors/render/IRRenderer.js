/**
 * @file IRRenderer - 即时渲染器
 * @description 行级 Markdown 渲染，支持块级元素（代码块、表格等）的检测和渲染
 */

/**
 * 块类型枚举
 */
export const BlockType = {
  PARAGRAPH: 'paragraph',
  HEADING: 'heading',
  CODE_BLOCK: 'code_block',
  QUOTE: 'quote',
  LIST: 'list',
  TABLE: 'table',
  HR: 'hr',
  MATH_BLOCK: 'math_block',
  MERMAID: 'mermaid',
  ECHARTS: 'echarts'
};

/**
 * 块信息
 */
class BlockInfo {
  constructor(type, startLine, endLine = null, metadata = {}) {
    this.type = type;
    this.startLine = startLine;
    this.endLine = endLine !== null ? endLine : startLine;
    this.metadata = metadata;
  }

  /**
   * 检查行是否在块内
   * @param {number} lineIndex 
   * @returns {boolean}
   */
  containsLine(lineIndex) {
    return lineIndex >= this.startLine && lineIndex <= this.endLine;
  }

  /**
   * 是否为多行块
   * @returns {boolean}
   */
  isMultiLine() {
    return this.endLine > this.startLine;
  }
}

/**
 * 即时渲染器
 */
export class IRRenderer {
  constructor() {
    // 渲染缓存
    this.cache = new Map();
    
    // 块信息缓存
    this.blocks = [];
    
    // markdown-it 实例（懒加载）
    this.md = null;
    this.mdLoading = null;
    
    // hljs 实例
    this.hljs = null;
  }

  /**
   * 初始化 markdown-it（懒加载）
   */
  async initMarkdownIt() {
    if (this.md) return this.md;
    if (this.mdLoading) return this.mdLoading;
    
    this.mdLoading = (async () => {
      try {
        const [
          { default: MarkdownIt },
          { default: markdownItMultimdTable },
          { default: markdownItKatex },
          { default: hljs }
        ] = await Promise.all([
          import('markdown-it'),
          import('markdown-it-multimd-table'),
          import('@iktakahiro/markdown-it-katex'),
          import('highlight.js')
        ]);

        this.hljs = hljs;
        
        this.md = new MarkdownIt({
          breaks: true,
          html: true,
          linkify: true,
          highlight: (str, lang) => {
            if (lang && hljs.getLanguage(lang)) {
              try {
                return hljs.highlight(str, { language: lang, ignoreIllegals: true }).value;
              } catch (e) {
                console.error('Highlight error:', e);
              }
            }
            return this.escapeHtml(str);
          }
        })
          .use(markdownItMultimdTable)
          .use(markdownItKatex);

        return this.md;
      } catch (e) {
        console.error('Failed to load markdown-it:', e);
        return null;
      }
    })();
    
    return this.mdLoading;
  }

  /**
   * 分析文档块结构
   * @param {import('../core/DocumentModel').DocumentModel} document 
   */
  analyzeBlocks(document) {
    this.blocks = [];
    const lineCount = document.getLineCount();
    
    let i = 0;
    while (i < lineCount) {
      const text = document.getLineText(i);
      
      // 检测代码块
      if (text.match(/^```(\w*)/)) {
        const startLine = i;
        const lang = text.match(/^```(\w*)/)[1] || '';
        
        // 查找结束标记
        let endLine = startLine;
        for (let j = startLine + 1; j < lineCount; j++) {
          if (document.getLineText(j).startsWith('```')) {
            endLine = j;
            break;
          }
          endLine = j;
        }
        
        // 确定代码块类型
        let blockType = BlockType.CODE_BLOCK;
        if (lang === 'mermaid') {
          blockType = BlockType.MERMAID;
        } else if (lang === 'echarts') {
          blockType = BlockType.ECHARTS;
        }
        
        this.blocks.push(new BlockInfo(blockType, startLine, endLine, { language: lang }));
        i = endLine + 1;
        continue;
      }
      
      // 检测数学块
      if (text.trim() === '$$') {
        const startLine = i;
        let endLine = startLine;
        
        for (let j = startLine + 1; j < lineCount; j++) {
          if (document.getLineText(j).trim() === '$$') {
            endLine = j;
            break;
          }
          endLine = j;
        }
        
        this.blocks.push(new BlockInfo(BlockType.MATH_BLOCK, startLine, endLine));
        i = endLine + 1;
        continue;
      }
      
      // 检测表格
      if (this.isTableLine(text)) {
        const startLine = i;
        let endLine = i;
        
        // 找表格结束
        for (let j = startLine + 1; j < lineCount; j++) {
          if (!this.isTableLine(document.getLineText(j))) {
            break;
          }
          endLine = j;
        }
        
        // 至少需要两行才是有效表格（表头 + 分隔线）
        if (endLine > startLine) {
          this.blocks.push(new BlockInfo(BlockType.TABLE, startLine, endLine));
          i = endLine + 1;
          continue;
        }
      }
      
      // 检测引用块
      if (text.startsWith('>')) {
        const startLine = i;
        let endLine = i;
        
        for (let j = startLine + 1; j < lineCount; j++) {
          const nextText = document.getLineText(j);
          if (!nextText.startsWith('>') && nextText.trim() !== '') {
            break;
          }
          if (nextText.startsWith('>')) {
            endLine = j;
          }
        }
        
        this.blocks.push(new BlockInfo(BlockType.QUOTE, startLine, endLine));
        i = endLine + 1;
        continue;
      }
      
      // 检测标题
      if (text.match(/^#{1,6}\s/)) {
        const level = text.match(/^(#{1,6})\s/)[1].length;
        this.blocks.push(new BlockInfo(BlockType.HEADING, i, i, { level }));
        i++;
        continue;
      }
      
      // 检测分隔线
      if (text.match(/^[-*_]{3,}$/)) {
        this.blocks.push(new BlockInfo(BlockType.HR, i));
        i++;
        continue;
      }
      
      // 检测列表
      if (text.match(/^(\s*)([-*+]|\d+\.)\s/)) {
        const startLine = i;
        let endLine = i;
        const indent = text.match(/^(\s*)/)[1].length;
        
        for (let j = startLine + 1; j < lineCount; j++) {
          const nextText = document.getLineText(j);
          // 列表项或缩进的延续行
          if (nextText.match(/^(\s*)([-*+]|\d+\.)\s/) || 
              (nextText.trim() !== '' && nextText.match(/^\s{2,}/))) {
            endLine = j;
          } else if (nextText.trim() === '') {
            // 空行可能是列表项之间的分隔
            const afterEmpty = j + 1 < lineCount ? document.getLineText(j + 1) : '';
            if (!afterEmpty.match(/^(\s*)([-*+]|\d+\.)\s/)) {
              break;
            }
          } else {
            break;
          }
        }
        
        this.blocks.push(new BlockInfo(BlockType.LIST, startLine, endLine, { indent }));
        i = endLine + 1;
        continue;
      }
      
      // 普通段落
      this.blocks.push(new BlockInfo(BlockType.PARAGRAPH, i));
      i++;
    }
  }

  /**
   * 检查是否为表格行
   * @param {string} text 
   * @returns {boolean}
   */
  isTableLine(text) {
    return /^\s*\|.*\|\s*$/.test(text);
  }

  /**
   * 获取行所在的块
   * @param {number} lineIndex 
   * @returns {BlockInfo|null}
   */
  getBlockForLine(lineIndex) {
    for (const block of this.blocks) {
      if (block.containsLine(lineIndex)) {
        return block;
      }
    }
    return null;
  }

  /**
   * 渲染单行
   * @param {string} text - 行文本
   * @param {number} lineIndex - 行索引
   * @param {Object} options - 渲染选项
   * @returns {string} HTML
   */
  renderLine(text, lineIndex, options = {}) {
    const { showMarkers = true, block = null } = options;
    
    if (!text) return '';
    
    // 检查缓存
    const cacheKey = `${lineIndex}:${text}:${showMarkers}`;
    if (this.cache.has(cacheKey)) {
      return this.cache.get(cacheKey);
    }
    
    let html = '';
    
    // 根据块类型渲染
    if (block) {
      html = this.renderBlockLine(text, lineIndex, block, showMarkers);
    } else {
      html = this.renderInlineLine(text, showMarkers);
    }
    
    // 缓存结果
    this.cache.set(cacheKey, html);
    
    return html;
  }

  /**
   * 渲染块内的行
   * @param {string} text 
   * @param {number} lineIndex 
   * @param {BlockInfo} block 
   * @param {boolean} showMarkers 
   * @returns {string}
   */
  renderBlockLine(text, lineIndex, block, showMarkers) {
    switch (block.type) {
      case BlockType.CODE_BLOCK:
        return this.renderCodeBlockLine(text, lineIndex, block, showMarkers);
      
      case BlockType.HEADING:
        return this.renderHeadingLine(text, block.metadata.level, showMarkers);
      
      case BlockType.QUOTE:
        return this.renderQuoteLine(text, showMarkers);
      
      case BlockType.TABLE:
        return this.renderTableLine(text, lineIndex, block);
      
      case BlockType.LIST:
        return this.renderListLine(text, showMarkers);
      
      case BlockType.HR:
        return '<hr class="md-hr">';
      
      case BlockType.MATH_BLOCK:
        return this.renderMathBlockLine(text, lineIndex, block);
      
      case BlockType.MERMAID:
      case BlockType.ECHARTS:
        return this.renderCodeBlockLine(text, lineIndex, block, showMarkers);
      
      default:
        return this.renderInlineLine(text, showMarkers);
    }
  }

  /**
   * 渲染代码块行
   * @param {string} text 
   * @param {number} lineIndex 
   * @param {BlockInfo} block 
   * @param {boolean} showMarkers 
   * @returns {string}
   */
  renderCodeBlockLine(text, lineIndex, block, showMarkers) {
    const isStartLine = lineIndex === block.startLine;
    const isEndLine = lineIndex === block.endLine;
    const lang = block.metadata.language || '';
    
    if (isStartLine) {
      // 开始行：显示语言标记
      if (showMarkers) {
        return `<span class="md-code-fence-start"><span class="md-marker">\`\`\`</span><span class="md-code-lang">${this.escapeHtml(lang)}</span></span>`;
      } else {
        return `<span class="md-code-fence-start md-hidden">\`\`\`${this.escapeHtml(lang)}</span>`;
      }
    } else if (isEndLine && text.startsWith('```')) {
      // 结束行
      if (showMarkers) {
        return `<span class="md-code-fence-end"><span class="md-marker">\`\`\`</span></span>`;
      } else {
        return `<span class="md-code-fence-end md-hidden">\`\`\`</span>`;
      }
    } else {
      // 代码内容行
      let highlighted = this.escapeHtml(text);
      
      if (lang && this.hljs && this.hljs.getLanguage(lang)) {
        try {
          highlighted = this.hljs.highlight(text, { language: lang, ignoreIllegals: true }).value;
        } catch (e) {
          // 使用转义后的文本
        }
      }
      
      return `<span class="md-code-line">${highlighted || '&nbsp;'}</span>`;
    }
  }

  /**
   * 渲染标题行
   * @param {string} text 
   * @param {number} level 
   * @param {boolean} showMarkers 
   * @returns {string}
   */
  renderHeadingLine(text, level, showMarkers) {
    const match = text.match(/^(#{1,6})\s(.*)$/);
    if (!match) return this.escapeHtml(text);
    
    const [, markers, content] = match;
    const renderedContent = this.renderInlineElements(content);
    
    if (showMarkers) {
      return `<span class="md-heading md-h${level}"><span class="md-marker">${markers} </span>${renderedContent}</span>`;
    } else {
      return `<span class="md-heading md-h${level}">${renderedContent}</span>`;
    }
  }

  /**
   * 渲染引用行
   * @param {string} text 
   * @param {boolean} showMarkers 
   * @returns {string}
   */
  renderQuoteLine(text, showMarkers) {
    const match = text.match(/^(>+)\s?(.*)$/);
    if (!match) return this.escapeHtml(text);
    
    const [, markers, content] = match;
    const level = markers.length;
    const renderedContent = this.renderInlineElements(content);
    
    if (showMarkers) {
      return `<span class="md-quote md-quote-${level}"><span class="md-marker">${markers} </span>${renderedContent}</span>`;
    } else {
      return `<span class="md-quote md-quote-${level}">${renderedContent}</span>`;
    }
  }

  /**
   * 渲染表格行
   * @param {string} text 
   * @param {number} lineIndex 
   * @param {BlockInfo} block 
   * @returns {string}
   */
  renderTableLine(text, lineIndex, block) {
    // 检查是否为分隔行
    if (/^\s*\|(\s*:?-+:?\s*\|)+\s*$/.test(text)) {
      return `<span class="md-table-separator">${this.escapeHtml(text)}</span>`;
    }
    
    // 渲染表格单元格
    const cells = text.split('|').filter((_, i, arr) => i > 0 && i < arr.length - 1);
    const isHeader = lineIndex === block.startLine;
    
    const renderedCells = cells.map(cell => {
      const trimmed = cell.trim();
      const rendered = this.renderInlineElements(trimmed);
      const tag = isHeader ? 'th' : 'td';
      return `<${tag} class="md-table-cell">${rendered}</${tag}>`;
    }).join('');
    
    return `<span class="md-table-row"><span class="md-marker">|</span>${renderedCells}<span class="md-marker">|</span></span>`;
  }

  /**
   * 渲染列表行
   * @param {string} text 
   * @param {boolean} showMarkers 
   * @returns {string}
   */
  renderListLine(text, showMarkers) {
    // 无序列表
    const ulMatch = text.match(/^(\s*)([-*+])\s(.*)$/);
    if (ulMatch) {
      const [, indent, marker, content] = ulMatch;
      const renderedContent = this.renderInlineElements(content);
      
      if (showMarkers) {
        return `<span class="md-list-item md-ul">${indent}<span class="md-marker">${marker} </span>${renderedContent}</span>`;
      } else {
        return `<span class="md-list-item md-ul">${indent}• ${renderedContent}</span>`;
      }
    }
    
    // 有序列表
    const olMatch = text.match(/^(\s*)(\d+)\.\s(.*)$/);
    if (olMatch) {
      const [, indent, num, content] = olMatch;
      const renderedContent = this.renderInlineElements(content);
      
      if (showMarkers) {
        return `<span class="md-list-item md-ol">${indent}<span class="md-marker">${num}. </span>${renderedContent}</span>`;
      } else {
        return `<span class="md-list-item md-ol">${indent}${num}. ${renderedContent}</span>`;
      }
    }
    
    // 待办列表
    const todoMatch = text.match(/^(\s*)([-*+])\s\[([ x])\]\s(.*)$/);
    if (todoMatch) {
      const [, indent, marker, check, content] = todoMatch;
      const isChecked = check === 'x';
      const renderedContent = this.renderInlineElements(content);
      const checkboxClass = isChecked ? 'md-todo-checked' : 'md-todo-unchecked';
      
      if (showMarkers) {
        return `<span class="md-list-item md-todo ${checkboxClass}">${indent}<span class="md-marker">${marker} [${check}] </span>${renderedContent}</span>`;
      } else {
        // const checkbox = isChecked ? '☑' : '☐';
        // 使用 input 元素以保持样式一致
        const checkbox = `<input type="checkbox" class="ir-todo-checkbox" ${isChecked ? 'checked' : ''} disabled>`;
        return `<span class="md-list-item md-todo ${checkboxClass}">${indent}${checkbox} ${renderedContent}</span>`;
      }
    }
    
    return this.renderInlineLine(text, showMarkers);
  }

  /**
   * 渲染数学块行
   * @param {string} text 
   * @param {number} lineIndex 
   * @param {BlockInfo} block 
   * @returns {string}
   */
  renderMathBlockLine(text, lineIndex, block) {
    if (text.trim() === '$$') {
      return `<span class="md-math-fence">${this.escapeHtml(text)}</span>`;
    }
    return `<span class="md-math-content">${this.escapeHtml(text)}</span>`;
  }

  /**
   * 渲染行内元素的行
   * @param {string} text 
   * @param {boolean} showMarkers 
   * @returns {string}
   */
  renderInlineLine(text, showMarkers) {
    if (!text) return '';
    return this.renderInlineElements(text);
  }

  /**
   * 渲染行内元素
   * @param {string} text 
   * @returns {string}
   */
  renderInlineElements(text) {
    if (!text) return '';
    
    let html = this.escapeHtml(text);
    
    // 粗体 **text**
    html = html.replace(/\*\*(.+?)\*\*/g, '<strong class="md-bold">$1</strong>');
    
    // 斜体 *text* （避免与粗体冲突）
    html = html.replace(/(?<!\*)\*(?!\*)(.+?)(?<!\*)\*(?!\*)/g, '<em class="md-italic">$1</em>');
    
    // 删除线 ~~text~~
    html = html.replace(/~~(.+?)~~/g, '<del class="md-strike">$1</del>');
    
    // 行内代码 `code`
    html = html.replace(/`([^`]+)`/g, '<code class="md-inline-code">$1</code>');
    
    // 链接 [text](url)
    html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a class="md-link" href="$2" target="_blank">$1</a>');
    
    // 图片 ![alt](url)
    html = html.replace(/!\[([^\]]*)\]\(([^)]+)\)/g, '<img class="md-image" src="$2" alt="$1">');
    
    // 行内数学 $...$
    html = html.replace(/\$([^$]+)\$/g, '<span class="md-inline-math">$1</span>');
    
    // 高亮 ==text==
    html = html.replace(/==(.+?)==/g, '<mark class="md-highlight">$1</mark>');
    
    return html;
  }

  /**
   * 渲染完整块（用于预览）
   * @param {import('../core/DocumentModel').DocumentModel} document 
   * @param {BlockInfo} block 
   * @returns {string}
   */
  async renderBlock(document, block) {
    const lines = [];
    for (let i = block.startLine; i <= block.endLine; i++) {
      lines.push(document.getLineText(i));
    }
    const content = lines.join('\n');
    
    // 使用 markdown-it 渲染完整块
    if (!this.md) {
      await this.initMarkdownIt();
    }
    
    if (this.md) {
      return this.md.render(content);
    }
    
    return this.escapeHtml(content);
  }

  /**
   * 清除缓存
   */
  clearCache() {
    this.cache.clear();
  }

  /**
   * 清除指定行的缓存
   * @param {number} lineIndex 
   */
  clearLineCache(lineIndex) {
    for (const key of this.cache.keys()) {
      if (key.startsWith(`${lineIndex}:`)) {
        this.cache.delete(key);
      }
    }
  }

  /**
   * HTML 转义
   * @param {string} text 
   * @returns {string}
   */
  escapeHtml(text) {
    if (!text) return '';
    const map = {
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#039;'
    };
    return text.replace(/[&<>"']/g, m => map[m]);
  }
}

export default IRRenderer;
