package com.ciff.workflow.engine;

import com.ciff.workflow.dto.StepDefinition;
import com.ciff.workflow.dto.WorkflowDefinition;
import com.ciff.workflow.engine.dto.WorkflowExecutionResult;
import com.ciff.workflow.engine.dto.WorkflowExecutionResult.StepResult;
import com.ciff.workflow.engine.step.StepExecutor;
import com.ciff.workflow.exception.InvalidWorkflowDefinitionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class WorkflowEngine {

    private static final int MAX_STEPS = 20;

    private final Map<String, StepExecutor> executors;

    public WorkflowEngine(List<StepExecutor> executorList) {
        this.executors = new HashMap<>();
        for (StepExecutor executor : executorList) {
            String beanName = executor.getClass().getSimpleName();
            String type = beanName.replace("StepExecutor", "").toLowerCase();
            executors.put(type, executor);
        }
    }

    public WorkflowExecutionResult execute(WorkflowDefinition definition, Map<String, Object> inputs) {
        return execute(definition, inputs, null);
    }

    public WorkflowExecutionResult execute(WorkflowDefinition definition, Map<String, Object> inputs,
                                           StepCallback callback) {
        if (definition == null || definition.getSteps() == null || definition.getSteps().isEmpty()) {
            throw new InvalidWorkflowDefinitionException("Definition steps cannot be empty");
        }

        Map<String, StepDefinition> stepMap = definition.getSteps().stream()
                .collect(Collectors.toMap(StepDefinition::getId, Function.identity()));

        StepDefinition entry = definition.getSteps().stream()
                .filter(s -> s.getDependsOn() == null || s.getDependsOn().isEmpty())
                .findFirst()
                .orElseThrow(() -> new InvalidWorkflowDefinitionException("No entry step found"));

        int totalSteps = definition.getSteps().size();
        WorkflowContext context = new WorkflowContext(inputs);
        List<StepResult> stepResults = new ArrayList<>();

        StepDefinition current = entry;
        StepResult lastSuccessfulResult = null;
        int stepCount = 0;

        while (current != null && stepCount < MAX_STEPS) {
            stepCount++;
            StepExecutor executor = executors.get(current.getType());
            if (executor == null) {
                log.error("No executor for step type: {}", current.getType());
                StepResult errorResult = StepResult.builder()
                        .stepId(current.getId())
                        .stepName(current.getName())
                        .type(current.getType())
                        .success(false)
                        .error("Unknown step type: " + current.getType())
                        .build();
                stepResults.add(errorResult);
                break;
            }

            StepResult result = executor.execute(current, context);
            stepResults.add(result);
            context.setStepOutput(current.getId(), result.getOutputs());

            if (result.isSuccess()) {
                lastSuccessfulResult = result;
            } else {
                log.warn("Step {} failed: {}", current.getId(), result.getError());
            }

            if (callback != null) {
                callback.onStepComplete(current, result, stepResults, totalSteps);
            }

            if (!result.isSuccess()) {
                break;
            }

            current = resolveNextStep(current, result, stepMap);
        }

        if (stepCount >= MAX_STEPS) {
            log.warn("Workflow exceeded max steps limit: {}", MAX_STEPS);
        }

        Map<String, Object> finalOutputs = new HashMap<>();
        if (lastSuccessfulResult != null && lastSuccessfulResult.getOutputs() != null) {
            finalOutputs.putAll(lastSuccessfulResult.getOutputs());
        }

        return WorkflowExecutionResult.builder()
                .success(stepResults.stream().allMatch(StepResult::isSuccess))
                .stepResults(stepResults)
                .finalOutputs(finalOutputs)
                .build();
    }

    private StepDefinition resolveNextStep(StepDefinition current, StepResult result, Map<String, StepDefinition> stepMap) {
        if ("condition".equals(current.getType())) {
            Map<String, Object> outputs = result.getOutputs();
            if (outputs != null && outputs.containsKey("_nextStepId")) {
                String nextId = (String) outputs.get("_nextStepId");
                return nextId != null ? stepMap.get(nextId) : null;
            }
            return null;
        }

        String nextStepId = current.getNextStepId();
        return nextStepId != null ? stepMap.get(nextStepId) : null;
    }

    @FunctionalInterface
    public interface StepCallback {
        void onStepComplete(StepDefinition step, StepResult result,
                            List<StepResult> allResults, int totalSteps);
    }
}
