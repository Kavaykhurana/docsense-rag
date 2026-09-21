import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import {
  askChat,
  listConversations,
  listDocuments,
  getConversation,
  deleteConversation,
} from '../api'

function citationLabel(c) {
  return c.pageNumber != null ? `${c.filename} · p.${c.pageNumber}` : c.filename
}

export default function Chat() {
  const navigate = useNavigate()
  const params = useParams()
  const [searchParams] = useSearchParams()
  const activeId = params.conversationId ? Number(params.conversationId) : null

  const [conversations, setConversations] = useState([])
  const [docs, setDocs] = useState([])
  const [messages, setMessages] = useState([])
  const [selectedDocs, setSelectedDocs] = useState(() => {
    const preset = searchParams.get('documents')
    return preset ? new Set(preset.split(',').map(Number).filter(Boolean)) : new Set()
  })
  const [input, setInput] = useState('')
  const [sending, setSending] = useState(false)
  const [error, setError] = useState(null)
  const [pickerOpen, setPickerOpen] = useState(false)

  const loadedRef = useRef(null)
  const scrollRef = useRef(null)

  const refreshConversations = useCallback(() => {
    listConversations().then(setConversations).catch(() => {})
  }, [])

  useEffect(() => {
    listDocuments().then(setDocs).catch(() => {})
    refreshConversations()
  }, [refreshConversations])

  const loadConversation = useCallback((id) => {
    getConversation(id)
      .then((c) => {
        setMessages(c.messages || [])
        loadedRef.current = id
      })
      .catch(() => {
        setMessages([])
        setError('Could not open that conversation.')
      })
  }, [])

  // Sync the message view with the route.
  useEffect(() => {
    setError(null)
    if (activeId == null) {
      setMessages([])
      loadedRef.current = null
      return
    }
    if (loadedRef.current === activeId) return
    loadConversation(activeId)
  }, [activeId, loadConversation])

  useEffect(() => {
    if (scrollRef.current) scrollRef.current.scrollTop = scrollRef.current.scrollHeight
  }, [messages, sending])

  function toggleDoc(id) {
    setSelectedDocs((prev) => {
      const next = new Set(prev)
      next.has(id) ? next.delete(id) : next.add(id)
      return next
    })
  }

  async function onSend() {
    const question = input.trim()
    if (!question || sending) return
    setError(null)
    setSending(true)
    setInput('')
    // Optimistic user bubble; replaced by the authoritative reload after the reply.
    setMessages((prev) => [...prev, { role: 'USER', content: question, citations: [], pending: true }])
    try {
      const res = await askChat({
        question,
        conversationId: activeId,
        documentIds: Array.from(selectedDocs),
      })
      refreshConversations()
      loadedRef.current = null
      if (res.conversationId !== activeId) {
        navigate(`/chat/${res.conversationId}`, { replace: true })
      } else {
        await loadConversation(res.conversationId)
      }
    } catch (err) {
      setMessages((prev) => prev.filter((m) => !m.pending))
      setInput(question)
      setError(err?.response?.data?.message || 'Something went wrong. Please try again.')
    } finally {
      setSending(false)
    }
  }

  async function onDeleteConversation(id) {
    if (!window.confirm('Delete this conversation and its messages?')) return
    await deleteConversation(id).catch(() => {})
    if (id === activeId) navigate('/chat')
    refreshConversations()
  }

  const scopeLabel =
    selectedDocs.size === 0
      ? 'All documents'
      : selectedDocs.size === 1
        ? docs.find((d) => d.id === [...selectedDocs][0])?.filename || '1 document'
        : `${selectedDocs.size} documents`

  return (
    <div className="mx-auto flex h-[calc(100vh-57px)] max-w-6xl gap-0 px-4 py-4">
      {/* Conversation history */}
      <aside className="flex w-60 flex-none flex-col rounded-xl border border-slate-200 bg-white">
        <div className="p-3">
          <button
            onClick={() => navigate('/chat')}
            className="w-full rounded-lg bg-slate-900 px-3 py-2 text-sm font-semibold text-white hover:bg-slate-800"
          >
            + New chat
          </button>
        </div>
        <div className="min-h-0 flex-1 overflow-y-auto px-2 pb-2">
          {conversations.length === 0 ? (
            <p className="px-2 py-3 text-xs text-slate-400">No conversations yet.</p>
          ) : (
            conversations.map((c) => (
              <div
                key={c.id}
                className={`group flex items-center gap-1 rounded-lg px-2 py-1.5 text-sm ${
                  c.id === activeId ? 'bg-slate-100 text-slate-900' : 'text-slate-600 hover:bg-slate-50'
                }`}
              >
                <button onClick={() => navigate(`/chat/${c.id}`)} className="min-w-0 flex-1 truncate text-left">
                  {c.title}
                </button>
                <button
                  onClick={() => onDeleteConversation(c.id)}
                  className="hidden flex-none px-1 text-slate-400 hover:text-red-600 group-hover:block"
                  aria-label="Delete conversation"
                >
                  ✕
                </button>
              </div>
            ))
          )}
        </div>
      </aside>

      {/* Message pane */}
      <section className="ml-4 flex min-w-0 flex-1 flex-col rounded-xl border border-slate-200 bg-white">
        <div className="flex items-center justify-between border-b border-slate-200 p-3">
          <div className="text-sm font-medium text-slate-700">
            {docs.length === 0 ? (
              <span className="text-slate-400">Upload documents to begin</span>
            ) : (
              <>Searching: {scopeLabel}</>
            )}
          </div>
          <div className="relative">
            <button
              onClick={() => setPickerOpen((v) => !v)}
              disabled={docs.length === 0}
              className="rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50"
            >
              {selectedDocs.size === 0 ? 'All documents' : `${selectedDocs.size} selected`} ▾
            </button>
            {pickerOpen && (
              <>
                <button className="fixed inset-0 z-10 cursor-default" onClick={() => setPickerOpen(false)} aria-hidden />
                <div className="absolute right-0 z-20 mt-1 max-h-72 w-72 overflow-y-auto rounded-lg border border-slate-200 bg-white p-2 shadow-lg">
                  <label className="flex cursor-pointer items-center gap-2 rounded px-2 py-1.5 text-sm text-slate-600 hover:bg-slate-50">
                    <input
                      type="checkbox"
                      checked={selectedDocs.size === 0}
                      onChange={() => setSelectedDocs(new Set())}
                    />
                    All documents
                  </label>
                  <div className="my-1 border-t border-slate-100" />
                  {docs.map((d) => (
                    <label
                      key={d.id}
                      className="flex cursor-pointer items-center gap-2 rounded px-2 py-1.5 text-sm text-slate-700 hover:bg-slate-50"
                    >
                      <input type="checkbox" checked={selectedDocs.has(d.id)} onChange={() => toggleDoc(d.id)} />
                      <span className="truncate">{d.filename}</span>
                    </label>
                  ))}
                </div>
              </>
            )}
          </div>
        </div>

        <div ref={scrollRef} className="min-h-0 flex-1 space-y-4 overflow-y-auto p-5">
          {messages.length === 0 && !sending && (
            <div className="flex h-full flex-col items-center justify-center text-center text-slate-400">
              <p className="text-lg font-medium text-slate-500">Ask a question about your documents</p>
              <p className="mt-1 max-w-sm text-sm">
                Answers are grounded in your indexed content and include source citations.
              </p>
            </div>
          )}

          {messages.map((m, i) => (
            <MessageBubble key={m.id || i} message={m} />
          ))}

          {sending && (
            <div className="flex items-center gap-2 text-sm text-slate-400">
              <span className="h-2 w-2 animate-bounce rounded-full bg-slate-400 [animation-delay:-0.2s]" />
              <span className="h-2 w-2 animate-bounce rounded-full bg-slate-400 [animation-delay:-0.1s]" />
              <span className="h-2 w-2 animate-bounce rounded-full bg-slate-400" />
              <span className="ml-1">Thinking…</span>
            </div>
          )}
        </div>

        {error && <div className="border-t border-red-100 bg-red-50 px-5 py-2 text-sm text-red-700">{error}</div>}

        <div className="border-t border-slate-200 p-3">
          <div className="flex items-end gap-2">
            <textarea
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                  e.preventDefault()
                  onSend()
                }
              }}
              rows={1}
              placeholder={docs.length === 0 ? 'Upload a document first…' : 'Ask anything about your documents…'}
              className="max-h-40 min-h-[44px] flex-1 resize-y rounded-xl border border-slate-300 bg-white px-3.5 py-2.5 text-sm text-slate-800 placeholder:text-slate-400 focus:border-slate-400 focus:outline-none"
            />
            <button
              onClick={onSend}
              disabled={sending || !input.trim()}
              className="rounded-xl bg-slate-900 px-4 py-2.5 text-sm font-semibold text-white hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-40"
            >
              Send
            </button>
          </div>
        </div>
      </section>
    </div>
  )
}

