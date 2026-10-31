import { useEffect, useRef, useState } from 'react'
import { googleClientId, loadGoogleIdentity, renderGoogleButton } from '../api/googleIdentity.js'
import { loginWithGoogle } from '../api/userAuth.js'
import { reviewErrorMessage } from '../api/reviews.js'
import siteContent from '../content/siteContent.js'

const copy = siteContent.reviews

export default function GoogleSignIn({ near, onSignedIn }) {
  const container = useRef(null)
  const [attempt, setAttempt] = useState(0)
  const [loading, setLoading] = useState(true)
  const [loadFailed, setLoadFailed] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!near || !googleClientId) return undefined
    let cancelled = false
    let cleanup
    let request
    async function credentialReceived(response) {
      if (cancelled || request) return
      if (!response?.credential) { setError(copy.signInFailed); return }
      request = new AbortController()
      setBusy(true); setError('')
      try {
        const session = await loginWithGoogle(response.credential, request.signal)
        if (!cancelled) onSignedIn(session)
      } catch (failure) {
        if (!cancelled && !request.signal.aborted) setError(reviewErrorMessage(failure))
      } finally {
        if (!cancelled) { setBusy(false); request = undefined }
      }
    }
    loadGoogleIdentity().then((sdk) => {
      if (cancelled) return
      cleanup = renderGoogleButton(sdk, container.current, credentialReceived)
      setLoading(false)
    }).catch(() => {
      if (!cancelled) { setLoading(false); setLoadFailed(true) }
    })
    return () => { cancelled = true; request?.abort(); cleanup?.() }
  }, [near, attempt, onSignedIn])

  if (!googleClientId) return <div className="review-connect">
    <button type="button" className="google-review-button" disabled>{copy.googleButton}</button>
    <p>{copy.notConfigured}</p>
  </div>

  return <div className="review-connect">
    <p>{copy.googleNote}</p>
    <div className="google-button-slot" ref={container} inert={busy} aria-busy={busy} />
    <p className="review-feedback" role="status" aria-live="polite">{busy ? copy.signingIn : loading && near ? copy.googleLoading : ''}</p>
    {loadFailed && <div className="review-feedback review-error" role="alert">
      <p>{copy.googleLoadFailed}</p>
      <button type="button" className="button button-text" onClick={() => { setLoading(true); setLoadFailed(false); setAttempt((current) => current + 1) }}>{copy.retry}</button>
    </div>}
    <p className="review-feedback review-error" role="alert" aria-live="assertive">{error}</p>
  </div>
}
