import { useEffect, useMemo, useRef, useState } from 'react'
import { CalendarClock, Clock, Lock, MapPin, Sunrise, Sun, Moon, Users } from 'lucide-react'
import api, { errorMessage } from '../api/client'
import useFetch from '../hooks/useFetch'
import { useToast } from '../context/ToastContext'
import { Empty, Modal, PageHeader, Pill, Spinner } from '../components/ui'
import { addDays, hhmm, isActive, isoDate, longDate, money, newKey, startOf, weekday } from '../utils/format'

const periods = [
  { key: 'morning', label: 'Morning', icon: Sunrise, test: (h) => h < 12 },
  { key: 'afternoon', label: 'Afternoon', icon: Sun, test: (h) => h >= 12 && h < 17 },
  { key: 'evening', label: 'Evening', icon: Moon, test: (h) => h >= 17 },
]

export default function Book() {
  const toast = useToast()
  const [courtId, setCourtId] = useState(null)
  const [date, setDate] = useState(isoDate(new Date()))
  const [selected, setSelected] = useState(null)

  const days = useMemo(() => Array.from({ length: 14 }, (_, i) => addDays(new Date(), i)), [])
  const { data: courts, loading: loadingCourts } = useFetch(() => api.get('/courts').then((r) => r.data))
  const { data: types } = useFetch(() => api.get('/court_types').then((r) => r.data))

  const typeOf = (court) => (types ?? []).find((t) => t.id === court.courtTypeId)
  const activeCourts = (courts ?? []).filter((c) => isActive(c) && (typeOf(c) ? isActive(typeOf(c)) : true))
  const court = activeCourts.find((c) => c.id === courtId)

  useEffect(() => {
    if (!courtId && activeCourts.length) setCourtId(activeCourts[0].id)
  }, [activeCourts, courtId])

  const { data: slots, loading, reload } = useFetch(
    () => (courtId ? api.get(`/courts/${courtId}/slots`, { params: { date } }).then((r) => r.data) : []),
    [courtId, date],
  )

  const now = new Date()
  const visible = (slots ?? [])
    .map((s) => ({ ...s, passed: startOf(date, s.startTime) < now }))
    .sort((a, b) => a.startTime.localeCompare(b.startTime))
  const freeCount = visible.filter((s) => s.available && !s.passed).length

  if (loadingCourts) return <Spinner label="Loading courts…" />
  if (!activeCourts.length)
    return <Empty icon={MapPin} title="No courts available yet" text="Courts will show up here as soon as an admin adds them." />

  return (
    <>
      <PageHeader title="Book a court" subtitle="Pick a court, a day and a time. That's it." />

      <div className="split">
        <aside className="side">
          <h4 className="side-title">Courts</h4>
          <div className="side-list">
            {activeCourts.map((c) => {
              const t = typeOf(c)
              return (
                <button key={c.id} className={`court-card${c.id === courtId ? ' active' : ''}`} onClick={() => setCourtId(c.id)}>
                  <span className="court-name">{c.name}</span>
                  {t && (
                    <span className="court-meta">
                      {t.name} · {t.durationMinutes} min · {money(t.pricePerSlot)}
                    </span>
                  )}
                </button>
              )
            })}
          </div>
        </aside>

        <section className="panel-col">
          <div className="days" role="tablist">
            {days.map((d) => {
              const iso = isoDate(d)
              return (
                <button key={iso} role="tab" aria-selected={iso === date} className={`day${iso === date ? ' active' : ''}`} onClick={() => setDate(iso)}>
                  <span>{iso === isoDate(new Date()) ? 'Today' : weekday(d)}</span>
                  <strong>{d.getDate()}</strong>
                </button>
              )
            })}
          </div>

          <div className="card">
            <div className="card-head">
              <div>
                <h3>{court?.name}</h3>
                <p className="muted">{longDate(date)}</p>
              </div>
              {!loading && <Pill tone={freeCount ? 'success' : 'neutral'}>{freeCount} free</Pill>}
            </div>

            {loading ? (
              <div className="slot-grid">
                {Array.from({ length: 8 }, (_, i) => (
                  <div key={i} className="skeleton slot-skel" />
                ))}
              </div>
            ) : visible.length === 0 ? (
              <Empty icon={CalendarClock} title="No slots for this day" text="Slots are published 14 days ahead. Try another date." />
            ) : (
              periods.map(({ key, label, icon: Icon, test }) => {
                const group = visible.filter((s) => test(Number(s.startTime.slice(0, 2))))
                if (!group.length) return null
                return (
                  <div key={key} className="period">
                    <div className="period-title">
                      <Icon size={15} /> {label}
                    </div>
                    <div className="slot-grid">
                      {group.map((s) => (
                        <button
                          key={s.id}
                          disabled={s.passed}
                          className={`slot ${s.passed ? 'passed' : s.available ? 'free' : 'taken'}`}
                          onClick={() => setSelected(s)}
                        >
                          <span className="slot-time">
                            {hhmm(s.startTime)} – {hhmm(s.endTime)}
                          </span>
                          <span className="slot-state">
                            {s.passed ? 'Passed' : s.available ? 'Available' : (<><Lock size={12} /> Booked</>)}
                          </span>
                        </button>
                      ))}
                    </div>
                  </div>
                )
              })
            )}
          </div>
        </section>
      </div>

      {selected && (
        <SlotModal
          slot={selected}
          court={court}
          type={typeOf(court)}
          date={date}
          onClose={() => setSelected(null)}
          onDone={(message) => {
            toast.success(message)
            setSelected(null)
            reload()
          }}
        />
      )}
    </>
  )
}

function SlotModal({ slot, court, type, date, onClose, onDone }) {
  const toast = useToast()
  const key = useRef(newKey())
  const [busy, setBusy] = useState(false)
  const booking = slot.available

  const submit = async () => {
    setBusy(true)
    try {
      if (booking) {
        await api.post('/bookings', { slotId: slot.id }, { headers: { 'Idempotency-Key': key.current } })
        onDone('Booked. See you on court!')
      } else {
        await api.post(`/slots/${slot.id}/waitlist`)
        window.dispatchEvent(new Event('waitlist:changed'))
        onDone("You're on the waitlist. We'll let you know if it opens up.")
      }
    } catch (err) {
      toast.error(errorMessage(err))
      setBusy(false)
    }
  }

  return (
    <Modal
      title={booking ? 'Confirm booking' : 'Join the waitlist'}
      onClose={onClose}
      footer={
        <>
          <button className="btn" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" onClick={submit} disabled={busy}>
            {busy ? 'Working…' : booking ? 'Confirm booking' : 'Join waitlist'}
          </button>
        </>
      }
    >
      <dl className="summary">
        <div><dt><MapPin size={15} /> Court</dt><dd>{court?.name}{type ? ` · ${type.name}` : ''}</dd></div>
        <div><dt><CalendarClock size={15} /> Date</dt><dd>{longDate(date)}</dd></div>
        <div><dt><Clock size={15} /> Time</dt><dd>{hhmm(slot.startTime)} – {hhmm(slot.endTime)}</dd></div>
        {type && <div><dt>Price</dt><dd className="price">{money(type.pricePerSlot)}</dd></div>}
      </dl>
      {booking ? (
        <p className="note">Free cancellation up to 2 hours before the start.</p>
      ) : (
        <p className="note"><Users size={14} /> This slot is taken. If the booking is cancelled, the first person in line gets 30 minutes to claim it.</p>
      )}
    </Modal>
  )
}
