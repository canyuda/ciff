package com.ciff.knowledge.service;

import com.ciff.knowledge.config.RerankProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Filters search results by confidence score.
 * Keeps top-1 result if all are below threshold.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchFilterService {

    private final RerankProperties rerankProperties;

    /**
     * Filter reranked entries by score threshold.
     * If all entries are below threshold, keeps the top-1 (highest score).
     *
     * @param entries reranked entries (must be ordered by relevance desc)
     * @return filtered entries
     */
    public List<RerankService.RerankEntry> filter(List<RerankService.RerankEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }

        double threshold = rerankProperties.getScoreThreshold();
        List<RerankService.RerankEntry> filtered = entries.stream()
                .filter(e -> e.relevanceScore() >= threshold)
                .toList();

        if (filtered.isEmpty()) {
            RerankService.RerankEntry top = entries.get(0);
            log.info("All results below threshold {}, keeping top-1 (score={})",
                    threshold, top.relevanceScore());
            return List.of(top);
        }

        return filtered;
    }
}
