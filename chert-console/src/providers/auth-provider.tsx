import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { ApiError } from '@/lib/api'
import {
  getCurrentUser,
  login as loginRequest,
  logout as logoutRequest,
  type ConsoleUser,
  type LoginPayload,
} from '@/lib/auth'

type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated'

type AuthContextValue = {
  errorMessage: string
  login: (payload: LoginPayload) => Promise<void>
  logout: () => Promise<void>
  refresh: () => Promise<void>
  status: AuthStatus
  user: ConsoleUser | null
}

const AuthContext = createContext<AuthContextValue | null>(null)

type AuthProviderProps = {
  children: ReactNode
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [status, setStatus] = useState<AuthStatus>('loading')
  const [user, setUser] = useState<ConsoleUser | null>(null)
  const [errorMessage, setErrorMessage] = useState('')

  async function refresh() {
    setStatus((currentStatus) =>
      currentStatus === 'authenticated' ? currentStatus : 'loading',
    )
    setErrorMessage('')

    try {
      const nextUser = await getCurrentUser()
      setUser(nextUser)
      setStatus('authenticated')
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        setUser(null)
        setStatus('unauthenticated')
        return
      }

      setUser(null)
      setStatus('unauthenticated')
      setErrorMessage(
        error instanceof Error ? error.message : 'Failed to load current session.',
      )
    }
  }

  async function login(payload: LoginPayload) {
    setErrorMessage('')
    const nextUser = await loginRequest(payload)
    setUser(nextUser)
    setStatus('authenticated')
  }

  async function logout() {
    try {
      await logoutRequest()
    } finally {
      setUser(null)
      setStatus('unauthenticated')
      setErrorMessage('')
    }
  }

  useEffect(() => {
    void refresh()
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      errorMessage,
      login,
      logout,
      refresh,
      status,
      user,
    }),
    [errorMessage, status, user],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)

  if (context === null) {
    throw new Error('useAuth must be used within AuthProvider')
  }

  return context
}
