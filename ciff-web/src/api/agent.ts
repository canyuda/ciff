import { get, post, put, del } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { ToolVO } from './tool'
import type { KnowledgeVO } from './knowledge'

export const DEFAULT_MODEL_PARAMS: AgentModelParam = {
  temperature: 0.7,
  maxTokens: 4096,
  maxContextTurns: 5,
}

export interface AgentModelParam {
  temperature?: number | null
  maxTokens?: number | null
  maxContextTurns?: number | null
}

export interface AgentVO {
  id?: number
  name: string
  description?: string
  type: string
  modelId: number
  modelName?: string
  workflowId?: number | null
  systemPrompt: string
  modelParams?: AgentModelParam | null
  status: string
  tools?: ToolVO[]
  knowledges?: KnowledgeVO[]
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
  modelParams?: AgentModelParam | null
  toolIds?: number[]
  knowledgeIds?: number[]
}

export interface AgentUpdateRequest {
  name?: string
  description?: string
  type?: string
  modelId?: number
  workflowId?: number | null
  systemPrompt?: string
  modelParams?: AgentModelParam | null
  status?: string
  toolIds?: number[]
  knowledgeIds?: number[]
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

export function bindKnowledge(agentId: number, knowledgeId: number) {
  return post(`/v1/app/agents/${agentId}/knowledges/${knowledgeId}`)
}

export function unbindKnowledge(agentId: number, knowledgeId: number) {
  return del(`/v1/app/agents/${agentId}/knowledges/${knowledgeId}`)
}

export function replaceAgentKnowledges(agentId: number, knowledgeIds: number[]) {
  return put(`/v1/app/agents/${agentId}/knowledges`, knowledgeIds)
}
