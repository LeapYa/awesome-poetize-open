/**
 * 验证码工具类
 */

function getCaptchaBaseURL() {
  if (
    typeof window !== 'undefined' &&
    window.VueAppConfig &&
    window.VueAppConfig.baseURL
  ) {
    return window.VueAppConfig.baseURL
  }

  if (import.meta.env.DEV) {
    return 'http://localhost:8081'
  }

  return `${location.protocol}//${location.host}/api`
}

/**
 * 检查指定操作是否需要验证码
 * @param {string} action - 操作类型: login, register, comment, reset_password
 * @returns {Promise<boolean>} - 是否需要验证码
 */
export function checkCaptchaRequired(action) {
  const baseURL = getCaptchaBaseURL()
  const requestURL = `${baseURL}/captcha/validate?action=${encodeURIComponent(action)}`

  return fetch(requestURL, {
    method: 'GET',
    headers: {
      Accept: 'application/json',
    },
  })
    .then((response) => {
      if (!response.ok) {
        throw new Error(`验证码检查请求失败: ${response.status}`)
      }

      return response.json()
    })
    .then((res) => {
      if (res && res.code === 200) {
        // 正确解析API返回的{required: true/false}格式
        return res.data && res.data.required === true
      }

      // 如果API出错，默认不需要验证（修改：使验证码出错时跳过验证，确保用户能登录）
      return false
    })
    .catch((error) => {
      console.error('验证码检查失败:', error)
      // 如果网络错误，默认不需要验证（确保在验证码服务失败时用户仍能登录）
      return false
    })
}

/**
 * 验证码状态本地缓存
 * 避免频繁请求API
 */
const captchaStatusCache = {
  data: {},
  timestamp: 0,
  // 缓存有效期30秒
  TTL: 30 * 1000,
}

/**
 * 检查指定操作是否需要验证码(带缓存)
 * @param {string} action - 操作类型
 * @returns {Promise<boolean>} - 是否需要验证码
 */
export function checkCaptchaWithCache(action) {
  const now = Date.now()

  // 如果缓存有效且包含请求的action
  if (
    now - captchaStatusCache.timestamp < captchaStatusCache.TTL &&
    captchaStatusCache.data.hasOwnProperty(action)
  ) {
    return Promise.resolve(captchaStatusCache.data[action])
  }

  // 否则请求API并更新缓存
  return checkCaptchaRequired(action).then((required) => {
    // 更新缓存
    if (now - captchaStatusCache.timestamp >= captchaStatusCache.TTL) {
      // 如果缓存过期，重置缓存
      captchaStatusCache.data = {}
      captchaStatusCache.timestamp = now
    }

    captchaStatusCache.data[action] = required
    return required
  })
}
