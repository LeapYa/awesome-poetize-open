/**
 * Session Validation for Admin Frontend
 * Validates the user session by calling the backend /user/token endpoint.
 * Since auth has migrated to HttpOnly cookies, we no longer read tokens from localStorage.
 */
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

async function validateSessionWithServer() {
  const response = await fetch(`${constant.baseURL}/user/token`, {
    method: 'POST',
    credentials: 'include',
  })

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`)
  }

  const result = await response.json()
  if (!result || result.code !== 200 || !result.data) {
    throw new Error(result?.message || '登录已过期，请重新登录')
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
    return true
  }

  if (sessionState.inFlight) {
    return sessionState.inFlight
  }

  const validationPromise = validateSessionWithServer()
    .catch(() => {
      resetSessionValidation()
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
