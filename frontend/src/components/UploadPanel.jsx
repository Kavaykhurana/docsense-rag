import { useRef, useState } from 'react'
import { uploadDocument } from '../api'

const ACCEPT = '.pdf,.docx,.txt,.md,.markdown'

// Drag-and-drop / click upload widget. Validates the extension client-side for
// fast feedback and streams the file to the backend (the original never leaves
// the user's machine beyond this transient upload).
export default function UploadPanel({ onUploaded, onError }) {
  const inputRef = useRef(null)
  const [dragging, setDragging] = useState(false)
  const [busy, setBusy] = useState(false)
  const [progress, setProgress] = useState(0)

  async function handleFiles(fileList) {
    const files = Array.from(fileList || [])
    if (!files.length) return
    for (const file of files) {
      setBusy(true)
      setProgress(0)
      try {
        const doc = await uploadDocument(file, setProgress)
        onUploaded(doc)
      } catch (err) {
        onError?.(err?.response?.data?.message || 'Upload failed. Please try again.')
      } finally {
        setBusy(false)
        setProgress(0)
      }
    }
    if (inputRef.current) inputRef.current.value = ''
  }

  return (
    <div
      onDragOver={(e) => {
        e.preventDefault()
        setDragging(true)
      }}
      onDragLeave={() => setDragging(false)}
      onDrop={(e) => {
        e.preventDefault()
        setDragging(false)
        handleFiles(e.dataTransfer.files)
      }}
      className={`flex flex-col items-center justify-center gap-3 rounded-2xl border-2 border-dashed p-8 text-center transition ${
        dragging ? 'border-slate-900 bg-slate-50' : 'border-slate-300 bg-white'
      }`}
    >
      <p className="text-sm font-medium text-slate-700">
        Drag &amp; drop files here, or
        <button
          type="button"
          onClick={() => inputRef.current?.click()}
          disabled={busy}
          className="ml-1 text-slate-900 underline underline-offset-2 disabled:opacity-50"
        >
          browse
        </button>
      </p>
      <p className="text-xs text-slate-400">PDF, DOCX, TXT or Markdown · up to 20&nbsp;MB each</p>
      <input
        ref={inputRef}
        type="file"
        accept={ACCEPT}
        multiple
        className="hidden"
        onChange={(e) => handleFiles(e.target.files)}
      />
      {busy && (
        <div className="w-full max-w-xs">
          <div className="h-1.5 w-full overflow-hidden rounded-full bg-slate-100">
            <div
              className="h-full rounded-full bg-slate-900 transition-all"
              style={{ width: `${progress}%` }}
            />
          </div>
          <p className="mt-1 text-xs text-slate-500">Processing… {progress}%</p>
        </div>
      )}
    </div>
  )
}
