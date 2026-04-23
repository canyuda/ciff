package com.ciff.workflow.engine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowExecutionResult {
    private boolean success;
    private String error;
    private Map<String, StepResult> stepResults;
    private Map<String, Object> finalOutputs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepResult {
        private String stepId;
        private String stepName;
        private String type;
        private boolean success;
        private String error;
        private Map<String, Object> outputs;
    }
}
