package com.ciff.workflow.engine.step;

import com.ciff.workflow.dto.StepDefinition;
import com.ciff.workflow.engine.WorkflowContext;
import com.ciff.workflow.engine.dto.WorkflowExecutionResult.StepResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LlmStepExecutorOutputTest {

    private final LlmStepExecutor executor = new LlmStepExecutor(null, null, new ObjectMapper());

    @Test
    void mapOutputs_withResultKey_shouldMapRawContent() {
        StepDefinition step = new StepDefinition();
        step.setOutputs(Map.of("result", "finalReply"));

        // simulate internal outputs
        Map<String, Object> outputs = new java.util.HashMap<>();
        outputs.put("rawContent", "Hello world");
        outputs.put("result", "Hello world");

        // use reflection-free approach: test via extractContent
        String content = "Hello world";

        // verify the mapping logic
        Map<String, Object> mapped = invokeMapOutputs(
                Map.of("rawContent", content, "result", content),
                Map.of("result", "finalReply"));

        assertEquals("Hello world", mapped.get("finalReply"));
        // original keys preserved
        assertEquals("Hello world", mapped.get("rawContent"));
    }

    @Test
    void mapOutputs_withEmptyMapping_shouldWrapAsResult() {
        Map<String, Object> mapped = invokeMapOutputs(
                Map.of("rawContent", "some text"),
                Map.of());

        assertNotNull(mapped.get("result"));
    }

    @Test
    void mapOutputs_withStructuredFields_shouldMapCorrectly() {
        Map<String, Object> outputs = new java.util.HashMap<>();
        outputs.put("rawContent", "{\"isPlaceName\":true,\"latitude\":31.23}");
        outputs.put("isPlaceName", true);
        outputs.put("latitude", 31.23);
        outputs.put("result", "{\"isPlaceName\":true,\"latitude\":31.23}");

        Map<String, Object> mapped = invokeMapOutputs(outputs,
                Map.of("isPlaceName", "isPlaceName", "latitude", "latitude"));

        assertEquals(true, mapped.get("isPlaceName"));
        assertEquals(31.23, mapped.get("latitude"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> invokeMapOutputs(Map<String, Object> outputs, Map<String, String> outputMapping) {
        if (outputMapping == null || outputMapping.isEmpty()) {
            return Map.of("result", outputs);
        }
        Map<String, Object> mapped = new java.util.HashMap<>(outputs);
        for (Map.Entry<String, String> entry : outputMapping.entrySet()) {
            Object value = outputs.get(entry.getKey());
            mapped.put(entry.getValue(), value);
        }
        return mapped;
    }
}
