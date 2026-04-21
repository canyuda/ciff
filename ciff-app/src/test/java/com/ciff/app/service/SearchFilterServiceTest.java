package com.ciff.app.service;

import com.ciff.knowledge.config.RerankProperties;
import com.ciff.knowledge.service.RerankService;
import com.ciff.knowledge.service.SearchFilterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchFilterServiceTest {

    @Mock
    private RerankProperties rerankProperties;

    @InjectMocks
    private SearchFilterService searchFilterService;

    private void stubRelativeDefaults(double ratio, double floor) {
        when(rerankProperties.getRelativeRatio()).thenReturn(ratio);
        when(rerankProperties.getMinAbsoluteFloor()).thenReturn(floor);
    }

    @Test
    void filterRelative_withNull_shouldReturnEmpty() {
        List<RerankService.RerankEntry> result = searchFilterService.filterRelative(null);
        assertThat(result).isEmpty();
    }

    @Test
    void filterRelative_withEmpty_shouldReturnEmpty() {
        List<RerankService.RerankEntry> result = searchFilterService.filterRelative(List.of());
        assertThat(result).isEmpty();
    }

    @Test
    void filterRelative_allAboveRelativeThreshold_shouldKeepAll() {
        stubRelativeDefaults(0.6, 0.05);
        // maxScore = 0.9, threshold = max(0.9 * 0.6, 0.05) = 0.54
        List<RerankService.RerankEntry> entries = List.of(
                new RerankService.RerankEntry(0, 0.9),
                new RerankService.RerankEntry(1, 0.7),
                new RerankService.RerankEntry(2, 0.6)
        );

        List<RerankService.RerankEntry> result = searchFilterService.filterRelative(entries);

        assertThat(result).hasSize(3);
    }

    @Test
    void filterRelative_someAboveRelativeThreshold_shouldKeepOnlyAbove() {
        stubRelativeDefaults(0.6, 0.05);
        // maxScore = 0.9, threshold = 0.54
        List<RerankService.RerankEntry> entries = List.of(
                new RerankService.RerankEntry(0, 0.9),
                new RerankService.RerankEntry(1, 0.5),
                new RerankService.RerankEntry(2, 0.1)
        );

        List<RerankService.RerankEntry> result = searchFilterService.filterRelative(entries);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).relevanceScore()).isEqualTo(0.9);
    }

    @Test
    void filterRelative_multiQueryLowScores_shouldKeepMultiple() {
        stubRelativeDefaults(0.6, 0.05);
        // maxScore = 0.25, threshold = max(0.25 * 0.6, 0.05) = 0.15
        // This is the key scenario: multi-query question leads to universally low scores
        List<RerankService.RerankEntry> entries = List.of(
                new RerankService.RerankEntry(0, 0.25),
                new RerankService.RerankEntry(1, 0.23),
                new RerankService.RerankEntry(2, 0.10)
        );

        List<RerankService.RerankEntry> result = searchFilterService.filterRelative(entries);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).relevanceScore()).isEqualTo(0.25);
        assertThat(result.get(1).relevanceScore()).isEqualTo(0.23);
    }

    @Test
    void filterRelative_allBelowAbsoluteFloor_shouldReturnEmpty() {
        stubRelativeDefaults(0.6, 0.1);
        // maxScore = 0.08, threshold = max(0.08 * 0.6, 0.1) = 0.1
        // All below absolute floor, so nothing kept
        List<RerankService.RerankEntry> entries = List.of(
                new RerankService.RerankEntry(0, 0.08),
                new RerankService.RerankEntry(1, 0.05)
        );

        List<RerankService.RerankEntry> result = searchFilterService.filterRelative(entries);

        assertThat(result).isEmpty();
    }
}
