/**
 * AES-GCM 加解密工具函数
 * 从 common.js 拆分，仅登录/文章解密等少数场景使用
 */
import constant from './constant'

/**
 * 检查 Web Crypto API 是否可用
 * 在移动端 HTTP 环境下，crypto.subtle 不可用
 */
export function isWebCryptoAvailable() {
  return (
    typeof crypto !== 'undefined' &&
    typeof crypto.subtle !== 'undefined' &&
    typeof crypto.subtle.encrypt === 'function'
  )
}

/**
 * 加密 - 使用AES-GCM模式（异步）
 * 需要 Web Crypto API 支持（HTTPS 或 localhost）
 */
export async function encrypt(plaintText) {
  // 检查 Web Crypto API 是否可用
  if (!isWebCryptoAvailable()) {
    const errorMsg =
      '安全警告！当前环境不支持加密功能。请使用 HTTPS 访问此网站，或联系站长启用 HTTPS。'
    console.error(errorMsg)
    // 抛出错误让调用方处理，而不是返回明文
    throw new Error(errorMsg)
  }

  try {
    // 使用Web Crypto API实现GCM模式
    const key = await crypto.subtle.importKey(
      'raw',
      new TextEncoder().encode(constant.cryptojs_key),
      { name: 'AES-GCM' },
      false,
      ['encrypt']
    )

    // 生成随机IV
    const iv = crypto.getRandomValues(new Uint8Array(12))

    // 加密
    const encrypted = await crypto.subtle.encrypt(
      {
        name: 'AES-GCM',
        iv: iv,
        tagLength: 128,
      },
      key,
      new TextEncoder().encode(plaintText)
    )

    // 组合IV和密文
    const combined = new Uint8Array(iv.length + encrypted.byteLength)
    combined.set(iv, 0)
    combined.set(new Uint8Array(encrypted), iv.length)

    // 转换为Base64并处理特殊字符
    const base64 = btoa(String.fromCharCode(...combined))
    return base64.replace(/\//g, '_').replace(/\+/g, '-')
  } catch (error) {
    console.error('加密失败:', error)
    throw error // 重新抛出错误
  }
}

/**
 * 解密 - 使用AES-GCM模式（异步）
 * 需要 Web Crypto API 支持（HTTPS 或 localhost）
 */
export async function decrypt(encryptedBase64Str) {
  // 检查 Web Crypto API 是否可用
  if (!isWebCryptoAvailable()) {
    console.error('Web Crypto API 不可用，无法解密')
    return null
  }

  try {
    // 还原Base64特殊字符
    let base64 = encryptedBase64Str.replace(/-/g, '+').replace(/_/g, '/')

    // 转换为ArrayBuffer
    const binary = atob(base64)
    const bytes = new Uint8Array(binary.length)
    for (let i = 0; i < binary.length; i++) {
      bytes[i] = binary.charCodeAt(i)
    }

    // 提取IV和密文
    const iv = bytes.slice(0, 12)
    const ciphertext = bytes.slice(12)

    // 导入密钥
    const key = await crypto.subtle.importKey(
      'raw',
      new TextEncoder().encode(constant.cryptojs_key),
      { name: 'AES-GCM' },
      false,
      ['decrypt']
    )

    // 解密
    const decrypted = await crypto.subtle.decrypt(
      {
        name: 'AES-GCM',
        iv: iv,
        tagLength: 128,
      },
      key,
      ciphertext
    )

    return new TextDecoder().decode(decrypted)
  } catch (error) {
    console.error('解密失败:', error)
    return null
  }
}
