import { useCallback, useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { getReviews } from '../api/reviews.js'
import { getUserSession, logoutUser, onUserSessionExpired } from '../api/userAuth.js'
import siteContent from '../content/siteContent.js'
import GoogleSignIn from './GoogleSignIn.jsx'
import OwnReview from './OwnReview.jsx'
import ReviewCard from './ReviewCard.jsx'
import './reviews.css'

const copy = siteContent.reviews
const number = new Intl.NumberFormat('he-IL', { maximumFractionDigits: 1 })

export default function Reviews({ children }) {
  const section = useRef(null)
  const [near, setNear] = useState(false)
  const [session, setSession] = useState(getUserSession)
  const [sessionMessage, setSessionMessage] = useState('')
  const [query, setQuery] = useState({ page: 0, revision: 0 })
  const [list, setList] = useState({ items: [], data: null, loading: true, failed: false })

  useEffect(() => {
    if (typeof IntersectionObserver !== 'function') {
      const timeout = window.setTimeout(() => setNear(true), 0)
      return () => window.clearTimeout(timeout)
    }
    const observer = new IntersectionObserver((entries) => {
      if (entries.some((entry) => entry.isIntersecting)) { setNear(true); observer.disconnect() }
    }, { rootMargin: '400px 0px' })
    observer.observe(section.current)
    return () => observer.disconnect()
  }, [])

  useEffect(() => onUserSessionExpired(() => { setSession(null); setSessionMessage(copy.sessionExpired) }), [])

  useEffect(() => {
    const controller = new AbortController()
    getReviews(query.page, controller.signal).then((data) => {
      if (controller.signal.aborted) return
      setList((current) => {
        const items = query.page === 0 ? data.items : [...current.items, ...data.items]
        return { data, items: [...new Map(items.map((item) => [item.id, item])).values()], loading: false, failed: false }
      })
    }).catch(() => {
      if (!controller.signal.aborted) setList((current) => ({ ...current, loading: false, failed: true }))
    })
    return () => controller.abort()
  }, [query])

  const signedIn = useCallback((value) => { setSession(value); setSessionMessage('') }, [])

  function loadPage(page) {
    setList((current) => ({ ...current, loading: true, failed: false }))
    setQuery((current) => ({ page, revision: current.revision + 1 }))
  }

  function signOut() { logoutUser(); setSession(null); setSessionMessage(copy.loggedOut) }

  const placeholders = !list.loading && list.items.length === 0
  return <section className="section reviews-section" id="reviews" ref={section} aria-labelledby="reviews-title">
    <div className="wrap">
      {children}
      {list.data && !list.failed && <div className="reviews-summary" role="status">
        <span><strong>{number.format(list.data.average)}</strong> / 5 <span>{copy.average}</span></span>
        <span>{number.format(list.data.count)} {copy.count}</span>
      </div>}
      <p className="review-feedback" role="status" aria-live="polite">{list.loading ? copy.loading : ''}</p>
      {list.failed && <div className="review-feedback review-error" role="alert"><p>{copy.listFailed}</p>
        <button type="button" className="button button-text" onClick={() => loadPage(query.page)}>{copy.retry}</button></div>}
      <div className="reviews-grid">
        {list.items.map((item) => <ReviewCard key={item.id} item={item} />)}
        {placeholders && copy.items.map((review) => <article className="review-card" key={review.id}>
          <span className="review-stars" aria-label={copy.placeholderLabel}>★★★★★</span>
          <p>“{review.quote}”</p>
          <div className="review-author"><span aria-hidden="true">✳</span><span>{review.author}</span></div>
        </article>)}
      </div>
      {list.data && list.data.page + 1 < list.data.totalPages && !list.failed && <div className="reviews-more">
        <button type="button" className="button button-text" disabled={list.loading} onClick={() => loadPage(list.data.page + 1)}>{list.loading ? copy.loading : copy.loadMore}</button>
      </div>}
      <div className="reviews-account">
        <p className="review-feedback" role="status" aria-live="polite">{sessionMessage}</p>
        {session ? <>
          <div className="review-session"><p>{copy.signedInAs} <strong>{session.displayName}</strong></p>
            <button type="button" className="button button-text" onClick={signOut}>{copy.logout}</button></div>
          <OwnReview key={session.jwt} onChanged={() => loadPage(0)} />
        </> : <GoogleSignIn near={near} onSignedIn={signedIn} />}
        <Link className="review-privacy" to="/privacy">{copy.privacy}</Link>
      </div>
    </div>
  </section>
}
