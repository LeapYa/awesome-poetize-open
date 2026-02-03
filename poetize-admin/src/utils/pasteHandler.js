import { htmlToMarkdown, isRichHtml } from '@/utils/htmlToMarkdown';
import { isMarkdownContent } from '@/utils/markdownTypeDetection';
import { Message } from 'element-ui-ce';

/**
 * 统一粘贴处理器
 * @param {ClipboardEvent} event - 粘贴事件对象
 * @param {Object} callbacks - 回调函数集合
 * @param {Function} callbacks.onImage - 处理图片上传 (file) => void
 * @param {Function} callbacks.onText - 处理文本插入 (text) => void
 * @returns {Promise<void>}
 */
export async function handlePaste(event, { onImage, onText }) {
  const clipboardData = event.clipboardData || window.clipboardData;
  if (!clipboardData) return;

  // 1. 优先处理文件 (图片)
  // 注意：剪贴板可能包含多个项目，我们优先寻找图片
  const items = clipboardData.items;
  let hasFile = false;
  
  if (items) {
    for (let i = 0; i < items.length; i++) {
      const item = items[i];
      if (item.kind === 'file') {
        hasFile = true;
        // 检查是否是图片
        if (item.type.indexOf('image') !== -1) {
          event.preventDefault(); // 拦截默认行为
          const file = item.getAsFile();
          if (file && onImage) {
            onImage(file);
          }
          return; // 处理完第一张图片后停止 (通常粘贴只需处理一个主要内容)
        }
      }
    }
  }

  // 如果包含文件但没有一个是图片，且没有文本内容，提示用户
  // 注意：有些文件复制也会带有文件名作为 text/plain，所以需要综合判断
  // 这里策略是：如果是文件但非图片，我们不处理文件，继续尝试处理文本
  // 如果最终也没有有效的文本，再提示

  // 2. 检测自定义 Markdown 格式 (编辑器内部复制)
  const poetizeMarkdown = clipboardData.getData('text/x-poetize-markdown');
  if (poetizeMarkdown) {
    event.preventDefault();
    if (onText) onText(poetizeMarkdown);
    return;
  }

  // 3. 检测标准 Markdown 格式
  const standardMarkdown = clipboardData.getData('text/markdown');
  if (standardMarkdown) {
    event.preventDefault();
    if (onText) onText(standardMarkdown);
    return;
  }

  // 4. HTML / 纯文本处理
  const html = clipboardData.getData('text/html');
  const plainText = clipboardData.getData('text/plain');

  if (!html && !plainText) {
    if (hasFile) {
      // 有文件但不是图片，且没有文本 -> 不支持的文件类型
      event.preventDefault();
      Message.warning('目前仅支持粘贴图片文件或文本内容');
    }
    return;
  }

  // 总是阻止默认行为，接管粘贴（因为需要异步检测，无法让浏览器默认行为正确执行）
  // 唯一的例外是如果调用方希望纯文本走默认行为，但为了统一体验，建议都由 JS 插入
  event.preventDefault();

  // 异步检测纯文本是否是 Markdown 代码
  let isMarkdown = false;
  if (plainText) {
    isMarkdown = await isMarkdownContent(plainText);
  }

  // 5. 决策逻辑
  // 优先使用 HTML 转 Markdown，除非检测到明显的 Markdown 特征 (避免双重转义)
  if (html && isRichHtml(html) && !isMarkdown) {
    try {
      const markdown = htmlToMarkdown(html);
      if (markdown && markdown.trim()) {
        if (onText) onText(markdown);
        return;
      }
    } catch (err) {
      console.error('HTML to Markdown conversion failed:', err);
      // 转换失败，回退到纯文本
    }
  }

  // 6. 回退到纯文本
  if (plainText) {
    if (onText) onText(plainText);
  }
}
