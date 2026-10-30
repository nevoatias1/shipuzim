const baseUrl = (import.meta.env.VITE_API_URL ?? '').trim().replace(/\/+$/, '')

export function apiUrl(path) {
  return `${baseUrl}/${path.replace(/^\/+/, '')}`
}

export function imageUrl(url) {
  if (!url) return ''
  return /^(?:https?:)?\/\//i.test(url) ? url : apiUrl(url)
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
