package com.ciff.mcp.convertor;

import com.ciff.mcp.dto.ToolCreateRequest;
import com.ciff.mcp.dto.ToolUpdateRequest;
import com.ciff.mcp.dto.ToolVO;
import com.ciff.mcp.entity.ToolPO;

public final class ToolConvertor {

    private ToolConvertor() {
    }

    public static ToolPO toPO(ToolCreateRequest request) {
        ToolPO po = new ToolPO();
        po.setName(request.getName());
        po.setDescription(request.getDescription());
        po.setType(request.getType());
        po.setEndpoint(request.getEndpoint());
        po.setParamSchema(request.getParamSchema());
        po.setAuthConfig(request.getAuthConfig());
        po.setStatus("enabled");
        return po;
    }

    public static void updatePO(ToolPO po, ToolUpdateRequest request) {
        if (request.getName() != null) {
            po.setName(request.getName());
        }
        if (request.getDescription() != null) {
            po.setDescription(request.getDescription());
        }
        if (request.getType() != null) {
            po.setType(request.getType());
        }
        if (request.getEndpoint() != null) {
            po.setEndpoint(request.getEndpoint());
        }
        if (request.getParamSchema() != null) {
            po.setParamSchema(request.getParamSchema());
        }
        if (request.getAuthConfig() != null) {
            po.setAuthConfig(request.getAuthConfig());
        }
        if (request.getStatus() != null) {
            po.setStatus(request.getStatus());
        }
    }

    public static ToolVO toVO(ToolPO po) {
        ToolVO vo = new ToolVO();
        vo.setId(po.getId());
        vo.setName(po.getName());
        vo.setDescription(po.getDescription());
        vo.setType(po.getType());
        vo.setEndpoint(po.getEndpoint());
        vo.setParamSchema(po.getParamSchema());
        vo.setAuthConfig(po.getAuthConfig());
        vo.setStatus(po.getStatus());
        vo.setCreateTime(po.getCreateTime());
        vo.setUpdateTime(po.getUpdateTime());
        return vo;
    }
}
