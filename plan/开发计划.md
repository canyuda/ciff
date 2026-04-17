# Ciff 后续开发计划

> 基于 2026-04-15 项目代码状态制定。当前已完成：后端基础设施 + 数据库设计 + Demo 模块；前端设计系统 + 公共组件 + Provider mock 页面。所有业务模块待开发。
>
> **修订记录**：采纳 Kimi 评审 4 条建议 + Codex 评审 6 条建议（详见文末修订日志）

---

## 一、前后端依赖关系图

```
                          ┌──────────────┐
                          │ ciff-common  │ ✅ 已完成
                          └──────┬───────┘
                                 │
                    ┌────────────┼────────────┐
                    ▼            ▼            ▼
             ┌──────────┐ ┌──────────┐ ┌──────────┐
             │ Provider │ │   MCP    │ │Knowledge │
             │  供应商   │ │  工具    │ │  知识库   │
             └────┬─────┘ └────┬─────┘ └────┬─────┘
                  │            │            │
                  │     ┌──────┴──────┐     │
                  └─────►│   Agent    │◄────┘
                         │  智能助手  │
                         └──────┬─────┘
                                │
                    ┌───────────┼───────────┐
                    ▼           ▼           ▼
              ┌──────────┐ ┌────────┐ ┌──────────┐
              │   Chat   │ │Workflow│ │ API Key  │
              │  对话引擎 │ │ 工作流  │ │ 外部发布  │
              └──────────┘ └────────┘ └──────────┘
                                │
                         ┌──────┴──────┐
                         │ Auth + 部署  │
                         └─────────────┘

前端依赖:
  Provider页面 ◄── 等待后端 Provider API
  Agent页面   ◄── 等待后端 Agent API + 前端 Provider页面(模型下拉)
  Chat页面    ◄── 等待后端 Chat API + 前端 Agent页面(选择Agent)
  其余页面    ◄── 等待各自后端 API + 基础页面组件
```

---

## 二、分阶段任务列表（含测试节点）

### Phase 0：前置准备

> **目标**：建立跨阶段的共享基础设施，为 Phase 1 扫清障碍。

| 序号 | 端 | 任务 | 说明 |
|------|-----|------|------|
| 0.1 | 后端 | `UserContext`（ThreadLocal）+ `X-User-Id` Header 拦截器 | 认证系统（Phase 6）落地前的临时方案。所有接口通过 `X-User-Id` Header 传入用户 ID，`UserContext` 统一提取。Phase 6 JWT 拦截器上线后替换 Header 来源，业务层无需改动。 |
| 0.2 | 后端 | Provider type 枚举定义：在 ciff-common 定义 `ProviderType` 枚举作为**唯一事实源** | 前端通过后端接口获取 provider type 列表，不硬编码。当前前端 `providerTypes` 数组迁移为动态获取。 |
| 0.3 | 后端 | `UserContext` 跨线程传递方案（SSE/线程池） | ThreadLocal 仅在请求线程有效。异步任务统一采用”显式 userId 入参优先 + 线程池任务包装兜底”，避免 Phase 4/5 出现 user_id 串用或丢失。 |
| 0.4 | 后端 | Flyway 数据库迁移初始化 | 引入 Flyway，将现有 `schema.sql` 和 `pgvector/schema.sql` 转为版本化迁移脚本（`V1__init.sql`），后续 schema 变更通过增量迁移管理，避免多阶段开发中手动同步 DDL。 |
| 0.5 | 后端 | MySQL + PGVector 双数据源配置 | `ciff-knowledge` 需要同时访问 MySQL（`t_knowledge` / `t_knowledge_document`）和 PostgreSQL + PGVector（`t_knowledge_chunk`）。在 ciff-common 或 ciff-knowledge 中配置双数据源，Phase 3 直接使用。 |

> **数据可见性约定**：`t_provider` / `t_model` / `t_tool` 为**全局共享资源**（管理员配置，所有用户可用），不加 `user_id` 隔离。`t_agent` / `t_knowledge` / `t_workflow` / `t_conversation` 为**用户级资源**，通过 `user_id` 隔离。

### Phase 1：Provider 基础层

> **目标**：前端能配置模型供应商和模型，后端能调用 LLM。
> **涉及模块**：`ciff-provider` / `ciff-common`（补全） / `ciff-web`

#### 开发任务

