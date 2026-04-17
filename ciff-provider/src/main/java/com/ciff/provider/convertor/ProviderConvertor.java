package com.ciff.provider.convertor;

import com.ciff.common.enums.ProviderStatus;
import com.ciff.provider.dto.ProviderCreateRequest;
import com.ciff.provider.dto.ProviderUpdateRequest;
import com.ciff.provider.dto.ProviderVO;
import com.ciff.provider.entity.ProviderPO;

/**
 * Provider 对象转换器。
 * Request → PO、PO → VO，apiKey 加密由 Service 层处理，Convertor 不涉及加密逻辑。
 */
public final class ProviderConvertor {

    private ProviderConvertor() {
    }

    public static ProviderPO toPO(ProviderCreateRequest request) {
        ProviderPO po = new ProviderPO();
        po.setName(request.getName());
        po.setType(request.getType());
        po.setAuthType(request.getAuthType());
        po.setApiBaseUrl(request.getApiBaseUrl());
        po.setAuthConfig(request.getAuthConfig());
        po.setStatus(ProviderStatus.ACTIVE);
        return po;
    }

    public static void updatePO(ProviderPO po, ProviderUpdateRequest request) {
        if (request.getName() != null) {
            po.setName(request.getName());
        }
        if (request.getType() != null) {
            po.setType(request.getType());
        }
        if (request.getAuthType() != null) {
            po.setAuthType(request.getAuthType());
        }
        if (request.getApiBaseUrl() != null) {
            po.setApiBaseUrl(request.getApiBaseUrl());
        }
        if (request.getAuthConfig() != null) {
            po.setAuthConfig(request.getAuthConfig());
        }
        if (request.getStatus() != null) {
            po.setStatus(request.getStatus());
        }
        // apiKey 更新由 Service 层处理（需加密），不在此处赋值
    }

    public static ProviderVO toVO(ProviderPO po) {
        ProviderVO vo = new ProviderVO();
        vo.setId(po.getId());
        vo.setName(po.getName());
        vo.setType(po.getType() != null ? po.getType().getType() : null);
        vo.setTypeDisplayName(po.getType() != null ? po.getType().getDisplayName() : null);
        vo.setAuthType(po.getAuthType() != null ? po.getAuthType().getType() : null);
        vo.setApiBaseUrl(po.getApiBaseUrl());
        vo.setApiKeyMasked(maskApiKey(po.getApiKeyEncrypted()));
        vo.setAuthConfig(po.getAuthConfig());
        vo.setStatus(po.getStatus() != null ? po.getStatus().getValue() : null);
        vo.setCreateTime(po.getCreateTime());
        vo.setUpdateTime(po.getUpdateTime());
        return vo;
    }

    /**
     * 对加密后的 apiKey 生成掩码展示。
     * 无法从密文还原前缀，故直接返回固定掩码格式。
     */
    private static String maskApiKey(String apiKeyEncrypted) {
        if (apiKeyEncrypted == null || apiKeyEncrypted.isEmpty()) {
            return null;
        }
        return "******";
    }
}
