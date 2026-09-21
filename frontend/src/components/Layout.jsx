import { useEffect, useState } from 'react'
import { NavLink, Outlet } from 'react-router-dom'
import { getMe, beginGoogleLogin, logout } from '../api'

// Authenticated application shell. Verifies the Google session, renders the
// top navigation, and exposes the active user to child routes via <Outlet context>.
export default function Layout() {
  const [user, setUser] = useState(null)
  const [state, setState] = useState('loading') // loading | authed | anon | error

  useEffect(() => {
    getMe()
      .then((u) => {
        setUser(u)
        setState('authed')
      })
      .catch((err) => {
        setState(err?.response?.status === 401 ? 'anon' : 'error')
      })
  }, [])

  if (state === 'loading') {
    return (
      <div className="flex min-h-screen items-center justify-center text-slate-500">
        Loading…
      </div>
    )
  }

  if (state === 'anon') {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-4 px-6 text-center">
        <h1 className="text-xl font-semibold text-slate-900">Please sign in</h1>
        <p className="text-slate-500">You need a Google account to access your documents.</p>
        <button
          onClick={beginGoogleLogin}
          className="rounded-xl bg-slate-900 px-5 py-2.5 text-sm font-semibold text-white hover:bg-slate-800"
        >
          Continue with Google
        </button>
      </div>
    )
  }

  if (state === 'error') {
    return (
      <div className="flex min-h-screen items-center justify-center px-6 text-red-600">
        Could not reach the server. Please refresh to try again.
      </div>
    )
  }

  async function onLogout() {
    try {
      await logout()
    } finally {
      window.location.href = '/'
    }
  }

  const navClass = ({ isActive }) =>
    `rounded-lg px-3 py-1.5 text-sm font-medium ${
      isActive ? 'bg-slate-100 text-slate-900' : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
    }`

  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-10 flex items-center justify-between border-b border-slate-200 bg-white/90 px-6 py-3 backdrop-blur">
        <div className="flex items-center gap-6">
          <span className="text-lg font-semibold tracking-tight text-slate-900">DocSense</span>
          <nav className="flex items-center gap-1">
            <NavLink to="/dashboard" className={navClass} end>
              Documents
            </NavLink>
            <NavLink to="/chat" className={navClass}>
              Chat
            </NavLink>
          </nav>
        </div>
        <div className="flex items-center gap-3">
          {user?.picture ? (
            <img src={user.picture} alt="" className="h-8 w-8 rounded-full" referrerPolicy="no-referrer" />
          ) : (
            <span className="h-8 w-8 rounded-full bg-slate-200" />
          )}
          <button
            onClick={onLogout}
            className="rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            Sign out
          </button>
        </div>
      </header>
      <Outlet context={{ user }} />
    </div>
  )
}
