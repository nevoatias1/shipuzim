import { useEffect, useRef, useState } from 'react'
import { deleteServiceImage, putServiceImage } from '../api/admin.js'
import { SessionExpiredError } from '../api/auth.js'
import { errorMessage } from '../api/errors.js'
import { validateImageFile } from '../api/gallery.js'
import { imageUrl } from '../api/http.js'
import { getServiceImages } from '../api/serviceImages.js'
import siteContent from '../content/siteContent.js'

const copy = siteContent.admin

function ServiceImageCard({ service, url, onChanged }) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const input = useRef(null)
  const submitting = useRef(false)
  const prefix = `service-${service.key}`

  async function upload(event) {
    event.preventDefault()
    if (submitting.current) return
    submitting.current = true
    setBusy(true); setError(''); setMessage('')
    try {
      const file = input.current.files[0]
      const code = await validateImageFile(file)
      if (code) {
        setError(copy.errors[code])
        return
      }
      const result = await putServiceImage(service.key, file)
      onChanged(service.key, result.imageUrl)
      input.current.value = ''
      setMessage(copy.serviceImageSaved)
    } catch (failure) {
      if (!(failure instanceof SessionExpiredError)) setError(errorMessage(failure))
    } finally {
      submitting.current = false; setBusy(false)
    }
  }

  async function remove() {
    if (submitting.current || !window.confirm(`${copy.confirmServiceImageDelete} ${service.title}`)) return
    submitting.current = true
    setBusy(true); setError(''); setMessage('')
    try {
      await deleteServiceImage(service.key)
      onChanged(service.key, null)
      setMessage(copy.serviceImageDeleted)
    } catch (failure) {
      if (!(failure instanceof SessionExpiredError)) setError(errorMessage(failure))
    } finally {
      submitting.current = false; setBusy(false)
    }
  }

  return (
    <article className="admin-surface service-image-editor" aria-labelledby={`${prefix}-title`}>
      <h3 id={`${prefix}-title`}>{service.title}</h3>
      {url ? <img className="admin-service-image" src={imageUrl(url, 600)} alt="" loading="lazy" width="600" height="400" />
        : <p className="admin-help">{copy.noServiceImage}</p>}
      <form onSubmit={upload} noValidate>
        <fieldset className="admin-fieldset" disabled={busy}>
          <div className="form-field">
            <label htmlFor={`${prefix}-file`}>{copy.file}</label>
            <input ref={input} id={`${prefix}-file`} type="file" accept="image/jpeg,image/png,image/webp"
              aria-invalid={Boolean(error)} aria-describedby={`${prefix}-help ${prefix}-error`}
              onChange={() => { setError(''); setMessage('') }} />
            <span className="admin-help" id={`${prefix}-help`}>{copy.fileHelp}</span>
          </div>
          <div className="service-image-actions">
            <button className="button button-primary" type="submit" aria-busy={busy}>{busy ? copy.saving : url ? copy.replaceServiceImage : copy.uploadServiceImage}</button>
            {url && <button className="button button-danger" type="button" onClick={remove}>{copy.removeServiceImage}</button>}
          </div>
        </fieldset>
      </form>
      <p className="admin-error" id={`${prefix}-error`} role="alert">{error}</p>
      <p className="admin-notice" role="status">{message}</p>
    </article>
  )
}

export default function ServicesPanel() {
  const [images, setImages] = useState({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    getServiceImages(controller.signal).then((result) => {
      if (!controller.signal.aborted) { setImages(result); setLoading(false) }
    }).catch((failure) => {
      if (!controller.signal.aborted) { setError(errorMessage(failure)); setLoading(false) }
    })
    return () => controller.abort()
  }, [attempt])

  function changed(key, url) {
    setImages((current) => {
      const next = { ...current }
      if (url) next[key] = url
      else delete next[key]
      return next
    })
  }

  return (
    <section aria-labelledby="service-images-title">
      <h2 id="service-images-title">{copy.servicesTab}</h2>
      <p className="admin-intro">{copy.serviceImagesIntro}</p>
      {loading && <p role="status">{copy.loading}</p>}
      {error && <div className="admin-error" role="alert"><p>{error}</p><button className="button button-text" type="button" onClick={() => { setError(''); setLoading(true); setAttempt((value) => value + 1) }}>{copy.retry}</button></div>}
      {!loading && !error && <div className="admin-service-grid">
        {siteContent.services.items.map((service) => <ServiceImageCard key={service.key} service={service} url={images[service.key]} onChanged={changed} />)}
      </div>}
    </section>
  )
}
