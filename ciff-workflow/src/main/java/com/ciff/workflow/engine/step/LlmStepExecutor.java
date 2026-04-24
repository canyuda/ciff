package com.ciff.workflow.engine.step;

import com.ciff.common.enums.ProviderType;
import com.ciff.common.http.LlmHttpClient;
import com.ciff.provider.dto.LlmCallConfig;
import com.ciff.provider.facade.ProviderFacade;
import com.ciff.workflow.dto.StepDefinition;
import com.ciff.workflow.engine.WorkflowContext;
import com.ciff.workflow.engine.dto.WorkflowExecutionResult.StepResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class LlmStepExecutor implements StepExecutor {

    private final ProviderFacade providerFacade;
    private final LlmHttpClient llmHttpClient;
    private final ObjectMapper objectMapper;

    @Override
    public StepResult execute(StepDefinition step, WorkflowContext context) {
        Map<String, Object> config = step.getConfig();
        Long modelId = toLong(config.get("modelId"));
        String systemPrompt = context.interpolate((String) config.getOrDefault("systemPrompt", ""));
        String userPrompt = context.interpolate((String) config.getOrDefault("userPrompt", ""));

        LlmCallConfig llmConfig = providerFacade.getLlmCallConfig(modelId);

        List<Map<String, String>> messages = new ArrayList<>();
        if (!systemPrompt.isEmpty()) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        messages.add(Map.of("role", "user", "content", userPrompt));

        String url = getChatEndpoint(llmConfig);
        Map<String, String> headers = buildHeaders(llmConfig);
        Map<String, Object> body = buildRequestBody(llmConfig, messages);

        try {
            String requestBody = objectMapper.writeValueAsString(body);
            String responseBody = llmHttpClient.post(llmConfig.getProviderName(), url, headers, requestBody);
            Map<String, Object> response = objectMapper.readValue(responseBody, new TypeReference<>() {});

            String content = extractContent(response);

            Map<String, Object> outputs = new HashMap<>();
            outputs.put("rawContent", content);
            parseStructuredOutput(content, outputs);

            // expose "result" as alias for rawContent (for output mapping like {"result": "finalReply"})
            outputs.put("result", content);

            Map<String, Object> mappedOutputs = mapOutputs(outputs, step.getOutputs());

            return StepResult.builder()
                    .stepId(step.getId())
                    .stepName(step.getName())
                    .type(step.getType())
                    .success(true)
                    .outputs(mappedOutputs)
                    .build();
        } catch (Exception e) {
            log.error("LLM step {} failed", step.getId(), e);
            return StepResult.builder()
                    .stepId(step.getId())
                    .stepName(step.getName())
                    .type(step.getType())
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }

    private String extractContent(Map<String, Object> response) {
        // OpenAI format: choices[0].message.content
        Object choices = response.get("choices");
        if (choices instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof Map<?, ?> choice) {
                Object message = choice.get("message");
                if (message instanceof Map<?, ?> msg) {
                    Object content = msg.get("content");
                    return content != null ? content.toString() : "";
                }
            }
        }
        // Claude format: content[0].text
        Object content = response.get("content");
        if (content instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof Map<?, ?> block) {
                Object text = block.get("text");
                return text != null ? text.toString() : "";
            }
        }
        return response.toString();
    }

    private void parseStructuredOutput(String content, Map<String, Object> outputs) {
        String trimmed = content.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                Map<String, Object> parsed = objectMapper.readValue(trimmed, new TypeReference<>() {});
                outputs.putAll(parsed);
            } catch (Exception ignored) {
                // not JSON, keep as raw content
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapOutputs(Map<String, Object> outputs, Map<String, String> outputMapping) {
        if (outputMapping == null || outputMapping.isEmpty()) {
            return Map.of("result", outputs);
        }
        // preserve original keys so inter-step references still work
        Map<String, Object> mapped = new HashMap<>(outputs);
        for (Map.Entry<String, String> entry : outputMapping.entrySet()) {
            Object value = outputs.get(entry.getKey());
            mapped.put(entry.getValue(), value);
        }
        return mapped;
    }

    private String getChatEndpoint(LlmCallConfig config) {
        String baseUrl = config.getApiBaseUrl().replaceAll("/+$", "");
        if (config.getProviderType() == ProviderType.CLAUDE) {
            return baseUrl + "/v1/messages";
        }
        return baseUrl + "/v1/chat/completions";
    }

    private Map<String, String> buildHeaders(LlmCallConfig config) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");
        if (config.getProviderType() == ProviderType.CLAUDE) {
            headers.put("x-api-key", config.getApiKey());
            headers.put("anthropic-version", "2023-06-01");
        } else {
            headers.put("Authorization", "Bearer " + config.getApiKey());
        }
        return headers;
    }

    private Map<String, Object> buildRequestBody(LlmCallConfig config, List<Map<String, String>> messages) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.getModelName());
        body.put("messages", messages);
        if (config.getMaxTokens() != null) {
            body.put("max_tokens", config.getMaxTokens());
        }
        return body;
    }

    private Long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        if (value instanceof String s) return Long.parseLong(s);
        throw new IllegalArgumentException("Cannot convert to Long: " + value);
    }
}
