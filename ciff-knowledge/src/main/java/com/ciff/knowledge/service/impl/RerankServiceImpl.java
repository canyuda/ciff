package com.ciff.knowledge.service.impl;

import com.ciff.common.http.LlmHttpClient;
import com.ciff.knowledge.config.RerankProperties;
import com.ciff.knowledge.service.RerankService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RerankServiceImpl implements RerankService {

    private final LlmHttpClient llmHttpClient;
    private final RerankProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public List<RerankEntry> rerank(String query, List<String> documents, int topN) {
        if (query == null || query.isBlank() || documents == null || documents.isEmpty()) {
            return List.of();
        }

        RerankRequest request = new RerankRequest(
                properties.getModel(),
                new RerankInput(query, documents),
                new RerankParams(topN, false)
        );

        String body = writeValue(request);
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + properties.getApiKey());

        long start = System.currentTimeMillis();
        String response = llmHttpClient.post(properties.getEndpoint(), headers, body);
        long elapsed = System.currentTimeMillis() - start;

        if (elapsed > properties.getTimeoutMs()) {
            log.warn("Rerank API slow: {}ms > {}ms threshold", elapsed, properties.getTimeoutMs());
        } else {
            log.info("Rerank API completed in {}ms, docs={}", elapsed, documents.size());
        }

        RerankResponse rerankResponse = readValue(response);

        List<ApiRerankResult> apiResults = rerankResponse.getOutput().getResults();
        if (apiResults == null || apiResults.isEmpty()) {
            return List.of();
        }

        // Convert to RerankEntry
        return apiResults.stream()
                .map(r -> new RerankEntry(r.getIndex(), r.getRelevanceScore()))
                .toList();
    }

    private String writeValue(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize rerank request", e);
        }
    }

    private RerankResponse readValue(String json) {
        try {
            return objectMapper.readValue(json, RerankResponse.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse rerank response", e);
        }
    }

    // ---- DTOs ----

    @Data
    static class RerankRequest {
        private final String model;
        private final RerankInput input;
        private final RerankParams parameters;

        RerankRequest(String model, RerankInput input, RerankParams parameters) {
            this.model = model;
            this.input = input;
            this.parameters = parameters;
        }
    }

    @Data
    static class RerankInput {
        private final String query;
        private final List<String> documents;

        RerankInput(String query, List<String> documents) {
            this.query = query;
            this.documents = documents;
        }
    }

    @Data
    static class RerankParams {
        @JsonProperty("top_n")
        private final int topN;
        @JsonProperty("return_documents")
        private final boolean returnDocuments;

        RerankParams(int topN, boolean returnDocuments) {
            this.topN = topN;
            this.returnDocuments = returnDocuments;
        }
    }

    @Data
    static class RerankResponse {
        private RerankOutput output;
        private RerankUsage usage;
    }

    @Data
    static class RerankOutput {
        private List<ApiRerankResult> results;
    }

    @Data
    static class ApiRerankResult {
        private int index;
        @JsonProperty("relevance_score")
        private double relevanceScore;
    }

    @Data
    static class RerankUsage {
        @JsonProperty("total_tokens")
        private int totalTokens;
    }
}