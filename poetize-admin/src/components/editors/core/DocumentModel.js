/**
 * @file DocumentModel - 文档模型
 * @description 按行存储 Markdown 内容，支持增删改操作
 */

/**
 * 表示文档中的一行
 */
class Line {
  constructor(text = '') {
    this.text = text;
    this.renderedHTML = null; // 缓存的渲染结果
    this.dirty = true; // 是否需要重新渲染
  }

  /**
   * 设置行文本
   * @param {string} text 
   */
  setText(text) {
    if (this.text !== text) {
      this.text = text;
      this.dirty = true;
      this.renderedHTML = null;
    }
  }

  /**
   * 获取行文本
   * @returns {string}
   */
  getText() {
    return this.text;
  }

  /**
   * 设置渲染后的 HTML
   * @param {string} html 
   */
  setRenderedHTML(html) {
    this.renderedHTML = html;
    this.dirty = false;
  }

  /**
   * 获取渲染后的 HTML
   * @returns {string|null}
   */
  getRenderedHTML() {
    return this.renderedHTML;
  }

  /**
   * 检查是否需要重新渲染
   * @returns {boolean}
   */
  isDirty() {
    return this.dirty;
  }

  /**
   * 标记为需要重新渲染
   */
  markDirty() {
    this.dirty = true;
    this.renderedHTML = null;
  }
}

/**
 * 文档模型 - 管理整个文档的行
 */
export class DocumentModel {
  constructor(initialContent = '') {
    this.lines = [];
    this.listeners = new Set();
    
    // 初始化内容
    this.setContent(initialContent);
  }

  /**
   * 设置整个文档内容
   * @param {string} content 
   */
  setContent(content) {
    const textLines = content.split('\n');
    this.lines = textLines.map(text => new Line(text));
    
    // 确保至少有一行
    if (this.lines.length === 0) {
      this.lines.push(new Line(''));
    }
    
    this.notifyChange({ type: 'setContent' });
  }

  /**
   * 获取整个文档内容
   * @returns {string}
   */
  getContent() {
    return this.lines.map(line => line.getText()).join('\n');
  }

  /**
   * 获取行数
   * @returns {number}
   */
  getLineCount() {
    return this.lines.length;
  }

  /**
   * 获取指定行
   * @param {number} lineIndex 
   * @returns {Line|null}
   */
  getLine(lineIndex) {
    if (lineIndex < 0 || lineIndex >= this.lines.length) {
      return null;
    }
    return this.lines[lineIndex];
  }

  /**
   * 获取指定行的文本
   * @param {number} lineIndex 
   * @returns {string}
   */
  getLineText(lineIndex) {
    const line = this.getLine(lineIndex);
    return line ? line.getText() : '';
  }

  /**
   * 获取指定行的长度
   * @param {number} lineIndex 
   * @returns {number}
   */
  getLineLength(lineIndex) {
    return this.getLineText(lineIndex).length;
  }

  /**
   * 设置指定行的文本
   * @param {number} lineIndex 
   * @param {string} text 
   */
  setLineText(lineIndex, text) {
    const line = this.getLine(lineIndex);
    if (line) {
      line.setText(text);
      this.notifyChange({ type: 'lineChange', lineIndex, text });
    }
  }

