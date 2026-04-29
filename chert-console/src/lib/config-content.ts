import { apiRequest } from '@/lib/api'

export type ConfigContent = {
  id: number
  resourceId: number
  environmentId: number
  content: string
  createdAt: string
  updatedAt: string
}

export type ConfigDiff = {
  oldContent: string | null
  newContent: string | null
  hasChanges: boolean
}

export async function getLatestConfigContent(
  resourceId: number,
  environmentId: number,
  signal?: AbortSignal,
) {
  return apiRequest<ConfigContent>(
    `/api/console/config-resources/${resourceId}/environments/${environmentId}/contents/latest`,
    { signal },
  )
}

export async function saveConfigContent(
  resourceId: number,
  environmentId: number,
  content: string,
) {
  return apiRequest<ConfigContent>(
    `/api/console/config-resources/${resourceId}/environments/${environmentId}/contents`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ content }),
    },
  )
}

export async function getConfigContentDiff(
  resourceId: number,
  environmentId: number,
  signal?: AbortSignal,
) {
  return apiRequest<ConfigDiff>(
    `/api/console/config-resources/${resourceId}/environments/${environmentId}/contents/diff`,
    { signal },
  )
}
