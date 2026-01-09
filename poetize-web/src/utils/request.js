import axios from 'axios'
import constant from './constant'
//处理url参数
import cryptoUtil from './crypto'

import router from '../router'
import { handleTokenExpire } from './tokenExpireHandler'

// 缓存翻译配置，避免重复请求
let cachedTranslationConfig = null
let configCacheTime = 0
const CACHE_DURATION = 5 * 60 * 1000 // 5分钟缓存

// 获取翻译配置中的超时时间
async function getTranslationTimeout() {
  const now = Date.now()

  // 如果缓存有效，直接使用
  if (cachedTranslationConfig && now - configCacheTime < CACHE_DURATION) {
    return cachedTranslationConfig.timeout || 30
  }

  try {
    const adminToken = localStorage.getItem('adminToken')
    const userToken = localStorage.getItem('userToken')
    const token = adminToken || userToken

    const headers = {
      'User-Agent': 'axios',
    }
    if (token) {
      // 确保Authorization头存在并添加Bearer前缀
      headers.Authorization = token.startsWith('Bearer ')
        ? token
        : `Bearer ${token}`
    }

    const response = await axios.get(
      constant.pythonBaseURL + '/api/translation/config',
      {
        timeout: 10000,
        headers: headers,
      }
    )

    if (response.data && response.data.code === 200 && response.data.data) {
      const timeout = response.data.data.llm?.timeout || 30
      cachedTranslationConfig = { timeout }
      configCacheTime = now
      return timeout
    }
  } catch (error) {
    // 静默失败，使用默认值
  }

  // 如果获取失败，返回默认值
  return 30
}

// 超时配置常量
const TIMEOUT_CONFIG = {
  DEFAULT: 60000, // 默认60秒
  SEO: 30000, // SEO请求30秒
  TRANSLATION: 120000, // 翻译请求2分钟
  ARTICLE_SAVE: 300000, // 文章保存最少5分钟
  ARTICLE_BUFFER: 30, // 文章保存缓冲时间（秒）
}

// 设置请求基本配置
axios.defaults.baseURL = constant.baseURL
axios.defaults.timeout = TIMEOUT_CONFIG.DEFAULT

/**
 * 根据URL路径设置超时时间
 * @param {Object} config - axios请求配置
 * @returns {Object} 处理后的配置
 */
function configureTimeout(config) {
  if (!config.url) return config

  const url = config.url
  const isDefaultTimeout =
    !config.timeout || config.timeout === TIMEOUT_CONFIG.DEFAULT

  // SEO请求：短超时 + 重试机制
  if (url.includes('/seo/')) {
    config.timeout = TIMEOUT_CONFIG.SEO
    config.retry = 3
    config.retryDelay = 1000

    // 防止请求卡住
    const source = axios.CancelToken.source()
    config.cancelToken = source.token
    setTimeout(
      () => source.cancel('SEO请求超时自动取消'),
      TIMEOUT_CONFIG.SEO + 5000
    )

    return config
  }

  // 文章保存/更新：动态超时（根据翻译配置）
  if (
    url.includes('/article/saveArticle') ||
    url.includes('/article/updateArticle')
  ) {
    if (isDefaultTimeout) {
      const cachedTimeout = cachedTranslationConfig?.timeout || 30
      const dynamicTimeout =
        (cachedTimeout + TIMEOUT_CONFIG.ARTICLE_BUFFER) * 1000
      config.timeout = Math.max(dynamicTimeout, TIMEOUT_CONFIG.ARTICLE_SAVE)
    }
    return config
  }

  // 翻译API：长超时（AI处理需要更多时间）
  if (url.includes('/api/translation/')) {
    if (isDefaultTimeout) {
      config.timeout = TIMEOUT_CONFIG.TRANSLATION
    }
    return config
  }

  return config
}

