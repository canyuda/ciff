# Kimi 指定的后续开发计划

> 本计划基于 Ciff 项目当前代码状态（截至分析日）及模块依赖关系制定，将前后端任务按依赖顺序混合编排，并在每个完整功能阶段后嵌入测试节点，确保质量可控。
>
> **最后更新**：2026-04-15，根据 Claude 反馈优化了 Phase 拆分、安全项、SSE 实现方案和限流覆盖。

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
│  Phase 4: Chat 对话引擎——基础版（依赖 Phase 1~2）                │
│  ├── 开发: 会话管理 + 基础对话 + SSE 流式 + 调用链日志           │
│  ├── 前端: 对话页面（基础版）+ SSE 流式打字机效果                │
│  └── 测试: Chat 基础功能测试                                    │
├─────────────────────────────────────────────────────────────────┤
│  Phase 5: Chat 增强 + Workflow 工作流（依赖 Phase 1~3）          │
│  ├── 开发: Agent 工具调用 + RAG 检索增强 + Workflow 引擎         │
│  ├── 前端: 工作流管理页面（JSON 编辑器/表单配置）                │
│  └── 测试: Chat 增强 + Workflow 引擎测试                        │
├─────────────────────────────────────────────────────────────────┤
│  Phase 6: API 发布 + 用户认证                                    │
│  ├── 开发: API Key 管理 + 外部调用接口 + 登录鉴权（JWT）        │
│  ├── 前端: API Key 管理 + 登录页面 + 路由守卫                    │
│  └── 测试: 安全与认证测试                                       │
├─────────────────────────────────────────────────────────────────┤
│  Phase 7: Docker 部署 + 全局 UI 打磨                             │
│  ├── 开发: Docker Compose 配置 + Nginx + 全局 UI 优化           │
│  └── 测试: 部署测试 + 全链路 E2E                                │
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
| 1.3 | 后端 | LLM 统一调用封装（`ChatClient` / `StreamClient`） | 1.2 | 基于现有 `LlmHttpClient`（WebClient）封装统一调用层，接入四级超时（TCP 5s / 首 Token 30s / Token 间隔 15s / SSE 整体 180s）、Resilience4j 熔断重试、**限流（RateLimiter）**、日志脱敏 |
| 1.4 | 后端 | **API Key AES 加解密**：`t_provider.api_key_encrypted` 的存储加密与读取解密 | 1.1 | 使用 AES-GCM 或项目选定的对称加密方案，密钥外置到 `application.yml`，加密后落库，解密后用于 LLM 调用 |
| 1.5 | 前端 | Axios 统一封装完善 | `ciff-web` 基础框架 | 完成请求/响应拦截、统一错误提示、loading 状态管理、基础类型定义 |
| 1.6 | 前端 | Provider 管理页面对接真实 API（替换 mock） | 1.5 + 1.1 | 供应商列表页、新增/编辑/删除弹窗；模型列表子页面（绑定在供应商下或独立路由） |
| 1.7 | 联调 | 前后端联调验证 | 1.1 ~ 1.6 | 能在前端完成：新增 OpenAI 供应商 → 添加 gpt-4o 模型 → 后端健康检查通过 |

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
| T1.8 | 单元测试 | `RateLimiterTest`：Resilience4j 限流触发与降级行为 | `ciff-common` |
| T1.9 | **单元测试** | **`AesEncryptionTest`：加密/解密一致性、密钥错误时失败、密文不可读** | `ciff-provider` |
| T1.10 | 前端单元测试 | `ProviderList.spec.ts`：列表渲染、分页、弹窗开关、表单提交 | `ciff-web` |
| T1.11 | 前端单元测试 | `ModelList.spec.ts`：模型列表渲染、绑定供应商、参数编辑 | `ciff-web` |
| T1.12 | 前端单元测试 | `CiffTable.spec.ts`：自动加载、分页切换、slot 渲染、空状态 | `ciff-web` 公共组件 |
| T1.13 | 前端单元测试 | `CiffFormDialog.spec.ts`：open 方法、表单验证、submit 事件 | `ciff-web` 公共组件 |
| T1.14 | 联调测试 | 端到端：前端新增 OpenAI 供应商 → 添加 gpt-4o 模型 → 列表回显 → 删除供应商 | E2E |

