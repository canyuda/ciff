# Phase 4 执行计划：Chat 对话引擎

> **目标**：完整的对话能力——基础对话、SSE 流式、工具调用、RAG 增强。
> **涉及模块**：`ciff-chat` / `ciff-agent`（扩展） / `ciff-web`
>
> **设计决策**：`t_chat_message` 为仅追加（append-only）表，不含 `update_time` 和 `deleted` 字段。V1 不支持消息编辑/撤回/删除，如需清理通过删除整个会话实现。

---

## 一、状态快照

### 前置 Phase

| Phase | 状态 | 证据 |
|-------|------|------|
| Phase 0 | 完成 | UserContext / Flyway / 枚举 / 双数据源均就绪 |
| Phase 1 | 完成 | `mvn test -pl ciff-provider,ciff-common` 全部通过 |
| Phase 2 | 完成 | `mvn test -pl ciff-agent,ciff-mcp,ciff-app` 全部通过 |
| Phase 3 | 完成 | `mvn test -pl ciff-knowledge,ciff-agent` 全部通过 |

### Phase 4 当前状态

| 组件 | 状态 | 说明 |
|------|------|------|
| 4.1 Conversation/ChatMessage CRUD | 完成 | Entity/Mapper/Service/Controller/Facade 完整 |
| 4.2 基础对话（非流式） | 完成 | `AppChatController.chat()` → `ChatServiceImpl.chat()` |
| 4.3 SSE 流式对话 | 完成 | `AppChatController.streamChat()` → `SseEmitter` |
| 4.4 Agent 工具调用（单轮） | 完成 | `ChatServiceImpl.handleToolCall()` |
| 4.5 RAG 增强 | 完成 | `ChatServiceImpl.enhanceWithRag()` |
| 4.6 前端对话页面 | 完成 | ChatView.vue：左侧会话列表 + 消息气泡 + 输入框 + Agent 选择 + RAG 模式切换 |
| 4.7 前端 SSE 流式 | 完成 | fetch + ReadableStream 消费，打字机效果，停止生成按钮 |
| ProviderFacade LLM 配置 | 完成 | `getLlmCallConfig()` 支持 OpenAI + Claude 格式 |

---

## 二、执行步骤

### Step 1：前端 — Chat API 层

| 项 | 内容 |
|----|------|
| Task | 创建 `api/chat.ts`，封装对话相关 HTTP 调用 |
| Output | `ciff-web/src/api/chat.ts` |
| Verification | TypeScript 编译通过，接口类型与后端 Swagger 一致 |

**端点清单**：

| 方法 | 路径 | 用途 |
|------|------|------|
| GET | `/v1/conversations` | 分页查询会话列表 |
| DELETE | `/v1/conversations/{id}` | 删除会话（级联消息） |
| GET | `/v1/conversations/{id}/messages` | 分页查询会话消息 |
| POST | `/v1/app/chat` | 非流式对话 |
| POST | `/v1/app/chat/stream` | SSE 流式对话（fetch + ReadableStream） |

**类型定义**：
- `ConversationVO` / `ChatMessageVO` / `ChatRequest` / `ChatResponse`
- `SseMetaEvent` / `SseTokenEvent` / `SseDoneEvent` / `SseErrorEvent`
- `RagMode` 枚举

---

### Step 2：前端 — 对话页面基础版

| 项 | 内容 |
|----|------|
| Task | 实现 ChatView.vue：左侧会话列表 + 右侧消息区域 + 输入框 + Agent 选择 |
| Output | `ciff-web/src/views/chat/ChatView.vue`（替换占位页面） |
| Verification | 手动联调：选择 Agent → 发送消息 → 非流式回复 → 消息历史正确 |

**页面结构**：

```
┌─────────────────┬──────────────────────────────────────┐
│ 会话列表         │  消息区域                              │
│                 │                                      │
│ [Agent 选择器]   │  ┌──────────────────────────────┐   │
│                 │  │ 助手：你好，有什么可以帮你的？   │   │
│ 会话 1          │  └──────────────────────────────┘   │
│ 会话 2          │           ┌──────────────────┐      │
│ 会话 3          │           │ 用户：今天天气怎样 │      │
│                 │           └──────────────────┘      │
│ [+ 新建会话]     │                                      │
│                 │  ┌──────────────────────────────┐   │
│                 │  │ 助手：北京今天...              │   │
│                 │  └──────────────────────────────┘   │
│                 │                                      │
│                 │  ┌────────────────────────────────┐ │
│                 │  │ 输入框...              [发送]  │ │
│                 │  └────────────────────────────────┘ │
└─────────────────┴──────────────────────────────────────┘
```

**交互细节**：
- 首次进入页面：弹出 Agent 选择器（从已有 Agent 列表选择），选择后自动创建新会话
- 左侧会话列表：按创建时间倒序，支持滚动加载更多
- 点击会话：加载该会话的历史消息（分页，倒序加载）
- 新建会话：选择 Agent → 创建新会话 → 清空消息区域
- 消息气泡：用户消息右对齐（主色背景），助手消息左对齐（灰色背景）
- 消息时间：悬停显示创建时间
- 删除会话：hover 显示删除按钮，确认后删除

---

### Step 3：前端 — SSE 流式对接

| 项 | 内容 |
|----|------|
| Task | 实现 fetch + ReadableStream 消费 SSE，打字机效果渲染 |
| Output | ChatView.vue 流式发送/接收逻辑 |
| Verification | 手动联调：发送消息 → 逐字输出 → done 事件后停止 → 消息落库 |

**SSE 消费逻辑**：

