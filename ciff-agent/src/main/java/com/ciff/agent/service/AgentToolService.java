package com.ciff.agent.service;

import com.ciff.mcp.dto.ToolVO;

import java.util.List;

public interface AgentToolService {

    /** Bind a tool to an agent. Throws if already bound or tool not found. */
    void bind(Long agentId, Long toolId);

    /** Unbind a tool from an agent. */
    void unbind(Long agentId, Long toolId);

    /** Replace all tool bindings for an agent (full replacement). */
    void replaceAll(Long agentId, List<Long> toolIds);

    /** Get all tools bound to an agent. */
    List<ToolVO> listTools(Long agentId);

    /** Get all tool IDs bound to an agent. */
    List<Long> listToolIds(Long agentId);
}
