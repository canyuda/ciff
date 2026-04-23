package com.ciff.workflow.engine.step;

import com.ciff.workflow.dto.ConditionRule;
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
public class ConditionStepExecutor implements StepExecutor {

    private final ObjectMapper objectMapper;

    @Override
    public StepResult execute(StepDefinition step, WorkflowContext context) {
        Map<String, Object> config = step.getConfig();
        String fieldExpr = (String) config.get("field");

        // resolve field value from context
        Object fieldValue = context.resolveVariable(fieldExpr);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawRules = config.get("rules") instanceof List
                ? (List<Map<String, Object>>) config.get("rules")
                : List.of();

        List<ConditionRule> rules = rawRules.stream()
                .map(r -> objectMapper.convertValue(r, ConditionRule.class))
                .toList();

        String matchedStepId = null;
        for (ConditionRule rule : rules) {
            if ("default".equals(rule.getOperator())) {
                matchedStepId = rule.getNextStepId();
                break;
            }
            if (matchRule(rule, fieldValue)) {
                matchedStepId = rule.getNextStepId();
                break;
            }
        }

        Map<String, Object> outputs = new HashMap<>();
        if (matchedStepId != null) {
            outputs.put("_nextStepId", matchedStepId);
        }

        return StepResult.builder()
                .stepId(step.getId())
                .stepName(step.getName())
                .type(step.getType())
                .success(true)
                .outputs(outputs)
                .build();
    }

    private boolean matchRule(ConditionRule rule, Object actualValue) {
        if (actualValue == null) return false;
        Object expected = rule.getValue();
        String operator = rule.getOperator();

        return switch (operator) {
            case "eq" -> actualValue.toString().equals(expected != null ? expected.toString() : null);
            case "contains" -> actualValue.toString().contains(expected != null ? expected.toString() : "");
            case "gt" -> compareNumbers(actualValue, expected) > 0;
            case "gte" -> compareNumbers(actualValue, expected) >= 0;
            case "lt" -> compareNumbers(actualValue, expected) < 0;
            case "lte" -> compareNumbers(actualValue, expected) <= 0;
            case "ne" -> !actualValue.toString().equals(expected != null ? expected.toString() : null);
            default -> false;
        };
    }

    private int compareNumbers(Object a, Object b) {
        double da = toDouble(a);
        double db = toDouble(b);
        return Double.compare(da, db);
    }

    private double toDouble(Object value) {
        if (value instanceof Number n) return n.doubleValue();
        return Double.parseDouble(value.toString());
    }
}
