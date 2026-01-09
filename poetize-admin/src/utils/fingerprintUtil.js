/**
 * 浏览器指纹工具
 * 使用 FingerprintJS 开源版生成浏览器指纹
 */

import FingerprintJS from '@fingerprintjs/fingerprintjs'

let fpPromise = null

/**
 * 初始化FingerprintJS
 */
function initFingerprint() {
  if (!fpPromise) {
    fpPromise = FingerprintJS.load()
  }
  return fpPromise
}

/**
 * 获取浏览器指纹
 * @returns {Promise<string>} 返回唯一的访客ID
 */
export async function getBrowserFingerprint() {
  try {
    const fp = await initFingerprint()
    const result = await fp.get()
    
    
    return result.visitorId
  } catch (error) {
    console.error('获取浏览器指纹失败:', error)
    // 降级方案：返回简单的设备特征组合
    return getSimpleFingerprint()
  }
}

/**
 * 获取详细的浏览器指纹信息（含组件）
 * @returns {Promise<Object>} 返回完整的指纹结果
 */
export async function getDetailedFingerprint() {
  try {
    const fp = await initFingerprint()
    const result = await fp.get()
    
    return {
      visitorId: result.visitorId,
      confidence: result.confidence || {},
      components: simplifyComponents(result.components)
    }
  } catch (error) {
    console.error('获取详细指纹失败:', error)
    return {
      visitorId: getSimpleFingerprint(),
      confidence: { score: 0 },
      components: {}
    }
  }
}

/**
 * 简化指纹组件（只保留关键信息，减少数据量）
 */
function simplifyComponents(components) {
  if (!components) return {}
  
  const simplified = {}
  const importantKeys = [
    'canvas',
    'webgl', 
    'audio',
    'fonts',
    'screenResolution',
    'timezone',
    'language',
    'platform',
    'hardwareConcurrency'
  ]
  
  importantKeys.forEach(key => {
    if (components[key]) {
      simplified[key] = components[key].value
    }
  })
  
  return simplified
}

/**
 * 简单指纹（降级方案，不依赖FingerprintJS）
 * 只采集非隐私敏感数据
 */
function getSimpleFingerprint() {
  try {
    const data = [
      screen.width + 'x' + screen.height,
      screen.colorDepth,
      navigator.language,
      navigator.platform,
      new Date().getTimezoneOffset(),
      navigator.hardwareConcurrency || 'unknown',
      navigator.maxTouchPoints || 0
    ].join('|')
    
    // 简单哈希
    let hash = 0
    for (let i = 0; i < data.length; i++) {
      const char = data.charCodeAt(i)
      hash = ((hash << 5) - hash) + char
      hash = hash & hash
    }
    
    return 'simple_' + Math.abs(hash).toString(36)
  } catch (error) {
    console.error('生成简单指纹失败:', error)
    return 'fallback_' + Date.now()
  }
}

/**
 * 检查浏览器是否支持FingerprintJS
 */
export function isFingerprintSupported() {
  return typeof window !== 'undefined' && 
         typeof document !== 'undefined'
}

