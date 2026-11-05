import { useEffect, useRef, useState } from 'react'
import { Link, Route, Routes, useLocation } from 'react-router-dom'
import { submitContactRequest } from './api/contact.js'
import AdminPage from './admin/AdminPage.jsx'
import HomeGallery from './components/HomeGallery.jsx'
import Reviews from './components/Reviews.jsx'
import siteContent from './content/siteContent.js'
import { getServiceImages } from './api/serviceImages.js'
import { imageUrl } from './api/http.js'
import heroWide from './assets/hero-1920.webp'
import heroNarrow from './assets/hero-960.webp'

const phonePattern = /^(?:0|\+972)[ -]?(?:5\d(?:[ -]?\d){7}|[23489](?:[ -]?\d){7})$/

function ArrowIcon({ diagonal = false }) {
  return (
    <svg aria-hidden="true" viewBox="0 0 24 24" className="icon-arrow">
      {diagonal
        ? <path d="M7 17 17 7M8 7h9v9" />
        : <path d="M19 12H5m0 0 6 6m-6-6 6-6" />}
    </svg>
  )
}

function WhatsAppIcon() {
  return (
    <svg aria-hidden="true" viewBox="0 0 24 24" className="icon-whatsapp">
      <path d="M20.4 11.8a8.4 8.4 0 0 1-12.4 7.4L4 20l.9-3.8a8.4 8.4 0 1 1 15.5-4.4Z" />
      <path d="M9 8.2c.2-.5.4-.5.7-.5h.5c.2 0 .4.1.5.4l.8 1.8c.1.3.1.4-.1.6l-.6.7c-.2.2-.2.3-.1.5.4.7 1.1 1.4 1.8 1.8.2.1.4.1.5-.1l.7-.8c.2-.2.3-.2.6-.1l1.7.8c.3.1.4.3.4.5 0 .3-.2 1.1-.7 1.5-.5.5-1.2.7-1.9.6-1.1-.2-2.4-.9-3.7-2-1.2-1.1-2.1-2.5-2.3-3.6-.2-.7.1-1.4.6-2.1Z" />
    </svg>
  )
}

function SiteHeader() {
  const [menuOpen, setMenuOpen] = useState(false)
  const headerRef = useRef(null)
  const menuButtonRef = useRef(null)

  useEffect(() => {
    if (!menuOpen) return undefined

    function closeOnEscape(event) {
      if (event.key === 'Escape') {
        setMenuOpen(false)
        menuButtonRef.current?.focus()
      }
    }

    function closeOnOutsideClick(event) {
      if (!headerRef.current?.contains(event.target)) setMenuOpen(false)
    }

    function closeOnDesktopResize() {
      if (window.innerWidth >= 900) setMenuOpen(false)
    }

    document.addEventListener('keydown', closeOnEscape)
    document.addEventListener('pointerdown', closeOnOutsideClick)
    window.addEventListener('resize', closeOnDesktopResize)
    return () => {
      document.removeEventListener('keydown', closeOnEscape)
      document.removeEventListener('pointerdown', closeOnOutsideClick)
      window.removeEventListener('resize', closeOnDesktopResize)
    }
  }, [menuOpen])

  function closeMenu() {
    setMenuOpen(false)
  }

  return (
    <header className="site-header" ref={headerRef}>
      <div className="header-main wrap">
        <Link className="wordmark" to="/" aria-label={siteContent.business.name}>
          <span className="wordmark-symbol" aria-hidden="true">{siteContent.business.mark}</span>
          <span>{siteContent.business.name}</span>
        </Link>
        <a className="button button-small header-cta" href="#contact">
          {siteContent.headerCta}
          <ArrowIcon />
        </a>
        <button
          className="mobile-menu-toggle"
          type="button"
          aria-expanded={menuOpen}
          aria-controls="main-navigation"
          aria-label={menuOpen ? 'סגירת תפריט' : 'פתיחת תפריט'}
          onClick={() => setMenuOpen((open) => !open)}
          ref={menuButtonRef}
        >
          {menuOpen ? '✕' : '☰'}
        </button>
      </div>
      <nav id="main-navigation" className={`site-nav wrap${menuOpen ? ' is-open' : ''}`} aria-label={siteContent.accessibility.mainNavigation}>
        {siteContent.navigation.map((item) => <a key={item.href} href={item.href} onClick={closeMenu}>{item.label}</a>)}
        <a className="button button-small button-primary mobile-menu-cta" href="#contact" onClick={closeMenu}>
          {siteContent.headerCta}
          <ArrowIcon />
        </a>
      </nav>
    </header>
  )
}

