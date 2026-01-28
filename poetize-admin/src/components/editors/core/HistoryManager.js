/**
 * @file HistoryManager - 历史管理器
 * @description 实现撤销/重做功能，基于操作的增量记录
 */

/**
 * 操作类型枚举
 */
export const OperationType = {
  INSERT: 'insert',
  DELETE: 'delete',
  REPLACE: 'replace'
};

/**
 * 单个操作记录
 */
class Operation {
  constructor(type, data) {
    this.type = type;
    this.data = data;
    this.timestamp = Date.now();
  }
}

/**
 * 插入操作
 */
class InsertOperation extends Operation {
  constructor(line, column, text, newLine, newColumn) {
    super(OperationType.INSERT, {
      line,
      column,
      text,
      newLine,
      newColumn
    });
  }

  /**
   * 获取撤销操作
   * @returns {DeleteOperation}
   */
  getUndo() {
    const { line, column, text, newLine, newColumn } = this.data;
    return new DeleteOperation(line, column, newLine, newColumn, text);
  }
}

/**
 * 删除操作
 */
class DeleteOperation extends Operation {
  constructor(startLine, startColumn, endLine, endColumn, deletedText) {
    super(OperationType.DELETE, {
      startLine,
      startColumn,
      endLine,
      endColumn,
      deletedText
    });
  }

  /**
   * 获取撤销操作
   * @returns {InsertOperation}
   */
  getUndo() {
    const { startLine, startColumn, deletedText, endLine, endColumn } = this.data;
    return new InsertOperation(startLine, startColumn, deletedText, endLine, endColumn);
  }
}

/**
 * 复合操作（多个操作合并为一个撤销单元）
 */
class CompositeOperation extends Operation {
  constructor(operations) {
    super('composite', { operations });
  }

  /**
   * 获取撤销操作
   * @returns {CompositeOperation}
   */
  getUndo() {
    const undoOps = this.data.operations
      .slice()
      .reverse()
      .map(op => op.getUndo());
    return new CompositeOperation(undoOps);
  }
}

/**
 * 历史管理器
 */
export class HistoryManager {
  /**
   * @param {import('./DocumentModel').DocumentModel} document 
   * @param {import('./CursorManager').CursorManager} cursor 
   * @param {Object} options 
   */
  constructor(document, cursor, options = {}) {
    this.document = document;
    this.cursor = cursor;
    
    // 配置
    this.maxHistorySize = options.maxHistorySize || 1000;
    this.mergeInterval = options.mergeInterval || 300; // 操作合并间隔（毫秒）
    
    // 历史栈
    this.undoStack = [];
    this.redoStack = [];
    
    // 当前操作组（用于合并连续操作）
    this.currentGroup = [];
    this.lastOperationTime = 0;
    this.groupTimer = null;
    
    // 是否正在执行撤销/重做
    this.isUndoRedo = false;
    
    // 光标位置历史
    this.cursorHistory = new Map();
  }

  /**
   * 记录插入操作
   * @param {number} line 
   * @param {number} column 
   * @param {string} text 
   * @param {number} newLine 
   * @param {number} newColumn 
   */
  recordInsert(line, column, text, newLine, newColumn) {
    if (this.isUndoRedo) return;
    
    const operation = new InsertOperation(line, column, text, newLine, newColumn);
    this.addOperation(operation);
  }

  /**
   * 记录删除操作
   * @param {number} startLine 
   * @param {number} startColumn 
   * @param {number} endLine 
   * @param {number} endColumn 
   * @param {string} deletedText 
   */
  recordDelete(startLine, startColumn, endLine, endColumn, deletedText) {
    if (this.isUndoRedo) return;
    
    const operation = new DeleteOperation(startLine, startColumn, endLine, endColumn, deletedText);
    this.addOperation(operation);
  }

  /**
   * 记录替换操作（删除 + 插入，作为一个撤销单元）
   * @param {number} startLine
   * @param {number} startColumn
   * @param {number} endLine
   * @param {number} endColumn
   * @param {string} deletedText
   * @param {string} insertedText
   * @param {number} newLine
   * @param {number} newColumn
   */
  recordReplace(startLine, startColumn, endLine, endColumn, deletedText, insertedText, newLine, newColumn) {
    if (this.isUndoRedo) return;

    const operations = [];

    if (deletedText) {
      operations.push(new DeleteOperation(startLine, startColumn, endLine, endColumn, deletedText));
    }

    if (insertedText) {
      operations.push(new InsertOperation(startLine, startColumn, insertedText, newLine, newColumn));
    }

    if (operations.length === 0) return;

    const operation = operations.length === 1 ? operations[0] : new CompositeOperation(operations);
    this.addOperation(operation);
  }

  /**
   * 添加操作到历史
   * @param {Operation} operation 
   */
  addOperation(operation) {
    const now = Date.now();
    
    // 保存操作前的光标位置
    this.saveCursorPosition(operation);
    
    // 判断是否需要合并操作
    if (this.shouldMerge(operation, now)) {
      this.currentGroup.push(operation);
    } else {
      // 先提交之前的操作组
      this.commitCurrentGroup();
      // 开始新的操作组
      this.currentGroup = [operation];
    }
    
    this.lastOperationTime = now;
    
    // 清空重做栈
    this.redoStack = [];
    
    // 设置定时器，延迟提交操作组
    this.scheduleCommit();
  }

