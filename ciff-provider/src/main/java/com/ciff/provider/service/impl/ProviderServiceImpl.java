package com.ciff.provider.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ciff.common.constant.ErrorCode;
import com.ciff.common.dto.PageResult;
import com.ciff.common.enums.ProviderStatus;
import com.ciff.common.enums.ProviderType;
import com.ciff.common.exception.BizException;
import com.ciff.common.util.ApiKeyEncryptor;
import com.ciff.common.util.PageHelper;
import com.ciff.provider.convertor.ProviderConvertor;
import com.ciff.provider.dto.ProviderCreateRequest;
import com.ciff.provider.dto.ProviderHealthVO;
import com.ciff.provider.dto.ProviderListItemVO;
import com.ciff.provider.dto.ProviderUpdateRequest;
import com.ciff.provider.dto.ProviderVO;
import com.ciff.provider.entity.ModelPO;
import com.ciff.provider.entity.ProviderHealthPO;
import com.ciff.provider.entity.ProviderPO;
import com.ciff.provider.mapper.ModelMapper;
import com.ciff.provider.mapper.ProviderHealthMapper;
import com.ciff.provider.mapper.ProviderMapper;
import com.ciff.provider.service.ProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProviderServiceImpl implements ProviderService {

    private final ProviderMapper providerMapper;
    private final ModelMapper modelMapper;
    private final ProviderHealthMapper healthMapper;
    private final ApiKeyEncryptor apiKeyEncryptor;
    private final ProviderDetailCacheHelper detailCacheHelper;

    @Override
    public ProviderVO create(ProviderCreateRequest request) {
        validateNameUnique(request.getName(), null);

        ProviderPO po = ProviderConvertor.toPO(request);

        if (request.getApiKey() != null && !request.getApiKey().isEmpty()) {
            po.setApiKeyEncrypted(apiKeyEncryptor.encrypt(request.getApiKey()));
        }

        providerMapper.insert(po);
        return ProviderConvertor.toVO(po);
    }

    @Override
    @CacheEvict(cacheNames = "provider-cache", key = "#id")
    public ProviderVO update(Long id, ProviderUpdateRequest request) {
        ProviderPO po = requireExists(id);

        if (request.getName() != null && !request.getName().equals(po.getName())) {
            validateNameUnique(request.getName(), id);
        }

        ProviderConvertor.updatePO(po, request);

        if (request.getApiKey() != null && !request.getApiKey().isEmpty()) {
            po.setApiKeyEncrypted(apiKeyEncryptor.encrypt(request.getApiKey()));
        }

        providerMapper.updateById(po);
        return ProviderConvertor.toVO(po);
    }

    @Override
    public ProviderVO getById(Long id) {
        // base info + models are cached; health is real-time
        ProviderVO vo = detailCacheHelper.getDetail(id);
        vo.setHealth(buildHealthVO(id, vo.getName()));
        return vo;
    }

    @Override
    @CacheEvict(cacheNames = "provider-cache", key = "#id")
    public void delete(Long id) {
        requireExists(id);

        Long modelCount = modelMapper.selectCount(
                new LambdaQueryWrapper<ModelPO>().eq(ModelPO::getProviderId, id));
        if (modelCount > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "该供应商下存在关联模型，无法删除");
        }

        providerMapper.deleteById(id);
    }

    @Override
    public PageResult<ProviderVO> page(Integer page, Integer pageSize, ProviderType type, ProviderStatus status) {
        Page<ProviderPO> pageParam = PageHelper.toPage(page, pageSize);

        LambdaQueryWrapper<ProviderPO> wrapper = new LambdaQueryWrapper<ProviderPO>()
                .eq(type != null, ProviderPO::getType, type)
                .eq(status != null, ProviderPO::getStatus, status)
                .orderByDesc(ProviderPO::getCreateTime);

        Page<ProviderPO> result = providerMapper.selectPage(pageParam, wrapper);
        return PageHelper.toPageResult(result, ProviderConvertor::toVO);
    }

    @Override
    public List<ProviderListItemVO> listAll() {
        return providerMapper.selectList(
                new LambdaQueryWrapper<ProviderPO>()
                        .select(ProviderPO::getId, ProviderPO::getName)
                        .orderByDesc(ProviderPO::getCreateTime)
        ).stream().map(po -> {
            ProviderListItemVO vo = new ProviderListItemVO();
            vo.setId(po.getId());
            vo.setName(po.getName());
            return vo;
        }).collect(Collectors.toList());
    }

    private void validateNameUnique(String name, Long excludeId) {
        LambdaQueryWrapper<ProviderPO> wrapper = new LambdaQueryWrapper<ProviderPO>()
                .eq(ProviderPO::getName, name);
        if (excludeId != null) {
            wrapper.ne(ProviderPO::getId, excludeId);
        }
        if (providerMapper.selectCount(wrapper) > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "供应商名称已存在: " + name);
        }
    }

    private ProviderPO requireExists(Long id) {
        ProviderPO po = providerMapper.selectById(id);
        if (po == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "供应商不存在: " + id);
        }
        return po;
    }

    private ProviderHealthVO buildHealthVO(Long providerId, String providerName) {
        ProviderHealthPO health = healthMapper.selectOne(
                new LambdaQueryWrapper<ProviderHealthPO>()
                        .eq(ProviderHealthPO::getProviderId, providerId));

        ProviderHealthVO vo = new ProviderHealthVO();
        vo.setProviderId(providerId);
        vo.setProviderName(providerName);

        if (health == null) {
            vo.setStatus("UNKNOWN");
            return vo;
        }

        vo.setStatus(health.getStatus() != null ? health.getStatus().getValue() : null);
        vo.setConsecutiveFailures(health.getConsecutiveFailures());
        vo.setLastLatencyMs(health.getLastLatencyMs());
        vo.setLastSuccessTime(health.getLastSuccessTime());
        vo.setLastFailureTime(health.getLastFailureTime());
        vo.setLastFailureReason(health.getLastFailureReason());
        vo.setLastProbeTime(health.getLastProbeTime());
        return vo;
    }
}
