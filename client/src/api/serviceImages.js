import { ApiError, apiUrl, readBody } from './http.js'

export async function getServiceImages(signal) {
  const response = await fetch(apiUrl('/api/service-images'), { signal, credentials: 'omit', cache: 'no-store' })
  const body = await readBody(response)
  if (!response.ok || !body || Array.isArray(body) || typeof body !== 'object'
      || Object.values(body).some((value) => typeof value !== 'string')) throw new ApiError(response.status || 500, body)
  return body
}
