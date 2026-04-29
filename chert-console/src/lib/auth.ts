import { apiRequest } from '@/lib/api'

export type ConsoleUser = {
  id: number
  username: string
  email: string
  roles: string[]
  permissions: string[]
  createdAt: string
}

export type LoginPayload = {
  username: string
  password: string
}

export async function getCurrentUser(signal?: AbortSignal) {
  return apiRequest<ConsoleUser>('/api/auth/me', { signal })
}

export async function login(payload: LoginPayload) {
  return apiRequest<ConsoleUser>('/api/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  })
}

export async function logout() {
  return apiRequest<void>('/api/auth/logout', {
    method: 'POST',
  })
}
