import { get, post, del } from '@/utils/request'
import type { PageResult } from '@/types/common'

export interface ConversationVO {
  id: number
  agentId: number
  agentName?: string
  title: string
  status: string
  createTime: string
  updateTime: string
}

export interface ChatMessageVO {
  id: number
  conversationId: number
  role: 'user' | 'assistant' | 'tool'
  content: string
  tokenUsage?: {
    promptTokens: number
    completionTokens: number
    totalTokens: number
  }
  modelName?: string
  latencyMs?: number
  referenceDocuments?: string[]
  createTime: string
}

export interface ChatRequest {
  agentId: number
  message: string
  conversationId?: number | null
  ragMode?: 'RAG_WITH_RERANKER' | 'RAG_WITHOUT_RERANKER' | 'NO_RAG'
}

export interface ChatResponse {
  conversationId: number
  newConversation: boolean
  userMessageId: number
  assistantMessageId: number
  content: string
  tokenUsage: {
    promptTokens: number
    completionTokens: number
    totalTokens: number
  }
  modelName: string
  latencyMs: number
  referenceDocuments?: string[]
}

export interface SseMetaEvent {
  conversationId: number
  newConversation: boolean
}

export interface SseDoneEvent {
  tokenUsage: {
    promptTokens: number
    completionTokens: number
  }
  latencyMs: number
  referenceDocuments?: string[]
}

export interface SseErrorEvent {
  message: string
}

export type SseEvent =
  | { type: 'meta'; data: SseMetaEvent }
  | { type: 'token'; data: string }
  | { type: 'done'; data: SseDoneEvent }
  | { type: 'error'; data: SseErrorEvent }

// ========== Conversation APIs ==========

export function getConversations(params: {
  page?: number
  pageSize?: number
  agentId?: number
}) {
  return get<PageResult<ConversationVO>>('/v1/conversations', params)
}

export function deleteConversation(id: number) {
  return del(`/v1/conversations/${id}`)
}

// ========== Message APIs ==========

export function getMessages(params: {
  conversationId: number
  page?: number
  pageSize?: number
}) {
  return get<PageResult<ChatMessageVO>>(
    `/v1/conversations/${params.conversationId}/messages`,
    { page: params.page, pageSize: params.pageSize }
  )
}

// ========== Chat APIs ==========

export function sendChat(data: ChatRequest) {
  return post<ChatResponse>('/v1/app/chat', data)
}

/**
 * SSE stream chat. Returns an abort controller so caller can cancel.
 */
export function streamChat(
  data: ChatRequest,
  onEvent: (event: SseEvent) => void
): AbortController {
  const controller = new AbortController()

  fetch('/api/v1/app/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-User-Id': '1',
    },
    body: JSON.stringify(data),
    signal: controller.signal,
  })
    .then(async (response) => {
      if (!response.ok) {
        const text = await response.text()
        onEvent({ type: 'error', data: { message: `HTTP ${response.status}: ${text}` } })
        return
      }

      const reader = response.body!.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      try {
        while (true) {
          const { done, value } = await reader.read()
          if (done) break

          buffer += decoder.decode(value, { stream: true })

          // SSE events are separated by double newlines
          const events = parseSseEvents(buffer)
          buffer = events.remainder

          for (const event of events.parsed) {
            switch (event.name) {
              case 'meta':
                onEvent({ type: 'meta', data: JSON.parse(event.data) })
                break
              case 'token':
                onEvent({ type: 'token', data: JSON.parse(event.data) })
                break
              case 'done':
                onEvent({ type: 'done', data: JSON.parse(event.data) })
                break
              case 'error':
                onEvent({ type: 'error', data: JSON.parse(event.data) })
                break
            }
          }
        }
      } catch (e: any) {
        if (e.name !== 'AbortError') {
          onEvent({ type: 'error', data: { message: e.message || 'Stream error' } })
        }
      }
    })
    .catch((e: any) => {
      if (e.name !== 'AbortError') {
        onEvent({ type: 'error', data: { message: e.message || 'Network error' } })
      }
    })

  return controller
}

interface ParsedSseEvent {
  name: string
  data: string
}

interface SseParseResult {
  parsed: ParsedSseEvent[]
  remainder: string
}

function parseSseEvents(buffer: string): SseParseResult {
  const parsed: ParsedSseEvent[] = []
  const chunks = buffer.split('\n\n')
  const remainder = chunks.pop() || ''

  for (const chunk of chunks) {
    const lines = chunk.split('\n')
    let name = 'message'
    let data = ''

    for (const line of lines) {
      if (line.startsWith('event:')) {
        name = line.slice(6).trim()
      } else if (line.startsWith('data:')) {
        data = line.slice(5).trim()
      }
    }

    if (data) {
      parsed.push({ name, data })
    }
  }

  return { parsed, remainder }
}
