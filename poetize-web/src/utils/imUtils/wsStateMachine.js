/**
 * WebSocket状态机
 * 统一管理WebSocket连接的各种状态
 */

// WebSocket连接状态枚举
export const WsState = {
  DISCONNECTED: 'DISCONNECTED',  // 断开连接
  CONNECTING: 'CONNECTING',      // 正在连接
  CONNECTED: 'CONNECTED',        // 已连接
  RECONNECTING: 'RECONNECTING',  // 正在重连
  CLOSED: 'CLOSED'               // 已关闭（不再重连）
};

// 状态转换事件
export const WsEvent = {
  CONNECT: 'CONNECT',                 // 请求连接
  CONNECTED: 'CONNECTED',             // 连接成功
  DISCONNECT: 'DISCONNECT',           // 断开连接
  RECONNECT: 'RECONNECT',            // 请求重连
  CLOSE: 'CLOSE',                    // 关闭（不再重连）
  ERROR: 'ERROR',                    // 连接错误
  KICKED: 'KICKED'                   // 被踢出（重复登录）
};

// 允许的状态转换规则
const stateTransitions = {
  [WsState.DISCONNECTED]: [WsState.CONNECTING, WsState.CLOSED],
  [WsState.CONNECTING]: [WsState.CONNECTED, WsState.RECONNECTING, WsState.DISCONNECTED, WsState.CLOSED],
  [WsState.CONNECTED]: [WsState.DISCONNECTED, WsState.RECONNECTING, WsState.CLOSED],
  [WsState.RECONNECTING]: [WsState.CONNECTING, WsState.DISCONNECTED, WsState.CLOSED],
  [WsState.CLOSED]: []  // 终态，不允许转换
};

export class WebSocketStateMachine {
  constructor() {
    this.currentState = WsState.DISCONNECTED;
    this.listeners = new Map();
    this.metadata = {
      reconnectAttempts: 0,
      lastConnectionTime: 0,
      lastDisconnectTime: 0,
      disconnectReason: null
    };
  }

  /**
   * 获取当前状态
   */
  getState() {
    return this.currentState;
  }

  /**
   * 检查是否处于某个状态
   */
  is(state) {
    return this.currentState === state;
  }

  /**
   * 检查是否可以转换到目标状态
   */
  canTransitionTo(newState) {
    const allowedStates = stateTransitions[this.currentState];
    return allowedStates && allowedStates.includes(newState);
  }

  /**
   * 状态转换
   */
  transition(newState, reason = null) {
    if (this.currentState === newState) {
      console.log(`[状态机] 已经处于 ${newState} 状态`);
      return false;
    }

    if (!this.canTransitionTo(newState)) {
      console.warn(`[状态机] 不允许从 ${this.currentState} 转换到 ${newState}`);
      return false;
    }

    const oldState = this.currentState;
    this.currentState = newState;
    
    // 更新元数据
    this.updateMetadata(newState, reason);
    
    console.log(`[状态机] ${oldState} -> ${newState}`, reason ? `原因: ${reason}` : '');
    
    // 触发状态变更监听器
    this.notifyListeners(oldState, newState, reason);
    
    return true;
  }

  /**
   * 更新元数据
   */
  updateMetadata(newState, reason) {
    const now = Date.now();
    
    if (newState === WsState.CONNECTED) {
      this.metadata.lastConnectionTime = now;
      this.metadata.reconnectAttempts = 0;  // 连接成功后重置重连计数
    } else if (newState === WsState.DISCONNECTED || newState === WsState.RECONNECTING) {
      this.metadata.lastDisconnectTime = now;
      this.metadata.disconnectReason = reason;
      if (newState === WsState.RECONNECTING) {
        this.metadata.reconnectAttempts++;
      }
    } else if (newState === WsState.CLOSED) {
      this.metadata.reconnectAttempts = 0;
      this.metadata.disconnectReason = reason;
    }
  }

  /**
   * 获取元数据
   */
  getMetadata() {
    return { ...this.metadata };
  }

  /**
   * 重置重连计数
   */
  resetReconnectAttempts() {
    this.metadata.reconnectAttempts = 0;
  }

  /**
   * 增加重连计数
   */
  incrementReconnectAttempts() {
    this.metadata.reconnectAttempts++;
  }

  /**
   * 获取重连计数
   */
  getReconnectAttempts() {
    return this.metadata.reconnectAttempts;
  }

  /**
   * 获取连接持续时间（毫秒）
   */
  getConnectionDuration() {
    if (this.metadata.lastConnectionTime === 0) {
      return 0;
    }
    const endTime = this.metadata.lastDisconnectTime || Date.now();
    return endTime - this.metadata.lastConnectionTime;
  }

  /**
   * 添加状态变更监听器
   */
  onStateChange(callback) {
    const id = Date.now() + Math.random();
    this.listeners.set(id, callback);
    return () => this.listeners.delete(id);  // 返回取消监听的函数
  }

  /**
   * 通知所有监听器
   */
  notifyListeners(oldState, newState, reason) {
    this.listeners.forEach(callback => {
      try {
        callback(oldState, newState, reason);
      } catch (error) {
        console.error('[状态机] 监听器执行错误:', error);
      }
    });
  }

  /**
   * 重置状态机（用于清理）
   */
  reset() {
    this.currentState = WsState.DISCONNECTED;
    this.metadata = {
      reconnectAttempts: 0,
      lastConnectionTime: 0,
      lastDisconnectTime: 0,
      disconnectReason: null
    };
    this.listeners.clear();
  }
}

