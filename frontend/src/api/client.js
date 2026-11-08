import axios from 'axios'

const KEY = 'cbs.auth'

export const tokens = {
  get() {
    try {
      return JSON.parse(localStorage.getItem(KEY))
    } catch {
      return null
    }
  },
  set(value) {
    localStorage.setItem(KEY, JSON.stringify(value))
  },
  clear() {
    localStorage.removeItem(KEY)
  },
}

const expiryOf = (jwt) => {
  try {
    const payload = jwt.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    return JSON.parse(atob(payload)).exp * 1000
  } catch {
    return 0
  }
}

let refreshing = null

async function freshAccessToken() {
  const current = tokens.get()
  if (!current) return null
  if (expiryOf(current.accessToken) - Date.now() > 30_000) return current.accessToken

  refreshing ??= axios
    .post('/api/auth/refresh', { refreshToken: current.refreshToken })
    .then((res) => {
      tokens.set(res.data)
      return res.data.accessToken
    })
    .catch((err) => {
      tokens.clear()
      window.dispatchEvent(new Event('auth:expired'))
      throw err
    })
    .finally(() => {
      refreshing = null
    })
  return refreshing
}

const api = axios.create({ baseURL: '/api' })

api.interceptors.request.use(async (config) => {
  if (!config.url.startsWith('/auth/')) {
    const token = await freshAccessToken().catch(() => null)
    if (token) config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export function errorMessage(err, fallback = 'Something went wrong') {
  const data = err?.response?.data
  if (data?.errors) return Object.values(data.errors).join(' · ')
  if (data?.error) return data.error
  if (err?.code === 'ERR_NETWORK') return 'Cannot reach the server'
  return fallback
}

export default api
