package com.ciff.knowledge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ciff.chunk")
public class ChunkProperties {

    private int size = 700;

    private int overlap = 70;
}
