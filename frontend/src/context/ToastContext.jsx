import { createContext, useCallback, useContext, useState } from 'react'
import { AlertCircle, CheckCircle2, Info } from 'lucide-react'

const ToastContext = createContext(null)
export const useToast = () => useContext(ToastContext)

const icons = { success: CheckCircle2, error: AlertCircle, info: Info }

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])

  const push = useCallback((type, message) => {
    const id = Math.random().toString(36).slice(2)
    setToasts((list) => [...list, { id, type, message }])
    setTimeout(() => setToasts((list) => list.filter((t) => t.id !== id)), 4500)
  }, [])

  const api = {
    success: (m) => push('success', m),
    error: (m) => push('error', m),
    info: (m) => push('info', m),
  }

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div className="toasts" role="status" aria-live="polite">
        {toasts.map((t) => {
          const Icon = icons[t.type]
          return (
            <div key={t.id} className={`toast toast-${t.type}`}>
              <Icon size={18} />
              <span>{t.message}</span>
            </div>
          )
        })}
      </div>
    </ToastContext.Provider>
  )
}
