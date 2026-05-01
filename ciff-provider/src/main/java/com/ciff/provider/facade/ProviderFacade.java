package com.ciff.provider.facade;

import com.ciff.provider.dto.LlmCallConfig;
import com.ciff.provider.dto.ModelVO;
import com.ciff.provider.entity.ProviderPO;

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

    /**
     * Get ProviderPO by provider id.
     *
     * @throws com.ciff.common.exception.BizException if provider not found
     */
    ProviderPO getProviderById(Long providerId);
}
