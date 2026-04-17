package com.ciff.provider.service;

import com.ciff.common.dto.PageResult;
import com.ciff.provider.dto.ModelCreateRequest;
import com.ciff.provider.dto.ModelUpdateRequest;
import com.ciff.provider.dto.ModelVO;

import java.util.List;

public interface ModelService {

    ModelVO create(ModelCreateRequest request);

    ModelVO update(Long id, ModelUpdateRequest request);

    ModelVO getById(Long id);

    void delete(Long id);

    PageResult<ModelVO> page(Integer page, Integer pageSize, Long providerId, String status);

    List<ModelVO> listByProviderId(Long providerId);
}
