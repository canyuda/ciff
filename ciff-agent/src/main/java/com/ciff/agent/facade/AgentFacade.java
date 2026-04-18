package com.ciff.agent.facade;

import com.ciff.agent.dto.AgentVO;

import java.util.List;

/**
 * Agent facade for cross-module access.
 */
public interface AgentFacade {

    AgentVO getById(Long id);

    List<Long> getToolIds(Long agentId);
}
