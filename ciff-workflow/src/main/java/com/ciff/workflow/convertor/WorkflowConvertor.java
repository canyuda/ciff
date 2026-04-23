package com.ciff.workflow.convertor;

import com.ciff.workflow.dto.WorkflowCreateRequest;
import com.ciff.workflow.dto.WorkflowUpdateRequest;
import com.ciff.workflow.dto.WorkflowVO;
import com.ciff.workflow.entity.WorkflowPO;

public final class WorkflowConvertor {
    private WorkflowConvertor() {}

    public static WorkflowPO toPO(WorkflowCreateRequest request, Long userId) {
        WorkflowPO po = new WorkflowPO();
        po.setUserId(userId);
        po.setName(request.getName());
        po.setDescription(request.getDescription() != null ? request.getDescription() : "");
        po.setDefinition(request.getDefinition());
        po.setStatus(request.getStatus());
        return po;
    }

    public static void updatePO(WorkflowPO po, WorkflowUpdateRequest request) {
        if (request.getName() != null) {
            po.setName(request.getName());
        }
        if (request.getDescription() != null) {
            po.setDescription(request.getDescription());
        }
        if (request.getDefinition() != null) {
            po.setDefinition(request.getDefinition());
        }
        if (request.getStatus() != null) {
            po.setStatus(request.getStatus());
        }
    }

    public static WorkflowVO toVO(WorkflowPO po) {
        WorkflowVO vo = new WorkflowVO();
        vo.setId(po.getId());
        vo.setName(po.getName());
        vo.setDescription(po.getDescription());
        vo.setDefinition(po.getDefinition());
        vo.setStatus(po.getStatus());
        vo.setCreateTime(po.getCreateTime());
        vo.setUpdateTime(po.getUpdateTime());
        return vo;
    }
}
