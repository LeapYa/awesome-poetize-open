/**
 * 图片懒加载管理模块
 * 处理图片加载状态，提升页面渲染性能
 */

const IMAGE_LOAD_TIMEOUT = 3000; // 图片加载超时时间（毫秒）

/**
 * 为单个图片添加加载状态监听
 */
function attachImageLoadListener(img) {
  // 已加载完成的图片
  if (img.complete && img.naturalWidth > 0) {
    img.classList.add('loaded');
    return;
  }
  
  // Data URL 和 Blob URL 立即标记为已加载
  if (img.src.startsWith('data:') || img.src.startsWith('blob:')) {
    img.classList.add('loaded');
    return;
  }
  
  // 有 src 的图片添加加载监听
  if (img.src) {
    img.addEventListener('load', function() {
      this.classList.add('loaded');
    }, { once: true });
    
    img.addEventListener('error', function() {
      this.classList.add('loaded');
    }, { once: true });
    
    // 超时保护：确保图片不会永远隐藏
    setTimeout(() => {
      if (!img.classList.contains('loaded')) {
        img.classList.add('loaded');
      }
    }, IMAGE_LOAD_TIMEOUT);
  } else {
    // 没有 src 的图片直接标记为已加载
    img.classList.add('loaded');
  }
}

/**
 * 处理所有现有图片
 */
function handleExistingImages() {
  const images = document.querySelectorAll('img');
  images.forEach(img => attachImageLoadListener(img));
}

/**
 * 监听动态添加的图片
 */
function observeDynamicImages() {
  const observer = new MutationObserver((mutations) => {
    mutations.forEach((mutation) => {
      if (mutation.type === 'childList') {
        mutation.addedNodes.forEach((node) => {
          if (node.nodeType === 1) { // 元素节点
            // 直接是 img 元素
            if (node.tagName === 'IMG') {
              attachImageLoadListener(node);
            } else if (node.querySelectorAll) {
              // 检查新添加元素内部的 img
              const nestedImages = node.querySelectorAll('img');
              nestedImages.forEach(img => {
                if (!img.classList.contains('loaded')) {
                  attachImageLoadListener(img);
                }
              });
            }
          }
        });
      }
    });
  });
  
  // 开始观察 DOM 变化
  observer.observe(document.body, {
    childList: true,
    subtree: true
  });
  
  return observer;
}

/**
 * 初始化图片加载管理器
 * @returns {MutationObserver} DOM 变化观察器实例
 */
export function initImageLoader() {
  handleExistingImages();
  return observeDynamicImages();
}

