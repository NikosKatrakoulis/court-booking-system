import { useState } from 'react'
import { Banknote, CalendarX2, Clock, Ticket } from 'lucide-react'
import api, { errorMessage } from '../api/client'
import useFetch from '../hooks/useFetch'
import { useToast } from '../context/ToastContext'
import { Empty, Pill, Spinner, PageHeader } from '../components/ui'
import { hhmm, money, parseDate, startOf } from '../utils/format'

function DateBlock({ date }) {
  const d = parseDate(date)
  return (
    <div className="date-block">
      <span>{d.toLocaleDateString('en-GB', { month: 'short' })}</span>
      <strong>{d.getDate()}</strong>
      <small>{d.toLocaleDateString('en-GB', { weekday: 'short' })}</small>
    </div>
  )
}

function BookingCard({ b, onCancel, busy }) {
  const [confirming, setConfirming] = useState(false)
  const upcoming = b.status === 'CONFIRMED' && startOf(b.date, b.startTime) > new Date()

  return (
    <article className={`booking${b.status === 'CANCELLED' ? ' is-cancelled' : ''}`}>
      <DateBlock date={b.date} />
      <div className="booking-main">
        <h3>{b.courtName}</h3>
        <div className="booking-meta">
          <span><Clock size={14} /> {hhmm(b.startTime)} – {hhmm(b.endTime)}</span>
          <span><Banknote size={14} /> {money(b.price)}</span>
        </div>
      </div>
      <div className="booking-side">
        {b.status === 'CANCELLED' ? (
          <Pill tone="danger">Cancelled</Pill>
        ) : upcoming ? (
          <Pill tone="success">Confirmed</Pill>
        ) : (
          <Pill>Completed</Pill>
        )}
        {upcoming &&
          (confirming ? (
            <div className="inline-confirm">
              <button className="btn btn-danger btn-sm" disabled={busy} onClick={() => onCancel(b.id)}>Yes, cancel</button>
              <button className="btn btn-sm" onClick={() => setConfirming(false)}>Keep</button>
            </div>
          ) : b.cancellable ? (
            <button className="btn btn-sm" onClick={() => setConfirming(true)}>Cancel booking</button>
          ) : (
            <small className="muted">Can't cancel within 2 h</small>
          ))}
      </div>
    </article>
  )
}

export default function MyBookings() {
  const toast = useToast()
  const { data, loading, reload } = useFetch(() => api.get('/bookings').then((r) => r.data))
  const [tab, setTab] = useState('upcoming')
  const [busy, setBusy] = useState(false)

  const cancel = async (id) => {
    setBusy(true)
    try {
      await api.patch(`/bookings/${id}`)
      toast.success('Booking cancelled')
      await reload()
    } catch (err) {
      toast.error(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  if (loading) return <Spinner label="Loading your bookings…" />

  const now = new Date()
  const upcoming = (data ?? []).filter((b) => b.status === 'CONFIRMED' && startOf(b.date, b.startTime) > now).reverse()
  const history = (data ?? []).filter((b) => !upcoming.includes(b))
  const list = tab === 'upcoming' ? upcoming : history

  return (
    <>
      <PageHeader title="My bookings" subtitle="Everything you've reserved, in one place." />
      <div className="tabs">
        <button className={tab === 'upcoming' ? 'on' : ''} onClick={() => setTab('upcoming')}>
          Upcoming <span className="count">{upcoming.length}</span>
        </button>
        <button className={tab === 'history' ? 'on' : ''} onClick={() => setTab('history')}>
          History <span className="count">{history.length}</span>
        </button>
      </div>
      {list.length === 0 ? (
        <Empty
          icon={tab === 'upcoming' ? Ticket : CalendarX2}
          title={tab === 'upcoming' ? 'No upcoming bookings' : 'Nothing here yet'}
          text={tab === 'upcoming' ? 'Head to the Book tab to reserve your next game.' : 'Past and cancelled bookings will appear here.'}
        />
      ) : (
        <div className="stack">
          {list.map((b) => <BookingCard key={b.id} b={b} onCancel={cancel} busy={busy} />)}
        </div>
      )}
    </>
  )
}
