import { adminRequest } from './auth.js'
import { ApiError } from './http.js'
import { validateReviewPage } from './reviews.js'

export async function getRequests(page, signal) {
  const data = await adminRequest(`/admin/contact-requests?page=${page}&size=20`, { signal })
  if (!data || !Array.isArray(data.items)) throw new ApiError(500)
  return data
}

export function updateRequestStatus(id, status) {
  return adminRequest(`/admin/contact-requests/${id}/status`, {
    method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ status }),
  })
}

export async function getAdminGallery(signal) {
  const data = await adminRequest('/admin/gallery', { signal })
  if (!Array.isArray(data)) throw new ApiError(500)
  return data
}

export function uploadGalleryImage(file, caption, category) {
  const body = new FormData()
  body.append('file', file)
  body.append('caption', caption)
  body.append('category', category)
  return adminRequest('/admin/gallery', { method: 'POST', body })
}

export function updateGalleryItem(id, values) {
  return adminRequest(`/admin/gallery/${id}`, {
    method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(values),
  })
}

export function deleteGalleryItem(id) {
  return adminRequest(`/admin/gallery/${id}`, { method: 'DELETE' })
}

export function putServiceImage(key, file) {
  const body = new FormData()
  body.append('file', file)
  return adminRequest(`/admin/service-images/${key}`, { method: 'PUT', body })
}

export function deleteServiceImage(key) {
  return adminRequest(`/admin/service-images/${key}`, { method: 'DELETE' })
}

export async function getAdminReviews(page, signal) {
  return validateReviewPage(await adminRequest(`/admin/reviews?page=${page}&size=20`, { signal }))
}

export function deleteAdminReview(id) {
  return adminRequest(`/admin/reviews/${id}`, { method: 'DELETE' })
}
