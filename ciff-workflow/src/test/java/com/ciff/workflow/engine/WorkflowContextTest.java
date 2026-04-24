package com.ciff.workflow.engine;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowContextTest {

    @Test
    void interpolate_shouldResolveInputs() {
        WorkflowContext ctx = new WorkflowContext(Map.of("userInput", "上海"));

        String result = ctx.interpolate("你好 ${inputs.userInput}");

        assertEquals("你好 上海", result);
    }

    @Test
    void interpolate_shouldResolveStepOutput() {
        WorkflowContext ctx = new WorkflowContext(Map.of());
        ctx.setStepOutput("judge_place", Map.of("isPlaceName", true, "placeName", "上海"));

        String result = ctx.interpolate("${judge_place.output.placeName}");

        assertEquals("上海", result);
    }

    @Test
    void interpolate_shouldResolveNestedOutput() {
        WorkflowContext ctx = new WorkflowContext(Map.of());
        ctx.setStepOutput("get_weather", Map.of("result", Map.of("temp", 25, "status", "sunny")));

        Object value = ctx.resolveVariable("${get_weather.output.result.temp}");

        assertEquals(25, value);
    }

    @Test
    void interpolate_noVariables_shouldReturnOriginal() {
        WorkflowContext ctx = new WorkflowContext(Map.of());

        String result = ctx.interpolate("plain text");

        assertEquals("plain text", result);
    }

    @Test
    void interpolate_missingVariable_shouldEmptyString() {
        WorkflowContext ctx = new WorkflowContext(Map.of());

        String result = ctx.interpolate("hello ${inputs.missing}");

        assertEquals("hello ", result);
    }

    @Test
    void resolveVariable_shouldReturnNullForMissingStep() {
        WorkflowContext ctx = new WorkflowContext(Map.of());

        Object result = ctx.resolveVariable("${unknown.output.field}");

        assertNull(result);
    }
}
