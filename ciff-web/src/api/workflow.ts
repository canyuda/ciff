import { get, post, put, del } from '@/utils/request'
import type { PageResult } from '@/types/common'

export interface StepDefinition {
  id: string
  type: string // llm / tool / condition / knowledge_retrieval
  name: string
  config?: Record<string, unknown>
  dependsOn?: string[]
  nextStepId?: string
  outputs?: Record<string, string>
}

export interface WorkflowDefinition {
  steps: StepDefinition[]
  inputs?: Record<string, unknown>
}

export interface WorkflowVO {
  id?: number
  name: string
  description?: string
  definition?: WorkflowDefinition
  status: string
  createTime?: string
  updateTime?: string
  [key: string]: unknown
}

export interface WorkflowCreateRequest {
  name: string
  description?: string
  definition: WorkflowDefinition
  status?: string
}

export interface WorkflowUpdateRequest {
  name?: string
  description?: string
  definition?: WorkflowDefinition
  status?: string
}

export interface WorkflowExecutionResult {
  success: boolean
  error?: string
  stepResults?: StepResult[]
  finalOutputs?: Record<string, unknown>
}

export interface StepResult {
  stepId: string
  stepName: string
  type: string
  success: boolean
  error?: string
  outputs?: Record<string, unknown>
}

export type TaskStatus = 'STARTED' | 'RUNNING' | 'SUCCESS' | 'FAILED' | 'TIMEOUT'

export interface WorkflowTask {
  taskId: string
  workflowId: number
  userId: number
  status: TaskStatus
  currentStepId?: string
  currentStepName?: string
  completedSteps: number
  totalSteps: number
  startTime: string
  endTime?: string
}

export interface WorkflowTaskDetail {
  taskId: string
  workflowId: number
  status: TaskStatus
  currentStepId?: string
  currentStepName?: string
  completedSteps: number
  totalSteps: number
  inputs?: Record<string, unknown>
  stepResults?: StepResult[]
  finalOutputs?: Record<string, unknown>
  error?: string
  startTime: string
  endTime?: string
}

export function getWorkflows(params: {
  page: number
  pageSize: number
  status?: string
}) {
  return get<PageResult<WorkflowVO>>('/v1/app/workflows', params)
}

export function getWorkflowById(id: number) {
  return get<WorkflowVO>(`/v1/app/workflows/${id}`)
}

export function createWorkflow(data: WorkflowCreateRequest) {
  return post<WorkflowVO>('/v1/app/workflows', data)
}

export function updateWorkflow(id: number, data: WorkflowUpdateRequest) {
  return put<WorkflowVO>(`/v1/app/workflows/${id}`, data)
}

export function deleteWorkflow(id: number) {
  return del(`/v1/app/workflows/${id}`)
}

export function submitWorkflow(id: number, inputs?: Record<string, unknown>) {
  return post<WorkflowTask>(`/v1/app/workflows/${id}/execute`, inputs ?? {})
}

export function getWorkflowTasks(workflowId: number) {
  return get<WorkflowTask[]>(`/v1/app/workflows/${workflowId}/tasks`)
}

export function getWorkflowTaskDetail(workflowId: number, taskId: string) {
  return get<WorkflowTaskDetail>(`/v1/app/workflows/${workflowId}/tasks/${taskId}`)
}