| 序号 | 端 | 任务 | 前置依赖 |
|------|-----|------|----------|
| 1.1 | 后端 | `t_provider` CRUD：Entity → Mapper → Service → Controller → Facade。**type 字段使用 ciff-common 定义的 `ProviderType` 枚举（唯一事实源）**。含 `api_key_encrypted` 的 AES 加解密工具类（加密入库、解密调用） | Phase 0 ✅ |
| 1.2 | 后端 | `t_model` CRUD：绑定到 Provider，含 max_tokens / default_params | 1.1 |
| 1.3a | 后端 | `LlmHttpClient` 四级超时改造：在现有 TCP 连接超时(5s) + 读取超时(60s/120s) 基础上，增加首 Token 超时(30s) 和 Token 间隔超时(15s)，SSE 场景按 `rules/02-llm-calling.md` 规范实现 | 1.2 |
| 1.3b | 后端 | Resilience4j 熔断/重试集成：将现有 `CircuitBreakerService` 接入 `LlmHttpClient`，per-provider 熔断，指数退避重试（仅首 Token 前），失败率 ≥ 50% 触发熔断 | 1.3a |
| 1.3c | 后端 | ChatClient / StreamClient 统一封装：按 Provider 类型适配请求格式（OpenAI 兼容 vs Claude Messages API），统一入参出参，屏蔽各厂商差异 | 1.3b |
| 1.4 | 前端 | Axios 封装完善 + API 类型定义 | ciff-web 基础 ✅ |
| 1.5 | 前端 | Provider 管理页面对接真实 API（替换 mock） | 1.4 + 1.1 |
| 1.6 | 前端 | Model 管理页面，路由采用嵌套形式 `/providers/:id/models`，保持 URL 可分享 | 1.5 + 1.2 |
| **1.T** | **测试** | **Phase 1 测试节点** | **1.1 ~ 1.6** |

#### 测试用例

| 编号 | 类型 | 用例 | 覆盖 |
|------|------|------|------|
| T1.1 | Controller 切片 | `ProviderControllerTest`：创建 / 查询 / 更新 / 删除 / 分页 / 参数校验（type 枚举覆盖前端 14 种类型、URL 格式） | ciff-provider |
| T1.2 | Service 单元 | `ProviderServiceTest`：CRUD 业务逻辑、重复 name 校验、status 切换（enabled/disabled）、逻辑删除 | ciff-provider |
| T1.3 | Mapper 集成 | `ProviderMapperTest`：数据库 CRUD、逻辑删除验证、按 status 筛选 | ciff-provider |
| T1.4 | Controller 切片 | `ModelControllerTest`：模型 CRUD、绑定 provider_id 校验、分页 | ciff-provider |
| T1.5 | Service 单元 | `ModelServiceTest`：default_params JSON 解析、provider 不存在时校验 | ciff-provider |
| T1.6 | 单元 | `ChatClientTest`：正常调用 / TCP 超时 / 首 Token 超时 / Token 间隔超时 / 熔断触发 / 重试退避 / 日志脱敏 / Provider 格式适配（OpenAI 兼容 vs Claude） | ciff-common |
| T1.6a | 单元 | `ApiKeyEncryptorTest`：AES 加密 / 解密 / 空值处理 / 密钥轮换兼容 | ciff-common |
| T1.7 | 前端单元 | `CiffTable.spec.ts`：传入 api 后自动加载 / 分页切换 / slot 渲染 / 空状态 | ciff-web 公共组件 |
| T1.8 | 前端单元 | `CiffFormDialog.spec.ts`：open 方法 / 表单验证 / submit 事件触发 / loading 状态 | ciff-web 公共组件 |
| T1.9 | 前端单元 | `ProviderList.spec.ts`：列表渲染 / 分页 / 弹窗开关 / 表单提交 / 类型标签 | ciff-web |
| T1.10 | 前端单元 | `ModelList.spec.ts`：模型列表 / 新增模型绑定供应商 / 参数编辑 | ciff-web |
| T1.11 | 联调 E2E | 前端新增 OpenAI 供应商 → 添加 gpt-4o 模型 → 列表回显 → 删除供应商（模型级联处理） | 全链路 |

#### 验收标准

1. `mvn test -pl ciff-provider,ciff-common` 全部通过，无 `@Disabled`
2. Provider/Model CRUD 接口 Swagger 文档完整可访问
3. **枚举单一事实源**：`ProviderType` 枚举定义在 ciff-common，前端通过接口动态获取 type 列表，不允许前端硬编码
4. LLM 调用层：Mock Server 下验证四级超时（TCP ≤ 5s / 首 Token ≤ 30s / Token 间隔 ≤ 15s / SSE ≤ 180s）、失败率 ≥ 50% 时熔断器打开、重试仅在首 Token 前触发、日志不打印完整 API Key 和响应 body；Provider 格式适配覆盖 OpenAI 兼容和 Claude Messages API
5. API Key 加解密：AES 加密存储、解密调用、密钥配置外化到 `application.yml`，不硬编码
6. 前端 Vitest 全部通过（含公共组件 CiffTable / CiffFormDialog 测试）；Provider 页面操作真实后端 API 无报错
7. 手动联调：创建 3 家供应商（OpenAI / Claude / Ollama），各绑定 1-2 个模型，刷新后数据一致
8. 单条 CRUD 接口 P95 < 200ms（本地开发环境、单线程顺序执行、数据量 < 1000 条，仅作回归参考）

