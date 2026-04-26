# Ciff

> **C**ode **I**t **F**or **F**uture

简化版 Dify，面向小团队的 AI Agent 开发与运行平台。

## V1 功能范围

| 功能 | 说明 |
|------|------|
| Agent 智能助手 | 工具调用 + 多轮对话，核心形态 |
| Chatbot 对话应用 | 基础对话，最高频入口 |
| Workflow 工作流 | JSON 配置，线性步骤 + 条件分支，不做可视化拖拽 |
| 模型供应商适配 | 统一接口，OpenAI / Claude / 本地模型 |
| 知识库 RAG | TXT 文档，固定长度分块，PGVector |
| 自定义工具 / API | Agent 可调用外部接口 |
| Web + API 发布 | 对话界面 + REST API |
| 日志追踪 | 完整调用链，用于调试排错 |

## 技术栈

**后端** — JDK 17 / Spring Boot 3.3 / MyBatis-Plus / MySQL / Redis (Redisson) / Spring AI / Resilience4j

**前端** — Vue 3 / TypeScript / Element Plus / Vite / SSE

**基础设施** — Nginx + Docker Compose

## 项目结构

```
ciff/
├── ciff-common/      # 公共模块（工具类、异常、DTO、配置）
├── ciff-provider/    # 模型供应商管理
├── ciff-mcp/         # MCP 工具管理与调用
├── ciff-knowledge/   # 知识库与 RAG
├── ciff-agent/       # Agent 管理与编排
├── ciff-workflow/    # 工作流编排与执行
├── ciff-chat/        # 对话引擎（顶层编排）
├── ciff-app/         # Spring Boot 启动模块
└── rules/            # 编码规范与设计文档
```

## 产品展示

### 页面一览

<table>
  <tr>
    <td align="center"><b>供应商管理</b></td>
    <td align="center"><b>模型管理</b></td>
  </tr>
  <tr>
    <td><img src="image/provider.png" alt="供应商管理" width="100%"></td>
    <td><img src="image/model.png" alt="模型管理" width="100%"></td>
  </tr>
  <tr>
    <td align="center"><b>工具管理</b></td>
    <td align="center"><b>Agent 管理</b></td>
  </tr>
  <tr>
    <td><img src="image/tool.png" alt="工具管理" width="100%"></td>
    <td><img src="image/agent.png" alt="Agent 管理" width="100%"></td>
  </tr>
  <tr>
    <td align="center"><b>知识库管理</b></td>
    <td align="center"><b>文档管理</b></td>
  </tr>
  <tr>
    <td><img src="image/knowledge.png" alt="知识库管理" width="100%"></td>
    <td><img src="image/knowledge-documents.png" alt="文档管理" width="100%"></td>
  </tr>
  <tr>
    <td align="center"><b>召回测试</b></td>
    <td align="center"><b>对话页面</b></td>
  </tr>
  <tr>
    <td><img src="image/recall-test.png" alt="召回测试" width="100%"></td>
    <td><img src="image/chat.png" alt="对话页面" width="100%"></td>
  </tr>
  <tr>
    <td align="center" colspan="2"><b>对话示例 — SSE 流式 + Markdown 渲染</b></td>
  </tr>
  <tr>
    <td colspan="2"><img src="image/chat_example.png" alt="对话示例" width="100%"></td>
  </tr>
</table>

### 弹窗/表单

<table>
  <tr>
    <td align="center"><b>新增供应商</b></td>
    <td align="center"><b>新增工具</b></td>
  </tr>
  <tr>
    <td><img src="image/provider-create.png" alt="新增供应商" width="100%"></td>
    <td><img src="image/tool-create.png" alt="新增工具" width="100%"></td>
  </tr>
  <tr>
    <td align="center"><b>创建知识库</b></td>
    <td align="center"><b>创建 Agent</b></td>
  </tr>
  <tr>
    <td><img src="image/knowledge-create.png" alt="创建知识库" width="100%"></td>
    <td><img src="image/agent-create.png" alt="创建 Agent" width="100%"></td>
  </tr>
  <tr>
    <td align="center"><b>选择 Agent（对话）</b></td>
    <td align="center"><b>编辑工作流（JSON）</b></td>
  </tr>
  <tr>
    <td><img src="image/chat-agent-selector.png" alt="选择 Agent" width="100%"></td>
    <td><img src="image/edit_workflow.png" alt="编辑工作流 JSON" width="100%"></td>
  </tr>
  <tr>
    <td align="center"><b>编辑工作流（流程图）</b></td>
    <td align="center"><b>执行工作流 — 任务已提交</b></td>
  </tr>
  <tr>
    <td><img src="image/edit_workflow_2.png" alt="编辑工作流 流程图" width="100%"></td>
    <td><img src="image/handle_workflow.png" alt="执行工作流" width="100%"></td>
  </tr>
  <tr>
    <td align="center"><b>任务详情 — 步骤执行</b></td>
    <td align="center"><b>任务详情 — 最终结果</b></td>
  </tr>
  <tr>
    <td><img src="image/handle_workflow_2.png" alt="任务详情 步骤执行" width="100%"></td>
    <td><img src="image/handle_workflow_3.png" alt="任务详情 最终结果" width="100%"></td>
  </tr>
  <tr>
    <td align="center"><b>任务详情 — 执行失败</b></td>
    <td align="center"><b>历史任务列表</b></td>
  </tr>
  <tr>
    <td><img src="image/error_handle_workflow.png" alt="执行失败" width="100%"></td>
    <td><img src="image/history_workflow.png" alt="历史任务" width="100%"></td>
  </tr>
