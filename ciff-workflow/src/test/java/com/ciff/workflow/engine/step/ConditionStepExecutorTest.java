package com.ciff.workflow.engine.step;

import com.ciff.workflow.dto.StepDefinition;
import com.ciff.workflow.engine.WorkflowContext;
import com.ciff.workflow.engine.dto.WorkflowExecutionResult.StepResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConditionStepExecutorTest {

    private ConditionStepExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new ConditionStepExecutor(new ObjectMapper());
    }

    @Test
    void execute_eqMatch_shouldReturnCorrectNextStep() {
        WorkflowContext ctx = new WorkflowContext(Map.of());
        ctx.setStepOutput("judge", Map.of("isPlaceName", true));

        StepDefinition step = buildConditionStep("${judge.output.isPlaceName}",
                List.of(
                        Map.of("operator", "eq", "value", true, "nextStepId", "weather"),
                        Map.of("operator", "default", "nextStepId", "fallback")
                ));

        StepResult result = executor.execute(step, ctx);

        assertTrue(result.isSuccess());
        assertEquals("weather", result.getOutputs().get("_nextStepId"));
    }

    @Test
    void execute_noMatch_shouldFollowDefault() {
        WorkflowContext ctx = new WorkflowContext(Map.of());
        ctx.setStepOutput("judge", Map.of("isPlaceName", false));

        StepDefinition step = buildConditionStep("${judge.output.isPlaceName}",
                List.of(
                        Map.of("operator", "eq", "value", true, "nextStepId", "weather"),
                        Map.of("operator", "default", "nextStepId", "fallback")
                ));

        StepResult result = executor.execute(step, ctx);

        assertTrue(result.isSuccess());
        assertEquals("fallback", result.getOutputs().get("_nextStepId"));
    }

    @Test
    void execute_containsOperator_shouldMatch() {
        WorkflowContext ctx = new WorkflowContext(Map.of());
        ctx.setStepOutput("weather", Map.of("type", "大雨"));

        StepDefinition step = buildConditionStep("${weather.output.type}",
                List.of(
                        Map.of("operator", "contains", "value", "雨", "nextStepId", "rain"),
                        Map.of("operator", "default", "nextStepId", "sunny")
                ));

        StepResult result = executor.execute(step, ctx);

        assertEquals("rain", result.getOutputs().get("_nextStepId"));
    }

    @Test
    void execute_gtOperator_shouldMatch() {
        WorkflowContext ctx = new WorkflowContext(Map.of());
        ctx.setStepOutput("data", Map.of("score", 85));

        StepDefinition step = buildConditionStep("${data.output.score}",
                List.of(
                        Map.of("operator", "gt", "value", 80, "nextStepId", "high"),
                        Map.of("operator", "default", "nextStepId", "low")
                ));

        StepResult result = executor.execute(step, ctx);

        assertEquals("high", result.getOutputs().get("_nextStepId"));
    }

    private StepDefinition buildConditionStep(String field, List<Map<String, Object>> rules) {
        StepDefinition step = new StepDefinition();
        step.setId("branch");
        step.setType("condition");
        step.setName("Condition");
        step.setConfig(Map.of("field", field, "rules", rules));
        return step;
    }
}
