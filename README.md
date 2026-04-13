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

## 开发进度

### 基础框架搭建 (2026-04-13)

- [x] Maven 多模块工程骨架（8 子模块，分层依赖）
- [x] 统一响应 `Result<T>` / 分页 `PageResult<T>`
- [x] 统一异常处理 `BizException` + `ErrorCode` + `GlobalExceptionHandler`
- [x] MyBatis-Plus 配置（分页插件、自动填充 createTime/updateTime、逻辑删除）
- [x] Redis 配置（Redisson + JSON 序列化 + RedisUtil）
- [x] Spring Boot 启动类 + application.yml
- [x] 项目规范文档（模块结构、数据库、接口、编码、测试）
- [x] 健康检查接口 `GET /api/v1/health`

### 进行中

- [ ] Provider 模块：模型供应商 CRUD + LLM 调用封装

### 待开发

- [ ] MCP 工具管理
- [ ] 知识库 + RAG
- [ ] Agent 管理 + 工具编排
- [ ] Workflow 引擎
- [ ] Chat 对话引擎
- [ ] 前端界面
- [ ] Docker 部署