function SectionHeading({ id, eyebrow, title, intro, light = false }) {
  return (
    <div className={`section-heading${light ? ' section-heading-light' : ''}`}>
      <p className="eyebrow"><span aria-hidden="true" />{eyebrow}</p>
      <h2 id={id}>{title}</h2>
      {intro && <p className="section-intro">{intro}</p>}
    </div>
  )
}

function Hero() {
  return (
    <section className="hero" aria-labelledby="hero-title">
      <img
        className="hero-background"
        src={heroWide}
        srcSet={`${heroNarrow} 960w, ${heroWide} 1920w`}
        sizes="(max-width: 600px) 100vw, 100vw"
        alt="חזית בית לפני ואחרי שיפוץ – חיפוי וצביעה"
        loading="eager"
        fetchPriority="high"
      />
      <div className="hero-layout wrap">
        <div className="hero-copy">
          <p className="eyebrow"><span aria-hidden="true" />{siteContent.hero.eyebrow}</p>
          <h1 id="hero-title">{siteContent.hero.title.split('\n').map((line) => <span key={line}>{line}</span>)}</h1>
          <p className="hero-description">{siteContent.hero.description}</p>
          <div className="hero-actions">
            <a className="button button-primary" href="#contact">{siteContent.hero.primaryCta}<ArrowIcon /></a>
            <a className="button button-text" href={siteContent.whatsappUrl} target="_blank" rel="noreferrer">
              <WhatsAppIcon />{siteContent.hero.secondaryCta}
            </a>
          </div>
          <p className="hero-note"><span aria-hidden="true">✳</span>{siteContent.hero.note}</p>
        </div>
      </div>
      <a className="hero-scroll" href="#services"><span aria-hidden="true" />{siteContent.hero.scrollLabel}</a>
    </section>
  )
}

function Services() {
  const [images, setImages] = useState({})
  useEffect(() => {
    const controller = new AbortController()
    getServiceImages(controller.signal).then((result) => {
      if (!controller.signal.aborted) setImages(result)
    }).catch(() => { /* Keep the original cards when images are unavailable. */ })
    return () => controller.abort()
  }, [])

  return (
    <section className="section services-section" id="services" aria-labelledby="services-title">
      <div className="wrap">
        <SectionHeading id="services-title" {...siteContent.services} />
        <div className="services-grid">
          {siteContent.services.items.map((service) => (
            <article className="service-card" key={service.key}>
              {images[service.key] && <img className="service-card-image" src={imageUrl(images[service.key], 600)} alt="" loading="lazy" decoding="async" width="600" height="400" />}
              <div className="service-card-top"><span className="service-number">{service.number}</span><span className="service-mark" aria-hidden="true">↙</span></div>
              <h3>{service.title}</h3>
              <p>{service.description}</p>
              <a className="text-link" href="#contact" aria-label={`${siteContent.services.cardCta} ${service.title}`}>
                {siteContent.services.cardCta}<ArrowIcon />
              </a>
            </article>
          ))}
        </div>
      </div>
    </section>
  )
}

function Process() {
  return (
    <section className="section process-section" id="process" aria-labelledby="process-title">
      <div className="wrap process-layout">
        <SectionHeading id="process-title" {...siteContent.process} />
        <ol className="process-list">
          {siteContent.process.steps.map((step) => (
            <li className="process-step" key={step.number}>
              <span className="process-number">{step.number}</span>
              <div><h3>{step.title}</h3><p>{step.description}</p></div>
              <ArrowIcon diagonal />
            </li>
          ))}
        </ol>
      </div>
    </section>
  )
}

