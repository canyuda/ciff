# Kimi 指定的后续开发计划

> 本计划基于 Ciff 项目当前代码状态（截至分析日）及模块依赖关系制定，将前后端任务按依赖顺序混合编排，并在每个完整功能阶段后嵌入测试节点，确保质量可控。

---

## 一、依赖关系总览

### 1.1 后端模块依赖链（Layer 顺序）

```
Layer 0: ciff-common          ✅ 已完成
Layer 1: ciff-provider        ← 当前进行中
         ciff-mcp             ← 可与 provider 并行
Layer 2: ciff-knowledge       ← 依赖 provider（embedding 模型调用）
Layer 3: ciff-agent           ← 依赖 mcp + knowledge + provider
Layer 4: ciff-workflow        ← 依赖 agent + knowledge + provider
Layer 5: ciff-chat            ← 依赖所有业务模块
Layer 6: ciff-app             ← 最终组装入口
```

### 1.2 功能级依赖关系

```
┌─────────────────────────────────────────────────────────────────┐
│  Phase 1: Provider 基础层（最优先）                              │
│  ├── 开发: 供应商 CRUD + 模型 CRUD + LLM 统一调用封装            │
│  ├── 前端: 布局框架 + Axios 封装 + 模型提供商管理页面             │
│  └── 测试: Provider 模块单元/集成/联调测试                       │
├─────────────────────────────────────────────────────────────────┤
│  Phase 2: Agent 基础 + MCP 工具（可部分并行）                    │
│  ├── 开发: MCP 工具 CRUD + Agent CRUD + Agent-Tool 绑定         │
│  ├── 前端: Agent 管理 + 工具管理 + Agent 工具绑定配置             │
│  └── 测试: Agent & MCP 模块测试                                 │
├─────────────────────────────────────────────────────────────────┤
│  Phase 3: 知识库 + Agent 增强（依赖 Phase 1）                    │
│  ├── 开发: 知识库 CRUD + 文档分块 + 向量检索 + Agent-KB 绑定    │
│  ├── 前端: 知识库管理 + Agent 知识库绑定配置                     │
│  └── 测试: Knowledge 模块测试                                   │
├─────────────────────────────────────────────────────────────────┤
│  Phase 4: Chat 对话引擎（依赖 Phase 1~3）                        │
│  ├── 开发: 会话管理 + 基础对话 + SSE 流式 + 工具调用 + RAG      │
│  ├── 前端: 对话页面（基础版）+ SSE 流式打字机效果                │
│  └── 测试: Chat 端到端测试                                      │
├─────────────────────────────────────────────────────────────────┤
│  Phase 5: Workflow 工作流（依赖 Phase 1~3）                      │
│  ├── 开发: 工作流定义 CRUD + JSON 执行引擎                       │
│  ├── 前端: 工作流管理页面（JSON 编辑器/表单配置）                │
│  └── 测试: Workflow 引擎测试                                    │
├─────────────────────────────────────────────────────────────────┤
│  Phase 6: API 发布 + 用户认证 + 部署（收尾）                     │
│  ├── 开发: API Key 管理 + 外部调用接口 + 登录鉴权 + Docker      │
│  ├── 前端: API Key 管理 + 登录页面 + 全局 UI 打磨                │
│  └── 测试: 安全与部署测试                                       │
└─────────────────────────────────────────────────────────────────┘
```

---

## 二、分阶段混合任务列表

### Phase 1：Provider 基础层（P0）
> **目标**：打通第一个端到端闭环，能够在前端配置模型供应商和模型，后端支持 LLM 调用。
> **模块**：`ciff-provider` / `ciff-common`（补全）/ `ciff-web`

| 序号 | 类型 | 任务 | 前置依赖 | 验收标准 |
|------|------|------|----------|----------|
| 1.1 | 后端 | `t_provider` 表实体、Mapper、Service、Controller、Facade | `ciff-common` 基础组件 | 完成供应商增删改查接口，支持 openai / claude / ollama 等类型 |
| 1.2 | 后端 | `t_model` 表实体、Mapper、Service、Controller、Facade | 1.1 | 完成模型增删改查，支持绑定到供应商，字段含 max_tokens / default_params |
| 1.3 | 后端 | LLM 统一调用封装（`ChatClient` / `StreamClient`） | 1.2 | 基于现有 `LlmHttpClient`（WebClient）封装统一调用层，接入超时（TCP 5s / 首 Token 30s / Token 间隔 15s / SSE 整体 180s）、Resilience4j 熔断重试、日志脱敏 |
| 1.4 | 前端 | Axios 统一封装完善 | `ciff-web` 基础框架 | 完成请求/响应拦截、统一错误提示、loading 状态管理、基础类型定义 |
| 1.5 | 前端 | 模型提供商管理页面 | 1.4 | 供应商列表页、新增/编辑/删除弹窗；模型列表子页面（绑定在供应商下或独立路由） |
| 1.6 | 联调 | 前后端联调验证 | 1.1 ~ 1.5 | 能在前端完成：新增 OpenAI 供应商 → 添加 gpt-4o 模型 → 后端健康检查通过 |

