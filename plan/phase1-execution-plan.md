# Phase 1 执行细则

> 目标：前端能配置模型供应商和模型，后端能调用 LLM。
> 涉及模块：ciff-provider / ciff-common（补全） / ciff-app
> 前端（1.4-1.6）暂不在本细则范围，后端完成后单独拆分。

---

## Step 0: AES 加解密工具类

**顺序**: 0
**任务**: 在 ciff-common 中实现 AES 加解密工具，用于 Provider API Key 的加密存储和解密调用
**产出**:
- `ApiKeyEncryptor` (ciff-common 模块)
- `ApiKeyEncryptorTest` (ciff-common 测试)

**细节**:
- AES/CBC/PKCS5Padding 模式
- 密钥从 application.yml 读取 (`ciff.security.api-key-secret`)
- 加密结果以 Base64 存储
- 空值处理: encrypt(null) → 空字符串, decrypt("") → 空字符串
- 提供 `rotateKey(String newSecret)` 方法预留密钥轮换能力

**验证方式**: `mvn test -pl ciff-common` 中 ApiKeyEncryptorTest 全部通过

---

## Step 1: Provider DTO + Convertor

**顺序**: 1
**任务**: Provider 的请求/响应 DTO 和对象转换器
**产出**:
- `ProviderCreateRequest` — name, type(ProviderType), authType(AuthType), apiBaseUrl, apiKey(明文,不入库), authConfig(nullable)
- `ProviderUpdateRequest` — 所有字段可选更新
- `ProviderVO` — 返回给前端，apiKey 显示为掩码
- `ProviderConvertor` — Request↔PO, PO→VO 转换

**细节**:
- `ProviderCreateRequest.apiKey`: 明文传入，Service 层加密后存入 `apiKeyEncrypted`
- `ProviderVO.apiKey`: 显示 `key_prefix***` 格式掩码
- `ProviderVO.type` 返回枚举 type 字符串 (如 "openai")
- 校验: name 不为空, type 为合法枚举, apiBaseUrl 合法 URL 格式

**验证方式**: `mvn compile -pl ciff-provider -am` 编译通过

---

## Step 2: Provider Service + Controller

**顺序**: 2
**任务**: Provider 完整 CRUD 业务层 + 接口层
**产出**:
- `ProviderService` 接口 + `ProviderServiceImpl`
- `ProviderController` — `/api/v1/providers`

**细节**:
- **创建**: 校验 name 唯一 → apiKey 加密 → 入库
- **更新**: 校验存在 → 部分更新(apiKey 如传入则重新加密) → 更新
- **删除**: 逻辑删除，检查是否有关联 Model，有则拒绝删除
- **分页查询**: 支持按 status 筛选，按 createTime 倒序
- **详情**: 按 ID 查询
- Service 注入 ApiKeyEncryptor, ProviderMapper
- Controller 遵循 DemoItemController 风格，使用 Swagger 注解

**验证方式**: `mvn compile -pl ciff-provider -am` 编译通过

---

## Step 3: Provider Controller 单元测试

**顺序**: 3
**任务**: Provider CRUD 切片测试
**产出**:
- `ProviderControllerTest` — @WebMvcTest 切片测试

**细节**:
- 覆盖: 创建/查询/更新/删除/分页/参数校验
- Mock ProviderService
- 校验 type 枚举非法值返回 400
- 校验 URL 格式非法值返回 400

**验证方式**: `mvn test -pl ciff-provider -Dtest=ProviderControllerTest` 通过

---

## Step 4: Model DTO + Convertor + Service + Controller

**顺序**: 4
**任务**: Model 完整 CRUD（DTO + Convertor + Service + Controller 一步到位）
**产出**:
- `ModelCreateRequest`, `ModelUpdateRequest`, `ModelVO`
- `ModelConvertor`
- `ModelService` 接口 + `ModelServiceImpl`
- `ModelController` — `/api/v1/models`

**细节**:
- **创建**: 校验 providerId 存在 → 入库
- **更新**: 校验存在 → 部分更新
- **删除**: 逻辑删除
- **分页查询**: 支持按 providerId 和 status 筛选
- **列表查询**: 按 providerId 查询该供应商下的模型列表
- ModelVO 中包含 providerName（关联查询）
- defaultParams 使用 JacksonTypeHandler (同 ProviderPO.authConfig)

**验证方式**: `mvn compile -pl ciff-provider -am` 编译通过

---

## Step 5: Model 单元测试

**顺序**: 5
**任务**: Model Service + Controller 测试
**产出**:
- `ModelControllerTest` — @WebMvcTest 切片测试
- `ModelServiceTest` — Service 单元测试

**细节**:
- Controller: CRUD / providerId 校验 / 分页
- Service: providerId 不存在时校验 / defaultParams JSON 解析 / 逻辑删除

**验证方式**: `mvn test -pl ciff-provider -Dtest=ModelControllerTest,ModelServiceTest` 通过

---

## Step 6: Provider Mapper 集成测试

**顺序**: 6
**任务**: Provider + Model Mapper 层集成测试（连真实数据库）
**产出**:
- `ProviderMapperTest` — 数据库 CRUD + 逻辑删除 + status 筛选
- `ModelMapperTest` — 数据库 CRUD + providerId 筛选

**验证方式**: `mvn test -pl ciff-provider -Dtest=ProviderMapperTest,ModelMapperTest` 通过

---

## Step 7: LlmHttpClient 四级超时改造