// 添加请求拦截器
axios.interceptors.request.use(
  async function (config) {
    // 统一处理超时配置
    config = configureTimeout(config)

    // 如果是验证码相关的请求，不需要token
    if (config.url && config.url.includes('/captcha/')) {
      // 特殊处理：getBlockedIps 接口需要管理员token，不能跳过
      if (config.url.includes('/captcha/getBlockedIps')) {
        // 这个接口需要token，继续执行token添加逻辑
      } else {
        // 对于验证码配置请求，添加加密处理
        if (
          config.url.includes('/captcha/getConfig') &&
          config.method === 'get'
        ) {
          // 为GET请求添加加密标识
          if (!config.params) {
            config.params = {}
          }
          config.params.encrypted = 'true'
        }
        // 对于验证码验证请求，添加加密处理
        else if (
          (config.url.includes('/captcha/verify-checkbox') ||
            config.url.includes('/captcha/verify-slide')) &&
          config.method === 'post' &&
          config.data
        ) {
          // 检查数据是否已经加密
          if (config.data.encrypted) {
            // 数据已经加密，直接使用
            // console.log('数据已加密，跳过重复加密');
          } else {
            // 加密请求数据
            try {
              const encryptedData = await cryptoUtil.encrypt(config.data)
              if (encryptedData) {
                config.data = {
                  encrypted: encryptedData,
                }
              }
            } catch (error) {
              console.error('加密验证码验证请求失败:', error)
              // 加密失败时抛出错误，不继续发送未加密数据
              const cryptoError = new Error(error.message || '加密失败')
              cryptoError.isHttpsRequired = error.code === 'CRYPTO_NOT_AVAILABLE' ||
                (error.message && error.message.includes('HTTPS'))
              throw cryptoError
            }
          }
        }
        return config
      }
    }

    // 统一处理token逻辑
    // 确定是否为管理员请求，优先使用config中的isAdmin标志
    const isAdmin = config.isAdmin || false

    // 根据请求类型获取相应的token
    const token = isAdmin
      ? localStorage.getItem('adminToken')
      : localStorage.getItem('userToken')

    // 如果token存在，统一添加到请求头
    if (token) {
      // 确保headers对象存在
      if (!config.headers) {
        config.headers = {}
      }
      // 统一添加Bearer前缀
      config.headers.Authorization = token.startsWith('Bearer ')
        ? token
        : `Bearer ${token}`
    }

    return config
  },
  function (error) {
    return Promise.reject(error)
  }
)

// 添加响应拦截器
axios.interceptors.response.use(
  async function (response) {
    if (
      response.data !== null &&
      response.data.hasOwnProperty('code') &&
      response.data.code !== 200
    ) {
      if (response.data.code === 300 || response.data.code === 401) {
        // token失效，使用统一的token过期处理逻辑
        const isAdminRequest = response.config.isAdmin || false
        handleTokenExpire(isAdminRequest, router.currentRoute.fullPath, {
          showMessage: true,
        })
      }
      return Promise.reject(new Error(response.data.message || '请求失败'))
    }

    // 处理验证码配置响应的解密
    if (
      response.config.url &&
      response.config.url.includes('/captcha/getConfig') &&
      response.config.method === 'get' &&
      response.data &&
      response.data.data
    ) {
      try {
        // 检查响应是否加密
        if (response.data.data.encrypted) {
          // 使用decryptBase64方法解密验证码配置响应数据
          const decryptedData = await cryptoUtil.decryptBase64(
            response.data.data.encrypted
          )
          if (decryptedData) {
            response.data.data = decryptedData
          }
        }
      } catch (error) {
        console.error('解密验证码配置失败:', error)
        // 解密失败时返回原始数据
      }
    }

    // 处理验证码验证响应的解密
    if (
      response.config.url &&
      (response.config.url.includes('/captcha/verify-checkbox') ||
        response.config.url.includes('/captcha/verify-slide')) &&
      response.config.method === 'post' &&
      response.data &&
      response.data.data
    ) {
      try {
        // 检查响应是否加密
        if (response.data.data.encrypted) {
          // 使用decryptBase64方法解密验证码验证响应数据
          const decryptedData = await cryptoUtil.decryptBase64(
            response.data.data.encrypted
          )
          if (decryptedData) {
            response.data.data = decryptedData
          }
        }
      } catch (error) {
        console.error('解密验证码验证响应失败:', error)
        // 解密失败时返回原始数据
      }
    }

    // 处理登录接口响应的解密
    if (
      response.config.url &&
      response.config.url.includes('/user/login') &&
      response.config.method === 'post' &&
      response.data &&
      response.data.data &&
      response.data.data.data
    ) {
      try {
        // 使用decryptBase64方法解密登录响应数据
        const decryptedData = await cryptoUtil.decryptBase64(
          response.data.data.data
        )
        if (decryptedData) {
          response.data.data = decryptedData
        }
      } catch (error) {
        console.error('解密登录响应失败:', error)
        // 解密失败时返回原始数据
      }
    }

    return response
  },
  function (error) {
    // 处理网络错误
    if (error.response) {
      // 服务器返回错误状态码
      if (error.response.status === 401 || error.response.status === 403) {
        // token相关错误，使用统一的token过期处理逻辑
        const isAdminRequest = (error.config && error.config.isAdmin) || false
        handleTokenExpire(isAdminRequest, router.currentRoute.fullPath, {
          showMessage: true,
        })
      }
    }
    return Promise.reject(error)
  }
)