#### Phase 1 测试用例

| 编号 | 测试类型 | 测试用例名称 | 覆盖范围 |
|------|----------|--------------|----------|
| T1.1 | Controller 切片测试 | `ProviderControllerTest`：创建、查询、更新、删除、分页、参数校验 | `ciff-provider` Controller 层 |
| T1.2 | Service 单元测试 | `ProviderServiceTest`：CRUD 业务逻辑、重复名称校验、状态变更 | `ciff-provider` Service 层 |
| T1.3 | Mapper 集成测试 | `ProviderMapperTest`：数据库操作、逻辑删除、分页查询 | `ciff-provider` Mapper 层 |
| T1.4 | Controller 切片测试 | `ModelControllerTest`：模型 CRUD、绑定到供应商、分页查询 | `ciff-provider` Controller 层 |
| T1.5 | Service 单元测试 | `ModelServiceTest`：模型参数 JSON 解析、绑定校验 | `ciff-provider` Service 层 |
| T1.6 | 单元测试 | `ChatClientTest`：正常调用、超时场景、异常处理、日志脱敏校验 | `ciff-common` / `ciff-provider` |
| T1.7 | 单元测试 | `CircuitBreakerServiceTest`：熔断器打开/半开/关闭状态转换 | `ciff-common` |
| T1.8 | 前端单元测试 | `ProviderList.spec.ts`：列表渲染、分页、弹窗开关、表单提交 | `ciff-web` |
| T1.9 | 前端单元测试 | `request.test.ts`：Axios 拦截器、错误码处理、超时重试 | `ciff-web` |
| T1.10 | 联调测试 | 端到端：前端新增供应商 → 后端落库 → 新增模型 → 列表回显正确 | E2E |

#### Phase 1 验收标准（逐条）
1. **单元测试**：Service 层核心方法覆盖率不低于 80%，Controller 切片测试覆盖所有公开接口。
2. **集成测试**：Mapper 测试在真实 MySQL 环境中全部通过，包含逻辑删除验证。
3. **LLM 调用测试**：Mock LLM Server 验证超时（TCP 连接超时 ≤ 5s）、熔断触发（失败率 50% 以上时开启熔断）、日志脱敏（请求/响应内容不打印完整 payload）。
4. **前端测试**：Vitest 全部通过，`ProviderList` 组件渲染、事件触发、API Mock 无报错。
5. **联调测试**：前端手动操作新增 3 家供应商（OpenAI、Claude、Ollama），每家绑定 1~2 个模型，数据持久化正确，页面刷新后回显无误。
6. **接口规范**：Swagger 文档可正常访问，`/v3/api-docs` 输出包含 Provider 和 Model 所有接口定义。
7. **性能基准**：单条 Provider/Model CRUD 接口 P95 响应时间 < 200ms。
8. **Bug 清零**：Phase 1 范围内无 P0/P1 级阻塞 Bug。

---

### Phase 2：Agent 基础 + MCP 工具（P1）
> **目标**：能够创建 Agent，并为其绑定外部工具。
> **模块**：`ciff-agent` / `ciff-mcp` / `ciff-web`

