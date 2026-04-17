package com.ciff.provider.convertor;

import com.ciff.provider.dto.ModelCreateRequest;
import com.ciff.provider.dto.ModelUpdateRequest;
import com.ciff.provider.dto.ModelVO;
import com.ciff.provider.entity.ModelPO;

/**
 * Model 对象转换器。
 * providerName 由 Service 层查询后设置，Convertor 不涉及关联查询。
 */
public final class ModelConvertor {

    private ModelConvertor() {
    }

    public static ModelPO toPO(ModelCreateRequest request) {
        ModelPO po = new ModelPO();
        po.setProviderId(request.getProviderId());
        po.setName(request.getName());
        po.setDisplayName(request.getDisplayName());
        po.setMaxTokens(request.getMaxTokens());
        po.setDefaultParams(request.getDefaultParams());
        po.setStatus("active");
        return po;
    }

    public static void updatePO(ModelPO po, ModelUpdateRequest request) {
        if (request.getProviderId() != null) {
            po.setProviderId(request.getProviderId());
        }
        if (request.getName() != null) {
            po.setName(request.getName());
        }
        if (request.getDisplayName() != null) {
            po.setDisplayName(request.getDisplayName());
        }
        if (request.getMaxTokens() != null) {
            po.setMaxTokens(request.getMaxTokens());
        }
        if (request.getDefaultParams() != null) {
            po.setDefaultParams(request.getDefaultParams());
        }
        if (request.getStatus() != null) {
            po.setStatus(request.getStatus());
        }
    }

    public static ModelVO toVO(ModelPO po) {
        ModelVO vo = new ModelVO();
        vo.setId(po.getId());
        vo.setProviderId(po.getProviderId());
        vo.setName(po.getName());
        vo.setDisplayName(po.getDisplayName());
        vo.setMaxTokens(po.getMaxTokens());
        vo.setDefaultParams(po.getDefaultParams());
        vo.setStatus(po.getStatus());
        vo.setCreateTime(po.getCreateTime());
        vo.setUpdateTime(po.getUpdateTime());
        return vo;
    }
}
