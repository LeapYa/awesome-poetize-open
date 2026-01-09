/**
 * 定时器管理器
 * 统一管理所有定时器，避免定时器混乱和泄漏
 */

export class TimerManager {
  constructor() {
    this.timers = new Map();
  }

  /**
   * 设置定时器
   * @param {string} name - 定时器名称
   * @param {Function} callback - 回调函数
   * @param {number} delay - 延迟时间（毫秒）
   * @param {boolean} isInterval - 是否为间隔定时器
   */
  set(name, callback, delay, isInterval = false) {
    // 如果已存在同名定时器，先清除
    this.clear(name);
    
    const timerId = isInterval 
      ? setInterval(callback, delay)
      : setTimeout(callback, delay);
    
    this.timers.set(name, {
      id: timerId,
      isInterval,
      callback,
      delay,
      createdAt: Date.now()
    });
    
    console.log(`[定时器] 设置 ${isInterval ? '间隔' : '延时'}定时器: ${name}, 延迟: ${delay}ms`);
    
    return timerId;
  }

  /**
   * 设置延时定时器
   */
  setTimeout(name, callback, delay) {
    return this.set(name, callback, delay, false);
  }

  /**
   * 设置间隔定时器
   */
  setInterval(name, callback, delay) {
    return this.set(name, callback, delay, true);
  }

  /**
   * 清除指定定时器
   */
  clear(name) {
    const timer = this.timers.get(name);
    if (timer) {
      if (timer.isInterval) {
        clearInterval(timer.id);
      } else {
        clearTimeout(timer.id);
      }
      this.timers.delete(name);
      console.log(`[定时器] 清除定时器: ${name}`);
      return true;
    }
    return false;
  }

  /**
   * 清除所有定时器
   */
  clearAll() {
    console.log(`[定时器] 清除所有定时器，共 ${this.timers.size} 个`);
    this.timers.forEach((timer, name) => {
      if (timer.isInterval) {
        clearInterval(timer.id);
      } else {
        clearTimeout(timer.id);
      }
    });
    this.timers.clear();
  }

  /**
   * 检查定时器是否存在
   */
  has(name) {
    return this.timers.has(name);
  }

  /**
   * 获取定时器信息
   */
  get(name) {
    return this.timers.get(name);
  }

  /**
   * 获取所有定时器名称
   */
  getNames() {
    return Array.from(this.timers.keys());
  }

  /**
   * 获取定时器数量
   */
  size() {
    return this.timers.size;
  }

  /**
   * 获取定时器运行时长
   */
  getRuntime(name) {
    const timer = this.timers.get(name);
    if (timer) {
      return Date.now() - timer.createdAt;
    }
    return 0;
  }
}

// 定时器名称常量，避免字符串拼写错误
export const TimerNames = {
  TOKEN_RENEWAL: 'tokenRenewal',           // token续签检查
  HEARTBEAT_HTTP: 'heartbeatHttp',         // HTTP心跳
  HEARTBEAT_WS: 'heartbeatWs',             // WebSocket心跳
  RECONNECT: 'reconnect',                  // 重连
  CONNECTION_STABLE: 'connectionStable'    // 连接稳定性检查
};

