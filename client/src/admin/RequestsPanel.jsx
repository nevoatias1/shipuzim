import { useEffect, useRef, useState } from 'react'
import { getRequests, updateRequestStatus } from '../api/admin.js'
import { SessionExpiredError } from '../api/auth.js'
import { errorMessage } from '../api/errors.js'
import siteContent from '../content/siteContent.js'

const copy = siteContent.admin
const dateFormatter = new Intl.DateTimeFormat('he-IL', { dateStyle: 'medium', timeStyle: 'short' })

function createdDate(value) {
  if (!value) return null
  const date = new Date(/(?:Z|[+-]\d\d:\d\d)$/.test(value) ? value : `${value}Z`)
  return Number.isNaN(date.getTime()) ? null : date
}

export default function RequestsPanel() {
  const [page, setPage] = useState(0)
  const [revision, setRevision] = useState(0)
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [pending, setPending] = useState({})
  const pendingIds = useRef(new Set())

  useEffect(() => {
    const controller = new AbortController()
    getRequests(page, controller.signal).then((result) => {
      if (!controller.signal.aborted) { setData(result); setLoading(false) }
    }).catch((failure) => {
      if (!controller.signal.aborted && !(failure instanceof SessionExpiredError)) {
        setError(errorMessage(failure)); setLoading(false)
      }
    })
    return () => controller.abort()
  }, [page, revision])

  function loadPage(next) {
    setData(null); setLoading(true); setError(''); setNotice(''); setPage(next); setRevision((current) => current + 1)
  }

  async function toggle(item) {
    if (pendingIds.current.has(item.id)) return
    pendingIds.current.add(item.id)
    setPending((current) => ({ ...current, [item.id]: true }))
    setError(''); setNotice('')
    const nextStatus = item.status === 'NEW' ? 'HANDLED' : 'NEW'
    try {
      await updateRequestStatus(item.id, nextStatus)
      setData((current) => current && ({ ...current, items: current.items.map((row) => row.id === item.id ? { ...row, status: nextStatus } : row) }))
      setNotice(copy.statusSaved)
    } catch (failure) {
      if (!(failure instanceof SessionExpiredError)) setError(errorMessage(failure))
    } finally {
      pendingIds.current.delete(item.id)
      setPending((current) => ({ ...current, [item.id]: false }))
    }
  }

  const working = Object.values(pending).some(Boolean)
  return (
    <div className="admin-requests">
      <p className="admin-intro">{copy.requestsIntro}</p>
      <p className="admin-notice" role="status" aria-live="polite">{loading ? copy.loading : notice}</p>
      {error && <div className="admin-error" role="alert"><p>{error}</p><button type="button" className="button button-text" onClick={() => loadPage(page)}>{copy.retry}</button></div>}
      {!loading && data && <>
        {data.items.length === 0 ? <p className="admin-empty">{copy.noRequests}</p> : <div className="request-list">
          {data.items.map((item) => {
            const date = createdDate(item.createdAt)
            const phone = String(item.phone ?? '').replace(/\D/g, '')
            const whatsapp = phone.startsWith('0') ? `972${phone.slice(1)}` : phone
            const work = siteContent.form.workTypes.find((option) => option.value === item.workType)?.label
            return (
              <article className="request-card admin-surface" key={item.id}>
                <header className="request-card-heading">
                  <div><span className="admin-kicker">#{item.id} · {work}</span><h2>{item.fullName}</h2></div>
                  <span className={`request-badge ${item.status === 'NEW' ? 'is-new' : 'is-handled'}`}>{item.status === 'NEW' ? copy.newStatus : copy.handledStatus}</span>
                </header>
                {date && <time className="request-time" dateTime={date.toISOString()}>{dateFormatter.format(date)}</time>}
                <dl className="request-details">
                  {item.city && <div><dt>{copy.city}</dt><dd>{item.city}</dd></div>}
                  {item.email && <div><dt>{copy.email}</dt><dd><a href={`mailto:${encodeURIComponent(item.email)}`}>{item.email}</a></dd></div>}
                  {item.description && <div><dt>{copy.description}</dt><dd className="request-description">{item.description}</dd></div>}
                </dl>
                <footer className="request-actions">
                  <a className="button button-text" href={`tel:${phone}`} aria-label={`${copy.call}: ${item.fullName}`}><span>{copy.call}</span><bdi>{item.phone}</bdi></a>
                  <a className="button button-text" href={`https://wa.me/${whatsapp}`} target="_blank" rel="noreferrer" aria-label={`${copy.whatsapp}: ${item.fullName}`}>{copy.whatsapp}</a>
                  <button className="button button-primary" type="button" disabled={Boolean(pending[item.id])} onClick={() => toggle(item)} aria-busy={Boolean(pending[item.id])}>
                    {pending[item.id] ? copy.saving : item.status === 'NEW' ? copy.markHandled : copy.markNew}
                  </button>
                </footer>
              </article>
            )
          })}
        </div>}
        <nav className="admin-pagination" aria-label={copy.pagination}>
          <button type="button" className="button button-text" disabled={page === 0 || working} onClick={() => loadPage(page - 1)}>{copy.previous}</button>
          <span role="status">{copy.page} {data.page + 1} {copy.of} {Math.max(data.totalPages, 1)} · {data.totalElements} {copy.requestsCount}</span>
          <button type="button" className="button button-text" disabled={page + 1 >= data.totalPages || working} onClick={() => loadPage(page + 1)}>{copy.next}</button>
        </nav>
      </>}
    </div>
  )
}
