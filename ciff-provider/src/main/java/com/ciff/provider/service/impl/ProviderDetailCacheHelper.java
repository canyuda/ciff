package com.ciff.provider.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ciff.common.constant.ErrorCode;
import com.ciff.common.exception.BizException;
import com.ciff.provider.convertor.ModelConvertor;
import com.ciff.provider.convertor.ProviderConvertor;
import com.ciff.provider.dto.ProviderVO;
import com.ciff.provider.entity.ModelPO;
import com.ciff.provider.entity.ProviderPO;
import com.ciff.provider.mapper.ModelMapper;
import com.ciff.provider.mapper.ProviderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Provider detail cache helper.
 * Separate bean to avoid self-invocation bypassing Spring AOP proxy.
 * Caches base info + associated models; health status is excluded and queried in real-time.
 */
@Component
@RequiredArgsConstructor
public class ProviderDetailCacheHelper {

    private final ProviderMapper providerMapper;
    private final ModelMapper modelMapper;

    @Cacheable(cacheNames = "provider-cache", key = "#id")
    public ProviderVO getDetail(Long id) {
        ProviderPO po = providerMapper.selectById(id);
        if (po == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "供应商不存在: " + id);
        }

        ProviderVO vo = ProviderConvertor.toVO(po);

        List<ModelPO> models = modelMapper.selectList(
                new LambdaQueryWrapper<ModelPO>()
                        .eq(ModelPO::getProviderId, id)
                        .orderByAsc(ModelPO::getCreateTime));
        vo.setModels(models.stream().map(ModelConvertor::toVO).toList());

        return vo;
    }
}