  /**
   * 在指定位置插入文本
   * @param {number} lineIndex 
   * @param {number} column 
   * @param {string} text 
   * @returns {{line: number, column: number}} 插入后的光标位置
   */
  insertText(lineIndex, column, text) {
    const line = this.getLine(lineIndex);
    if (!line) return { line: lineIndex, column };

    const currentText = line.getText();
    const safeColumn = Math.min(Math.max(0, column), currentText.length);
    
    // 检查是否包含换行符
    if (text.includes('\n')) {
      const insertLines = text.split('\n');
      const beforeCursor = currentText.substring(0, safeColumn);
      const afterCursor = currentText.substring(safeColumn);
      
      // 修改当前行
      line.setText(beforeCursor + insertLines[0]);
      
      // 插入新行
      const newLines = [];
      for (let i = 1; i < insertLines.length - 1; i++) {
        newLines.push(new Line(insertLines[i]));
      }
      
      // 最后一行需要加上原来光标后面的内容
      const lastLineText = insertLines[insertLines.length - 1] + afterCursor;
      newLines.push(new Line(lastLineText));
      
      // 插入到 lines 数组
      this.lines.splice(lineIndex + 1, 0, ...newLines);
      
      const newLineIndex = lineIndex + insertLines.length - 1;
      const newColumn = insertLines[insertLines.length - 1].length;
      
      this.notifyChange({ 
        type: 'insertLines', 
        startLine: lineIndex, 
        count: insertLines.length 
      });
      
      return { line: newLineIndex, column: newColumn };
    } else {
      // 简单插入（无换行）
      const newText = currentText.substring(0, safeColumn) + text + currentText.substring(safeColumn);
      line.setText(newText);
      
      this.notifyChange({ type: 'lineChange', lineIndex, text: newText });
      
      return { line: lineIndex, column: safeColumn + text.length };
    }
  }

  /**
   * 删除指定范围的文本
   * @param {number} startLine 
   * @param {number} startColumn 
   * @param {number} endLine 
   * @param {number} endColumn 
   * @returns {{line: number, column: number}} 删除后的光标位置
   */
  deleteRange(startLine, startColumn, endLine, endColumn) {
    // 确保 start 在 end 之前
    if (startLine > endLine || (startLine === endLine && startColumn > endColumn)) {
      [startLine, startColumn, endLine, endColumn] = [endLine, endColumn, startLine, startColumn];
    }

    const startLineObj = this.getLine(startLine);
    const endLineObj = this.getLine(endLine);
    
    if (!startLineObj || !endLineObj) {
      return { line: startLine, column: startColumn };
    }

    const startText = startLineObj.getText();
    const endText = endLineObj.getText();
    
    const safeStartColumn = Math.min(Math.max(0, startColumn), startText.length);
    const safeEndColumn = Math.min(Math.max(0, endColumn), endText.length);

    if (startLine === endLine) {
      // 同一行内删除
      const newText = startText.substring(0, safeStartColumn) + startText.substring(safeEndColumn);
      startLineObj.setText(newText);
      
      this.notifyChange({ type: 'lineChange', lineIndex: startLine, text: newText });
    } else {
      // 跨行删除
      const newText = startText.substring(0, safeStartColumn) + endText.substring(safeEndColumn);
      startLineObj.setText(newText);
      
      // 删除中间的行
      this.lines.splice(startLine + 1, endLine - startLine);
      
      this.notifyChange({ 
        type: 'deleteLines', 
        startLine: startLine + 1, 
        count: endLine - startLine 
      });
    }

    return { line: startLine, column: safeStartColumn };
  }

  /**
   * 在指定行后插入新行
   * @param {number} afterLineIndex 在此行之后插入，-1 表示在开头插入
   * @param {string} text 新行的文本
   * @returns {number} 新行的索引
   */
  insertLineAfter(afterLineIndex, text = '') {
    const newLine = new Line(text);
    const insertIndex = afterLineIndex + 1;
    
    this.lines.splice(insertIndex, 0, newLine);
    
    this.notifyChange({ 
      type: 'insertLines', 
      startLine: insertIndex, 
      count: 1 
    });
    
    return insertIndex;
  }

  /**
   * 删除指定行
   * @param {number} lineIndex 
   */
  deleteLine(lineIndex) {
    if (lineIndex < 0 || lineIndex >= this.lines.length) return;
    
    // 确保至少保留一行
    if (this.lines.length === 1) {
      this.lines[0].setText('');
      this.notifyChange({ type: 'lineChange', lineIndex: 0, text: '' });
      return;
    }
    
    this.lines.splice(lineIndex, 1);
    
    this.notifyChange({ 
      type: 'deleteLines', 
      startLine: lineIndex, 
      count: 1 
    });
  }

