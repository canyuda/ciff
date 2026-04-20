package com.ciff.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Agent 模型参数")
public class AgentModelParam {

    private static final BigDecimal DEFAULT_TEMPERATURE = new BigDecimal("0.7");
    private static final int DEFAULT_MAX_TOKENS = 4096;
    private static final int DEFAULT_MAX_CONTEXT_TURNS = 5;

    @Schema(description = "温度，控制随机性。范围 0.0 ~ 2.0", example = "0.7")
    private BigDecimal temperature = DEFAULT_TEMPERATURE;

    @Schema(description = "最大输出 token 数", example = "4096")
    private Integer maxTokens = DEFAULT_MAX_TOKENS;

    @Schema(description = "最大上下文轮数（历史对话轮数限制）", example = "5")
    private Integer maxContextTurns = DEFAULT_MAX_CONTEXT_TURNS;
}
