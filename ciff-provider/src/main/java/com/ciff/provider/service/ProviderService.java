package com.ciff.provider.service;

import com.ciff.common.dto.PageResult;
import com.ciff.provider.dto.ProviderCreateRequest;
import com.ciff.provider.dto.ProviderUpdateRequest;
import com.ciff.provider.dto.ProviderVO;

public interface ProviderService {

    ProviderVO create(ProviderCreateRequest request);

    ProviderVO update(Long id, ProviderUpdateRequest request);

    ProviderVO getById(Long id);

    void delete(Long id);

    PageResult<ProviderVO> page(Integer page, Integer pageSize, String status);
}