// 当data为URLSearchParams对象时设置为application/x-www-form-urlencoded;charset=utf-8
// 当data为普通对象时，会被设置为application/json;charset=utf-8

// 导出工具函数
export { getTranslationTimeout }

export default {
  post(url, params = {}, isAdmin = false, json = true) {
    if (params === null || typeof params !== 'object') {
      params = {}
    }
    let config = {
      isAdmin: isAdmin,
      headers: {},
    }

    // 注意：token处理已移至请求拦截器中统一处理，此处不再重复处理

    // 显式设置Content-Type，确保JSON请求正确识别
    if (json) {
      config.headers['Content-Type'] = 'application/json;charset=UTF-8'
    }

    // 如果不是json格式，将参数转换为URLSearchParams
    const data = json ? params : new URLSearchParams(params)

    return new Promise((resolve, reject) => {
      axios
        .post(url, data, config)
        .then((res) => {
          resolve(res.data)
        })
        .catch((err) => {
          reject(err)
        })
    })
  },

  get(url, params = {}, isAdmin = false) {
    // 防御：如果调用方传入 null / 非对象，转为空对象，避免 axios 1.x toFormData 报错
    if (params === null || typeof params !== 'object') {
      params = {}
    }
    // Axios 1.x 要求 headers 必须是纯对象，不能为 undefined。
    // 先生成基础配置，然后在有 token 时再补充 headers，避免 "target must be an object" 错误。
    const config = {
      params,
      isAdmin,
      headers: {},
    }

    // 注意：token处理已移至请求拦截器中统一处理，此处不再重复处理

    return new Promise((resolve, reject) => {
      axios
        .get(url, config)
        .then((res) => resolve(res.data))
        .catch((err) => reject(err))
    })
  },

  upload(url, param, isAdmin = false, option) {
    let config = {
      isAdmin: isAdmin,
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 60000,
    }

    // 注意：token处理已移至请求拦截器中统一处理，此处不再重复处理

    if (typeof option !== 'undefined') {
      config.onUploadProgress = (progressEvent) => {
        if (progressEvent.total > 0) {
          progressEvent.percent =
            (progressEvent.loaded / progressEvent.total) * 100
        }
        option.onProgress(progressEvent)
      }
    }

    return new Promise((resolve, reject) => {
      axios
        .post(url, param, config)
        .then((res) => {
          resolve(res.data)
        })
        .catch((err) => {
          reject(err)
        })
    })
  },

  uploadQiniu(url, param) {
    let config = {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 60000,
    }

    return new Promise((resolve, reject) => {
      axios
        .post(url, param, config)
        .then((res) => {
          resolve(res.data)
        })
        .catch((err) => {
          reject(err)
        })
    })
  },
}
