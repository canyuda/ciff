# Phase 0 执行细则

> 目标：建立跨阶段的共享基础设施，为 Phase 1 扫清障碍。
> 原则：每个步骤都是独立可验证单元，编译通过即可推进下一步。

---

## Step 0: Entity + Mapper（数据层基础）

**顺序**: 0
**任务**: t_provider, t_model, t_provider_health 三张表的 Entity + Mapper
**产出**:
- `ProviderPO` (ciff-provider 模块)
- `ModelPO` (ciff-provider 模块)
- `ProviderHealthPO` (ciff-provider 模块)
- `ProviderMapper` (ciff-provider 模块)
- `ModelMapper` (ciff-provider 模块)
- `ProviderHealthMapper` (ciff-provider 模块)

**细节**:
- Entity 继承 `SoftDeletableEntity`（t_provider_health 无 deleted 字段，继承 `BaseEntity`）
- 使用 `@TableName` 注解映射表名
- JSON 字段（config, default_params）使用 MyBatis-Plus 的 JSON TypeHandler 或 String 存储
- Mapper 接口继承 `BaseMapper<PO>`
- 需要在 ciff-provider 模块下创建 entity/mapper 包结构

**验证方式**: `mvn compile -pl ciff-provider -am` 编译通过

---

## Step 1: ProviderType 枚举

**顺序**: 1
**任务**: 在 ciff-common 中定义 ProviderType 枚举，作为前后端唯一事实源
**产出**:
- `ProviderType` 枚举 (ciff-common 模块)

**细节**:
- 枚举值: `OPENAI`, `CLAUDE`, `GEMINI`, `OLLAMA`, `DEEPSEEK`, `ZHIPU`, `MOONSHOT`, `QWEN`, `BAICHUAN`, `MINIMAX`, `SPARK`, `DOUBAO`, `HUNYUAN`, `SENSETIME` 等当前前端已定义的类型
- 每个枚举值包含: `type`(数据库存储值), `displayName`(展示名)
- 枚举需与前端 `providerTypes` 数组保持一致

**验证方式**: `mvn compile -pl ciff-common` 编译通过

---

## Step 2: UserContext + 拦截器

**顺序**: 2
**任务**: UserContext (ThreadLocal) + X-User-Id Header 拦截器
**产出**:
- `UserContext` (ciff-common 模块) — ThreadLocal 存取 userId
- `UserIdInterceptor` (ciff-common 模块) — 从 X-User-Id Header 提取 userId
- 拦截器注册到 WebMvcConfig

**细节**:
- `UserContext` 提供 static 方法: `setUserId(Long)`, `getUserId()`, `clear()`
- 拦截器在 preHandle 中提取 Header 设入 UserContext，afterCompletion 中清理
- Header 缺失时不阻断请求（Phase 6 JWT 上线后统一处理），userId 设为 null
- 注册到现有 `WebMvcConfig`

**验证方式**: `mvn compile -pl ciff-common -am` 编译通过

---

## Step 3: UserContext 跨线程传递

**顺序**: 3
**任务**: 异步任务中 UserContext 的跨线程传递方案
**产出**:
- `UserContextTaskDecorator` (ciff-common 模块) — 线程池 TaskDecorator
- 修改 `ThreadPoolConfig` 应用 TaskDecorator
- `UserContextRunnable` 工具方法 — 用于 @Async 或手动提交任务时包装

**细节**:
- TaskDecorator 在提交任务时捕获当前线程的 userId，在新线程中恢复
- 对现有 `ThreadPoolConfig` 做最小改动
- 明确约定: 异步方法优先通过方法参数显式传递 userId，TaskDecorator 作为兜底

**验证方式**: `mvn compile -pl ciff-common -am` 编译通过

---

## Step 4: Flyway 配置验证

**顺序**: 4
**任务**: 验证 Flyway 迁移机制正常工作（V1, V2 已存在）
**产出**:
- application.yml 中 Flyway 配置确认
- V1, V2 迁移脚本在本地 MySQL 可正确执行

**细节**:
- 确认 Flyway 依赖已引入（检查 pom.xml）
- 确认 application.yml 中 flyway 配置正确（locations 指向 db/migration/mysql）
- 本地启动应用验证迁移执行

**验证方式**: 应用启动不报错，数据库中 flyway_schema_history 表有 V1, V2 记录

---

## Step 5: 编译集成验证

**顺序**: 5
**任务**: 全量编译 + 模块依赖验证
**产出**:
- 所有模块可编译通过
- ciff-app 可正常依赖 ciff-provider 和 ciff-common

**细节**:
- 确保 ciff-provider 的 pom.xml 正确依赖 ciff-common
- 确保 ciff-app 的 pom.xml 正确依赖 ciff-provider
- 各模块包结构一致: `com.ciff.{module}.entity`, `com.ciff.{module}.mapper`

**验证方式**: `mvn compile` 全量编译通过，无错误

---

## Step 6: 单元测试

**顺序**: 6
**任务**: 为 Step 0-3 产出编写单元测试
**产出**:
- `UserContextTest` — userId 设置/获取/清理
- `UserContextTaskDecoratorTest` — 跨线程 userId 传递
- `ProviderTypeTest` — 枚举值覆盖、type 字段映射

**验证方式**: `mvn test -pl ciff-common` 全部通过

---

## 依赖关系

```
Step 0 (Entity+Mapper) ──┐
Step 1 (ProviderType)  ──┤── 无前后依赖，可并行
Step 2 (UserContext)   ──┤
                         │
Step 3 (跨线程传递) ──────┘── 依赖 Step 2
Step 4 (Flyway验证) ───────── 无代码依赖
Step 5 (编译集成)   ───────── 依赖 Step 0-3 全部完成
Step 6 (单元测试)   ───────── 依赖 Step 0-3 全部完成
```

## 前置条件检查清单

- [ ] ciff-provider 模块目录已创建
- [ ] ciff-provider 的 pom.xml 配置正确（依赖 ciff-common, mybatis-plus）
- [ ] Flyway 依赖已引入 ciff-app 的 pom.xml
- [ ] application.yml 数据库连接配置正确
