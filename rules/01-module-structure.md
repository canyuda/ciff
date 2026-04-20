# 模块结构与依赖规范

## 项目结构

```
ciff/
├── ciff-app/               # Spring Boot 启动模块
├── ciff-chat/              # 对话引擎（顶层编排）
├── ciff-workflow/          # 工作流编排与执行
├── ciff-agent/             # Agent 管理与配置
├── ciff-knowledge/         # 知识库与 RAG
├── ciff-mcp/               # MCP 工具管理与调用
├── ciff-provider/          # 模型提供商管理
├── ciff-common/            # 公共模块（工具类、常量、异常、DTO）
├── ciff-web/               # Vue 前端
├── deploy/                 # Docker Compose 配置
│   └── nginx/              # Nginx 配置（nginx.conf、SSL 证书）
└── docs/                   # 架构文档、数据库设计、演进路径
```

## 模块依赖关系

```
Layer 0: ciff-common
Layer 1: ciff-provider, ciff-mcp            （不依赖业务模块）
Layer 2: ciff-knowledge                      （依赖 provider）
Layer 3: ciff-agent                          （依赖 mcp + knowledge）
Layer 4: ciff-workflow                       （依赖 agent + knowledge + provider）
Layer 5: ciff-chat                           （依赖所有业务模块）
Layer 6: ciff-app                            （组装入口）
```

| 模块 | 直接依赖 |
|------|---------|
| ciff-common | 无 |
| ciff-provider | common |
| ciff-knowledge | common, provider |
| ciff-mcp | common |
| ciff-agent | common, mcp, knowledge |
| ciff-workflow | common, provider, agent, knowledge |
| ciff-chat | common, provider, agent, knowledge, mcp, workflow |
| ciff-app | 以上全部 |

严格单向依赖，禁止反向引用。Maven pom 声明即编译时强制。

## 模块内部结构

```
com.ciff.{module}
├── facade/              # 对外接口（跨模块调用的唯一入口）
├── controller/          # REST 接口
├── service/             # 业务逻辑
├── mapper/              # MyBatis-Plus Mapper
├── entity/              # 数据库表映射（PO）
├── dto/                 # 数据传输对象（DTO / Request / VO）
├── convertor/           # PO ↔ DTO / VO 转换
├── constant/            # 模块级常量
├── exception/           # 模块内自定义异常
└── config/              # 模块级 Spring 配置
```

## 各层职责

| 层 | 只做 | 禁止 |
|---|---|---|
| facade | 定义 interface，方法参数和返回值只用 DTO | 写实现逻辑、出现 entity 类型、注入 Spring Bean |
| controller | 参数校验（@Valid）、调 facade/service、包装 Result<T> 返回 | 写业务逻辑、直接调 mapper、调其他模块的 service |
| service | 业务逻辑、实现 facade 接口、调 mapper 和其他模块 facade | 被其他模块直接 import、返回 PO 给外部 |
| mapper | 继承 BaseMapper<T>，复杂查询写 XML | 写业务逻辑 |
| entity | 表字段映射（@TableName @TableId） | 离开本模块范围 |
| dto | 纯数据载体，request 加 JSR-303 校验注解 | 包含业务逻辑、出现 MyBatis 注解 |
| convertor | PO ↔ DTO / VO 互转，纯静态方法 | 调 service / mapper |
| constant | public static final 常量，私有构造函数 | 魔法值散落在代码中 |
| exception | 继承 BizException，语义化异常类 | 继承非 BizException 的异常 |
| config | 模块级 @Configuration | 包含业务逻辑 |

## 跨模块调用规则

1. **只能通过 Facade 接口调用** — 禁止直接注入其他模块的 Service / Mapper
2. **Facade 方法只传 DTO** — 参数和返回值禁止出现 PO 类型
3. **构造器注入** — 用 @RequiredArgsConstructor，禁止 @Autowired 字段注入
4. **反向依赖 = 编译失败** — Maven pom 不声明反向依赖，编译直接报错

## 命名约定

| 类别 | 格式 | 示例 |
|------|------|------|
| Facade 接口 | {Module}Facade | AgentFacade |
| Facade 实现 | {Module}FacadeImpl | AgentFacadeImpl |
| 模块内 Service | {Entity}Service | AgentToolService |
| Controller | {Module}Controller | AgentController |
| Mapper | {Entity}Mapper | AgentMapper |
| 数据库实体 | {Entity}PO | AgentPO |
| Facade 传参 | {Entity}DTO | AgentDTO |
| 请求入参 | {Entity}{Action}Request | AgentCreateRequest |
| 响应出参 | {Entity}VO | AgentVO |
| 转换器 | {Entity}Convertor | AgentConvertor |
| 常量类 | {Module}Constants | AgentConstants |
| 异常类 | {Entity}{Status}Exception | AgentNotFoundException |
| 数据库表名 | t_{module}_{entity} | t_agent |
| 数据库字段 | snake_case | model_name |

## 统一响应与异常

- 统一响应：`Result<T>`（code / message / data），见源码 `ciff-common`
- 基础异常：`BizException`（code + message）
- 模块内异常继承 BizException，code 用 4 位业务错误码
- 全局异常捕获：`@RestControllerAdvice`