---

### Phase 2：Agent 基础 + MCP 工具

> **目标**：能创建 Agent 并绑定工具。
> **涉及模块**：`ciff-agent` / `ciff-mcp` / `ciff-web`

#### 开发任务

| 序号 | 端 | 任务 | 前置依赖 |
|------|-----|------|----------|
| 2.1 | 后端 | `t_tool` CRUD：支持 api / mcp 类型，含 param_schema / auth_config | ciff-common ✅ |
| 2.2 | 后端 | `t_agent` 领域能力：Entity/Mapper/Service/Facade（含 name / model_id / system_prompt / model_params / type / fallback_model_id） | Phase 1.2（模型） |
| 2.2.1 | 后端 | Agent 对外 API 聚合到 `ciff-app`：新增聚合 Controller，调用 `ciff-agent` + `ciff-provider` Facade 完成 model_id / fallback_model_id 合法性校验后再落库 | 2.2 |
| 2.3 | 后端 | `t_agent_tool` 关联：绑定 / 解绑 / 批量替换 | 2.1 + 2.2 |
| 2.4 | 前端 | 工具管理页面：列表 + 新增/编辑弹窗（区分 API / MCP） | Phase 1.5（页面框架） |
| 2.5 | 前端 | Agent 管理页面：列表 + 创建/编辑（名称、模型下拉、system_prompt、模型参数 JSON） | 2.4 + Phase 1.6（模型下拉） |
| 2.6 | 前端 | Agent 工具绑定：Agent 编辑页中多选工具、保存关联 | 2.5 |
| **2.T** | **测试** | **Phase 2 测试节点** | **2.1 ~ 2.6** |

#### 测试用例

| 编号 | 类型 | 用例 | 覆盖 |
|------|------|------|------|
| T2.1 | Controller 切片 | `ToolControllerTest`：CRUD / param_schema 格式校验 / 按 type 筛选 | ciff-mcp |
| T2.2 | Service 单元 | `ToolServiceTest`：Schema JSON 校验、重复 name 校验、auth_config 加密存储 | ciff-mcp |
| T2.3 | Mapper 集成 | `ToolMapperTest`：CRUD + status 筛选 | ciff-mcp |
| T2.4 | Controller 切片 | `AppAgentControllerTest`：CRUD / type 枚举校验 / 分页 / model_id 与 fallback_model_id 外键校验 | ciff-app |
| T2.4.1 | Facade 单元 | `AppAgentFacadeTest`：跨模块聚合调用（`ciff-provider` 校验 + `ciff-agent` 落库） | ciff-app |
| T2.5 | Service 单元 | `AgentServiceTest`：创建/更新逻辑、Prompt 长度校验、model_params 覆盖默认、fallback_model 校验 | ciff-agent |
| T2.6 | Mapper 集成 | `AgentMapperTest`：CRUD + `UserContext` user_id 隔离 + 关联查询 | ciff-agent |
| T2.7 | Service 单元 | `AgentToolServiceTest`：绑定 / 解绑 / 批量替换 / 重复绑定校验 / 工具不存在时校验 | ciff-agent |
| T2.8 | Facade 单元 | `AgentFacadeTest`：查询 Agent 详情（含工具列表） | ciff-agent |
| T2.9 | 前端单元 | `ToolList.spec.ts`：列表渲染 / 类型标签颜色 / 表单校验 | ciff-web |
| T2.10 | 前端单元 | `AgentList.spec.ts`：Agent 表单提交 / 模型下拉 / 工具多选 / 类型切换 | ciff-web |
| T2.11 | 联调 E2E | 创建 Agent（chatbot / agent / workflow 各 1）→ 绑定 0-3 个工具 → 详情页回显 → 解绑 → 保存 | 全链路 |

#### 验收标准

1. `mvn test -pl ciff-agent,ciff-mcp,ciff-app` 全部通过（model_id 校验测试在 ciff-app）
2. Tool param_schema 支持 JSON Schema 基础校验（必填项 + 类型），非法 Schema 返回明确错误码
3. Agent CRUD + 工具绑定测试全部通过：ciff-app Controller 切片（含 model_id 外键校验）+ ciff-agent Service/Mapper
4. Agent 详情查询含关联工具列表，P95 < 300ms
5. 前端 Vitest 通过；手动创建 3 个 Agent 各绑定不同数量工具，刷新后数据一致
6. Redis 缓存 `agent:{id}` 在配置变更后主动失效

---