function ContactForm() {
  const initialValues = { fullName: '', phone: '', city: '', workType: '', description: '', website: '' }
  const [values, setValues] = useState(initialValues)
  const [errors, setErrors] = useState({})
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [feedback, setFeedback] = useState(null)
  const fieldRefs = useRef({})
  const submittingRef = useRef(false)

  function focusFirstError(nextErrors) {
    const fieldOrder = ['fullName', 'phone', 'city', 'workType', 'description']
    const firstInvalidField = fieldOrder.find((field) => nextErrors[field] && fieldRefs.current[field])
    if (firstInvalidField) window.requestAnimationFrame(() => fieldRefs.current[firstInvalidField]?.focus())
  }

  function updateValue(event) {
    const { name, value } = event.target
    setValues((current) => ({ ...current, [name]: value }))
    setErrors((current) => ({ ...current, [name]: '' }))
    setFeedback(null)
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (submittingRef.current) return

    const nextErrors = {}
    const fullName = values.fullName.trim()
    if (fullName.length < 2 || fullName.length > 80) nextErrors.fullName = siteContent.form.errors.name
    if (!phonePattern.test(values.phone.trim())) nextErrors.phone = siteContent.form.errors.phone
    if (values.city.trim().length > 60) nextErrors.city = siteContent.form.errors.city
    if (!siteContent.form.workTypes.some((option) => option.value === values.workType)) nextErrors.workType = siteContent.form.errors.workType
    if (values.description.length > 1000) nextErrors.description = siteContent.form.errors.description
    setErrors(nextErrors)
    setFeedback(null)
    if (Object.keys(nextErrors).length > 0) {
      focusFirstError(nextErrors)
      return
    }

    submittingRef.current = true
    setIsSubmitting(true)
    try {
      await submitContactRequest({
        fullName: values.fullName,
        phone: values.phone,
        city: values.city,
        workType: values.workType,
        description: values.description,
        website: values.website,
      })
      setValues(initialValues)
      setErrors({})
      setFeedback({ type: 'success' })
    } catch (error) {
      if (error.status === 400 && error.code === 'VALIDATION_FAILED') {
        const serverErrors = error.fields ?? {}
        setErrors(serverErrors)
        if (Object.keys(serverErrors).length > 0) {
          focusFirstError(serverErrors)
        } else {
          setFeedback({ type: 'generalError' })
        }
      } else if (error.status === 429) {
        setFeedback({ type: 'rateLimited' })
      } else {
        setFeedback({ type: 'generalError' })
      }
    } finally {
      submittingRef.current = false
      setIsSubmitting(false)
    }
  }

  function errorProps(name) {
    return errors[name]
      ? { 'aria-invalid': 'true', 'aria-describedby': `${name}-error` }
      : { 'aria-invalid': 'false' }
  }

  return (
    <section className="section contact-section" id="contact" aria-labelledby="contact-title">
      <div className="contact-decoration" aria-hidden="true"><span /><span /><span /></div>
      <div className="wrap contact-layout">
        <div className="contact-intro">
          <SectionHeading id="contact-title" {...siteContent.form} light />
          <p className="contact-aside"><span aria-hidden="true">✳</span>{siteContent.form.privacyNote}</p>
          <div className="contact-aside-line" />
          <p className="contact-aside-small">{siteContent.form.responseNote}</p>
        </div>
        <form className="contact-form" onSubmit={handleSubmit} noValidate>
          <div className="honeypot" aria-hidden="true">
            <label htmlFor="website">{siteContent.form.honeypot}</label>
            <input id="website" name="website" type="text" aria-hidden="true" tabIndex={-1} autoComplete="off" value={values.website} onChange={updateValue} />
          </div>
          <div className="form-row">
            <div className="form-field">
              <label htmlFor="fullName">{siteContent.form.fullName}<span aria-hidden="true">*</span></label>
              <input ref={(element) => { fieldRefs.current.fullName = element }} id="fullName" name="fullName" type="text" autoComplete="name" required placeholder={siteContent.form.fullNamePlaceholder} value={values.fullName} onChange={updateValue} {...errorProps('fullName')} />
              {errors.fullName && <span className="field-error" id="fullName-error" aria-live="polite">{errors.fullName}</span>}
            </div>
            <div className="form-field">
              <label htmlFor="phone">{siteContent.form.phone}<span aria-hidden="true">*</span></label>
              <input ref={(element) => { fieldRefs.current.phone = element }} id="phone" name="phone" type="tel" inputMode="tel" autoComplete="tel" required placeholder={siteContent.form.phonePlaceholder} value={values.phone} onChange={updateValue} {...errorProps('phone')} />
              {errors.phone && <span className="field-error" id="phone-error" aria-live="polite">{errors.phone}</span>}
            </div>
          </div>
          <div className="form-row">
            <div className="form-field">
              <label htmlFor="city">{siteContent.form.city}</label>
              <input ref={(element) => { fieldRefs.current.city = element }} id="city" name="city" type="text" autoComplete="address-level2" maxLength={60} placeholder={siteContent.form.cityPlaceholder} value={values.city} onChange={updateValue} {...errorProps('city')} />
              {errors.city && <span className="field-error" id="city-error" aria-live="polite">{errors.city}</span>}
            </div>
            <div className="form-field">
              <label htmlFor="workType">{siteContent.form.workType}<span aria-hidden="true">*</span></label>
              <select ref={(element) => { fieldRefs.current.workType = element }} id="workType" name="workType" required value={values.workType} onChange={updateValue} {...errorProps('workType')}>
                <option value="">{siteContent.form.workTypePlaceholder}</option>
                {siteContent.form.workTypes.map((option) => <option value={option.value} key={option.value}>{option.label}</option>)}
              </select>
              {errors.workType && <span className="field-error" id="workType-error" aria-live="polite">{errors.workType}</span>}
            </div>
          </div>
          <div className="form-field">
            <label htmlFor="description">{siteContent.form.description}</label>
            <textarea ref={(element) => { fieldRefs.current.description = element }} id="description" name="description" rows="4" maxLength={1000} placeholder={siteContent.form.descriptionPlaceholder} value={values.description} onChange={updateValue} {...errorProps('description')} />
            {errors.description && <span className="field-error" id="description-error" aria-live="polite">{errors.description}</span>}
            <span className="character-count">{values.description.length} / 1,000</span>
          </div>
          <div className="form-submit-row">
            <button className="button button-primary" type="submit" disabled={isSubmitting} aria-busy={isSubmitting}>
              {isSubmitting ? siteContent.form.sending : siteContent.form.submit}<ArrowIcon />
            </button>
            <span>{siteContent.form.requiredNote}</span>
          </div>
          {feedback?.type === 'success' && <p className="form-success" role="status" aria-live="polite">{siteContent.form.success}</p>}
          {feedback?.type === 'rateLimited' && <p className="form-message-error" role="alert" aria-live="assertive">{siteContent.form.rateLimited}</p>}
          {feedback?.type === 'generalError' && (
            <p className="form-message-error" role="alert" aria-live="assertive">
              {siteContent.form.generalError}{' '}
              <a href={siteContent.whatsappUrl}>{siteContent.form.whatsappFallback}</a>
            </p>
          )}
        </form>
      </div>
    </section>
  )
}

