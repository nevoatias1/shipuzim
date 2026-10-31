import { useEffect, useRef, useState } from 'react'
import { deleteAdminReview, getAdminReviews } from '../api/admin.js'
import { SessionExpiredError } from '../api/auth.js'
import { errorMessage } from '../api/errors.js'
import ReviewCard from '../components/ReviewCard.jsx'
import siteContent from '../content/siteContent.js'

const copy = siteContent.admin

export default function ReviewsPanel() {
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
    getAdminReviews(page, controller.signal).then((result) => {
      if (!controller.signal.aborted) { setData(result); setLoading(false) }
    }).catch((failure) => {
      if (!controller.signal.aborted && !(failure instanceof SessionExpiredError)) {
        setError(errorMessage(failure)); setLoading(false)
      }
    })
    return () => controller.abort()
  }, [page, revision])

  function loadPage(next, message = '') {
    setData(null); setLoading(true); setError(''); setNotice(message)
    setPage(next); setRevision((current) => current + 1)
  }

  async function remove(item) {
    if (pendingIds.current.has(item.id) || !window.confirm(copy.confirmDeleteReview)) return
    pendingIds.current.add(item.id); setPending((current) => ({ ...current, [item.id]: true }))
    setError(''); setNotice('')
    try {
      await deleteAdminReview(item.id)
      loadPage(data.items.length === 1 && page > 0 ? page - 1 : page, copy.reviewDeleted)
    } catch (failure) {
      if (!(failure instanceof SessionExpiredError)) setError(errorMessage(failure))
    } finally {
      pendingIds.current.delete(item.id); setPending((current) => ({ ...current, [item.id]: false }))
    }
  }

  const working = Object.values(pending).some(Boolean)
  return <div className="admin-reviews">
    <p className="admin-intro">{copy.reviewsIntro}</p>
    <p className="admin-notice" role="status" aria-live="polite">{loading ? copy.loading : notice}</p>
    {error && <div className="admin-error" role="alert"><p>{error}</p>
      <button type="button" className="button button-text" onClick={() => loadPage(page)}>{copy.retry}</button></div>}
    {!loading && data && <>
      {data.items.length === 0 ? <p className="admin-empty">{copy.noReviews}</p> : <div className="admin-review-grid">
        {data.items.map((item) => <ReviewCard key={item.id} item={item}>
          <p className="admin-review-email"><span>{copy.email}: </span><bdi>{item.authorEmail}</bdi></p>
          <button type="button" className="button button-danger" disabled={working} aria-busy={Boolean(pending[item.id])}
            aria-label={`${copy.deleteReview}: ${item.displayName}`} onClick={() => remove(item)}>
            {pending[item.id] ? copy.deletingReview : copy.deleteReview}
          </button>
        </ReviewCard>)}
      </div>}
      <nav className="admin-pagination" aria-label={copy.reviewsPagination}>
        <button type="button" className="button button-text" disabled={page === 0 || working} onClick={() => loadPage(page - 1)}>{copy.previous}</button>
        <span role="status">{copy.page} {data.page + 1} {copy.of} {Math.max(data.totalPages, 1)} · {data.totalElements} {siteContent.reviews.count}</span>
        <button type="button" className="button button-text" disabled={page + 1 >= data.totalPages || working} onClick={() => loadPage(page + 1)}>{copy.next}</button>
      </nav>
    </>}
  </div>
}
