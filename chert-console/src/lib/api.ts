export type ProblemDetail = {
  detail?: string
  title?: string
  errors?: Array<{
    code?: string
    field?: string
    message?: string
  }>
}

export class ApiError extends Error {
  status: number
  title?: string
  errors?: ProblemDetail['errors']

  constructor(
    status: number,
    message: string,
    options?: {
      title?: string
      errors?: ProblemDetail['errors']
    },
  ) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.title = options?.title
    this.errors = options?.errors
  }
}

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, '') ?? ''

function resolveApiUrl(path: string) {
  return `${apiBaseUrl}${path}`
}

export async function apiRequest<T>(
  path: string,
  init?: RequestInit,
): Promise<T> {
  const response = await fetch(resolveApiUrl(path), {
    credentials: 'include',
    ...init,
    headers: {
      Accept: 'application/json',
      ...init?.headers,
    },
  })

  if (!response.ok) {
    let message = `Request failed with status ${response.status}`
    let title: string | undefined
    let errors: ProblemDetail['errors']

    try {
      const problem = (await response.json()) as ProblemDetail
      message = problem.detail ?? problem.title ?? message
      title = problem.title
      errors = problem.errors
    } catch {
      // Ignore JSON parsing failures and fall back to the generic message.
    }

    throw new ApiError(response.status, message, { title, errors })
  }

  if (response.status === 204) {
    return undefined as T
  }

  const contentType = response.headers.get('content-type')
  if (!contentType?.includes('application/json')) {
    return undefined as T
  }

  return (await response.json()) as T
}
