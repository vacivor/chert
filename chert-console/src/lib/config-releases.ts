import { apiRequest } from '@/lib/api'

export type ConfigRelease = {
  id: number
  configResourceId: number
  environmentId: number
  type: string
  snapshot: string
  version: number
  comment: string | null
  createdAt: string
}

export async function getLatestConfigRelease(
  resourceId: number,
  environmentId: number,
  signal?: AbortSignal,
) {
  return apiRequest<ConfigRelease>(
    `/api/console/config-resources/${resourceId}/environments/${environmentId}/releases/latest`,
    { signal },
  )
}
