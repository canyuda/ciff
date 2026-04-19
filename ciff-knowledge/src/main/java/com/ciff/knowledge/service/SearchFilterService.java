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
     * @param entries   reranked entries (must be ordered by relevance desc)
     * @param threshold custom threshold (null = use config default)
     * @return filtered entries
     */
    public List<RerankService.RerankEntry> filter(List<RerankService.RerankEntry> entries, Double threshold) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }

        double actualThreshold = threshold != null ? threshold : rerankProperties.getScoreThreshold();
        List<RerankService.RerankEntry> filtered = entries.stream()
                .filter(e -> e.relevanceScore() >= actualThreshold)
                .toList();

        if (filtered.isEmpty()) {
            RerankService.RerankEntry top = entries.get(0);
            log.info("All results below threshold {}, keeping top-1 (score={})",
                    actualThreshold, top.relevanceScore());
            return List.of(top);
        }

        return filtered;
    }

    /**
     * Filter using configured default threshold.
     */
    public List<RerankService.RerankEntry> filter(List<RerankService.RerankEntry> entries) {
        return filter(entries, null);
    }

    /**
     * Strict filter without fallback.
     * Entries below threshold are simply dropped, even if all are below.
     */
    public List<RerankService.RerankEntry> filterStrict(List<RerankService.RerankEntry> entries, Double threshold) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        double actualThreshold = threshold != null ? threshold : rerankProperties.getScoreThreshold();
        return entries.stream()
                .filter(e -> e.relevanceScore() >= actualThreshold)
                .toList();
    }
}
