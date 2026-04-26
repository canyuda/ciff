# Frontend Page Design Optimization & Feature Fix

> Date: 2026-04-26
> Status: Approved
> Scope: ciff-web frontend

## Overview

Optimize page design consistency, improve Chat UX, streamline action buttons, and fix functional logic issues across all frontend pages.

## Phase 1: Language & Global Consistency

### TopBar Breadcrumb — Chinese labels

File: `ciff-web/src/components/TopBar.vue`

Change `breadcrumbMap` values from English to Chinese:
```
'/provider' → '供应商管理'
'/tool' → '工具管理'
'/knowledge' → '知识库管理'
'/knowledge-documents' → '文档管理'
'/recall-test' → '召回测试'
'/agent' → 'Agent 管理'
'/chat' → '对话'
'/workflow' → '工作流管理'
'/api-keys' → 'API Key'
```

### Login Page — Chinese labels

File: `ciff-web/src/views/auth/LoginView.vue`

- Form labels: Username → 用户名, Password → 密码
- Placeholders: "Enter username" → "请输入用户名", "Enter password" → "请输入密码"
- Validation messages: translate to Chinese
- Button: "Sign in" → "登录"
- GitHub button: "Sign in with GitHub" → "GitHub 登录"

## Phase 2: Style Normalization

### WorkflowList.vue — Replace hardcoded colors with design tokens

| Hardcoded | Replacement |
|-----------|-------------|
| `#333` | `var(--ciff-text-primary)` |
| `#606266` | `var(--ciff-text-secondary)` |
| `#999`, `#909399` | `var(--ciff-text-tertiary)` |
| `#f56c6c` | keep (Element Plus danger color, already semantic) |
| `#f5f7fa` | `var(--ciff-bg-tertiary)` |
| `#ebeef5` | `var(--ciff-border-light)` |
| `#fdf6ec` | `var(--ciff-warning-light, #fdf6ec)` |
| `#f0f9eb`, `#e1f3d8` | `var(--ciff-success-light, #f0f9eb)` |
| `#fafafa` | `var(--ciff-bg-secondary)` |

### ChatView.vue — Delete button fix

- Replace hardcoded colors with design tokens
- Change `opacity: 1` to `opacity: 0` by default
- Show on `.conversation-item:hover .delete-btn { opacity: 1 }`

## Phase 3: Chat Page UX Upgrade

### Markdown rendering for assistant messages

- Install: `markdown-it` + `highlight.js`
- Create utility: `ciff-web/src/utils/markdown.ts`
  - Configure markdown-it with HTML sanitization
  - Add highlight.js for code block syntax highlighting
  - Export `renderMarkdown(content: string): string`
- Update `ChatView.vue`:
  - Replace `{{ msg.content }}` with `v-html="renderMarkdown(msg.content)"` for assistant messages
  - Keep plain text for user messages
  - Add markdown styles (code block, lists, tables, links)
  - Ensure proper overflow handling for wide content

### Avatar icon fix

- User message: use `User` icon (currently `UserFilled`)
- Assistant message: use `Service` icon from `@element-plus/icons-vue`

### Delete button hover behavior

As specified in Phase 2.

## Phase 4: Action Buttons & Empty States

### KnowledgeList.vue — Consolidate action buttons

Keep primary actions visible: `编辑`, `删除`
Move to dropdown: `管理`, `召回测试`, `重建索引`

Use `el-dropdown` with trigger="click":
```html
<el-button link type="primary">编辑</el-button>
<el-dropdown>
  <el-button link type="primary">更多</el-button>
  <template #dropdown>
    <el-dropdown-menu>
      <el-dropdown-item>管理</el-dropdown-item>
      <el-dropdown-item>召回测试</el-dropdown-item>
      <el-dropdown-item>重建索引</el-dropdown-item>
    </el-dropdown-menu>
  </template>
</el-dropdown>
<el-button link type="danger">删除</el-button>
```

### ProviderList.vue — Consolidate action buttons

Keep: `模型管理`, `编辑`
Move to dropdown: `测试`, `删除`

### EmptyState component enhancement

File: `ciff-web/src/components/EmptyState.vue`

- Accept `title`, `description`, `actionLabel`, `actionHandler` props
- Show icon/illustration + descriptive text + optional action button
- Example: Knowledge empty → "还没有知识库" + "创建第一个知识库" button

## Phase 5: Functional Logic Fixes

### Agent type=workflow requires workflow binding

File: `ciff-web/src/views/agent/AgentList.vue`

- When type === 'workflow': show `workflowId` select field (required)
- Fetch workflow list from `getWorkflows` API
- Add validation rule: workflowId required when type is workflow
- Hide tool/knowledge binding when type is workflow (workflows handle their own tools)

### System prompt optional for chatbot

File: `ciff-web/src/views/agent/AgentList.vue`

- Remove `required` rule from `systemPrompt` when type === 'chatbot'
- Use dynamic validation rules based on type selection
- Keep placeholder text as guidance

### Embedding model — deferred to backend API

File: `ciff-web/src/views/knowledge/KnowledgeList.vue`

- Add TODO comment for dynamic loading
- Keep current hardcoded option for now
- Will be connected to backend API when available

## Phase 6: Merge Chatbot and Agent Types

Chatbot and Agent have zero behavioral difference in backend logic — the chat flow treats them identically. Merging simplifies the product model.

### Backend changes

- `AgentServiceImpl.java` — Remove "chatbot" from `VALID_TYPES` list
- `AgentCreateRequest.java`, `AgentUpdateRequest.java`, `AgentVO.java`, `AgentPO.java` — Update schema comments
- `AgentController.java` — Update API parameter description
- Tests — Replace "chatbot" with "agent" in test fixtures

### Frontend changes

- `AgentList.vue`:
  - Remove `Chatbot（纯对话）` option, keep `Agent` and `Workflow`
  - Merge tag rendering: only show "Agent" tag (remove chatbot branch)
  - Default type for new agents: `"agent"` instead of `"chatbot"`
- `ChatView.vue` — Simplify type tag display (remove chatbot-specific logic)
- `agent.ts` API types — Update comments

### Database migration

Add Flyway script to update existing data:
```sql
UPDATE agent SET type = 'agent' WHERE type = 'chatbot';
```

## Root route change

File: `ciff-web/src/router/index.ts`

- Change root redirect from `/provider` to `/chat`

## Dependencies

- `markdown-it` — Markdown parser
- `highlight.js` — Code syntax highlighting (can import only needed languages)

## Out of scope

- Dark mode support
- Dashboard/home page creation
- Responsive/mobile layout
- Backend API changes