**顺序**: 7
**任务**: 在现有 TCP 连接超时 + 读取超时基础上，增加首 Token 超时和 Token 间隔超时
**产出**:
- 改造后 `LlmHttpClient`
- `LlmHttpClientTest`

**细节**:
- 现有: TCP 连接超时(5s) + 读取超时(60s/120s) — 保持不变
- 新增: 首 Token 超时(30s) — 发出请求后首条 SSE 数据在 30s 内未到则超时
- 新增: Token 间隔超时(15s) — 两个 SSE event 间隔超 15s 则超时
- SSE 流式场景: 在 stream() 方法中实现首 Token 和 Token 间隔超时检测
- 超时抛出 `LlmApiException.timeout()`
- 超时值外化到 application.yml (`ciff.llm.timeout.*`)

**验证方式**: `mvn test -pl ciff-common -Dtest=LlmHttpClientTest` 通过

---

## Step 8: Resilience4j 集成 LlmHttpClient

**顺序**: 8
**任务**: 将现有 CircuitBreakerService 接入 LlmHttpClient，per-provider 熔断
**产出**:
- 改造后 `LlmHttpClient` (post/stream 方法接入熔断)
- 改造后 `CircuitBreakerService` (如需调整)

**细节**:
- LlmHttpClient 新增 `CircuitBreakerService` 依赖注入
- post/stream 方法接收 providerName 参数，用 `circuitBreakerService.execute()` 包装
- 调用方需传入 providerName 而非 url，内部根据 provider 配置拼装 url
- 现有 CircuitBreakerService 的重试逻辑仅在首 Token 前触发（SSE 流式开始后不重试）

**验证方式**: `mvn compile -pl ciff-common -am` 编译通过

---

## Step 9: ChatClient / StreamClient 统一封装

**顺序**: 9
**任务**: 按 Provider 类型适配请求格式，统一入参出参
**产出**:
- `LlmChatRequest` — 统一入参 (model, messages, temperature, maxTokens, tools)
- `LlmChatResponse` — 统一出参 (content, finishReason, toolCalls, usage)
- `LlmChatClient` 接口 + Provider 类型实现
  - `OpenAiCompatibleClient` (覆盖: OpenAI, DeepSeek, Qwen, Ollama 等兼容接口)
  - `ClaudeClient` (Claude Messages API)

**细节**:
- 按计划中 1.3c 要求，屏蔽各厂商请求/响应格式差异
- 入参: 消息列表、模型参数、工具定义(Phase 2 用)
- 出参: 统一为 LlmChatResponse，包含 content/toolCalls/usage
- Provider 的 apiBaseUrl + 路径拼装在此层完成
- 请求头构建: 根据 AuthType 决定 (Bearer / x-api-key / custom header)

**验证方式**: `mvn compile -pl ciff-provider -am` 编译通过

---

## Step 10: Provider 健康检查

**顺序**: 10
**任务**: 定时探测 + 每次调用 LLM 更新健康状态
**产出**:
- `ProviderHealthService` — 记录成功/失败/延迟
- `ProviderHealthScheduler` — @Scheduled 定时探测 (每分钟)
- `ProviderHealthVO` — 健康状态 VO

**细节**:
- 成功时: 重置 consecutiveFailures, 更新 lastSuccessTime, lastLatencyMs
- 失败时: 累加 consecutiveFailures, 更新 lastFailureTime, lastFailureReason
- 状态判定: consecutiveFailures == 0 → healthy, ≥ 3 → unhealthy, 其余 → unknown
- 定时探测: 使用轻量级 LLM 调用(如发送简单 prompt)
- 查询接口: `/api/v1/providers/{id}/health`

**验证方式**: `mvn compile -pl ciff-provider -am` 编译通过

---

## Step 11: Provider 健康检查单元测试

**顺序**: 11
**任务**: ProviderHealthService 单元测试
**产出**:
- `ProviderHealthServiceTest`

**细节**:
- 成功时重置连续失败计数
- 失败时累加计数
- 延迟记录正确
- 状态判定: healthy / unhealthy / unknown
- 首次调用(无历史记录)返回 unknown

**验证方式**: `mvn test -pl ciff-provider -Dtest=ProviderHealthServiceTest` 通过

---

## Step 12: 全量编译 + 测试集成

**顺序**: 12
**任务**: 全量编译 + 全部测试通过
**产出**: 所有模块编译和测试通过

**验证方式**:
- `mvn compile` 全量编译通过
- `mvn test -pl ciff-provider,ciff-common` 全部通过
- 无 `@Disabled` 测试

---

## 依赖关系

```
Step 0 (AES工具) ─────────────────┐
                                   │
Step 1 (Provider DTO/Convertor) ───┤
                                   ├──► Step 2 (Provider Service/Controller) ──► Step 3 (Provider测试)
                                   │
                                   ├──► Step 6 (Mapper集成测试) ──┐
                                   │                              │
Step 4 (Model全套) ────────────────┤                              │
           │                       │                              │
           └──► Step 5 (Model测试) │                              │
                                   │                              │
Step 7 (四级超时) ──► Step 8 (熔断集成) ──► Step 9 (ChatClient封装) │
                                                          │       │
                                                          └──► Step 10 (健康检查) ──► Step 11 (健康检查测试)
                                                                   │
Step 12 (全量验证) ◄────────────────────────────────────────────────┘
```

## 并行机会

- Step 0/1/4 可并行
- Step 7/8/9 和 Step 0-6 可并行（不依赖 Provider CRUD）
- Step 10/11 依赖 Step 9
