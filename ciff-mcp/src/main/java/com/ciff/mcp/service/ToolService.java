package com.ciff.mcp.service;

import com.ciff.common.dto.PageResult;
import com.ciff.mcp.dto.ToolCreateRequest;
import com.ciff.mcp.dto.ToolUpdateRequest;
import com.ciff.mcp.dto.ToolVO;

public interface ToolService {

    ToolVO create(ToolCreateRequest request);

    ToolVO update(Long id, ToolUpdateRequest request);

    ToolVO getById(Long id);

    void delete(Long id);

    PageResult<ToolVO> page(Integer page, Integer pageSize, String type, String status);
}