| 序号 | 类型 | 任务 | 前置依赖 | 验收标准 |
|------|------|------|----------|----------|
| 2.1 | 后端 | `t_tool` 表实体、Mapper、Service、Controller、Facade | `ciff-common` | 完成工具增删改查，支持 API / MCP 类型，含 param_schema / auth_config |
| 2.2 | 后端 | `t_agent` 表实体、Mapper、Service、Controller、Facade | 1.2（模型依赖） | 完成 Agent 增删改查，支持配置 name / model_id / system_prompt / model_params / fallback_model_id / type |
| 2.3 | 后端 | `t_agent_tool` 关联管理 | 2.1 + 2.2 | 支持为 Agent 绑定/解绑工具，查询 Agent 详情时返回关联工具列表 |
| 2.4 | 前端 | 工具管理页面 | 1.5 | 工具列表、新增/编辑弹窗（区分 API 工具和 MCP 工具） |
| 2.5 | 前端 | Agent 管理页面 | 2.4 | Agent 列表、创建/编辑表单（名称、模型下拉选择、系统 Prompt 文本框、模型参数 JSON 编辑） |
| 2.6 | 前端 | Agent 工具绑定配置 | 2.5 | 在 Agent 编辑页中支持多选/拖拽绑定工具，保存关联关系 |
| 2.7 | 联调 | 前后端联调验证 | 2.1 ~ 2.6 | 能在前端完成：创建 Agent → 选择模型 → 绑定工具 → 保存成功 |

#### Phase 2 测试用例

| 编号 | 测试类型 | 测试用例名称 | 覆盖范围 |
|------|----------|--------------|----------|
| T2.1 | Controller 切片测试 | `ToolControllerTest`：工具 CRUD、param_schema 校验、类型过滤 | `ciff-mcp` Controller 层 |
| T2.2 | Service 单元测试 | `ToolServiceTest`：Schema 格式校验、重复名称校验 | `ciff-mcp` Service 层 |
| T2.3 | Mapper 集成测试 | `ToolMapperTest`：工具数据库操作、状态筛选 | `ciff-mcp` Mapper 层 |
| T2.4 | Controller 切片测试 | `AgentControllerTest`：Agent CRUD、模型绑定校验、分页 | `ciff-agent` Controller 层 |
| T2.5 | Service 单元测试 | `AgentServiceTest`：创建/更新逻辑、Prompt 长度校验、模型参数覆盖 | `ciff-agent` Service 层 |
| T2.6 | Mapper 集成测试 | `AgentMapperTest`：Agent 数据库操作、关联查询 | `ciff-agent` Mapper 层 |
| T2.7 | 单元测试 | `AgentToolServiceTest`：绑定工具、解绑工具、批量替换、越界校验 | `ciff-agent` Service 层 |
| T2.8 | Facade 单元测试 | `AgentFacadeTest`：跨模块调用 Agent 详情（含工具列表） | `ciff-agent` Facade 层 |
| T2.9 | 前端单元测试 | `ToolList.spec.ts`：工具列表渲染、类型标签、表单校验 | `ciff-web` |
| T2.10 | 前端单元测试 | `AgentList.spec.ts`：Agent 表单提交、模型下拉选择、工具多选 | `ciff-web` |
| T2.11 | 联调测试 | 端到端：创建 Agent → 绑定 2 个 API 工具 + 1 个 MCP 工具 → 详情页回显正确 | E2E |

#### Phase 2 验收标准（逐条）
1. **MCP 模块测试**：Tool CRUD 的 Controller 切片测试、Service 单元测试、Mapper 集成测试全部通过，无报错。
2. **Agent 模块测试**：Agent CRUD 及关联工具绑定的各层测试全部通过，Facade 层测试覆盖跨模块查询场景。
3. **Schema 校验**：工具 param_schema 支持 JSON Schema 基础校验（必填项、类型），非法 Schema 返回明确错误码。
4. **前端测试**：`ToolList` 和 `AgentList` 组件的 Vitest 测试通过，表单提交、下拉选择、多选交互正常。
5. **联调测试**：手动创建 3 个 Agent（chatbot / agent / workflow 类型各 1 个），分别绑定 0~3 个工具，保存后刷新页面，数据完全一致。
6. **缓存验证**：Agent 配置变更后，Redis 缓存 `agent:{id}` 主动失效，再次查询命中数据库并重建缓存。
7. **性能基准**：Agent 详情查询（含工具列表）P95 < 300ms。
8. **Bug 清零**：Phase 2 范围内无 P0/P1 级阻塞 Bug。

---

### Phase 3：知识库 + Agent 增强（P2）
> **目标**：支持上传 TXT 文档构建知识库，并将知识库绑定到 Agent。
> **模块**：`ciff-knowledge` / `ciff-agent`（扩展）/ `ciff-web`

