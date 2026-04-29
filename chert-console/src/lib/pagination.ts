export type PageResponse<T> = {
  content: T[]
  last: boolean
  number: number
  totalPages: number
}
