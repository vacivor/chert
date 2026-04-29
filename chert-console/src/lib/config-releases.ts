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

export type ConfigReleaseRequestStatus =
  | 'PENDING'
  | 'APPROVED'
  | 'REJECTED'
  | 'WITHDRAWN'

export type ConfigReleaseRequestResponse = {
  id: number
  configResourceId: number
  environmentId: number
  status: ConfigReleaseRequestStatus
  snapshot: string
  requestComment: string | null
  requestedBy: string | null
  reviewComment: string | null
  reviewedBy: string | null
  reviewedAt: string | null
  approvedReleaseId: number | null
  createdAt: string
}

export type ConfigReleasePublishResponse = {
  outcome: 'PUBLISHED' | 'SUBMITTED_FOR_REVIEW'
  release: ConfigRelease | null
  request: ConfigReleaseRequestResponse | null
}

export type ConfigReleaseHistoryResponse = {
  id: number
  configResourceId: number
  environmentId: number
  releaseId: number
  previousReleaseId: number | null
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

export async function publishConfigRelease(
  resourceId: number,
  environmentId: number,
  comment = '',
) {
  return apiRequest<ConfigReleasePublishResponse>(
    `/api/console/config-resources/${resourceId}/environments/${environmentId}/releases`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ comment }),
    },
  )
}

export async function listConfigReleaseHistory(
  resourceId: number,
  environmentId: number,
  signal?: AbortSignal,
) {
  return apiRequest<ConfigReleaseHistoryResponse[]>(
    `/api/console/config-resources/${resourceId}/environments/${environmentId}/releases/history`,
    { signal },
  )
}

export async function rollbackConfigRelease(
  resourceId: number,
  environmentId: number,
  releaseId: number,
  comment = '',
) {
  return apiRequest<ConfigRelease>(
    `/api/console/config-resources/${resourceId}/environments/${environmentId}/releases/${releaseId}/rollback`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ comment }),
    },
  )
}
