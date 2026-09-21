// Colour-coded pill for a document's processing status (light theme palette).
const STYLES = {
  PENDING: 'bg-slate-100 text-slate-600 ring-slate-200',
  PROCESSING: 'bg-amber-50 text-amber-700 ring-amber-200',
  COMPLETED: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  FAILED: 'bg-red-50 text-red-700 ring-red-200',
}

export default function StatusBadge({ status }) {
  const cls = STYLES[status] || STYLES.PENDING
  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ring-inset ${cls}`}
    >
      {status === 'PROCESSING' && (
        <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-amber-500" />
      )}
      {status ? status.charAt(0) + status.slice(1).toLowerCase() : 'Unknown'}
    </span>
  )
}
