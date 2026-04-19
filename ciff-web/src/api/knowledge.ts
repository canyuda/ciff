import { get, post, put, del } from '@/utils/request'
import type { PageResult } from '@/types/common'

export interface KnowledgeVO {
  id?: number
  name: string
  description?: string
  chunkSize?: number
  embeddingModel?: string
  status: string
  documentCount?: number
  createTime?: string
  updateTime?: string
}

export interface KnowledgeCreateRequest {
  name: string
  description?: string
  chunkSize?: number
  embeddingModel: string
}

export interface KnowledgeUpdateRequest {
  name?: string
  description?: string
  chunkSize?: number
  embeddingModel?: string
  status?: string
}

export interface DocumentVO {
  id?: number
  knowledgeId?: number
  fileName?: string
  fileSize?: number
  chunkCount?: number
  status?: string
  createTime?: string
  updateTime?: string
}

export function getKnowledgeList(params: {
  page: number
  pageSize: number
  status?: string
}) {
  return get<PageResult<KnowledgeVO>>('/v1/app/knowledge', params)
}

export function getKnowledgeById(id: number) {
  return get<KnowledgeVO>(`/v1/app/knowledge/${id}`)
}

export function createKnowledge(data: KnowledgeCreateRequest) {
  return post<KnowledgeVO>('/v1/app/knowledge', data)
}

export function updateKnowledge(id: number, data: KnowledgeUpdateRequest) {
  return put<KnowledgeVO>(`/v1/app/knowledge/${id}`, data)
}

export function deleteKnowledge(id: number) {
  return del(`/v1/app/knowledge/${id}`)
}

export function uploadDocument(knowledgeId: number, file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return post<DocumentVO>(`/v1/app/knowledge/${knowledgeId}/documents`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function listDocuments(knowledgeId: number) {
  return get<DocumentVO[]>(`/v1/app/knowledge/${knowledgeId}/documents`)
}

export function deleteDocument(documentId: number) {
  return del(`/v1/app/knowledge/documents/${documentId}`)
}

export function processDocument(documentId: number) {
  return post(`/v1/app/knowledge/documents/${documentId}/process`)
}

export function rebuildVectors(
  knowledgeId: number,
  params?: { documentId?: number; chunkSize?: number; overlap?: number }
) {
  return post(`/v1/app/knowledge/${knowledgeId}/rebuild`, null, { params })
}