| 序号 | 类型 | 任务 | 前置依赖 | 验收标准 |
|------|------|------|----------|----------|
| 3.1 | 后端 | `t_knowledge` 表实体、Mapper、Service、Controller、Facade | `ciff-common` | 完成知识库增删改查，支持 chunk_size / embedding_model 配置 |
| 3.2 | 后端 | 文档上传与 TXT 分块处理 | 3.1 | 支持文件上传接口，仅接受 TXT，按固定长度分块，更新 `t_knowledge_document` 元数据 |
| 3.3 | 后端 | PGVector 向量存储与相似度检索 | 1.3（LLM 调用用于生成 embedding）+ 3.2 | 分块后调用 embedding 模型生成向量，写入 `t_knowledge_chunk`，支持 top-k 相似度检索 |
| 3.4 | 后端 | `t_agent_knowledge` 关联管理 | 3.1 + 2.2 | 支持为 Agent 绑定/解绑知识库，查询 Agent 详情时返回关联知识库列表 |
| 3.5 | 前端 | 知识库管理页面 | 1.5 | 知识库列表、创建/编辑、文档上传列表、分块状态展示 |
| 3.6 | 前端 | Agent 知识库绑定配置 | 2.6 | 在 Agent 编辑页中支持多选绑定知识库，保存关联关系 |
| 3.7 | 联调 | 前后端联调验证 | 3.1 ~ 3.6 | 能在前端完成：创建知识库 → 上传 TXT → 分块成功 → 绑定到 Agent |

#### Phase 3 测试用例

| 编号 | 测试类型 | 测试用例名称 | 覆盖范围 |
|------|----------|--------------|----------|
| T3.1 | Controller 切片测试 | `KnowledgeControllerTest`：知识库 CRUD、分页、用户隔离 | `ciff-knowledge` Controller 层 |
| T3.2 | Service 单元测试 | `KnowledgeServiceTest`：chunk_size 边界校验、embedding_model 校验 | `ciff-knowledge` Service 层 |
| T3.3 | Mapper 集成测试 | `KnowledgeMapperTest`：知识库数据库操作、用户维度查询 | `ciff-knowledge` Mapper 层 |
| T3.4 | 单元测试 | `DocumentChunkServiceTest`：TXT 分块逻辑（空文件、短文本、长文本、中文换行） | `ciff-knowledge` |
| T3.5 | 集成测试 | `EmbeddingServiceTest`：调用 embedding 接口生成向量、PGVector 写入与检索 | `ciff-knowledge` |
| T3.6 | 单元测试 | `VectorStoreTest`：top-k 检索、相似度阈值过滤、空结果处理 | `ciff-knowledge` |
| T3.7 | 单元测试 | `AgentKnowledgeServiceTest`：绑定/解绑知识库、重复绑定校验 | `ciff-agent` Service 层 |
| T3.8 | 前端单元测试 | `KnowledgeList.spec.ts`：列表、上传组件、分块进度展示 | `ciff-web` |
| T3.9 | 联调测试 | 端到端：上传 1MB TXT 文件 → 分块数正确 → 向量检索返回 Top 3 | E2E |
| T3.10 | 联调测试 | 端到端：Agent 绑定 2 个知识库 → 详情页回显 → 解绑 1 个 → 保存成功 | E2E |

#### Phase 3 验收标准（逐条）
1. **知识库 CRUD 测试**：Controller、Service、Mapper 三层测试全部通过，用户隔离正确（只能查到自己的知识库）。
2. **分块逻辑测试**：TXT 分块服务单元测试覆盖边界场景（空文件、单段落、超长文本、特殊换行符），分块长度误差不超过 10%。
3. **向量存储测试**：PGVector 集成测试通过，embedding 生成成功写入，top-k 检索结果语义相关度合理（人工抽检 10 条，相关度 ≥ 80%）。
4. **文件上传测试**：上传非 TXT 文件返回明确错误码；上传 5MB 以内 TXT 文件分块处理成功，状态流转为 `ready`。
5. **前端测试**：知识库列表和上传组件的 Vitest 测试通过，分块状态展示与实际后端状态一致。
6. **联调测试**：手动创建 2 个知识库，分别上传不同 TXT，绑定到同一个 Agent，解绑后数据一致性无误。
7. **性能基准**：1000 个 chunk 的向量检索 P95 < 500ms；单文件上传分块处理（< 1MB）P95 < 3s。
8. **Bug 清零**：Phase 3 范围内无 P0/P1 级阻塞 Bug。

---

### Phase 4：Chat 对话引擎（P2 ~ P3）
> **目标**：实现完整的对话能力，包括非流式、SSE 流式、工具调用、RAG 增强。
> **模块**：`ciff-chat` / `ciff-agent`（扩展）/ `ciff-web`

