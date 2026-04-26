<template>
  <div class="chat-page">
    <!-- Left sidebar: conversation list -->
    <aside class="chat-sidebar">
      <div class="sidebar-header">
        <el-button type="primary" style="width: 100%" @click="openAgentSelector">
          <el-icon><Plus /></el-icon>
          <span style="margin-left: 4px">新会话</span>
        </el-button>
      </div>
      <el-scrollbar class="conversation-list">
        <div
          v-for="conv in conversations"
          :key="conv.id"
          :class="['conversation-item', { active: conv.id === currentConversationId }]"
          @click="selectConversation(conv.id)"
        >
          <div class="conversation-title">{{ conv.title }}</div>
          <div class="conversation-meta">
            <span class="agent-name">{{ conv.agentName }}</span>
            <span class="conv-time">{{ formatDate(conv.updateTime) }}</span>
          </div>
          <el-icon class="delete-btn" @click.stop="handleDeleteConversation(conv.id)">
            <Delete />
          </el-icon>
        </div>
        <div v-if="conversations.length === 0" class="conv-empty">
          <span>暂无会话</span>
        </div>
      </el-scrollbar>
    </aside>

    <!-- Right: chat area -->
    <main class="chat-main">
      <!-- Header -->
      <div v-if="currentAgent" class="chat-header">
        <span class="chat-agent-name">{{ currentAgent.name }}</span>
        <el-tag v-if="currentAgent.type" size="small" :type="currentAgent.type === 'agent' ? 'success' : 'primary'">
          {{ currentAgent.type }}
        </el-tag>
      </div>

      <!-- Message list -->
      <el-scrollbar ref="messageScrollRef" class="message-list" :key="scrollKey">
        <div v-if="messages.length === 0 && !isStreaming" class="empty-state">
          <el-empty :description="currentAgent ? '开始发送消息...' : '选择 Agent 开始新会话'" />
        </div>

        <div v-for="msg in messages" :key="msg.id" :class="['message-row', msg.role]">
          <div class="message-avatar">
            <el-avatar v-if="msg.role === 'assistant'" :size="32" :icon="Service" />
            <el-avatar v-else :size="32" :icon="User" />
          </div>
          <div class="message-bubble">
            <div v-if="msg.role === 'assistant'" class="message-content markdown-body" v-html="renderMarkdown(msg.content)"></div>
            <div v-else class="message-content">{{ msg.content }}</div>
            <div class="message-meta">
              <template v-if="msg.role === 'assistant'">
                <span v-if="msg.modelName" class="model-tag">{{ msg.modelName }}</span>
                <span v-if="msg.referenceDocuments && msg.referenceDocuments.length > 0" class="ref-docs">
                  <el-icon><Document /></el-icon>
                  {{ msg.referenceDocuments.join('，') }}
                </span>
              </template>
              <span class="msg-time">{{ formatTime(msg.createTime) }}</span>
            </div>
          </div>
        </div>

        <!-- Streaming temporary message -->
        <div v-if="isStreaming" class="message-row assistant">
          <div class="message-avatar">
            <el-avatar :size="32" :icon="Service" />
          </div>
          <div class="message-bubble streaming">
            <div class="message-content">{{ streamingContent }}</div>
            <div class="message-meta">
              <span class="streaming-indicator">
                <el-icon class="is-loading"><Loading /></el-icon>
                生成中...
              </span>
            </div>
          </div>
        </div>
      </el-scrollbar>

      <!-- Input area -->
      <div class="input-area">
        <div v-if="!currentAgent" class="input-placeholder">
          <el-button type="primary" @click="openAgentSelector">
            <el-icon><Plus /></el-icon>
            选择 Agent 开始新会话
          </el-button>
        </div>
        <template v-else>
          <div class="input-toolbar">
            <span class="toolbar-label">RAG 模式</span>
            <el-select v-model="ragMode" size="small" style="width: 160px">
              <el-option label="RAG + 精排" value="RAG_WITH_RERANKER" />
              <el-option label="RAG" value="RAG_WITHOUT_RERANKER" />
              <el-option label="关闭 RAG" value="NO_RAG" />
            </el-select>
          </div>
          <div class="input-wrapper">
            <el-input
              v-model="inputMessage"
              type="textarea"
              :rows="3"
              placeholder="输入消息，Shift + Enter 换行，Enter 发送"
              :disabled="isStreaming"
            resize="none"
            @keydown="handleKeydown"
          />
          <div class="input-actions">
            <el-button
              v-if="isStreaming"
              type="danger"
              @click="handleStop"
            >
              <el-icon><VideoPause /></el-icon>
              停止
            </el-button>
            <el-button
              v-else
              type="primary"
              :disabled="!inputMessage.trim() || !currentAgent"
              @click="handleSend"
            >
              <el-icon><Promotion /></el-icon>
              发送
            </el-button>
          </div>
        </div>
        </template>
      </div>
    </main>

    <!-- Agent selector dialog -->
    <el-dialog v-model="agentDialogVisible" title="选择 Agent" width="480px">
      <el-select v-model="selectedAgentId" placeholder="请选择 Agent" style="width: 100%">
        <el-option
          v-for="agent in agentList"
          :key="agent.id"
          :label="agent.name"
          :value="agent.id!"
        >
          <span>{{ agent.name }}</span>
          <el-tag size="small" style="margin-left: 8px" :type="agent.type === 'agent' ? 'success' : 'primary'">
            {{ agent.type }}
          </el-tag>
        </el-option>
      </el-select>
      <template #footer>
        <el-button @click="agentDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!selectedAgentId" @click="confirmAgent">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  Delete,
  Promotion,
  VideoPause,
  Loading,
  User,
  Service,
  Document,
} from '@element-plus/icons-vue'
import { renderMarkdown } from '@/utils/markdown'
import {
  getConversations,
  deleteConversation,
  getMessages,
  streamChat,
  type ConversationVO,
  type ChatMessageVO,
  type SseEvent,
} from '@/api/chat'
import { getAgents, type AgentVO } from '@/api/agent'
import type { ElScrollbar } from 'element-plus'

