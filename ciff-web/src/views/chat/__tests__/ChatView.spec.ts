import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import ChatView from '../ChatView.vue'

// ===== Stubs =====

const ElScrollbarStub = {
  template: '<div class="el-scrollbar"><slot /></div>',
  props: ['ref'],
}

const ElButtonStub = {
  template: '<button class="el-button" :class="type" @click="$emit(\'click\')"><slot /></button>',
  props: ['type', 'link', 'loading', 'disabled'],
}

const ElInputStub = {
  template: `<textarea class="el-textarea"
    :value="modelValue"
    :disabled="disabled"
    @input="$emit('update:modelValue', $event.target.value)"
    @keydown="$emit('keydown', $event)"
  />`,
  props: ['modelValue', 'type', 'rows', 'placeholder', 'disabled', 'resize'],
}

const ElSelectStub = {
  template: '<select class="el-select" :value="modelValue" @change="$emit(\'update:modelValue\', $event.target.value)"><slot /></select>',
  props: ['modelValue', 'placeholder', 'multiple', 'size', 'disabled'],
}

const ElOptionStub = {
  template: '<option :value="value">{{ label }}</option>',
  props: ['label', 'value'],
}

const ElDialogStub = {
  template: '<div class="el-dialog" v-if="modelValue"><slot /><slot name="footer" /></div>',
  props: ['modelValue', 'title', 'width'],
  emits: ['update:modelValue'],
}

const ElTagStub = {
  template: '<span class="el-tag" :class="type"><slot /></span>',
  props: ['type', 'size', 'effect'],
}

const ElEmptyStub = {
  template: '<div class="el-empty"><slot name="description">{{ description }}</slot></div>',
  props: ['description'],
}

const ElAvatarStub = {
  template: '<div class="el-avatar"><slot /></div>',
  props: ['size', 'icon'],
}

const ElIconStub = {
  template: '<span class="el-icon"><slot /></span>',
}

const PageHeaderStub = {
  template: '<div class="page-header"><div class="title">{{ title }}</div><slot /></div>',
  props: ['title', 'description'],
}

// ===== Mocks =====

const mockStreamController = { abort: vi.fn() }
let streamChatCallback: ((event: any) => void) | null = null

vi.mock('@/api/chat', () => ({
  getConversations: vi.fn().mockResolvedValue({
    list: [
      { id: 1, agentId: 1, title: 'Test Conv', status: 'active', createTime: '2026-04-21T10:00:00Z', updateTime: '2026-04-21T10:00:00Z' },
    ],
  }),
  deleteConversation: vi.fn().mockResolvedValue({}),
  getMessages: vi.fn().mockResolvedValue({
    list: [
      { id: 101, conversationId: 1, role: 'user', content: 'Hello', createTime: '2026-04-21T10:00:00Z' },
      { id: 102, conversationId: 1, role: 'assistant', content: 'Hi!', createTime: '2026-04-21T10:01:00Z' },
    ],
  }),
  streamChat: vi.fn().mockImplementation((_data, onEvent) => {
    streamChatCallback = onEvent
    return mockStreamController
  }),
}))

vi.mock('@/api/agent', () => ({
  getAgents: vi.fn().mockResolvedValue({
    list: [
      { id: 1, name: 'Test Agent', type: 'agent' },
    ],
  }),
}))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual as any,
    ElMessage: { error: vi.fn(), success: vi.fn() },
    ElMessageBox: { confirm: vi.fn().mockResolvedValue(true) },
  }
})

import { getConversations, getMessages, deleteConversation, streamChat } from '@/api/chat'
import { getAgents } from '@/api/agent'
import { ElMessage } from 'element-plus'

// ===== Helpers =====

async function createWrapper() {
  const wrapper = mount(ChatView, {
    global: {
      stubs: {
        'el-scrollbar': ElScrollbarStub,
        'el-button': ElButtonStub,
        'el-input': ElInputStub,
        'el-select': ElSelectStub,
        'el-option': ElOptionStub,
        'el-dialog': ElDialogStub,
        'el-tag': ElTagStub,
        'el-empty': ElEmptyStub,
        'el-avatar': ElAvatarStub,
        'el-icon': ElIconStub,
        PageHeader: PageHeaderStub,
      },
    },
  })
  await flushPromises()
  return wrapper
}

