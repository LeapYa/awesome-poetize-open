/**
 * 推送通知解析工具
 * 从 common.js 拆分，仅首页 index.vue 使用
 */

/**
 * 判断是否为空（内联版本，避免依赖 common.js）
 */
function isEmpty(value) {
  if (
    typeof value === 'undefined' ||
    value === null ||
    (typeof value === 'string' && value.trim() === '') ||
    (Array.prototype.isPrototypeOf(value) && value.length === 0) ||
    (Object.prototype.isPrototypeOf(value) && Object.keys(value).length === 0)
  ) {
    return true
  } else {
    return false
  }
}

/**
 * 解析推送通知
 * @param {Array} notices - 通知数组
 * @param {boolean} isNotification - true返回过滤后的通知数组，false返回推送信息对象
 */
export function pushNotification(notices, isNotification) {
  // 统一的空值检查，防止null/undefined错误
  if (isEmpty(notices)) {
    return isNotification ? [] : {}
  }

  // 确保notices是数组类型
  if (!Array.isArray(notices)) {
    return isNotification ? [] : {}
  }

  if (isNotification) {
    // 返回过滤后的通知数组
    return notices.filter(
      (f) =>
        typeof f === 'string' &&
        '推送标题：' !== f.substr(0, 5) &&
        '推送封面：' !== f.substr(0, 5) &&
        '推送链接：' !== f.substr(0, 5)
    )
  } else {
    // 解析推送信息对象
    let push = {}
    notices.forEach((notice) => {
      if (typeof notice === 'string') {
        if ('推送标题：' === notice.substr(0, 5)) {
          push['标题'] = notice.substr(5)
        } else if ('推送封面：' === notice.substr(0, 5)) {
          push['封面'] = notice.substr(5)
        } else if ('推送链接：' === notice.substr(0, 5)) {
          push['链接'] = notice.substr(5)
        }
      }
    })
    return push
  }
}
