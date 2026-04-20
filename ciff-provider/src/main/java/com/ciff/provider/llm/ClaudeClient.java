package com.ciff.provider.llm;

import com.ciff.common.enums.AuthType;
import com.ciff.common.http.LlmHttpClient;
import com.ciff.common.util.ApiKeyEncryptor;
import com.ciff.provider.dto.ProviderAuthConfig;
import com.ciff.provider.entity.ProviderPO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Claude Messages API 客户端。
 * 使用 /v1/messages 接口，请求/响应格式与 OpenAI 不同。
 */
@RequiredArgsConstructor
public class ClaudeClient implements LlmChatClient {

    private static final String DEFAULT_CHAT_PATH = "/v1/messages";
    private static final String MODELS_PATH = "/v1/models";

    private final ProviderPO provider;
    private final LlmHttpClient httpClient;
    private final ApiKeyEncryptor apiKeyEncryptor;
    private final ObjectMapper objectMapper;

    @Override
    public void probe() {
        String url = buildModelsUrl();
        Map<String, String> headers = buildHeaders();
        httpClient.get(provider.getName(), url, headers);
    }

    @Override
    public LlmChatResponse chat(LlmChatRequest request) {
        String url = buildUrl();
        Map<String, String> headers = buildHeaders();
        String body = buildRequestBody(request, false);

        String response = httpClient.post(provider.getName(), url, headers, body);
        return parseResponse(response);
    }

    @Override
    public void streamChat(LlmChatRequest request, Consumer<String> callback) {
        String url = buildUrl();
        Map<String, String> headers = buildHeaders();
        String body = buildRequestBody(request, true);

        httpClient.stream(provider.getName(), url, headers, body, chunk -> {
            String content = parseStreamChunk(chunk);
            if (content != null) {
                callback.accept(content);
            }
        });
    }

    private String buildUrl() {
        String url = normalizeBaseUrl(provider.getApiBaseUrl()) + DEFAULT_CHAT_PATH;
        return appendUrlAuth(url);
    }

    private String buildModelsUrl() {
        String url = normalizeBaseUrl(provider.getApiBaseUrl()) + MODELS_PATH;
        return appendUrlAuth(url);
    }

    private Map<String, String> buildHeaders() {
        Map<String, String> headers = new HashMap<>();
        String apiKey = apiKeyEncryptor.decrypt(provider.getApiKeyEncrypted());

        switch (provider.getAuthType()) {
            case BEARER -> headers.put("Authorization", "Bearer " + apiKey);
            case API_KEY_HEADER -> {
                headers.put("x-api-key", apiKey);
                ProviderAuthConfig config = provider.getAuthConfig();
                if (config != null && config.getApiVersion() != null) {
                    headers.put("anthropic-version", config.getApiVersion());
                }
            }
            case URL -> { /* API key appended in buildUrl() */ }
            case JWT, DUAL_KEY -> throw new UnsupportedOperationException(
                    "AuthType " + provider.getAuthType() + " not yet supported for provider: " + provider.getName());
        }
        return headers;
    }

    /**
     * Claude Messages API 请求格式：
     * - messages 不含 system role，system 单独提出
     * - max_tokens 必填
     */
    private String buildRequestBody(LlmChatRequest request, boolean stream) {
        // Separate system messages from conversation messages
        StringBuilder systemContent = new StringBuilder();
        List<LlmChatRequest.Message> chatMessages = new ArrayList<>();
        for (LlmChatRequest.Message msg : request.getMessages()) {
            if ("system".equals(msg.getRole())) {
                if (!systemContent.isEmpty()) {
                    systemContent.append("\n");
                }
                systemContent.append(msg.getContent());
            } else {
                chatMessages.add(msg);
            }
        }

        ClaudeMessagesRequest body = ClaudeMessagesRequest.builder()
                .model(request.getModelName())
                .stream(stream)
                .maxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : 4096)
                .temperature(request.getTemperature())
                .system(systemContent.isEmpty() ? null : systemContent.toString())
                .messages(chatMessages)
                .build();
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize request body", e);
        }
    }

    private LlmChatResponse parseResponse(String responseBody) {
        try {
            ClaudeMessagesResponse resp = objectMapper.readValue(responseBody, ClaudeMessagesResponse.class);

            LlmChatResponse.Usage usage = null;
            if (resp.getUsage() != null) {
                ClaudeMessagesResponse.Usage u = resp.getUsage();
                usage = LlmChatResponse.Usage.builder()
                        .promptTokens(u.getInputTokens())
                        .completionTokens(u.getOutputTokens())
                        .build();
                if (usage.getPromptTokens() != null && usage.getCompletionTokens() != null) {
                    usage.setTotalTokens(usage.getPromptTokens() + usage.getCompletionTokens());
                }
            }

            String content = "";
            if (resp.getContent() != null) {
                StringBuilder sb = new StringBuilder();
                for (ClaudeMessagesResponse.ContentBlock block : resp.getContent()) {
                    if ("text".equals(block.getType())) {
                        sb.append(block.getText() != null ? block.getText() : "");
                    }
                }
                content = sb.toString();
            }

            return LlmChatResponse.builder()
                    .content(content)
                    .finishReason(resp.getStopReason())
                    .usage(usage)
                    .build();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse response: " + responseBody, e);
        }
    }

    /**
     * 解析 Claude SSE 流式 chunk。
     * Claude 格式:
     * - content_block_delta: {"type":"content_block_delta","delta":{"type":"text_delta","text":"xxx"}}
     */
    private String parseStreamChunk(String chunk) {
        try {
            ClaudeSseChunk sse = objectMapper.readValue(chunk, ClaudeSseChunk.class);
            if ("content_block_delta".equals(sse.getType()) && sse.getDelta() != null) {
                if ("text_delta".equals(sse.getDelta().getType())) {
                    return sse.getDelta().getText();
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }

    private String appendUrlAuth(String url) {
        if (provider.getAuthType() == AuthType.URL) {
            String apiKey = apiKeyEncryptor.decrypt(provider.getApiKeyEncrypted());
            String separator = url.contains("?") ? "&" : "?";
            return url + separator + "key=" + apiKey;
        }
        return url;
    }
}
