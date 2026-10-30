import { useEffect, useRef, useState } from 'react'
import { deleteGalleryItem, getAdminGallery, updateGalleryItem, uploadGalleryImage } from '../api/admin.js'
import { SessionExpiredError } from '../api/auth.js'
import { errorMessage, fieldErrors } from '../api/errors.js'
import { validateImageFile } from '../api/gallery.js'
import { imageUrl } from '../api/http.js'
import siteContent from '../content/siteContent.js'

const copy = siteContent.admin
const categories = siteContent.gallery.categories.filter((option) => option.value !== 'ALL')

function focusError(form, errors) {
  const first = ['file', 'caption', 'category', 'sortOrder'].find((name) => errors[name])
  const field = first && form?.elements.namedItem(first)
  if (field) requestAnimationFrame(() => { if (field.isConnected) field.focus() })
}

function validateFields(values, withOrder = false) {
  const errors = {}
  if (values.caption.trim().length > 200) errors.caption = copy.validation.caption
  if (!categories.some((option) => option.value === values.category)) errors.category = copy.validation.category
  if (withOrder && (!values.sortOrder.trim() || !Number.isInteger(Number(values.sortOrder))
      || Number(values.sortOrder) < -2147483648 || Number(values.sortOrder) > 2147483647)) errors.sortOrder = copy.validation.sortOrder
  return errors
}

