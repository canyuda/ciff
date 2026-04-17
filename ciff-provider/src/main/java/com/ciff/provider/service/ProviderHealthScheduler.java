package com.ciff.provider.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ciff.common.enums.ProviderStatus;
import com.ciff.provider.entity.ProviderPO;
import com.ciff.provider.llm.LlmChatClient;
import com.ciff.provider.llm.LlmChatClientFactory;
import com.ciff.provider.mapper.ProviderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Periodically probes provider health status.
 * Runs every minute, checks all active providers concurrently via asyncExecutor.
 */
@Slf4j
@Component
public class ProviderHealthScheduler {

    private final ProviderMapper providerMapper;
    private final LlmChatClientFactory clientFactory;
    private final ProviderHealthService healthService;
    private final ThreadPoolTaskExecutor asyncExecutor;

    public ProviderHealthScheduler(ProviderMapper providerMapper,
                                   LlmChatClientFactory clientFactory,
                                   ProviderHealthService healthService,
                                   @Qualifier("asyncExecutor") ThreadPoolTaskExecutor asyncExecutor) {
        this.providerMapper = providerMapper;
        this.clientFactory = clientFactory;
        this.healthService = healthService;
        this.asyncExecutor = asyncExecutor;
    }

    @Scheduled(fixedDelayString = "${ciff.llm.health-check-interval:60000}")
    public void probe() {
        List<ProviderPO> providers = providerMapper.selectList(
                new LambdaQueryWrapper<ProviderPO>()
                        .eq(ProviderPO::getStatus, ProviderStatus.ACTIVE));

        for (ProviderPO provider : providers) {
            asyncExecutor.execute(() -> probeProvider(provider));
        }
    }

    private void probeProvider(ProviderPO provider) {
        long start = System.currentTimeMillis();
        try {
            LlmChatClient client = clientFactory.create(provider);
            client.probe();
            int latencyMs = (int) (System.currentTimeMillis() - start);
            healthService.recordSuccess(provider.getId(), latencyMs);
            log.debug("Provider {} health check success, latency: {}ms", provider.getName(), latencyMs);
        } catch (Exception e) {
            healthService.recordFailure(provider.getId(), e.getMessage());
            log.warn("Provider {} health check failed: {}", provider.getName(), e.getMessage());
        }
    }
}
