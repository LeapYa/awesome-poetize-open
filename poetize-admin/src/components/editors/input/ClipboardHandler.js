/**
 * @file ClipboardHandler - 剪贴板处理器
 * @description 处理复制、粘贴、剪切操作
 */

/**
 * 剪贴板处理器
 */
export class ClipboardHandler {
  /**
   * @param {Object} editor - 编辑器实例
   */
  constructor(editor) {
    this.editor = editor;
    
    // 图片粘贴回调
    this.onImagePaste = null;
  }

  /**
   * 设置图片粘贴回调
   * @param {Function} callback 
   */
  setImagePasteCallback(callback) {
    this.onImagePaste = callback;
  }

  /**
   * 处理粘贴事件
   * @param {ClipboardEvent} e 
   */
  handlePaste(e) {
    e.preventDefault();
    
    const clipboardData = e.clipboardData || window.clipboardData;
    if (!clipboardData) return;

    // 检查是否有图片
    const items = clipboardData.items;
    if (items) {
      for (let i = 0; i < items.length; i++) {
        const item = items[i];
        
        if (item.kind === 'file' && item.type.indexOf('image') !== -1) {
          const file = item.getAsFile();
          if (file && this.onImagePaste) {
            this.onImagePaste(file);
            return;
          }
        }
      }
    }

    // 获取文本
    const text = clipboardData.getData('text/plain');
    if (text) {
      this.pasteText(text);
    }
  }

  /**
   * 粘贴文本
   * @param {string} text 
   */
  pasteText(text) {
    const cursor = this.editor.cursor;
    const document = this.editor.document;
    
    // 如果有选区，先删除选中内容
    if (cursor.hasSelection()) {
      const sel = cursor.getSelection();
      const min = sel.getMin();
      const max = sel.getMax();
      document.deleteRange(min.line, min.column, max.line, max.column);
      cursor.setPosition(min.line, min.column);
    }
    
    // 插入文本
    const pos = cursor.getPosition();
    const newPos = document.insertText(pos.line, pos.column, text);
    cursor.setPosition(newPos.line, newPos.column);
  }

  /**
   * 处理复制事件
   * @param {ClipboardEvent} e 
   */
  handleCopy(e) {
    const cursor = this.editor.cursor;
    
    if (!cursor.hasSelection()) {
      // 没有选区时，复制整行
      const line = cursor.getLine();
      const lineText = this.editor.document.getLineText(line);
      
      e.preventDefault();
      e.clipboardData.setData('text/plain', lineText + '\n');
      return;
    }
    
    e.preventDefault();
    
    const sel = cursor.getSelection();
    const min = sel.getMin();
    const max = sel.getMax();
    const text = this.editor.document.getTextRange(min.line, min.column, max.line, max.column);
    
    e.clipboardData.setData('text/plain', text);
  }

  /**
   * 处理剪切事件
   * @param {ClipboardEvent} e 
   */
  handleCut(e) {
    const cursor = this.editor.cursor;
    const document = this.editor.document;
    
    if (!cursor.hasSelection()) {
      // 没有选区时，剪切整行
      const line = cursor.getLine();
      const lineText = document.getLineText(line);
      
      e.preventDefault();
      e.clipboardData.setData('text/plain', lineText + '\n');
      
      // 删除整行
      document.deleteLine(line);
      
      // 调整光标位置
      const lineCount = document.getLineCount();
      const newLine = Math.min(line, lineCount - 1);
      cursor.setPosition(newLine, 0);
      return;
    }
    
    // 先复制
    this.handleCopy(e);
    
    // 删除选中内容
    const sel = cursor.getSelection();
    const min = sel.getMin();
    const max = sel.getMax();
    document.deleteRange(min.line, min.column, max.line, max.column);
    cursor.setPosition(min.line, min.column);
  }

  /**
   * 使用 Clipboard API 复制文本到剪贴板
   * @param {string} text 
   * @returns {Promise<boolean>}
   */
  async copyToClipboard(text) {
    try {
      if (navigator.clipboard && navigator.clipboard.writeText) {
        await navigator.clipboard.writeText(text);
        return true;
      }
      
      // 降级方案
      return this.copyToClipboardFallback(text);
    } catch (e) {
      console.error('Failed to copy to clipboard:', e);
      return false;
    }
  }

