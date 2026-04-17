package com.ciff.provider.llm;

import com.ciff.common.enums.ProviderType;
import com.ciff.common.http.LlmHttpClient;
import com.ciff.common.util.ApiKeyEncryptor;
import com.ciff.provider.entity.ProviderPO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 根据 Provider 类型创建对应的 LlmChatClient。
 */
@Component
@RequiredArgsConstructor
public class LlmChatClientFactory {

    private final LlmHttpClient httpClient;
    private final ApiKeyEncryptor apiKeyEncryptor;
    private final ObjectMapper objectMapper;

    /**
     * 创建 LlmChatClient。
     * OpenAI 兼容类型（OPENAI, DEEPSEEK, QWEN, OLLAMA 等）统一使用 OpenAiCompatibleClient。
     * Claude 使用 ClaudeClient。
     */
    public LlmChatClient create(ProviderPO provider) {
        if (provider.getType() == ProviderType.CLAUDE) {
            return new ClaudeClient(provider, httpClient, apiKeyEncryptor, objectMapper);
        }
        // 其他所有类型走 OpenAI 兼容接口
        return new OpenAiCompatibleClient(provider, httpClient, apiKeyEncryptor, objectMapper);
    }
}
