package com.ciff.provider.service;

import com.ciff.common.dto.PageResult;
import com.ciff.common.enums.ProviderStatus;
import com.ciff.common.enums.ProviderType;
import com.ciff.provider.dto.ProviderCreateRequest;
import com.ciff.provider.dto.ProviderListItemVO;
import com.ciff.provider.dto.ProviderUpdateRequest;
import com.ciff.provider.dto.ProviderVO;

import java.util.List;

public interface ProviderService {

    ProviderVO create(ProviderCreateRequest request);

    ProviderVO update(Long id, ProviderUpdateRequest request);

    ProviderVO getById(Long id);

    void delete(Long id);

    PageResult<ProviderVO> page(Integer page, Integer pageSize, ProviderType type, ProviderStatus status);

    List<ProviderListItemVO> listAll();
}
