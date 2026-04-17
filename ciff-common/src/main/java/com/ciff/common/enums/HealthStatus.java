package com.ciff.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Provider health check status.
 * Updated by ProviderHealthScheduler based on probe results.
 */
@Getter
@RequiredArgsConstructor
public enum HealthStatus {

    UP("UP"),
    DOWN("DOWN"),
    UNKNOWN("UNKNOWN");
    @EnumValue
    @JsonValue
    private final String value;
}
