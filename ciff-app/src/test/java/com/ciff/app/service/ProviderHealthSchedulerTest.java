package com.ciff.app.service;

import com.ciff.common.enums.ProviderStatus;
import com.ciff.common.enums.ProviderType;
import com.ciff.provider.entity.ProviderPO;
import com.ciff.provider.llm.LlmChatClient;
import com.ciff.provider.llm.LlmChatClientFactory;
import com.ciff.provider.mapper.ProviderMapper;
import com.ciff.provider.service.ProviderHealthScheduler;
import com.ciff.provider.service.ProviderHealthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProviderHealthSchedulerTest {

    @Mock
    private ProviderMapper providerMapper;

    @Mock
    private LlmChatClientFactory clientFactory;

    @Mock
    private ProviderHealthService healthService;

    @Test
    void probe_shouldSubmitTaskForEachActiveProvider() {
        ThreadPoolTaskExecutor executor = mock(ThreadPoolTaskExecutor.class);
        ProviderHealthScheduler scheduler = new ProviderHealthScheduler(
                providerMapper, clientFactory, healthService, executor);

        ProviderPO p1 = buildProvider(1L, "OpenAI");
        ProviderPO p2 = buildProvider(2L, "Claude");
        given(providerMapper.selectList(any())).willReturn(List.of(p1, p2));

        scheduler.probe();

        verify(executor, times(2)).execute(any(Runnable.class));
    }

    @Test
    void probe_whenClientSuccess_shouldRecordSuccess() throws Exception {
        ThreadPoolTaskExecutor executor = mock(ThreadPoolTaskExecutor.class);
        // make execute synchronous so we can verify healthService calls inline
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(executor).execute(any(Runnable.class));

        ProviderHealthScheduler scheduler = new ProviderHealthScheduler(
                providerMapper, clientFactory, healthService, executor);

        ProviderPO provider = buildProvider(1L, "OpenAI");
        given(providerMapper.selectList(any())).willReturn(List.of(provider));

        LlmChatClient client = mock(LlmChatClient.class);
        given(clientFactory.create(provider)).willReturn(client);

        scheduler.probe();

        verify(healthService).recordSuccess(argThat(id -> id.equals(1L)), any(Integer.class));
        verify(healthService, never()).recordFailure(any(), any());
    }

    @Test
    void probe_whenClientThrows_shouldRecordFailure() throws Exception {
        ThreadPoolTaskExecutor executor = mock(ThreadPoolTaskExecutor.class);
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(executor).execute(any(Runnable.class));

        ProviderHealthScheduler scheduler = new ProviderHealthScheduler(
                providerMapper, clientFactory, healthService, executor);

        ProviderPO provider = buildProvider(1L, "OpenAI");
        given(providerMapper.selectList(any())).willReturn(List.of(provider));

        LlmChatClient client = mock(LlmChatClient.class);
        given(clientFactory.create(provider)).willReturn(client);
        client.probe();
        org.mockito.Mockito.doThrow(new RuntimeException("connection refused")).when(client).probe();

        scheduler.probe();

        verify(healthService).recordFailure(1L, "connection refused");
        verify(healthService, never()).recordSuccess(any(), any(Integer.class));
    }

    @Test
    void probe_whenNoActiveProviders_shouldDoNothing() {
        ThreadPoolTaskExecutor executor = mock(ThreadPoolTaskExecutor.class);
        ProviderHealthScheduler scheduler = new ProviderHealthScheduler(
                providerMapper, clientFactory, healthService, executor);

        given(providerMapper.selectList(any())).willReturn(List.of());

        scheduler.probe();

        verify(executor, never()).execute(any());
        verify(healthService, never()).recordSuccess(any(), any(Integer.class));
        verify(healthService, never()).recordFailure(any(), any());
    }

    private ProviderPO buildProvider(Long id, String name) {
        ProviderPO po = new ProviderPO();
        po.setId(id);
        po.setName(name);
        po.setType(ProviderType.OPENAI);
        po.setStatus(ProviderStatus.ACTIVE);
        return po;
    }
}