function MessageBubble({ message }) {
  const isUser = message.role === 'USER'
  const citations = message.citations || []
  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'}`}>
      <div className={`max-w-[80%] ${isUser ? 'items-end' : 'items-start'}`}>
        <div
          className={`whitespace-pre-wrap rounded-2xl px-4 py-2.5 text-sm leading-relaxed ${
            isUser ? 'bg-slate-900 text-white' : 'border border-slate-200 bg-slate-50 text-slate-800'
          }`}
        >
          {message.content}
        </div>
        {!isUser && citations.length > 0 && (
          <details className="mt-1.5 rounded-lg border border-slate-200 bg-white">
            <summary className="cursor-pointer px-3 py-1.5 text-xs font-medium text-slate-500">
              {citations.length} source{citations.length > 1 ? 's' : ''}
            </summary>
            <ul className="space-y-1.5 border-t border-slate-100 p-2">
              {citations.map((c, idx) => (
                <li key={idx} className="rounded-md bg-slate-50 p-2">
                  <div className="flex items-center justify-between text-xs font-medium text-slate-600">
                    <span className="truncate">
                      [{idx + 1}] {citationLabel(c)}
                    </span>
                    <span className="flex-none text-slate-400">{Math.round(c.score * 100)}%</span>
                  </div>
                  {c.snippet && <p className="mt-1 line-clamp-3 text-xs text-slate-500">{c.snippet}</p>}
                </li>
              ))}
            </ul>
          </details>
        )}
      </div>
    </div>
  )
}