  /**
   * 判断是否应该合并操作
   * @param {Operation} operation 
   * @param {number} now 
   * @returns {boolean}
   */
  shouldMerge(operation, now) {
    // 时间间隔过长，不合并
    if (now - this.lastOperationTime > this.mergeInterval) {
      return false;
    }
    
    // 没有当前操作组，不能合并
    if (this.currentGroup.length === 0) {
      return false;
    }
    
    const lastOp = this.currentGroup[this.currentGroup.length - 1];
    
    // 类型不同，不合并
    if (operation.type !== lastOp.type) {
      return false;
    }
    
    // 插入操作：检查是否连续
    if (operation.type === OperationType.INSERT) {
      const lastEnd = { line: lastOp.data.newLine, column: lastOp.data.newColumn };
      const curStart = { line: operation.data.line, column: operation.data.column };
      
      // 位置连续且是单字符插入
      if (lastEnd.line === curStart.line && 
          lastEnd.column === curStart.column &&
          operation.data.text.length === 1 &&
          !/\n/.test(operation.data.text)) {
        return true;
      }
    }
    
    // 删除操作：检查是否连续退格
    if (operation.type === OperationType.DELETE) {
      const lastStart = { line: lastOp.data.startLine, column: lastOp.data.startColumn };
      const curEnd = { line: operation.data.endLine, column: operation.data.endColumn };
      
      // 位置连续（退格删除）
      if (lastStart.line === curEnd.line && 
          lastStart.column === curEnd.column &&
          operation.data.deletedText.length === 1 &&
          !/\n/.test(operation.data.deletedText)) {
        return true;
      }
    }
    
    return false;
  }

  /**
   * 调度提交操作组
   */
  scheduleCommit() {
    if (this.groupTimer) {
      clearTimeout(this.groupTimer);
    }
    
    this.groupTimer = setTimeout(() => {
      this.commitCurrentGroup();
    }, this.mergeInterval + 50);
  }

  /**
   * 提交当前操作组
   */
  commitCurrentGroup() {
    if (this.currentGroup.length === 0) return;
    
    let operation;
    
    if (this.currentGroup.length === 1) {
      operation = this.currentGroup[0];
    } else {
      // 合并为复合操作
      operation = new CompositeOperation(this.currentGroup);
    }
    
    this.undoStack.push(operation);
    this.currentGroup = [];
    
    // 限制历史大小
    while (this.undoStack.length > this.maxHistorySize) {
      this.undoStack.shift();
    }
  }

  /**
   * 保存光标位置
   * @param {Operation} operation 
   */
  saveCursorPosition(operation) {
    const pos = this.cursor.getPosition();
    this.cursorHistory.set(operation, {
      line: pos.line,
      column: pos.column
    });
  }

  /**
   * 撤销
   * @returns {boolean} 是否成功
   */
  undo() {
    // 先提交未提交的操作
    this.commitCurrentGroup();
    
    if (this.undoStack.length === 0) {
      return false;
    }
    
    const operation = this.undoStack.pop();
    const undoOp = operation.getUndo();
    
    this.isUndoRedo = true;
    try {
      this.applyOperation(undoOp);
      this.redoStack.push(operation);
    } finally {
      this.isUndoRedo = false;
    }
    
    return true;
  }

  /**
   * 重做
   * @returns {boolean} 是否成功
   */
  redo() {
    if (this.redoStack.length === 0) {
      return false;
    }
    
    const operation = this.redoStack.pop();
    
    this.isUndoRedo = true;
    try {
      this.applyOperation(operation);
      this.undoStack.push(operation);
    } finally {
      this.isUndoRedo = false;
    }
    
    return true;
  }

  /**
   * 应用操作
   * @param {Operation} operation 
   */
  applyOperation(operation) {
    if (operation.type === 'composite') {
      // 复合操作：按顺序应用所有子操作
      for (const subOp of operation.data.operations) {
        this.applyOperation(subOp);
      }
      return;
    }
    
    switch (operation.type) {
      case OperationType.INSERT: {
        const { line, column, text, newLine, newColumn } = operation.data;
        this.document.insertText(line, column, text);
        this.cursor.setPosition(newLine, newColumn);
        break;
      }
      
      case OperationType.DELETE: {
        const { startLine, startColumn, endLine, endColumn } = operation.data;
        this.document.deleteRange(startLine, startColumn, endLine, endColumn);
        this.cursor.setPosition(startLine, startColumn);
        break;
      }
    }
  }

  /**
   * 检查是否可以撤销
   * @returns {boolean}
   */
  canUndo() {
    return this.undoStack.length > 0 || this.currentGroup.length > 0;
  }

  /**
   * 检查是否可以重做
   * @returns {boolean}
   */
  canRedo() {
    return this.redoStack.length > 0;
  }

  /**
   * 获取撤销栈大小
   * @returns {number}
   */
  getUndoStackSize() {
    return this.undoStack.length + (this.currentGroup.length > 0 ? 1 : 0);
  }

  /**
   * 获取重做栈大小
   * @returns {number}
   */
  getRedoStackSize() {
    return this.redoStack.length;
  }

  /**
   * 清空历史
   */
  clear() {
    this.undoStack = [];
    this.redoStack = [];
    this.currentGroup = [];
    this.cursorHistory.clear();
    
    if (this.groupTimer) {
      clearTimeout(this.groupTimer);
      this.groupTimer = null;
    }
  }

  /**
   * 标记保存点
   * 可用于实现"是否有未保存更改"功能
   */
  markSavePoint() {
    this.commitCurrentGroup();
    this.savePointIndex = this.undoStack.length;
  }

  /**
   * 检查是否有未保存的更改
   * @returns {boolean}
   */
  hasUnsavedChanges() {
    if (this.savePointIndex === undefined) {
      return this.undoStack.length > 0 || this.currentGroup.length > 0;
    }
    
    return this.undoStack.length !== this.savePointIndex || this.currentGroup.length > 0;
  }

  /**
   * 销毁
   */
  destroy() {
    this.clear();
    this.document = null;
    this.cursor = null;
  }
}

export default HistoryManager;
