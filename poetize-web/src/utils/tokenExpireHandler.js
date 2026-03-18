import { $on, $off, $once, $emit } from './gogocodeTransfer'
/**
 * Token过期处理工具
 * 统一处理token过期的逻辑，包括清除状态、跳转登录页等
 */

import { useMainStore } from '@/stores/main'
import router from '@/router'
import { resetSessionValidation } from './sessionValidation'

let handlingTokenExpire = false
let expireResetTimer = null

/**
 * 清除所有认证相关的状态
 */
export function clearAuthState() {
  resetSessionValidation()

  localStorage.removeItem('currentUser')
  localStorage.removeItem('currentAdmin')

  // 清除Pinia store中的状态
  const mainStore = useMainStore()
  mainStore.loadCurrentUser({})
  mainStore.loadCurrentAdmin({})
}

/**
 * 处理token过期的统一逻辑
 * @param {boolean} isAdmin - 是否为管理员token过期
 * @param {string} currentPath - 当前页面路径，用于登录后重定向
 * @param {Object} options - 额外选项
 */
export function handleTokenExpire(
  isAdmin = false,
  currentPath = null,
  options = {}
) {
  if (handlingTokenExpire) {
    return
  }

  handlingTokenExpire = true
  if (expireResetTimer) {
    clearTimeout(expireResetTimer)
  }
  expireResetTimer = setTimeout(() => {
    handlingTokenExpire = false
    expireResetTimer = null
  }, 1500)

  // 清除认证状态
  clearAuthState()

  // 确定当前路径，确保不为 undefined
  const routeValue = router.currentRoute?.value || router.currentRoute
  const currentRoutePath = routeValue?.path || ''
  const redirectPath = currentPath || routeValue?.fullPath || '/'

  // 根据token类型决定跳转目标
  if (isAdmin) {
    // 管理员token过期，跳转到管理员登录页

    // 检查当前是否已经在管理员登录页，避免无限重定向
    if (currentRoutePath !== '/verify') {
      router.push({
        path: '/verify',
        query: {
          redirect: redirectPath,
          expired: 'true', // 标识是因为token过期而跳转
        },
      })
    }
  } else {
    // 普通用户token过期，跳转到用户登录页

    // 检查当前是否已经在用户登录页，避免无限重定向
    if (currentRoutePath !== '/user') {
      // 如果当前在管理员页面，则跳转到管理员登录页
      if (
        redirectPath.startsWith('/admin') ||
        redirectPath.startsWith('/verify')
      ) {
        router.push({
          path: '/verify',
          query: {
            redirect: redirectPath,
            expired: 'true',
          },
        })
      } else {
        // 普通页面跳转到用户登录页
        router.push({
          path: '/user',
          query: {
            redirect: redirectPath,
            expired: 'true',
          },
        })
      }
    }
  }

  // 显示提示消息
  if (options.showMessage !== false) {
    // 延迟显示消息，确保页面已经跳转
    setTimeout(() => {
      // 尝试通过多种方式显示消息
      try {
        // 方法1: 通过Vue实例
        if (
          window.Vue &&
          window.Vue.prototype &&
          window.Vue.prototype.$message
        ) {
          window.Vue.prototype.$message({
            message: isAdmin
              ? '管理员登录已过期，请重新登录'
              : '登录已过期，请重新登录',
            type: 'warning',
          })
        }
        // 方法2: 通过全局事件总线
        else if (window.eventBus && window.eventBus.$emit) {
          $emit(window.eventBus, 'showMessage', {
            message: isAdmin
              ? '管理员登录已过期，请重新登录'
              : '登录已过期，请重新登录',
            type: 'warning',
          })
        }
        // 方法3: 简单的console提示
        else {
        }
      } catch (error) {}
    }, 100)
  }
}

/**
 * 检查token是否有效
 * @param {string} token - 要检查的token
 * @returns {boolean} - token是否有效
 */
export function isTokenValid(token) {
  if (!token || token === 'null' || token === 'undefined') {
    return false
  }

  // 基本格式检查
  if (token.length < 10) {
    return false
  }

  // 可以添加更多的token格式验证逻辑
  return true
}

