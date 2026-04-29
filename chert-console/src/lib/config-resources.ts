import { apiRequest } from '@/lib/api'

export type ConfigType = 'CONTENT' | 'ENTRIES'
export type ConfigFormat = 'YAML' | 'PROPERTIES' | 'JSON' | 'TOML' | 'XML' | 'NONE'

export type ConfigResource = {
  id: number
  applicationId: number
  name: string
  type: ConfigType
  format: ConfigFormat
  version: number | null
  description: string | null
  createdAt: string
  updatedAt: string
}

export type ConfigResourceCreatePayload = {
  configName: string
  type: ConfigType
  format: ConfigFormat
  version: number | null
  description: string
}

export async function listConfigResources(
  applicationId: number,
  signal?: AbortSignal,
) {
  return apiRequest<ConfigResource[]>(
    `/api/console/applications/${applicationId}/config-resources`,
    { signal },
  )
}

export async function createConfigResource(
  applicationId: number,
  payload: ConfigResourceCreatePayload,
) {
  return apiRequest<ConfigResource>(
    `/api/console/applications/${applicationId}/config-resources`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    },
  )
}
