package com.ciff.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ciff.agent.entity.AgentToolPO;
import com.ciff.agent.mapper.AgentToolMapper;
import com.ciff.agent.service.AgentToolService;
import com.ciff.common.constant.ErrorCode;
import com.ciff.common.exception.BizException;
import com.ciff.mcp.dto.ToolVO;
import com.ciff.mcp.facade.ToolFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentToolServiceImpl implements AgentToolService {

    private final AgentToolMapper agentToolMapper;
    private final ToolFacade toolFacade;

    @Override
    public void bind(Long agentId, Long toolId) {
        validateToolExists(toolId);

        if (isBound(agentId, toolId)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "工具已绑定到该 Agent: toolId=" + toolId);
        }

        AgentToolPO po = new AgentToolPO();
        po.setAgentId(agentId);
        po.setToolId(toolId);
        agentToolMapper.insert(po);
    }

    @Override
    public void unbind(Long agentId, Long toolId) {
        agentToolMapper.delete(new LambdaQueryWrapper<AgentToolPO>()
                .eq(AgentToolPO::getAgentId, agentId)
                .eq(AgentToolPO::getToolId, toolId));
    }

    @Override
    @Transactional
    public void replaceAll(Long agentId, List<Long> toolIds) {
        // validate all tools exist
        if (toolIds != null) {
            for (Long toolId : toolIds) {
                validateToolExists(toolId);
            }
        }

        // delete existing bindings
        agentToolMapper.delete(new LambdaQueryWrapper<AgentToolPO>()
                .eq(AgentToolPO::getAgentId, agentId));

        // insert new bindings
        if (toolIds != null && !toolIds.isEmpty()) {
            for (Long toolId : toolIds) {
                AgentToolPO po = new AgentToolPO();
                po.setAgentId(agentId);
                po.setToolId(toolId);
                agentToolMapper.insert(po);
            }
        }
    }

    @Override
    public List<ToolVO> listTools(Long agentId) {
        List<Long> toolIds = listToolIds(agentId);
        if (toolIds.isEmpty()) {
            return Collections.emptyList();
        }
        return toolFacade.listByIds(toolIds);
    }

    @Override
    public List<Long> listToolIds(Long agentId) {
        return agentToolMapper.selectList(
                new LambdaQueryWrapper<AgentToolPO>()
                        .eq(AgentToolPO::getAgentId, agentId))
                .stream()
                .map(AgentToolPO::getToolId)
                .toList();
    }

    private boolean isBound(Long agentId, Long toolId) {
        return agentToolMapper.selectCount(
                new LambdaQueryWrapper<AgentToolPO>()
                        .eq(AgentToolPO::getAgentId, agentId)
                        .eq(AgentToolPO::getToolId, toolId)) > 0;
    }

    private void validateToolExists(Long toolId) {
        ToolVO tool = toolFacade.getById(toolId);
        if (tool == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "工具不存在: " + toolId);
        }
    }
}
