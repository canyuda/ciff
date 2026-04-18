# Phase 3 执行计划：知识库 + Agent 增强

> 目标：上传 TXT 构建知识库，绑定到 Agent。
> 涉及模块：`ciff-knowledge` / `ciff-agent`（扩展）/ `ciff-app`（聚合）/ `ciff-web`

---

## 一、状态快照

### 前置 Phase

| Phase | 状态 | 证据 |
|-------|------|------|
| Phase 0 | 基本完成，缺双数据源 | UserContext/Flyway/枚举均就绪；`application.yml` 仅配置了 MySQL 单数据源，PGVector 数据源未配置 |
| Phase 1 | 完成 | `mvn test -pl ciff-provider,ciff-common` 全部通过 |
| Phase 2 | 完成 | `mvn test -pl ciff-agent,ciff-mcp,ciff-app` 全部通过 |

### Phase 3 当前状态

| 组件 | 状态 | 说明 |
|------|------|------|
| ciff-knowledge 模块 | 空 | 无任何 Java 源文件 |
| t_knowledge / t_knowledge_document | schema 就绪 | MySQL V1__init_schema.sql 已定义 |
| t_knowledge_chunk | schema 就绪 | PGVector pgvector_schema.sql 已定义 |
| t_agent_knowledge | schema 就绪 | MySQL V1__init_schema.sql 已定义 |
| 双数据源配置 | 未配置 | application.yml 只有 MySQL |
| 前端知识库页面 | 未开发 | 无路由、无组件、无 API |

---

## 二、执行步骤

### Step 1：双数据源配置（PostgreSQL/PGVector）

| 项 | 内容 |
|----|------|
| Task | 配置 MySQL + PostgreSQL 双数据源；MyBatis-Plus 扫描 MySQL mapper；PGVector 使用 JdbcTemplate |
| Output | `DataSourceConfig.java` + `application.yml` 更新 + `PgVectorConfig.java` |
| Verification | 应用启动成功，两个数据源连接正常 |
| 降级选项 | 无 — Phase 3 核心依赖 |

**说明**：
- MySQL 数据源保持现有配置不变（MyBatis-Plus 自动配置）
- 新增 PostgreSQL 数据源，专用于 `t_knowledge_chunk` 的向量操作
- PGVector 使用 `JdbcTemplate` 直接执行原生 SQL，避免 mybatis-plus 对 `vector` 类型的兼容问题
- PostgreSQL 连接使用 `org.postgresql:postgresql` 驱动，需要确认是否已在 pom 中引入

---

### Step 2：ciff-knowledge — t_knowledge CRUD

| 项 | 内容 |
|----|------|
| Task | Entity → Mapper → Service → Facade（含分页、user_id 隔离） |
| Output | `KnowledgePO.java` / `KnowledgeMapper.java` / `KnowledgeService.java` / `KnowledgeServiceImpl.java` / `KnowledgeFacade.java` / `KnowledgeFacadeImpl.java` / DTOs / VO |
| Verification | Service 单元测试通过：CRUD、chunk_size 范围校验(128-2048)、embedding_model 校验 |

**设计要点**：
- `KnowledgePO` 继承 `SoftDeletableEntity`
- `chunk_size` 默认 500，校验范围 128-2048
- `embedding_model` V1 仅支持 text-embedding-v3（阿里云，1024 维），写入时校验
- Service 接口遵循已有模块的 `create/update/getById/delete/page` 模式
- Facade 暴露给外部模块（ciff-app / ciff-chat）调用

---

### Step 3：ciff-knowledge — 文档上传 + TXT 分块

| 项 | 内容 |
|----|------|
| Task | 文件上传接口 → 保存文件 → 读取 TXT → 固定长度分块 → 更新 document 状态 |
| Output | `DocumentService.java` / `ChunkService.java` / `DocumentChunkServiceImpl.java` / 上传 Controller 方法 |
| Verification | Service 单元测试通过：空文件/短文本/超长文本/中文换行/特殊字符分块 |

**设计要点**：
- 上传接口接收 `MultipartFile`，限制 5MB，仅接受 `.txt`
- 文件存储到本地目录（`ciff.upload.path`，默认 `./uploads/`），配置外化
- 分块逻辑：按固定 `chunk_size` 字符分割，尽量在换行符/句号处切分（简化版：按字符数硬切，误差 < 10%）
- 分块后状态流转：`uploading → processing → ready`（同步完成，V1 不引入异步队列）
- `t_knowledge_document` 记录文件元数据 + `chunk_count`

---

### Step 4：ciff-knowledge — Embedding 生成 + PGVector 存储/检索

