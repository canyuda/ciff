# Ciff - AI Agent 开发平台

> Ciff = **C**ode **I**t **F**or **F**uture

## 项目概述

Ciff 是一个面向 20-50 人小团队的简化版 Dify，定位于本地部署的 AI Agent 开发与运行平台。当前项目处于早期开发阶段（基础框架已搭建），采用 Maven 多模块后端 + Vue 3 前端的全栈架构。

### V1 功能范围

- **Agent 智能助手**：工具调用 + 多轮对话，核心形态
- **Chatbot 对话应用**：基础对话，最高频入口
- **Workflow 工作流**：JSON 配置，线性步骤 + 条件分支，不做可视化拖拽
- **模型供应商适配**：统一接口，OpenAI / Claude / 本地模型
- **知识库 RAG**：仅支持 TXT 文档，固定长度分块，PGVector
- **自定义工具 / API**：Agent 可调用外部接口
- **Web + API 发布**：对话界面 + REST API
- **日志追踪**：完整调用链，用于调试排错

## 技术栈

### 后端
- **JDK 17** / **Spring Boot 3.3.6**
- **Maven** 多模块工程（8 个子模块）
- **MyBatis-Plus 3.5.9**（ORM + 分页插件 + 自动填充 + 逻辑删除）
- **MySQL 8.0.33**（主存储）
- **Redis 7.x**（缓存 + 会话），通过 **Redisson 4.3.1** 接入
- **Spring AI 1.0.0**（LLM 统一调用层）
- **Resilience4j 2.2.0**（熔断 / 重试 / 限流 / 隔离）
- **Spring Boot Actuator**（健康检查）
- **Lombok**（全模块共享）

### 前端
- **Vue 3.5.32** / **TypeScript ~6.0.2**
- **Element Plus 2.13.7**
- **Vite 8.0.4**
- **Pinia 3.0.4**（状态管理）
- **Axios**（HTTP 客户端）
- **SSE**（打字机流式输出）
- **Vitest 4.1.4**（前端测试）

### 基础设施
- **Nginx**（反向代理 + SSE 透传 + 静态资源托管）
- **Docker + Docker Compose**（单机部署）
- **PGVector**（知识库向量存储）

## 项目结构

```
ciff/
├── ciff-app/               # Spring Boot 启动模块（唯一可执行入口）
├── ciff-chat/              # 对话引擎（顶层业务编排）
├── ciff-workflow/          # 工作流编排与执行
├── ciff-agent/             # Agent 管理与配置
├── ciff-knowledge/         # 知识库与 RAG
├── ciff-mcp/               # MCP 工具管理与调用
├── ciff-provider/          # 模型供应商管理
├── ciff-common/            # 公共模块（工具类、常量、异常、DTO、配置）
├── ciff-web/               # Vue 3 前端项目
├── rules/                  # 编码规范与设计文档（9 份规则文件）
├── docs/                   # 架构文档、数据库设计、演进路径
├── deploy/                 # Docker Compose 部署配置（规划中）
├── start.sh / stop.sh      # 本地开发启停脚本
├── Makefile                # 常用构建命令
└── pom.xml                 # 根 POM
```

### 模块依赖关系（严格单向，禁止反向引用）

```
Layer 0: ciff-common
Layer 1: ciff-provider, ciff-mcp
Layer 2: ciff-knowledge         （依赖 provider）
Layer 3: ciff-agent             （依赖 mcp + knowledge）
Layer 4: ciff-workflow          （依赖 agent + knowledge + provider）
Layer 5: ciff-chat              （依赖所有业务模块）
Layer 6: ciff-app               （组装入口）
```

### 模块内部包结构规范

所有业务模块统一遵循以下结构：

```
com.ciff.{module}
├── facade/          # 对外接口（跨模块调用的唯一入口）
├── controller/      # REST 接口层
├── service/         # 业务逻辑层
├── mapper/          # MyBatis-Plus Mapper
├── entity/          # 数据库实体（PO）
├── dto/             # 数据传输对象（DTO / Request / VO）
├── convertor/       # PO ↔ DTO / VO 转换器
├── constant/        # 模块级常量
├── exception/       # 模块内自定义异常
└── config/          # 模块级 Spring 配置
```

