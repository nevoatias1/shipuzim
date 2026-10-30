import { ApiError, apiUrl, readBody } from './http.js'

export async function getGallery(signal) {
  const response = await fetch(apiUrl('/api/gallery'), { signal, credentials: 'omit', cache: 'no-store' })
  const body = await readBody(response)
  if (!response.ok || !Array.isArray(body)) throw new ApiError(response.status || 500, body)
  return body
}

export async function validateImageFile(file) {
  if (!file) return 'IMAGE_REQUIRED'
  if (file.size > 10 * 1024 * 1024) return 'IMAGE_TOO_LARGE'
  const bytes = new Uint8Array(await file.slice(0, 12).arrayBuffer())
  const jpeg = bytes.length >= 3 && bytes[0] === 255 && bytes[1] === 216 && bytes[2] === 255
  const png = bytes.length >= 8 && [137, 80, 78, 71, 13, 10, 26, 10].every((value, i) => bytes[i] === value)
  const webp = bytes.length >= 12 && [82, 73, 70, 70].every((value, i) => bytes[i] === value)
    && [87, 69, 66, 80].every((value, i) => bytes[i + 8] === value)
  return jpeg || png || webp ? null : 'INVALID_IMAGE'
}
