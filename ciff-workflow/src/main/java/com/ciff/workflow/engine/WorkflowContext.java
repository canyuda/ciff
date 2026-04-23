package com.ciff.workflow.engine;

import java.util.HashMap;
import java.util.Map;

public class WorkflowContext {
    private final Map<String, Object> inputs;
    private final Map<String, Map<String, Object>> stepOutputs = new HashMap<>();

    public WorkflowContext(Map<String, Object> inputs) {
        this.inputs = inputs != null ? inputs : new HashMap<>();
    }

    public Object getInput(String key) {
        return inputs.get(key);
    }

    public Map<String, Object> getInputs() {
        return inputs;
    }

    public void setStepOutput(String stepId, Map<String, Object> outputs) {
        stepOutputs.put(stepId, outputs);
    }

    public Map<String, Object> getStepOutput(String stepId) {
        return stepOutputs.get(stepId);
    }

    public Object resolveVariable(String expression) {
        if (expression == null) return null;

        // ${inputs.xxx}
        if (expression.startsWith("${inputs.") && expression.endsWith("}")) {
            String key = expression.substring("${inputs.".length(), expression.length() - 1);
            return inputs.get(key);
        }

        // ${stepId.output.xxx}
        if (expression.startsWith("${") && expression.endsWith("}")) {
            String inner = expression.substring(2, expression.length() - 1);
            String[] parts = inner.split("\\.");
            if (parts.length >= 3 && "output".equals(parts[1])) {
                String stepId = parts[0];
                String field = parts[2];
                Map<String, Object> output = stepOutputs.get(stepId);
                if (output == null) return null;
                if (parts.length == 3) return output.get(field);
                // nested: stepId.output.a.b.c
                Object current = output.get(field);
                for (int i = 3; i < parts.length && current != null; i++) {
                    if (current instanceof Map) {
                        current = ((Map<String, Object>) current).get(parts[i]);
                    } else {
                        return null;
                    }
                }
                return current;
            }
        }

        return expression;
    }

    public String interpolate(String template) {
        if (template == null) return null;

        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < template.length()) {
            int start = template.indexOf("${", i);
            if (start == -1) {
                result.append(template.substring(i));
                break;
            }
            result.append(template.substring(i, start));
            int end = template.indexOf("}", start);
            if (end == -1) {
                result.append(template.substring(start));
                break;
            }
            String expr = template.substring(start, end + 1);
            Object value = resolveVariable(expr);
            result.append(value != null ? value.toString() : "");
            i = end + 1;
        }
        return result.toString();
    }
}