| 项 | 内容 |
|----|------|
| Task | 调用 Provider Embedding API → 生成向量 → 写入 t_knowledge_chunk → 实现 top-k 相似度检索 |
| Output | `EmbeddingService.java` / `VectorStoreService.java` |
| Verification | 集成测试（Mock embedding）：固定向量写入 → 检索 → Top 3 余弦相似度验证 |

**设计要点**：
- `EmbeddingService`：通过 `ProviderFacade` 获取 Provider 配置，用 `WebClient` 调用 OpenAI 兼容的 `/v1/embeddings` 接口
- 请求格式：`{"input": ["text1", "text2"], "model": "text-embedding-ada-002"}`
- 响应解析：提取 `data[].embedding`（float[1024]）
- `VectorStoreService`：使用 JdbcTemplate
  - `insertChunks(Long documentId, Long knowledgeId, List<ChunkData> chunks)` — 批量写入
  - `search(Long knowledgeId, float[] queryEmbedding, int topK, float minScore)` — 余弦相似度检索
- PGVector SQL：`SELECT content, embedding <=> ?::vector AS distance ... ORDER BY embedding <=> ?::vector LIMIT ?`
- **降级选项**：如果 Provider embedding API 调用过于复杂，可先预留接口，用随机/固定向量完成 PGVector 读写测试；待联调时接入真实 API

---

### Step 5：ciff-knowledge — 对外 Facade 与 Controller

| 项 | 内容 |
|----|------|
| Task | KnowledgeFacade 暴露给 ciff-agent / ciff-chat；内部 Controller 提供知识库管理 API |
| Output | 完整的 Facade 接口 + Controller（CRUD + 上传 + 文档列表） |
| Verification | Controller 切片测试通过 |

---

### Step 6：ciff-agent — t_agent_knowledge 关联管理

| 项 | 内容 |
|----|------|
| Task | AgentKnowledgePO / Mapper / Service；绑定/解绑/批量替换/查询 Agent 详情时返回知识库列表 |
| Output | `AgentKnowledgePO.java` / `AgentKnowledgeMapper.java` / `AgentKnowledgeService.java` / `AgentKnowledgeServiceImpl.java` |
| Verification | Service 单元测试通过：绑定/解绑/重复绑定校验 |

**设计要点**：
- 遵循已有 `AgentToolService` 模式
- `AgentVO` 新增 `knowledges` 字段（`List<KnowledgeVO>`）
- Agent 详情查询时 JOIN 知识库信息

---

### Step 7：ciff-app — 聚合 Controller + 模型校验

| 项 | 内容 |
|----|------|
| Task | `AppKnowledgeController` 聚合知识库 CRUD；`AppAgentController` 扩展知识库绑定接口 |
| Output | `AppKnowledgeController.java` + AppAgentController 新增知识库相关端点 |
| Verification | Controller 切片测试通过 |

**端点设计**：
- `GET /api/v1/app/knowledges` — 分页列表
- `POST /api/v1/app/knowledges` — 创建
- `PUT /api/v1/app/knowledges/{id}` — 更新
- `DELETE /api/v1/app/knowledges/{id}` — 删除
- `GET /api/v1/app/knowledges/{id}` — 详情（含文档列表）
- `POST /api/v1/app/knowledges/{id}/documents` — 上传文档
- `GET /api/v1/app/knowledges/{id}/documents` — 文档列表
- `PUT /api/v1/app/agents/{id}/knowledges` — 全量替换 Agent 知识库绑定（参照 tools）

---

### Step 8：前端 — 知识库管理页面

| 项 | 内容 |
|----|------|
| Task | 知识库列表 / 创建编辑 / 文档上传列表 / 分块状态展示 |
| Output | `KnowledgeList.vue` + `knowledge.ts` API 层 |
| Verification | Vitest 单元测试 + 手动联调 |

**页面结构**：
- 列表页：CiffTable + 创建按钮
- 弹窗表单：名称、描述、chunk_size（滑块/数字输入）、embedding_model（下拉）
- 详情/文档页：知识库基本信息 + 文档列表（文件名、大小、状态、chunk_count）+ 上传按钮
- 状态标签：`uploading` 灰色 / `processing` 蓝色 / `ready` 绿色 / `failed` 红色

---

### Step 9：前端 — Agent 编辑页知识库绑定

| 项 | 内容 |
|----|------|
| Task | AgentList.vue 编辑弹窗增加知识库多选 |
| Output | 更新 `AgentList.vue` |
| Verification | 手动联调：创建 Agent → 绑定知识库 → 详情回显 → 解绑 → 保存 |

---

### Step 10：Phase 3 测试节点

| 项 | 内容 |
|----|------|
| Task | 编写并运行全部测试用例 |
| Verification | `mvn test -pl ciff-knowledge,ciff-agent` 全部通过；前端 Vitest 通过 |

