import { get, post, put, del } from '@/utils/request'
import type { PageResult } from '@/types/common'

export interface ModelDefaultParam {
  temperature?: number
  topP?: number
  maxTokens?: number
  frequencyPenalty?: number
  presencePenalty?: number
}

export interface ModelVO {
  id?: number
  providerId: number
  providerName?: string
  name: string
  displayName?: string
  maxTokens?: number
  defaultParams?: ModelDefaultParam | null
  status: string
  createTime?: string
  updateTime?: string
  [key: string]: unknown
}

export interface ModelCreateRequest {
  providerId: number
  name: string
  displayName?: string
  maxTokens?: number
  defaultParams?: string
}

export interface ModelUpdateRequest {
  providerId?: number
  name?: string
  displayName?: string
  maxTokens?: number
  defaultParams?: string
  status?: string
}

export function getModels(params: {
  page: number
  pageSize: number
  providerId?: number
  status?: string
}) {
  return get<PageResult<ModelVO>>('/v1/models', params)
}

export function getModelById(id: number) {
  return get<ModelVO>(`/v1/models/${id}`)
}

export function getModelsByProviderId(providerId: number) {
  return get<ModelVO[]>(`/v1/models/providers/${providerId}`)
}

export function createModel(data: ModelCreateRequest) {
  return post<ModelVO>('/v1/models', data)
}

export function updateModel(id: number, data: ModelUpdateRequest) {
  return put<ModelVO>(`/v1/models/${id}`, data)
}

export function deleteModel(id: number) {
  return del(`/v1/models/${id}`)
}
