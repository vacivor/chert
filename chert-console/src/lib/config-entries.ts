import { apiRequest } from '@/lib/api'

export type ConfigEntry = {
  id: number
  key: string
  value: string
  valueType: string | null
  description: string | null
  updatedAt: string
}

export type ConfigEntryPayload = {
  key: string
  value: string
  valueType: string
  description: string
}

export async function listConfigEntries(
  resourceId: number,
  environmentId: number,
  signal?: AbortSignal,
) {
  return apiRequest<ConfigEntry[]>(
    `/api/console/config-resources/${resourceId}/environments/${environmentId}/entries`,
    { signal },
  )
}

export async function saveConfigEntry(
  resourceId: number,
  environmentId: number,
  payload: ConfigEntryPayload,
) {
  return apiRequest<ConfigEntry>(
    `/api/console/config-resources/${resourceId}/environments/${environmentId}/entries`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    },
  )
}

export async function deleteConfigEntry(
  resourceId: number,
  environmentId: number,
  entryId: number,
) {
  return apiRequest<void>(
    `/api/console/config-resources/${resourceId}/environments/${environmentId}/entries/${entryId}`,
    {
      method: 'DELETE',
    },
  )
}
