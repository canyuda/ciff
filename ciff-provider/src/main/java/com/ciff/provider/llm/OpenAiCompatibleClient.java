package com.ciff.provider.llm;

import com.ciff.common.enums.AuthType;
import com.ciff.common.enums.ProviderType;
import com.ciff.common.http.LlmHttpClient;
import com.ciff.common.util.ApiKeyEncryptor;
import com.ciff.common.util.JsonUtil;
import com.ciff.provider.dto.ProviderAuthConfig;
import com.ciff.provider.entity.ProviderPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * OpenAI 兼容客户端。
 * 覆盖 OpenAI、DeepSeek、Qwen、Ollama 等使用 /v1/chat/completions 接口的供应商。
 */
@RequiredArgsConstructor
@Slf4j
public class OpenAiCompatibleClient implements LlmChatClient {

    private static final String DEFAULT_CHAT_PATH = "/v1/chat/completions";

    private final ProviderPO provider;
    private final LlmHttpClient httpClient;
    private final ApiKeyEncryptor apiKeyEncryptor;

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
        log.debug("LLM request body: {}", body);
        String response = httpClient.post(provider.getName(), url, headers, body);
        log.debug("LLM response body: {}", response);
        return parseResponse(response);
    }

    @Override
    public void streamChat(LlmChatRequest request, Consumer<String> callback) {
        streamChat(request, new StreamCallback() {
            @Override
            public void onToken(String content) {
                callback.accept(content);
            }
        });
    }

    @Override
    public void streamChat(LlmChatRequest request, StreamCallback callback) {
        String url = buildUrl();
        Map<String, String> headers = buildHeaders();
        String body = buildRequestBody(request, true);
        log.debug("LLM request body: {}", body);

        // Accumulate tool calls by index across streaming chunks
        Map<Integer, ToolCallAccumulator> toolCallAccumulators = new TreeMap<>();

        httpClient.stream(provider.getName(), url, headers, body, chunk -> {
            OpenAiSseChunk sse = parseStreamChunkRaw(chunk);
            if (sse == null || sse.getChoices() == null || sse.getChoices().isEmpty()) {
                return;
            }
            OpenAiSseChunk.Choice choice = sse.getChoices().get(0);

            // Text content
            if (choice.getDelta() != null && choice.getDelta().getContent() != null) {
                callback.onToken(choice.getDelta().getContent());
            }

            // Tool calls (incremental)
            if (choice.getDelta() != null && choice.getDelta().getToolCalls() != null) {
                for (OpenAiSseChunk.ToolCallDelta tc : choice.getDelta().getToolCalls()) {
                    int index = tc.getIndex() != null ? tc.getIndex() : 0;
                    ToolCallAccumulator acc = toolCallAccumulators.computeIfAbsent(index, k -> new ToolCallAccumulator());
                    if (tc.getId() != null) acc.id = tc.getId();
                    if (tc.getType() != null) acc.type = tc.getType();
                    if (tc.getFunction() != null) {
                        if (tc.getFunction().getName() != null) acc.name = tc.getFunction().getName();
                        if (tc.getFunction().getArguments() != null) acc.arguments.append(tc.getFunction().getArguments());
                    }
                }
            }

            // Stream finished with tool calls
            if ("tool_calls".equals(choice.getFinishReason())) {
                for (ToolCallAccumulator acc : toolCallAccumulators.values()) {
                    if (acc.id != null && acc.name != null) {
                        callback.onToolCall(acc.id, acc.name, acc.arguments.toString());
                    }
                }
            }
        });
    }

    private static class ToolCallAccumulator {
        String id;
        String type;
        String name;
        final StringBuilder arguments = new StringBuilder();
    }

    private OpenAiSseChunk parseStreamChunkRaw(String chunk) {
        try {
            return JsonUtil.fromJson(chunk, OpenAiSseChunk.class);
        } catch (Exception e) {
            log.warn("Failed to parse SSE chunk: {}", chunk, e);
            return null;
        }
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
                .tools(request.getTools())
                .build();
        return JsonUtil.toJson(body);
    }

    private LlmChatResponse parseResponse(String responseBody) {
        try {
            OpenAiCompletionResponse resp = JsonUtil.fromJsonSnakeCase(responseBody, OpenAiCompletionResponse.class);

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
            List<LlmChatResponse.ToolCall> toolCalls = null;
            if (resp.getChoices() != null && !resp.getChoices().isEmpty()) {
                OpenAiCompletionResponse.Choice choice = resp.getChoices().get(0);
                finishReason = choice.getFinishReason();
                if (choice.getMessage() != null) {
                    content = choice.getMessage().getContent() != null ? choice.getMessage().getContent() : "";
                    // Parse tool calls
                    if (choice.getMessage().getToolCalls() != null && !choice.getMessage().getToolCalls().isEmpty()) {
                        toolCalls = choice.getMessage().getToolCalls().stream()
                                .map(tc -> LlmChatResponse.ToolCall.builder()
                                        .id(tc.getId())
                                        .type(tc.getType())
                                        .function(LlmChatResponse.FunctionCall.builder()
                                                .name(tc.getFunction().getName())
                                                .arguments(tc.getFunction().getArguments())
                                                .build())
                                        .build())
                                .toList();
                    }
                }
            }

            return LlmChatResponse.builder()
                    .content(content)
                    .finishReason(finishReason)
                    .usage(usage)
                    .toolCalls(toolCalls)
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse response: " + responseBody, e);
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
