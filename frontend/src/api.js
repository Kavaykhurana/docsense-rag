import axios from 'axios'

// Base URLs. In development the Vite proxy forwards /api and OAuth2 paths to the
// Spring Boot backend (see vite.config.js), so relative defaults are used. In a
// split deployment (e.g. Render static frontend + separate backend service) the
// build injects VITE_API_BASE_URL and VITE_BACKEND_URL pointing at the backend.
const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api'
const BACKEND_BASE = import.meta.env.VITE_BACKEND_URL || ''

// credentials:'include' carries the session cookie established by Google OAuth2 login.
const api = axios.create({
  baseURL: API_BASE,
  withCredentials: true,
})

// ---- Auth -----------------------------------------------------------------

export function getMe() {
  return api.get('/auth/me').then((r) => r.data)
}

export function logout() {
  return api.post('/auth/logout')
}

// Kick off the Google OAuth2 flow (browser navigation, not XHR).
export function beginGoogleLogin() {
  window.location.href = `${BACKEND_BASE}/oauth2/authorization/google`
}

// ---- Documents ------------------------------------------------------------

export function listDocuments() {
  return api.get('/documents').then((r) => r.data)
}

export function uploadDocument(file, onProgress) {
  const form = new FormData()
  form.append('file', file)
  return api
    .post('/documents', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (e) => {
        if (onProgress && e.total) onProgress(Math.round((e.loaded * 100) / e.total))
      },
    })
    .then((r) => r.data)
}

export function getDocument(id) {
  return api.get(`/documents/${id}`).then((r) => r.data)
}

export function previewDocument(id) {
  return api.get(`/documents/${id}/preview`).then((r) => r.data)
}

export function deleteDocument(id) {
  return api.delete(`/documents/${id}`).then((r) => r.data)
}

// ---- Chat / RAG -----------------------------------------------------------

// Ask a grounded question. documentIds empty/null => search across all docs.
export function askChat({ question, conversationId = null, documentIds = [] }) {
  return api
    .post('/chat', {
      question,
      conversationId,
      documentIds: documentIds.length ? documentIds : null,
    })
    .then((r) => r.data)
}

export function listConversations() {
  return api.get('/conversations').then((r) => r.data)
}

export function getConversation(id) {
  return api.get(`/conversations/${id}`).then((r) => r.data)
}

export function renameConversation(id, title) {
  return api.patch(`/conversations/${id}`, { title }).then((r) => r.data)
}

export function deleteConversation(id) {
  return api.delete(`/conversations/${id}`).then((r) => r.data)
}

export default api
