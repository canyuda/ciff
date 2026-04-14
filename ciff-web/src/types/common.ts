/** Paginated response from backend APIs */
export interface PageResult<T> {
  list: T[]
  total: number
}

/** Pagination request params */
export interface PageParams {
  page: number
  pageSize: number
}

/** Table column definition */
export interface TableColumn {
  label: string
  prop?: string
  width?: number | string
  minWidth?: number | string
  slot?: string
  align?: 'left' | 'center' | 'right'
  fixed?: 'left' | 'right' | boolean
}