### Phase 3：知识库 + Agent 增强

> **目标**：上传 TXT 构建知识库，绑定到 Agent。
> **涉及模块**：`ciff-knowledge` / `ciff-agent`（扩展） / `ciff-web`
>
> **数据源说明**：`t_knowledge` / `t_knowledge_document` 存储于 MySQL；`t_knowledge_chunk`（含向量）存储于 PostgreSQL + PGVector（双数据源已在 Phase 0.5 配置）。
>
> **V1 向量维度约定**：`t_knowledge_chunk.embedding` 固定为 `VECTOR(1536)`（OpenAI text-embedding-ada-002 / text-embedding-3-small 兼容）。V1 不支持其他维度的 embedding 模型，`t_knowledge.embedding_model` 配置需校验兼容性。

#### 开发任务

| 序号 | 端 | 任务 | 前置依赖 |
|------|-----|------|----------|
| 3.1 | 后端 | `t_knowledge` CRUD：含 chunk_size / embedding_model 配置 | ciff-common ✅ |
| 3.2 | 后端 | 文档上传 + TXT 固定长度分块：更新 `t_knowledge_document` 元数据和状态 | 3.1 |
| 3.3 | 后端 | PGVector 向量存储 + 相似度检索：embedding 生成 → 写入 `t_knowledge_chunk` → top-k 检索 | Phase 1.3（LLM 调用）+ 3.2 |
| 3.4 | 后端 | `t_agent_knowledge` 关联管理 | Phase 2.2（Agent）+ 3.1 |
| 3.5 | 前端 | 知识库管理页面：列表 / 创建 / 编辑 / 文档上传列表 / 分块状态 | Phase 1.5（页面框架） |
| 3.6 | 前端 | Agent 知识库绑定配置：Agent 编辑页多选知识库 | Phase 2.6 + 3.5 |
| **3.T** | **测试** | **Phase 3 测试节点** | **3.1 ~ 3.6** |

#### 测试用例

| 编号 | 类型 | 用例 | 覆盖 |
|------|------|------|------|
| T3.1 | Controller 切片 | `KnowledgeControllerTest`：CRUD / `UserContext` user_id 隔离 / chunk_size 边界 | ciff-knowledge |
| T3.2 | Service 单元 | `KnowledgeServiceTest`：chunk_size 范围校验（128-2048）/ embedding_model 可用性校验 | ciff-knowledge |
| T3.3 | Mapper 集成 | `KnowledgeMapperTest`：CRUD + `UserContext` user_id 维度查询（MySQL 数据源） | ciff-knowledge |
| T3.4 | Service 单元 | `DocumentChunkServiceTest`：空文件 / 短文本 / 超长文本 / 中文换行 / 特殊字符 | ciff-knowledge |
| T3.5 | 集成 | `EmbeddingServiceTest`：embedding 生成 + PGVector 写入 + 检索（需 PostgreSQL + PGVector 测试环境） | ciff-knowledge |
| T3.6 | 单元 | `VectorStoreTest`：top-k 检索 / 相似度阈值过滤 / 空结果处理 / knowledge_id 隔离（PGVector 数据源） | ciff-knowledge |
| T3.6a | 集成 | `DualDataSourceTest`：验证 MySQL 和 PGVector 双数据源配置正确、事务隔离、连接池独立 | ciff-knowledge |
| T3.7 | Service 单元 | `AgentKnowledgeServiceTest`：绑定 / 解绑 / 重复绑定校验 | ciff-agent |
| T3.8 | 前端单元 | `KnowledgeList.spec.ts`：列表 / 上传组件 / 分块进度 / 状态流转 | ciff-web |
| T3.9 | 联调 E2E | 上传 1MB TXT → 分块数正确 → 向量检索返回 Top 3 → 结果语义相关 | 全链路 |
| T3.10 | 联调 E2E | Agent 绑定 2 个知识库 → 详情回显 → 解绑 1 个 → 保存成功 | 全链路 |
| T3.11 | 单元 | `UserContextPropagationTest`：异步分块/向量化任务中 userId 不丢失、不串用 | ciff-knowledge |

#### 验收标准

1. `mvn test -pl ciff-knowledge,ciff-agent` 全部通过；Mapper 集成测试在真实 MySQL 环境执行
2. TXT 分块：空文件报错、单段落不截断、超长文本按 chunk_size 分割，长度误差 < 10%
3. PGVector 集成测试通过，向量检索 Top 3 余弦相似度 > 0.7（单测中用固定 embedding 验证）
4. 上传非 TXT 文件返回 400；上传 5MB 以内 TXT 成功，状态流转 uploading → processing → ready
5. 前端 Vitest 通过；上传进度和分块状态与后端一致
6. 向量检索 1000 chunks 场景 P95 < 500ms；文件分块（< 1MB）P95 < 3s

