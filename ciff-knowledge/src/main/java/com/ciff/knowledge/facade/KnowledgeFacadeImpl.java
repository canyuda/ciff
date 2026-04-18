package com.ciff.knowledge.facade;

import com.ciff.knowledge.convertor.KnowledgeConvertor;
import com.ciff.knowledge.dto.KnowledgeVO;
import com.ciff.knowledge.entity.KnowledgePO;
import com.ciff.knowledge.mapper.KnowledgeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KnowledgeFacadeImpl implements KnowledgeFacade {

    private final KnowledgeMapper knowledgeMapper;

    @Override
    public KnowledgeVO getById(Long id) {
        KnowledgePO po = knowledgeMapper.selectById(id);
        return po != null ? KnowledgeConvertor.toVO(po) : null;
    }
}