</table>

## 开发进度

### Phase 1: 基础框架搭建 (2026-04-13 ~ 2026-04-15)

- [x] Maven 多模块工程骨架（8 子模块，分层依赖）
- [x] 统一响应 `Result<T>` / 分页 `PageResult<T>`
- [x] 统一异常处理 `BizException` + `ErrorCode` + `GlobalExceptionHandler`
- [x] MyBatis-Plus 配置（分页插件、自动填充 createTime/updateTime、逻辑删除）
- [x] Redis 配置（Redisson + JSON 序列化 + RedisUtil）
- [x] Spring Boot 启动类 + application.yml
- [x] 项目规范文档（模块结构、数据库、接口、编码、测试）
- [x] 健康检查接口 `GET /api/v1/health`
- [x] Flyway 数据库迁移管理
- [x] 请求日志拦截 + LLM 日志脱敏

### Phase 2: Provider 模型供应商 (2026-04-15 ~ 2026-04-17)

- [x] 供应商 CRUD（支持 OpenAI / Claude / 本地模型）
- [x] 模型管理 CRUD + 默认参数配置
- [x] LLM HTTP 客户端（WebClient + Reactor Netty，同步 + SSE 流式）
- [x] Claude 客户端适配 + LLM 客户端工厂抽象
- [x] 供应商健康检查定时任务
- [x] Resilience4j 熔断保护
- [x] LLM 调用超时配置（四级超时策略）
- [x] API Key 加密存储

### Phase 3: Agent / MCP / Knowledge 模块 (2026-04-17 ~ 2026-04-19)

- [x] MCP 工具管理 CRUD + Facade 层
- [x] Agent 管理 CRUD + Agent-Tool 绑定关系
- [x] Agent 聚合控制器（模型校验、统一入口）
- [x] 知识库 CRUD + PGVector 双数据源配置
- [x] 文档管理（上传、分块、向量化、定时处理调度）
- [x] 本地文件存储 + TXT 固定长度分块
- [x] 缓存层支持（Provider / Agent 详情缓存）

### Phase 4: 前端界面 (2026-04-17 ~ 2026-04-21)

- [x] 前端工程初始化（Vue 3 + TypeScript + Element Plus + Vite）
- [x] 设计系统与公共组件库（CiffTable / CiffFormDialog / PageHeader / TopBar）
- [x] 供应商管理页面
- [x] 模型管理页面
- [x] 工具管理页面
- [x] Agent 管理页面
- [x] Chat 对话页面（会话列表 + SSE 流式 + 消息历史 + RAG 模式切换）

### 待开发

### Phase 5: Workflow 工作流 (2026-04-22 ~ 2026-04-23)

- [x] Workflow CRUD + JSON 定义校验（步骤类型 / ID 唯一 / nextStepId 引用 / DFS 循环检测）
- [x] 工作流执行引擎（llm / tool / condition / knowledge_retrieval 四种步骤类型）
- [x] 变量插值 `${stepId.output.xxx}` / `${inputs.xxx}` + WorkflowContext
- [x] 工具参数校验（param_schema required + 类型强转）
- [x] 工作流管理前端页面（列表 + JSON 编辑器 + 执行弹窗 + 动态输入参数）
- [x] 9 个后端测试 + 1 个前端测试全部通过

### Phase 6: 认证 + API 发布 + 部署 (2026-04-25)

- [x] Sa-Token + JWT 认证（登录 / 注册 / 登出 / Token Redis 持久化）
- [x] GitHub OAuth 2.0 自动注册登录
- [x] API Key 管理（生成 / 列表 / 撤销 / SHA256 哈希存储）
- [x] 外部对话接口（API Key 认证，`/api/v1/external/chat`）
- [x] 前端登录页 + 路由守卫 + Token 管理
- [x] API Key 管理页面
- [x] Docker Compose 部署（ciff-app + Nginx 反向代理 + SSE 透传）
- [x] 全局 UI 打磨（空状态、错误提示、响应式侧边栏）
