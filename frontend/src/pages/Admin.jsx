import { useState } from 'react'
import { BarChart3, LayoutGrid, Percent, Plus, RefreshCw, Sparkles, TrendingUp } from 'lucide-react'
import api, { errorMessage } from '../api/client'
import useFetch from '../hooks/useFetch'
import { useToast } from '../context/ToastContext'
import { Empty, PageHeader, Pill, Spinner } from '../components/ui'
import { addDays, hhmm, isActive, isoDate, money } from '../utils/format'

const tabs = [
  { key: 'analytics', label: 'Analytics', icon: BarChart3 },
  { key: 'courts', label: 'Courts', icon: LayoutGrid },
  { key: 'slots', label: 'Slots', icon: Sparkles },
]

export default function Admin() {
  const [tab, setTab] = useState('analytics')
  return (
    <>
      <PageHeader title="Admin" subtitle="Performance, courts and slot generation." />
      <div className="tabs">
        {tabs.map(({ key, label, icon: Icon }) => (
          <button key={key} className={tab === key ? 'on' : ''} onClick={() => setTab(key)}>
            <Icon size={15} /> {label}
          </button>
        ))}
      </div>
      {tab === 'analytics' && <Analytics />}
      {tab === 'courts' && <Courts />}
      {tab === 'slots' && <Slots />}
    </>
  )
}

