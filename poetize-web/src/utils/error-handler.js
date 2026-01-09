/**
 * 全局错误处理模块
 */

/**
 * 初始化 Vue 全局错误处理器
 */
export function initVueErrorHandler(Vue) {
  window.$vueApp.config.errorHandler = (err, vm, info) => {
    // 忽略 DOM 操作相关错误
    if (
      err.message &&
      (err.message.includes('appendChild') || err.message.includes('node type'))
    ) {
      return
    }

    // 忽略 Live2D 看板娘错误
    if (err.message && err.message.includes('live2d')) {
      return
    }

    // 记录其他错误
    console.error('Vue错误:', err)
    console.error('错误信息:', info)
  }
}

/**
 * 初始化全局 Promise 错误处理器
 */
export function initPromiseErrorHandler() {
  window.addEventListener('unhandledrejection', (event) => {
    const errorStr =
      event.reason && event.reason.toString ? event.reason.toString() : ''

    // 忽略 DOM 操作相关错误
    if (errorStr.includes('appendChild') || errorStr.includes('node type')) {
      event.preventDefault()
      return
    }

    // 忽略 Live2D 看板娘错误
    if (errorStr.includes('live2d')) {
      event.preventDefault()
    }
  })
}