// ========== State ==========
const conversations = ref<ConversationVO[]>([])
const messages = ref<ChatMessageVO[]>([])
const agentList = ref<AgentVO[]>([])
const currentConversationId = ref<number | null>(null)
const currentAgent = ref<AgentVO | null>(null)
const inputMessage = ref('')
const isStreaming = ref(false)
const streamingContent = ref('')
const streamingRefDocs = ref<string[]>([])
const agentDialogVisible = ref(false)
const selectedAgentId = ref<number | null>(null)
const ragMode = ref<'RAG_WITH_RERANKER' | 'RAG_WITHOUT_RERANKER' | 'NO_RAG'>('RAG_WITH_RERANKER')
const messageScrollRef = ref<InstanceType<typeof ElScrollbar>>()
const scrollKey = ref(0)
let streamController: AbortController | null = null

// ========== Lifecycle ==========
onMounted(async () => {
  // agents must load first — selectConversation needs agentList to set currentAgent
  await loadAgents()
  await loadConversations()
})

// ========== Data loading ==========
async function loadConversations() {
  try {
    const res = await getConversations({ page: 1, pageSize: 50 })
    conversations.value = res.list
    // Auto-select last (most recent) conversation if none selected
    if (conversations.value.length > 0 && !currentConversationId.value) {
      selectConversation(conversations.value[conversations.value.length - 1].id)
    }
  } catch {
    // silently fail, keep existing list
  }
}

async function loadAgents() {
  try {
    const res = await getAgents({ page: 1, pageSize: 100 })
    agentList.value = res.list
  } catch {
    // silently fail
  }
}

async function loadMessages(conversationId: number) {
  try {
    const res = await getMessages({ conversationId, page: 1, pageSize: 100 })
    // Backend returns ASC (oldest first), no need to reverse
    messages.value = res.list
    scrollToBottom()
  } catch {
    messages.value = []
  }
}

// ========== Conversation actions ==========
function selectConversation(id: number) {
  abortStream()
  currentConversationId.value = id
  const conv = conversations.value.find((c) => c.id === id)
  if (conv) {
    currentAgent.value = agentList.value.find((a) => a.id === conv.agentId) || null
  }
  loadMessages(id)
}

async function handleDeleteConversation(id: number) {
  try {
    await ElMessageBox.confirm('确定要删除该会话吗？会话内所有消息将被清除。', '确认删除', {
      type: 'warning',
    })
    await deleteConversation(id)
    ElMessage.success('删除成功')
    if (currentConversationId.value === id) {
      currentConversationId.value = null
      currentAgent.value = null
      messages.value = []
    }
    await loadConversations()
  } catch {
    // user cancelled
  }
}

// ========== Agent selection ==========
function openAgentSelector() {
  selectedAgentId.value = null
  agentDialogVisible.value = true
}

