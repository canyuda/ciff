package com.ciff.mcp.facade;

import com.ciff.mcp.dto.ToolVO;

import java.util.List;

/**
 * Tool facade for cross-module access.
 * Only exposes read operations needed by other modules.
 */
public interface ToolFacade {

    ToolVO getById(Long id);

    List<ToolVO> listByIds(List<Long> ids);
}
