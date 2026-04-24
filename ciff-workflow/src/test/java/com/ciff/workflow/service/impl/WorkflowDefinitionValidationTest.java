package com.ciff.workflow.service.impl;

import com.ciff.workflow.dto.StepDefinition;
import com.ciff.workflow.dto.WorkflowDefinition;
import com.ciff.workflow.exception.InvalidWorkflowDefinitionException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowDefinitionValidationTest {

    private final WorkflowServiceImpl service = new WorkflowServiceImpl(null, null, null, null);

    @Test
    void validate_nullDefinition_shouldThrow() {
        assertThrows(InvalidWorkflowDefinitionException.class,
                () -> service.validateDefinition(null));
    }

    @Test
    void validate_emptySteps_shouldThrow() {
        WorkflowDefinition def = new WorkflowDefinition();
        def.setSteps(List.of());
        assertThrows(InvalidWorkflowDefinitionException.class,
                () -> service.validateDefinition(def));
    }

    @Test
    void validate_duplicateStepId_shouldThrow() {
        WorkflowDefinition def = new WorkflowDefinition();
        def.setSteps(List.of(
                buildStep("s1", "llm", "S1"),
                buildStep("s1", "llm", "S1 duplicate")
        ));
        assertThrows(InvalidWorkflowDefinitionException.class,
                () -> service.validateDefinition(def));
    }

    @Test
    void validate_invalidStepType_shouldThrow() {
        WorkflowDefinition def = new WorkflowDefinition();
        def.setSteps(List.of(buildStep("s1", "invalid_type", "S1")));
        assertThrows(InvalidWorkflowDefinitionException.class,
                () -> service.validateDefinition(def));
    }

    @Test
    void validate_nextStepIdNotFound_shouldThrow() {
        WorkflowDefinition def = new WorkflowDefinition();
        StepDefinition step = buildStep("s1", "llm", "S1");
        step.setNextStepId("nonexistent");
        def.setSteps(List.of(step));
        assertThrows(InvalidWorkflowDefinitionException.class,
                () -> service.validateDefinition(def));
    }

    @Test
    void validate_dependsOnNotFound_shouldThrow() {
        WorkflowDefinition def = new WorkflowDefinition();
        StepDefinition step = buildStep("s1", "llm", "S1");
        step.setDependsOn(List.of("nonexistent"));
        def.setSteps(List.of(step));
        assertThrows(InvalidWorkflowDefinitionException.class,
                () -> service.validateDefinition(def));
    }

    @Test
    void validate_cycleDetected_shouldThrow() {
        WorkflowDefinition def = new WorkflowDefinition();
        def.setSteps(List.of(
                buildStepWithNext("s1", "llm", "S1", "s2"),
                buildStepWithNext("s2", "llm", "S2", "s3"),
                buildStepWithNext("s3", "llm", "S3", "s1")
        ));
        assertThrows(InvalidWorkflowDefinitionException.class,
                () -> service.validateDefinition(def));
    }

    @Test
    void validate_validDefinition_shouldPass() {
        WorkflowDefinition def = new WorkflowDefinition();
        def.setSteps(List.of(
                buildStepWithNext("s1", "llm", "S1", "s2"),
                buildStep("s2", "tool", "S2")
        ));
        assertDoesNotThrow(() -> service.validateDefinition(def));
    }

    private StepDefinition buildStep(String id, String type, String name) {
        StepDefinition step = new StepDefinition();
        step.setId(id);
        step.setType(type);
        step.setName(name);
        step.setConfig(Map.of());
        return step;
    }

    private StepDefinition buildStepWithNext(String id, String type, String name, String nextStepId) {
        StepDefinition step = buildStep(id, type, name);
        step.setNextStepId(nextStepId);
        return step;
    }
}