---

### Phase 4：Chat 对话引擎

> **目标**：完整的对话能力——基础对话、SSE 流式、工具调用、RAG 增强。
> **涉及模块**：`ciff-chat` / `ciff-agent`（扩展） / `ciff-web`
>
> **设计决策**：`t_chat_message` 为仅追加（append-only）表，不含 `update_time` 和 `deleted` 字段。V1 不支持消息编辑/撤回/删除，如需清理通过删除整个会话实现。

#### 开发任务

| 序号 | 端 | 任务 | 前置依赖 |
|------|-----|------|----------|
| 4.1 | 后端 | `t_conversation` / `t_chat_message` CRUD | ciff-common ✅ |
| 4.2 | 后端 | 基础对话接口（非流式）：根据 Agent 配置组装 prompt → 调用 LLM → 保存消息 | Phase 1.3 + Phase 2.2 + 4.1 |
| 4.3 | 后端 | SSE 流式对话接口：WebClient SSE → SseEmitter，不阻塞 Tomcat | 4.2 |
| 4.4 | 后端 | Agent 工具调用（ReAct 循环）：解析 tool_call → 执行工具 → 回传结果，支持多轮 | Phase 2.3 + Phase 1.3 |
| 4.5 | 后端 | RAG 检索增强：用户问题 → 检索关联 chunk → 注入上下文 | Phase 3.3 + 4.2 |
| 4.6 | 前端 | 对话页面基础版：左侧会话列表 + 右侧消息气泡 + 输入框 + 新建会话 | Phase 2.5（Agent 选择） |
| 4.7 | 前端 | SSE 流式：EventSource / fetch + ReadableStream，打字机效果，停止生成按钮 | 4.6 + 4.3 |
| **4.T** | **测试** | **Phase 4 测试节点** | **4.1 ~ 4.7** |

#### 测试用例

| 编号 | 类型 | 用例 | 覆盖 |
|------|------|------|------|
| T4.1 | Controller 切片 | `ConversationControllerTest`：会话 CRUD / 消息分页 / 删除会话级联消息 | ciff-chat |
| T4.2 | Mapper 集成 | `ChatMessageMapperTest`：消息插入 / 按 conversation_id + create_time 排序 / token_usage JSON | ciff-chat |
| T4.3 | Service 单元 | `ChatServiceTest`：prompt 组装 / 历史消息拼接 / model_params 覆盖 / 空 prompt 处理 | ciff-chat |
| T4.4 | 集成 | `ChatApiTest`：Mock LLM 返回回复 → 消息落库 → token_usage 和 latency_ms 记录正确 | ciff-chat |
| T4.5 | 集成 | `SseChatTest`：SSE 连接建立 / 首 Token < 30s / 流式数据完整 / 客户端断开后端回收 | ciff-chat |
| T4.6 | 单元 | `ToolCallingTest`：单轮工具调用 / 多轮调用（≤5 轮） / 工具超时降级 / 工具异常回退 | ciff-chat |
| T4.7 | 单元 | `RagEnhancementTest`：检索注入 prompt / 无知识库时直接对话 / 检索为空降级 | ciff-chat |
| T4.8 | 前端单元 | `ChatView.spec.ts`：消息列表渲染 / 会话切换 / 输入框状态 / 空状态 | ciff-web |
| T4.9 | 前端单元 | `SseStream.spec.ts`：EventSource 消息拼接 / 打字机效果 / 停止生成 / 连接断开重试 | ciff-web |
| T4.10 | 联调 E2E | 选择 Agent → 发送消息 → 流式回复 → 工具调用 → RAG 上下文生效 → 消息历史正确 | 全链路 |
| T4.11 | 联调 E2E | 3 种 Agent（无工具 / 有工具 / 有 RAG）各 5 轮对话，SSE 流畅无断连 | 全链路 |
| T4.12 | 集成 | `SseUserContextTest`：SSE 异步线程中 userId 透传正确，会话/消息隔离正确 | ciff-chat |

#### 验收标准

1. `mvn test -pl ciff-chat` 全部通过
2. 非流式对话：Mock + 真实 LLM 均返回正确内容，消息记录完整（含 token_usage / latency_ms / model_name）
3. SSE 流式：首 Token < 30s，Tomcat 线程无阻塞，客户端断开后端正确回收 SseEmitter
4. 工具调用：多轮（≤5 轮）结果正确，工具超时/异常有降级处理，不影响对话继续
5. RAG：绑定知识库时自动检索 Top 3 注入上下文；未绑定时不影响正常对话
6. 前端 Vitest 通过；手动 3 种 Agent 各 5 轮对话，流式输出流畅
7. SSE 首 Token P95 < 30s；非流式对话 P95 < 10s；消息分页 P95 < 200ms
8. SSE 长连接压测 100 次后 Heap 稳定，无内存泄漏

