package com.ciff.knowledge.facade;

import com.ciff.knowledge.convertor.KnowledgeConvertor;
import com.ciff.knowledge.dto.KnowledgeVO;
import com.ciff.knowledge.entity.KnowledgeChunkPO;
import com.ciff.knowledge.entity.KnowledgePO;
import com.ciff.knowledge.mapper.KnowledgeMapper;
import com.ciff.knowledge.service.EmbeddingService;
import com.ciff.knowledge.service.KnowledgeChunkService;
import com.ciff.knowledge.service.RerankService;
import com.ciff.knowledge.service.SearchFilterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeFacadeImpl implements KnowledgeFacade {

    private final KnowledgeMapper knowledgeMapper;
    private final EmbeddingService embeddingService;
    private final KnowledgeChunkService knowledgeChunkService;
    private final RerankService rerankService;
    private final SearchFilterService searchFilterService;

    @Override
    public KnowledgeVO getById(Long id) {
        KnowledgePO po = knowledgeMapper.selectById(id);
        return po != null ? KnowledgeConvertor.toVO(po) : null;
    }

    @Override
    public List<KnowledgeVO> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return knowledgeMapper.selectBatchIds(ids).stream()
                .map(KnowledgeConvertor::toVO)
                .toList();
    }

    @Override
    public List<KnowledgeChunkPO> retrieve(String query, List<Long> knowledgeIds, int topN) {
        // 1. Embed query
        float[] queryEmbedding = embeddingService.embed(List.of(query)).get(0);

        // 2. Vector search across all specified knowledge bases
        List<KnowledgeChunkPO> candidates;
        if (knowledgeIds != null && knowledgeIds.size() == 1) {
            candidates = knowledgeChunkService.search(queryEmbedding, knowledgeIds.get(0), topN * 3);
        } else if (knowledgeIds != null && !knowledgeIds.isEmpty()) {
            // Search each knowledge base and merge
            candidates = knowledgeIds.stream()
                    .flatMap(kid -> knowledgeChunkService.search(queryEmbedding, kid, topN * 2).stream())
                    .toList();
        } else {
            candidates = knowledgeChunkService.search(queryEmbedding, null, topN * 3);
        }

        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. Rerank
        List<String> texts = candidates.stream().map(KnowledgeChunkPO::getContent).toList();
        List<RerankService.RerankEntry> reranked = rerankService.rerank(query, texts, topN);

        // 4. Confidence filter
        List<RerankService.RerankEntry> filtered = searchFilterService.filter(reranked);

        // 5. Map back to chunks
        return filtered.stream()
                .map(r -> {
                    KnowledgeChunkPO chunk = candidates.get(r.index());
                    chunk.setSimilarity(r.relevanceScore());
                    return chunk;
                })
                .toList();
    }
}