function GalleryFields({ prefix, values, onChange, errors, withOrder = false }) {
  function errorAttributes(field) {
    return { 'aria-invalid': Boolean(errors[field]), 'aria-describedby': errors[field] ? `${prefix}-${field}-error` : undefined }
  }
  return <>
    <div className="form-field">
      <label htmlFor={`${prefix}-caption`}>{copy.caption}</label>
      <input id={`${prefix}-caption`} name="caption" value={values.caption} maxLength={200} onChange={onChange} {...errorAttributes('caption')} />
      <span className="admin-help">{copy.captionHelp}</span>
      {errors.caption && <span className="field-error" id={`${prefix}-caption-error`} aria-live="polite">{errors.caption}</span>}
    </div>
    <div className={withOrder ? 'form-row' : undefined}>
      <div className="form-field">
        <label htmlFor={`${prefix}-category`}>{copy.category}</label>
        <select id={`${prefix}-category`} name="category" value={values.category} required onChange={onChange} {...errorAttributes('category')}>
          {categories.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
        </select>
        {errors.category && <span className="field-error" id={`${prefix}-category-error`} aria-live="polite">{errors.category}</span>}
      </div>
      {withOrder && <div className="form-field">
        <label htmlFor={`${prefix}-sortOrder`}>{copy.order}</label>
        <input id={`${prefix}-sortOrder`} name="sortOrder" type="number" step="1" min="-2147483648" max="2147483647" required value={values.sortOrder} onChange={onChange} {...errorAttributes('sortOrder')} />
        <span className="admin-help">{copy.orderHelp}</span>
        {errors.sortOrder && <span className="field-error" id={`${prefix}-sortOrder-error`} aria-live="polite">{errors.sortOrder}</span>}
      </div>}
    </div>
  </>
}

function UploadForm({ onUploaded }) {
  const [selection, setSelection] = useState(null)
  const [values, setValues] = useState({ caption: '', category: 'CLADDING' })
  const [errors, setErrors] = useState({})
  const [error, setError] = useState('')
  const [checking, setChecking] = useState(false)
  const [busy, setBusy] = useState(false)
  const fileInput = useRef(null)
  const form = useRef(null)
  const serial = useRef(0)
  const submitting = useRef(false)

  useEffect(() => () => { if (selection) URL.revokeObjectURL(selection.preview) }, [selection])
  useEffect(() => () => { serial.current += 1 }, [])

  async function chooseFile(event) {
    const file = event.target.files[0]
    const version = ++serial.current
    setSelection(null); setErrors({}); setError('')
    if (!file) return
    setChecking(true)
    try {
      const code = await validateImageFile(file)
      if (version !== serial.current) return
      if (code) {
        setErrors({ file: copy.errors[code] }); fileInput.current.value = ''
      } else {
        setSelection({ file, preview: URL.createObjectURL(file) })
      }
    } catch {
      if (version === serial.current) { setErrors({ file: copy.errors.INVALID_IMAGE }); fileInput.current.value = '' }
    } finally {
      if (version === serial.current) setChecking(false)
    }
  }

  function change(event) {
    const { name, value } = event.target
    setValues((current) => ({ ...current, [name]: value }))
    setErrors((current) => ({ ...current, [name]: '' }))
    setError('')
  }

  async function submit(event) {
    event.preventDefault()
    if (submitting.current || checking) return
    const next = validateFields(values)
    if (!selection) next.file = copy.errors.IMAGE_REQUIRED
    setErrors(next); setError('')
    if (Object.keys(next).length) { focusError(form.current, next); return }
    submitting.current = true; setBusy(true)
    try {
      await uploadGalleryImage(selection.file, values.caption, values.category)
      setSelection(null); setValues({ caption: '', category: 'CLADDING' })
      fileInput.current.value = ''
      onUploaded()
    } catch (failure) {
      if (!(failure instanceof SessionExpiredError)) {
        const nextErrors = fieldErrors(failure)
        setErrors(nextErrors); setError(errorMessage(failure)); focusError(form.current, nextErrors)
      }
    } finally {
      submitting.current = false; setBusy(false)
    }
  }

  return (
    <section className="gallery-upload admin-surface" aria-labelledby="upload-title">
      <div><p className="admin-kicker">{copy.galleryTab}</p><h2 id="upload-title">{copy.uploadTitle}</h2><p className="admin-intro">{copy.uploadIntro}</p></div>
      <form onSubmit={submit} ref={form} noValidate>
        <fieldset className="admin-fieldset" disabled={busy || checking}>
          <div className="form-field">
            <label htmlFor="gallery-file">{copy.file}</label>
            <input ref={fileInput} id="gallery-file" name="file" type="file" accept="image/jpeg,image/png,image/webp" onChange={chooseFile} required
              aria-invalid={Boolean(errors.file)} aria-describedby={errors.file ? 'gallery-file-error gallery-file-help' : 'gallery-file-help'} />
            <span className="admin-help" id="gallery-file-help">{copy.fileHelp}</span>
            {errors.file && <span className="field-error" id="gallery-file-error" aria-live="polite">{errors.file}</span>}
          </div>
          {selection && <img className="upload-preview" src={selection.preview} alt={copy.preview} />}
          <GalleryFields prefix="gallery-upload" values={values} onChange={change} errors={errors} />
          <button className="button button-primary" type="submit" aria-busy={busy}>{busy ? copy.uploading : copy.upload}</button>
        </fieldset>
        {busy && <progress className="upload-progress" aria-label={copy.uploading} />}
        <p className="admin-notice" role="status" aria-live="polite">{checking ? copy.checkingFile : busy ? copy.uploading : ''}</p>
        <p className="admin-error" role="alert" aria-live="assertive">{error}</p>
      </form>
    </section>
  )
}

function GalleryCard({ item, position, onChanged }) {
  const [values, setValues] = useState({ caption: item.caption ?? '', category: item.category, sortOrder: String(item.sortOrder) })
  const [errors, setErrors] = useState({})
  const [error, setError] = useState('')
  const [action, setAction] = useState('')
  const pending = useRef(false)
  const form = useRef(null)

  function change(event) {
    const { name, value } = event.target
    setValues((current) => ({ ...current, [name]: value }))
    setErrors((current) => ({ ...current, [name]: '' })); setError('')
  }

  async function save(event) {
    event.preventDefault()
    if (pending.current) return
    const next = validateFields(values, true)
    setErrors(next); setError('')
    if (Object.keys(next).length) { focusError(form.current, next); return }
    pending.current = true; setAction('save')
    try {
      await updateGalleryItem(item.id, { ...values, sortOrder: Number(values.sortOrder) })
      onChanged(copy.saved)
    } catch (failure) {
      if (!(failure instanceof SessionExpiredError)) {
        const nextErrors = fieldErrors(failure)
        setErrors(nextErrors); setError(errorMessage(failure)); focusError(form.current, nextErrors)
      }
    } finally { pending.current = false; setAction('') }
  }

  async function remove() {
    if (pending.current || !window.confirm(copy.confirmDelete)) return
    pending.current = true; setAction('delete'); setError('')
    try {
      await deleteGalleryItem(item.id)
      onChanged(copy.deleted, item.id)
    } catch (failure) {
      if (!(failure instanceof SessionExpiredError)) setError(errorMessage(failure))
    } finally { pending.current = false; setAction('') }
  }

  return (
    <article className="admin-gallery-card admin-surface">
      <img className="admin-gallery-image" src={imageUrl(item.imageUrl)} alt={item.caption || siteContent.gallery.imageFallback} loading="lazy" decoding="async" />
      <div className="admin-gallery-card-body">
        <span className="admin-kicker">{copy.currentPosition}: {position} · {categories.find((option) => option.value === item.category)?.label}</span>
        <h3>{item.caption || siteContent.gallery.imageFallback}</h3>
        <details className="gallery-editor">
          <summary>{copy.edit}</summary>
          <form ref={form} onSubmit={save} noValidate>
            <fieldset className="admin-fieldset" disabled={Boolean(action)}>
              <GalleryFields prefix={`gallery-${item.id}`} values={values} onChange={change} errors={errors} withOrder />
              <button className="button button-primary" type="submit" aria-busy={action === 'save'}>{action === 'save' ? copy.saving : copy.save}</button>
            </fieldset>
          </form>
        </details>
        <button className="button button-danger" type="button" disabled={Boolean(action)} onClick={remove} aria-busy={action === 'delete'}>{action === 'delete' ? copy.deleting : copy.delete}</button>
        <p className="admin-error" role="alert" aria-live="assertive">{error}</p>
      </div>
    </article>
  )
}

export default function GalleryPanel() {
  const [items, setItems] = useState([])
  const [revision, setRevision] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  useEffect(() => {
    const controller = new AbortController()
    getAdminGallery(controller.signal).then((result) => {
      if (!controller.signal.aborted) { setItems(result); setLoading(false) }
    }).catch((failure) => {
      if (!controller.signal.aborted && !(failure instanceof SessionExpiredError)) { setError(errorMessage(failure)); setLoading(false) }
    })
    return () => controller.abort()
  }, [revision])

  function reload(message = '', removedId) {
    if (removedId !== undefined) setItems((current) => current.filter((item) => item.id !== removedId))
    setNotice(message); setError(''); setLoading(true); setRevision((current) => current + 1)
  }

  return (
    <div className="admin-gallery">
      <UploadForm onUploaded={() => reload(copy.uploadSuccess)} />
      <section className="admin-gallery-list" aria-labelledby="admin-gallery-title">
        <h2 id="admin-gallery-title">{copy.galleryList}</h2>
        <p className="admin-notice" role="status" aria-live="polite">{loading ? copy.loading : notice}</p>
        {error && <div className="admin-error" role="alert"><p>{error}</p><button className="button button-text" type="button" onClick={() => reload()}>{copy.retry}</button></div>}
        {!loading && !error && items.length === 0 && <p className="admin-empty">{copy.noGallery}</p>}
        <div className="admin-gallery-grid">
          {items.map((item, index) => <GalleryCard key={item.id} item={item} position={index + 1} onChanged={reload} />)}
        </div>
      </section>
    </div>
  )
}