---

### Phase 5：Workflow 工作流

> **目标**：JSON 配置的工作流——线性步骤 + 条件分支。
> **涉及模块**：`ciff-workflow` / `ciff-web`

#### 开发任务

| 序号 | 端 | 任务 | 前置依赖 |
|------|-----|------|----------|
| 5.1 | 后端 | `t_workflow` CRUD：definition 字段存 JSON 步骤定义 | ciff-common ✅ |
| 5.2 | 后端 | 工作流执行引擎：解析 JSON → 按序执行（LLM / 工具 / 条件分支）→ 上下文变量传递 | 5.1 + Phase 1.3 + Phase 2.3 + Phase 3.3 |
| 5.3 | 前端 | 工作流管理页面：列表 + 创建/编辑（JSON 编辑器或表单式步骤配置） | Phase 1.5（页面框架） |
| **5.T** | **测试** | **Phase 5 测试节点** | **5.1 ~ 5.3** |

#### 测试用例

| 编号 | 类型 | 用例 | 覆盖 |
|------|------|------|------|
| T5.1 | Controller 切片 | `WorkflowControllerTest`：CRUD / definition JSON 校验 / 分页 | ciff-workflow |
| T5.2 | Service 单元 | `WorkflowServiceTest`：definition 格式校验 / 循环引用检测 / 步骤类型校验 | ciff-workflow |
| T5.3 | Mapper 集成 | `WorkflowMapperTest`：CRUD + `UserContext` user_id 隔离 | ciff-workflow |
| T5.4 | 单元 | `WorkflowEngineTest`：单步 LLM 调用 / 单步工具调用 / 线性多步骤 / 上下文传递 | ciff-workflow |
| T5.5 | 单元 | `WorkflowConditionTest`：等于 / 包含 / 大于 / 默认分支 / 条件嵌套 | ciff-workflow |
| T5.6 | 集成 | `WorkflowExecutionTest`：完整 JSON 工作流端到端执行（3 步骤 + 1 条件分支） | ciff-workflow |
| T5.7 | 前端单元 | `WorkflowList.spec.ts`：列表 / 编辑器渲染 / JSON 格式错误提示 | ciff-web |
| T5.8 | 联调 E2E | 创建 3 步骤工作流（LLM → 条件分支 → 工具调用）→ 执行成功 → 结果正确 | 全链路 |
| T5.9 | 单元 | `WorkflowUserContextTest`：工作流异步步骤 userId 透传正确 | ciff-workflow |

#### 验收标准

1. `mvn test -pl ciff-workflow` 全部通过
2. definition JSON 校验拦截非法配置（缺少 type / 未知步骤类型 / 循环引用）
3. 条件分支：等于 / 包含 / 大于 三种判断正常，未命中走默认分支
4. 不同工作流实例上下文变量不串扰，步骤输出正确映射为后续步骤输入
5. 前端 Vitest 通过；JSON 编辑器格式错误有明确提示
6. 手动配置并执行 3 个典型工作流，结果与预期一致
7. 5 步骤以内工作流执行 P95 < 15s（含 LLM 调用）；定义查询 P95 < 200ms

---

### Phase 6：API 发布 + 用户认证 + 部署

> **目标**：系统可对外发布 API，支持登录鉴权，可 Docker 单机部署。
> **涉及模块**：`ciff-app`（扩展） / `ciff-web`（扩展） / `deploy/`

#### 开发任务

| 序号 | 端 | 任务 | 前置依赖 |
|------|-----|------|----------|
| 6.1 | 后端 | `t_api_key` 管理：生成 / 权限配置 / 过期管理；外部调用接口 `/api/v1/external/chat` | Phase 2.2（Agent） |
| 6.2 | 后端 | 用户登录鉴权（JWT）：登录接口 → 签发 JWT → 鉴权拦截 | `t_user` 表 ✅ |
| 6.3 | 后端 | Docker Compose 部署配置：MySQL / Redis / PGVector / 应用 / Nginx | 全部后端模块 |
| 6.4 | 前端 | API Key 管理页面：生成（仅显示一次明文） / 列表 / 撤销 | Phase 1.5（页面框架） |
| 6.5 | 前端 | 登录页面 + 路由守卫：登录表单 / JWT 存储 / 未登录拦截 / 登出 | 6.2 |
| 6.6 | 前端 | 全局 UI 打磨：响应式 / 空状态 / 加载骨架屏 / 全局错误提示 | 全部前端页面 |
| **6.T** | **测试** | **Phase 6 测试节点** | **6.1 ~ 6.6** |

#### 测试用例

