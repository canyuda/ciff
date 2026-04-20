# 工程、编码与接口规范

## 通用工程规范

- SSE 接口必须用异步模式，不能一个长连接占一个 Tomcat 线程
- 优先函数式编程范式
- 所有外部调用必须有超时设置
- 配置项外化到 application.yml，不硬编码

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
- 使用 SLF4J 门面，日志拼接用 `{}` 占位符，禁止 `+` 拼接
- 抛异常前打日志，级别 ERROR/WARN，必须包含异常对象
- 日志级别：ERROR 影响业务的故障，WARN 可容忍的异常，INFO 关键业务节点，DEBUG 仅开发调试用

---

## 接口规范

- 用 Swagger 生成接口文档，导出接口 `/v3/api-docs`
- RESTful 风格：`/api/v1/{资源复数名}`
- 所有创建和更新接口必须校验入参，使用 `@Valid` + JSR-303 注解
- 所有接口返回 `Result<T>`：`{ code: 200, message: "success", data: {...} }`
- 分页：page（从 1 开始）、pageSize（默认 20，最大 100）
- 空值：列表空返回 `[]`，字符串空返回 `""`，对象不存在返回 `null`
- 错误码四位数字，按模块分段：1000-1999 通用、2000-2999 Provider、3000-3999 Agent、4000-4999 Chat、5000-5999 MCP、6000-6999 Workflow、7000-7999 Knowledge

---

## Java 编码规范

### 命名

1. **类名 UpperCamelCase**，领域模型后缀：`XxxPO`(数据库), `XxxDTO`(传输), `XxxVO`(展示), `XxxBO`(业务对象)
2. **方法名/变量名 lowerCamelCase**。常量 UPPER_SNAKE_CASE
3. **抽象类以 Abstract/Base 开头**，异常类以 Exception 结尾，测试类以 Test 结尾，枚举类以 Enum 结尾
4. **POJO 中布尔变量不加 is 前缀**（`deleted` 而非 `isDeleted`）
5. **Service/DAO 层方法命名**：`get` 单查询, `list` 多查询, `count` 计数, `save` 插入, `update` 更新, `delete` 删除, `page` 分页查询
6. **禁止拼音命名、禁止缩写除通用词外**（如 `id`, `url`, `dao` 可用，`xm` 写成 `name`）。包名全小写单数形式

### 异常处理

7. **不要捕获顶层 Exception/RuntimeException 后静默忽略**。catch 块至少打一行 WARN 日志
8. **不要用异常控制业务流程**。能用 if/else 判断的，不要依赖 catch 做分支
9. **finally 块中禁止 return / continue / break**
10. **抛异常时必须保留原始异常链**：`throw new BizException("msg", e)`
11. **对外接口方法签名用 @throws 注释受检异常**，方法内部优先抛 RuntimeException

### 并发

12. **线程池必须用 ThreadPoolExecutor 手动创建**，禁止 `Executors.newXxx()`，核心参数从配置文件读取
13. **SimpleDateFormat 非线程安全**，多线程下用 `DateTimeFormatter` 或 ThreadLocal
14. **锁的获取顺序必须全局一致**，防止死锁
15. **多线程共享的可变对象必须同步**，优先用 `ConcurrentHashMap` / `AtomicXxx` / `LongAdder`
16. **线程资源必须通过 try-finally 或 try-with-resources 释放**

---

## 测试规范

- 框架：JUnit 5 + Spring Boot Test + MockMvc + Mockito + AssertJ
- Controller：@WebMvcTest 切片测试，只测参数校验和响应格式
- Service/Facade：纯单元测试，Mock 依赖，禁止 @Autowired
- Mapper：集成测试，连接真实数据库
- Util/Convertor：纯单元测试，无 Spring 依赖
- 命名：测试类 `{Target}Test`，测试方法 `{method}_{scenario}_{expected}`
- 每个测试方法只测一个场景，用 Given-When-Then 结构
- 外部依赖（LLM、Redis、数据库）在单元测试中必须 Mock
- **禁止**：不写无意义测试（getter/setter）、不测框架本身、不在单元测试中启动完整 Spring Context
- **自动生成**：每次新建或修改 Controller / Service 后，必须主动生成对应的单元测试
