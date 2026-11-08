import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import { ToastProvider } from './context/ToastContext'
import Layout from './components/Layout'
import { Spinner } from './components/ui'
import Login from './pages/Login'
import Book from './pages/Book'
import MyBookings from './pages/MyBookings'
import Waitlist from './pages/Waitlist'
import Admin from './pages/Admin'

function Protected({ admin }) {
  const { user, loading, isAdmin } = useAuth()
  if (loading) return <Spinner label="Loading…" />
  if (!user) return <Navigate to="/login" replace />
  if (admin && !isAdmin) return <Navigate to="/" replace />
  return <Layout />
}

function GuestOnly({ children }) {
  const { user, loading } = useAuth()
  if (loading) return <Spinner />
  return user ? <Navigate to="/" replace /> : children
}

export default function App() {
  return (
    <BrowserRouter>
      <ToastProvider>
        <AuthProvider>
          <Routes>
            <Route path="/login" element={<GuestOnly><Login /></GuestOnly>} />
            <Route element={<Protected />}>
              <Route path="/" element={<Book />} />
              <Route path="/bookings" element={<MyBookings />} />
              <Route path="/waitlist" element={<Waitlist />} />
            </Route>
            <Route element={<Protected admin />}>
              <Route path="/admin" element={<Admin />} />
            </Route>
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </AuthProvider>
      </ToastProvider>
    </BrowserRouter>
  )
}
