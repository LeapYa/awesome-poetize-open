/**
 * Session Validation for Admin Frontend
 * Validates the user session by calling the backend /user/token endpoint.
 * Since auth has migrated to HttpOnly cookies, we no longer read tokens from localStorage.
 */
import Vue from 'vue'
import constant from './constant'
import { useMainStore } from '../stores/main'

const SESSION_FRESH_MS = 2 * 60 * 1000
const SESSION_VALIDATE_TIMEOUT_MS = 20 * 1000

const sessionState = Vue.observable({
  status: 'unknown',
  verifiedAt: 0,
  inFlight: null,
  navigationPending: false,
  contentLoadingCount: 0,
})

function clearValidatedUser() {
  const mainStore = useMainStore()
  mainStore.loadCurrentUser({})
  mainStore.loadCurrentAdmin({})
}

function fetchWithTimeout(url, options = {}, timeoutMs = SESSION_VALIDATE_TIMEOUT_MS) {
  const controller = new AbortController()
  const timeoutId = window.setTimeout(() => controller.abort(), timeoutMs)

  return fetch(url, {
    ...options,
    signal: controller.signal,
  }).finally(() => {
    window.clearTimeout(timeoutId)
  })
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
  const response = await fetchWithTimeout(`${constant.baseURL}/user/token`, {
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
  sessionState.status = 'valid'
  sessionState.verifiedAt = verifiedAt
}

export function resetSessionValidation(options = {}) {
  const { status = 'unknown', clearStore = false } = options
  sessionState.status = status
  sessionState.verifiedAt = 0
  sessionState.inFlight = null
  sessionState.navigationPending = false
  sessionState.contentLoadingCount = 0

  if (clearStore) {
    clearValidatedUser()
  }
}

export function isSessionFresh() {
  return Date.now() - sessionState.verifiedAt < SESSION_FRESH_MS
}

export function getSessionState() {
  return sessionState
}

export function beginSessionValidation() {
  if (sessionState.status !== 'valid') {
    sessionState.status = 'validating'
  }
}

export function setAdminNavigationPending(pending) {
  sessionState.navigationPending = pending
}

export function setAdminContentLoading(loading) {
  if (loading) {
    sessionState.contentLoadingCount += 1
    return
  }
  sessionState.contentLoadingCount = Math.max(0, sessionState.contentLoadingCount - 1)
}

export function hasStoredSessionHint() {
  const mainStore = useMainStore()
  return (
    Boolean(mainStore.currentUser && Object.keys(mainStore.currentUser).length > 0) ||
    Boolean(mainStore.currentAdmin && Object.keys(mainStore.currentAdmin).length > 0)
  )
}

export async function ensureSessionValid(options = {}) {
  const { force = false } = options

  if (!force && isSessionFresh()) {
    sessionState.status = 'valid'
    return true
  }

  if (sessionState.inFlight) {
    return sessionState.inFlight
  }

  beginSessionValidation()

  const validationPromise = validateSessionWithServer()
    .catch((error) => {
      // 网络或服务暂时不可用不等于登录失效，保留已验证的会话和后台内容。
      if (!error?.isAuthenticationFailure) {
        sessionState.status = sessionState.verifiedAt > 0 ? 'valid' : 'unknown'
        return true
      }

      resetSessionValidation({ status: 'invalid', clearStore: true })
      return false
    })
    .finally(() => {
      sessionState.inFlight = null
    })

  sessionState.inFlight = validationPromise
  return validationPromise
}
