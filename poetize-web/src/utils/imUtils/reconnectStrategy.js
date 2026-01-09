/**
 * 重连策略
 * 实现指数退避算法，避免频繁重连
 */

export class ReconnectStrategy {
  constructor(options = {}) {
    this.maxAttempts = options.maxAttempts || 10;          // 最大重连次数
    this.baseDelay = options.baseDelay || 1000;            // 基础延迟（毫秒）
    this.maxDelay = options.maxDelay || 60000;             // 最大延迟（毫秒）
    this.backoffFactor = options.backoffFactor || 2;       // 退避因子
    this.jitter = options.jitter !== false;                // 是否添加随机抖动
  }

  /**
   * 计算下次重连延迟时间
   * @param {number} attemptCount - 当前重连次数
   * @param {number} connectionDuration - 连接持续时间（毫秒）
   */
  getDelay(attemptCount, connectionDuration = 0) {
    // 基础延迟：指数退避
    let delay = this.baseDelay * Math.pow(this.backoffFactor, attemptCount - 1);
    
    // 如果连接持续时间很短，说明可能存在服务器问题，增加延迟
    if (connectionDuration > 0 && connectionDuration < 5000) {
      delay *= 2;
      console.log('[重连策略] 连接持续时间过短，延长重连延迟');
    }
    
    // 添加随机抖动，避免多个客户端同时重连
    if (this.jitter) {
      const jitterAmount = delay * 0.3;  // 30%的随机抖动
      delay += Math.random() * jitterAmount - jitterAmount / 2;
    }
    
    // 限制最大延迟
    delay = Math.min(delay, this.maxDelay);
    
    return Math.round(delay);
  }

  /**
   * 判断是否应该重连
   * @param {number} attemptCount - 当前重连次数
   * @param {Object} context - 重连上下文
   */
  shouldReconnect(attemptCount, context = {}) {
    // 检查重连次数
    if (attemptCount >= this.maxAttempts) {
      console.log('[重连策略] 已达最大重连次数，停止重连');
      return false;
    }

    // 如果被踢出，不重连
    if (context.isKicked) {
      console.log('[重连策略] 已被踢出，不重连');
      return false;
    }

    // 如果页面不可见，不重连
    if (context.isPageHidden) {
      console.log('[重连策略] 页面不可见，暂停重连');
      return false;
    }

    // 如果网络离线，不重连
    if (context.isOffline) {
      console.log('[重连策略] 网络离线，暂停重连');
      return false;
    }

    // 如果是正常关闭（close code 1000），不重连
    if (context.closeCode === 1000) {
      console.log('[重连策略] 正常关闭，不重连');
      return false;
    }

    return true;
  }

  /**
   * 检测是否可能被重复登录踢出
   * @param {number} attemptCount - 重连次数
   * @param {number} connectionDuration - 连接持续时间
   */
  detectDuplicateConnection(attemptCount, connectionDuration) {
    // 条件：连接时间很短(<3秒) 且 重连次数较多(>=5次)
    return connectionDuration < 3000 && attemptCount >= 5;
  }

  /**
   * 重置策略（在连接稳定后调用）
   */
  reset() {
    console.log('[重连策略] 重置策略');
  }
}

/**
 * 重连管理器
 * 统一管理重连逻辑
 */
export class ReconnectManager {
  constructor(strategy, timerManager) {
    this.strategy = strategy || new ReconnectStrategy();
    this.timerManager = timerManager;
    this.onReconnectCallback = null;
  }

  /**
   * 设置重连回调
   */
  onReconnect(callback) {
    this.onReconnectCallback = callback;
  }

  /**
   * 执行重连
   * @param {number} attemptCount - 当前重连次数
   * @param {Object} context - 重连上下文
   */
  scheduleReconnect(attemptCount, context = {}) {
    // 检查是否应该重连
    if (!this.strategy.shouldReconnect(attemptCount, context)) {
      return false;
    }

    // 检测是否被重复登录踢出
    if (this.strategy.detectDuplicateConnection(attemptCount, context.connectionDuration || 0)) {
      console.warn('[重连管理] 检测到可能被重复登录踢出');
      if (context.onKicked) {
        context.onKicked();
      }
      return false;
    }

    // 计算延迟
    const delay = this.strategy.getDelay(attemptCount, context.connectionDuration);
    
    console.log(`[重连管理] 安排第 ${attemptCount} 次重连，延迟 ${delay}ms`);

    // 使用定时器管理器设置重连定时器
    if (this.timerManager) {
      this.timerManager.setTimeout('reconnect', () => {
        if (this.onReconnectCallback) {
          this.onReconnectCallback(attemptCount);
        }
      }, delay);
    } else {
      // 降级：直接使用setTimeout
      setTimeout(() => {
        if (this.onReconnectCallback) {
          this.onReconnectCallback(attemptCount);
        }
      }, delay);
    }

    return true;
  }

  /**
   * 取消重连
   */
  cancel() {
    if (this.timerManager) {
      this.timerManager.clear('reconnect');
    }
    console.log('[重连管理] 取消重连');
  }
}

