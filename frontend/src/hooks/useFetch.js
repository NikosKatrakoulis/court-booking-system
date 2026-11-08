import { useCallback, useEffect, useRef, useState } from 'react'

export default function useFetch(fn, deps = [], { interval } = {}) {
  const [state, setState] = useState({ data: null, loading: true, error: null })
  const fnRef = useRef(fn)
  const latest = useRef(0)
  fnRef.current = fn

  const load = useCallback(async (silent = false) => {
    const id = ++latest.current
    if (!silent) setState((s) => ({ ...s, loading: true, error: null }))
    try {
      const data = await fnRef.current()
      if (id === latest.current) setState({ data, loading: false, error: null })
    } catch (error) {
      if (id === latest.current) setState((s) => ({ ...s, loading: false, error }))
    }
  }, [])

  useEffect(() => {
    load()
    if (!interval) return undefined
    const timer = setInterval(() => load(true), interval)
    return () => clearInterval(timer)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps)

  return { ...state, reload: () => load(true), refetch: () => load(false) }
}