function SiteFooter() {
  return (
    <footer className="site-footer">
      <div className="wrap footer-main">
        <div className="footer-brand">
          <Link className="wordmark wordmark-footer" to="/">
            <span className="wordmark-symbol" aria-hidden="true">{siteContent.business.mark}</span><span>{siteContent.business.name}</span>
          </Link>
          <p>{siteContent.footer.message}</p>
        </div>
        <div className="footer-column">
          <h2>{siteContent.footer.contactTitle}</h2>
          <span className="footer-detail-label">{siteContent.footer.phone}</span>
          {siteContent.contact.phoneHref
            ? <a href={`tel:${siteContent.contact.phoneHref}`}>{siteContent.contact.phone}</a>
            : <span>{siteContent.contact.phone}</span>}
          <span className="footer-detail-label">{siteContent.footer.email}</span>
          {siteContent.contact.emailHref
            ? <a href={`mailto:${siteContent.contact.emailHref}`}>{siteContent.contact.email}</a>
            : <span>{siteContent.contact.email}</span>}
          <span className="footer-detail-label">{siteContent.footer.whatsapp}</span>
          <a href={siteContent.whatsappUrl} target="_blank" rel="noreferrer">{siteContent.contact.whatsappLabel}</a>
        </div>
        <div className="footer-column footer-areas">
          <h2>{siteContent.footer.areas}</h2>
          <ul>{siteContent.serviceAreas.map((area) => <li key={area}>{area}</li>)}</ul>
        </div>
        <div className="footer-column footer-links">
          <h2>{siteContent.footer.information}</h2>
          <Link to="/privacy">{siteContent.footer.privacy}</Link>
          <Link to="/admin">{siteContent.footer.admin}</Link>
        </div>
      </div>
      <div className="footer-bottom wrap">
        <span>© {new Date().getFullYear()} {siteContent.business.name} · {siteContent.footer.rights}</span>
        <a href="#top">{siteContent.footer.backToTop}<ArrowIcon diagonal /></a>
      </div>
    </footer>
  )
}

