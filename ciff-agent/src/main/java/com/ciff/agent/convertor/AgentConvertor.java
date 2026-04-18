package com.ciff.agent.convertor;

import com.ciff.agent.dto.AgentCreateRequest;
import com.ciff.agent.dto.AgentUpdateRequest;
import com.ciff.agent.dto.AgentVO;
import com.ciff.agent.entity.AgentPO;

public final class AgentConvertor {

    private AgentConvertor() {
    }

    public static AgentPO toPO(AgentCreateRequest request, Long userId) {
        AgentPO po = new AgentPO();
        po.setUserId(userId);
        po.setName(request.getName());
        po.setDescription(request.getDescription());
        po.setType(request.getType());
        po.setModelId(request.getModelId());
        po.setWorkflowId(request.getWorkflowId());
        po.setSystemPrompt(request.getSystemPrompt());
        po.setModelParams(request.getModelParams());
        po.setFallbackModelId(request.getFallbackModelId());
        po.setStatus("draft");
        return po;
    }

    public static void updatePO(AgentPO po, AgentUpdateRequest request) {
        if (request.getName() != null) {
            po.setName(request.getName());
        }
        if (request.getDescription() != null) {
            po.setDescription(request.getDescription());
        }
        if (request.getType() != null) {
            po.setType(request.getType());
        }
        if (request.getModelId() != null) {
            po.setModelId(request.getModelId());
        }
        if (request.getWorkflowId() != null) {
            po.setWorkflowId(request.getWorkflowId());
        }
        if (request.getSystemPrompt() != null) {
            po.setSystemPrompt(request.getSystemPrompt());
        }
        if (request.getModelParams() != null) {
            po.setModelParams(request.getModelParams());
        }
        if (request.getFallbackModelId() != null) {
            po.setFallbackModelId(request.getFallbackModelId());
        }
        if (request.getStatus() != null) {
            po.setStatus(request.getStatus());
        }
    }

    public static AgentVO toVO(AgentPO po) {
        AgentVO vo = new AgentVO();
        vo.setId(po.getId());
        vo.setName(po.getName());
        vo.setDescription(po.getDescription());
        vo.setType(po.getType());
        vo.setModelId(po.getModelId());
        vo.setWorkflowId(po.getWorkflowId());
        vo.setSystemPrompt(po.getSystemPrompt());
        vo.setModelParams(po.getModelParams());
        vo.setFallbackModelId(po.getFallbackModelId());
        vo.setStatus(po.getStatus());
        vo.setCreateTime(po.getCreateTime());
        vo.setUpdateTime(po.getUpdateTime());
        return vo;
    }
}
