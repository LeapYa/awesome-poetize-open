/**
 * @file KeyboardHandler - 键盘输入处理器
 * @description 处理普通按键、快捷键、方向键等键盘事件
 */

/**
 * 键盘处理器
 */
export class KeyboardHandler {
  /**
   * @param {Object} editor - 编辑器实例
   */
  constructor(editor) {
    this.editor = editor;
    this.shortcuts = new Map();
    
    // 注册默认快捷键
    this.registerDefaultShortcuts();
  }

  /**
   * 注册默认快捷键
   */
  registerDefaultShortcuts() {
    // 格式化
    this.registerShortcut('ctrl+b', () => this.editor.insertFormat('bold'));
    this.registerShortcut('ctrl+i', () => this.editor.insertFormat('italic'));
    this.registerShortcut('ctrl+k', () => this.editor.insertFormat('link'));
    this.registerShortcut('ctrl+`', () => this.editor.insertFormat('code'));
    this.registerShortcut('ctrl+d', () => this.editor.insertFormat('strike'));
    
    // 标题
    this.registerShortcut('ctrl+1', () => this.editor.insertHeading(1));
    this.registerShortcut('ctrl+2', () => this.editor.insertHeading(2));
    this.registerShortcut('ctrl+3', () => this.editor.insertHeading(3));
    this.registerShortcut('ctrl+4', () => this.editor.insertHeading(4));
    this.registerShortcut('ctrl+5', () => this.editor.insertHeading(5));
    this.registerShortcut('ctrl+6', () => this.editor.insertHeading(6));
    
    // 列表
    this.registerShortcut('ctrl+u', () => this.editor.insertFormat('ul'));
    this.registerShortcut('ctrl+o', () => this.editor.insertFormat('ol'));
    
    // 引用
    this.registerShortcut('ctrl+q', () => this.editor.insertFormat('quote'));
    
    // 编辑
    this.registerShortcut('ctrl+a', () => this.editor.cursor.selectAll());
    this.registerShortcut('ctrl+z', () => this.editor.undo());
    this.registerShortcut('ctrl+shift+z', () => this.editor.redo());
    this.registerShortcut('ctrl+y', () => this.editor.redo());
    
    // 保存
    this.registerShortcut('ctrl+s', () => this.editor.$emit('save'));
  }

  /**
   * 注册快捷键
   * @param {string} shortcut - 快捷键字符串，如 'ctrl+b', 'ctrl+shift+z'
   * @param {Function} handler - 处理函数
   */
  registerShortcut(shortcut, handler) {
    this.shortcuts.set(shortcut.toLowerCase(), handler);
  }

  /**
   * 移除快捷键
   * @param {string} shortcut 
   */
  unregisterShortcut(shortcut) {
    this.shortcuts.delete(shortcut.toLowerCase());
  }

  /**
   * 处理键盘按下事件
   * @param {KeyboardEvent} e 
   * @returns {boolean} 是否已处理
   */
  handleKeydown(e) {
    const { key, ctrlKey, metaKey, shiftKey, altKey } = e;
    const modKey = ctrlKey || metaKey;
    
    // 构建快捷键字符串
    const shortcutParts = [];
    if (ctrlKey || metaKey) shortcutParts.push('ctrl');
    if (shiftKey) shortcutParts.push('shift');
    if (altKey) shortcutParts.push('alt');
    shortcutParts.push(key.toLowerCase());
    const shortcutKey = shortcutParts.join('+');
    
    // 检查快捷键
    if (this.shortcuts.has(shortcutKey)) {
      e.preventDefault();
      this.shortcuts.get(shortcutKey)();
      return true;
    }
    
    // 方向键处理
    if (this.handleArrowKeys(e, modKey, shiftKey)) {
      return true;
    }
    
    // Home / End
    if (this.handleHomeEnd(e, modKey, shiftKey)) {
      return true;
    }
    
    // 编辑键处理
    if (this.handleEditKeys(e)) {
      return true;
    }
    
    // 功能键处理
    if (this.handleFunctionKeys(e)) {
      return true;
    }
    
    return false;
  }

  /**
   * 处理方向键
   * @param {KeyboardEvent} e 
   * @param {boolean} modKey 
   * @param {boolean} shiftKey 
   * @returns {boolean}
   */
  handleArrowKeys(e, modKey, shiftKey) {
    const { key } = e;
    const cursor = this.editor.cursor;
    
    switch (key) {
      case 'ArrowLeft':
        e.preventDefault();
        if (modKey) {
          cursor.moveWordLeft(shiftKey);
        } else {
          cursor.moveLeft(shiftKey);
        }
        return true;
        
      case 'ArrowRight':
        e.preventDefault();
        if (modKey) {
          cursor.moveWordRight(shiftKey);
        } else {
          cursor.moveRight(shiftKey);
        }
        return true;
        
      case 'ArrowUp':
        e.preventDefault();
        cursor.moveUp(shiftKey);
        return true;
        
      case 'ArrowDown':
        e.preventDefault();
        cursor.moveDown(shiftKey);
        return true;
    }
    
    return false;
  }

