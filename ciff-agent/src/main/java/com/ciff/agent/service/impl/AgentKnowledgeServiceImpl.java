package com.ciff.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ciff.agent.entity.AgentKnowledgePO;
import com.ciff.agent.mapper.AgentKnowledgeMapper;
import com.ciff.agent.service.AgentKnowledgeService;
import com.ciff.common.constant.ErrorCode;
import com.ciff.common.exception.BizException;
import com.ciff.knowledge.dto.KnowledgeVO;
import com.ciff.knowledge.facade.KnowledgeFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentKnowledgeServiceImpl implements AgentKnowledgeService {

    private final AgentKnowledgeMapper agentKnowledgeMapper;
    private final KnowledgeFacade knowledgeFacade;

    @Override
    @CacheEvict(cacheNames = "agent-cache", key = "#agentId")
    public void bind(Long agentId, Long knowledgeId) {
        validateKnowledgeExists(knowledgeId);

        if (isBound(agentId, knowledgeId)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "知识库已绑定到该 Agent: knowledgeId=" + knowledgeId);
        }

        AgentKnowledgePO po = new AgentKnowledgePO();
        po.setAgentId(agentId);
        po.setKnowledgeId(knowledgeId);
        agentKnowledgeMapper.insert(po);
    }

    @Override
    @CacheEvict(cacheNames = "agent-cache", key = "#agentId")
    public void unbind(Long agentId, Long knowledgeId) {
        agentKnowledgeMapper.delete(new LambdaQueryWrapper<AgentKnowledgePO>()
                .eq(AgentKnowledgePO::getAgentId, agentId)
                .eq(AgentKnowledgePO::getKnowledgeId, knowledgeId));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "agent-cache", key = "#agentId")
    public void replaceAll(Long agentId, List<Long> knowledgeIds) {
        if (knowledgeIds != null) {
            for (Long knowledgeId : knowledgeIds) {
                validateKnowledgeExists(knowledgeId);
            }
        }

        agentKnowledgeMapper.delete(new LambdaQueryWrapper<AgentKnowledgePO>()
                .eq(AgentKnowledgePO::getAgentId, agentId));

        if (knowledgeIds != null && !knowledgeIds.isEmpty()) {
            for (Long knowledgeId : knowledgeIds) {
                AgentKnowledgePO po = new AgentKnowledgePO();
                po.setAgentId(agentId);
                po.setKnowledgeId(knowledgeId);
                agentKnowledgeMapper.insert(po);
            }
        }
    }

    @Override
    public List<KnowledgeVO> listKnowledges(Long agentId) {
        List<Long> knowledgeIds = listKnowledgeIds(agentId);
        if (knowledgeIds.isEmpty()) {
            return Collections.emptyList();
        }
        return knowledgeFacade.listByIds(knowledgeIds);
    }

    @Override
    public List<Long> listKnowledgeIds(Long agentId) {
        return agentKnowledgeMapper.selectList(
                new LambdaQueryWrapper<AgentKnowledgePO>()
                        .eq(AgentKnowledgePO::getAgentId, agentId))
                .stream()
                .map(AgentKnowledgePO::getKnowledgeId)
                .toList();
    }

    private boolean isBound(Long agentId, Long knowledgeId) {
        return agentKnowledgeMapper.selectCount(
                new LambdaQueryWrapper<AgentKnowledgePO>()
                        .eq(AgentKnowledgePO::getAgentId, agentId)
                        .eq(AgentKnowledgePO::getKnowledgeId, knowledgeId)) > 0;
    }

    private void validateKnowledgeExists(Long knowledgeId) {
        KnowledgeVO knowledge = knowledgeFacade.getById(knowledgeId);
        if (knowledge == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "知识库不存在: " + knowledgeId);
        }
    }
}
