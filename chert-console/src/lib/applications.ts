import { apiRequest } from '@/lib/api'

type PageResponse<T> = {
  content: T[]
  last: boolean
  number: number
  totalPages: number
}

export type Application = {
  id: number
  appId: string
  name: string
  description: string | null
  createdAt: string
  updatedAt: string
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