| 编号 | 类型 | 用例 | 覆盖 |
|------|------|------|------|
| T6.1 | Controller 切片 | `ApiKeyControllerTest`：生成 / 列表 / 撤销 / 权限校验 | API Key |
| T6.2 | 集成 | `ExternalApiTest`：合法 Key 调用成功 / 无效 Key 返回 401 / 过期 Key 返回 403 | 外部接口 |
| T6.3 | 集成 | `JwtAuthTest`：登录签发 JWT / 携带 Token 访问受保护接口 / Token 过期拦截 / 篡改拦截 | 认证 |
| T6.4 | 单元 | `PasswordEncoderTest`：bcrypt 加密 / 校验匹配 | 用户认证 |
| T6.5 | 脚本验收 | `scripts/verify-deploy.sh`：`docker-compose up -d` 后全部服务健康检查通过 | 部署 |
| T6.6 | 脚本验收 | `scripts/verify-nginx.sh`：`/api/*` 转发后端 / 静态资源正常 / SSE `proxy_buffering off` | 部署 |
| T6.7 | 前端单元 | `LoginView.spec.ts`：表单校验 / 登录成功跳转 / Token 存储 / 登出清除 | ciff-web |
| T6.8 | 前端单元 | `ApiKeyList.spec.ts`：生成弹窗 / Key 显示隐藏 / 撤销确认 | ciff-web |
| T6.9 | 联调 E2E | Docker 启动 → 登录 → 创建 Agent → 对话 → 全部正常 | 全链路 |
| T6.10 | 联调 E2E | 生成 API Key → curl 调用外部对话接口 → 返回正确 | 全链路 |

#### 验收标准

1. API Key：无效/过期 Key 返回 401/403；Key 仅生成时显示一次明文，数据库存 SHA256 + 前缀
2. JWT：合法 Token 可访问受保护接口；缺失/过期/篡改被拦截；过期时间可配置
3. 密码 bcrypt 加密存储，不出现明文
4. `docker-compose up -d` 一键启动全部服务健康；SSE 接口 `proxy_buffering off` 生效
5. 前端 Vitest 通过；路由守卫未登录时正确拦截
6. Docker 全量启动时间 < 2 分钟；登录 P95 < 200ms；外部 API（含 Key 校验）P95 < 500ms
7. 无安全漏洞：JWT 密钥不硬编码、API Key 不明文存储、密码不日志输出

---

## 三、执行顺序与并行策略

### 3.1 MVP 最短可演示路径

```
Phase 0（前置准备）→ Phase 1（Provider）→ Phase 2（Agent + Tool）→ Phase 4 核心（Chat 基础 + SSE）
```

Phase 0 是所有阶段的前置（UserContext / 枚举 / Flyway / 双数据源），不可跳过。之后跳过 Knowledge 和 Workflow，先跑通「创建 Agent → 选模型 → 对话」。

### 3.2 可并行任务

| 并行组 | 后端 | 前端 |
|--------|------|------|
| Phase 1 | 1.1~1.3 Provider 后端 | 1.4~1.6 Provider 前端（1.4 先行，1.5 等 API） |
| Phase 2 | 2.1 MCP 与 2.2 Agent 可并行 | 2.4 工具页与 2.5 Agent 页可并行（均等 1.5） |
| Phase 3 | 3.1~3.3 Knowledge 后端 | 3.5 知识库页面（等页面框架就绪） |
| Phase 4 | 4.1~4.5 Chat 后端 | 4.6~4.7 对话页（等 Agent 页就绪） |
| Phase 5 | 5.1~5.2 Workflow 后端 | 5.3 工作流页面 |
| 各 Phase | 测试用例编写可与开发并行（TDD） | |

### 3.3 不可跳过的核心依赖

- Agent 依赖 Provider（模型绑定）+ MCP（工具绑定）+ Knowledge（知识库绑定）
- Chat 依赖 Agent + Provider（基础对话需 Agent 配置和 LLM 调用）
- Chat 工具调用 依赖 MCP
- Chat RAG 依赖 Knowledge
- Workflow 依赖 Agent + Provider + MCP + Knowledge
- API 发布 依赖 Agent

---

## 四、测试总体要求

### 测试分层比例

| 层级 | 占比 | 说明 |
|------|------|------|
| 单元测试 | 60% | Service 纯逻辑 / Convertor / 工具类 / 前端组件 |
| 集成测试 | 25% | Mapper 连库 / LLM Mock 联调 / PGVector / Docker |
| 切片测试 | 10% | Controller `@WebMvcTest` |
| E2E 联调 | 5% | 关键链路手动验证 |

### 通用通过标准