function WhatsAppFloat() {
  return (
    <a className="whatsapp-float" href={siteContent.whatsappUrl} target="_blank" rel="noreferrer" aria-label={siteContent.whatsapp.ariaLabel}>
      <WhatsAppIcon /><span>{siteContent.whatsapp.floatLabel}</span>
    </a>
  )
}

function HomePage() {
  return (
    <>
      <SiteHeader />
      <main id="top">
        <Hero />
        <Services />
        <HomeGallery><SectionHeading id="gallery-title" {...siteContent.gallery} /></HomeGallery>
        <Process />
        <ContactForm />
        <Reviews><SectionHeading id="reviews-title" {...siteContent.reviews} /></Reviews>
        <WhatsAppFloat />
      </main>
      <SiteFooter />
    </>
  )
}

function PrivacyPage() {
  const page = siteContent.privacyPage
  return (
    <main className="placeholder-page wrap">
      <Link className="wordmark" to="/"><span className="wordmark-symbol" aria-hidden="true">{siteContent.business.mark}</span><span>{siteContent.business.name}</span></Link>
      <div className="placeholder-panel">
        <p className="eyebrow"><span aria-hidden="true" />{siteContent.placeholderEyebrow}</p>
        <h1>{page.title}</h1>
        <p>{page.intro}</p>
        <p className="privacy-draft">{page.draft}</p>
        <section className="privacy-google" aria-labelledby="privacy-google-title">
          <h2 id="privacy-google-title">{page.googleTitle}</h2>
          <p>{page.googleText}</p>
          <p>{page.removalText}</p>
        </section>
        <Link className="button button-primary" to="/">{page.back}<ArrowIcon /></Link>
      </div>
    </main>
  )
}

function NotFoundPage() {
  return (
    <main className="placeholder-page wrap">
      <Link className="wordmark" to="/"><span className="wordmark-symbol" aria-hidden="true">{siteContent.business.mark}</span><span>{siteContent.business.name}</span></Link>
      <div className="placeholder-panel">
        <p className="eyebrow"><span aria-hidden="true" />404</p>
        <h1>{siteContent.notFoundPage.title}</h1>
        <p>{siteContent.notFoundPage.intro}</p>
        <Link className="button button-primary" to="/">{siteContent.notFoundPage.back}<ArrowIcon /></Link>
      </div>
    </main>
  )
}

function App() {
  const { pathname } = useLocation()
  useEffect(() => {
    const route = pathname.replace(/\/+$/, '').toLowerCase() || '/'
    const title = route === '/' ? siteContent.business.tagline
      : route === '/privacy' ? siteContent.privacyPage.title
        : route === '/admin' ? siteContent.admin.title : siteContent.notFoundPage.title
    document.title = `${title} | ${siteContent.business.name}`
    document.querySelector('meta[property="og:title"]')?.setAttribute('content', document.title)
  }, [pathname])

  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/privacy" element={<PrivacyPage />} />
      <Route path="/admin" element={<AdminPage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}

export default App
