package com.ciff.knowledge.service;

import com.ciff.common.http.LlmHttpClient;
import com.ciff.knowledge.config.EmbeddingProperties;
import com.ciff.knowledge.service.impl.EmbeddingServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

    @Mock
    private LlmHttpClient llmHttpClient;

    @Mock
    private EmbeddingProperties properties;

    private ObjectMapper objectMapper;
    private EmbeddingServiceImpl embeddingService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        embeddingService = new EmbeddingServiceImpl(llmHttpClient, properties, objectMapper);
    }

    @Test
    void embed_withSingleText_shouldReturnEmbedding() {
        stubProperties(2, "https://api.test.com", "test-key", "text-embedding-v3");
        String responseJson = """
                {
                    "data": [
                        {"index": 0, "embedding": [0.1, 0.2, 0.3]}
                    ],
                    "usage": {"prompt_tokens": 10}
                }
                """;
        when(llmHttpClient.post(eq("https://api.test.com/v1/embeddings"), any(), any()))
                .thenReturn(responseJson);

        List<float[]> result = embeddingService.embed(List.of("hello"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).containsExactly(0.1f, 0.2f, 0.3f);
    }

    @Test
    void embed_withMultipleTexts_shouldReturnEmbeddingsInOrder() {
        stubProperties(2, "https://api.test.com", "test-key", "text-embedding-v3");
        String responseJson = """
                {
                    "data": [
                        {"index": 0, "embedding": [0.1, 0.2]},
                        {"index": 1, "embedding": [0.3, 0.4]}
                    ]
                }
                """;
        when(llmHttpClient.post(eq("https://api.test.com/v1/embeddings"), any(), any()))
                .thenReturn(responseJson);

        List<float[]> result = embeddingService.embed(List.of("text1", "text2"));

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).containsExactly(0.1f, 0.2f);
        assertThat(result.get(1)).containsExactly(0.3f, 0.4f);
    }

    @Test
    void embed_withEmptyList_shouldReturnEmpty() {
        List<float[]> result = embeddingService.embed(List.of());

        assertThat(result).isEmpty();
    }

    @Test
    void embed_withNull_shouldReturnEmpty() {
        List<float[]> result = embeddingService.embed(null);

        assertThat(result).isEmpty();
    }

    @Test
    void embed_withBatchSizeLimit_shouldSplitIntoMultipleCalls() {
        stubProperties(2, "https://api.test.com", "test-key", "text-embedding-v3");
        String responseJson1 = """
                {"data": [
                    {"index": 0, "embedding": [0.1]},
                    {"index": 1, "embedding": [0.2]}
                ]}
                """;
        String responseJson2 = """
                {"data": [
                    {"index": 0, "embedding": [0.3]}
                ]}
                """;
        when(llmHttpClient.post(eq("https://api.test.com/v1/embeddings"), any(), any()))
                .thenReturn(responseJson1)
                .thenReturn(responseJson2);

        List<float[]> result = embeddingService.embed(List.of("a", "b", "c"));

        assertThat(result).hasSize(3);
        verify(llmHttpClient, times(2)).post(eq("https://api.test.com/v1/embeddings"), any(), any());
    }

    @Test
    void embed_whenResponseMissingData_shouldThrow() {
        stubProperties(2, "https://api.test.com", "test-key", "text-embedding-v3");
        String responseJson = "{\"data\": []}";
        when(llmHttpClient.post(eq("https://api.test.com/v1/embeddings"), any(), any()))
                .thenReturn(responseJson);

        assertThatThrownBy(() -> embeddingService.embed(List.of("hello")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing 'data' field");
    }

    @Test
    void embed_whenCountMismatch_shouldThrow() {
        stubProperties(2, "https://api.test.com", "test-key", "text-embedding-v3");
        String responseJson = """
                {"data": [
                    {"index": 0, "embedding": [0.1]}
                ]}
                """;
        when(llmHttpClient.post(eq("https://api.test.com/v1/embeddings"), any(), any()))
                .thenReturn(responseJson);

        assertThatThrownBy(() -> embeddingService.embed(List.of("hello", "world")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("count mismatch");
    }

    @Test
    void embed_whenApiThrows_shouldPropagate() {
        stubProperties(2, "https://api.test.com", "test-key", "text-embedding-v3");
        when(llmHttpClient.post(eq("https://api.test.com/v1/embeddings"), any(), any()))
                .thenThrow(new RuntimeException("API timeout"));

        assertThatThrownBy(() -> embeddingService.embed(List.of("hello")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("API timeout");
    }

    private void stubProperties(int batchSize, String baseUrl, String apiKey, String model) {
        when(properties.getBatchSize()).thenReturn(batchSize);
        when(properties.getBaseUrl()).thenReturn(baseUrl);
        when(properties.getApiKey()).thenReturn(apiKey);
        when(properties.getModel()).thenReturn(model);
    }
}
