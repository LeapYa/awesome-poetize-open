const DEBUGGER_INTERVAL = 50; // debugger循环间隔50ms

// Vite/Rolldown 生产构建会移除直接写在源码里的 debugger，
// 这里用固定字面量创建触发器，避免被构建阶段抹掉。
// 不拼接用户输入，确保这里只执行静态的 debugger 语句。
let triggerDebugger = () => { };
try {
  triggerDebugger = new Function('', 'debugger');
} catch (error) {
  // 如果运行环境禁用了 unsafe-eval，保留快捷键拦截作为兜底。
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
    return () => { };
  }

  const shouldEnable = enableInDev || import.meta.env.PROD;
  if (!shouldEnable) {
    return () => { };
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


