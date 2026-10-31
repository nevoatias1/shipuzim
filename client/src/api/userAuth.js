import { ApiError, apiUrl, readBody } from './http.js'

const sessionKey = 'shipuzim.user.session'
const expiredEvent = 'shipuzim:user-session-expired'

export function getUserSession() {
  try {
    const session = JSON.parse(sessionStorage.getItem(sessionKey))
    return typeof session?.jwt === 'string' && session.jwt.trim()
      && typeof session.displayName === 'string' ? session : null
  } catch { return null }
}

export function logoutUser() {
  try { sessionStorage.removeItem(sessionKey) } catch { /* Storage may be disabled. */ }
  try { window.google?.accounts?.id?.disableAutoSelect() } catch { /* A blocked Google script must not prevent local logout. */ }
}

export function onUserSessionExpired(listener) {
  window.addEventListener(expiredEvent, listener)
  return () => window.removeEventListener(expiredEvent, listener)
}

export class UserSessionExpiredError extends Error {
  constructor() { super('Review user session unavailable') }
}

export async function loginWithGoogle(credential, signal) {
  const response = await fetch(apiUrl('/auth/google'), {
    method: 'POST', credentials: 'omit', signal,
    headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ credential }),
  })
  const body = await readBody(response)
  if (!response.ok) throw new ApiError(response.status, body)
  if (typeof body?.jwt !== 'string' || !body.jwt.trim() || typeof body.displayName !== 'string') throw new ApiError(500)
  signal?.throwIfAborted()
  const session = { jwt: body.jwt, displayName: body.displayName }
  try { sessionStorage.setItem(sessionKey, JSON.stringify(session)) } catch {
    throw new ApiError(0, { error: 'STORAGE_UNAVAILABLE' })
  }
  return session
}

export async function userRequest(path, options = {}) {
  const token = getUserSession()?.jwt
  if (!token) {
    window.dispatchEvent(new Event(expiredEvent))
    throw new UserSessionExpiredError()
  }
  const response = await fetch(apiUrl(path), {
    ...options, credentials: 'omit', headers: { ...options.headers, Authorization: `Bearer ${token}` },
  })
  const body = await readBody(response)
  const message = typeof body === 'string' ? body : `${body?.error ?? ''} ${body?.message ?? ''}`
  if (response.status === 401 || response.status === 403 || /Token has expired|Invalid token/i.test(message)) {
    const currentToken = getUserSession()?.jwt
    if (!currentToken || currentToken === token) {
      logoutUser()
      window.dispatchEvent(new Event(expiredEvent))
    }
    throw new UserSessionExpiredError()
  }
  if (!response.ok) throw new ApiError(response.status, body)
  return body
}
