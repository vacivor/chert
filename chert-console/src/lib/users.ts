import { apiRequest } from '@/lib/api'
import type { ConsoleUser } from '@/lib/auth'
import type { PageResponse } from '@/lib/pagination'

export type UserCreatePayload = {
  username: string
  email: string
  password: string
  roles: string[]
}

const pageSize = 100

export async function listUsers(signal?: AbortSignal) {
  const users: ConsoleUser[] = []
  let page = 0

  while (true) {
    const searchParams = new URLSearchParams({
      page: String(page),
      size: String(pageSize),
      sort: 'username,asc',
    })

    const response = await apiRequest<PageResponse<ConsoleUser>>(
      `/api/console/users?${searchParams.toString()}`,
      { signal },
    )

    users.push(...response.content)

    if (response.last || page + 1 >= response.totalPages) {
      break
    }

    page = response.number + 1
  }

  return users
}

export async function createUser(payload: UserCreatePayload) {
  return apiRequest<ConsoleUser>('/api/console/users', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  })
}