#### Phase 1 验收标准（逐条）
1. **单元测试**：Service 层核心方法覆盖率不低于 80%，Controller 切片测试覆盖所有公开接口。
2. **集成测试**：Mapper 测试在真实 MySQL 环境中全部通过，包含逻辑删除验证。
3. **LLM 调用测试**：Mock LLM Server 验证超时（TCP 连接超时 ≤ 5s）、熔断触发（失败率 50% 以上时开启熔断）、**限流触发（QPS 超限时降级）**、日志脱敏（请求/响应内容不打印完整 payload）。
4. **API Key 加密测试**：`AesEncryptionTest` 通过；数据库中 `api_key_encrypted` 字段存储的是密文而非明文；密钥通过环境变量或配置外置，不硬编码在代码中。
5. **前端测试**：Vitest 全部通过，`ProviderList` / `ModelList` / `CiffTable` / `CiffFormDialog` 组件渲染、事件触发、API Mock 无报错。
6. **联调测试**：手动操作新增 3 家供应商（OpenAI、Claude、Ollama），每家绑定 1~2 个模型，数据持久化正确，页面刷新后回显无误；API Key 在编辑时不回显明文（仅显示占位符）。
7. **接口规范**：Swagger 文档可正常访问，`/v3/api-docs` 输出包含 Provider 和 Model 所有接口定义。
8. **性能基准**：单条 Provider/Model CRUD 接口 P95 响应时间 < 200ms。
9. **Bug 清零**：Phase 1 范围内无 P0/P1 级阻塞 Bug。

---

### Phase 2：Agent 基础 + MCP 工具（P1）
> **目标**：能创建 Agent 并绑定外部工具。
> **模块**：`ciff-agent` / `ciff-mcp` / `ciff-web`

| 序号 | 类型 | 任务 | 前置依赖 | 验收标准 |
|------|------|------|----------|----------|
| 2.1 | 后端 | `t_tool` 表实体、Mapper、Service、Controller、Facade | `ciff-common` | 完成工具增删改查，支持 API / MCP 类型，含 param_schema / auth_config |
| 2.2 | 后端 | `t_agent` 表实体、Mapper、Service、Controller、Facade | 1.2（模型依赖） | 完成 Agent 增删改查，支持配置 name / model_id / system_prompt / model_params / type / fallback_model_id |
| 2.3 | 后端 | `t_agent_tool` 关联管理 | 2.1 + 2.2 | 支持为 Agent 绑定/解绑工具，查询 Agent 详情时返回关联工具列表 |
| 2.4 | 前端 | 工具管理页面：列表 + 新增/编辑弹窗（区分 API / MCP） | 1.6（页面框架） | 工具列表、新增/编辑弹窗（区分 API 工具和 MCP 工具） |
| 2.5 | 前端 | Agent 管理页面：列表 + 创建/编辑（名称、模型下拉、system_prompt、模型参数 JSON） | 2.4 + 1.6（模型下拉） | Agent 列表、创建/编辑表单 |
| 2.6 | 前端 | Agent 工具绑定：Agent 编辑页中多选工具、保存关联 | 2.5 | 在 Agent 编辑页中支持多选/拖拽绑定工具，保存关联关系 |
| 2.7 | 联调 | 前后端联调验证 | 2.1 ~ 2.6 | 能在前端完成：创建 Agent → 选择模型 → 绑定工具 → 保存成功 |

#### Phase 2 测试用例

