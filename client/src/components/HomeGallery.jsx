import { useEffect, useRef, useState } from 'react'
import { getGallery } from '../api/gallery.js'
import { imageUrl } from '../api/http.js'
import siteContent from '../content/siteContent.js'
import './gallery.css'

function ImageDialog({ image, onClose }) {
  const dialog = useRef(null)
  useEffect(() => {
    const element = dialog.current
    if (image && !element.open) element.showModal()
    if (!image && element.open) element.close()
    return () => { if (element.open) element.close() }
  }, [image])

  function closeOnBackdrop(event) {
    if (event.target !== event.currentTarget) return
    const bounds = event.currentTarget.getBoundingClientRect()
    if (event.clientX < bounds.left || event.clientX > bounds.right
        || event.clientY < bounds.top || event.clientY > bounds.bottom) event.currentTarget.close()
  }

  return (
    <dialog className="image-dialog" ref={dialog} aria-labelledby="gallery-dialog-title" onClose={onClose} onClick={closeOnBackdrop}>
      <button className="dialog-close" type="button" onClick={() => dialog.current.close()} aria-label={siteContent.gallery.close}>✕</button>
      {image && <>
        <img src={imageUrl(image.imageUrl)} alt={image.caption || siteContent.gallery.imageFallback} />
        <h2 id="gallery-dialog-title">{image.caption || siteContent.gallery.imageFallback}</h2>
      </>}
    </dialog>
  )
}

export default function HomeGallery({ children }) {
  const [items, setItems] = useState([])
  const [category, setCategory] = useState('ALL')
  const [selected, setSelected] = useState(null)
  const opener = useRef(null)
  const copy = siteContent.gallery

  useEffect(() => {
    const controller = new AbortController()
    getGallery(controller.signal).then((result) => {
      if (!controller.signal.aborted) setItems(result)
    }).catch(() => { /* Keep the existing placeholders when the API is unavailable. */ })
    return () => controller.abort()
  }, [])

  const live = items.length > 0
  const visible = category === 'ALL' ? items : items.filter((item) => item.category === category)

  return (
    <section className="section gallery-section" id="gallery" aria-labelledby="gallery-title">
      <div className="wrap">
        <div className="gallery-heading-row">
          {children}
          <span className="gallery-count" aria-label={copy.countLabel}>
            <span>{String(live ? visible.length : copy.items.length).padStart(2, '0')}</span> / {live ? String(items.length).padStart(2, '0') : copy.countTotal}
          </span>
        </div>
        {live && <div className="gallery-filters" role="group" aria-label={copy.filterLabel}>
          {copy.categories.map((option) => (
            <button type="button" key={option.value} aria-pressed={category === option.value} onClick={() => setCategory(option.value)}>{option.label}</button>
          ))}
        </div>}
        <div className="gallery-grid">
          {live ? visible.map((item) => (
            <article className="gallery-tile" key={item.id}>
              <button type="button" className="gallery-image-button" onClick={(event) => { opener.current = event.currentTarget; setSelected(item) }} aria-label={`${copy.enlarge}: ${item.caption || copy.imageFallback}`}>
                <img src={imageUrl(item.imageUrl)} alt={item.caption || copy.imageFallback} loading="lazy" decoding="async" />
                <span className="gallery-enlarge" aria-hidden="true">↗</span>
              </button>
              <div className="gallery-caption">
                <h3>{item.caption || copy.imageFallback}</h3>
                <span>{copy.categories.find((option) => option.value === item.category)?.label}</span>
              </div>
            </article>
          )) : copy.items.map((item, index) => (
            <article className={`gallery-tile gallery-tile-${item.tone}`} key={item.id}>
              <div className="material-art" role="img" aria-label={item.label}>
                <span className="material-shape" /><span className="material-orbit" />
                <span className="material-number">0{index + 1}</span>
                <span className="material-label">{copy.tilePlaceholder}</span>
              </div>
              <div className="gallery-caption"><h3>{item.label}</h3><span>{item.material}</span></div>
            </article>
          ))}
        </div>
        {live && visible.length === 0 && <p className="gallery-empty" role="status">{copy.emptyCategory}</p>}
        <p className="gallery-note"><span aria-hidden="true">✳</span>{live ? copy.liveNote : copy.note}</p>
      </div>
      <ImageDialog image={selected} onClose={() => { setSelected(null); if (opener.current?.isConnected) opener.current.focus() }} />
    </section>
  )
}
