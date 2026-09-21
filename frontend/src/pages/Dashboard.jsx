import { useEffect, useState } from 'react'
import { getMe, beginGoogleLogin, logout } from '../api'

// Minimal authenticated dashboard used to verify the Google OAuth2 session in
// Phase 1. The full document/chat experience is built in a later phase.
export default function Dashboard() {
  const [user, setUser] = useState(null)
  const [state, setState] = useState('loading') // loading | authed | anon | error

  useEffect(() => {
    getMe()
      .then((u) => {
        setUser(u)
        setState('authed')
      })
      .catch((err) => {
        if (err?.response?.status === 401) setState('anon')
        else setState('error')
      })
  }, [])

  async function onLogout() {
    try {
      await logout()
    } finally {
      window.location.href = '/'
    }
  }

  return (
    <div className="min-h-screen">
      <header className="flex items-center justify-between px-6 py-4 border-b border-slate-200 bg-white">
        <div className="text-lg font-semibold tracking-tight text-slate-900">DocSense</div>
        {state === 'authed' && (
          <button
            onClick={onLogout}
            className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            Sign out
          </button>
        )}
      </header>

      <main className="mx-auto max-w-3xl px-6 py-10">
        {state === 'loading' && <p className="text-slate-500">Checking your session…</p>}

        {state === 'anon' && (
          <div className="space-y-4">
            <p className="text-slate-600">You are not signed in.</p>
            <button
              onClick={beginGoogleLogin}
              className="rounded-xl bg-slate-900 px-5 py-2.5 text-sm font-semibold text-white hover:bg-slate-800"
            >
              Continue with Google
            </button>
          </div>
        )}

        {state === 'error' && (
          <p className="text-red-600">Could not reach the server. Please try again.</p>
        )}

        {state === 'authed' && user && (
          <div className="space-y-4">
            <h1 className="text-2xl font-semibold text-slate-900">Signed in</h1>
            <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
              <div className="flex items-center gap-4">
                {user.picture && (
                  <img src={user.picture} alt="" className="h-12 w-12 rounded-full" referrerPolicy="no-referrer" />
                )}
                <div>
                  <p className="font-medium text-slate-900">{user.name || 'User'}</p>
                  <p className="text-sm text-slate-500">{user.email}</p>
                </div>
              </div>
            </div>
            <p className="text-sm text-slate-500">
              Document upload and chat will appear here in the next phases.
            </p>
          </div>
        )}
      </main>
    </div>
  )
}