**测试清单**：

| 编号 | 类型 | 用例 | 覆盖 |
|------|------|------|------|
| T3.1 | Controller 切片 | `KnowledgeControllerTest`：CRUD / user_id 隔离 / chunk_size 边界 | ciff-knowledge |
| T3.2 | Service 单元 | `KnowledgeServiceTest`：chunk_size 范围校验 / embedding_model 校验 | ciff-knowledge |
| T3.3 | Mapper 集成 | `KnowledgeMapperTest`：CRUD + user_id 查询 | ciff-knowledge |
| T3.4 | Service 单元 | `DocumentChunkServiceTest`：空文件 / 短文本 / 超长文本 / 中文 / 特殊字符 | ciff-knowledge |
| T3.5 | 集成 | `EmbeddingServiceTest`：Mock embedding 生成 + PGVector 写入 + 检索 | ciff-knowledge |
| T3.6 | 单元 | `VectorStoreTest`：top-k 检索 / 相似度阈值 / 空结果 / knowledge_id 隔离 | ciff-knowledge |
| T3.6a | 集成 | `DualDataSourceTest`：验证双数据源配置正确 | ciff-knowledge |
| T3.7 | Service 单元 | `AgentKnowledgeServiceTest`：绑定 / 解绑 / 重复绑定校验 | ciff-agent |
| T3.8 | 前端单元 | `KnowledgeList.spec.ts`：列表 / 上传组件 / 状态流转 | ciff-web |

---

## 三、依赖关系

```
Step 1 (双数据源)
    │
    ├──→ Step 2 (Knowledge CRUD)
    │       │
    │       ├──→ Step 3 (文档上传+分块)
    │       │       │
    │       │       ├──→ Step 4 (Embedding+向量)
    │       │       │
    │       │       └──→ Step 5 (Facade+Controller)
    │       │
    │       └──→ Step 6 (AgentKnowledge 关联) ──→ Step 7 (聚合 Controller)
    │                                                   │
    └──→ Step 8 (前端知识库页面) ←──────────────────────┤
            │                                           │
            └──→ Step 9 (Agent 知识库绑定) ←────────────┘
                        │
                        └──→ Step 10 (测试节点)
```

**并行机会**：
- Step 2 和 Step 6 可并行（知识库 CRUD 与 AgentKnowledge 关联互不依赖）
- Step 8 可在 Step 5 完成后开始（前端只需等 API 就绪）

---

## 四、验收标准（来自主计划）

1. `mvn test -pl ciff-knowledge,ciff-agent` 全部通过；Mapper 集成测试在真实 MySQL 环境执行
2. TXT 分块：空文件报错、单段落不截断、超长文本按 chunk_size 分割，长度误差 < 10%
3. PGVector 集成测试通过，向量检索 Top 3 余弦相似度 > 0.7（单测中用固定 embedding 验证）
4. 上传非 TXT 文件返回 400；上传 5MB 以内 TXT 成功，状态流转 uploading → processing → ready
5. 前端 Vitest 通过；上传进度和分块状态与后端一致
6. 向量检索 1000 chunks 场景 P95 < 500ms；文件分块（< 1MB）P95 < 3s

---

## 五、技术决策记录

| 决策 | 选择 | 原因 |
|------|------|------|
| PGVector 访问方式 | JdbcTemplate + 原生 SQL | mybatis-plus 对 `vector` 类型支持不佳，原生 SQL 最直接 |
| Embedding API 调用 | WebClient 直接调用（复用 Provider 配置） | 不引入新 HTTP 客户端，与现有 LlmHttpClient 风格一致 |
| 文件存储 | 本地文件系统（路径配置外化） | V1 不引入对象存储，最简方案 |
| 分块策略 | 固定字符长度切分（误差 < 10%） | V1 降级，不做语义分块 |
| 文档处理同步/异步 | 同步处理（上传→分块→向量化一气呵成） | V1 不引入 Redis Stream 异步队列，Phase 4/5 再评估 |

---

## 六、风险与降级

| 风险 | 影响 | 降级方案 |
|------|------|----------|
| PGVector 本地环境不可用 | Step 4/5 阻塞 | 先用 H2 内存数据库模拟向量表结构，或跳过 PGVector 集成测试（标记为 `@Disabled` + 注释说明） |
| Embedding API 调用复杂 | Step 4 延期 | 先用固定随机向量填充，保证 PGVector 读写检索链路跑通 |
| 双数据源配置踩坑 | Step 1 阻塞 | 简化方案：将 t_knowledge_chunk 的 vector 存为 float[] JSON 到 MySQL（性能差但可用） |