  /**
   * 处理 Home/End 键
   * @param {KeyboardEvent} e 
   * @param {boolean} modKey 
   * @param {boolean} shiftKey 
   * @returns {boolean}
   */
  handleHomeEnd(e, modKey, shiftKey) {
    const { key } = e;
    const cursor = this.editor.cursor;
    
    switch (key) {
      case 'Home':
        e.preventDefault();
        if (modKey) {
          cursor.moveToDocumentStart(shiftKey);
        } else {
          cursor.moveToLineStart(shiftKey);
        }
        return true;
        
      case 'End':
        e.preventDefault();
        if (modKey) {
          cursor.moveToDocumentEnd(shiftKey);
        } else {
          cursor.moveToLineEnd(shiftKey);
        }
        return true;
    }
    
    return false;
  }

  /**
   * 处理编辑键（Enter, Backspace, Delete, Tab）
   * @param {KeyboardEvent} e 
   * @returns {boolean}
   */
  handleEditKeys(e) {
    const { key, shiftKey } = e;
    
    switch (key) {
      case 'Enter':
        e.preventDefault();
        this.handleEnter(shiftKey);
        return true;
        
      case 'Backspace':
        e.preventDefault();
        this.handleBackspace();
        return true;
        
      case 'Delete':
        e.preventDefault();
        this.handleDelete();
        return true;
        
      case 'Tab':
        e.preventDefault();
        this.handleTab(shiftKey);
        return true;
    }
    
    return false;
  }

  /**
   * 处理功能键
   * @param {KeyboardEvent} e 
   * @returns {boolean}
   */
  handleFunctionKeys(e) {
    const { key } = e;
    
    switch (key) {
      case 'Escape':
        if (this.editor.isFullscreen) {
          this.editor.toggleFullscreen();
        } else if (this.editor.cursor.hasSelection()) {
          this.editor.cursor.clearSelection();
        }
        return true;
        
      case 'F11':
        e.preventDefault();
        this.editor.toggleFullscreen();
        return true;
    }
    
    return false;
  }

  /**
   * 处理回车
   * @param {boolean} shiftKey 
   */
  handleEnter(shiftKey) {
    const editor = this.editor;
    const cursor = editor.cursor;
    const document = editor.document;
    
    // 如果有选区，先删除
    if (cursor.hasSelection()) {
      const sel = cursor.getSelection();
      const min = sel.getMin();
      const max = sel.getMax();
      document.deleteRange(min.line, min.column, max.line, max.column);
      cursor.setPosition(min.line, min.column);
    }
    
    const pos = cursor.getPosition();
    
    // Shift+Enter 可以用于软换行（在某些模式下）
    // 目前统一处理为普通换行
    const newPos = document.splitLine(pos.line, pos.column);
    cursor.setPosition(newPos.line, newPos.column);
    
    // 智能缩进：如果当前行有缩进，新行也保持相同缩进
    const prevLineText = document.getLineText(pos.line);
    const indentMatch = prevLineText.match(/^(\s+)/);
    if (indentMatch) {
      const indent = indentMatch[1];
      document.insertText(newPos.line, 0, indent);
      cursor.setPosition(newPos.line, indent.length);
    }
    
    // 智能列表续行
    this.handleListContinuation(pos.line, newPos.line);
  }

  /**
   * 处理列表续行
   * @param {number} prevLine 
   * @param {number} newLine 
   */
  handleListContinuation(prevLine, newLine) {
    const document = this.editor.document;
    const cursor = this.editor.cursor;
    const prevText = document.getLineText(prevLine);
    
    // 无序列表
    const ulMatch = prevText.match(/^(\s*)([-*+])\s/);
    if (ulMatch) {
      const [, indent, marker] = ulMatch;
      // 如果上一行只有标记没有内容，删除标记
      if (prevText.trim() === marker) {
        document.setLineText(prevLine, '');
      } else {
        // 在新行添加列表标记
        const newIndent = indent || '';
        document.insertText(newLine, document.getLineLength(newLine), newIndent + marker + ' ');
        cursor.setPosition(newLine, newIndent.length + marker.length + 1);
      }
      return;
    }
    
    // 有序列表
    const olMatch = prevText.match(/^(\s*)(\d+)\.\s/);
    if (olMatch) {
      const [, indent, num] = olMatch;
      if (prevText.trim() === `${num}.`) {
        document.setLineText(prevLine, '');
      } else {
        const newIndent = indent || '';
        const newNum = parseInt(num) + 1;
        document.insertText(newLine, document.getLineLength(newLine), `${newIndent}${newNum}. `);
        cursor.setPosition(newLine, newIndent.length + String(newNum).length + 2);
      }
      return;
    }
    
    // 待办列表
    const todoMatch = prevText.match(/^(\s*)([-*+])\s\[[ x]\]\s/);
    if (todoMatch) {
      const [fullMatch, indent, marker] = todoMatch;
      if (prevText.trim() === `${marker} [ ]` || prevText.trim() === `${marker} [x]`) {
        document.setLineText(prevLine, '');
      } else {
        const newIndent = indent || '';
        document.insertText(newLine, document.getLineLength(newLine), `${newIndent}${marker} [ ] `);
        cursor.setPosition(newLine, newIndent.length + marker.length + 5);
      }
      return;
    }
  }

