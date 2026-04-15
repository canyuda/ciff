package com.ciff.common.resilience;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "ciff.resilience.circuit-breaker")
public class CircuitBreakerProperties {

    private int slidingWindowSize = 10;
    private float failureRateThreshold = 50;
    private Duration waitDurationInOpenState = Duration.ofSeconds(30);
    private int permittedNumberOfCallsInHalfOpenState = 3;
}