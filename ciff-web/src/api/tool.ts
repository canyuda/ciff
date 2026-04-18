import { get, post, put, del } from '@/utils/request'
import type { PageResult } from '@/types/common'

export interface ToolVO {
  id?: number
  name: string
  description?: string
  type: string
  endpoint: string
  paramSchema?: Record<string, unknown> | null
  authConfig?: Record<string, unknown> | null
  status: string
  createTime?: string
  updateTime?: string
  [key: string]: unknown
}

export interface ToolCreateRequest {
  name: string
  description?: string
  type: string
  endpoint: string
  paramSchema?: Record<string, unknown> | null
  authConfig?: Record<string, unknown> | null
}

export interface ToolUpdateRequest {
  name?: string
  description?: string
  type?: string
  endpoint?: string
  paramSchema?: Record<string, unknown> | null
  authConfig?: Record<string, unknown> | null
  status?: string
}

export function getTools(params: {
  page: number
  pageSize: number
  type?: string
  status?: string
}) {
  return get<PageResult<ToolVO>>('/v1/tools', params)
}

export function getToolById(id: number) {
  return get<ToolVO>(`/v1/tools/${id}`)
}

export function createTool(data: ToolCreateRequest) {
  return post<ToolVO>('/v1/tools', data)
}

export function updateTool(id: number, data: ToolUpdateRequest) {
  return put<ToolVO>(`/v1/tools/${id}`, data)
}

export function deleteTool(id: number) {
  return del(`/v1/tools/${id}`)
}
