import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// The frontend dev server proxies API + OAuth2 requests to the Spring Boot backend
// so the browser talks to a single origin during local development.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
      '/oauth2': 'http://localhost:8080',
      '/login': 'http://localhost:8080',
    },
  },
})
