package com.ciff.workflow.engine.step;

import com.ciff.common.util.JsonUtil;
import com.ciff.provider.dto.LlmCallConfig;
import com.ciff.provider.entity.ProviderPO;
import com.ciff.provider.facade.ProviderFacade;
import com.ciff.provider.llm.LlmChatClient;
import com.ciff.provider.llm.LlmChatClientFactory;
import com.ciff.provider.llm.LlmChatRequest;
import com.ciff.provider.llm.LlmChatResponse;
import com.ciff.workflow.dto.StepDefinition;
import com.ciff.workflow.engine.WorkflowContext;
import com.ciff.workflow.engine.dto.WorkflowExecutionResult.StepResult;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class LlmStepExecutor implements StepExecutor {

    private final ProviderFacade providerFacade;
    private final LlmChatClientFactory llmChatClientFactory;

    @Override
    public StepResult execute(StepDefinition step, WorkflowContext context) {
        Map<String, Object> config = step.getConfig();
        Object modelIdRaw = config.get("modelId");
        if (modelIdRaw == null) {
            return StepResult.builder()
                    .stepId(step.getId()).stepName(step.getName()).type(step.getType())
                    .success(false).error("LLM step missing required config: modelId")
                    .build();
        }
        Long modelId = toLong(modelIdRaw);
        String systemPrompt = context.interpolate((String) config.getOrDefault("systemPrompt", ""));
        String userPrompt = context.interpolate((String) config.getOrDefault("userPrompt", ""));

        LlmCallConfig llmConfig = providerFacade.getLlmCallConfig(modelId);

        List<LlmChatRequest.Message> messages = new ArrayList<>();
        if (!systemPrompt.isEmpty()) {
            messages.add(LlmChatRequest.Message.builder()
                    .role("system")
                    .content(systemPrompt)
                    .build());
        }
        messages.add(LlmChatRequest.Message.builder()
                .role("user")
                .content(userPrompt)
                .build());

        try {
            ProviderPO provider = providerFacade.getProviderById(llmConfig.getProviderId());
            LlmChatClient client = llmChatClientFactory.create(provider);

            LlmChatRequest request = LlmChatRequest.builder()
                    .modelName(llmConfig.getModelName())
                    .messages(messages)
                    .maxTokens(llmConfig.getMaxTokens())
                    .build();

            LlmChatResponse response = client.chat(request);
            String content = response.getContent() != null ? response.getContent() : "";

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

    private void parseStructuredOutput(String content, Map<String, Object> outputs) {
        String trimmed = content.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                Map<String, Object> parsed = JsonUtil.fromJson(trimmed, new TypeReference<>() {});
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

    private Long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        if (value instanceof String s) return Long.parseLong(s);
        throw new IllegalArgumentException("Cannot convert to Long: " + value);
    }
}
