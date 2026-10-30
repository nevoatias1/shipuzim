import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { getToken, login, logout, onSessionExpired } from '../api/auth.js'
import { errorMessage } from '../api/errors.js'
import siteContent from '../content/siteContent.js'
import RequestsPanel from './RequestsPanel.jsx'
import GalleryPanel from './GalleryPanel.jsx'
import './admin.css'

const copy = siteContent.admin

function LoginForm({ message, onLogin }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const submitting = useRef(false)

  async function submit(event) {
    event.preventDefault()
    if (submitting.current) return
    if (!username.trim() || !password) {
      setError(copy.loginRequired)
      event.currentTarget.elements.namedItem(!username.trim() ? 'username' : 'password').focus()
      return
    }
    submitting.current = true
    setBusy(true)
    setError('')
    try {
      await login(username.trim(), password)
      setPassword('')
      onLogin()
    } catch (failure) {
      setError(failure.status === 403 || failure.status === 401 ? copy.invalidCredentials : errorMessage(failure))
    } finally {
      submitting.current = false
      setBusy(false)
    }
  }

  return (
    <section className="admin-login admin-surface" aria-labelledby="login-title">
      <p className="eyebrow"><span aria-hidden="true" />{copy.eyebrow}</p>
      <h1 id="login-title">{copy.loginTitle}</h1>
      <p className="admin-intro">{copy.loginIntro}</p>
      {message && <p className="admin-notice" role="status">{message}</p>}
      <form onSubmit={submit} noValidate>
        <fieldset className="admin-fieldset" disabled={busy}>
          <div className="form-field">
            <label htmlFor="admin-username">{copy.username}</label>
            <input id="admin-username" name="username" autoComplete="username" dir="auto" value={username} onChange={(event) => setUsername(event.target.value)} required autoFocus />
          </div>
          <div className="form-field">
            <label htmlFor="admin-password">{copy.password}</label>
            <input id="admin-password" name="password" type="password" autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)} required />
          </div>
          <button className="button button-primary" type="submit" aria-busy={busy}>{busy ? copy.loggingIn : copy.login}</button>
        </fieldset>
        <p className="admin-error" role="alert" aria-live="assertive">{error}</p>
      </form>
    </section>
  )
}

export default function AdminPage() {
  const [authenticated, setAuthenticated] = useState(() => Boolean(getToken()))
  const [message, setMessage] = useState('')
  const [tab, setTab] = useState('requests')
  const tabRefs = useRef({})

  useEffect(() => {
    const existing = document.querySelector('meta[name="robots"]')
    const meta = existing || document.createElement('meta')
    const previous = existing?.getAttribute('content')
    const previousTitle = document.title
    meta.name = 'robots'
    meta.content = 'noindex'
    if (!existing) document.head.append(meta)
    document.title = `${copy.title} | ${siteContent.business.name}`
    return () => {
      if (existing) {
        if (previous === null) meta.removeAttribute('content')
        else meta.setAttribute('content', previous)
      } else meta.remove()
      document.title = previousTitle
    }
  }, [])

  useEffect(() => onSessionExpired(() => {
    setAuthenticated(false)
    setMessage(copy.sessionExpired)
  }), [])

  function signOut() {
    logout()
    setAuthenticated(false)
    setMessage(copy.loggedOut)
  }

  function changeTab(event) {
    const names = ['requests', 'gallery']
    const index = names.indexOf(tab)
    let next
    if (event.key === 'ArrowLeft') next = names[(index + 1) % names.length]
    if (event.key === 'ArrowRight') next = names[(index + names.length - 1) % names.length]
    if (event.key === 'Home') next = names[0]
    if (event.key === 'End') next = names.at(-1)
    if (next) {
      event.preventDefault()
      setTab(next)
      tabRefs.current[next]?.focus()
    }
  }

  return (
    <main className="admin-page">
      <div className="wrap">
        <header className="admin-header">
          <Link className="wordmark" to="/" aria-label={copy.backHome}>
            <span className="wordmark-symbol" aria-hidden="true">{siteContent.business.mark}</span><span>{siteContent.business.name}</span>
          </Link>
          {authenticated ? <button className="button button-text" type="button" onClick={signOut}>{copy.logout}</button>
            : <Link className="admin-home-link" to="/">{copy.backHome}</Link>}
        </header>
        {authenticated ? <>
          <div className="admin-heading"><p className="eyebrow"><span aria-hidden="true" />{copy.eyebrow}</p><h1 id="admin-title">{copy.title}</h1></div>
          <div className="admin-tabs" role="tablist" aria-label={copy.tabsLabel} onKeyDown={changeTab}>
            {['requests', 'gallery'].map((name) => (
              <button key={name} ref={(element) => { tabRefs.current[name] = element }} type="button" role="tab" id={`tab-${name}`}
                aria-selected={tab === name} aria-controls={`panel-${name}`} tabIndex={tab === name ? 0 : -1} onClick={() => setTab(name)}>
                {name === 'requests' ? copy.requestsTab : copy.galleryTab}
              </button>
            ))}
          </div>
          <section id={`panel-${tab}`} role="tabpanel" aria-labelledby={`tab-${tab}`} tabIndex={0}>
            {tab === 'requests' ? <RequestsPanel /> : <GalleryPanel />}
          </section>
        </> : <LoginForm message={message} onLogin={() => { setAuthenticated(true); setMessage(''); setTab('requests') }} />}
      </div>
    </main>
  )
}
