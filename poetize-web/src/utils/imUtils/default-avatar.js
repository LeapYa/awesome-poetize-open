/**
 * 网站默认头像配置
 * 提供统一的默认头像，用于所有未设置头像的场景
 */

/**
 * 获取默认头像
 * 使用 SVG data URI，不依赖外部文件
 * 
 * @returns {string} SVG data URI 格式的头像
 */
export function getDefaultAvatar() {
  // SVG 蓝色人像头像（浅蓝背景 + 深蓝人物）
  const svg = `<svg viewBox="0 0 1024 1024" xmlns="http://www.w3.org/2000/svg"><path d="M0 512c0 282.4 229.6 512 512 512s512-229.6 512-512S794.4 0 512 0 0 229.6 0 512z" fill="#C0E0FF"/><path d="M709.6 824H315.2c-36 0-65.6-29.6-65.6-65.6 0-117.6 95.2-213.6 213.6-213.6h98.4c117.6 0 213.6 95.2 213.6 213.6 0 36-28.8 65.6-65.6 65.6zM512 528c-90.4 0-164-73.6-164-164S421.6 200 512 200s164 73.6 164 164C676.8 455.2 602.4 528 512 528z" fill="#64B9FF"/><path d="M709.6 824H315.2c-36 0-65.6-29.6-65.6-65.6 0-117.6 95.2-213.6 213.6-213.6h98.4c117.6 0 213.6 95.2 213.6 213.6 0 36-28.8 65.6-65.6 65.6zM512.8 528c-90.4 0-164-73.6-164-164C348 273.6 421.6 200 512 200s164 73.6 164 164C676.8 455.2 602.4 528 512.8 528z" fill="#64B9FF"/></svg>`
  
  // URL 编码
  return 'data:image/svg+xml,' + encodeURIComponent(svg)
}

/**
 * 获取头像 URL（带默认头像回退）
 * 
 * @param {string} avatar - 用户头像 URL
 * @returns {string} 头像 URL 或默认头像
 */
export function getAvatarUrl(avatar) {
  // 处理 null、undefined、空字符串、纯空格
  if (avatar && typeof avatar === 'string' && avatar.trim()) {
    return avatar
  }
  return getDefaultAvatar()
}

/**
 * 默认头像常量
 * 可直接导入使用
 */
export const DEFAULT_AVATAR = getDefaultAvatar()

