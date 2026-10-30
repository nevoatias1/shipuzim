const apiBaseUrl = (import.meta.env.VITE_API_URL ?? '').replace(/\/+$/, '')

export async function submitContactRequest(payload) {
  const response = await fetch(`${apiBaseUrl}/api/contact`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })

  if (response.status === 201) return

  const body = await response.json().catch(() => ({}))
  const error = new Error(body.error ?? `HTTP_${response.status}`)
  error.status = response.status
  error.code = body.error
  error.fields = body.fields ?? {}
  throw error
}