| 编号 | 测试类型 | 测试用例名称 | 覆盖范围 |
|------|----------|--------------|----------|
| T2.1 | Controller 切片测试 | `ToolControllerTest`：CRUD / param_schema 格式校验 / 按 type 筛选 | `ciff-mcp` Controller 层 |
| T2.2 | Service 单元测试 | `ToolServiceTest`：Schema JSON 校验、重复 name 校验、auth_config 加密存储 | `ciff-mcp` Service 层 |
| T2.3 | Mapper 集成测试 | `ToolMapperTest`：CRUD + status 筛选 | `ciff-mcp` Mapper 层 |
| T2.4 | Controller 切片测试 | `AgentControllerTest`：CRUD / model_id 外键校验 / type 枚举校验 / 分页 | `ciff-agent` Controller 层 |
| T2.5 | Service 单元测试 | `AgentServiceTest`：创建/更新逻辑、Prompt 长度校验、model_params 覆盖默认、fallback_model 校验 | `ciff-agent` Service 层 |
| T2.6 | Mapper 集成测试 | `AgentMapperTest`：CRUD + user_id 隔离 + 关联查询 | `ciff-agent` Mapper 层 |
| T2.7 | Service 单元测试 | `AgentToolServiceTest`：绑定 / 解绑 / 批量替换 / 重复绑定校验 / 工具不存在时校验 | `ciff-agent` Service 层 |
| T2.8 | Facade 单元测试 | `AgentFacadeTest`：查询 Agent 详情（含工具列表）跨模块调用 | `ciff-agent` Facade 层 |
| T2.9 | 前端单元测试 | `ToolList.spec.ts`：列表渲染 / 类型标签颜色 / 表单校验 | `ciff-web` |
| T2.10 | 前端单元测试 | `AgentList.spec.ts`：Agent 表单提交 / 模型下拉 / 工具多选 / 类型切换 | `ciff-web` |
| T2.11 | 联调 E2E | 创建 Agent（chatbot / agent / workflow 各 1）→ 绑定 0-3 个工具 → 详情页回显 → 解绑 → 保存 | 全链路 |

#### Phase 2 验收标准（逐条）
1. `mvn test -pl ciff-agent,ciff-mcp` 全部通过
2. Tool param_schema 支持 JSON Schema 基础校验（必填项 + 类型），非法 Schema 返回明确错误码
3. Agent CRUD + 工具绑定的三层测试（Controller / Service / Mapper）全部通过
4. Agent 详情查询含关联工具列表，P95 < 300ms
5. 前端 Vitest 通过；手动创建 3 个 Agent 各绑定不同数量工具，刷新后数据一致
6. Redis 缓存 `agent:{id}` 在配置变更后主动失效
7. Bug 清零：Phase 2 范围内无 P0/P1 级阻塞 Bug

---

### Phase 3：知识库 + Agent 增强（P2）
> **目标**：支持上传 TXT 文档构建知识库，并将知识库绑定到 Agent。
> **模块**：`ciff-knowledge` / `ciff-agent`（扩展） / `ciff-web`

| 序号 | 类型 | 任务 | 前置依赖 | 验收标准 |
|------|------|------|----------|----------|
| 3.1 | 后端 | `t_knowledge` CRUD：含 chunk_size / embedding_model 配置 | `ciff-common` | 完成知识库增删改查 |
| 3.2 | 后端 | 文档上传 + TXT 固定长度分块：更新 `t_knowledge_document` 元数据和状态 | 3.1 | 支持文件上传接口，仅接受 TXT，按固定长度分块 |
| 3.3 | 后端 | PGVector 向量存储 + 相似度检索：embedding 生成 → 写入 `t_knowledge_chunk` → top-k 检索 | 1.3（LLM 调用）+ 3.2 | 分块后调用 embedding 模型生成向量，支持 top-k 检索 |
| 3.4 | 后端 | `t_agent_knowledge` 关联管理 | 2.2（Agent）+ 3.1 | 支持为 Agent 绑定/解绑知识库 |
| 3.5 | 前端 | 知识库管理页面：列表 / 创建 / 编辑 / 文档上传列表 / 分块状态 | 1.6 | 知识库页面完整功能 |
| 3.6 | 前端 | Agent 知识库绑定配置：Agent 编辑页多选知识库 | 2.6 + 3.5 | 在 Agent 编辑页中支持多选绑定知识库 |
| 3.7 | 联调 | 前后端联调验证 | 3.1 ~ 3.6 | 能在前端完成：创建知识库 → 上传 TXT → 分块成功 → 绑定到 Agent |

#### Phase 3 测试用例

