package com.ciff.provider.llm;

import com.ciff.common.enums.AuthType;
import com.ciff.common.enums.ProviderType;
import com.ciff.common.http.LlmHttpClient;
import com.ciff.common.util.ApiKeyEncryptor;
import com.ciff.provider.dto.ProviderAuthConfig;
import com.ciff.provider.entity.ProviderPO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * OpenAI 兼容客户端。
 * 覆盖 OpenAI、DeepSeek、Qwen、Ollama 等使用 /v1/chat/completions 接口的供应商。
 */
@RequiredArgsConstructor
public class OpenAiCompatibleClient implements LlmChatClient {

    private static final String DEFAULT_CHAT_PATH = "/v1/chat/completions";

    private final ProviderPO provider;
    private final LlmHttpClient httpClient;
    private final ApiKeyEncryptor apiKeyEncryptor;
    private final ObjectMapper objectMapper;

    @Override
    public void probe() {
        String probeUrl = buildProbeUrl();
        Map<String, String> headers = buildProbeHeaders();
        httpClient.get(provider.getName(), probeUrl, headers);
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

    private String buildUrl() {
        String baseUrl = normalizeBaseUrl(provider.getApiBaseUrl());
        String url = baseUrl + DEFAULT_CHAT_PATH;
        return appendUrlAuth(url);
    }

    /**
     * Build probe URL for connectivity check.
     * OpenAI-compatible: GET /v1/models; Ollama: GET /api/tags.
     */
    private String buildProbeUrl() {
        String baseUrl = normalizeBaseUrl(provider.getApiBaseUrl());
        String url;
        if (provider.getType() == ProviderType.OLLAMA) {
            url = baseUrl + "/api/tags";
        } else {
            url = baseUrl + "/v1/models";
        }
        return appendUrlAuth(url);
    }

    /**
     * 构建探测请求头。Ollama 无需认证。
     */
    private Map<String, String> buildProbeHeaders() {
        if (provider.getType() == ProviderType.OLLAMA) {
            return new HashMap<>();
        }
        return buildHeaders();
    }

    private String buildRequestBody(LlmChatRequest request, boolean stream) {
        OpenAiCompatibleRequest body = OpenAiCompatibleRequest.builder()
                .model(request.getModelName())
                .stream(stream)
                .temperature(request.getTemperature())
                .maxTokens(request.getMaxTokens())
                .messages(request.getMessages())
                .build();
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize request body", e);
        }
    }

    private LlmChatResponse parseResponse(String responseBody) {
        try {
            OpenAiCompletionResponse resp = objectMapper.readValue(responseBody, OpenAiCompletionResponse.class);

            LlmChatResponse.Usage usage = null;
            if (resp.getUsage() != null) {
                OpenAiCompletionResponse.Usage u = resp.getUsage();
                usage = LlmChatResponse.Usage.builder()
                        .promptTokens(u.getPromptTokens())
                        .completionTokens(u.getCompletionTokens())
                        .totalTokens(u.getTotalTokens())
                        .build();
            }

            String content = "";
            String finishReason = null;
            if (resp.getChoices() != null && !resp.getChoices().isEmpty()) {
                OpenAiCompletionResponse.Choice choice = resp.getChoices().get(0);
                finishReason = choice.getFinishReason();
                if (choice.getMessage() != null && choice.getMessage().getContent() != null) {
                    content = choice.getMessage().getContent();
                }
            }

            return LlmChatResponse.builder()
                    .content(content)
                    .finishReason(finishReason)
                    .usage(usage)
                    .build();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse response: " + responseBody, e);
        }
    }

    /**
     * 解析 SSE 流式 chunk，提取增量 content。
     * OpenAI 格式: {"choices":[{"delta":{"content":"xxx"}}]}
     */
    private String parseStreamChunk(String chunk) {
        try {
            OpenAiSseChunk sse = objectMapper.readValue(chunk, OpenAiSseChunk.class);
            if (sse.getChoices() != null && !sse.getChoices().isEmpty()) {
                OpenAiSseChunk.Choice choice = sse.getChoices().get(0);
                if (choice.getDelta() != null && choice.getDelta().getContent() != null) {
                    return choice.getDelta().getContent();
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

    /**
     * Append API key as URL query parameter for AuthType.URL.
     */
    private String appendUrlAuth(String url) {
        if (provider.getAuthType() == AuthType.URL) {
            String apiKey = apiKeyEncryptor.decrypt(provider.getApiKeyEncrypted());
            String separator = url.contains("?") ? "&" : "?";
            return url + separator + "key=" + apiKey;
        }
        return url;
    }
}