function confirmAgent() {
  if (!selectedAgentId.value) return
  const agent = agentList.value.find((a) => a.id === selectedAgentId.value)
  if (agent) {
    currentAgent.value = agent
    currentConversationId.value = null
    messages.value = []
    agentDialogVisible.value = false
  }
}

// ========== Send message ==========
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

async function handleSend() {
  const text = inputMessage.value.trim()
  if (!text || !currentAgent.value) return
  if (isStreaming.value) return

  inputMessage.value = ''

  // Add user message locally
  const userMsg: ChatMessageVO = {
    id: Date.now(),
    conversationId: currentConversationId.value || 0,
    role: 'user',
    content: text,
    createTime: new Date().toISOString(),
  }
  messages.value.push(userMsg)
  scrollToBottom()

  isStreaming.value = true
  streamingContent.value = ''
  streamingRefDocs.value = []

  streamController = streamChat(
    {
      agentId: currentAgent.value.id!,
      message: text,
      conversationId: currentConversationId.value ?? undefined,
      ragMode: ragMode.value,
    },
    handleSseEvent
  )
}

function handleSseEvent(event: SseEvent) {
  switch (event.type) {
    case 'meta': {
      currentConversationId.value = event.data.conversationId
      if (event.data.newConversation) {
        loadConversations()
      }
      break
    }
    case 'token': {
      streamingContent.value += event.data
      scrollToBottom()
      break
    }
    case 'done': {
      streamingRefDocs.value = event.data.referenceDocuments || []
      finishStream()
      break
    }
    case 'error': {
      ElMessage.error(event.data.message)
      finishStream()
      break
    }
  }
}

function finishStream() {
  isStreaming.value = false
  if (streamingContent.value) {
    messages.value.push({
      id: Date.now(),
      conversationId: currentConversationId.value || 0,
      role: 'assistant',
      content: streamingContent.value,
      modelName: currentAgent.value?.name,
      referenceDocuments: streamingRefDocs.value.length > 0 ? [...streamingRefDocs.value] : undefined,
      createTime: new Date().toISOString(),
    })
    streamingContent.value = ''
    streamingRefDocs.value = []
    scrollToBottom()
  }
}

function handleStop() {
  abortStream()
  if (streamingContent.value) {
    messages.value.push({
      id: Date.now(),
      conversationId: currentConversationId.value || 0,
      role: 'assistant',
      content: streamingContent.value + '\n\n[已停止生成]',
      createTime: new Date().toISOString(),
    })
    streamingContent.value = ''
    scrollToBottom()
  }
}

function abortStream() {
  if (streamController) {
    streamController.abort()
    streamController = null
  }
  isStreaming.value = false
  streamingContent.value = ''
}

// ========== Scroll ==========
function scrollToBottom() {
  nextTick(() => {
    const wrap = messageScrollRef.value?.$refs.wrapRef as HTMLElement | undefined
    if (wrap) {
      wrap.scrollTop = wrap.scrollHeight
    }
  })
}

// ========== Formatters ==========
function formatDate(iso: string): string {
  const d = new Date(iso)
  const now = new Date()
  if (d.toDateString() === now.toDateString()) {
    return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

function formatTime(iso: string): string {
  return new Date(iso).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.chat-page {
  display: flex;
  height: calc(100vh - var(--ciff-header-height));
  overflow: hidden;
}

/* ===== Sidebar ===== */
.chat-sidebar {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  border-right: 2px solid var(--ciff-border);
  background: var(--ciff-bg-secondary);
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid var(--ciff-border-light);
}

.conversation-list {
  flex: 1;
  overflow: auto;
}

.conversation-item {
  position: relative;
  padding: 12px 16px;
  padding-right: 48px;
  cursor: pointer;
  border-bottom: 1px solid var(--ciff-border-light);
  transition: background 0.2s;
}

.conversation-item:hover {
  background: var(--ciff-bg-tertiary);
}

.conversation-item.active {
  background: rgba(99, 102, 241, 0.08);
  border-left: 3px solid var(--ciff-primary-400);
}

.conversation-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--ciff-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}

.conversation-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: var(--ciff-text-tertiary);
}

.agent-name {
  color: var(--ciff-primary-500);
}

.delete-btn {
  position: absolute;
  top: 50%;
  right: 8px;
  transform: translateY(-50%);
  opacity: 0;
  transition: all 0.2s;
  color: var(--ciff-text-tertiary);
  cursor: pointer;
  padding: 6px;
  font-size: 18px;
  border-radius: 6px;
  background: var(--ciff-bg-primary);
  border: 1px solid var(--ciff-border-light);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.conversation-item:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  color: var(--el-color-danger);
  background: var(--el-color-danger-light-9);
  border-color: var(--el-color-danger-light-5);
}

.conv-empty {
  padding: 32px 16px;
  text-align: center;
  color: var(--ciff-text-tertiary);
  font-size: 13px;
}

/* ===== Main chat area ===== */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--ciff-bg-primary);
  min-width: 0;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  border-bottom: 1px solid var(--ciff-border-light);
  background: var(--ciff-bg-secondary);
}

