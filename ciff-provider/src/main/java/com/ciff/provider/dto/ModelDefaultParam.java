package com.ciff.provider.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Model default parameters.
 * Stored as JSON in t_model.default_params column.
 */
@Data
public class ModelDefaultParam {

    /**
     * Controls randomness. Lower values = more deterministic.
     * Range: 0.0 ~ 2.0, default varies by provider.
     */
    private BigDecimal temperature;

    /**
     * Nucleus sampling threshold.
     * Range: 0.0 ~ 1.0.
     */
    private BigDecimal topP;

    /**
     * Maximum tokens for the completion.
     * Overrides the model-level maxTokens if set.
     */
    private Integer maxTokens;

    /**
     * Frequency penalty. Positive values reduce repetition.
     * Range: -2.0 ~ 2.0.
     */
    private BigDecimal frequencyPenalty;

    /**
     * presence penalty. positive values encourage new topics.
     * Range: -2.0 ~ 2.0.
     */
    private BigDecimal presencePenalty;
}
