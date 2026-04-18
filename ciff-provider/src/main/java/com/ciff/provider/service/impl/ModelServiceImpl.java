package com.ciff.provider.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ciff.common.constant.ErrorCode;
import com.ciff.common.dto.PageResult;
import com.ciff.common.exception.BizException;
import com.ciff.common.util.PageHelper;
import com.ciff.provider.convertor.ModelConvertor;
import com.ciff.provider.dto.ModelCreateRequest;
import com.ciff.provider.dto.ModelUpdateRequest;
import com.ciff.provider.dto.ModelVO;
import com.ciff.provider.entity.ModelPO;
import com.ciff.provider.entity.ProviderPO;
import com.ciff.provider.mapper.ModelMapper;
import com.ciff.provider.mapper.ProviderMapper;
import com.ciff.provider.service.ModelService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModelServiceImpl implements ModelService {

    private final ModelMapper modelMapper;
    private final ProviderMapper providerMapper;
    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper;

    @Override
    public ModelVO create(ModelCreateRequest request) {
        requireProviderExists(request.getProviderId());
        validateDefaultParams(request.getDefaultParams());

        ModelPO po = ModelConvertor.toPO(request);
        modelMapper.insert(po);
        evictProviderCache(request.getProviderId());
        return enrichProviderName(ModelConvertor.toVO(po));
    }

    @Override
    public ModelVO update(Long id, ModelUpdateRequest request) {
        ModelPO po = requireExists(id);
        Long oldProviderId = po.getProviderId();

        if (request.getProviderId() != null && !request.getProviderId().equals(po.getProviderId())) {
            requireProviderExists(request.getProviderId());
        }
        if (request.getDefaultParams() != null) {
            validateDefaultParams(request.getDefaultParams());
        }

        ModelConvertor.updatePO(po, request);
        modelMapper.updateById(po);

        evictProviderCache(oldProviderId);
        if (request.getProviderId() != null && !request.getProviderId().equals(oldProviderId)) {
            evictProviderCache(request.getProviderId());
        }

        return enrichProviderName(ModelConvertor.toVO(po));
    }

    @Override
    public ModelVO getById(Long id) {
        return enrichProviderName(ModelConvertor.toVO(requireExists(id)));
    }

    @Override
    public void delete(Long id) {
        ModelPO po = requireExists(id);
        modelMapper.deleteById(id);
        evictProviderCache(po.getProviderId());
    }

    @Override
    public PageResult<ModelVO> page(Integer page, Integer pageSize, Long providerId, String status) {
        Page<ModelPO> pageParam = PageHelper.toPage(page, pageSize);

        LambdaQueryWrapper<ModelPO> wrapper = new LambdaQueryWrapper<ModelPO>()
                .eq(providerId != null, ModelPO::getProviderId, providerId)
                .eq(status != null && !status.isEmpty(), ModelPO::getStatus, status)
                .orderByDesc(ModelPO::getCreateTime);

        Page<ModelPO> result = modelMapper.selectPage(pageParam, wrapper);
        PageResult<ModelVO> pageResult = PageHelper.toPageResult(result, ModelConvertor::toVO);

        // 填充 providerName
        pageResult.getList().forEach(this::enrichProviderName);
        return pageResult;
    }

    @Override
    public List<ModelVO> listByProviderId(Long providerId) {
        requireProviderExists(providerId);

        List<ModelPO> models = modelMapper.selectList(
                new LambdaQueryWrapper<ModelPO>()
                        .eq(ModelPO::getProviderId, providerId)
                        .orderByDesc(ModelPO::getCreateTime));

        return models.stream()
                .map(ModelConvertor::toVO)
                .peek(this::enrichProviderName)
                .toList();
    }

    private ModelVO enrichProviderName(ModelVO vo) {
        if (vo.getProviderId() != null) {
            ProviderPO provider = providerMapper.selectById(vo.getProviderId());
            if (provider != null) {
                vo.setProviderName(provider.getName());
            }
        }
        return vo;
    }

    private ModelPO requireExists(Long id) {
        ModelPO po = modelMapper.selectById(id);
        if (po == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "模型不存在: " + id);
        }
        return po;
    }

    private void requireProviderExists(Long providerId) {
        if (providerMapper.selectById(providerId) == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "供应商不存在: " + providerId);
        }
    }

    private void validateDefaultParams(String defaultParams) {
        if (defaultParams == null || defaultParams.isBlank()) {
            return;
        }
        try {
            objectMapper.readTree(defaultParams);
        } catch (JsonProcessingException e) {
            throw new BizException(ErrorCode.BAD_REQUEST, "defaultParams 不是有效的 JSON: " + e.getMessage());
        }
    }

    private void evictProviderCache(Long providerId) {
        var cache = cacheManager.getCache("provider-cache");
        if (cache != null) {
            cache.evict(providerId);
        }
    }
}
