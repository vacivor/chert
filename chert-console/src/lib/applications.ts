import { apiRequest } from '@/lib/api'
import type { PageResponse } from '@/lib/pagination'

export type ApplicationUserSummary = {
  id: number
  username: string
  email: string
}

export type Application = {
  id: number
  appId: string
  name: string
  description: string | null
  owner: ApplicationUserSummary
  maintainer: ApplicationUserSummary
  developers: ApplicationUserSummary[]
  createdAt: string
  updatedAt: string
}

export type ApplicationCreatePayload = {
  appId: string
  name: string
  description: string
  ownerUserId: number
  maintainerUserId: number
  developerUserIds: number[]
}

export type ApplicationPublishPolicy = {
  id: number | null
  applicationId: number
  environmentId: number
  publishRequiresApproval: boolean
  createdAt: string | null
  updatedAt: string | null
}

type ListApplicationsOptions = {
  signal?: AbortSignal
}

const pageSize = 100

export async function listApplications({
  signal,
}: ListApplicationsOptions = {}): Promise<Application[]> {
  const applications: Application[] = []
  let page = 0

  while (true) {
    const searchParams = new URLSearchParams({
      page: String(page),
      size: String(pageSize),
      sort: 'name,asc',
    })

    const response = await apiRequest<PageResponse<Application>>(
      `/api/console/applications?${searchParams.toString()}`,
      { signal },
    )

    applications.push(...response.content)

    if (response.last || page + 1 >= response.totalPages) {
      break
    }

    page = response.number + 1
  }

  return applications
}

export async function createApplication(payload: ApplicationCreatePayload) {
  return apiRequest<Application>('/api/console/applications', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  })
}

export async function getApplication(applicationId: number, signal?: AbortSignal) {
  return apiRequest<Application>(`/api/console/applications/${applicationId}`, {
    signal,
  })
}

export async function getApplicationPublishPolicy(
  applicationId: number,
  environmentId: number,
) {
  return apiRequest<ApplicationPublishPolicy>(
    `/api/console/applications/${applicationId}/environments/${environmentId}/publish-policy`,
  )
}

export async function saveApplicationPublishPolicy(
  applicationId: number,
  environmentId: number,
  publishRequiresApproval: boolean,
) {
  return apiRequest<ApplicationPublishPolicy>(
    `/api/console/applications/${applicationId}/environments/${environmentId}/publish-policy`,
    {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        publishRequiresApproval,
      }),
    },
  )
}
