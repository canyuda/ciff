package com.ciff.provider.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ciff.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_provider_health")
public class ProviderHealthPO extends BaseEntity {

    private Long providerId;

    private String status;

    private Integer consecutiveFailures;

    private Integer lastLatencyMs;

    private LocalDateTime lastSuccessTime;

    private LocalDateTime lastFailureTime;

    private String lastFailureReason;

    private LocalDateTime lastProbeTime;
}
