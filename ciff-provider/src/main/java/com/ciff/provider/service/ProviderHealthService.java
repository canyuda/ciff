package com.ciff.provider.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ciff.common.enums.HealthStatus;
import com.ciff.provider.dto.ProviderHealthVO;
import com.ciff.provider.entity.ProviderHealthPO;
import com.ciff.provider.entity.ProviderPO;
import com.ciff.provider.mapper.ProviderHealthMapper;
import com.ciff.provider.mapper.ProviderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderHealthService {

    private final ProviderHealthMapper healthMapper;
    private final ProviderMapper providerMapper;

    /**
     * Record probe success, reset consecutive failure count.
     */
    public void recordSuccess(Long providerId, int latencyMs) {
        ProviderHealthPO health = getOrCreate(providerId);
        health.setConsecutiveFailures(0);
        health.setLastLatencyMs(latencyMs);
        health.setLastSuccessTime(LocalDateTime.now());
        health.setLastProbeTime(LocalDateTime.now());
        health.setStatus(HealthStatus.UP);
        saveOrUpdate(health);
    }

    /**
     * Record probe failure, increment consecutive failure count.
     */
    public void recordFailure(Long providerId, String reason) {
        ProviderHealthPO health = getOrCreate(providerId);
        health.setConsecutiveFailures(
                health.getConsecutiveFailures() != null ? health.getConsecutiveFailures() + 1 : 1);
        health.setLastFailureTime(LocalDateTime.now());
        health.setLastFailureReason(truncate(reason, 500));
        health.setLastProbeTime(LocalDateTime.now());
        health.setStatus(determineStatus(health.getConsecutiveFailures()));
        saveOrUpdate(health);
    }

    /**
     * Query provider health status.
     */
    public ProviderHealthVO getHealth(Long providerId) {
        ProviderHealthPO health = healthMapper.selectOne(
                new LambdaQueryWrapper<ProviderHealthPO>().eq(ProviderHealthPO::getProviderId, providerId));

        ProviderHealthVO vo = new ProviderHealthVO();
        vo.setProviderId(providerId);

        if (health == null) {
            vo.setStatus(HealthStatus.UNKNOWN.getValue());
            return vo;
        }

        ProviderPO provider = providerMapper.selectById(providerId);
        if (provider != null) {
            vo.setProviderName(provider.getName());
        }

        vo.setStatus(health.getStatus().getValue());
        vo.setConsecutiveFailures(health.getConsecutiveFailures());
        vo.setLastLatencyMs(health.getLastLatencyMs());
        vo.setLastSuccessTime(health.getLastSuccessTime());
        vo.setLastFailureTime(health.getLastFailureTime());
        vo.setLastFailureReason(health.getLastFailureReason());
        vo.setLastProbeTime(health.getLastProbeTime());
        return vo;
    }

    private ProviderHealthPO getOrCreate(Long providerId) {
        ProviderHealthPO health = healthMapper.selectOne(
                new LambdaQueryWrapper<ProviderHealthPO>().eq(ProviderHealthPO::getProviderId, providerId));
        if (health == null) {
            health = new ProviderHealthPO();
            health.setProviderId(providerId);
            health.setConsecutiveFailures(0);
            health.setStatus(HealthStatus.UNKNOWN);
        }
        return health;
    }

    private void saveOrUpdate(ProviderHealthPO health) {
        if (health.getId() == null) {
            healthMapper.insert(health);
        } else {
            healthMapper.updateById(health);
        }
    }

    /**
     * Status: 0 failures → UP, ≥ 3 → DOWN, otherwise → UNKNOWN.
     */
    private HealthStatus determineStatus(int consecutiveFailures) {
        if (consecutiveFailures == 0) {
            return HealthStatus.UP;
        }
        if (consecutiveFailures >= 3) {
            return HealthStatus.DOWN;
        }
        return HealthStatus.UNKNOWN;
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }
}
