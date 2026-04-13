# Java 编码规范（基于阿里巴巴 Java 开发手册精简）

## 命名（6 条）

1. **类名 UpperCamelCase**，领域模型后缀按类型：`XxxPO`(数据库), `XxxDTO`(传输), `XxxVO`(展示), `XxxBO`(业务对象)。例外：DTO/VO/BO 等缩写全大写
2. **方法名/变量名 lowerCamelCase**。常量 UPPER_SNAKE_CASE
3. **抽象类以 Abstract/Base 开头**，异常类以 Exception 结尾，测试类以 Test 结尾，枚举类以 Enum 结尾
4. **POJO 中布尔变量不加 is 前缀**（`deleted` 而非 `isDeleted`），否则部分序列化框架会解析错误
5. **Service/DAO 层方法命名**：`get` 单查询, `list` 多查询, `count` 计数, `save` 插入, `update` 更新, `delete` 删除, `page` 分页查询
6. **禁止拼音命名、禁止缩写除通用词外**（如 `id`, `url`, `dao` 可用，`xm` 写成 `name`）。包名全小写单数形式

---

## 异常处理（5 条）

7. **不要捕获顶层 Exception/RuntimeException 后静默忽略**。catch 块至少打一行 WARN 日志并记录关键入参
8. **不要用异常控制业务流程**。能用 if/else 判断的，不要依赖 catch 来做分支
9. **finally 块中禁止 return / continue / break**，它会吞掉 try 中抛出的异常
10. **抛异常时必须保留原始异常链**：`throw new BizException("msg", e)` 不要丢掉 cause
11. **对外接口方法签名用 @throws 注释受检异常**，调用方必须处理。方法内部优先抛 RuntimeException

---

## 日志（4 条）

12. **使用 SLF4J 门面**，不直接依赖 Log4j/Logback 实现类
13. **日志拼接用 `{}` 占位符**，禁止 `+` 拼接字符串（`log.info("userId={}", id)` 而非 `log.info("userId=" + id)`）
14. **抛异常前打日志**，级别 ERROR/WARN，必须包含异常对象（`log.error("msg: {}", param, e)`），保留完整堆栈
15. **日志级别语义**：ERROR 影响业务的故障，WARN 可容忍的异常，INFO 关键业务节点，DEBUG 仅开发调试用。线上 WARN 以上

---

## 并发（5 条）

16. **线程池必须用 ThreadPoolExecutor 手动创建**，禁止 `Executors.newXxx()`（避免无界队列 OOM）。核心参数从配置文件读取
17. **SimpleDateFormat 非线程安全**，多线程下用 `DateTimeFormatter` 或 ThreadLocal 包装
18. **锁的获取顺序必须全局一致**，防止死锁。获取多把锁时按固定顺序
19. **多线程共享的可变对象必须同步**，优先用 `ConcurrentHashMap` / `AtomicXxx` / `LongAdder`，而非 `synchronized` 大锁
20. **线程资源必须通过 try-finally 或 try-with-resources 释放**，Lock 必须 `lock()` 后在 finally 中 `unlock()`
