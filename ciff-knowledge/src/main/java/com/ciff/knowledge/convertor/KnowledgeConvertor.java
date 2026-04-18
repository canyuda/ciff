package com.ciff.knowledge.convertor;

import com.ciff.knowledge.dto.KnowledgeCreateRequest;
import com.ciff.knowledge.dto.KnowledgeUpdateRequest;
import com.ciff.knowledge.dto.KnowledgeVO;
import com.ciff.knowledge.entity.KnowledgePO;

public final class KnowledgeConvertor {

    private KnowledgeConvertor() {
    }

    public static KnowledgePO toPO(KnowledgeCreateRequest request, Long userId) {
        KnowledgePO po = new KnowledgePO();
        po.setUserId(userId);
        po.setName(request.getName());
        po.setDescription(request.getDescription() != null ? request.getDescription() : "");
        po.setChunkSize(request.getChunkSize() != null ? request.getChunkSize() : 500);
        po.setEmbeddingModel(request.getEmbeddingModel());
        po.setStatus("active");
        return po;
    }

    public static void updatePO(KnowledgePO po, KnowledgeUpdateRequest request) {
        if (request.getName() != null) {
            po.setName(request.getName());
        }
        if (request.getDescription() != null) {
            po.setDescription(request.getDescription());
        }
        if (request.getChunkSize() != null) {
            po.setChunkSize(request.getChunkSize());
        }
        if (request.getEmbeddingModel() != null) {
            po.setEmbeddingModel(request.getEmbeddingModel());
        }
        if (request.getStatus() != null) {
            po.setStatus(request.getStatus());
        }
    }

    public static KnowledgeVO toVO(KnowledgePO po) {
        KnowledgeVO vo = new KnowledgeVO();
        vo.setId(po.getId());
        vo.setName(po.getName());
        vo.setDescription(po.getDescription());
        vo.setChunkSize(po.getChunkSize());
        vo.setEmbeddingModel(po.getEmbeddingModel());
        vo.setStatus(po.getStatus());
        vo.setCreateTime(po.getCreateTime());
        vo.setUpdateTime(po.getUpdateTime());
        return vo;
    }
}
