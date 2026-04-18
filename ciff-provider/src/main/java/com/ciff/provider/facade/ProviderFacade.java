package com.ciff.provider.facade;

import com.ciff.provider.dto.ModelVO;

/**
 * Provider facade for cross-module access.
 */
public interface ProviderFacade {

    /** Get model by id. Returns null if not found. */
    ModelVO getModelById(Long modelId);
}
