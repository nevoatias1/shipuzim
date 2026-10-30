import siteContent from '../content/siteContent.js'

export function fieldErrors(error) {
  const labels = siteContent.admin.validation
  return Object.fromEntries(Object.keys(error.fields ?? {}).filter((field) => labels[field])
    .map((field) => [field, labels[field]]))
}

export function errorMessage(error) {
  const messages = siteContent.admin.errors
  if (messages[error.code]) return messages[error.code]
  if (error.status === 413) return messages.IMAGE_TOO_LARGE
  if (error.status === 404) return messages.NOT_FOUND
  if (error.status === 429) return messages.TOO_MANY_REQUESTS
  return messages.GENERAL
}
