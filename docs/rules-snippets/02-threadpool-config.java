// ThreadPoolConfig.java — 详细实现参考源码
@Configuration
@ConfigurationProperties(prefix = "ciff.thread-pool")
@Data
public class ThreadPoolConfig {
    private PoolConfig llm = new PoolConfig();
    private PoolConfig biz = new PoolConfig();

    @Data
    public static class PoolConfig {
        private int coreSize;
        private int maxSize;
        private int queueCapacity;
    }

    @Bean("llmExecutor")
    public ThreadPoolTaskExecutor llmExecutor() {
        return createExecutor(llm, "ciff-llm-");
    }

    @Bean("bizExecutor")
    public ThreadPoolTaskExecutor bizExecutor() {
        return createExecutor(biz, "ciff-biz-");
    }

    private ThreadPoolTaskExecutor createExecutor(PoolConfig config, String prefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(config.getCoreSize());
        executor.setMaxPoolSize(config.getMaxSize());
        executor.setQueueCapacity(config.getQueueCapacity());
        executor.setThreadNamePrefix(prefix);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
