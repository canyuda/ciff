package com.ciff.provider.llm;

import com.ciff.common.enums.AuthType;
import com.ciff.common.http.LlmHttpClient;
import com.ciff.common.util.ApiKeyEncryptor;
import com.ciff.common.util.JsonUtil;
import com.ciff.provider.dto.ProviderAuthConfig;
import com.ciff.provider.entity.ProviderPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Claude Messages API 客户端。
 * 使用 /v1/messages 接口，请求/响应格式与 OpenAI 不同。
 */
@Slf4j
@RequiredArgsConstructor
public class ClaudeClient implements LlmChatClient {

    private static final String DEFAULT_CHAT_PATH = "/v1/messages";
    private static final String MODELS_PATH = "/v1/models";

    private final ProviderPO provider;
    private final LlmHttpClient httpClient;
    private final ApiKeyEncryptor apiKeyEncryptor;

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

        // Accumulate tool use across streaming chunks
        String[] currentToolId = {null};
        String[] currentToolName = {null};
        StringBuilder currentToolArgs = new StringBuilder();

        httpClient.stream(provider.getName(), url, headers, body, chunk -> {
            try {
                ClaudeSseChunk sse = JsonUtil.fromJson(chunk, ClaudeSseChunk.class);
                if (sse == null) return;

                switch (sse.getType()) {
                    case "content_block_start" -> {
                        if (sse.getContentBlock() != null && "tool_use".equals(sse.getContentBlock().getType())) {
                            currentToolId[0] = sse.getContentBlock().getId();
                            currentToolName[0] = sse.getContentBlock().getName();
                            currentToolArgs.setLength(0);
                        }
                    }
                    case "content_block_delta" -> {
                        if (sse.getDelta() != null) {
                            if ("text_delta".equals(sse.getDelta().getType()) && sse.getDelta().getText() != null) {
                                callback.onToken(sse.getDelta().getText());
                            } else if ("input_json_delta".equals(sse.getDelta().getType())
                                    && sse.getDelta().getPartialJson() != null) {
                                currentToolArgs.append(sse.getDelta().getPartialJson());
                            }
                        }
                    }
                    case "content_block_stop" -> {
                        if (currentToolId[0] != null && currentToolName[0] != null) {
                            String args = currentToolArgs.isEmpty() ? "{}" : currentToolArgs.toString();
                            callback.onToolCall(currentToolId[0], currentToolName[0], args);
                            currentToolId[0] = null;
                            currentToolName[0] = null;
                            currentToolArgs.setLength(0);
                        }
                    }
                    default -> { /* message_start, message_delta, ping, etc. */ }
                }
            } catch (Exception e) {
                log.warn("Failed to parse Claude SSE chunk: {}", chunk, e);
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

        // Convert OpenAI tool format to Claude format: {name, description, input_schema}
        List<Object> claudeTools = null;
        if (request.getTools() != null && !request.getTools().isEmpty()) {
            claudeTools = request.getTools().stream().map(td -> {
                Map<String, Object> tool = new LinkedHashMap<>();
                tool.put("name", td.getFunction().getName());
                tool.put("description", td.getFunction().getDescription());
                tool.put("input_schema", td.getFunction().getParameters());
                return (Object) tool;
            }).toList();
        }

        ClaudeMessagesRequest body = ClaudeMessagesRequest.builder()
                .model(request.getModelName())
                .stream(stream)
                .maxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : 4096)
                .temperature(request.getTemperature())
                .system(systemContent.isEmpty() ? null : systemContent.toString())
                .messages(chatMessages)
                .tools(claudeTools)
                .build();
        return JsonUtil.toJsonSnakeCase(body);
    }

    private LlmChatResponse parseResponse(String responseBody) {
        try {
            ClaudeMessagesResponse resp = JsonUtil.fromJsonSnakeCase(responseBody, ClaudeMessagesResponse.class);

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
            List<LlmChatResponse.ToolCall> toolCalls = null;
            if (resp.getContent() != null) {
                StringBuilder sb = new StringBuilder();
                List<LlmChatResponse.ToolCall> tcList = new ArrayList<>();
                for (ClaudeMessagesResponse.ContentBlock block : resp.getContent()) {
                    if ("text".equals(block.getType())) {
                        sb.append(block.getText() != null ? block.getText() : "");
                    } else if ("tool_use".equals(block.getType())) {
                        // Parse tool_use block
                        String arguments = "{}";
                        if (block.getInput() != null) {
                            arguments = JsonUtil.toJson(block.getInput());
                        }
                        tcList.add(LlmChatResponse.ToolCall.builder()
                                .id(block.getId())
                                .type("function")
                                .function(LlmChatResponse.FunctionCall.builder()
                                        .name(block.getName())
                                        .arguments(arguments)
                                        .build())
                                .build());
                    }
                }
                content = sb.toString();
                if (!tcList.isEmpty()) {
                    toolCalls = tcList;
                }
            }

            return LlmChatResponse.builder()
                    .content(content)
                    .finishReason(resp.getStopReason())
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

    private String appendUrlAuth(String url) {
        if (provider.getAuthType() == AuthType.URL) {
            String apiKey = apiKeyEncryptor.decrypt(provider.getApiKeyEncrypted());
            String separator = url.contains("?") ? "&" : "?";
            return url + separator + "key=" + apiKey;
        }
        return url;
    }
}
