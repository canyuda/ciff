package com.ciff.workflow.engine.dto;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowExecutionResultTest {

    @Test
    void stepResult_builder_shouldCreateCorrectly() {
        WorkflowExecutionResult.StepResult step = WorkflowExecutionResult.StepResult.builder()
                .stepId("get_weather")
                .stepName("查询天气")
                .type("tool")
                .success(true)
                .outputs(Map.of("weatherData", Map.of("temp", 25)))
                .build();

        assertEquals("get_weather", step.getStepId());
        assertEquals("查询天气", step.getStepName());
        assertEquals("tool", step.getType());
        assertTrue(step.isSuccess());
        assertNotNull(step.getOutputs().get("weatherData"));
    }

    @Test
    void result_withStepResultsList_shouldMaintainOrder() {
        WorkflowExecutionResult.StepResult s1 = WorkflowExecutionResult.StepResult.builder()
                .stepId("step1").stepName("S1").type("llm").success(true).build();
        WorkflowExecutionResult.StepResult s2 = WorkflowExecutionResult.StepResult.builder()
                .stepId("step2").stepName("S2").type("tool").success(true).build();

        WorkflowExecutionResult result = WorkflowExecutionResult.builder()
                .success(true)
                .stepResults(List.of(s1, s2))
                .finalOutputs(Map.of("rawContent", "done"))
                .build();

        assertTrue(result.isSuccess());
        assertEquals(2, result.getStepResults().size());
        assertEquals("step1", result.getStepResults().get(0).getStepId());
        assertEquals("step2", result.getStepResults().get(1).getStepId());
        assertEquals("done", result.getFinalOutputs().get("rawContent"));
    }
}
