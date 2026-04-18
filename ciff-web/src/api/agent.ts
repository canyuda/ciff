import { get, post, put, del } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { ToolVO } from './tool'

export interface AgentVO {
  id?: number
  name: string
  description?: string
  type: string
  modelId: number
  modelName?: string
  workflowId?: number | null
  systemPrompt: string
  modelParams?: Record<string, unknown> | null
  fallbackModelId?: number | null
  fallbackModelName?: string
  status: string
  tools?: ToolVO[]
  createTime?: string
  updateTime?: string
  [key: string]: unknown
}

export interface AgentCreateRequest {
  name: string
  description?: string
  type: string
  modelId: number
  workflowId?: number | null
  systemPrompt: string
  modelParams?: Record<string, unknown> | null
  fallbackModelId?: number | null
  toolIds?: number[]
}

export interface AgentUpdateRequest {
  name?: string
  description?: string
  type?: string
  modelId?: number
  workflowId?: number | null
  systemPrompt?: string
  modelParams?: Record<string, unknown> | null
  fallbackModelId?: number | null
  status?: string
  toolIds?: number[]
}

export function getAgents(params: {
  page: number
  pageSize: number
  type?: string
  status?: string
}) {
  return get<PageResult<AgentVO>>('/v1/app/agents', params)
}

export function getAgentById(id: number) {
  return get<AgentVO>(`/v1/app/agents/${id}`)
}

export function createAgent(data: AgentCreateRequest) {
  return post<AgentVO>('/v1/app/agents', data)
}

export function updateAgent(id: number, data: AgentUpdateRequest) {
  return put<AgentVO>(`/v1/app/agents/${id}`, data)
}

export function deleteAgent(id: number) {
  return del(`/v1/app/agents/${id}`)
}

export function bindTool(agentId: number, toolId: number) {
  return post(`/v1/app/agents/${agentId}/tools/${toolId}`)
}

export function unbindTool(agentId: number, toolId: number) {
  return del(`/v1/app/agents/${agentId}/tools/${toolId}`)
}

export function replaceAgentTools(agentId: number, toolIds: number[]) {
  return put(`/v1/app/agents/${agentId}/tools`, toolIds)
}
