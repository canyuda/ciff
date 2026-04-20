# 异步任务方案

## 分层策略

| 任务类型 | 实现方式 | 丢了？ |
|----------|---------|--------|
| 缓存刷新 | @Async (bizExecutor) | 无所谓，下次请求回源 |
| 对话记录写入 | @Async (bizExecutor) | 影响小，可通过日志补 |
| 文档处理（分块 → embedding → 存向量） | Redis Stream | 不接受，必须可重试 |
| 工作流步骤 | 同步执行（LLM 线程池内） | 不异步 |

## @Async 用法（轻量任务）

使用已定义的 `bizExecutor` 线程池。示例见 `docs/rules-snippets/03-async-examples.java`。

## Redis Stream 用法（文档处理等可靠任务）

- 生产者：上传文档时发送任务到 `stream:doc_process`
- 消费者：`@Scheduled(fixedDelay = 2000)` 消费，ACK 确认，失败不 ACK 自动重试
- Stream 初始化：应用启动时创建消费组
- 完整示例见 `docs/rules-snippets/03-async-examples.java`

不使用 RabbitMQ / Kafka 的理由：50 人单机场景，Redis Stream 已满足持久化、ACK、消费组、Pending 重试需求。
