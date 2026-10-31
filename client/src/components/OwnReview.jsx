import { useEffect, useRef, useState } from 'react'
import { deleteOwnReview, getOwnReview, reviewErrorMessage, saveOwnReview } from '../api/reviews.js'
import { UserSessionExpiredError } from '../api/userAuth.js'
import siteContent from '../content/siteContent.js'

const copy = siteContent.reviews
const emptyValues = { rating: '', text: '' }

export default function OwnReview({ onChanged }) {
  const [values, setValues] = useState(emptyValues)
  const [saved, setSaved] = useState(null)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [revision, setRevision] = useState(0)
  const [action, setAction] = useState('')
  const [errors, setErrors] = useState({})
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const form = useRef(null)
  const heading = useRef(null)
  const mutation = useRef(null)

  useEffect(() => {
    const controller = new AbortController()
    getOwnReview(controller.signal).then((item) => {
      if (!controller.signal.aborted) {
        setSaved(item); setValues({ rating: String(item.rating), text: item.text ?? '' }); setLoading(false)
      }
    }).catch((failure) => {
      if (controller.signal.aborted || failure instanceof UserSessionExpiredError) return
      if (failure.status === 404) { setSaved(null); setValues(emptyValues) }
      else setLoadError(reviewErrorMessage(failure))
      setLoading(false)
    })
    return () => controller.abort()
  }, [revision])

  useEffect(() => () => mutation.current?.abort(), [])

  function focusError(next) {
    const name = next.rating ? 'rating' : next.text ? 'text' : null
    const field = name && form.current?.querySelector(`[name="${name}"]`)
    requestAnimationFrame(() => { if (field?.isConnected) field.focus() })
  }

  function change(event) {
    const { name, value } = event.target
    setValues((current) => ({ ...current, [name]: value }))
    setErrors((current) => ({ ...current, [name]: '' })); setError(''); setNotice('')
  }

  async function submit(event) {
    event.preventDefault()
    if (mutation.current || loading || loadError) return
    const text = values.text.replace(/[\p{Cc}\p{Cf}]/gu, '').trim()
    const next = {}
    if (!/^[1-5]$/.test(values.rating)) next.rating = copy.ratingError
    if (text.length > 500) next.text = copy.textError
    setErrors(next); setError(''); setNotice('')
    if (Object.keys(next).length) { focusError(next); return }
    const controller = new AbortController()
    mutation.current = controller; setAction('save')
    try {
      const item = await saveOwnReview({ rating: Number(values.rating), text }, controller.signal)
      if (controller.signal.aborted) return
      setSaved(item); setValues({ rating: String(item.rating), text: item.text ?? '' })
      setNotice(copy.saved); onChanged()
    } catch (failure) {
      if (!controller.signal.aborted && !(failure instanceof UserSessionExpiredError)) {
        const fields = {}
        if (failure.fields?.rating) fields.rating = copy.ratingError
        if (failure.fields?.text) fields.text = copy.textError
        setErrors(fields); setError(reviewErrorMessage(failure)); focusError(fields)
      }
    } finally {
      mutation.current = null
      if (!controller.signal.aborted) setAction('')
    }
  }

  async function remove() {
    if (mutation.current || !window.confirm(copy.confirmDelete)) return
    const controller = new AbortController()
    mutation.current = controller; setAction('delete'); setError(''); setNotice('')
    try {
      await deleteOwnReview(controller.signal)
      if (controller.signal.aborted) return
      setSaved(null); setValues(emptyValues); setErrors({}); setNotice(copy.deleted)
      onChanged(); heading.current?.focus()
    } catch (failure) {
      if (!controller.signal.aborted && !(failure instanceof UserSessionExpiredError)) setError(reviewErrorMessage(failure))
    } finally {
      mutation.current = null
      if (!controller.signal.aborted) setAction('')
    }
  }

  return <section className="own-review" aria-labelledby="own-review-title" aria-busy={loading}>
    <h3 id="own-review-title" ref={heading} tabIndex={-1}>{saved ? copy.editTitle : copy.newTitle}</h3>
    <p className="review-help">{copy.formIntro}</p>
    <p className="review-feedback" role="status" aria-live="polite">{loading ? copy.loading : notice}</p>
    {loadError && <div className="review-feedback review-error" role="alert">
      <p>{loadError}</p><button type="button" className="button button-text" onClick={() => { setLoading(true); setLoadError(''); setRevision((current) => current + 1) }}>{copy.retry}</button>
    </div>}
    {!loading && !loadError && <form ref={form} onSubmit={submit} noValidate>
      <fieldset className="review-fields" disabled={Boolean(action)}>
        <fieldset className="review-rating" aria-describedby={`review-rating-help${errors.rating ? ' review-rating-error' : ''}`}>
          <legend>{copy.rating}</legend>
          <div className="review-rating-options">
            {[1, 2, 3, 4, 5].map((rating) => <label key={rating} className={Number(values.rating) === rating ? 'is-selected' : ''}>
              <input type="radio" name="rating" value={rating} checked={Number(values.rating) === rating} onChange={change} required
                aria-label={copy.starsLabel.replace('{rating}', rating)} aria-invalid={Boolean(errors.rating)} />
              <span aria-hidden="true">★</span><span aria-hidden="true">{rating}</span>
            </label>)}
          </div>
          <p className="review-help" id="review-rating-help">{copy.ratingHelp}</p>
          {errors.rating && <p className="field-error" id="review-rating-error" role="alert">{errors.rating}</p>}
        </fieldset>
        <div className="form-field">
          <label htmlFor="review-text">{copy.text}</label>
          <textarea id="review-text" name="text" rows={4} maxLength={500} value={values.text} onChange={change} placeholder={copy.textPlaceholder}
            aria-invalid={Boolean(errors.text)} aria-describedby={`review-text-help review-text-count${errors.text ? ' review-text-error' : ''}`} />
          <div className="review-text-help"><span className="review-help" id="review-text-help">{copy.textHelp}</span>
            <span className="review-help" id="review-text-count" aria-label={`${values.text.length} ${copy.counter}`}><bdi>{values.text.length} / 500</bdi></span></div>
          {errors.text && <p className="field-error" id="review-text-error" role="alert">{errors.text}</p>}
        </div>
        <div className="review-actions">
          <button type="submit" className="button button-primary" aria-busy={action === 'save'}>{action === 'save' ? copy.saving : saved ? copy.save : copy.submit}</button>
          {saved && <button type="button" className="button review-delete" onClick={remove} aria-busy={action === 'delete'}>{action === 'delete' ? copy.deleting : copy.delete}</button>}
        </div>
      </fieldset>
      <p className="review-feedback review-error" role="alert" aria-live="assertive">{error}</p>
    </form>}
  </section>
}