  /**
   * 处理退格
   */
  handleBackspace() {
    const editor = this.editor;
    const cursor = editor.cursor;
    const document = editor.document;
    
    if (cursor.hasSelection()) {
      // 删除选区
      const sel = cursor.getSelection();
      const min = sel.getMin();
      const max = sel.getMax();
      document.deleteRange(min.line, min.column, max.line, max.column);
      cursor.setPosition(min.line, min.column);
    } else {
      const pos = cursor.getPosition();
      
      if (pos.column > 0) {
        // 检查是否可以删除整个缩进
        const lineText = document.getLineText(pos.line);
        const beforeCursor = lineText.substring(0, pos.column);
        
        // 如果光标前是空格，尝试删除整个缩进单位（2个空格）
        if (beforeCursor.endsWith('  ') && /^\s*$/.test(beforeCursor)) {
          document.deleteRange(pos.line, pos.column - 2, pos.line, pos.column);
          cursor.setPosition(pos.line, pos.column - 2);
        } else {
          // 删除前一个字符
          document.deleteRange(pos.line, pos.column - 1, pos.line, pos.column);
          cursor.setPosition(pos.line, pos.column - 1);
        }
      } else if (pos.line > 0) {
        // 合并到上一行
        const newPos = document.joinLines(pos.line - 1);
        cursor.setPosition(newPos.line, newPos.column);
      }
    }
  }

  /**
   * 处理删除
   */
  handleDelete() {
    const editor = this.editor;
    const cursor = editor.cursor;
    const document = editor.document;
    
    if (cursor.hasSelection()) {
      // 删除选区
      const sel = cursor.getSelection();
      const min = sel.getMin();
      const max = sel.getMax();
      document.deleteRange(min.line, min.column, max.line, max.column);
      cursor.setPosition(min.line, min.column);
    } else {
      const pos = cursor.getPosition();
      const lineLength = document.getLineLength(pos.line);
      
      if (pos.column < lineLength) {
        // 删除后一个字符
        document.deleteRange(pos.line, pos.column, pos.line, pos.column + 1);
      } else if (pos.line < document.getLineCount() - 1) {
        // 合并下一行
        document.joinLines(pos.line);
      }
    }
  }

  /**
   * 处理 Tab
   * @param {boolean} shiftKey 
   */
  handleTab(shiftKey) {
    const editor = this.editor;
    const cursor = editor.cursor;
    const document = editor.document;
    
    if (shiftKey) {
      // Shift+Tab: 减少缩进
      this.decreaseIndent();
    } else {
      // Tab: 增加缩进或插入缩进
      if (cursor.hasSelection()) {
        this.increaseIndent();
      } else {
        // 插入缩进（2个空格）
        const pos = cursor.getPosition();
        document.insertText(pos.line, pos.column, '  ');
        cursor.setPosition(pos.line, pos.column + 2);
      }
    }
  }

  /**
   * 增加缩进
   */
  increaseIndent() {
    const cursor = this.editor.cursor;
    const document = this.editor.document;
    const sel = cursor.getSelection();
    const startLine = sel.getMin().line;
    const endLine = sel.getMax().line;
    
    for (let i = startLine; i <= endLine; i++) {
      const text = document.getLineText(i);
      document.setLineText(i, '  ' + text);
    }
    
    // 调整选区
    cursor.setSelection(
      startLine, sel.getMin().column + 2,
      endLine, sel.getMax().column + 2
    );
  }

  /**
   * 减少缩进
   */
  decreaseIndent() {
    const cursor = this.editor.cursor;
    const document = this.editor.document;
    const sel = cursor.getSelection();
    const startLine = sel.getMin().line;
    const endLine = cursor.hasSelection() ? sel.getMax().line : startLine;
    
    for (let i = startLine; i <= endLine; i++) {
      const text = document.getLineText(i);
      // 移除开头的空格（最多2个）
      if (text.startsWith('  ')) {
        document.setLineText(i, text.substring(2));
      } else if (text.startsWith(' ')) {
        document.setLineText(i, text.substring(1));
      } else if (text.startsWith('\t')) {
        document.setLineText(i, text.substring(1));
      }
    }
    
    // 调整光标位置
    if (!cursor.hasSelection()) {
      const pos = cursor.getPosition();
      const text = document.getLineText(pos.line);
      cursor.setPosition(pos.line, Math.max(0, pos.column - 2));
    }
  }

  /**
   * 销毁处理器
   */
  destroy() {
    this.shortcuts.clear();
    this.editor = null;
  }
}

export default KeyboardHandler;