| 序号 | 类型 | 任务 | 前置依赖 | 验收标准 |
|------|------|------|----------|----------|
| 4.1 | 后端 | `t_conversation` / `t_chat_message` 实体与 CRUD | `ciff-common` | 会话和消息的增删改查，消息支持 role / content / token_usage / model_name / latency_ms |
| 4.2 | 后端 | 基础对话接口（非流式） | 1.3（LLM 调用）+ 2.2（Agent 配置）+ 4.1 | 根据 Agent 配置组装请求，调用 LLM，返回完整回复并保存消息记录 |
| 4.3 | 后端 | SSE 流式对话接口 | 4.2 | 使用 `SseEmitter` 异步返回流式输出（基于 WebClient SSE），不阻塞 Tomcat 线程 |
| 4.4 | 后端 | Agent 工具调用（ReAct / Function Calling） | 2.3（Agent-Tool 绑定）+ 1.3 | 识别 LLM 的 tool_call 请求，调用对应工具，将结果回传给 LLM，支持多轮 |
| 4.5 | 后端 | RAG 检索增强 | 3.3（向量检索）+ 4.2 | 对话前根据用户问题检索关联知识库分块，将检索结果注入 system_prompt 或 user_message 上下文 |
| 4.6 | 前端 | 对话页面（基础版） | 1.5 | 聊天界面、左侧会话列表、消息气泡、输入框、新建会话 |
| 4.7 | 前端 | 对话页面（SSE 流式） | 4.6 | 接入 `EventSource` 或 `fetch` + `ReadableStream`，实现打字机效果，支持停止生成 |
| 4.8 | 联调 | 前后端联调验证 | 4.1 ~ 4.7 | 能在前端完成：选择 Agent → 发送消息 → 流式接收回复 → 工具调用成功 → RAG 上下文生效 |

#### Phase 4 测试用例

| 编号 | 测试类型 | 测试用例名称 | 覆盖范围 |
|------|----------|--------------|----------|
| T4.1 | Controller 切片测试 | `ConversationControllerTest`：会话 CRUD、消息分页、删除会话 | `ciff-chat` Controller 层 |
| T4.2 | Mapper 集成测试 | `ChatMessageMapperTest`：消息插入、会话维度查询、token_usage JSON 解析 | `ciff-chat` Mapper 层 |
| T4.3 | Service 单元测试 | `ChatServiceTest`：基础对话组装 Prompt、历史消息拼接、模型参数覆盖 | `ciff-chat` Service 层 |
| T4.4 | 集成测试 | `ChatApiTest`：调用真实/Mock LLM 返回完整回复，消息落库正确 | `ciff-chat` |
| T4.5 | 集成测试 | `SseChatTest`：SSE 流式接口连接正常、首包返回 < 30s、断开重连 | `ciff-chat` |
| T4.6 | 单元测试 | `ToolCallingTest`：单轮工具调用、多轮工具调用、工具调用失败回退 | `ciff-chat` |
| T4.7 | 单元测试 | `RagEnhancementTest`：检索结果注入 Prompt、无关联知识库时直接对话、检索为空时正常降级 | `ciff-chat` |
| T4.8 | 前端单元测试 | `ChatView.spec.ts`：消息列表渲染、会话切换、输入框禁用状态 | `ciff-web` |
| T4.9 | 前端单元测试 | `SseStreamTest`：EventSource 消息拼接、打字机效果、停止生成按钮 | `ciff-web` |
| T4.10 | 联调测试 | 端到端：选择 Agent（带工具+RAG）→ 提问 → 流式回复 → 工具调用 → 最终结果正确 | E2E |

#### Phase 4 验收标准（逐条）
1. **会话消息测试**：Conversation/Message CRUD 的 Controller 切片和 Mapper 集成测试全部通过，分页和排序正确。
2. **基础对话测试**：非流式对话接口在 Mock LLM 和真实 LLM 环境下均返回正确内容，消息记录完整（含 token_usage、latency_ms）。
3. **SSE 流式测试**：SSE 接口连接稳定，首 Token 返回时间 < 30s，Tomcat 线程无阻塞，客户端断开连接后端能正确回收资源。
4. **工具调用测试**：配置工具的 Agent 在测试中触发工具调用，多轮调用（最多 5 轮）结果正确，工具超时/异常时有降级处理。
5. **RAG 测试**：绑定知识库的 Agent，对话时自动检索 Top 3 分块并注入上下文；未绑定知识库时不影响正常对话；检索为空时正常降级。
6. **前端测试**：ChatView 组件渲染、会话切换、SSE 流式消息拼接的 Vitest 测试通过，停止生成按钮功能正常。
7. **联调测试**：手动与 3 个不同 Agent（无工具/有工具/有 RAG）进行 5 轮以上对话，流式输出流畅，工具调用和知识库引用正确。
8. **性能基准**：SSE 首 Token < 30s；单条非流式对话 P95 < 10s；消息列表分页查询 P95 < 200ms。
9. **Bug 清零**：Phase 4 范围内无 P0/P1 级阻塞 Bug，无内存泄漏（SSE 长连接压测 100 次后 Heap 稳定）。

