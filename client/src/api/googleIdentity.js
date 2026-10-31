export const googleClientId = (import.meta.env.VITE_GOOGLE_CLIENT_ID ?? '').trim()

let scriptPromise
let initializedSdk
let credentialListener

export function loadGoogleIdentity() {
  if (window.google?.accounts?.id) return Promise.resolve(window.google.accounts.id)
  if (scriptPromise) return scriptPromise
  scriptPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = 'https://accounts.google.com/gsi/client?hl=he'
    script.async = true
    const timeout = window.setTimeout(fail, 15000)
    function fail() {
      window.clearTimeout(timeout)
      script.onload = null; script.onerror = null
      script.remove(); scriptPromise = undefined
      reject(new Error('Google sign-in could not load'))
    }
    script.onerror = fail
    script.onload = () => {
      window.clearTimeout(timeout)
      if (!window.google?.accounts?.id) { fail(); return }
      resolve(window.google.accounts.id)
    }
    document.head.append(script)
  })
  return scriptPromise
}

export function renderGoogleButton(sdk, container, listener) {
  credentialListener = listener
  if (initializedSdk !== sdk) {
    sdk.initialize({ client_id: googleClientId, auto_select: false, callback: (response) => credentialListener?.(response) })
    initializedSdk = sdk
  }
  let previousWidth = 0
  function render() {
    const width = Math.floor(Math.min(320, container.clientWidth))
    if (width < 120 || width === previousWidth) return
    previousWidth = width
    container.replaceChildren()
    sdk.renderButton(container, { type: 'standard', theme: 'outline', size: 'large', text: 'signin_with', locale: 'he', width })
  }
  render()
  const observer = typeof ResizeObserver === 'function' ? new ResizeObserver(render) : null
  observer?.observe(container)
  return () => {
    observer?.disconnect()
    if (credentialListener === listener) credentialListener = undefined
    container.replaceChildren()
  }
}
