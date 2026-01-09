/**
 * 滚动动画指令
 * 使用 Intersection Observer API 实现元素进入视口时的动画效果
 * 替代过时的 WOW.js 库
 */

/**
 * 创建滚动动画指令
 * @param {Object} options - 配置选项
 * @param {number} options.threshold - 触发阈值，默认为 0.1
 * @param {string} options.rootMargin - 根边距，默认为 '0px 0px -50px 0px'
 * @param {boolean} options.once - 是否只触发一次，默认为 true
 * @returns {Object} Vue 指令对象
 */
export function createAnimateDirective(options = {}) {
  const {
    threshold = 0.1,
    rootMargin = '0px 0px -50px 0px',
    once = true
  } = options

  return {
    /**
     * 元素插入到 DOM 时调用
     */
    inserted(el, binding) {
      // 初始状态：隐藏元素
      el.style.opacity = '0'
      el.style.transform = 'translateY(20px)'
      el.style.transition = 'opacity 0.6s ease-out, transform 0.6s ease-out'
      
      // 存储动画状态
      el._hasAnimated = false

      // 创建 Intersection Observer
      const observer = new IntersectionObserver(
        (entries) => {
          entries.forEach((entry) => {
            // 当元素进入视口
            if (entry.isIntersecting) {
              // 触发动画
              el.style.opacity = '1'
              el.style.transform = 'translateY(0)'
              el._hasAnimated = true

              // 如果设置为只触发一次，停止观察
              if (once) {
                observer.unobserve(el)
              }
            } else if (!once && el._hasAnimated) {
              // 如果允许重复触发，当元素离开视口时重置
              el.style.opacity = '0'
              el.style.transform = 'translateY(20px)'
              el._hasAnimated = false
            }
          })
        },
        {
          threshold,
          rootMargin
        }
      )

      // 开始观察元素
      observer.observe(el)
      
      // 将 observer 存储到元素上，以便后续可以清理
      el._observer = observer
    },

    /**
     * 元素从 DOM 移除时调用，清理 observer
     */
    unbind(el) {
      if (el._observer) {
        el._observer.disconnect()
        el._observer = null
      }
    }
  }
}

/**
 * 默认滚动动画指令
 * 可直接在 Vue 中注册使用: Vue.directive('animate', animateDirective)
 */
export const animateDirective = createAnimateDirective()

export default animateDirective