---

### Phase 5：Workflow 工作流（P3）
> **目标**：支持 JSON 配置的工作流，线性步骤 + 条件分支。
> **模块**：`ciff-workflow` / `ciff-web`

| 序号 | 类型 | 任务 | 前置依赖 | 验收标准 |
|------|------|------|----------|----------|
| 5.1 | 后端 | `t_workflow` 表实体、Mapper、Service、Controller、Facade | `ciff-common` | 工作流增删改查，definition 字段存储 JSON 步骤定义 |
| 5.2 | 后端 | 工作流执行引擎 | 5.1 + 1.3（LLM 调用）+ 2.3（工具调用）+ 3.3（RAG） | 解析 JSON 定义，按顺序执行步骤（LLM 调用、工具调用、条件分支），支持上下文变量传递 |
| 5.3 | 前端 | 工作流管理页面 | 1.5 | 工作流列表、创建/编辑页面。V1 不做可视化拖拽，可用 Monaco/JSON 编辑器或表单式步骤配置 |
| 5.4 | 联调 | 前后端联调验证 | 5.1 ~ 5.3 | 能在前端完成：创建工作流 → 配置步骤 → 保存 → 后端能正确执行并返回结果 |

#### Phase 5 测试用例

| 编号 | 测试类型 | 测试用例名称 | 覆盖范围 |
|------|----------|--------------|----------|
| T5.1 | Controller 切片测试 | `WorkflowControllerTest`：工作流 CRUD、JSON 定义校验、分页 | `ciff-workflow` Controller 层 |
| T5.2 | Service 单元测试 | `WorkflowServiceTest`：definition 格式校验、循环引用检测 | `ciff-workflow` Service 层 |
| T5.3 | Mapper 集成测试 | `WorkflowMapperTest`：工作流数据库操作、用户隔离 | `ciff-workflow` Mapper 层 |
| T5.4 | 单元测试 | `WorkflowEngineTest`：单步骤 LLM 调用、单步骤工具调用、线性多步骤 | `ciff-workflow` |
| T5.5 | 单元测试 | `WorkflowConditionTest`：条件分支（等于/包含/大于）、默认分支、条件嵌套 | `ciff-workflow` |
| T5.6 | 单元测试 | `WorkflowContextTest`：变量传递、步骤间引用、输出映射 | `ciff-workflow` |
| T5.7 | 集成测试 | `WorkflowExecutionTest`：完整 JSON 工作流端到端执行 | `ciff-workflow` |
| T5.8 | 前端单元测试 | `WorkflowList.spec.ts`：列表、编辑器渲染、JSON 校验提示 | `ciff-web` |
| T5.9 | 联调测试 | 端到端：创建 3 步骤工作流（LLM → 条件分支 → 工具调用）→ 执行成功 | E2E |

#### Phase 5 验收标准（逐条）
1. **工作流 CRUD 测试**：Controller、Service、Mapper 三层测试全部通过，definition JSON 结构校验拦截非法配置。
2. **执行引擎测试**：线性步骤、条件分支、变量传递的核心单元测试全部通过，循环引用和非法步骤类型能被检测并报错。
3. **条件分支测试**：支持等于、包含、大于三种基础条件判断，未命中任何条件时走默认分支，不抛异常。
4. **上下文隔离**：不同工作流实例执行时上下文变量不串扰，步骤输出能正确映射为后续步骤输入。
5. **前端测试**：WorkflowList 和编辑器组件的 Vitest 测试通过，JSON 格式错误时有明确提示。
6. **联调测试**：手动配置并执行 3 个典型工作流（单步骤 LLM / 线性多步骤 / 带条件分支），执行结果与预期一致。
7. **性能基准**：5 步骤以内的工作流执行 P95 < 15s（含 LLM 调用时间）；工作流定义查询 P95 < 200ms。
8. **Bug 清零**：Phase 5 范围内无 P0/P1 级阻塞 Bug。

---

