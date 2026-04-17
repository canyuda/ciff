package com.ciff.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Provider entity status.
 * Controls whether the provider participates in routing and health checks.
 */
@Getter
@RequiredArgsConstructor
public enum ProviderStatus {

    ACTIVE("active"),
    INACTIVE("inactive");
    @EnumValue
    @JsonValue
    private final String value;
}