.chat-agent-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--ciff-text-primary);
}

/* ===== Message list ===== */
.message-list {
  flex: 1;
  overflow: auto;
  padding: 24px 16px;
}

.message-list :deep(.el-scrollbar__wrap) {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.message-list :deep(.el-scrollbar__view) {
  width: 100%;
  max-width: 800px;
  margin: 0 auto;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.message-row {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
  max-width: 85%;
}

.message-row.user {
  margin-left: auto;
  flex-direction: row-reverse;
}

.message-row.assistant {
  margin-right: auto;
}

.message-avatar {
  flex-shrink: 0;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 12px;
  max-width: 100%;
  word-break: break-word;
}

.message-row.user .message-bubble {
  background: var(--ciff-primary-500);
  color: white;
  border-bottom-right-radius: 4px;
}

.message-row.assistant .message-bubble {
  background: var(--ciff-bg-card);
  color: var(--ciff-text-primary);
  border: 1px solid var(--ciff-border);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  border-bottom-left-radius: 4px;
}

.message-content {
  font-size: 14px;
  line-height: 1.4;
}

.markdown-body :deep(p) {
  margin: 0 0 4px;
}

.markdown-body :deep(p:last-child) {
  margin-bottom: 0;
}

.markdown-body :deep(pre.hljs) {
  background: var(--ciff-bg-primary);
  border: 1px solid var(--ciff-border-light);
  border-radius: 6px;
  padding: 12px;
  overflow-x: auto;
  margin: 8px 0;
  font-size: 13px;
  line-height: 1.5;
}

.markdown-body :deep(code) {
  font-family: var(--ciff-font-mono);
  font-size: 13px;
}

.markdown-body :deep(:not(pre) > code) {
  background: rgba(99, 102, 241, 0.1);
  color: var(--ciff-primary-600);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 20px;
  margin: 4px 0;
}

.markdown-body :deep(blockquote) {
  border-left: 3px solid var(--ciff-primary-300);
  padding-left: 12px;
  margin: 8px 0;
  color: var(--ciff-text-tertiary);
}

.markdown-body :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 8px 0;
  font-size: 13px;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid var(--ciff-border-light);
  padding: 6px 12px;
  text-align: left;
}

.markdown-body :deep(th) {
  background: var(--ciff-bg-secondary);
  font-weight: 600;
}

.markdown-body :deep(a) {
  color: var(--ciff-primary-500);
  text-decoration: none;
}

.markdown-body :deep(a:hover) {
  text-decoration: underline;
}

.message-meta {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-top: 6px;
  font-size: 11px;
}

.message-row.user .message-meta {
  justify-content: flex-end;
  color: rgba(255, 255, 255, 0.7);
}

.message-row.assistant .message-meta {
  color: var(--ciff-text-tertiary);
}

.model-tag {
  background: var(--ciff-primary-100);
  color: var(--ciff-primary-600);
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 10px;
}

.ref-docs {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--ciff-primary-500);
  font-size: 10px;
  max-width: 400px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.streaming-indicator {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--ciff-primary-400);
}

/* ===== Input area ===== */
.input-area {
  padding: 16px 24px;
  border-top: 1px solid var(--ciff-border-light);
  background: var(--ciff-bg-secondary);
}

.input-placeholder {
  display: flex;
  justify-content: center;
  padding: 8px 0;
}

.input-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  max-width: 900px;
  margin-left: auto;
  margin-right: auto;
}

.toolbar-label {
  font-size: 13px;
  color: var(--ciff-text-secondary);
}

.input-wrapper {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  max-width: 900px;
  margin: 0 auto;
}

.input-wrapper :deep(.el-textarea__inner) {
  background: var(--ciff-bg-primary);
  border-color: var(--ciff-border-light);
  color: var(--ciff-text-primary);
}

.input-wrapper :deep(.el-textarea__inner:focus) {
  border-color: var(--ciff-primary-400);
}

.input-actions {
  flex-shrink: 0;
  padding-bottom: 4px;
}
</style>