| 编号 | 测试类型 | 测试用例名称 | 覆盖范围 |
|------|----------|--------------|----------|
| T3.1 | Controller 切片 | `KnowledgeControllerTest`：CRUD / user_id 隔离 / chunk_size 边界 | `ciff-knowledge` |
| T3.2 | Service 单元 | `KnowledgeServiceTest`：chunk_size 范围校验（128-2048）/ embedding_model 可用性校验 | `ciff-knowledge` |
| T3.3 | Mapper 集成 | `KnowledgeMapperTest`：CRUD + user_id 维度查询 | `ciff-knowledge` |
| T3.4 | Service 单元 | `DocumentChunkServiceTest`：空文件 / 短文本 / 超长文本 / 中文换行 / 特殊字符 | `ciff-knowledge` |
| T3.5 | 集成 | `EmbeddingServiceTest`：embedding 生成 + PGVector 写入 + 检索 | `ciff-knowledge` |
| T3.6 | 单元 | `VectorStoreTest`：top-k 检索 / 相似度阈值过滤 / 空结果处理 / knowledge_id 隔离 | `ciff-knowledge` |
| T3.7 | Service 单元 | `AgentKnowledgeServiceTest`：绑定 / 解绑 / 重复绑定校验 | `ciff-agent` |
| T3.8 | 前端单元 | `KnowledgeList.spec.ts`：列表 / 上传组件 / 分块进度 / 状态流转 | `ciff-web` |
| T3.9 | 联调 E2E | 上传 1MB TXT → 分块数正确 → 向量检索返回 Top 3 → 结果语义相关 | 全链路 |
| T3.10 | 联调 E2E | Agent 绑定 2 个知识库 → 详情回显 → 解绑 1 个 → 保存成功 | 全链路 |

#### Phase 3 验收标准（逐条）
1. `mvn test -pl ciff-knowledge` 全部通过；Mapper 集成测试在真实 MySQL 环境执行
2. TXT 分块：空文件报错、单段落不截断、超长文本按 chunk_size 分割，长度误差 < 10%
3. PGVector 集成测试通过，top-k 检索语义相关度 ≥ 80%（人工抽检 10 条）
4. 上传非 TXT 文件返回 400；上传 5MB 以内 TXT 成功，状态流转 uploading → processing → ready
5. 前端 Vitest 通过；上传进度和分块状态与后端一致
6. 向量检索 1000 chunks 场景 P95 < 500ms；文件分块（< 1MB）P95 < 3s
7. Bug 清零：Phase 3 范围内无 P0/P1 级阻塞 Bug

---

### Phase 4：Chat 对话引擎——基础版（P2）
> **目标**：实现基础对话能力（非流式 + SSE 流式）+ 调用链日志追踪。不需要等 Knowledge。
> **模块**：`ciff-chat` / `ciff-web`

| 序号 | 类型 | 任务 | 前置依赖 | 验收标准 |
|------|------|------|----------|----------|
| 4.1 | 后端 | `t_conversation` / `t_chat_message` 实体与 CRUD | `ciff-common` | 会话和消息的增删改查 |
| 4.2 | 后端 | 基础对话接口（非流式）：根据 Agent 配置组装 prompt → 调用 LLM → 保存消息 | 1.3 + 2.2 + 4.1 | 返回完整回复并保存消息记录 |
| 4.3 | 后端 | **SSE 流式对话接口**：WebClient `Flux<ServerSentEvent>` → 自定义异步线程池 → `SseEmitter` 输出，不阻塞 Tomcat | 4.2 | 流式输出稳定，首 Token < 30s |
| 4.4 | 后端 | **调用链日志追踪**：记录 LLM 请求/响应摘要、耗时、token 用量、工具调用链路 | 4.2 | 日志格式统一，可用于调试排错 |
| 4.5 | 后端 | **对话限流**：基于 Resilience4j RateLimiter，按会话或 Agent 维度限制并发请求数 | 4.2 | 超限请求返回 429，不拖垮后端 |
| 4.6 | 前端 | 对话页面基础版：左侧会话列表 + 右侧消息气泡 + 输入框 + 新建会话 | 2.5（Agent 选择） | 聊天界面基础交互完成 |
| 4.7 | 前端 | SSE 流式：EventSource / fetch + ReadableStream，打字机效果，停止生成按钮 | 4.6 + 4.3 | 流式消息渲染正常 |
| 4.8 | 联调 | 前后端联调验证 | 4.1 ~ 4.7 | 选择 Agent → 发送消息 → 流式接收回复 |

#### Phase 4 测试用例