  /**
   * 分割行（在指定位置插入换行符）
   * @param {number} lineIndex 
   * @param {number} column 
   * @returns {{line: number, column: number}} 新光标位置
   */
  splitLine(lineIndex, column) {
    const line = this.getLine(lineIndex);
    if (!line) return { line: lineIndex, column };

    const text = line.getText();
    const safeColumn = Math.min(Math.max(0, column), text.length);
    
    const beforeText = text.substring(0, safeColumn);
    const afterText = text.substring(safeColumn);
    
    // 修改当前行
    line.setText(beforeText);
    
    // 插入新行
    const newLineIndex = this.insertLineAfter(lineIndex, afterText);
    
    return { line: newLineIndex, column: 0 };
  }

  /**
   * 合并行（将下一行合并到当前行）
   * @param {number} lineIndex 
   * @returns {{line: number, column: number}} 合并后的光标位置（在原行末尾）
   */
  joinLines(lineIndex) {
    if (lineIndex < 0 || lineIndex >= this.lines.length - 1) {
      return { line: lineIndex, column: this.getLineLength(lineIndex) };
    }

    const currentLine = this.getLine(lineIndex);
    const nextLine = this.getLine(lineIndex + 1);
    
    const currentText = currentLine.getText();
    const nextText = nextLine.getText();
    const joinColumn = currentText.length;
    
    // 合并文本
    currentLine.setText(currentText + nextText);
    
    // 删除下一行
    this.lines.splice(lineIndex + 1, 1);
    
    this.notifyChange({ 
      type: 'joinLines', 
      lineIndex 
    });
    
    return { line: lineIndex, column: joinColumn };
  }

  /**
   * 获取指定范围的文本
   * @param {number} startLine 
   * @param {number} startColumn 
   * @param {number} endLine 
   * @param {number} endColumn 
   * @returns {string}
   */
  getTextRange(startLine, startColumn, endLine, endColumn) {
    // 确保 start 在 end 之前
    if (startLine > endLine || (startLine === endLine && startColumn > endColumn)) {
      [startLine, startColumn, endLine, endColumn] = [endLine, endColumn, startLine, startColumn];
    }

    if (startLine === endLine) {
      const text = this.getLineText(startLine);
      return text.substring(startColumn, endColumn);
    }

    const lines = [];
    
    // 第一行
    const firstLine = this.getLineText(startLine);
    lines.push(firstLine.substring(startColumn));
    
    // 中间行
    for (let i = startLine + 1; i < endLine; i++) {
      lines.push(this.getLineText(i));
    }
    
    // 最后一行
    const lastLine = this.getLineText(endLine);
    lines.push(lastLine.substring(0, endColumn));
    
    return lines.join('\n');
  }

  /**
   * 添加变更监听器
   * @param {Function} listener 
   */
  addChangeListener(listener) {
    this.listeners.add(listener);
  }

  /**
   * 移除变更监听器
   * @param {Function} listener 
   */
  removeChangeListener(listener) {
    this.listeners.delete(listener);
  }

  /**
   * 通知变更
   * @param {Object} change 
   */
  notifyChange(change) {
    this.listeners.forEach(listener => {
      try {
        listener(change);
      } catch (e) {
        console.error('DocumentModel change listener error:', e);
      }
    });
  }

  /**
   * 标记指定行需要重新渲染
   * @param {number} lineIndex 
   */
  markLineDirty(lineIndex) {
    const line = this.getLine(lineIndex);
    if (line) {
      line.markDirty();
    }
  }

  /**
   * 标记所有行需要重新渲染
   */
  markAllDirty() {
    this.lines.forEach(line => line.markDirty());
  }
}

export default DocumentModel;
