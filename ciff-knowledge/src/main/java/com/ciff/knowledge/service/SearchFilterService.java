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

    /**
     * Relative threshold filter.
     * Keeps entries whose score is within a ratio of the top score,
     * with an absolute floor to prevent keeping noise when all scores are low.
     *
     * <p>This is suitable for multi-query questions where reference documents
     * are scattered across chunks and each chunk only partially matches the query,
     * leading to universally low absolute scores.</p>
     *
     * @param entries reranked entries (must be ordered by relevance desc)
     * @param ratio   relative ratio to the max score (null = use config default)
     * @param floor   absolute minimum floor (null = use config default)
     * @return filtered entries
     */
    public List<RerankService.RerankEntry> filterRelative(
            List<RerankService.RerankEntry> entries, Double ratio, Double floor) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }

        double actualRatio = ratio != null ? ratio : rerankProperties.getRelativeRatio();
        double actualFloor = floor != null ? floor : rerankProperties.getMinAbsoluteFloor();

        double maxScore = entries.get(0).relevanceScore();
        double effectiveThreshold = Math.max(maxScore * actualRatio, actualFloor);

        List<RerankService.RerankEntry> filtered = entries.stream()
                .filter(e -> e.relevanceScore() >= effectiveThreshold)
                .toList();

        log.info("Relative filter: maxScore={}, ratio={}, floor={}, effectiveThreshold={}, kept={}/{}",
                maxScore, actualRatio, actualFloor, effectiveThreshold, filtered.size(), entries.size());

        return filtered;
    }

    /**
     * Relative threshold filter using configured defaults.
     */
    public List<RerankService.RerankEntry> filterRelative(List<RerankService.RerankEntry> entries) {
        return filterRelative(entries, null, null);
    }
}
