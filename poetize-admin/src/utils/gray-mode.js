/**
 * 灰度模式管理模块
 * 用于全站灰色纪念模式的启用和禁用
 */

/**
 * 应用灰度模式样式
 */
function applyGrayMode(enable) {
  try {
    const rootEl = document.documentElement;
    if (enable) {
      rootEl.classList.add('gray-mode');
      // 直接设置滤镜，防止样式被裁剪
      rootEl.style.filter = 'grayscale(100%)';
    } else {
      rootEl.classList.remove('gray-mode');
      rootEl.style.filter = '';
    }
  } catch (e) {
    console.error('应用灰度模式失败:', e);
  }
}

/**
 * 初始化灰度模式监听
 * @param {Object} mainStore - Pinia store 实例
 */
export function initGrayMode(mainStore) {
  // 初次进入时应用灰度模式
  const enable = mainStore.webInfo && mainStore.webInfo.enableGrayMode;
  applyGrayMode(enable);
  
  // 监听配置变化 - 使用 Pinia 的 $subscribe
  mainStore.$subscribe((mutation, state) => {
    const newValue = state.webInfo && state.webInfo.enableGrayMode;
    applyGrayMode(newValue);
  });
}
