import constant from './constant'
import common from './common'
import { useMainStore } from '../stores/main'
import { handleTokenExpire } from './tokenExpireHandler'

const SESSION_TYPES = ['user', 'admin']
const SESSION_FRESH_MS = 2 * 60 * 1000

const sessionState = {
  verifiedAt: {
    user: 0,
    admin: 0,
  },
  verifiedToken: {
    user: null,
    admin: null,
  },
  inFlight: {
    user: null,
    admin: null,
  },
}

function getTokenKey(type) {
  return type === 'admin' ? 'adminToken' : 'userToken'
}

function getStoredTokenByType(type) {
  const token = localStorage.getItem(getTokenKey(type))
  return typeof token === 'string' && token.trim() !== '' ? token.trim() : null
}

function getTokenWithoutBearer(token) {
  if (!token) {
    return null
  }

  return token.startsWith('Bearer ') ? token.slice(7) : token
}

function getCurrentPath(currentPath = null) {
  if (currentPath) {
    return currentPath
  }

  if (typeof window === 'undefined') {
    return '/'
  }

  return window.location.pathname + window.location.search
}

function syncStateWithToken(type) {
  const token = getStoredTokenByType(type)

  if (!token) {
    sessionState.verifiedAt[type] = 0
    sessionState.verifiedToken[type] = null
    sessionState.inFlight[type] = null
    return null
  }

  if (sessionState.verifiedToken[type] !== token) {
    sessionState.verifiedAt[type] = 0
    sessionState.verifiedToken[type] = token
  }

  return token
}

function applyValidatedUser(type, userData) {
  const mainStore = useMainStore()
  const userToken = getStoredTokenByType('user')
  const adminToken = getStoredTokenByType('admin')

  if (type === 'admin') {
    mainStore.loadCurrentAdmin(userData)
  } else {
    mainStore.loadCurrentUser(userData)
  }

  if (userToken && adminToken && userToken === adminToken) {
    mainStore.loadCurrentUser(userData)
    mainStore.loadCurrentAdmin(userData)
    markSessionVerified('user', Date.now(), userToken)
    markSessionVerified('admin', Date.now(), adminToken)
    return
  }

  if (!userToken) {
    mainStore.loadCurrentUser({})
  }

  if (!adminToken) {
    mainStore.loadCurrentAdmin({})
  }
}

async function validateSessionWithServer(type, token) {
  const encryptedToken = await common.encrypt(getTokenWithoutBearer(token))
  const response = await fetch(`${constant.baseURL}/user/token`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: `userToken=${encodeURIComponent(encryptedToken)}`,
  })

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`)
  }

  const result = await response.json()
  if (!result || result.code !== 200 || !result.data) {
    throw new Error(result?.message || '登录已过期，请重新登录')
  }

  applyValidatedUser(type, result.data)
  markSessionVerified(type, Date.now(), token)

  return true
}

export function markSessionVerified(type, verifiedAt = Date.now(), token = null) {
  if (!SESSION_TYPES.includes(type)) {
    return
  }

  const currentToken = token || getStoredTokenByType(type)
  sessionState.verifiedAt[type] = currentToken ? verifiedAt : 0
  sessionState.verifiedToken[type] = currentToken
}

export function resetSessionValidation(type = null) {
  if (type && SESSION_TYPES.includes(type)) {
    sessionState.verifiedAt[type] = 0
    sessionState.verifiedToken[type] = null
    sessionState.inFlight[type] = null
    return
  }

  SESSION_TYPES.forEach((sessionType) => {
    sessionState.verifiedAt[sessionType] = 0
    sessionState.verifiedToken[sessionType] = null
    sessionState.inFlight[sessionType] = null
  })
}

export function isSessionFresh(type) {
  if (!SESSION_TYPES.includes(type)) {
    return false
  }

  const token = syncStateWithToken(type)
  if (!token) {
    return false
  }

  return (
    sessionState.verifiedToken[type] === token &&
    Date.now() - sessionState.verifiedAt[type] < SESSION_FRESH_MS
  )
}

export function hasStoredSessionToken() {
  return Boolean(getStoredTokenByType('user') || getStoredTokenByType('admin'))
}

export function getTrackableToken() {
  const userToken = getStoredTokenByType('user')
  if (userToken && isSessionFresh('user')) {
    return userToken
  }

  const adminToken = getStoredTokenByType('admin')
  if (adminToken && isSessionFresh('admin')) {
    return adminToken
  }

  return null
}

export async function ensureSessionValid(options = {}) {
  const {
    force = false,
    source = 'route',
    currentPath = null,
    preferAdmin = false,
  } = options

  const primaryType = preferAdmin ? 'admin' : 'user'
  const secondaryType = preferAdmin ? 'user' : 'admin'
  const primaryToken = syncStateWithToken(primaryType)
  const secondaryToken = syncStateWithToken(secondaryType)

  if (!primaryToken && !secondaryToken) {
    return true
  }

  let typeToValidate = primaryToken ? primaryType : secondaryType
  let tokenToValidate = primaryToken || secondaryToken

  if (
    primaryToken &&
    secondaryToken &&
    primaryToken !== secondaryToken &&
    !force &&
    isSessionFresh(primaryType) &&
    isSessionFresh(secondaryType)
  ) {
    return true
  }

  if (!force && isSessionFresh(typeToValidate)) {
    return true
  }

  if (sessionState.inFlight[typeToValidate]) {
    return sessionState.inFlight[typeToValidate]
  }

  const validationPromise = validateSessionWithServer(
    typeToValidate,
    tokenToValidate
  )
    .catch(() => {
      resetSessionValidation()
      handleTokenExpire(typeToValidate === 'admin', getCurrentPath(currentPath), {
        showMessage: source === 'visibility' || source === 'action',
      })
      return false
    })
    .finally(() => {
      sessionState.inFlight[typeToValidate] = null
    })

  sessionState.inFlight[typeToValidate] = validationPromise
  return validationPromise
}