  /**
   * 复制到剪贴板（降级方案）
   * @param {string} text 
   * @returns {boolean}
   */
  copyToClipboardFallback(text) {
    const textarea = document.createElement('textarea');
    textarea.value = text;
    textarea.style.cssText = 'position:fixed;left:-9999px;top:0';
    document.body.appendChild(textarea);
    
    try {
      textarea.select();
      const result = document.execCommand('copy');
      return result;
    } catch (e) {
      console.error('Fallback copy failed:', e);
      return false;
    } finally {
      document.body.removeChild(textarea);
    }
  }

  /**
   * 从剪贴板读取文本
   * @returns {Promise<string>}
   */
  async readFromClipboard() {
    try {
      if (navigator.clipboard && navigator.clipboard.readText) {
        return await navigator.clipboard.readText();
      }
      return '';
    } catch (e) {
      console.error('Failed to read from clipboard:', e);
      return '';
    }
  }

  /**
   * 复制选中内容到剪贴板（程序调用）
   * @returns {Promise<boolean>}
   */
  async copy() {
    const cursor = this.editor.cursor;
    const document = this.editor.document;
    
    let text;
    
    if (!cursor.hasSelection()) {
      // 没有选区时，复制整行
      const line = cursor.getLine();
      text = document.getLineText(line) + '\n';
    } else {
      const sel = cursor.getSelection();
      const min = sel.getMin();
      const max = sel.getMax();
      text = document.getTextRange(min.line, min.column, max.line, max.column);
    }
    
    return this.copyToClipboard(text);
  }

  /**
   * 剪切选中内容到剪贴板（程序调用）
   * @returns {Promise<boolean>}
   */
  async cut() {
    const cursor = this.editor.cursor;
    const document = this.editor.document;
    
    let text;
    
    if (!cursor.hasSelection()) {
      // 没有选区时，剪切整行
      const line = cursor.getLine();
      text = document.getLineText(line) + '\n';
      
      const success = await this.copyToClipboard(text);
      if (success) {
        document.deleteLine(line);
        const lineCount = document.getLineCount();
        const newLine = Math.min(line, lineCount - 1);
        cursor.setPosition(newLine, 0);
      }
      return success;
    } else {
      const sel = cursor.getSelection();
      const min = sel.getMin();
      const max = sel.getMax();
      text = document.getTextRange(min.line, min.column, max.line, max.column);
      
      const success = await this.copyToClipboard(text);
      if (success) {
        document.deleteRange(min.line, min.column, max.line, max.column);
        cursor.setPosition(min.line, min.column);
      }
      return success;
    }
  }

  /**
   * 从剪贴板粘贴（程序调用）
   * @returns {Promise<boolean>}
   */
  async paste() {
    const text = await this.readFromClipboard();
    if (text) {
      this.pasteText(text);
      return true;
    }
    return false;
  }

  /**
   * 复制为带格式的 HTML（用于公众号等）
   * @param {string} html 
   * @param {string} plainText 
   * @returns {Promise<boolean>}
   */
  async copyAsHTML(html, plainText) {
    try {
      if (navigator.clipboard && navigator.clipboard.write && typeof ClipboardItem !== 'undefined') {
        const items = {};
        items['text/html'] = new Blob([html], { type: 'text/html' });
        items['text/plain'] = new Blob([plainText], { type: 'text/plain' });
        
        await navigator.clipboard.write([new ClipboardItem(items)]);
        return true;
      }
      
      // 降级方案
      return this.copyAsHTMLFallback(html);
    } catch (e) {
      console.error('Failed to copy as HTML:', e);
      return false;
    }
  }

  /**
   * 复制为 HTML（降级方案）
   * @param {string} html 
   * @returns {boolean}
   */
  copyAsHTMLFallback(html) {
    const div = document.createElement('div');
    div.innerHTML = html;
    div.style.cssText = 'position:fixed;left:-9999px;top:0';
    div.setAttribute('contenteditable', 'true');
    document.body.appendChild(div);
    
    try {
      const selection = window.getSelection();
      const range = document.createRange();
      range.selectNodeContents(div);
      selection.removeAllRanges();
      selection.addRange(range);
      
      const result = document.execCommand('copy');
      selection.removeAllRanges();
      return result;
    } catch (e) {
      console.error('Fallback HTML copy failed:', e);
      return false;
    } finally {
      document.body.removeChild(div);
    }
  }

  /**
   * 销毁
   */
  destroy() {
    this.onImagePaste = null;
    this.editor = null;
  }
}

export default ClipboardHandler;
