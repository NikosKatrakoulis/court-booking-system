import { useEffect } from 'react'
import { NavLink, Outlet } from 'react-router-dom'
import { CalendarDays, ListOrdered, LogOut, ShieldCheck, Ticket } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import useFetch from '../hooks/useFetch'
import api from '../api/client'

export default function Layout() {
  const { user, logout, isAdmin } = useAuth()
  const { data: entries, reload } = useFetch(() => api.get('/waitlist').then((r) => r.data), [], { interval: 20000 })

  useEffect(() => {
    window.addEventListener('waitlist:changed', reload)
    return () => window.removeEventListener('waitlist:changed', reload)
  })

  const offers = (entries ?? []).filter((e) => e.status === 'OFFERED').length

  const links = [
    { to: '/', label: 'Book', icon: CalendarDays, end: true },
    { to: '/bookings', label: 'My bookings', icon: Ticket },
    { to: '/waitlist', label: 'Waitlist', icon: ListOrdered, badge: offers },
    ...(isAdmin ? [{ to: '/admin', label: 'Admin', icon: ShieldCheck }] : []),
  ]

  return (
    <div className="app">
      <header className="nav">
        <div className="nav-inner">
          <div className="brand">
            <span className="brand-mark" />
            <span>Courtside</span>
          </div>
          <nav className="nav-links">
            {links.map(({ to, label, icon: Icon, end, badge }) => (
              <NavLink key={to} to={to} end={end} className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}>
                <Icon size={16} />
                <span>{label}</span>
                {badge > 0 && <span className="dot">{badge}</span>}
              </NavLink>
            ))}
          </nav>
          <div className="nav-user">
            <div className="avatar" title={user.email}>
              {user.firstName?.[0]}
              {user.lastName?.[0]}
            </div>
            <span className="nav-name">
              {user.firstName} {user.lastName}
            </span>
            <button className="btn btn-ghost-light" onClick={logout}>
              <LogOut size={15} />
              <span>Logout</span>
            </button>
          </div>
        </div>
      </header>
      <main className="container">
        <Outlet />
      </main>
    </div>
  )
}