### Phase 6：API 发布 + 用户认证 + 部署（P4）
> **目标**：系统可对外发布 API，支持用户登录鉴权，可 Docker 单机部署。
> **模块**：`ciff-app`（扩展）/ `ciff-web`（扩展）/ `deploy`

| 序号 | 类型 | 任务 | 前置依赖 | 验收标准 |
|------|------|------|----------|----------|
| 6.1 | 后端 | `t_api_key` 管理与外部调用接口 | 2.2（Agent 依赖） | 支持生成 API Key、权限配置、过期管理；提供 `/api/v1/external/chat` 等外部调用入口，校验 API Key |
| 6.2 | 后端 | 用户登录鉴权（JWT） | `t_user` 已存在 | 实现登录接口，签发 JWT，关键接口添加鉴权拦截 |
| 6.3 | 后端 | Docker Compose 部署配置 | 全部后端模块 | 补齐 `deploy/` 目录，包含 `docker-compose.yml`、Nginx 配置、MySQL / Redis / PGVector / 应用服务配置 |
| 6.4 | 前端 | API Key 管理页面 | 1.5 | API Key 列表、生成新 Key（只显示一次）、撤销 Key |
| 6.5 | 前端 | 登录页面 + 路由守卫 | 6.2 | 登录表单、JWT 存储、未登录拦截、登出 |
| 6.6 | 前端 | 全局 UI 打磨 | 全部前端页面 | 响应式适配、空状态、全局错误提示、深色模式（可选）、加载骨架屏 |
| 6.7 | 联调 | 完整系统验证 | 全部 | 能通过 Docker Compose 一键启动完整系统，前端登录后创建 Agent 并正常对话 |

#### Phase 6 测试用例

| 编号 | 测试类型 | 测试用例名称 | 覆盖范围 |
|------|----------|--------------|----------|
| T6.1 | Controller 切片测试 | `ApiKeyControllerTest`：生成、列表、撤销、权限校验 | API Key 管理接口 |
| T6.2 | 集成测试 | `ExternalApiTest`：携带合法 API Key 调用外部接口、无效 Key 拒绝、过期 Key 拒绝 | 外部调用接口 |
| T6.3 | 集成测试 | `JwtAuthTest`：登录签发 JWT、访问受保护接口、Token 过期、非法 Token 拦截 | 认证鉴权 |
| T6.4 | 单元测试 | `PasswordEncoderTest`：密码加密、校验匹配 | 用户认证 |
| T6.5 | 集成测试 | `DockerStartupTest`：`docker-compose up -d` 后所有服务健康检查通过 | 部署测试 |
| T6.6 | 集成测试 | `NginxProxyTest`：`/api/*` 转发后端、`/*` 转发前端静态资源、SSE 透传 | 部署测试 |
| T6.7 | 前端单元测试 | `LoginView.spec.ts`：表单校验、登录成功跳转、Token 存储 | `ciff-web` |
| T6.8 | 前端单元测试 | `ApiKeyList.spec.ts`：生成弹窗、Key 显示/隐藏、撤销确认 | `ciff-web` |
| T6.9 | 联调测试 | 端到端：Docker 启动 → 前端登录 → 创建 Agent → 发起对话 → 全部正常 | E2E |
| T6.10 | 联调测试 | 端到端：生成 API Key → 用 curl 调用外部对话接口 → 返回正确 | E2E |

#### Phase 6 验收标准（逐条）
1. **API Key 测试**：生成、撤销、权限控制的测试全部通过；无效/过期 API Key 访问外部接口返回 401/403。
2. **JWT 认证测试**：登录接口签发 JWT 正确；受保护接口携带合法 Token 可访问，缺失/过期/篡改 Token 被拦截；Token 过期时间可配置。
3. **安全测试**：密码使用 bcrypt 加密存储；API Key 仅生成时显示一次明文，数据库存储 SHA256 哈希和前缀。
4. **部署测试**：`docker-compose up -d` 一键启动后，MySQL / Redis / PGVector / 后端 / Nginx 全部健康；`make stop` 能正确停止。
5. **Nginx 测试**：静态资源访问正常，`/api/v1/health` 转发后端正常，SSE 流式接口 `proxy_buffering off` 生效。
6. **前端测试**：LoginView 和 ApiKeyList 组件的 Vitest 测试通过，路由守卫在未登录时正确拦截。
7. **联调测试**：从 Docker 启动到前端登录 → 创建 Agent → 发起对话的完整链路手动验证 3 次无失败；curl 调用外部 API 3 次无失败。
8. **性能基准**：登录接口 P95 < 200ms；外部 API 调用（含 Key 校验）P95 < 500ms；Docker 全量启动时间 < 2 分钟。
9. **Bug 清零**：Phase 6 范围内无 P0/P1 级阻塞 Bug；无安全漏洞（如明文存 Key、JWT 密钥硬编码等）。

