package com.ciff.knowledge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ciff.embedding")
public class EmbeddingProperties {

    /** Base URL for Alibaba Cloud DashScope OpenAI-compatible API */
    private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode";

    private String apiKey;

    private String model = "text-embedding-v3";

    /** Max texts per embedding API call */
    private int batchSize = 10;
}
