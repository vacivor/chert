import { apiRequest } from '@/lib/api'

export type Environment = {
  id: number
  code: string
  name: string
  description: string | null
  createdAt: string
  updatedAt: string
}

export type EnvironmentCreatePayload = {
  code: string
  name: string
  description: string
}

export async function listEnvironments(signal?: AbortSignal) {
  return apiRequest<Environment[]>('/api/console/environments', { signal })
}

export async function createEnvironment(payload: EnvironmentCreatePayload) {
  return apiRequest<Environment>('/api/console/environments', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  })
}
