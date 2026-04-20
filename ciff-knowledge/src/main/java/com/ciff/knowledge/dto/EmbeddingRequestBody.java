package com.ciff.knowledge.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Embedding API request body.
 */
@Data
@Builder
public class EmbeddingRequestBody {
    private String model;
    private List<String> input;
}
