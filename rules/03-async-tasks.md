# 异步任务方案

## 分层策略

| 任务类型 | 实现方式 | 丢了？ |
|----------|---------|--------|
| 缓存刷新 | @Async (bizExecutor) | 无所谓，下次请求回源 |
| 对话记录写入 | @Async (bizExecutor) | 影响小，可通过日志补 |
| 文档处理（分块 → embedding → 存向量） | Redis Stream | 不接受，必须可重试 |
| 工作流步骤 | 同步执行（LLM 线程池内） | 不异步 |

## @Async 用法（轻量任务）

```java
// 使用已定义的 bizExecutor 线程池
@Service
@RequiredArgsConstructor
public class AgentFacadeImpl implements AgentFacade {

    private final AgentMapper agentMapper;
    private final StringRedisTemplate redisTemplate;

    @Async("bizExecutor")
    @Override
    public void refreshAgentCache(Long agentId) {
        AgentPO po = agentMapper.selectById(agentId);
        redisTemplate.opsForValue().set(
            "agent:" + agentId,
            JSON.toJSONString(AgentConvertor.toDTO(po)),
            30, TimeUnit.MINUTES
        );
    }
}
```

## Redis Stream 用法（文档处理等可靠任务）

生产者（上传文档时发送任务）：

```java
@Service
@RequiredArgsConstructor
public class DocumentFacadeImpl implements DocumentFacade {

    private final StringRedisTemplate redisTemplate;

    @Override
    public DocumentDTO uploadDocument(Long knowledgeId, MultipartFile file) {
        DocumentPO doc = saveMetadata(knowledgeId, file);
        Map<String, String> message = Map.of(
            "documentId", doc.getId().toString(),
            "knowledgeId", knowledgeId.toString(),
            "filePath", doc.getFilePath()
        );
        redisTemplate.opsForStream().add("stream:doc_process", message);
        return DocumentConvertor.toDTO(doc);
    }
}
```

消费者（异步处理文档）：

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentProcessConsumer {

    private final StringRedisTemplate redisTemplate;
    private final DocumentService documentService;

    @Scheduled(fixedDelay = 2000)
    public void consume() {
        Consumer consumer = Consumer.from("cg-doc-process", "ciff-server-1");
        StreamOffset<String> offset = StreamOffset.create("stream:doc_process", ReadOffset.lastConsumed());
        List<MapRecord<String, Object, Object>> records =
            redisTemplate.opsForStream().read(consumer, StreamReadOptions.empty().count(5), offset);

        if (records == null || records.isEmpty()) return;

        for (MapRecord<String, Object, Object> record : records) {
            try {
                Long docId = Long.valueOf(record.getValue().get("documentId").toString());
                documentService.processDocument(docId);
                redisTemplate.opsForStream().acknowledge("stream:doc_process", "cg-doc-process", record.getId());
            } catch (Exception e) {
                log.error("Document process failed: {}", record.getId(), e);
                // no ACK → stays in pending, will be retried
            }
        }
    }
}
```

Stream 初始化（应用启动时）：

```java
@Configuration
public class StreamConfig {
    @Bean
    public CommandLineRunner initStream(StringRedisTemplate redisTemplate) {
        return args -> {
            try {
                redisTemplate.opsForStream().createGroup("stream:doc_process", "cg-doc-process");
            } catch (Exception ignored) { /* group already exists */ }
        };
    }
}
```

不使用 RabbitMQ / Kafka 的理由：50 人单机场景，Redis Stream 已满足持久化、ACK、消费组、Pending 重试需求，无需额外基础设施。
