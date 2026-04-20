# LLM 调用技术方案

## 线程管理：三个独立线程池

| 线程池 | 核心/最大 | 职责 | 前缀 |
|--------|----------|------|------|
| Tomcat (200) | 默认 | 接收请求、参数校验、创建 SseEmitter、立即返回 | nio- |
| LLM (10/30) | 独立池 | LLM API 调用、token 处理、写入 SseEmitter | ciff-llm- |
| 业务异步 (5/15) | 独立池 | RAG 检索、Workflow 步骤执行等非 LLM 异步任务 | ciff-biz- |

配置见 `application.yml`：`ciff.thread-pool.llm` / `ciff.thread-pool.biz`。
详细实现见源码 `ThreadPoolConfig.java`。

Tomcat 线程绝不阻塞等 LLM 响应。SseEmitter 创建后立即返回，token 处理跑在 LLM 线程池上。

## 容错：Resilience4J 每供应商独立熔断

- 每个 provider 独立 CircuitBreaker + Bulkhead 实例
- 配置见 `application.yml`：`resilience4j.circuitbreaker` / `resilience4j.bulkhead`
- 完整配置参考 `docs/rules-snippets/02-resilience4j-config.yml`
- 熔断后降级：返回提示信息或自动切换备选 provider

## 超时：四级保护

| 层级 | 默认值 | 说明 |
|------|--------|------|
| TCP 连接超时 | 5s | 建立连接阶段 |
| 首 Token 超时 | 30s | 等待 LLM 开始输出，本地模型 45s |
| Token 间隔超时 | 15s | 连续两个 token 之间超时认为连接僵死，本地模型 20s |
| SSE 整体超时 | 180s | SseEmitter 硬上限，本地模型 300s |

per-provider 可配置，ollama 等本地模型放宽阈值。

## 重试：首 Token 前才重试

- 指数退避：初始 1s，最大 5s，jitter 0.5
- 可重试错误：429 / 500 / 502 / 503 / TimeoutException / ConnectException
- 不可重试错误：400 / 401 / 403
- **核心规则**：首 token 到达后不再重试（已发给用户的 token 无法撤回）

## 完整调用链路

```
用户请求
  │ [Tomcat 线程] 创建 SseEmitter(180s)，立即返回
  │
  ▼ [LLM 线程池]
ChatFacade.streamChat()
  ├── 加载 Agent 配置 (Redis 缓存)
  ├── 构建 Prompt
  └── LlmInvoker.streamChatWithRetry(provider, request)
        ├── Bulkhead 检查并发限额
        ├── CircuitBreaker 熔断检查
        ├── 首次调用 actualLlmCall() (WebClient + Netty)
        ├── 首 token 超时 30s
        ├── 逐 token publishOn → LLM 线程池 → 写入 SseEmitter
        ├── token 间隔超时 15s
        ├── 失败 → 可重试 + 首 token 前才重试 → 指数退避
        ├── 熔断中 → 降级提示 / 切备选 provider
        └── 整体超时 180s 兜底
```
