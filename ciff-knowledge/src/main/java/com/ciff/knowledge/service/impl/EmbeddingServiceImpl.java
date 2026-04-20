package com.ciff.knowledge.service.impl;

import com.ciff.common.http.LlmHttpClient;
import com.ciff.knowledge.config.EmbeddingProperties;
import com.ciff.knowledge.dto.EmbeddingRequestBody;
import com.ciff.knowledge.dto.EmbeddingResponse;
import com.ciff.knowledge.service.EmbeddingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingServiceImpl implements EmbeddingService {

    private final LlmHttpClient llmHttpClient;
    private final EmbeddingProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public List<float[]> embed(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        // Batch processing to respect API limits
        List<float[]> allEmbeddings = new ArrayList<>(texts.size());
        int batchSize = properties.getBatchSize();

        for (int i = 0; i < texts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, texts.size());
            List<String> batch = texts.subList(i, end);
            List<float[]> batchResult = callEmbeddingApi(batch);
            allEmbeddings.addAll(batchResult);
        }

        return allEmbeddings;
    }

    private List<float[]> callEmbeddingApi(List<String> texts) {
        String url = properties.getBaseUrl() + "/v1/embeddings";

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + properties.getApiKey());

        String body = buildRequestBody(texts);
        log.info("Embedding API call: {} texts, model={}", texts.size(), properties.getModel());

        String response = llmHttpClient.post(url, headers, body);
        return parseResponse(response, texts.size());
    }

    private String buildRequestBody(List<String> texts) {
        EmbeddingRequestBody body = EmbeddingRequestBody.builder()
                .model(properties.getModel())
                .input(texts)
                .build();
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to build embedding request body", e);
        }
    }

    private List<float[]> parseResponse(String response, int expectedCount) {
        try {
            EmbeddingResponse resp = objectMapper.readValue(response, EmbeddingResponse.class);

            if (resp.getData() == null || resp.getData().isEmpty()) {
                throw new IllegalStateException("Embedding API response missing 'data' field");
            }

            // Sort by index to ensure correct order
            List<float[]> embeddings = new ArrayList<>(resp.getData().size());
            for (EmbeddingResponse.DataItem item : resp.getData()) {
                float[] vector = new float[item.getEmbedding().length];
                for (int i = 0; i < item.getEmbedding().length; i++) {
                    vector[i] = (float) item.getEmbedding()[i];
                }
                embeddings.add(item.getIndex(), vector);
            }

            if (embeddings.size() != expectedCount) {
                throw new IllegalStateException(
                        "Embedding count mismatch: expected " + expectedCount + ", got " + embeddings.size());
            }

            if (resp.getUsage() != null) {
                log.info("Embedding API usage: {} prompt_tokens", resp.getUsage().getPromptTokens());
            }

            return embeddings;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse embedding API response", e);
        }
    }
}
