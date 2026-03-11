/**
 * @file SnapshotHistory
 * @description 基于快照的撤销/重做历史栈，适合编辑器场景
 */

export class SnapshotHistory {
  constructor(options = {}) {
    this.limit = options.limit || 1000;
    this.mergeWindow = options.mergeWindow || 400;
    this.isEqual = options.isEqual || ((a, b) => a === b);

    this.undoStack = [];
    this.redoStack = [];
    this.lastRecordedAt = 0;
  }

  reset(snapshot) {
    this.undoStack = snapshot ? [snapshot] : [];
    this.redoStack = [];
    this.lastRecordedAt = Date.now();
  }

  push(snapshot, options = {}) {
    if (!snapshot) return false;

    const { merge = false } = options;
    const current = this.peek();

    if (!current) {
      this.undoStack.push(snapshot);
      this.redoStack = [];
      this.lastRecordedAt = Date.now();
      return true;
    }

    if (this.isEqual(current, snapshot)) {
      this.undoStack[this.undoStack.length - 1] = snapshot;
      this.lastRecordedAt = Date.now();
      return false;
    }

    const now = Date.now();
    if (merge && now - this.lastRecordedAt <= this.mergeWindow) {
      this.undoStack[this.undoStack.length - 1] = snapshot;
    } else {
      this.undoStack.push(snapshot);
      while (this.undoStack.length > this.limit) {
        this.undoStack.shift();
      }
    }

    this.redoStack = [];
    this.lastRecordedAt = now;
    return true;
  }

  canUndo() {
    return this.undoStack.length > 1;
  }

  canRedo() {
    return this.redoStack.length > 0;
  }

  undo() {
    if (!this.canUndo()) return null;

    const current = this.undoStack.pop();
    this.redoStack.push(current);
    this.lastRecordedAt = Date.now();
    return this.peek();
  }

  redo() {
    if (!this.canRedo()) return null;

    const snapshot = this.redoStack.pop();
    this.undoStack.push(snapshot);
    this.lastRecordedAt = Date.now();
    return snapshot;
  }

  peek() {
    return this.undoStack.length > 0
      ? this.undoStack[this.undoStack.length - 1]
      : null;
  }

  clear() {
    this.undoStack = [];
    this.redoStack = [];
    this.lastRecordedAt = 0;
  }
}

export default SnapshotHistory;
