import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getDocument, previewDocument } from '../api'
import { formatBytes, formatDate } from '../lib/format'
import StatusBadge from '../components/StatusBadge.jsx'

export default function DocumentDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [meta, setMeta] = useState(null)
  const [preview, setPreview] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    let active = true
    Promise.all([getDocument(id), previewDocument(id)])
      .then(([m, p]) => {
        if (!active) return
        setMeta(m)
        setPreview(p)
      })
      .catch((err) => {
        if (!active) return
        setError(err?.response?.status === 404 ? 'Document not found.' : 'Could not load document.')
      })
    return () => {
      active = false
    }
  }, [id])

  if (error) {
    return (
      <main className="mx-auto max-w-3xl px-6 py-10">
        <p className="text-red-600">{error}</p>
        <Link to="/dashboard" className="text-sm text-slate-600 hover:underline">
          ← Back to documents
        </Link>
      </main>
    )
  }

  if (!meta) {
    return <main className="mx-auto max-w-3xl px-6 py-10 text-slate-500">Loading…</main>
  }

  return (
    <main className="mx-auto max-w-3xl px-6 py-8">
      <Link to="/dashboard" className="text-sm text-slate-500 hover:text-slate-800">
        ← Back to documents
      </Link>

      <div className="mt-3 flex flex-wrap items-start justify-between gap-4">
        <div className="min-w-0">
          <h1 className="truncate text-2xl font-semibold tracking-tight text-slate-900">{meta.filename}</h1>
          <div className="mt-1 flex items-center gap-3 text-sm text-slate-500">
            <span>{meta.fileType}</span>
            <span>·</span>
            <span>{formatBytes(meta.fileSize)}</span>
            {meta.pageCount ? (
              <>
                <span>·</span>
                <span>{meta.pageCount} pages</span>
              </>
            ) : null}
            <span>·</span>
            <span>{formatDate(meta.createdAt)}</span>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <StatusBadge status={meta.status} />
          <button
            onClick={() => navigate(`/chat?documents=${meta.id}`)}
            className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-800"
          >
            Ask about this
          </button>
        </div>
      </div>

      <section className="mt-8">
        <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-500">
          Indexed content ({preview ? preview.chunks.length : 0} chunks)
        </h2>
        {!preview ? (
          <p className="mt-4 text-slate-500">Loading content…</p>
        ) : preview.chunks.length === 0 ? (
          <div className="mt-4 rounded-xl border border-slate-200 bg-white p-8 text-center text-slate-500">
            No indexed text yet. This document may still be processing or may have failed.
          </div>
        ) : (
          <ul className="mt-4 space-y-3">
            {preview.chunks.map((chunk) => (
              <li key={chunk.chunkIndex} className="rounded-xl border border-slate-200 bg-white p-4">
                <div className="mb-1.5 flex items-center gap-2 text-xs text-slate-400">
                  <span className="font-medium text-slate-500">Chunk #{chunk.chunkIndex + 1}</span>
                  {chunk.pageNumber != null && <span>· page {chunk.pageNumber}</span>}
                </div>
                <p className="whitespace-pre-wrap text-sm leading-relaxed text-slate-700">{chunk.content}</p>
              </li>
            ))}
          </ul>
        )}
      </section>
    </main>
  )
}