**跨模块调用规则**：
1. 只能通过 `Facade` 接口调用，禁止直接注入其他模块的 Service / Mapper
2. Facade 方法参数和返回值只能用 DTO，禁止出现 PO 类型
3. 统一使用构造器注入（`@RequiredArgsConstructor`），禁止 `@Autowired` 字段注入
4. 反向依赖会导致 Maven 编译失败

## 构建与运行命令

### 本地开发（一键启停）

```bash
# 启动前后端（自动检查 MySQL / Redis，编译后端，启动前端 dev server）
make start
# 或
./start.sh

# 停止服务
make stop
# 或
./stop.sh

# 重启
make restart
```

启动后访问地址：
- 后端 API：`http://localhost:8080`
- 前端页面：`http://localhost:3000`
- 健康检查：`GET http://localhost:8080/api/v1/health`

### 构建

```bash
# 构建前后端（不运行测试）
make build

# 单独构建后端
mvn package -pl ciff-app -am -DskipTests -q

# 单独构建前端
cd ciff-web && npm run build

# 清理
make clean

# 打包发布产物（jar + dist + deploy）
make package
```

### 前端开发

```bash
cd ciff-web
npm install    # 依赖已存在 node_modules 中
npm run dev    # Vite dev server（端口 3000，已代理 /api 到 8080）
npm run build  # 生产构建
npm run test   # 运行 Vitest
```

### Maven 构建细节

- 根 POM 使用 `spring-boot-starter-parent:3.3.6`
- Java 版本：**17**
- 编码：**UTF-8**
- 编译参数保留方法参数名（`-parameters`）

## 代码风格与规范

### 命名约定

| 类别 | 格式 | 示例 |
|------|------|------|
| Facade 接口 | `{Module}Facade` | `AgentFacade` |
| Facade 实现 | `{Module}FacadeImpl` | `AgentFacadeImpl` |
| Service | `{Entity}Service` | `AgentToolService` |
| Controller | `{Module}Controller` | `AgentController` |
| Mapper | `{Entity}Mapper` | `AgentMapper` |
| 数据库实体 | `{Entity}PO` | `AgentPO` |
| Facade 传参 | `{Entity}DTO` | `AgentDTO` |
| 请求入参 | `{Entity}{Action}Request` | `AgentCreateRequest` |
| 响应出参 | `{Entity}VO` | `AgentVO` |
| 转换器 | `{Entity}Convertor` | `AgentConvertor` |
| 常量类 | `{Module}Constants` | `AgentConstants` |
| 异常类 | `{Entity}{Status}Exception` | `AgentNotFoundException` |
| 数据库表名 | `t_{module}_{entity}` | `t_agent` |
| 数据库字段 | `snake_case` | `model_name` |

### 统一响应与异常

- 所有 Controller 返回 `Result<T>`：
  ```json
  { "code": 200, "message": "success", "data": {...} }
  ```
- 分页返回 `Result<PageResult<T>>`，`PageResult` 包含 `list`、`total`、`page`、`pageSize`
- 业务异常基类：`BizException`（`ciff-common`）
- 全局异常处理：`GlobalExceptionHandler`（`ciff-common`）
- 错误码为 4 位数字，按模块分段：
  - 1000-1999 通用
  - 2000-2999 Provider
  - 3000-3999 Agent
  - 4000-4999 Chat
  - 5000-5999 MCP
  - 6000-6999 Workflow
  - 7000-7999 Knowledge

### 接口规范

- RESTful 路径：`/api/v1/{资源复数名}`
- 分页参数：`page`（从 1 开始）、`pageSize`（默认 20，最大 100）
- 空列表返回 `[]`，空字符串返回 `""`，对象不存在返回 `null`
- 文档使用 Swagger，导出地址 `/v3/api-docs`

### 核心编码约束

1. **外部调用必须有超时设置**（TCP 5s、首 Token 30s、Token 间隔 15s、SSE 整体 180s）
2. **配置项必须外化到 `application.yml`**，禁止硬编码
3. **线程池必须手动创建** `ThreadPoolExecutor`，禁止 `Executors.newXxx()`
4. **日志使用 SLF4J + `{}` 占位符**，禁止 `+` 拼接
5. **抛异常前必须打 ERROR/WARN 日志**，并保留完整异常对象和堆栈
6. **LLM 日志脱敏**：不打印完整请求/响应内容，只记录 agentId、model、token 用量、耗时、状态码
7. **优先函数式编程范式**
8. **SSE 接口必须用异步模式**，创建 `SseEmitter` 后立即返回，不能阻塞 Tomcat 线程