| 编号 | 测试类型 | 测试用例名称 | 覆盖范围 |
|------|----------|--------------|----------|
| T4.1 | Controller 切片 | `ConversationControllerTest`：会话 CRUD / 消息分页 / 删除会话级联消息 | `ciff-chat` |
| T4.2 | Mapper 集成 | `ChatMessageMapperTest`：消息插入 / 按 conversation_id + create_time 排序 / token_usage JSON | `ciff-chat` |
| T4.3 | Service 单元 | `ChatServiceTest`：prompt 组装 / 历史消息拼接 / model_params 覆盖 / 空 prompt 处理 | `ciff-chat` |
| T4.4 | 集成 | `ChatApiTest`：Mock LLM 返回回复 → 消息落库 → token_usage 和 latency_ms 记录正确 | `ciff-chat` |
| T4.5 | 集成 | `SseChatTest`：SSE 连接建立 / 首 Token < 30s / 流式数据完整 / 客户端断开后端回收 | `ciff-chat` |
| T4.6 | 单元 | `ChatRateLimiterTest`：单用户超速请求返回 429 / 多用户并发限流隔离 | `ciff-chat` |
| T4.7 | 单元 | `ChatTracingLogTest`：调用链日志包含 agentId / model / 耗时 / token / 消息角色 | `ciff-chat` |
| T4.8 | 前端单元 | `ChatView.spec.ts`：消息列表渲染 / 会话切换 / 输入框状态 / 空状态 | `ciff-web` |
| T4.9 | 前端单元 | `SseStream.spec.ts`：EventSource 消息拼接 / 打字机效果 / 停止生成 / 连接断开重试 | `ciff-web` |
| T4.10 | 联调 E2E | 选择 Agent → 发送消息 → 流式回复 → 消息历史正确 | 全链路 |
| T4.11 | 联调 E2E | 3 种 Agent（无工具 / 有工具 / 有 RAG，但 RAG 此时可能未生效）各 5 轮对话，SSE 流畅无断连 | 全链路 |

#### Phase 4 验收标准（逐条）
1. `mvn test -pl ciff-chat` 全部通过
2. 非流式对话：Mock + 真实 LLM 均返回正确内容，消息记录完整（含 token_usage / latency_ms / model_name）
3. SSE 流式：首 Token < 30s，Tomcat 线程无阻塞，客户端断开后端正确回收 `SseEmitter`
4. **调用链日志**：每条对话请求产生统一格式的追踪日志，包含 `agentId`、`model`、`latencyMs`、`promptTokens`、`completionTokens`、`status`，可用于排查问题
5. **限流生效**：单用户/单 Agent 超速请求返回 429，后端不被拖垮；RateLimiter 配置可从 `application.yml` 读取
6. 前端 Vitest 通过；手动 3 种 Agent 各 5 轮对话，流式输出流畅
7. SSE 首 Token P95 < 30s；非流式对话 P95 < 10s；消息分页 P95 < 200ms
8. SSE 长连接压测 100 次后 Heap 稳定，无内存泄漏
9. Bug 清零：Phase 4 范围内无 P0/P1 级阻塞 Bug

---

### Phase 5：Chat 增强 + Workflow 工作流（P3）
> **目标**：在基础 Chat 上叠加工具调用、RAG 增强，并实现 Workflow 引擎。
> **模块**：`ciff-chat`（扩展）/ `ciff-workflow` / `ciff-web`

| 序号 | 类型 | 任务 | 前置依赖 | 验收标准 |
|------|------|------|----------|----------|
| 5.1 | 后端 | Agent 工具调用（ReAct 循环）：解析 tool_call → 执行工具 → 回传结果，支持多轮 | 2.3 + 1.3 | 单轮/多轮工具调用正确 |
| 5.2 | 后端 | RAG 检索增强：用户问题 → 检索关联 chunk → 注入上下文 | 3.3 + 4.2 | 绑定知识库时自动增强 |
| 5.3 | 后端 | `t_workflow` CRUD：definition 字段存 JSON 步骤定义 | `ciff-common` | 工作流增删改查 |
| 5.4 | 后端 | 工作流执行引擎：解析 JSON → 按序执行（LLM / 工具 / 条件分支）→ 上下文变量传递 | 5.3 + 1.3 + 2.3 + 3.3 | 5 步骤以内工作流可执行 |
| 5.5 | 前端 | 工作流管理页面：列表 + 创建/编辑（JSON 编辑器或表单式步骤配置） | 1.6 | 工作流配置页面 |
| 5.6 | 联调 | 前后端联调验证 | 5.1 ~ 5.5 | 工具调用 + RAG + Workflow 执行正确 |

#### Phase 5 测试用例

