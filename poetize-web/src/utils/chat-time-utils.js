/**
 * 聊天消息时间格式化工具
 * 从 common.js 拆分，仅 IM 模块使用
 */

/**
 * 格式化聊天消息时间
 * @param dateStr 时间字符串或时间对象
 * @returns 格式化后的时间（刚刚、几分钟前、今天 HH:mm、昨天 HH:mm、日期）
 */
export function formatChatTime(dateStr) {
  if (!dateStr) return ''

  let date
  if (typeof dateStr === 'string') {
    // 先尝试直接解析（ISO格式：2026-01-02T23:39:47）
    date = new Date(dateStr)
    // 如果解析失败，再尝试替换格式（旧版格式：2026-01-02 23:39:47）
    if (isNaN(date.getTime())) {
      date = new Date(dateStr.replace(/-/gi, '/'))
    }
  } else if (dateStr instanceof Date) {
    date = dateStr
  } else {
    date = new Date(dateStr)
  }

  if (isNaN(date.getTime())) return ''

  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffSeconds = Math.floor(diffMs / 1000)
  const diffMinutes = Math.floor(diffSeconds / 60)
  const diffHours = Math.floor(diffMinutes / 60)

  // 今天的开始时间
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  // 昨天的开始时间
  const yesterdayStart = new Date(todayStart.getTime() - 24 * 60 * 60 * 1000)

  const H = String(date.getHours()).padStart(2, '0')
  const m = String(date.getMinutes()).padStart(2, '0')
  const timeStr = H + ':' + m

  if (diffSeconds < 60) {
    return '刚刚'
  } else if (diffMinutes < 60) {
    return diffMinutes + '分钟前'
  } else if (date >= todayStart) {
    // 今天
    return timeStr
  } else if (date >= yesterdayStart) {
    // 昨天
    return '昨天 ' + timeStr
  } else {
    // 更早的日期
    const M = String(date.getMonth() + 1).padStart(2, '0')
    const D = String(date.getDate()).padStart(2, '0')
    // 同年只显示月日
    if (date.getFullYear() === now.getFullYear()) {
      return M + '-' + D + ' ' + timeStr
    } else {
      return date.getFullYear() + '-' + M + '-' + D
    }
  }
}
