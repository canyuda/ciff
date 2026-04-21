package com.ciff.knowledge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ciff.rerank")
public class RerankProperties {

    private String endpoint = "https://dashscope.aliyuncs.com/api/v1/services/rerank/text-rerank/text-rerank";

    private String apiKey;

    private String model = "gte-rerank-v2";

    private int timeoutMs = 800;

    private int topN = 5;

    /** Minimum relevance score threshold, chunks below this are filtered out */
    private double scoreThreshold = 0.3;

    /**
     * Relative threshold ratio.
     * When using relative filtering, a chunk is kept if its score >= maxScore * ratio.
     */
    private double relativeRatio = 0.6;

    /**
     * Minimum absolute floor for relative filtering.
     * Prevents keeping low scores when the top score itself is very low.
     */
    private double minAbsoluteFloor = 0.05;
}