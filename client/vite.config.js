import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => {
  const siteIsLive = loadEnv(mode, '.', 'VITE_').VITE_SITE_LIVE === 'true'

  return {
    plugins: [react(), {
      name: 'site-indexing',
      transformIndexHtml() {
        return siteIsLive ? [] : [{
          tag: 'meta',
          attrs: { name: 'robots', content: 'noindex' },
          injectTo: 'head',
        }]
      },
    }],
    server: {
      proxy: {
        '/api': 'http://localhost:8080',
        '/authenticate': 'http://localhost:8080',
        '/auth': 'http://localhost:8080',
        '/users': 'http://localhost:8080',
        '/admin': {
          target: 'http://localhost:8080',
          bypass(request) {
            if (request.headers.accept?.includes('text/html')
                && request.url?.split('?')[0].replace(/\/$/, '') === '/admin') return '/index.html'
          },
        },
        '/uploads': 'http://localhost:8080',
      },
    },
  }
})
