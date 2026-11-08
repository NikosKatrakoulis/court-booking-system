import { useEffect, useState } from 'react'
import { Bell, Clock, Hourglass, ListOrdered, Zap } from 'lucide-react'
import api, { errorMessage } from '../api/client'
import useFetch from '../hooks/useFetch'
import { useToast } from '../context/ToastContext'
import { Empty, PageHeader, Pill, Spinner } from '../components/ui'
import { hhmm, longDate, newKey, timeAgo } from '../utils/format'

function Countdown({ to }) {
  const [left, setLeft] = useState(() => new Date(to) - Date.now())
  useEffect(() => {
    const id = setInterval(() => setLeft(new Date(to) - Date.now()), 1000)
    return () => clearInterval(id)
  }, [to])
  if (left <= 0) return <strong>expired</strong>
  const s = Math.floor(left / 1000)
  return <strong className="mono">{String(Math.floor(s / 60)).padStart(2, '0')}:{String(s % 60).padStart(2, '0')}</strong>
}

export default function Waitlist() {
  const toast = useToast()
  const entries = useFetch(() => api.get('/waitlist').then((r) => r.data), [], { interval: 10000 })
  const notes = useFetch(() => api.get('/notifications').then((r) => r.data), [], { interval: 10000 })
  const [busy, setBusy] = useState(null)

  const run = async (id, action, message) => {
    setBusy(id)
    try {
      await action()
      toast.success(message)
      window.dispatchEvent(new Event('waitlist:changed'))
      await Promise.all([entries.reload(), notes.reload()])
    } catch (err) {
      toast.error(errorMessage(err))
    } finally {
      setBusy(null)
    }
  }

  const claim = (e) =>
    run(e.id, () => api.post('/bookings', { slotId: e.slotId }, { headers: { 'Idempotency-Key': newKey() } }), 'Booked. See you on court!')
  const leave = (e) => run(e.id, () => api.delete(`/waitlist/${e.id}`), 'You left the waitlist')

  if (entries.loading && !entries.data) return <Spinner label="Loading…" />
  const list = entries.data ?? []

  return (
    <>
      <PageHeader title="Waitlist" subtitle="Taken slots you're waiting on. When someone cancels, the first in line gets an exclusive offer." />
      <div className="two-col">
        <section>
          <h2 className="section-title"><ListOrdered size={18} /> Your queue</h2>
          {list.length === 0 ? (
            <Empty icon={Hourglass} title="You're not waiting for anything" text="Open a booked slot on the Book tab to join its waitlist." />
          ) : (
            <div className="stack">
              {list.map((e) => (
                <article key={e.id} className={`wait-card${e.status === 'OFFERED' ? ' offered' : ''}`}>
                  <div className="wait-top">
                    <div>
                      <h3>{e.courtName}</h3>
                      <p className="muted">{longDate(e.date)} · {hhmm(e.startTime)}</p>
                    </div>
                    {e.status === 'OFFERED' ? <Pill tone="success"><Zap size={12} /> Your turn</Pill> : <Pill tone="warn">In line</Pill>}
                  </div>
                  {e.status === 'OFFERED' && (
                    <div className="offer">
                      <span><Clock size={15} /> Claim it within <Countdown to={e.offerExpiresAt} /></span>
                      <button className="btn btn-primary btn-sm" disabled={busy === e.id} onClick={() => claim(e)}>Book now</button>
                    </div>
                  )}
                  <div className="wait-foot">
                    <button className="link-btn" disabled={busy === e.id} onClick={() => leave(e)}>Leave waitlist</button>
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>

        <section>
          <h2 className="section-title"><Bell size={18} /> Notifications</h2>
          {(notes.data ?? []).length === 0 ? (
            <Empty icon={Bell} title="All quiet" text="You'll be notified here when a slot is offered to you." />
          ) : (
            <ul className="feed">
              {notes.data.map((n) => (
                <li key={n.id}>
                  <span className="feed-dot" />
                  <div>
                    <p>{n.message}</p>
                    <small className="muted">{timeAgo(n.createdAt)}</small>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </>
  )
}
