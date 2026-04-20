package com.ciff.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenUsage {

    private Integer promptTokens;

    private Integer completionTokens;

    public int totalTokens() {
        return (promptTokens != null ? promptTokens : 0)
                + (completionTokens != null ? completionTokens : 0);
    }
}
