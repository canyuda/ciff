import { get, post, del } from '@/utils/request'

export interface ApiKeyVO {
  id: number
  name: string
  keyPrefix: string
  agentId: number
  agentName?: string
  status: string
  expiresAt: string | null
  createTime: string
  rawKey?: string
}

export interface ApiKeyCreateRequest {
  name: string
  agentId: number
  expiresAt?: string | null
}

export function listApiKeys(): Promise<ApiKeyVO[]> {
  return get<ApiKeyVO[]>('/keys')
}

export function createApiKey(data: ApiKeyCreateRequest): Promise<ApiKeyVO> {
  return post<ApiKeyVO>('/keys', data)
}

export function revokeApiKey(id: number): Promise<void> {
  return del<void>(`/keys/${id}`)
}
