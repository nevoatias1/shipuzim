import { ApiError, apiUrl, readBody } from './http.js'

const tokenKey = 'shipuzim.admin.jwt'
const expiredEvent = 'shipuzim:session-expired'

export function getToken() {
  try { return sessionStorage.getItem(tokenKey) } catch { return null }
}

export function logout() {
  try { sessionStorage.removeItem(tokenKey) } catch { /* Storage may be disabled. */ }
}

export function onSessionExpired(listener) {
  window.addEventListener(expiredEvent, listener)
  return () => window.removeEventListener(expiredEvent, listener)
}

export async function login(username, password) {
  const response = await fetch(apiUrl('/authenticate'), {
    method: 'POST', credentials: 'omit',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  })
  const body = await readBody(response)
  if (!response.ok) throw new ApiError(response.status, body)
  if (typeof body?.jwt !== 'string' || !body.jwt.trim()) throw new ApiError(500)
  try { sessionStorage.setItem(tokenKey, body.jwt) } catch {
    throw new ApiError(0, { error: 'STORAGE_UNAVAILABLE' })
  }
}

export class SessionExpiredError extends Error {
  constructor() { super('Admin session unavailable') }
}

export async function adminRequest(path, options = {}) {
  const token = getToken()
  if (!token) {
    window.dispatchEvent(new Event(expiredEvent))
    throw new SessionExpiredError()
  }
  const response = await fetch(apiUrl(path), {
    ...options, credentials: 'omit',
    headers: { ...options.headers, Authorization: `Bearer ${token}` },
  })
  const body = await readBody(response)
  const message = typeof body === 'string' ? body : `${body?.error ?? ''} ${body?.message ?? ''}`
  if (response.status === 401 || response.status === 403 || /Token has expired|Invalid token/i.test(message)) {
    // An older in-flight request must never clear a newer login.
    const currentToken = getToken()
    if (!currentToken || currentToken === token) {
      logout()
      window.dispatchEvent(new Event(expiredEvent))
    }
    throw new SessionExpiredError()
  }
  if (!response.ok) throw new ApiError(response.status, body)
  return body
}
