package com.ciff.provider.llm;

import com.ciff.common.enums.AuthType;
import com.ciff.common.http.LlmHttpClient;
import com.ciff.common.util.ApiKeyEncryptor;
import com.ciff.provider.dto.ProviderAuthConfig;
import com.ciff.provider.entity.ProviderPO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
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
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", request.getModelName());
        root.put("stream", stream);
        root.put("max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : 4096);

        if (request.getTemperature() != null) {
            root.put("temperature", request.getTemperature());
        }

        // 分离 system 消息
        StringBuilder systemContent = new StringBuilder();
        ArrayNode messagesNode = root.putArray("messages");
        for (LlmChatRequest.Message msg : request.getMessages()) {
            if ("system".equals(msg.getRole())) {
                if (!systemContent.isEmpty()) {
                    systemContent.append("\n");
                }
                systemContent.append(msg.getContent());
            } else {
                ObjectNode msgNode = messagesNode.addObject();
                msgNode.put("role", msg.getRole());
                msgNode.put("content", msg.getContent());
            }
        }

        if (!systemContent.isEmpty()) {
            root.put("system", systemContent.toString());
        }

        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize request body", e);
        }
    }

    private LlmChatResponse parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            LlmChatResponse.Usage usage = null;
            if (root.has("usage") && !root.get("usage").isNull()) {
                JsonNode usageNode = root.get("usage");
                usage = LlmChatResponse.Usage.builder()
                        .promptTokens(nullableInt(usageNode, "input_tokens"))
                        .completionTokens(nullableInt(usageNode, "output_tokens"))
                        .build();
                if (usage.getPromptTokens() != null && usage.getCompletionTokens() != null) {
                    usage.setTotalTokens(usage.getPromptTokens() + usage.getCompletionTokens());
                }
            }

            String content = "";
            String finishReason = null;
            if (root.has("content") && root.get("content").isArray()) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode block : root.get("content")) {
                    if ("text".equals(block.path("type").asText())) {
                        sb.append(block.path("text").asText(""));
                    }
                }
                content = sb.toString();
            }
            if (root.has("stop_reason")) {
                finishReason = root.get("stop_reason").asText(null);
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
     * 解析 Claude SSE 流式 chunk。
     * Claude 格式:
     * - content_block_delta: {"type":"content_block_delta","delta":{"type":"text_delta","text":"xxx"}}
     */
    private String parseStreamChunk(String chunk) {
        try {
            JsonNode root = objectMapper.readTree(chunk);
            String type = root.path("type").asText("");

            if ("content_block_delta".equals(type)) {
                JsonNode delta = root.path("delta");
                if ("text_delta".equals(delta.path("type").asText())) {
                    return delta.path("text").asText(null);
                }
            }
            return null;
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private Integer nullableInt(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asInt() : null;
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
