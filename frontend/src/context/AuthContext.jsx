import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import api, { tokens } from '../api/client'

const AuthContext = createContext(null)
export const useAuth = () => useContext(AuthContext)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(() => Boolean(tokens.get()))

  const loadProfile = useCallback(async () => {
    const { data } = await api.get('/users/me')
    setUser(data)
    return data
  }, [])

  useEffect(() => {
    if (tokens.get()) {
      loadProfile()
        .catch(() => tokens.clear())
        .finally(() => setLoading(false))
    }
    const onExpired = () => setUser(null)
    window.addEventListener('auth:expired', onExpired)
    return () => window.removeEventListener('auth:expired', onExpired)
  }, [loadProfile])

  const login = useCallback(
    async (email, password) => {
      const { data } = await api.post('/auth/login', { email, password })
      tokens.set(data)
      return loadProfile()
    },
    [loadProfile],
  )

  const register = useCallback(
    async (form) => {
      await api.post('/auth/register', form)
      return login(form.email, form.password)
    },
    [login],
  )

  const logout = useCallback(async () => {
    const current = tokens.get()
    tokens.clear()
    setUser(null)
    if (current) await api.post('/auth/logout', { refreshToken: current.refreshToken }).catch(() => {})
  }, [])

  const value = useMemo(
    () => ({ user, loading, login, register, logout, isAdmin: user?.role === 'ADMIN' }),
    [user, loading, login, register, logout],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
