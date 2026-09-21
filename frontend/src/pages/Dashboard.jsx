import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { listDocuments, deleteDocument } from '../api'
import { formatBytes, formatDate } from '../lib/format'
import StatusBadge from '../components/StatusBadge.jsx'
import UploadPanel from '../components/UploadPanel.jsx'

const NON_TERMINAL = new Set(['PENDING', 'PROCESSING'])

export default function Dashboard() {
  const navigate = useNavigate()
  const [docs, setDocs] = useState([])
  const [loading, setLoading] = useState(true)
  const [query, setQuery] = useState('')
  const [banner, setBanner] = useState(null) // { kind: 'error' | 'ok', text }

  const refresh = useCallback(async () => {
    try {
      const data = await listDocuments()
      setDocs(data)
    } catch (err) {
      setBanner({ kind: 'error', text: 'Could not load your documents.' })
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    refresh()
  }, [refresh])

  // Poll while any document is still being ingested so status chips update live.
  useEffect(() => {
    if (!docs.some((d) => NON_TERMINAL.has(d.status))) return undefined
    const id = setInterval(refresh, 4000)
    return () => clearInterval(id)
  }, [docs, refresh])

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase()
    if (!q) return docs
    return docs.filter(
      (d) => d.filename.toLowerCase().includes(q) || (d.fileType || '').toLowerCase().includes(q),
    )
  }, [docs, query])

  async function onDelete(doc) {
    if (!window.confirm(`Delete "${doc.filename}" and all of its indexed content?`)) return
    try {
      await deleteDocument(doc.id)
      setDocs((prev) => prev.filter((d) => d.id !== doc.id))
      setBanner({ kind: 'ok', text: `Deleted ${doc.filename}.` })
    } catch {
      setBanner({ kind: 'error', text: `Could not delete ${doc.filename}.` })
    }
  }

  return (
    <main className="mx-auto max-w-5xl px-6 py-8">
      <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-slate-900">Your documents</h1>
          <p className="text-sm text-slate-500">
            Upload files to index them, then ask questions in Chat. Originals are never stored.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={() => navigate('/chat')}
            className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            Open Chat
          </button>
          <button
            onClick={refresh}
            className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            Refresh
          </button>
        </div>
      </div>

      {banner && (
        <div
          className={`mb-5 flex items-center justify-between rounded-lg border px-4 py-2.5 text-sm ${
            banner.kind === 'error'
              ? 'border-red-200 bg-red-50 text-red-700'
              : 'border-emerald-200 bg-emerald-50 text-emerald-700'
          }`}
        >
          <span>{banner.text}</span>
          <button onClick={() => setBanner(null)} className="opacity-60 hover:opacity-100">
            Dismiss
          </button>
        </div>
      )}

      <UploadPanel
        onUploaded={(doc) => {
          setDocs((prev) => [doc, ...prev.filter((d) => d.id !== doc.id)])
          setBanner({ kind: 'ok', text: `Indexed ${doc.filename}.` })
          if (NON_TERMINAL.has(doc.status)) refresh()
        }}
        onError={(msg) => setBanner({ kind: 'error', text: msg })}
      />

      <div className="mt-8 flex items-center justify-between gap-4">
        <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-500">
          {filtered.length} {filtered.length === 1 ? 'document' : 'documents'}
        </h2>
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search by name or type…"
          className="w-64 rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-sm text-slate-700 placeholder:text-slate-400 focus:border-slate-400 focus:outline-none"
        />
      </div>

      {loading ? (
        <p className="mt-6 text-slate-500">Loading…</p>
      ) : filtered.length === 0 ? (
        <div className="mt-6 rounded-xl border border-slate-200 bg-white p-10 text-center text-slate-500">
          {docs.length === 0
            ? 'No documents yet. Upload one above to get started.'
            : 'No documents match your search.'}
        </div>
      ) : (
        <ul className="mt-4 divide-y divide-slate-200 overflow-hidden rounded-xl border border-slate-200 bg-white">
          {filtered.map((doc) => (
            <li key={doc.id} className="flex items-center justify-between gap-4 px-5 py-4 hover:bg-slate-50">
              <div className="min-w-0">
                <Link
                  to={`/documents/${doc.id}`}
                  className="block truncate font-medium text-slate-900 hover:underline"
                >
                  {doc.filename}
                </Link>
                <div className="mt-0.5 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-slate-500">
                  <span>{doc.fileType}</span>
                  <span>·</span>
                  <span>{formatBytes(doc.fileSize)}</span>
                  {doc.pageCount ? (
                    <>
                      <span>·</span>
                      <span>{doc.pageCount} pages</span>
                    </>
                  ) : null}
                  <span>·</span>
                  <span>{formatDate(doc.createdAt)}</span>
                </div>
              </div>
              <div className="flex flex-none items-center gap-3">
                <StatusBadge status={doc.status} />
                <Link
                  to={`/chat?documents=${doc.id}`}
                  className="rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-50"
                >
                  Ask
                </Link>
                <button
                  onClick={() => onDelete(doc)}
                  className="rounded-lg border border-transparent px-3 py-1.5 text-sm font-medium text-red-600 hover:bg-red-50"
                >
                  Delete
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </main>
  )
}
