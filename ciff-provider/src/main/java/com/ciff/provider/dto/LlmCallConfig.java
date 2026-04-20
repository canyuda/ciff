package com.ciff.provider.dto;

import com.ciff.common.enums.ProviderType;
import lombok.Data;

/**
 * Configuration needed to call an LLM, resolved from model → provider.
 */
@Data
public class LlmCallConfig {

    private Long providerId;

    private String providerName;

    private ProviderType providerType;

    private String apiBaseUrl;

    private String apiKey;

    private String modelName;

    private ModelDefaultParam defaultParams;

    private Integer maxTokens;
}
