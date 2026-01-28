/**
 * @file IMEHandler - 输入法处理器
 * @description 处理中文、日文等输入法的组合输入（composition）事件
 */

/**
 * 输入法处理器
 */
export class IMEHandler {
  /**
   * @param {Object} editor - 编辑器实例
   */
  constructor(editor) {
    this.editor = editor;
    
    // 输入法状态
    this.isComposing = false;
    this.compositionText = '';
    this.compositionStartPosition = null;
    
    // 回调
    this.onCompositionChange = null;
  }

  /**
   * 设置组合文本变化回调
   * @param {Function} callback 
   */
  setCompositionChangeCallback(callback) {
    this.onCompositionChange = callback;
  }

  /**
   * 处理输入法组合开始
   * @param {CompositionEvent} e 
   */
  handleCompositionStart(e) {
    this.isComposing = true;
    this.compositionText = '';
    
    // 记录组合开始时的光标位置
    this.compositionStartPosition = this.editor.cursor.getPosition();
    
    // 如果有选区，先删除选中内容
    if (this.editor.cursor.hasSelection()) {
      const sel = this.editor.cursor.getSelection();
      const min = sel.getMin();
      const max = sel.getMax();
      this.editor.document.deleteRange(min.line, min.column, max.line, max.column);
      this.editor.cursor.setPosition(min.line, min.column);
      this.compositionStartPosition = { line: min.line, column: min.column };
    }
    
    this.notifyChange();
  }

  /**
   * 处理输入法组合更新
   * @param {CompositionEvent} e 
   */
  handleCompositionUpdate(e) {
    this.compositionText = e.data || '';
    this.notifyChange();
  }

  /**
   * 处理输入法组合结束
   * @param {CompositionEvent} e 
   */
  handleCompositionEnd(e) {
    const finalText = e.data || '';
    
    this.isComposing = false;
    this.compositionText = '';
    
    // 插入最终文本
    if (finalText && this.compositionStartPosition) {
      const pos = this.compositionStartPosition;
      const newPos = this.editor.document.insertText(pos.line, pos.column, finalText);
      this.editor.cursor.setPosition(newPos.line, newPos.column);
    }
    
    this.compositionStartPosition = null;
    this.notifyChange();
  }

  /**
   * 检查是否正在输入法组合中
   * @returns {boolean}
   */
  getIsComposing() {
    return this.isComposing;
  }

  /**
   * 获取当前组合文本
   * @returns {string}
   */
  getCompositionText() {
    return this.compositionText;
  }

  /**
   * 获取组合开始位置
   * @returns {Object|null}
   */
  getCompositionStartPosition() {
    return this.compositionStartPosition;
  }

  /**
   * 通知变化
   */
  notifyChange() {
    if (this.onCompositionChange) {
      this.onCompositionChange({
        isComposing: this.isComposing,
        compositionText: this.compositionText,
        compositionStartPosition: this.compositionStartPosition
      });
    }
  }

  /**
   * 重置状态
   */
  reset() {
    this.isComposing = false;
    this.compositionText = '';
    this.compositionStartPosition = null;
  }

  /**
   * 销毁处理器
   */
  destroy() {
    this.reset();
    this.onCompositionChange = null;
    this.editor = null;
  }
}

export default IMEHandler;
