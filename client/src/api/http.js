const baseUrl = (import.meta.env.VITE_API_URL ?? '').trim().replace(/\/+$/, '')
export const API_REQUEST_TIMEOUT_MS = 90_000

export function apiFetch(url, options = {}) {
  const controller = new AbortController()
  const abortFromCaller = () => controller.abort(options.signal.reason)
  if (options.signal?.aborted) abortFromCaller()
  else options.signal?.addEventListener('abort', abortFromCaller, { once: true })
  const timeout = window.setTimeout(() => {
    controller.abort(new DOMException('API request timed out', 'TimeoutError'))
  }, API_REQUEST_TIMEOUT_MS)
  return fetch(url, { ...options, signal: controller.signal }).finally(() => {
    window.clearTimeout(timeout)
    options.signal?.removeEventListener('abort', abortFromCaller)
  })
}

export function apiUrl(path) {
  return `${baseUrl}/${path.replace(/^\/+/, '')}`
}

export function imageUrl(url, width = 800) {
  if (!url) return ''
  const resolved = /^(?:https?:)?\/\//i.test(url) ? url : apiUrl(url)
  try {
    const parsed = new URL(resolved.startsWith('//') ? `https:${resolved}` : resolved)
    if (parsed.hostname === 'res.cloudinary.com' && parsed.pathname.includes('/upload/')) {
      return resolved.replace('/upload/', `/upload/f_auto,q_auto,w_${width}/`)
    }
  } catch { /* Relative and malformed URLs retain the existing URL handling. */ }
  return resolved
}

export async function readBody(response) {
  const text = await response.text()
  if (!text) return null
  try { return JSON.parse(text) } catch { return text }
}

export class ApiError extends Error {
  constructor(status, body) {
    super('API request failed')
    this.status = status
    this.code = body?.error
    this.fields = body?.fields ?? {}
  }
}
