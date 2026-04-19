package com.ciff.knowledge.service.impl;

import com.ciff.common.http.LlmHttpClient;
import com.ciff.knowledge.config.EmbeddingProperties;
import com.ciff.knowledge.service.EmbeddingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", properties.getModel());
        ArrayNode inputArray = root.putArray("input");
        texts.forEach(inputArray::add);
        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to build embedding request body", e);
        }
    }

    private List<float[]> parseResponse(String response, int expectedCount) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode dataNode = root.get("data");
            if (dataNode == null || !dataNode.isArray()) {
                throw new IllegalStateException("Embedding API response missing 'data' field");
            }

            // Sort by index to ensure correct order
            List<float[]> embeddings = new ArrayList<>(dataNode.size());
            for (JsonNode item : dataNode) {
                int index = item.get("index").asInt();
                JsonNode embeddingNode = item.get("embedding");
                float[] vector = new float[embeddingNode.size()];
                for (int i = 0; i < embeddingNode.size(); i++) {
                    vector[i] = (float) embeddingNode.get(i).asDouble();
                }
                embeddings.add(index, vector);
            }

            if (embeddings.size() != expectedCount) {
                throw new IllegalStateException(
                        "Embedding count mismatch: expected " + expectedCount + ", got " + embeddings.size());
            }

            JsonNode usage = root.get("usage");
            if (usage != null) {
                log.info("Embedding API usage: {} prompt_tokens", usage.get("prompt_tokens").asInt());
            }

            return embeddings;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse embedding API response", e);
        }
    }
}
