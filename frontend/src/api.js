import axios from 'axios'

// Same-origin requests; in dev the Vite proxy forwards /api and OAuth2 paths to
// the Spring Boot backend (see vite.config.js). credentials:'include' carries the
// session cookie established by Google OAuth2 login.
const api = axios.create({
  baseURL: '/api',
  withCredentials: true,
})

export function getMe() {
  return api.get('/auth/me').then((r) => r.data)
}

export function logout() {
  return api.post('/auth/logout')
}

// Kick off the Google OAuth2 flow (browser navigation, not XHR).
export function beginGoogleLogin() {
  window.location.href = '/oauth2/authorization/google'
}

export default api
