import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './App.jsx'
import './index.css'
import siteContent from './content/siteContent.js'

document.title = `${siteContent.business.name} | ${siteContent.business.tagline}`
document.querySelector('meta[name="description"]')?.setAttribute('content', siteContent.business.description)
document.querySelector('meta[property="og:title"]')?.setAttribute('content', document.title)
document.querySelector('meta[property="og:description"]')?.setAttribute('content', siteContent.business.description)

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </StrictMode>,
)
