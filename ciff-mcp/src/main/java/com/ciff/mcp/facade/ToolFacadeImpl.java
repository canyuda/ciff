package com.ciff.mcp.facade;

import com.ciff.mcp.convertor.ToolConvertor;
import com.ciff.mcp.dto.ToolVO;
import com.ciff.mcp.entity.ToolPO;
import com.ciff.mcp.mapper.ToolMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ToolFacadeImpl implements ToolFacade {

    private final ToolMapper toolMapper;

    @Override
    public ToolVO getById(Long id) {
        ToolPO po = toolMapper.selectById(id);
        return po != null ? ToolConvertor.toVO(po) : null;
    }

    @Override
    public List<ToolVO> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return toolMapper.selectBatchIds(ids).stream()
                .map(ToolConvertor::toVO)
                .toList();
    }
}
