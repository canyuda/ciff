package com.ciff.agent.service;

import com.ciff.common.dto.PageResult;
import com.ciff.agent.dto.AgentCreateRequest;
import com.ciff.agent.dto.AgentUpdateRequest;
import com.ciff.agent.dto.AgentVO;

public interface AgentService {

    AgentVO create(AgentCreateRequest request, Long userId);

    AgentVO update(Long id, AgentUpdateRequest request, Long userId);

    AgentVO getById(Long id, Long userId);

    void delete(Long id, Long userId);

    PageResult<AgentVO> page(Integer page, Integer pageSize, String type, String status, Long userId);
}