function Analytics() {
  const [from, setFrom] = useState(isoDate(addDays(new Date(), -30)))
  const [to, setTo] = useState(isoDate(addDays(new Date(), 14)))
  const params = { from, to }
  const occ = useFetch(() => api.get('/admin/analytics/occupancy', { params }).then((r) => r.data), [from, to])
  const pop = useFetch(() => api.get('/admin/analytics/popular-slots', { params: { ...params, limit: 8 } }).then((r) => r.data), [from, to])

  const rows = occ.data ?? []
  const totalSlots = rows.reduce((s, r) => s + r.totalSlots, 0)
  const booked = rows.reduce((s, r) => s + r.bookedSlots, 0)
  const overall = totalSlots ? booked / totalSlots : 0
  const peak = Math.max(1, ...(pop.data ?? []).map((p) => p.bookings))

  return (
    <>
      <div className="filters">
        <label className="field inline"><span>From</span><input type="date" value={from} max={to} onChange={(e) => setFrom(e.target.value)} /></label>
        <label className="field inline"><span>To</span><input type="date" value={to} min={from} onChange={(e) => setTo(e.target.value)} /></label>
      </div>

      <div className="stats">
        <Stat icon={Percent} label="Overall occupancy" value={`${Math.round(overall * 100)}%`} />
        <Stat icon={TrendingUp} label="Confirmed bookings" value={booked} />
        <Stat icon={LayoutGrid} label="Slots in range" value={totalSlots} />
      </div>

      <div className="two-col">
        <div className="card">
          <h3>Occupancy by court</h3>
          {occ.loading ? <Spinner /> : rows.length === 0 ? (
            <Empty icon={BarChart3} title="No slots in this range" />
          ) : (
            <div className="bars">
              {rows.map((r) => (
                <div key={r.courtId} className="bar-row">
                  <div className="bar-label"><span>{r.courtName}</span><strong>{Math.round(r.occupancyRate * 100)}%</strong></div>
                  <div className="bar"><i style={{ width: `${r.occupancyRate * 100}%` }} /></div>
                  <small className="muted">{r.bookedSlots} of {r.totalSlots} slots booked</small>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="card">
          <h3>Most popular start times</h3>
          {pop.loading ? <Spinner /> : (pop.data ?? []).length === 0 ? (
            <Empty icon={TrendingUp} title="No bookings in this range" />
          ) : (
            <div className="columns">
              {pop.data.map((p) => (
                <div key={p.startTime} className="column">
                  <strong>{p.bookings}</strong>
                  <div className="column-bar"><i style={{ height: `${(p.bookings / peak) * 100}%` }} /></div>
                  <span>{hhmm(p.startTime)}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </>
  )
}

function Stat({ icon: Icon, label, value }) {
  return (
    <div className="stat">
      <span className="stat-icon"><Icon size={18} /></span>
      <div><small className="muted">{label}</small><strong>{value}</strong></div>
    </div>
  )
}

function Courts() {
  const toast = useToast()
  const types = useFetch(() => api.get('/court_types').then((r) => r.data))
  const courts = useFetch(() => api.get('/courts').then((r) => r.data))
  const [typeForm, setTypeForm] = useState({ name: '', durationMinutes: 90, pricePerSlot: 24 })
  const [courtForm, setCourtForm] = useState({ name: '', courtTypeId: '' })

  const act = async (fn, message, after) => {
    try {
      await fn()
      toast.success(message)
      after?.()
    } catch (err) {
      toast.error(errorMessage(err))
    }
  }

  const addType = (e) => {
    e.preventDefault()
    act(
      () => api.post('/court_types', { ...typeForm, durationMinutes: Number(typeForm.durationMinutes), pricePerSlot: Number(typeForm.pricePerSlot), isActive: true }),
      'Court type created',
      () => { setTypeForm({ name: '', durationMinutes: 90, pricePerSlot: 24 }); types.reload() },
    )
  }
  const addCourt = (e) => {
    e.preventDefault()
    act(
      () => api.post('/courts', { name: courtForm.name, courtTypeId: Number(courtForm.courtTypeId), isActive: true }),
      'Court created. Generate slots to make it bookable right away.',
      () => { setCourtForm({ name: '', courtTypeId: '' }); courts.reload() },
    )
  }
  const toggle = (path, item, reload) =>
    act(() => api.patch(`${path}/${item.id}`, { isActive: !isActive(item) }), isActive(item) ? 'Deactivated' : 'Activated', reload)

  return (
    <div className="two-col">
      <div className="card">
        <h3>Court types</h3>
        <form className="inline-form" onSubmit={addType}>
          <label className="field inline grow"><span>Name</span><input placeholder="e.g. Padel" value={typeForm.name} onChange={(e) => setTypeForm({ ...typeForm, name: e.target.value })} required /></label>
          <label className="field inline"><span>Minutes</span><input type="number" min="15" step="15" value={typeForm.durationMinutes} onChange={(e) => setTypeForm({ ...typeForm, durationMinutes: e.target.value })} required /></label>
          <label className="field inline"><span>Price (€)</span><input type="number" min="0" step="0.5" value={typeForm.pricePerSlot} onChange={(e) => setTypeForm({ ...typeForm, pricePerSlot: e.target.value })} required /></label>
          <button className="btn btn-primary"><Plus size={16} /> Add</button>
        </form>
        {types.loading ? <Spinner /> : (
          <ul className="rows">
            {(types.data ?? []).map((t) => (
              <li key={t.id}>
                <div><strong>{t.name}</strong><small className="muted">{t.durationMinutes} min · {money(t.pricePerSlot)}</small></div>
                <button className="chip-btn" onClick={() => toggle('/court_types', t, types.reload)}>
                  <Pill tone={isActive(t) ? 'success' : 'neutral'}>{isActive(t) ? 'Active' : 'Inactive'}</Pill>
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>

      <div className="card">
        <h3>Courts</h3>
        <form className="inline-form" onSubmit={addCourt}>
          <label className="field inline grow"><span>Name</span><input placeholder="e.g. Court 3" value={courtForm.name} onChange={(e) => setCourtForm({ ...courtForm, name: e.target.value })} required /></label>
          <label className="field inline grow"><span>Type</span>
            <select value={courtForm.courtTypeId} onChange={(e) => setCourtForm({ ...courtForm, courtTypeId: e.target.value })} required>
              <option value="" disabled>Choose…</option>
              {(types.data ?? []).filter(isActive).map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
            </select>
          </label>
          <button className="btn btn-primary"><Plus size={16} /> Add</button>
        </form>
        {courts.loading ? <Spinner /> : (
          <ul className="rows">
            {(courts.data ?? []).map((c) => (
              <li key={c.id}>
                <div><strong>{c.name}</strong><small className="muted">{(types.data ?? []).find((t) => t.id === c.courtTypeId)?.name ?? '—'}</small></div>
                <button className="chip-btn" onClick={() => toggle('/courts', c, courts.reload)}>
                  <Pill tone={isActive(c) ? 'success' : 'neutral'}>{isActive(c) ? 'Active' : 'Inactive'}</Pill>
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}

function Slots() {
  const toast = useToast()
  const [busy, setBusy] = useState(false)
  const [last, setLast] = useState(null)

  const generate = async () => {
    setBusy(true)
    try {
      const { data } = await api.post('/admin/slots/generate')
      setLast(data.created)
      toast.success(`${data.created} new slots created`)
    } catch (err) {
      toast.error(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="card hero-card">
      <div className="empty-icon"><Sparkles size={26} /></div>
      <h3>Generate slots</h3>
      <p className="muted">Slots are created automatically every night for the next 14 days. Run it now after adding a new court. It never creates duplicates.</p>
      <button className="btn btn-primary" onClick={generate} disabled={busy}>
        <RefreshCw size={16} className={busy ? 'spin' : ''} /> {busy ? 'Generating…' : 'Generate now'}
      </button>
      {last !== null && <p className="note">Last run created {last} slots.</p>}
    </div>
  )
}
