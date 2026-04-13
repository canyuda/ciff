# 测试规范

## 测试框架

- JUnit 5 + Spring Boot Test + MockMvc
- Mock: Mockito（spring-boot-starter-test 内置）
- 断言: AssertJ（spring-boot-starter-test 内置）

## 测试分层

| 层 | 测试方式 | 注解/工具 |
|---|---------|----------|
| Controller | @WebMvcTest 切片测试 | MockMvc |
| Service/Facade | 纯单元测试，Mock 依赖 | @ExtendWith(MockitoExtension.class) |
| Mapper | 集成测试，连接真实数据库 | @MybatisPlusTest |
| Util/Convertor | 纯单元测试，无 Spring 依赖 | 纯 JUnit 5 |

## 命名约定

| 类别 | 格式 | 示例 |
|------|------|------|
| 测试类 | {Target}Test | AgentServiceTest |
| 测试方法 | {method}_{scenario}_{expected} | createAgent_whenNameExists_throwException |

## 编写规范

1. 测试类与被测类同包，放在 `src/test/java` 下
2. 每个测试方法只测一个场景
3. Mock 注入用 `@Mock` + `@InjectMocks`，单元测试中禁止 `@Autowired`
4. 外部依赖（LLM 调用、Redis、数据库）在单元测试中必须 Mock，集成测试除外
5. `@WebMvcTest` 只测参数校验和响应格式，不测业务逻辑
6. 测试方法结构: Given-When-Then，用注释分段或空行分隔
7. 测试代码同样遵守编码规范（命名、异常处理等）

## 禁止事项

- 不为了覆盖率写无意义测试（如只测 getter/setter）
- 不测框架本身功能（如 Spring 注入是否生效）
- 不在单元测试中启动完整 Spring Context
- 不要求 100% 覆盖率，核心业务逻辑优先
