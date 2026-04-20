# Ciff - AI Agent 开发平台

> Ciff = **C**ode **I**t **F**or **F**uture

## 产品定位

简化版 Dify，面向团队内部 20-50 人的 AI Agent 开发与运行平台。本地部署，一个人开发。

## 做什么（V1 范围）

- **Agent 智能助手**：工具调用 + 多轮对话，核心形态
- **Chatbot 对话应用**：基础对话，最高频入口
- **Workflow 工作流（降级）**：JSON 配置，线性步骤 + 条件分支，覆盖"分类→路由"场景，不做可视化拖拽
- **模型供应商适配**：统一接口，先接 2-3 家（OpenAI / Claude / 本地模型）
- **模型参数配置**：temperature、max_tokens 等基础参数
- **知识库 RAG（降级）**：仅支持 TXT 文档，固定长度分块，单一向量库（PGVector）
- **自定义工具 / API**：Agent 可调用外部接口
- **Web + API 发布**：对话界面 + REST API
- **日志追踪**：记录完整调用链，用于调试排错

## 不做什么（V1 明确排除）

- 可视化工作流编辑器
- 多文档格式解析（PDF / Word / Excel）
- 混合检索 / Rerank
- 插件 / 市场系统
- MCP Server 发布、嵌入式小组件
- 数据统计分析 Dashboard
- 多环境隔离（dev / staging / prod）
- 复杂权限矩阵（只做管理员 / 普通用户）
- 应用模板市场
- 多向量库抽象层

## 技术栈

### 后端
- JDK 17+ / Spring Boot 3.3.x
- MyBatis-Plus 3.5.x
- MySQL 8.x（主存储）
- Redis 7.x（缓存 + 会话）
- WebClient + Reactor Netty（LLM HTTP 调用，同步 + SSE 流式）
- Resilience4j 2.x（熔断 / 重试 / 限流）
- Spring Boot Actuator（健康检查 + 基础指标）
- Maven 项目构建

### 前端
- Vue 3.4+ / TypeScript 5.x
- Element Plus 2.x
- Vite 5.x
- SSE（打字机流式输出）

### 基础设施
- Nginx（反向代理 + SSE 透传 + 静态资源托管）
- Docker + Docker Compose（单机编排）

## 详细规范索引

| 文件 | 内容 | 代码示例备份 |
|------|------|-------------|
| [01-模块结构与依赖规范](rules/01-module-structure.md) | 项目结构、模块依赖、内部分层、命名约定、跨模块调用 | — |
| [02-LLM 调用技术方案](rules/02-llm-calling.md) | 线程池职责、Resilience4J 容错、四级超时、重试策略、调用链路 | `docs/rules-snippets/02-threadpool-config.java` `02-resilience4j-config.yml` |
| [03-异步任务方案](rules/03-async-tasks.md) | @Async 与 Redis Stream 分层策略 | `docs/rules-snippets/03-async-examples.java` |
| [04-部署架构](rules/04-deployment.md) | 组件拓扑、Nginx SSE 配置、备份策略、关键参数 | `docs/rules-snippets/04-docker-compose.yml` |
| [05-工程/编码/接口/测试规范](rules/05-conventions.md) | 工程规范、REST 接口规范、Java 编码规范、测试规范 | — |
| [06-数据库性能规范](rules/06-database-performance.md) | 索引原则、大表应对、分页规范、SQL 规范、事务规范 | `docs/rules-snippets/06-create-table-templates.sql` `06-full-index-list.sql` |
| [10-前端开发规范](rules/10-frontend.md) | 技术栈、设计系统、布局规范、编码规范、API 层规范 | `docs/rules-snippets/10-frontend-examples.vue` |

## 行为指令

### 写代码时
- 每个功能用最简单直接的方式实现
- 不引入不必要的设计模式，除非我明确要求
- 不做过度抽象
- 不引入技术栈以外的依赖，需要时先问我
- 所有外部调用必须有超时设置
- 配置项外化到 application.yml，不硬编码

### 改代码时
- 先理解相关模块的设计意图
- 不要为了新功能破坏已有接口契约
- 改完确保已有测试通过

### 不确定时
- 架构选择给我 2-3 个方案对比，我来拍板
- 规范没覆盖的情况，先问我，不要自己编规矩
