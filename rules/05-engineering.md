# 工程规范

## 通用规范

- SSE 接口必须用异步模式，不能一个长连接占一个 Tomcat 线程
- 优先函数式编程范式

## 缓存规范

| 缓存对象 | Key 格式 | TTL | 策略 |
|----------|---------|-----|------|
| Agent 配置 | `agent:{id}` | 30min | 更新时主动失效 |
| Workflow 定义 | `workflow:{id}` | 30min | 更新时主动失效 |
| 模型供应商配置 | `model:provider:{id}` | 1h | 更新时主动失效 |
| 用户会话 | `session:{token}` | 跟随 JWT | 过期自动清除 |

不缓存：对话记录（写多读少）、LLM 响应（缓存命中低）

## 日志规范

- LLM 请求/响应不完整打印（太长 + 可能含敏感信息）
- 只记录：agentId、model、token 用量、耗时、状态码
- Docker 日志：`json-file` driver，`max-size: 10m`，`max-file: 3`
