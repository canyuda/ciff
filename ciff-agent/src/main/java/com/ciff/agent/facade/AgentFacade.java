package com.ciff.agent.facade;

import com.ciff.agent.dto.AgentVO;

/**
 * Agent facade for cross-module access.
 */
public interface AgentFacade {

    AgentVO getById(Long id);
}
