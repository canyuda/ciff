package com.ciff.provider.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ciff.common.constant.ErrorCode;
import com.ciff.common.dto.PageResult;
import com.ciff.common.exception.BizException;
import com.ciff.common.util.ApiKeyEncryptor;
import com.ciff.common.util.PageHelper;
import com.ciff.provider.convertor.ProviderConvertor;
import com.ciff.provider.dto.ProviderCreateRequest;
import com.ciff.provider.dto.ProviderUpdateRequest;
import com.ciff.provider.dto.ProviderVO;
import com.ciff.provider.entity.ProviderPO;
import com.ciff.provider.mapper.ModelMapper;
import com.ciff.provider.mapper.ProviderMapper;
import com.ciff.provider.service.ProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProviderServiceImpl implements ProviderService {

    private final ProviderMapper providerMapper;
    private final ModelMapper modelMapper;
    private final ApiKeyEncryptor apiKeyEncryptor;

    @Override
    public ProviderVO create(ProviderCreateRequest request) {
        // 校验 name 唯一
        Long count = providerMapper.selectCount(
                new LambdaQueryWrapper<ProviderPO>().eq(ProviderPO::getName, request.getName()));
        if (count > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "供应商名称已存在: " + request.getName());
        }

        ProviderPO po = ProviderConvertor.toPO(request);

        // apiKey 加密存储
        if (request.getApiKey() != null && !request.getApiKey().isEmpty()) {
            po.setApiKeyEncrypted(apiKeyEncryptor.encrypt(request.getApiKey()));
        }

        providerMapper.insert(po);
        return ProviderConvertor.toVO(po);
    }

    @Override
    public ProviderVO update(Long id, ProviderUpdateRequest request) {
        ProviderPO po = requireExists(id);

        // 名称唯一性校验（如果修改了名称）
        if (request.getName() != null && !request.getName().equals(po.getName())) {
            Long count = providerMapper.selectCount(
                    new LambdaQueryWrapper<ProviderPO>().eq(ProviderPO::getName, request.getName()));
            if (count > 0) {
                throw new BizException(ErrorCode.BAD_REQUEST, "供应商名称已存在: " + request.getName());
            }
        }

        ProviderConvertor.updatePO(po, request);

        // apiKey 如传入则重新加密
        if (request.getApiKey() != null && !request.getApiKey().isEmpty()) {
            po.setApiKeyEncrypted(apiKeyEncryptor.encrypt(request.getApiKey()));
        }

        providerMapper.updateById(po);
        return ProviderConvertor.toVO(po);
    }

    @Override
    public ProviderVO getById(Long id) {
        return ProviderConvertor.toVO(requireExists(id));
    }

    @Override
    public void delete(Long id) {
        requireExists(id);

        // 检查是否有关联 Model
        Long modelCount = modelMapper.selectCount(
                new LambdaQueryWrapper<com.ciff.provider.entity.ModelPO>()
                        .eq(com.ciff.provider.entity.ModelPO::getProviderId, id));
        if (modelCount > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "该供应商下存在关联模型，无法删除");
        }

        providerMapper.deleteById(id);
    }

    @Override
    public PageResult<ProviderVO> page(Integer page, Integer pageSize, String status) {
        Page<ProviderPO> pageParam = PageHelper.toPage(page, pageSize);

        LambdaQueryWrapper<ProviderPO> wrapper = new LambdaQueryWrapper<ProviderPO>()
                .eq(status != null && !status.isEmpty(), ProviderPO::getStatus, status)
                .orderByDesc(ProviderPO::getCreateTime);

        Page<ProviderPO> result = providerMapper.selectPage(pageParam, wrapper);
        return PageHelper.toPageResult(result, ProviderConvertor::toVO);
    }

    private ProviderPO requireExists(Long id) {
        ProviderPO po = providerMapper.selectById(id);
        if (po == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "供应商不存在: " + id);
        }
        return po;
    }
}