```javascript
// 使用 fetch + ReadableStream（非 EventSource，因为需要 POST + JSON body）
const response = await fetch('/api/v1/app/chat/stream', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json', 'X-User-Id': '1' },
  body: JSON.stringify({ agentId, message, conversationId, ragMode })
})

const reader = response.body.getReader()
const decoder = new TextDecoder()
let buffer = ''

while (true) {
  const { done, value } = await reader.read()
  if (done) break
  
  buffer += decoder.decode(value, { stream: true })
  // 按 \n\n 分割 SSE 事件
  const events = parseSseEvents(buffer)
  for (const event of events) {
    switch (event.name) {
      case 'meta': { /* 获取 conversationId */ }
      case 'token': { /* 追加到当前消息内容 */ }
      case 'done': { /* 记录 tokenUsage, latencyMs */ }
      case 'error': { /* 显示错误提示 */ }
    }
  }
}
```

**事件类型**：

| event name | 说明 | 数据结构 |
|-----------|------|----------|
| `meta` | 会话元信息 | `{ conversationId, newConversation }` |
| `token` | 单个 token | 纯文本字符串 |
| `done` | 流结束 | `{ tokenUsage: { promptTokens, completionTokens }, latencyMs }` |
| `error` | 错误 | `{ message }` |

**UI 状态**：
- 发送中：输入框禁用，显示"发送中..."或旋转图标
- 流式输出中：输入框禁用，显示"停止生成"按钮
- 完成：输入框恢复，停止按钮隐藏
- 错误：显示错误提示，输入框恢复

---

### Step 4：前端 — 联调与测试

| 项 | 内容 |
|----|------|
| Task | 完整联调 + 前端单元测试 |
| Output | `ChatView.spec.ts` |
| Verification | Vitest 通过 + 手动 3 种 Agent 各 5 轮对话 |

**联调场景**：
1. 无工具无 RAG 的 Agent → 基础对话
2. 有工具的 Agent → 触发工具调用（如天气查询）
3. 有 RAG 的 Agent → 知识库上下文生效
4. 新建会话 → 发送消息 → 切换会话 → 历史消息正确
5. 删除会话 → 列表刷新 → 消息清空

---

## 三、依赖关系

```
Step 1 (Chat API 层)
    │
    ├──→ Step 2 (对话页面基础版)
    │       │
    │       └──→ Step 3 (SSE 流式对接)
    │               │
    │               └──→ Step 4 (联调与测试)
    │
    └──→ Step 4 (前端单元测试，可与 Step 3 并行)
```

**并行机会**：
- Step 1 完成后，Step 2 和 Step 4 的测试用例设计可并行
- Step 3 和 Step 4 部分重叠（联调中发现问题即时修复）

---

## 四、验收标准（来自主计划）

1. `mvn test -pl ciff-chat` 全部通过（后端已满足）
2. 非流式对话：Mock + 真实 LLM 均返回正确内容，消息记录完整（含 token_usage / latency_ms / model_name）
3. SSE 流式：首 Token < 30s，Tomcat 线程无阻塞，客户端断开后端正确回收 SseEmitter
4. 工具调用：单轮结果正确（LLM 返回 tool_call → 执行 → 回传），工具超时/异常有降级处理
5. RAG：绑定知识库时自动检索 Top 3 注入上下文；未绑定时不影响正常对话
6. **前端 Vitest 通过**；手动 3 种 Agent 各 5 轮对话，流式输出流畅
7. SSE 首 Token P95 < 30s；非流式对话 P95 < 10s；消息分页 P95 < 200ms
8. SSE 长连接压测 100 次后 Heap 稳定，无内存泄漏

---

## 五、技术决策记录

| 决策 | 选择 | 原因 |
|------|------|------|
| SSE 消费方式 | fetch + ReadableStream | EventSource 只支持 GET，我们的接口是 POST + JSON body |
| 流式消息渲染 | 打字机效果（逐 token 追加） | 用户体验最佳，实时反馈 LLM 思考过程 |
| 历史消息加载 | 倒序分页 + 滚动加载 | 首次进入只加载最新 N 条，向上滚动加载更早消息 |
| 会话列表刷新策略 | 新建会话后主动刷新列表 | 简单直接，V1 不做 WebSocket 实时推送 |
| Agent 选择器 | 首次进入弹出选择 | 必须先选 Agent 才能对话，与 Agent 管理页联动 |

---

## 六、风险与降级

| 风险 | 影响 | 降级方案 |
|------|------|----------|
| SSE 在前端某些浏览器不兼容 | Step 3 阻塞 | 降级为非流式（`post` 替代 `fetch stream`），用户无感知降级 |
| 消息列表长时滚动性能差 | 用户体验差 | 虚拟滚动（虚拟列表），V1 可先不做（消息量 < 1000 时无感） |
| 流式输出中切换会话 | 状态混乱 | 切换会话时 abort 当前 fetch，清空流式状态 |
| Markdown 渲染复杂内容 | XSS / 样式冲突 | 使用简单文本渲染，V1 不做 Markdown 解析 |

---

## 七、完成总结

**后端完成日期**: 2026-04-20
**前端计划完成日期**: 2026-04-21

**后端交付内容**:
- ciff-chat: Conversation/ChatMessage CRUD + 非流式对话 + SSE 流式 + 工具调用 + RAG
- ciff-app: AppChatController 聚合接口
- ciff-provider: ClaudeClient + OpenAiCompatibleClient + LlmCallConfig
- 测试: 9 个后端测试全部通过

**前端待交付内容**:
- `api/chat.ts` — 对话 API 层
- `views/chat/ChatView.vue` — 完整对话页面
- `ChatView.spec.ts` — 前端单元测试
