export const pad = (n) => String(n).padStart(2, '0')

export const isoDate = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`

export const addDays = (d, n) => {
  const copy = new Date(d)
  copy.setDate(copy.getDate() + n)
  return copy
}

export const parseDate = (iso) => {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(y, m - 1, d)
}

export const hhmm = (t) => t.slice(0, 5)

export const startOf = (date, time) => new Date(`${date}T${time.length === 5 ? `${time}:00` : time}`)

export const money = (v) => `€${Number(v).toFixed(2)}`

export const weekday = (d) => d.toLocaleDateString('en-GB', { weekday: 'short' })

export const longDate = (iso) =>
  parseDate(iso).toLocaleDateString('en-GB', { weekday: 'long', day: 'numeric', month: 'long' })

export const timeAgo = (iso) => {
  const diff = Math.max(0, (Date.now() - new Date(iso).getTime()) / 1000)
  if (diff < 60) return 'just now'
  if (diff < 3600) return `${Math.floor(diff / 60)} min ago`
  if (diff < 86400) return `${Math.floor(diff / 3600)} h ago`
  return new Date(iso).toLocaleDateString('en-GB', { day: 'numeric', month: 'short' })
}

export const isActive = (x) => x.active ?? x.isActive ?? true

export const newKey = () =>
  globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random().toString(16).slice(2)}`