---

## 三、关键路径说明

### 3.1 最短可演示路径（MVP）

如果希望尽快出一个可演示版本，**必须按顺序完成以下任务**（含对应测试）：

```
Phase 1 全部（1.1 ~ 1.6 + T1.1 ~ T1.10 + 验收标准）
  → Phase 2 全部（2.1 ~ 2.7 + T2.1 ~ T2.11 + 验收标准）
    → Phase 4 核心部分（4.1 ~ 4.3 + 4.6 ~ 4.7 + T4.1 ~ T4.9 + 验收标准）
```

即：**Provider → Agent + Tool → Chat（基础+流式）**。此路径可跳过 Knowledge 和 Workflow，先让"Agent 能聊天"跑起来。

### 3.2 完整 V1 路径

按 Phase 1 → 2 → 3 → 4 → 5 → 6 顺序推进（含每个 Phase 的测试节点），不可跳过的核心依赖：

- **Agent** 必须等 **Provider + MCP + Knowledge**
- **Chat** 必须等 **Agent + Provider**（若要支持工具调用和 RAG，还需等 MCP + Knowledge）
- **Workflow** 必须等 **Agent + Knowledge + Provider**
- **API 发布** 必须等 **Agent**

### 3.3 可并行任务

| 并行组 | 任务 |
|--------|------|
| A | Phase 1 后端开发（1.1 ~ 1.3） 与 Phase 1 前端开发（1.4 ~ 1.5）可并行 |
| B | Phase 2 后端 MCP（2.1） 与 Phase 2 后端 Agent（2.2）可并行 |
| C | Phase 3 后端 Knowledge（3.1 ~ 3.3） 与 Phase 3 前端页面（3.5）可并行 |
| D | Phase 4 后端 Chat（4.1 ~ 4.5） 与 Phase 4 前端对话页（4.6 ~ 4.7）可并行 |
| E | Phase 5 后端 Workflow（5.1 ~ 5.2） 与 Phase 5 前端页面（5.3）可并行 |
| F | 各 Phase 的测试用例编写可与该 Phase 的开发工作并行进行（TDD 模式） |

---

## 四、测试总体要求

### 4.1 测试分层比例建议

| 层级 | 比例 | 说明 |
|------|------|------|
| 单元测试 | 60% | Service 纯逻辑、Convertor、工具类、前端组件 |
| 集成测试 | 25% | Mapper 连库测试、LLM/Mock 联调、PGVector 检索、Docker 部署 |
| 切片测试 | 10% | Controller 层 `@WebMvcTest` |
| E2E 联调 | 5% | 关键业务链路的手动或自动化端到端验证 |

### 4.2 测试通过通用标准

除各 Phase 的专属验收标准外，所有测试节点还需满足以下通用要求：

1. **所有新增测试用例必须全部通过**，不允许有 `@Disabled` 或跳过的核心用例。
2. **Maven 构建命令** `mvn package -pl ciff-app -am -DskipTests=false` 必须成功。
3. **前端测试命令** `cd ciff-web && npm run test` 必须全部通过，无 warning 堆积。
4. **代码审查**：每个 Phase 结束时至少完成一次 self-review，确保符合 `rules/` 目录下的规范。
5. **文档同步**：如修改了接口规范、缓存策略、部署配置，必须同步更新 `AGENTS.md` 或对应 `rules/` 文档。

---

## 五、当前状态速查

- [ ] Phase 1：Provider 基础层（开发 + 测试）
- [ ] Phase 2：Agent 基础 + MCP 工具（开发 + 测试）
- [ ] Phase 3：知识库 + Agent 增强（开发 + 测试）
- [ ] Phase 4：Chat 对话引擎（开发 + 测试）
- [ ] Phase 5：Workflow 工作流（开发 + 测试）
- [ ] Phase 6：API 发布 + 用户认证 + 部署（开发 + 测试）

> **当前进行中的任务**：Phase 1.1 ~ 1.3（Provider 后端 CRUD + LLM 调用封装）
> **建议同步启动**：T1.1 ~ T1.3 测试用例编写，采用 TDD 或开发-测试并行模式推进。
