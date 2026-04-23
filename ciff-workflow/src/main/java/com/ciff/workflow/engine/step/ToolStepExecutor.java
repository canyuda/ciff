package com.ciff.workflow.engine.step;

import com.ciff.mcp.dto.ToolVO;
import com.ciff.mcp.service.ToolService;
import com.ciff.workflow.dto.StepDefinition;
import com.ciff.workflow.engine.WorkflowContext;
import com.ciff.workflow.engine.dto.WorkflowExecutionResult.StepResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ToolStepExecutor implements StepExecutor {

    private static final int TOOL_TIMEOUT_SECONDS = 30;

    private final ToolService toolService;
    private final ObjectMapper objectMapper;
    private final WebClient webClient = WebClient.builder().build();

    @Override
    public StepResult execute(StepDefinition step, WorkflowContext context) {
        Map<String, Object> config = step.getConfig();
        Long toolId = toLong(config.get("toolId"));
        @SuppressWarnings("unchecked")
        Map<String, Object> params = config.get("params") instanceof Map
                ? (Map<String, Object>) config.get("params")
                : new HashMap<>();

        // interpolate params
        Map<String, Object> resolvedParams = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (entry.getValue() instanceof String s) {
                resolvedParams.put(entry.getKey(), context.interpolate(s));
            } else {
                resolvedParams.put(entry.getKey(), entry.getValue());
            }
        }

        ToolVO tool = toolService.getById(toolId);
        if (tool == null) {
            return StepResult.builder()
                    .stepId(step.getId())
                    .stepName(step.getName())
                    .type(step.getType())
                    .success(false)
                    .error("Tool not found: " + toolId)
                    .build();
        }

        // validate and filter params against paramSchema
        Map<String, Object> filteredParams = filterParamsBySchema(resolvedParams, tool.getParamSchema());

        try {
            String body = objectMapper.writeValueAsString(filteredParams);
            String responseBody = webClient.post()
                    .uri(tool.getEndpoint())
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(TOOL_TIMEOUT_SECONDS))
                    .block();

            Map<String, Object> resultData = objectMapper.readValue(
                    responseBody, new TypeReference<HashMap<String, Object>>() {});

            Map<String, Object> mappedOutputs = mapOutputs(resultData, step.getOutputs());

            return StepResult.builder()
                    .stepId(step.getId())
                    .stepName(step.getName())
                    .type(step.getType())
                    .success(true)
                    .outputs(mappedOutputs)
                    .build();
        } catch (Exception e) {
            log.error("Tool step {} failed", step.getId(), e);
            return StepResult.builder()
                    .stepId(step.getId())
                    .stepName(step.getName())
                    .type(step.getType())
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapOutputs(Map<String, Object> outputs, Map<String, String> outputMapping) {
        if (outputMapping == null || outputMapping.isEmpty()) {
            return Map.of("result", outputs);
        }
        Map<String, Object> mapped = new HashMap<>();
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> filterParamsBySchema(Map<String, Object> params, Map<String, Object> schema) {
        if (schema == null || schema.isEmpty()) {
            return params;
        }

        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        if (properties == null || properties.isEmpty()) {
            return params;
        }

        List<String> required = schema.get("required") instanceof List
                ? (List<String>) schema.get("required")
                : List.of();

        // check required params
        for (String req : required) {
            if (!params.containsKey(req) || params.get(req) == null) {
                throw new IllegalArgumentException("Missing required param: " + req);
            }
        }

        // filter: only send params defined in schema
        Map<String, Object> filtered = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (properties.containsKey(entry.getKey())) {
                filtered.put(entry.getKey(), coerceType(entry.getValue(), (Map<String, Object>) properties.get(entry.getKey())));
            }
        }
        return filtered;
    }

    @SuppressWarnings("unchecked")
    private Object coerceType(Object value, Map<String, Object> propDef) {
        if (value == null || propDef == null) return value;
        String type = (String) propDef.get("type");
        if (type == null) return value;

        return switch (type) {
            case "number" -> {
                if (value instanceof Number n) yield n.doubleValue();
                yield Double.parseDouble(value.toString());
            }
            case "integer" -> {
                if (value instanceof Number n) yield n.longValue();
                yield Long.parseLong(value.toString());
            }
            case "boolean" -> {
                if (value instanceof Boolean b) yield b;
                yield Boolean.parseBoolean(value.toString());
            }
            case "array" -> {
                if (value instanceof List<?> list) yield list;
                // single value -> wrap in list
                yield List.of(value);
            }
            default -> value; // string, object — pass through
        };
    }
}