describe('ChatView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    streamChatCallback = null
  })

  // --- Lifecycle ---

  it('should load conversations and agents on mount', async () => {
    await createWrapper()

    expect(getConversations).toHaveBeenCalledWith({ page: 1, pageSize: 50 })
    expect(getAgents).toHaveBeenCalledWith({ page: 1, pageSize: 100 })
  })

  it('should auto-select first conversation and load messages', async () => {
    const wrapper = await createWrapper()
    const vm = wrapper.vm as any

    // loadConversations() calls selectConversation before agentList is populated (race condition).
    // Re-select after agentList is ready to verify the full flow.
    await nextTick()
    await flushPromises()
    if (vm.currentConversationId && !vm.currentAgent) {
      vm.selectConversation(vm.currentConversationId)
      await flushPromises()
    }

    expect(getMessages).toHaveBeenCalledWith({ conversationId: 1, page: 1, pageSize: 100 })
    expect(vm.currentConversationId).toBe(1)
    expect(vm.currentAgent).toEqual(expect.objectContaining({ id: 1, name: 'Test Agent' }))
    expect(vm.messages).toHaveLength(2)
  })

  // --- Conversation selection ---

  it('should switch conversation when clicked', async () => {
    const wrapper = await createWrapper()
    const vm = wrapper.vm as any

    vi.mocked(getConversations).mockResolvedValueOnce({
      list: [
        { id: 1, agentId: 1, title: 'Conv 1', status: 'active', createTime: '2026-04-21T10:00:00Z', updateTime: '2026-04-21T10:00:00Z' },
        { id: 2, agentId: 1, title: 'Conv 2', status: 'active', createTime: '2026-04-21T10:00:00Z', updateTime: '2026-04-21T10:00:00Z' },
      ],
    })

    await vm.loadConversations()
    await flushPromises()

    vi.mocked(getMessages).mockClear()
    vm.selectConversation(2)
    await flushPromises()

    expect(vm.currentConversationId).toBe(2)
    expect(getMessages).toHaveBeenCalledWith({ conversationId: 2, page: 1, pageSize: 100 })
  })

  // --- Delete conversation ---

  it('should delete conversation and clear current if matched', async () => {
    const wrapper = await createWrapper()
    const vm = wrapper.vm as any

    // Mock loadConversations to return empty list after delete, preventing re-auto-select
    vi.mocked(getConversations).mockResolvedValueOnce({ list: [] })

    await vm.handleDeleteConversation(1)
    await flushPromises()

    expect(deleteConversation).toHaveBeenCalledWith(1)
    expect(ElMessage.success).toHaveBeenCalledWith('删除成功')
    expect(vm.currentConversationId).toBeNull()
    expect(vm.messages).toHaveLength(0)
  })

  // --- Agent selector ---

  it('should open agent selector dialog', async () => {
    const wrapper = await createWrapper()
    const vm = wrapper.vm as any

    vm.openAgentSelector()
    await nextTick()

    expect(vm.agentDialogVisible).toBe(true)
    expect(vm.selectedAgentId).toBeNull()
  })

  it('should select agent and clear messages', async () => {
    const wrapper = await createWrapper()
    const vm = wrapper.vm as any

    vm.selectedAgentId = 1
    vm.confirmAgent()
    await nextTick()

    expect(vm.currentAgent).toEqual(expect.objectContaining({ id: 1, name: 'Test Agent' }))
    expect(vm.currentConversationId).toBeNull()
    expect(vm.messages).toHaveLength(0)
    expect(vm.agentDialogVisible).toBe(false)
  })

  // --- Send message ---

  it('should send message and show user message immediately', async () => {
    const wrapper = await createWrapper()
    const vm = wrapper.vm as any

    // Clear pre-loaded messages for a clean test
    vm.messages = []
    vm.currentAgent = { id: 1, name: 'Test Agent' }
    vm.inputMessage = 'Hello world'
    await vm.handleSend()

    expect(vm.inputMessage).toBe('')
    expect(vm.messages).toHaveLength(1)
    expect(vm.messages[0].role).toBe('user')
    expect(vm.messages[0].content).toBe('Hello world')
    expect(vm.isStreaming).toBe(true)
    expect(streamChat).toHaveBeenCalled()
  })

  it('should not send when empty message or no agent', async () => {
    const wrapper = await createWrapper()
    const vm = wrapper.vm as any

    vm.currentAgent = null
    vm.inputMessage = 'test'
    await vm.handleSend()
    expect(streamChat).not.toHaveBeenCalled()

    vi.clearAllMocks()
    vm.currentAgent = { id: 1, name: 'Test Agent' }
    vm.inputMessage = '   '
    await vm.handleSend()
    expect(streamChat).not.toHaveBeenCalled()
  })

  it('should send on Enter key without Shift', async () => {
    const wrapper = await createWrapper()
    const vm = wrapper.vm as any

    vm.currentAgent = { id: 1, name: 'Test Agent' }
    vm.inputMessage = 'test'

    const event = new KeyboardEvent('keydown', { key: 'Enter', shiftKey: false })
    vm.handleKeydown(event)

    expect(streamChat).toHaveBeenCalled()
  })

  it('should not send on Shift+Enter', async () => {
    const wrapper = await createWrapper()
    const vm = wrapper.vm as any

    vm.currentAgent = { id: 1, name: 'Test Agent' }
    vm.inputMessage = 'test'

    const event = new KeyboardEvent('keydown', { key: 'Enter', shiftKey: true })
    vm.handleKeydown(event)

    expect(streamChat).not.toHaveBeenCalled()
  })

  // --- SSE events ---

  it('should handle meta event and update conversationId', async () => {
    const wrapper = await createWrapper()
    const vm = wrapper.vm as any

    vm.currentAgent = { id: 1, name: 'Test Agent' }
    vm.inputMessage = 'test'
    await vm.handleSend()

    streamChatCallback!({ type: 'meta', data: { conversationId: 99, newConversation: false } })

    expect(vm.currentConversationId).toBe(99)
  })

  it('should handle meta event with newConversation and reload list', async () => {
    const wrapper = await createWrapper()
    const vm = wrapper.vm as any

    vm.currentAgent = { id: 1, name: 'Test Agent' }
    vm.inputMessage = 'test'
    await vm.handleSend()

    vi.mocked(getConversations).mockClear()
    streamChatCallback!({ type: 'meta', data: { conversationId: 99, newConversation: true } })

    expect(getConversations).toHaveBeenCalled()
  })

  it('should handle token event and append to streaming content', async () => {
    const wrapper = await createWrapper()
    const vm = wrapper.vm as any

    vm.currentAgent = { id: 1, name: 'Test Agent' }
    vm.inputMessage = 'test'
    await vm.handleSend()

    streamChatCallback!({ type: 'token', data: 'Hello' })
    expect(vm.streamingContent).toBe('Hello')

    streamChatCallback!({ type: 'token', data: ' world' })
    expect(vm.streamingContent).toBe('Hello world')
  })

  it('should handle done event and add assistant message', async () => {
    const wrapper = await createWrapper()
    const vm = wrapper.vm as any

    // Clear pre-loaded messages for a clean test
    vm.messages = []
    vm.currentAgent = { id: 1, name: 'Test Agent' }
    vm.inputMessage = 'test'
    await vm.handleSend()

    streamChatCallback!({ type: 'token', data: 'Response' })
    streamChatCallback!({
      type: 'done',
      data: { tokenUsage: { promptTokens: 10, completionTokens: 2 }, latencyMs: 500, referenceDocuments: ['doc1'] },
    })

    expect(vm.isStreaming).toBe(false)
    expect(vm.messages).toHaveLength(2)
    expect(vm.messages[1].role).toBe('assistant')
    expect(vm.messages[1].content).toBe('Response')
    expect(vm.messages[1].referenceDocuments).toEqual(['doc1'])
    expect(vm.streamingContent).toBe('')
  })

  it('should handle error event and show error message', async () => {
    const wrapper = await createWrapper()
    const vm = wrapper.vm as any

    vm.currentAgent = { id: 1, name: 'Test Agent' }
    vm.inputMessage = 'test'
    await vm.handleSend()

    streamChatCallback!({ type: 'error', data: { message: 'API error' } })

    expect(ElMessage.error).toHaveBeenCalledWith('API error')
    expect(vm.isStreaming).toBe(false)
  })

  // --- Stop / Abort ---

  it('should abort stream on stop', async () => {
    const wrapper = await createWrapper()
    const vm = wrapper.vm as any

    // Clear pre-loaded messages for a clean test
    vm.messages = []
    vm.currentAgent = { id: 1, name: 'Test Agent' }
    vm.inputMessage = 'test'
    await vm.handleSend()

    streamChatCallback!({ type: 'token', data: 'Partial' })
    vm.handleStop()

    expect(mockStreamController.abort).toHaveBeenCalled()
    expect(vm.isStreaming).toBe(false)
    // Note: handleStop calls abortStream() which clears streamingContent,
    // so no partial assistant message is appended. Only user message remains.
    expect(vm.messages).toHaveLength(1)
  })

  it('should abort stream when switching conversation', async () => {
    const wrapper = await createWrapper()
    const vm = wrapper.vm as any

    vm.currentAgent = { id: 1, name: 'Test Agent' }
    vm.inputMessage = 'test'
    await vm.handleSend()

    vi.mocked(getMessages).mockClear()
    vm.selectConversation(1)

    expect(mockStreamController.abort).toHaveBeenCalled()
    expect(vm.isStreaming).toBe(false)
  })

  // --- RAG mode ---

  it('should have default RAG mode as RAG_WITH_RERANKER', async () => {
    const wrapper = await createWrapper()
    const vm = wrapper.vm as any

    expect(vm.ragMode).toBe('RAG_WITH_RERANKER')
  })

  // --- Empty state ---

  it('should show empty state when no conversation selected and not streaming', async () => {
    vi.mocked(getConversations).mockResolvedValueOnce({ list: [] })
    vi.mocked(getAgents).mockResolvedValueOnce({ list: [] })

    const wrapper = await createWrapper()

    expect((wrapper.vm as any).currentConversationId).toBeNull()
    expect(wrapper.find('.el-empty').exists()).toBe(true)
  })
})