/**
 * @file Markdown 标题级别转换工具
 * @description 
 * 在编辑器中，用户写 # 标题实际显示为 h2 样式（编辑器预览样式降级）
 * 保存到数据库时，需要将标题降级存储（# → ##），这样文章页渲染时 ## 对应 h2
 * 从数据库读取时，需要将标题升级显示（## → #），这样编辑器中用户看到的是 #
 * 
 * 这个逻辑确保了：
 * 1. 编辑器中用户写 # 看到的是 h2 样式
 * 2. 文章页渲染时显示的也是 h2 样式
 * 3. 用户体验一致，所见即所得
 */

/**
 * Markdown 标题降级（保存到数据库时用）
 * # → ##, ## → ###, ### → ####, #### → #####, ##### → ######
 * 
 * @param {string} markdown - 原始 Markdown 内容
 * @returns {string} 降级后的 Markdown 内容
 */
export function downgradeMarkdownHeadings(markdown) {
  if (!markdown) return '';
  
  // 按行处理，避免代码块中的 # 被误处理
  const lines = markdown.split('\n');
  let inCodeBlock = false;
  
  const processedLines = lines.map(line => {
    // 检测代码块（包括 ``` 和 ~~~）
    const trimmedLine = line.trim();
    if (trimmedLine.startsWith('```') || trimmedLine.startsWith('~~~')) {
      inCodeBlock = !inCodeBlock;
      return line;
    }
    
    // 代码块内不处理
    if (inCodeBlock) {
      return line;
    }
    
    // 处理标题行：在开头的 # 前面添加一个 #
    // 匹配行首的标题标记（1-5个#，支持前导空格）
    // 注意：6级标题不再降级，因为没有 h7
    if (/^\s*#{1,5}\s/.test(line)) {
      return line.replace(/^(\s*)(#{1,5})(\s)/, '$1#$2$3');
    }
    
    return line;
  });
  
  return processedLines.join('\n');
}

/**
 * Markdown 标题升级（从数据库读取时用于显示）
 * ## → #, ### → ##, #### → ###, ##### → ####, ###### → #####
 * 
 * @param {string} markdown - 数据库中的 Markdown 内容
 * @returns {string} 升级后的 Markdown 内容
 */
export function upgradeMarkdownHeadings(markdown) {
  if (!markdown) return '';
  
  // 按行处理，避免代码块中的 # 被误处理
  const lines = markdown.split('\n');
  let inCodeBlock = false;
  
  const processedLines = lines.map(line => {
    // 检测代码块（包括 ``` 和 ~~~）
    const trimmedLine = line.trim();
    if (trimmedLine.startsWith('```') || trimmedLine.startsWith('~~~')) {
      inCodeBlock = !inCodeBlock;
      return line;
    }
    
    // 代码块内不处理
    if (inCodeBlock) {
      return line;
    }
    
    // 处理标题行：移除开头的一个 #
    // 匹配行首的标题标记（2-6个#，支持前导空格）
    // 注意：1级标题（单个#）不升级，因为没有 h0
    if (/^\s*#{2,6}\s/.test(line)) {
      return line.replace(/^(\s*)#{1}(#{1,5}\s)/, '$1$2');
    }
    
    return line;
  });
  
  return processedLines.join('\n');
}

/**
 * 创建一个带标题转换的编辑器混入对象（Mixin）
 * 可以在 Vue 组件中使用，提供统一的标题转换能力
 * 
 * @example
 * // 在组件中使用
 * import { headingTransformMixin } from '@/utils/markdownHeadingUtils';
 * export default {
 *   mixins: [headingTransformMixin],
 *   // ...
 * }
 */
export const headingTransformMixin = {
  methods: {
    /**
     * 标题降级（保存时调用）
     */
    downgradeHeadings(markdown) {
      return downgradeMarkdownHeadings(markdown);
    },
    
    /**
     * 标题升级（加载时调用）
     */
    upgradeHeadings(markdown) {
      return upgradeMarkdownHeadings(markdown);
    }
  }
};

export default {
  downgradeMarkdownHeadings,
  upgradeMarkdownHeadings,
  headingTransformMixin
};
