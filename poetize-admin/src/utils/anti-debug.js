const DEBUGGER_INTERVAL = 50; // debugger循环间隔50ms

// 使用动态函数触发 debugger，避免被构建阶段移除
let triggerDebugger;
try {
  triggerDebugger = new Function('', 'debugger');
} catch (error) {
  triggerDebugger = () => {};
}

let debuggerTimer = null;

function handleKeydown(event) {
  const key = event.key?.toLowerCase();
  const forbidden = (
    key === 'f12' ||
    (event.ctrlKey && event.shiftKey && ['i', 'j', 'c', 'u'].includes(key)) ||
    (event.ctrlKey && key === 'u')
  );

  if (forbidden) {
    event.preventDefault();
    event.stopPropagation();
  }
}

export function initAntiDebug({ enableInDev = false } = {}) {
  if (typeof window === 'undefined') {
    return () => {};
  }

  const shouldEnable = enableInDev || process.env.NODE_ENV === 'production';
  if (!shouldEnable) {
    return () => {};
  }

  if (debuggerTimer) {
    return () => {
      clearInterval(debuggerTimer);
      debuggerTimer = null;
    };
  }

  // 快捷键拦截
  window.addEventListener('keydown', handleKeydown, true);

  // 直接启动持续的 debugger 循环
  // 如果 DevTools 打开，会立即卡住；如果没打开，debugger 会被忽略
  debuggerTimer = setInterval(() => {
    try {
      triggerDebugger();
    } catch (error) {
      // 忽略错误
    }
  }, DEBUGGER_INTERVAL);

  return () => {
    if (debuggerTimer) {
      clearInterval(debuggerTimer);
      debuggerTimer = null;
    }
    window.removeEventListener('keydown', handleKeydown, true);
  };
}

export default initAntiDebug;


