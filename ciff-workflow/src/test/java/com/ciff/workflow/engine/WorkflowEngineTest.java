package com.ciff.workflow.engine;

import com.ciff.workflow.dto.StepDefinition;
import com.ciff.workflow.dto.WorkflowDefinition;
import com.ciff.workflow.engine.dto.WorkflowExecutionResult;
import com.ciff.workflow.engine.dto.WorkflowExecutionResult.StepResult;
import com.ciff.workflow.engine.step.StepExecutor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowEngineTest {

    // Named classes so getClass().getSimpleName() produces "LlmStepExecutor" -> "llm"
    static class LlmStepExecutor implements StepExecutor {
        private final String failStepId;

        LlmStepExecutor() { this(null); }
        LlmStepExecutor(String failStepId) { this.failStepId = failStepId; }

        @Override
        public StepResult execute(StepDefinition step, WorkflowContext context) {
            if (failStepId != null && failStepId.equals(step.getId())) {
                return StepResult.builder()
                        .stepId(step.getId()).stepName("Fail").type("llm")
                        .success(false).error("LLM error").build();
            }
            return StepResult.builder()
                    .stepId(step.getId())
                    .stepName(step.getName())
                    .type("llm")
                    .success(true)
                    .outputs(Map.of("rawContent", "result text", "result", "result text"))
                    .build();
        }
    }

    static class ConditionStepExecutor implements StepExecutor {
        @Override
        public StepResult execute(StepDefinition step, WorkflowContext context) {
            Map<String, Object> config = step.getConfig();
            Object field = context.resolveVariable((String) config.get("field"));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rules = (List<Map<String, Object>>) config.get("rules");
            String nextId = null;
            for (Map<String, Object> rule : rules) {
                if ("default".equals(rule.get("operator")) || field.toString().equals(String.valueOf(rule.get("value")))) {
                    nextId = (String) rule.get("nextStepId");
                    break;
                }
            }
            return StepResult.builder()
                    .stepId(step.getId())
                    .stepName(step.getName())
                    .type("condition")
                    .success(true)
                    .outputs(nextId != null ? Map.of("_nextStepId", nextId) : Map.of())
                    .build();
        }
    }

    private WorkflowEngine buildEngine() {
        return new WorkflowEngine(List.of(new LlmStepExecutor(), new ConditionStepExecutor()));
    }

    @Test
    void execute_linearFlow_shouldReturnAllSteps() {
        WorkflowDefinition def = new WorkflowDefinition();
        def.setSteps(List.of(
                buildStep("step1", "llm", "Step 1", null, "step2", null),
                buildStep("step2", "llm", "Step 2", null, null, null)
        ));

        WorkflowExecutionResult result = buildEngine().execute(def, Map.of());

        assertTrue(result.isSuccess());
        assertEquals(2, result.getStepResults().size());
        assertEquals("step1", result.getStepResults().get(0).getStepId());
        assertEquals("step2", result.getStepResults().get(1).getStepId());
    }

    @Test
    void execute_withCondition_shouldFollowBranch() {
        WorkflowDefinition def = new WorkflowDefinition();
        def.setSteps(List.of(
                buildStep("judge", "llm", "Judge", null, "branch", null),
                buildConditionStep("branch", "Branch", "${judge.output.rawContent}",
                        List.of(
                                Map.of("operator", "eq", "value", "result text", "nextStepId", "path_a"),
                                Map.of("operator", "default", "nextStepId", "path_b")
                        )),
                buildStep("path_a", "llm", "Path A", null, null, null),
                buildStep("path_b", "llm", "Path B", null, null, null)
        ));

        WorkflowExecutionResult result = buildEngine().execute(def, Map.of());

        assertTrue(result.isSuccess());
        assertEquals(3, result.getStepResults().size());
        assertEquals("path_a", result.getStepResults().get(2).getStepId());
    }

    @Test
    void execute_emptySteps_shouldThrow() {
        WorkflowDefinition def = new WorkflowDefinition();
        def.setSteps(List.of());

        assertThrows(Exception.class, () -> buildEngine().execute(def, Map.of()));
    }

    @Test
    void execute_stepFailure_shouldStopAndReport() {
        WorkflowEngine failEngine = new WorkflowEngine(List.of(new LlmStepExecutor("fail_step")));

        WorkflowDefinition def = new WorkflowDefinition();
        def.setSteps(List.of(
                buildStep("step1", "llm", "Step 1", null, "fail_step", null),
                buildStep("fail_step", "llm", "Fail Step", null, "step3", null),
                buildStep("step3", "llm", "Step 3", null, null, null)
        ));

        WorkflowExecutionResult result = failEngine.execute(def, Map.of());

        assertFalse(result.isSuccess());
        assertEquals(2, result.getStepResults().size());
        assertFalse(result.getStepResults().get(1).isSuccess());
    }

    @Test
    void execute_withCallback_shouldFirePerStep() {
        StringBuilder log = new StringBuilder();

        WorkflowDefinition def = new WorkflowDefinition();
        def.setSteps(List.of(
                buildStep("s1", "llm", "S1", null, "s2", null),
                buildStep("s2", "llm", "S2", null, null, null)
        ));

        buildEngine().execute(def, Map.of(), (step, stepResult, allResults, total) -> {
            log.append(step.getId()).append(" ");
        });

        assertEquals("s1 s2 ", log.toString());
    }

    private StepDefinition buildStep(String id, String type, String name,
                                      List<String> dependsOn, String nextStepId,
                                      Map<String, String> outputs) {
        StepDefinition step = new StepDefinition();
        step.setId(id);
        step.setType(type);
        step.setName(name);
        step.setDependsOn(dependsOn);
        step.setNextStepId(nextStepId);
        step.setOutputs(outputs);
        step.setConfig(Map.of());
        return step;
    }

    private StepDefinition buildConditionStep(String id, String name, String field,
                                               List<Map<String, Object>> rules) {
        StepDefinition step = new StepDefinition();
        step.setId(id);
        step.setType("condition");
        step.setName(name);
        step.setConfig(Map.of("field", field, "rules", rules));
        return step;
    }
}