/**
 * 获取有效的token
 * @param {boolean} isAdmin - 是否获取管理员token
 * @returns {string|null} - 有效的token或null
 */
export function getValidToken(isAdmin = false) {
  const mainStore = useMainStore()
  const user = isAdmin ? mainStore.currentAdmin : mainStore.currentUser
  return user && Object.keys(user).length > 0 ? '__cookie_session__' : null
}

/**
 * 检查当前用户是否已登录
 * @param {boolean} isAdmin - 是否检查管理员登录状态
 * @returns {boolean} - 是否已登录
 */
export function isLoggedIn(isAdmin = false) {
  const token = getValidToken(isAdmin)
  if (!token) {
    return false
  }

  // 检查store中的用户信息
  const mainStore = useMainStore()
  const user = isAdmin ? mainStore.currentAdmin : mainStore.currentUser

  return user && Object.keys(user).length > 0
}

export function isHandlingTokenExpire() {
  return handlingTokenExpire
}

/**
 * 统一的登录跳转处理函数
 * 用于所有需要登录的场景，确保正确保存当前页面URL并在登录后返回
 * @param {Object} router - Vue Router实例
 * @param {Object} options - 配置选项
 * @param {string} options.currentPath - 当前页面路径，如果不提供则自动获取
 * @param {boolean} options.isAdmin - 是否跳转到管理员登录页
 * @param {Object} options.extraQuery - 额外的查询参数
 * @param {string} options.message - 提示消息
 */
export function redirectToLogin(router, options = {}) {
  const {
    currentPath = null,
    isAdmin = false,
    extraQuery = {},
    message = '请先登录！',
    vueInstance = null,
  } = options

  // 获取当前页面路径 - 优先使用router的当前路径
  let redirectPath = currentPath
  if (!redirectPath) {
    // 尝试从router获取当前路径
    if (router && router.currentRoute) {
      redirectPath = router.currentRoute.fullPath
    } else {
      // 回退到window.location
      redirectPath = window.location.pathname + window.location.search
    }
  }

  // 构建查询参数
  const query = {
    redirect: redirectPath,
    ...extraQuery,
  }

  // 确定登录页面路径
  const loginPath = isAdmin ? '/verify' : '/user'

  // 显示提示消息 - 优先使用传入的Vue实例
  if (message) {
    if (vueInstance && vueInstance.$message) {
      vueInstance.$message({
        message: message,
        type: 'info',
        duration: 2000,
      })
    } else if (
      window.Vue &&
      window.Vue.prototype &&
      window.Vue.prototype.$message
    ) {
      window.Vue.prototype.$message({
        message: message,
        type: 'info',
        duration: 2000,
      })
    } else {
    }
  }

  // 跳转到登录页面
  router.push({
    path: loginPath,
    query: query,
  })
}

/**
 * 处理登录成功后的重定向
 * @param {Object} route - 当前路由对象
 * @param {Object} router - 路由器对象
 * @param {Object} options - 额外选项
 */
export function handleLoginRedirect(route, router, options = {}) {
  const redirect = route.query.redirect
  const hasComment = route.query.hasComment
  const hasReplyAction = route.query.hasReplyAction

  if (redirect && redirect !== '/user' && redirect !== '/verify') {
    // 保留特殊参数以触发相应的状态恢复
    const query = {}
    if (hasComment === 'true') query.hasComment = 'true'
    if (hasReplyAction === 'true') query.hasReplyAction = 'true'

    // 使用replace: true来避免在浏览器历史中留下登录页面
    router.replace({ path: redirect, query: query })
  } else {
    // 没有重定向参数，跳转到默认页面
    const defaultPath = options.defaultPath || '/'
    router.replace({ path: defaultPath })
  }
}

export default {
  clearAuthState,
  handleTokenExpire,
  isTokenValid,
  getValidToken,
  isLoggedIn,
  handleLoginRedirect,
  redirectToLogin,
  emits: ['showMessage'],
}
