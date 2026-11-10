import constant from './constant'
import { useMainStore } from '../stores/main'
const SESSION_FRESH_MS = 2 * 60 * 1000

const sessionState = {
  verifiedAt: 0,
  inFlight: null,
}

function applyValidatedUser(userData) {
  const mainStore = useMainStore()
  mainStore.loadCurrentUser(userData)
  if (userData && (userData.userType === 0 || userData.userType === 1)) {
    mainStore.loadCurrentAdmin(userData)
  } else {
    mainStore.loadCurrentAdmin({})
  }
}

function createSessionValidationError(message, isAuthenticationFailure = false) {
  const error = new Error(message)
  error.isAuthenticationFailure = isAuthenticationFailure
  return error
}

async function validateSessionWithServer() {
  const response = await fetch(`${constant.baseURL}/user/token`, {
    method: 'POST',
    credentials: 'include',
  })

  if (!response.ok) {
    throw createSessionValidationError(
      `HTTP ${response.status}`,
      response.status === 401
    )
  }

  const result = await response.json()
  if (!result || result.code !== 200 || !result.data) {
    const isAuthenticationFailure = result?.code === 300 || result?.code === 401
    throw createSessionValidationError(
      result?.message || '登录状态校验失败',
      isAuthenticationFailure
    )
  }

  applyValidatedUser(result.data)
  markSessionVerified(Date.now())
  return true
}

export function markSessionVerified(verifiedAt = Date.now()) {
  sessionState.verifiedAt = verifiedAt
}

export function resetSessionValidation() {
  sessionState.verifiedAt = 0
  sessionState.inFlight = null
}

export function isSessionFresh() {
  return Date.now() - sessionState.verifiedAt < SESSION_FRESH_MS
}

export function hasStoredSessionToken() {
  const mainStore = useMainStore()
  return (
    Boolean(mainStore.currentUser && Object.keys(mainStore.currentUser).length > 0) ||
    Boolean(mainStore.currentAdmin && Object.keys(mainStore.currentAdmin).length > 0)
  )
}

export function getTrackableToken() {
  return null
}

export async function ensureSessionValid(options = {}) {
  const { force = false } = options

  if (!force && isSessionFresh()) {
    return true
  }

  if (sessionState.inFlight) {
    return sessionState.inFlight
  }

  const validationPromise = validateSessionWithServer()
    .catch((error) => {
      resetSessionValidation()

      // 断网、超时、5xx 或响应解析失败只能说明暂时无法校验，不能据此退出登录。
      // 保留本地会话并返回可用状态，下次路由或页面重新可见时会再次校验。
      if (!error?.isAuthenticationFailure) {
        return true
      }

      const mainStore = useMainStore()
      mainStore.loadCurrentUser({})
      mainStore.loadCurrentAdmin({})
      return false
    })
    .finally(() => {
      sessionState.inFlight = null
    })

  sessionState.inFlight = validationPromise
  return validationPromise
}
