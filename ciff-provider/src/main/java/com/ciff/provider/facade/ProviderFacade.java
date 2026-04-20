package com.ciff.provider.facade;

import com.ciff.provider.dto.LlmCallConfig;
import com.ciff.provider.dto.ModelVO;

/**
 * Provider facade for cross-module access.
 */
public interface ProviderFacade {

    /** Get model by id. Returns null if not found. */
    ModelVO getModelById(Long modelId);

    /**
     * Resolve LLM call config from modelId: apiBaseUrl, decrypted apiKey,
     * modelName, providerType, defaultParams.
     *
     * @throws com.ciff.common.exception.BizException if model or provider not found
     */
    LlmCallConfig getLlmCallConfig(Long modelId);
}
