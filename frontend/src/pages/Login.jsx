import { useState } from 'react'
import { Clock, ListOrdered, ShieldCheck } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { errorMessage } from '../api/client'

const empty = { email: '', password: '', firstName: '', lastName: '' }

export default function Login() {
  const { login, register } = useAuth()
  const [mode, setMode] = useState('login')
  const [form, setForm] = useState(empty)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }))

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      if (mode === 'login') await login(form.email, form.password)
      else await register(form)
    } catch (err) {
      setError(
        err?.response?.status === 401 ? 'Wrong email or password' : errorMessage(err, 'Could not sign you in'),
      )
    } finally {
      setBusy(false)
    }
  }

  const features = [
    { icon: Clock, text: 'Live availability for every court' },
    { icon: ShieldCheck, text: 'Double-booking is impossible by design' },
    { icon: ListOrdered, text: 'Join the waitlist and get the slot if someone cancels' },
  ]

  return (
    <div className="auth">
      <section className="auth-hero">
        <div className="brand brand-lg">
          <span className="brand-mark" />
          <span>Courtside</span>
        </div>
        <h1>Book your court in seconds.</h1>
        <p>Padel and tennis courts, one clear schedule.</p>
        <ul>
          {features.map(({ icon: Icon, text }) => (
            <li key={text}>
              <span className="feature-icon">
                <Icon size={16} />
              </span>
              {text}
            </li>
          ))}
        </ul>
      </section>

      <section className="auth-panel">
        <form className="auth-card" onSubmit={submit}>
          <div className="segmented">
            <button type="button" className={mode === 'login' ? 'on' : ''} onClick={() => setMode('login')}>
              Sign in
            </button>
            <button type="button" className={mode === 'register' ? 'on' : ''} onClick={() => setMode('register')}>
              Create account
            </button>
          </div>

          {mode === 'register' && (
            <div className="grid-2">
              <label className="field">
                <span>First name</span>
                <input value={form.firstName} onChange={set('firstName')} required autoComplete="given-name" />
              </label>
              <label className="field">
                <span>Last name</span>
                <input value={form.lastName} onChange={set('lastName')} required autoComplete="family-name" />
              </label>
            </div>
          )}
          <label className="field">
            <span>Email</span>
            <input type="email" value={form.email} onChange={set('email')} required autoComplete="email" />
          </label>
          <label className="field">
            <span>Password</span>
            <input
              type="password"
              value={form.password}
              onChange={set('password')}
              required
              minLength={mode === 'register' ? 8 : undefined}
              autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
            />
            {mode === 'register' && <small className="muted">At least 8 characters</small>}
          </label>

          {error && <div className="form-error">{error}</div>}

          <button className="btn btn-primary btn-block" disabled={busy}>
            {busy ? 'Please wait…' : mode === 'login' ? 'Sign in' : 'Create account'}
          </button>
        </form>
      </section>
    </div>
  )
}
