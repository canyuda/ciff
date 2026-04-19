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

    private void stubThreshold(double value) {
        when(rerankProperties.getScoreThreshold()).thenReturn(value);
    }

    @Test
    void filter_withNull_shouldReturnEmpty() {
        List<RerankService.RerankEntry> result = searchFilterService.filter(null);
        assertThat(result).isEmpty();
    }

    @Test
    void filter_withEmpty_shouldReturnEmpty() {
        List<RerankService.RerankEntry> result = searchFilterService.filter(List.of());
        assertThat(result).isEmpty();
    }

    @Test
    void filter_allAboveThreshold_shouldKeepAll() {
        stubThreshold(0.3);
        List<RerankService.RerankEntry> entries = List.of(
                new RerankService.RerankEntry(0, 0.9),
                new RerankService.RerankEntry(1, 0.5),
                new RerankService.RerankEntry(2, 0.4)
        );

        List<RerankService.RerankEntry> result = searchFilterService.filter(entries);

        assertThat(result).hasSize(3);
    }

    @Test
    void filter_someAboveThreshold_shouldKeepOnlyAbove() {
        stubThreshold(0.3);
        List<RerankService.RerankEntry> entries = List.of(
                new RerankService.RerankEntry(0, 0.9),
                new RerankService.RerankEntry(1, 0.2),
                new RerankService.RerankEntry(2, 0.1)
        );

        List<RerankService.RerankEntry> result = searchFilterService.filter(entries);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).relevanceScore()).isEqualTo(0.9);
    }

    @Test
    void filter_allBelowThreshold_shouldKeepTop1() {
        stubThreshold(0.3);
        List<RerankService.RerankEntry> entries = List.of(
                new RerankService.RerankEntry(0, 0.25),
                new RerankService.RerankEntry(1, 0.2),
                new RerankService.RerankEntry(2, 0.1)
        );

        List<RerankService.RerankEntry> result = searchFilterService.filter(entries);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).relevanceScore()).isEqualTo(0.25);
    }

    @Test
    void filter_exactlyAtThreshold_shouldKeep() {
        stubThreshold(0.3);
        List<RerankService.RerankEntry> entries = List.of(
                new RerankService.RerankEntry(0, 0.3),
                new RerankService.RerankEntry(1, 0.29)
        );

        List<RerankService.RerankEntry> result = searchFilterService.filter(entries);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).relevanceScore()).isEqualTo(0.3);
    }
}