| 编号 | 测试类型 | 测试用例名称 | 覆盖范围 |
|------|----------|--------------|----------|
| T5.1 | 单元 | `ToolCallingTest`：单轮工具调用 / 多轮调用（≤5 轮） / 工具超时降级 / 工具异常回退 | `ciff-chat` |
| T5.2 | 单元 | `RagEnhancementTest`：检索注入 prompt / 无知识库时直接对话 / 检索为空降级 | `ciff-chat` |
| T5.3 | 单元 | `WorkflowEngineTest`：单步 LLM 调用 / 单步工具调用 / 线性多步骤 / 上下文传递 | `ciff-workflow` |
| T5.4 | 单元 | `WorkflowConditionTest`：等于 / 包含 / 大于 / 默认分支 / 条件嵌套 | `ciff-workflow` |
| T5.5 | Controller 切片 | `WorkflowControllerTest`：CRUD / definition JSON 校验 / 分页 | `ciff-workflow` |
| T5.6 | Service 单元 | `WorkflowServiceTest`：definition 格式校验 / 循环引用检测 / 步骤类型校验 | `ciff-workflow` |
| T5.7 | Mapper 集成 | `WorkflowMapperTest`：CRUD + user_id 隔离 | `ciff-workflow` |
| T5.8 | 集成 | `WorkflowExecutionTest`：完整 JSON 工作流端到端执行（3 步骤 + 1 条件分支） | `ciff-workflow` |
| T5.9 | 前端单元 | `WorkflowList.spec.ts`：列表 / 编辑器渲染 / JSON 格式错误提示 | `ciff-web` |
| T5.10 | 联调 E2E | 选择 Agent（带工具+RAG）→ 提问 → 流式回复 → 工具调用 → RAG 上下文生效 → 消息历史正确 | 全链路 |
| T5.11 | 联调 E2E | 创建 3 步骤工作流（LLM → 条件分支 → 工具调用）→ 执行成功 → 结果正确 | 全链路 |

#### Phase 5 验收标准（逐条）
1. `mvn test -pl ciff-chat,ciff-workflow` 全部通过
2. 工具调用：多轮（≤5 轮）结果正确，工具超时/异常有降级处理，不影响对话继续
3. RAG：绑定知识库时自动检索 Top 3 注入上下文；未绑定时不影响正常对话
4. 工作流 definition JSON 校验拦截非法配置（缺少 type / 未知步骤类型 / 循环引用）
5. 条件分支：等于 / 包含 / 大于 三种判断正常，未命中走默认分支
6. 不同工作流实例上下文变量不串扰，步骤输出正确映射为后续步骤输入
7. 前端 Vitest 通过；JSON 编辑器格式错误有明确提示
8. 手动配置并执行 3 个典型工作流，结果与预期一致
9. 5 步骤以内工作流执行 P95 < 15s（含 LLM 调用）；定义查询 P95 < 200ms
10. Bug 清零：Phase 5 范围内无 P0/P1 级阻塞 Bug

---

### Phase 6：API 发布 + 用户认证（P4）
> **目标**：系统可对外发布 API，支持用户登录鉴权。
> **模块**：`ciff-app`（扩展）/ `ciff-web`（扩展）

| 序号 | 类型 | 任务 | 前置依赖 | 验收标准 |
|------|------|------|----------|----------|
| 6.1 | 后端 | `t_api_key` 管理：生成 / 权限配置 / 过期管理；外部调用接口 `/api/v1/external/chat` | 2.2（Agent） | 支持 API Key 全生命周期管理 |
| 6.2 | 后端 | 用户登录鉴权（JWT）：登录接口 → 签发 JWT → 鉴权拦截 | `t_user` 表 ✅ | 关键接口受保护 |
| 6.3 | 前端 | API Key 管理页面：生成（仅显示一次明文） / 列表 / 撤销 | 1.6 | API Key 页面完整功能 |
| 6.4 | 前端 | 登录页面 + 路由守卫：登录表单 / JWT 存储 / 未登录拦截 / 登出 | 6.2 | 认证流程闭环 |
| 6.5 | 联调 | 前后端联调验证 | 6.1 ~ 6.4 | 登录 → 生成 Key → curl 调用成功 |

#### Phase 6 测试用例

