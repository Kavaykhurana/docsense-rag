import { useEffect, useState } from 'react'
import { beginGoogleLogin } from '../api'

// Phase 1 landing page: product identity, RAG + privacy explanation, and the
// Google sign-in entry point. (The polished full UI lands in a later phase.)
export default function Landing() {
  const [authError, setAuthError] = useState(false)

  useEffect(() => {
    const params = new URLSearchParams(window.location.search)
    if (params.get('auth_error') === '1') {
      setAuthError(true)
      params.delete('auth_error')
      const q = params.toString()
      window.history.replaceState({}, '', window.location.pathname + (q ? `?${q}` : ''))
    }
  }, [])

  return (
    <div className="min-h-screen flex flex-col">
      <header className="flex items-center justify-between px-6 py-4 border-b border-slate-200 bg-white">
        <div className="text-lg font-semibold tracking-tight text-slate-900">DocSense</div>
        <button
          onClick={beginGoogleLogin}
          className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
        >
          Sign in
        </button>
      </header>

      <main className="flex-1 flex items-center justify-center px-6">
        <div className="max-w-2xl text-center space-y-6">
          <h1 className="text-4xl font-bold tracking-tight text-slate-900">
            Your private AI document assistant
          </h1>
          <p className="text-lg text-slate-600">
            Upload documents, then ask natural-language questions and get grounded answers with
            source citations — powered by Retrieval-Augmented Generation (RAG).
          </p>
          <p className="text-sm text-slate-500">
            Your original files stay on your computer. The server keeps only the text chunks and
            embeddings needed to answer your questions.
          </p>

          {authError && (
            <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
              Sign-in failed. Please try again.
            </div>
          )}

          <button
            onClick={beginGoogleLogin}
            className="inline-flex items-center justify-center gap-2 rounded-xl bg-slate-900 px-6 py-3 text-base font-semibold text-white hover:bg-slate-800"
          >
            Continue with Google
          </button>
        </div>
      </main>

      <footer className="px-6 py-4 text-center text-xs text-slate-400 border-t border-slate-200 bg-white">
        RAG pipeline: extract → chunk → embed → pgvector retrieval → grounded Gemini answers.
      </footer>
    </div>
  )
}
