package com.ciff.agent.service.impl;

import com.ciff.agent.convertor.AgentConvertor;
import com.ciff.agent.dto.AgentVO;
import com.ciff.agent.entity.AgentPO;
import com.ciff.agent.mapper.AgentMapper;
import com.ciff.agent.service.AgentToolService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * Agent detail cache helper.
 * Separate bean to avoid self-invocation bypassing Spring AOP proxy.
 * Caches base info + associated tools.
 */
@Component
@RequiredArgsConstructor
public class AgentDetailCacheHelper {

    private final AgentMapper agentMapper;
    private final AgentToolService agentToolService;

    @Cacheable(cacheNames = "agent-cache", key = "#id")
    public AgentVO getDetail(Long id) {
        AgentPO po = agentMapper.selectById(id);
        if (po == null) {
            return null;
        }
        AgentVO vo = AgentConvertor.toVO(po);
        vo.setTools(agentToolService.listTools(po.getId()));
        return vo;
    }
}