| 编号 | 测试类型 | 测试用例名称 | 覆盖范围 |
|------|----------|--------------|----------|
| T6.1 | Controller 切片 | `ApiKeyControllerTest`：生成 / 列表 / 撤销 / 权限校验 | API Key |
| T6.2 | 集成 | `ExternalApiTest`：合法 Key 调用成功 / 无效 Key 返回 401 / 过期 Key 返回 403 | 外部接口 |
| T6.3 | 集成 | `JwtAuthTest`：登录签发 JWT / 携带 Token 访问受保护接口 / Token 过期拦截 / 篡改拦截 | 认证 |
| T6.4 | 单元 | `PasswordEncoderTest`：bcrypt 加密 / 校验匹配 | 用户认证 |
| T6.5 | 前端单元 | `LoginView.spec.ts`：表单校验 / 登录成功跳转 / Token 存储 / 登出清除 | `ciff-web` |
| T6.6 | 前端单元 | `ApiKeyList.spec.ts`：生成弹窗 / Key 显示隐藏 / 撤销确认 | `ciff-web` |
| T6.7 | 联调 E2E | 生成 API Key → curl 调用外部对话接口 → 返回正确 | 全链路 |

#### Phase 6 验收标准（逐条）
1. API Key：无效/过期 Key 返回 401/403；Key 仅生成时显示一次明文，数据库存 SHA256 + 前缀
2. JWT：合法 Token 可访问受保护接口；缺失/过期/篡改被拦截；过期时间可配置
3. 密码 bcrypt 加密存储，不出现明文
4. 前端 Vitest 通过；路由守卫未登录时正确拦截
5. 登录 P95 < 200ms；外部 API（含 Key 校验）P95 < 500ms
6. 无安全漏洞：JWT 密钥不硬编码、API Key 不明文存储、密码不日志输出
7. Bug 清零：Phase 6 范围内无 P0/P1 级阻塞 Bug

---

### Phase 7：Docker 部署 + 全局 UI 打磨（P4）
> **目标**：系统可 Docker 单机部署，前端全局体验打磨到位。
> **模块**：`deploy/` / `ciff-web`

| 序号 | 类型 | 任务 | 前置依赖 | 验收标准 |
|------|------|------|----------|----------|
| 7.1 | 后端 | Docker Compose 部署配置：MySQL / Redis / PGVector / 应用 / Nginx | 全部后端模块 | `deploy/` 目录补齐 |
| 7.2 | 后端 | Nginx 配置：`/api/*` 转发后端、`/*` 转发前端静态资源、SSE `proxy_buffering off` | 7.1 | 反向代理和 SSE 透传正常 |
| 7.3 | 前端 | 全局 UI 打磨：响应式 / 空状态 / 加载骨架屏 / 全局错误提示 / 深色模式（可选） | 全部前端页面 | 体验一致性和完善度 |
| 7.4 | 联调 | 完整系统验证 | 全部 | Docker 一键启动后全流程可用 |

#### Phase 7 测试用例

| 编号 | 测试类型 | 测试用例名称 | 覆盖范围 |
|------|----------|--------------|----------|
| T7.1 | 集成 | `DockerStartupTest`：`docker-compose up -d` 后全部服务健康检查通过 | 部署 |
| T7.2 | 集成 | `NginxProxyTest`：`/api/*` 转发后端 / 静态资源正常 / SSE `proxy_buffering off` | 部署 |
| T7.3 | 联调 E2E | Docker 启动 → 登录 → 创建 Agent → 对话 → 全部正常 | 全链路 |

#### Phase 7 验收标准（逐条）
1. `docker-compose up -d` 一键启动后，MySQL / Redis / PGVector / 后端 / Nginx 全部健康；`make stop` 能正确停止
2. 静态资源访问正常，`/api/v1/health` 转发后端正常，SSE 流式接口 `proxy_buffering off` 生效
3. Docker 全量启动时间 < 2 分钟
4. 前端响应式适配：主流桌面分辨率下无布局崩坏
5. 所有页面空状态、加载状态、错误提示统一
6. 从 Docker 启动到前端登录 → 创建 Agent → 发起对话的完整链路手动验证 3 次无失败
7. Bug 清零：Phase 7 范围内无 P0/P1 级阻塞 Bug

---

## 三、关键路径说明

### 3.1 MVP 最短可演示路径

如果希望尽快出一个可演示版本，**必须按顺序完成以下任务**（含对应测试）：

```
Phase 1（Provider）→ Phase 2（Agent + Tool）→ Phase 4（Chat 基础 + SSE）
```

即：**Provider → Agent + Tool → Chat（基础对话 + 流式）**。此路径可跳过 Phase 3（Knowledge）、Phase 5（RAG + Workflow），先让"创建 Agent → 选模型 → 对话"这个核心闭环跑通。

