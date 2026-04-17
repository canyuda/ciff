package com.ciff.app.service;

import com.ciff.common.enums.HealthStatus;
import com.ciff.common.enums.ProviderStatus;
import com.ciff.provider.dto.ProviderHealthVO;
import com.ciff.provider.entity.ProviderHealthPO;
import com.ciff.provider.entity.ProviderPO;
import com.ciff.provider.mapper.ProviderHealthMapper;
import com.ciff.provider.mapper.ProviderMapper;
import com.ciff.provider.service.ProviderHealthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProviderHealthServiceTest {

    @Mock
    private ProviderHealthMapper healthMapper;

    @Mock
    private ProviderMapper providerMapper;

    @InjectMocks
    private ProviderHealthService healthService;

    @Test
    void recordSuccess_firstTime_shouldInsert() {
        given(healthMapper.selectOne(any())).willReturn(null);
        given(healthMapper.insert(any(ProviderHealthPO.class))).willReturn(1);

        healthService.recordSuccess(1L, 120);

        ArgumentCaptor<ProviderHealthPO> captor = ArgumentCaptor.forClass(ProviderHealthPO.class);
        verify(healthMapper).insert(captor.capture());
        ProviderHealthPO h = captor.getValue();
        assertThat(h.getProviderId()).isEqualTo(1L);
        assertThat(h.getStatus()).isEqualTo(HealthStatus.UP);
        assertThat(h.getConsecutiveFailures()).isZero();
        assertThat(h.getLastLatencyMs()).isEqualTo(120);
        assertThat(h.getLastSuccessTime()).isNotNull();
    }

    @Test
    void recordSuccess_afterFailures_shouldReset() {
        ProviderHealthPO existing = new ProviderHealthPO();
        existing.setId(10L);
        existing.setProviderId(1L);
        existing.setConsecutiveFailures(2);
        existing.setStatus(HealthStatus.UNKNOWN);
        given(healthMapper.selectOne(any())).willReturn(existing);

        healthService.recordSuccess(1L, 80);

        verify(healthMapper, never()).insert(any(ProviderHealthPO.class));
        ArgumentCaptor<ProviderHealthPO> captor = ArgumentCaptor.forClass(ProviderHealthPO.class);
        verify(healthMapper).updateById(captor.capture());
        ProviderHealthPO h = captor.getValue();
        assertThat(h.getId()).isEqualTo(10L);
        assertThat(h.getStatus()).isEqualTo(HealthStatus.UP);
        assertThat(h.getConsecutiveFailures()).isZero();
        assertThat(h.getLastLatencyMs()).isEqualTo(80);
    }

    @Test
    void recordFailure_once_shouldBeUnknown() {
        given(healthMapper.selectOne(any())).willReturn(null);
        given(healthMapper.insert(any(ProviderHealthPO.class))).willReturn(1);

        healthService.recordFailure(1L, "timeout");

        ArgumentCaptor<ProviderHealthPO> captor = ArgumentCaptor.forClass(ProviderHealthPO.class);
        verify(healthMapper).insert(captor.capture());
        ProviderHealthPO h = captor.getValue();
        assertThat(h.getProviderId()).isEqualTo(1L);
        assertThat(h.getStatus()).isEqualTo(HealthStatus.UNKNOWN);
        assertThat(h.getConsecutiveFailures()).isEqualTo(1);
        assertThat(h.getLastFailureReason()).isEqualTo("timeout");
    }

    @Test
    void recordFailure_threeTimes_shouldBeDown() {
        ProviderHealthPO existing = new ProviderHealthPO();
        existing.setId(10L);
        existing.setProviderId(1L);
        existing.setConsecutiveFailures(2);
        existing.setStatus(HealthStatus.UNKNOWN);
        given(healthMapper.selectOne(any())).willReturn(existing);

        healthService.recordFailure(1L, "connection refused");

        ArgumentCaptor<ProviderHealthPO> captor = ArgumentCaptor.forClass(ProviderHealthPO.class);
        verify(healthMapper).updateById(captor.capture());
        ProviderHealthPO h = captor.getValue();
        assertThat(h.getId()).isEqualTo(10L);
        assertThat(h.getStatus()).isEqualTo(HealthStatus.DOWN);
        assertThat(h.getConsecutiveFailures()).isEqualTo(3);
    }

    @Test
    void getHealth_whenExists_shouldReturnVo() {
        ProviderHealthPO health = new ProviderHealthPO();
        health.setProviderId(1L);
        health.setStatus(HealthStatus.UP);
        health.setConsecutiveFailures(0);
        health.setLastLatencyMs(100);
        given(healthMapper.selectOne(any())).willReturn(health);

        ProviderPO provider = new ProviderPO();
        provider.setId(1L);
        provider.setName("OpenAI");
        given(providerMapper.selectById(eq(1L))).willReturn(provider);

        ProviderHealthVO vo = healthService.getHealth(1L);

        assertThat(vo.getProviderId()).isEqualTo(1L);
        assertThat(vo.getProviderName()).isEqualTo("OpenAI");
        assertThat(vo.getStatus()).isEqualTo("UP");
        assertThat(vo.getConsecutiveFailures()).isZero();
        assertThat(vo.getLastLatencyMs()).isEqualTo(100);
    }

    @Test
    void getHealth_whenNotExists_shouldReturnUnknown() {
        given(healthMapper.selectOne(any())).willReturn(null);

        ProviderHealthVO vo = healthService.getHealth(1L);

        assertThat(vo.getProviderId()).isEqualTo(1L);
        assertThat(vo.getStatus()).isEqualTo("UNKNOWN");
        assertThat(vo.getConsecutiveFailures()).isNull();
    }
}
