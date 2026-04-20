package com.ciff.provider.facade;

import com.ciff.common.constant.ErrorCode;
import com.ciff.common.exception.BizException;
import com.ciff.common.util.ApiKeyEncryptor;
import com.ciff.provider.convertor.ModelConvertor;
import com.ciff.provider.dto.LlmCallConfig;
import com.ciff.provider.dto.ModelVO;
import com.ciff.provider.entity.ModelPO;
import com.ciff.provider.entity.ProviderPO;
import com.ciff.provider.mapper.ModelMapper;
import com.ciff.provider.mapper.ProviderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProviderFacadeImpl implements ProviderFacade {

    private final ModelMapper modelMapper;
    private final ProviderMapper providerMapper;
    private final ApiKeyEncryptor apiKeyEncryptor;

    @Override
    public ModelVO getModelById(Long modelId) {
        ModelPO po = modelMapper.selectById(modelId);
        return po != null ? ModelConvertor.toVO(po) : null;
    }

    @Override
    public LlmCallConfig getLlmCallConfig(Long modelId) {
        ModelPO model = modelMapper.selectById(modelId);
        if (model == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "模型不存在: " + modelId);
        }

        ProviderPO provider = providerMapper.selectById(model.getProviderId());
        if (provider == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "供应商不存在: " + model.getProviderId());
        }

        LlmCallConfig config = new LlmCallConfig();
        config.setProviderId(provider.getId());
        config.setProviderName(provider.getName());
        config.setProviderType(provider.getType());
        config.setApiBaseUrl(provider.getApiBaseUrl());
        config.setApiKey(provider.getApiKeyEncrypted() != null
                ? apiKeyEncryptor.decrypt(provider.getApiKeyEncrypted()) : null);
        config.setModelName(model.getName());
        config.setDefaultParams(model.getDefaultParams());
        config.setMaxTokens(model.getMaxTokens());
        return config;
    }
}
