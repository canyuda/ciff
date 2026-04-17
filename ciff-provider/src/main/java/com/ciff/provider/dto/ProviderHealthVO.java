package com.ciff.provider.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "供应商健康状态响应")
public class ProviderHealthVO {

    @Schema(description = "供应商ID")
    private Long providerId;

    @Schema(description = "供应商名称")
    private String providerName;

    @Schema(description = "健康状态：healthy / unhealthy / unknown")
    private String status;

    @Schema(description = "连续失败次数")
    private Integer consecutiveFailures;

    @Schema(description = "最近一次延迟（毫秒）")
    private Integer lastLatencyMs;

    @Schema(description = "最近一次成功时间")
    private LocalDateTime lastSuccessTime;

    @Schema(description = "最近一次失败时间")
    private LocalDateTime lastFailureTime;

    @Schema(description = "最近一次失败原因")
    private String lastFailureReason;

    @Schema(description = "最近一次探测时间")
    private LocalDateTime lastProbeTime;
}