### 缓存 Key 规范

| 缓存对象 | Key 格式 | TTL | 策略 |
|----------|---------|-----|------|
| Agent 配置 | `agent:{id}` | 30min | 更新时主动失效 |
| Workflow 定义 | `workflow:{id}` | 30min | 更新时主动失效 |
| 模型供应商配置 | `model:provider:{id}` | 1h | 更新时主动失效 |
| 用户会话 | `session:{token}` | 跟随 JWT | 过期自动清除 |

## 测试规范

### 测试框架
- JUnit 5 + Spring Boot Test + MockMvc
- Mockito + AssertJ（`spring-boot-starter-test` 内置）
- 前端：Vitest

### 测试分层

| 层 | 测试方式 | 注解/工具 |
|---|---------|----------|
| Controller | 切片测试 | `@WebMvcTest` + MockMvc |
| Service/Facade | 纯单元测试 | `@ExtendWith(MockitoExtension.class)` |
| Mapper | 集成测试（连真实库） | `@MybatisPlusTest` |
| Util/Convertor | 纯单元测试 | 纯 JUnit 5 |

### 命名约定
- 测试类：`{Target}Test`（如 `AgentServiceTest`）
- 测试方法：`{method}_{scenario}_{expected}`（如 `createAgent_whenNameExists_throwException`）

### 禁止事项
- 不写无意义测试（如只测 getter/setter）
- 不测框架本身功能
- 不在单元测试中启动完整 Spring Context
- 不要求 100% 覆盖率，核心业务逻辑优先

## 部署架构

采用 **Docker Compose 单机部署**：

```
Nginx (:80/:443)
  ├── /api/*  → ciff-server (:8080)
  └── /*      → ciff-web (静态资源)

ciff-server 依赖：MySQL (:3306) + Redis (:6379) + PGVector (:5432)
```

### 关键部署配置
- Nginx SSE 透传必须关闭缓冲：`proxy_buffering off`
- JVM 堆内存：`-Xms512m -Xmx512m`
- MySQL `innodb_buffer_pool_size`：256M
- Redis `maxmemory`：128MB，LRU 淘汰策略
- Docker 日志：`json-file` driver，`max-size: 10m`，`max-file: 3`

### 数据库分布
- **MySQL 8.x**：13 张业务表（用户、供应商、模型、Agent、工具、工作流、知识库、对话、API Key 等）
- **PGVector**：1 张向量表（`t_knowledge_chunk`，含 embedding 字段）

## 开发状态与进度

> 截至 2026-04-13 的基础框架搭建已完成：
> - Maven 多模块工程骨架
> - 统一响应 `Result<T>` / 分页 `PageResult<T>`
> - 统一异常处理 `BizException` + `ErrorCode` + `GlobalExceptionHandler`
> - MyBatis-Plus 配置（分页插件、自动填充 createTime/updateTime、逻辑删除）
> - Redis 配置（Redisson + JSON 序列化 + `RedisUtil`）
> - Spring Boot 启动类 + `application.yml`
> - 健康检查接口 `GET /api/v1/health`
> - 项目规范文档（rules/ 目录下的 9 份规则）

**进行中**：Provider 模块（模型供应商 CRUD + LLM 调用封装）

**待开发**：MCP 工具管理、知识库 + RAG、Agent 管理 + 工具编排、Workflow 引擎、Chat 对话引擎、前端界面完善、Docker 部署脚本完善。

## 参考资料

项目内的详细规范文档：
- `rules/01-module-structure.md` — 模块结构与依赖规范
- `rules/02-llm-calling.md` — LLM 调用技术方案（线程池、Resilience4J、超时、重试）
- `rules/03-async-tasks.md` — 异步任务方案
- `rules/04-deployment.md` — 部署架构
- `rules/05-engineering.md` — 工程规范（SSE、缓存、日志脱敏）
- `rules/06-database-performance.md` — 数据库性能规范
- `rules/07-api-specification.md` — 接口规范
- `rules/08-java-coding.md` — Java 编码规范
- `rules/09-testing.md` — 测试规范
- `docs/database-design.md` — 核心数据表设计
- `docs/演进路径总览.md` — 产品演进规划
