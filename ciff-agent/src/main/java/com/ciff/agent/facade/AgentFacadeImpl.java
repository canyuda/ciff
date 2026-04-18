package com.ciff.agent.facade;

import com.ciff.agent.convertor.AgentConvertor;
import com.ciff.agent.dto.AgentVO;
import com.ciff.agent.entity.AgentPO;
import com.ciff.agent.mapper.AgentMapper;
import com.ciff.agent.service.AgentToolService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentFacadeImpl implements AgentFacade {

    private final AgentMapper agentMapper;
    private final AgentToolService agentToolService;

    @Override
    public AgentVO getById(Long id) {
        AgentPO po = agentMapper.selectById(id);
        if (po == null) {
            return null;
        }
        AgentVO vo = AgentConvertor.toVO(po);
        vo.setTools(agentToolService.listTools(po.getId()));
        return vo;
    }

    @Override
    public List<Long> getToolIds(Long agentId) {
        return agentToolService.listToolIds(agentId);
    }
}