1. 每阶段所有测试必须全部通过，不允许 `@Disabled` 跳过核心用例
2. `mvn package -pl ciff-app -am -DskipTests=false` 构建成功
3. `cd ciff-web && npm run test` 全部通过
4. 每阶段结束完成 self-review，符合 `rules/` 目录规范
5. 修改接口 / 缓存策略 / 部署配置时同步更新对应文档
6. 文中 P95/耗时指标统一作为**本地基准回归参考**（需记录机器配置、数据量、并发），默认不作为 CI 阶段硬门禁

---

## 五、当前进度

- [ ] **Phase 1**：Provider 基础层（开发 + 测试）
- [ ] **Phase 2**：Agent 基础 + MCP 工具（开发 + 测试）
- [ ] **Phase 3**：知识库 + Agent 增强（开发 + 测试）
- [ ] **Phase 4**：Chat 对话引擎（开发 + 测试）
- [ ] **Phase 5**：Workflow 工作流（开发 + 测试）
- [ ] **Phase 6**：API 发布 + 用户认证 + 部署（开发 + 测试）

---

## 六、修订日志

### 2026-04-15：采纳 Kimi 评审建议

| 编号 | 建议 | 采纳内容 |
|------|------|----------|
| K-1 | `useConfirm.ts` 缺少 `notifySuccess` import | 已在代码中修复（从计划任务移除） |
| K-2 | 公共组件 CiffTable / CiffFormDialog 缺少独立测试 | Phase 1 测试用例新增 T1.7 / T1.8，后续编号顺延 |
| K-3 | Model 页面路由设计模糊 | 任务 1.6 明确采用嵌套路由 `/providers/:id/models` |
| K-4 | 前端 14 种 provider type 与后端枚举需一致 | 任务 1.1 增加 type 枚举约束说明，验收标准第 3 条增加前后端一致性检查 |

### 2026-04-15：采纳 Codex 评审建议

| 编号 | 建议 | 采纳内容 |
|------|------|----------|
| C-1 | agent.model_id 外键校验违反模块依赖规则 | model_id 校验移至 ciff-app 聚合层（AgentFacade），ciff-agent 不依赖 ciff-provider |
| C-2 | 验收命令与测试范围不一致 | 各 Phase 验收命令改为覆盖实际涉及的所有模块 |
| C-3 | Phase 0 的 useConfirm 修复已过期 | 删除旧 0.1（useConfirm 修复任务），替换为 UserContext 临时方案 + ProviderType 枚举定义 |
| C-4 | user_id 隔离在认证系统前无法实现 | 通过 Phase 0 的 `X-User-Id` Header + `UserContext` 统一方案解决，测试用例 mock UserContext |
| C-5 | 验收标准可测性不足 | 语义相关度改为余弦相似度 > 0.7（固定 embedding 验证）；P95 指标明确为本地基准参考 |
| C-6 | 枚举耦合方向反了 | 改为后端 `ProviderType` 枚举为唯一事实源，前端动态获取 |

### 2026-04-15：Claude 代码审查

| 编号 | 问题 | 采纳内容 |
|------|------|----------|
| R-1 | `t_provider` / `t_model` / `t_tool` 缺少 `user_id`，多租户隔离不明确 | Phase 0 新增"数据可见性约定"：Provider/Model/Tool 为全局共享资源（管理员配置），Agent/Knowledge/Workflow/Conversation 为用户级资源 |
| R-2 | `t_knowledge_chunk` 在 PGVector（PostgreSQL），计划未提及双数据源 | Phase 0 新增 0.5 双数据源配置任务；Phase 3 测试区分 MySQL 和 PGVector 环境，新增 T3.6a 双数据源集成测试 |
| R-3 | `LlmHttpClient` 改造量被低估（当前仅 2 级超时，缺 Resilience4j 集成） | 任务 1.3 拆分为 1.3a（四级超时）/ 1.3b（Resilience4j 集成）/ 1.3c（ChatClient/StreamClient 统一封装 + Provider 格式适配） |
| R-4 | 向量维度硬编码 `VECTOR(1536)` 与 embedding_model 可能不匹配 | Phase 3 新增"V1 向量维度约定"：固定 1536 维，仅支持兼容模型，embedding_model 配置需校验 |
| R-5 | 缺 Flyway 数据库迁移工具 | Phase 0 新增 0.4 Flyway 初始化任务 |
| R-6 | `api_key_encrypted` AES 加解密方案未规划 | 任务 1.1 增加 AES 加解密工具类，测试新增 T1.6a `ApiKeyEncryptorTest` |
| R-7 | MVP 路径遗漏 Phase 0 | MVP 路径修正为 `Phase 0 → Phase 1 → Phase 2 → Phase 4 核心` |
| R-8 | `t_chat_message` 缺少 `update_time` / `deleted` 字段 | Phase 4 新增设计决策说明：V1 消息表为 append-only，不支持编辑/撤回/删除 |
