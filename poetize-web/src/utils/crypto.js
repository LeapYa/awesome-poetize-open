import constant from './constant'

/**
 * 加密工具类 - 使用AES-GCM模式，更安全
 */
class CryptoUtil {
  constructor() {
    // 使用项目中定义的密钥
    this.key = constant.cryptojs_key
  }

  /**
   * 检查 Web Crypto API 是否可用
   * 在 HTTP 环境下（非 localhost），crypto.subtle 不可用
   */
  isWebCryptoAvailable() {
    return typeof crypto !== 'undefined' &&
      typeof crypto.subtle !== 'undefined' &&
      typeof crypto.subtle.encrypt === 'function'
  }

  /**
   * 将字符串转换为ArrayBuffer
   * @param {string} string 字符串
   * @returns {ArrayBuffer} ArrayBuffer
   */
  _stringToArrayBuffer(string) {
    const buf = new ArrayBuffer(string.length)
    const bufView = new Uint8Array(buf)
    for (let i = 0, strLen = string.length; i < strLen; i++) {
      bufView[i] = string.charCodeAt(i)
    }
    return buf
  }

  /**
   * 将ArrayBuffer转换为Base64字符串
   * @param {ArrayBuffer} buffer ArrayBuffer
   * @returns {string} Base64字符串
   */
  _arrayBufferToBase64(buffer) {
    let binary = ''
    const bytes = new Uint8Array(buffer)
    const len = bytes.byteLength
    for (let i = 0; i < len; i++) {
      binary += String.fromCharCode(bytes[i])
    }
    return btoa(binary)
  }

  /**
   * 将Base64字符串转换为ArrayBuffer
   * @param {string} base64 Base64字符串
   * @returns {ArrayBuffer} ArrayBuffer
   */
  _base64ToArrayBuffer(base64) {
    const binaryString = atob(base64)
    const len = binaryString.length
    const bytes = new Uint8Array(len)
    for (let i = 0; i < len; i++) {
      bytes[i] = binaryString.charCodeAt(i)
    }
    return bytes.buffer
  }

  /**
   * AES加密 - 使用GCM模式（更安全）
   * @param {string} data 待加密的数据
   * @returns {string} 加密后的字符串（Base64编码，格式：IV+密文）
   */
  async encrypt(data) {
    // 检查 Web Crypto API 是否可用
    if (!this.isWebCryptoAvailable()) {
      const error = new Error('安全功能不可用：请使用 HTTPS 访问此网站，或使用 localhost 开发环境')
      error.code = 'CRYPTO_NOT_AVAILABLE'
      throw error
    }

    try {
      // 生成随机IV（12字节）
      const iv = crypto.getRandomValues(new Uint8Array(12))

      // 导入密钥
      const key = await crypto.subtle.importKey(
        'raw',
        this._stringToArrayBuffer(this.key),
        { name: 'AES-GCM' },
        false,
        ['encrypt']
      )

      // 加密数据
      const text = JSON.stringify(data)
      const encrypted = await crypto.subtle.encrypt(
        {
          name: 'AES-GCM',
          iv: iv,
          tagLength: 128,
        },
        key,
        this._stringToArrayBuffer(text)
      )

      // 组合IV和密文
      const combined = new Uint8Array(iv.length + encrypted.byteLength)
      combined.set(iv, 0)
      combined.set(new Uint8Array(encrypted), iv.length)

      // 返回Base64编码
      return this._arrayBufferToBase64(combined.buffer)
    } catch (error) {
      console.error('加密失败:', error)
      return null
    }
  }

  /**
   * AES解密 - 使用GCM模式（更安全）
   * @param {string} encryptedData 加密的数据（Base64编码，格式：IV+密文）
   * @returns {object} 解密后的对象
   */
  async decrypt(encryptedData) {
    try {
      // 解码Base64
      const combined = new Uint8Array(this._base64ToArrayBuffer(encryptedData))

      // 提取IV（前12字节）
      const iv = combined.slice(0, 12)
      const ciphertext = combined.slice(12)

      // 导入密钥
      const key = await crypto.subtle.importKey(
        'raw',
        this._stringToArrayBuffer(this.key),
        { name: 'AES-GCM' },
        false,
        ['decrypt']
      )

      // 解密数据
      const decrypted = await crypto.subtle.decrypt(
        {
          name: 'AES-GCM',
          iv: iv,
          tagLength: 128,
        },
        key,
        ciphertext
      )

      // 转换为字符串并解析
      const decryptedText = new TextDecoder().decode(decrypted)
      return JSON.parse(decryptedText)
    } catch (error) {
      console.error('解密失败:', error)
      return null
    }
  }

  /**
   * AES解密Base64 - 专门用于解密后端使用Base64编码的加密数据（GCM模式）
   * @param {string} encryptedData Base64编码的加密数据（格式：IV+密文）
   * @returns {object} 解密后的对象
   */
  async decryptBase64(encryptedData) {
    return await this.decrypt(encryptedData)
  }

  /**
   * 同步加密接口（兼容旧代码，但会显示警告）
   * @param {string} data 待加密的数据
   * @returns {Promise<string>} 加密后的字符串
   */
  encryptSyncWarning(data) {
    console.warn('警告：使用了同步加密接口，建议使用async/await')
    return this.encrypt(data)
  }

  /**
   * 同步解密接口（兼容旧代码，但会显示警告）
   * @param {string} encryptedData 加密的数据
   * @returns {Promise<object>} 解密后的对象
   */
  decryptSyncWarning(encryptedData) {
    console.warn('警告：使用了同步解密接口，建议使用async/await')
    return this.decrypt(encryptedData)
  }


}

// 创建单例实例
const cryptoUtil = new CryptoUtil()

export default cryptoUtil
