package com.ciff.provider.facade;

import com.ciff.provider.convertor.ModelConvertor;
import com.ciff.provider.dto.ModelVO;
import com.ciff.provider.entity.ModelPO;
import com.ciff.provider.mapper.ModelMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProviderFacadeImpl implements ProviderFacade {

    private final ModelMapper modelMapper;

    @Override
    public ModelVO getModelById(Long modelId) {
        ModelPO po = modelMapper.selectById(modelId);
        return po != null ? ModelConvertor.toVO(po) : null;
    }
}
