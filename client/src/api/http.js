const baseUrl = (import.meta.env.VITE_API_URL ?? '').trim().replace(/\/+$/, '')

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
