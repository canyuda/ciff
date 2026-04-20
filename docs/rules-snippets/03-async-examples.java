// @Async 示例
@Service
@RequiredArgsConstructor
public class AgentFacadeImpl implements AgentFacade {
    @Async("bizExecutor")
    @Override
    public void refreshAgentCache(Long agentId) { /* ... */ }
}

// Redis Stream 消费者示例
@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentProcessConsumer {
    @Scheduled(fixedDelay = 2000)
    public void consume() {
        Consumer consumer = Consumer.from("cg-doc-process", "ciff-server-1");
        StreamOffset<String> offset = StreamOffset.create("stream:doc_process", ReadOffset.lastConsumed());
        List<MapRecord<String, Object, Object>> records =
            redisTemplate.opsForStream().read(consumer, StreamReadOptions.empty().count(5), offset);
        // ... ACK 处理
    }
}

// Stream 初始化
@Configuration
public class StreamConfig {
    @Bean
    public CommandLineRunner initStream(StringRedisTemplate redisTemplate) {
        return args -> {
            try {
                redisTemplate.opsForStream().createGroup("stream:doc_process", "cg-doc-process");
            } catch (Exception ignored) {}
        };
    }
}
