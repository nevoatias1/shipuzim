import { ApiError, apiUrl, readBody } from './http.js'
import { userRequest } from './userAuth.js'
import siteContent from '../content/siteContent.js'

export function validateReviewPage(data) {
  if (!data || !Array.isArray(data.items) || !Number.isInteger(data.page) || data.page < 0
      || !Number.isInteger(data.totalPages) || data.totalPages < 0
      || !Number.isInteger(data.count) || data.count < 0 || !Number.isFinite(data.average)
      || data.items.some((item) => !Number.isInteger(item.id) || !Number.isInteger(item.rating) || item.rating < 1 || item.rating > 5)) {
    throw new ApiError(500)
  }
  return data
}

export async function getReviews(page, signal) {
  const response = await fetch(apiUrl(`/api/reviews?page=${page}&size=6`), { signal, credentials: 'omit', cache: 'no-store' })
  const body = await readBody(response)
  if (!response.ok) throw new ApiError(response.status, body)
  return validateReviewPage(body)
}

export function getOwnReview(signal) { return userRequest('/users/review', { signal }) }

export function saveOwnReview(values, signal) {
  return userRequest('/users/review', {
    method: 'PUT', signal, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(values),
  })
}

export function deleteOwnReview(signal) { return userRequest('/users/review', { method: 'DELETE', signal }) }

export function reviewErrorMessage(error) {
  const copy = siteContent.reviews
  if (error.status === 409) return copy.emailConflict
  if (error.status === 429) return copy.rateLimited
  if (error.status === 401 || error.code === 'INVALID_GOOGLE_TOKEN') return copy.signInFailed
  if (error.code === 'GOOGLE_SIGN_IN_NOT_CONFIGURED') return copy.notConfigured
  if (error.code === 'STORAGE_UNAVAILABLE') return siteContent.admin.errors.STORAGE_UNAVAILABLE
  if (error.code === 'VALIDATION_FAILED') return copy.validationFailed
  if (error.status === 404) return copy.notFound
  return copy.generalError
}
