import { get, post, put, del } from '@/utils/request'
import type { PageResult } from '@/types/common'

export interface ProviderAuthConfig {
  apiVersion?: string
  tokenTtl?: number
  [key: string]: unknown
}

export interface ProviderHealthVO {
  providerId: number
  providerName?: string
  status: string
  consecutiveFailures?: number
  lastLatencyMs?: number
  lastSuccessTime?: string
  lastFailureTime?: string
  lastFailureReason?: string
}

export interface Provider {
  id?: number
  name: string
  type: string
  typeDisplayName?: string
  authType: string
  apiBaseUrl: string
  apiKeyMasked?: string
  apiKey?: string
  authConfig?: ProviderAuthConfig | null
  status: string
  models?: unknown[]
  health?: ProviderHealthVO
  createTime?: string
  updateTime?: string
  [key: string]: unknown
}

export interface ProviderCreateRequest {
  name: string
  type: string
  authType: string
  apiBaseUrl: string
  apiKey?: string
  authConfig?: ProviderAuthConfig | null
}

export interface ProviderUpdateRequest {
  name?: string
  type?: string
  authType?: string
  apiBaseUrl?: string
  apiKey?: string
  authConfig?: ProviderAuthConfig | null
  status?: string
}

export function getProviders(params: {
  page: number
  pageSize: number
  type?: string
  status?: string
}) {
  return get<PageResult<Provider>>('/v1/providers', params)
}

export function getProviderById(id: number) {
  return get<Provider>(`/v1/providers/${id}`)
}

export function createProvider(data: ProviderCreateRequest) {
  return post<Provider>('/v1/providers', data)
}

export function updateProvider(id: number, data: ProviderUpdateRequest) {
  return put<Provider>(`/v1/providers/${id}`, data)
}

export function deleteProvider(id: number) {
  return del(`/v1/providers/${id}`)
}

export function getProviderHealth(id: number) {
  return get<ProviderHealthVO>(`/v1/providers/${id}/health`)
}

export interface ProviderListItem {
  id: number
  name: string
}

export function getProviderList() {
  return get<ProviderListItem[]>('/v1/providers/list')
}