### 3.2 完整 V1 路径

按 Phase 1 → 2 → 3 → 4 → 5 → 6 → 7 顺序推进，不可跳过的核心依赖：

- **Agent** 必须等 **Provider + MCP + Knowledge**
- **Chat 基础版（Phase 4）** 只需等 **Agent + Provider**
- **Chat 增强（Phase 5）** 再等 **MCP + Knowledge**
- **Workflow** 必须等 **Agent + Knowledge + Provider**
- **API 发布** 必须等 **Agent**
- **部署** 必须等 **全部模块完成**

### 3.3 可并行任务

| 并行组 | 后端 | 前端 |
|--------|------|------|
| Phase 1 | 1.1~1.4 Provider 后端 | 1.5~1.6 Provider 前端（1.5 先行，1.6 等 API） |
| Phase 2 | 2.1 MCP 与 2.2 Agent 可并行 | 2.4 工具页与 2.5 Agent 页可并行（均需 1.6） |
| Phase 3 | 3.1~3.3 Knowledge 后端 | 3.5 知识库页面 |
| Phase 4 | 4.1~4.5 Chat 后端 | 4.6~4.7 对话页 |
| Phase 5 | 5.1~5.4 Chat 增强 + Workflow 后端 | 5.5 工作流页面 |
| Phase 6 | 6.1~6.2 API Key + JWT 后端 | 6.3~6.4 API Key + 登录前端 |
| Phase 7 | 7.1~7.2 Docker + Nginx | 7.3 全局 UI 打磨 |

---

## 四、测试总体要求

### 4.1 测试分层比例建议

| 层级 | 占比 | 说明 |
|------|------|------|
| 单元测试 | 60% | Service 纯逻辑 / Convertor / 工具类 / 前端组件 |
| 集成测试 | 25% | Mapper 连库 / LLM Mock 联调 / PGVector / Docker |
| 切片测试 | 10% | Controller `@WebMvcTest` |
| E2E 联调 | 5% | 关键业务链路手动或自动化验证 |

### 4.2 测试通过通用标准

除各 Phase 的专属验收标准外，所有测试节点还需满足以下通用要求：

1. 所有新增测试用例必须全部通过，不允许有 `@Disabled` 或跳过的核心用例
2. **Maven 构建命令** `mvn package -pl ciff-app -am -DskipTests=false` 必须成功
3. **前端测试命令** `cd ciff-web && npm run test` 必须全部通过，无 warning 堆积
4. 每阶段结束至少完成一次 self-review，确保符合 `rules/` 目录规范
5. 修改接口规范、缓存策略、部署配置时同步更新对应文档

---

## 五、Schema 变更与 Migration 管理（补充说明）

随着 Phase 推进，数据库 schema 可能会发生微调（如新增索引、字段类型调整）。由于当前项目规模不大，建议采用以下轻量策略：

- **Phase 1~3**：以 `db/schema.sql` 为基线，每阶段如有变更，在 `schema.sql` 中用 `ALTER TABLE` 追加变更脚本，并在该 Phase 的测试用例中验证 schema 兼容性
- **Phase 7 部署前**：评估是否引入 Flyway 或 Liquibase。若表结构已稳定，可维持手动 SQL 版本管理；若后续仍有频繁变更，建议补齐 Flyway 配置
- **当前原则**：不阻塞开发，但所有 schema 变更必须随代码一同提交，并在 `docs/database-design.md` 中同步更新表结构说明

---

## 六、当前状态速查

- [ ] Phase 1：Provider 基础层（开发 + 测试）
- [ ] Phase 2：Agent 基础 + MCP 工具（开发 + 测试）
- [ ] Phase 3：知识库 + Agent 增强（开发 + 测试）
- [ ] Phase 4：Chat 对话引擎——基础版（开发 + 测试）
- [ ] Phase 5：Chat 增强 + Workflow 工作流（开发 + 测试）
- [ ] Phase 6：API 发布 + 用户认证（开发 + 测试）
- [ ] Phase 7：Docker 部署 + 全局 UI 打磨（开发 + 测试）

> **当前进行中的任务**：Phase 1.1 ~ 1.4（Provider 后端 CRUD + LLM 调用封装 + API Key 加密）
> **建议同步启动**：T1.1 ~ T1.4 测试用例编写，采用 TDD 或开发-测试并行模式推进。
