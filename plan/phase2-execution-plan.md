# Phase 2 执行计划：Agent 基础 + MCP 工具

> **目标**：实现 Tool CRUD + Agent CRUD + 工具绑定，前后端完整链路。

## 前置依赖

- Phase 0 ✅（UserContext、枚举、Flyway、AES）
- Phase 1 ✅（Provider/Model CRUD + LLM Client + 前端 Provider 页面）

## 代码模式参考

严格遵循 Provider 模块已有的分层模式：
- Entity → Mapper → Service → Controller → Facade
- Convertor 纯静态方法
- Service 中 requireExists / validateNameUnique 模式
- 测试集中在 ciff-app，Controller 用 @WebMvcTest，Service 用 @ExtendWith(MockitoExtension.class)

---

## 实施步骤

### Step 1: ciff-mcp — Tool CRUD 后端（任务 2.1）

**Task**: 实现 t_tool 的 Entity / Mapper / Service / Facade / DTO / Convertor

**Output**: ciff-mcp 模块完整 CRUD 代码

**子步骤**:
1. `ToolPO` — 映射 t_tool 表，继承 SoftDeletableEntity，字段：name, description, type(api/mcp), endpoint, paramSchema(JSON), authConfig(JSON), status
2. `ToolMapper` — 继承 BaseMapper<ToolPO>
3. `ToolCreateRequest` / `ToolUpdateRequest` / `ToolVO` — DTO，含 JSR-303 校验
4. `ToolConvertor` — Request→PO、PO→VO
5. `ToolService` / `ToolServiceImpl` — CRUD + 分页 + 重复 name 校验 + status 筛选
6. `ToolFacade` / `ToolFacadeImpl` — 跨模块调用接口（ciff-agent 需要查 Tool）
7. `ToolController` — REST API `/api/v1/tools`

**Verification**: 编译通过 + ToolServiceTest + ToolControllerTest

---

### Step 2: ciff-agent — Agent CRUD 后端（任务 2.2 + 2.3）

**Task**: 实现 t_agent + t_agent_tool 的完整后端

**Output**: ciff-agent 模块完整 CRUD + 工具关联

**子步骤**:
1. `AgentToolPO` — 映射 t_agent_tool（不继承 SoftDeletableEntity，仅有 id/agentId/toolId/createTime）
2. `AgentPO` — 映射 t_agent，继承 SoftDeletableEntity，含 user_id / model_id / type / system_prompt / model_params / fallback_model_id
3. `AgentMapper` + `AgentToolMapper` — 继承 BaseMapper
4. `AgentCreateRequest` / `AgentUpdateRequest` / `AgentVO` / `AgentToolVO` — DTO
5. `AgentConvertor` — Request→PO、PO→VO
6. `AgentToolService` / `AgentToolServiceImpl` — 绑定/解绑/批量替换/查询 Agent 关联的工具
7. `AgentService` / `AgentServiceImpl` — Agent CRUD + 分页 + user_id 隔离 + name 校验
8. `AgentFacade` / `AgentFacadeImpl` — 对外提供 Agent 详情（含工具列表）
9. `AgentController` — REST API `/api/v1/agents`（模块内部，不对外暴露）

**Verification**: 编译通过 + AgentServiceTest + AgentToolServiceTest

---

### Step 3: ciff-app — Agent API 聚合层（任务 2.2.1）

**Task**: 新增 AppAgentController，聚合 Agent + Provider 校验

**Output**: `/api/v1/app/agents` 聚合接口

**子步骤**:
1. `AppAgentController` — 创建/更新 Agent 时校验 model_id / fallback_model_id 存在性
2. 调用 ProviderFacade 验证模型 → 调用 AgentService 落库
3. Agent 详情查询含关联工具列表
4. 工具绑定/解绑接口也在聚合层暴露

**Verification**: AppAgentControllerTest（含 model_id 外键校验场景）

---

### Step 4: 前端 — 工具管理页面（任务 2.4）

**Task**: 工具列表 + 新增/编辑弹窗

**Output**: `ciff-web/src/views/tool/ToolList.vue` + API 层

**子步骤**:
1. `ciff-web/src/api/tool.ts` — API 类型定义 + 请求函数
2. `ToolList.vue` — 列表（类型标签区分 api/mcp）+ 新增/编辑弹窗
3. 路由注册

**Verification**: ToolList.spec.ts + 手动联调

---

### Step 5: 前端 — Agent 管理页面（任务 2.5 + 2.6）

**Task**: Agent 列表 + 创建/编辑（含模型下拉、工具多选）

**Output**: `ciff-web/src/views/agent/AgentList.vue`（替换现有占位）

**子步骤**:
1. `ciff-web/src/api/agent.ts` — API 类型定义 + 请求函数
2. `AgentList.vue` — 列表 + 创建/编辑弹窗
   - 模型下拉：从 Provider 接口获取模型列表
   - Agent 类型：chatbot / agent / workflow
   - system_prompt 文本域
   - model_params JSON 编辑
   - 工具多选
3. 路由更新

**Verification**: AgentList.spec.ts + 手动联调

---

### Step 6: 测试补齐

**Task**: 补齐所有 Phase 2 测试

**Output**: 全部测试用例通过

**测试清单**（集中在 ciff-app）:

| 编号 | 测试类 | 覆盖 |
|------|--------|------|
| T2.1 | ToolControllerTest | CRUD / param_schema 格式 / 按 type 筛选 |
| T2.2 | ToolServiceTest | Schema 校验、重复 name、分页 |
| T2.3 | ToolMapperTest | 数据库 CRUD + status 筛选 |
| T2.4 | AppAgentControllerTest | CRUD / type 枚举 / model_id 外键校验 |
| T2.5 | AgentServiceTest | 创建/更新、Prompt 校验、user_id 隔离 |
| T2.6 | AgentMapperTest | CRUD + user_id 隔离 |
| T2.7 | AgentToolServiceTest | 绑定/解绑/批量替换/重复绑定校验 |
| T2.8 | AgentFacadeTest | Agent 详情含工具列表 |
| T2.9 | ToolList.spec.ts | 前端工具页 |
| T2.10 | AgentList.spec.ts | 前端 Agent 页 |

---

## 依赖关系（DAG）

```
Step 1 (Tool CRUD 后端) ──► Step 2 (Agent CRUD + Tool 绑定) ──► Step 3 (ciff-app 聚合)
        │
        ▼
Step 4 (前端 Tool 页面) ──► Step 5 (前端 Agent 页面)
                                              ▲
                              Step 3 完成（聚合 API 就绪）
```

## 并行机会

- Step 2 中 Agent CRUD 和 AgentToolService 可部分并行（AgentToolService 依赖 ToolFacade，但 AgentPO/Mapper 不依赖）
- 测试与开发 TDD 并行
- 前后端严格串行：先完成后端 API，再写前端页面

## 测试门禁

每个 Step 完成后运行：`mvn test -pl :ciff-app -am`
前端：`cd ciff-web && npx vitest run`

## 验收标准

1. `mvn test -pl :ciff-app -am` 全部通过
2. Tool param_schema 支持 JSON Schema 基础校验
3. Agent CRUD + 工具绑定测试全部通过
4. Agent 详情查询含关联工具列表
5. 前端 Vitest 通过
6. Agent model_id 校验在 ciff-app 聚合层完成
