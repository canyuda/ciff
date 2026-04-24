package com.ciff.workflow.engine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTaskDetail {

    private String taskId;
    private Long workflowId;
    private WorkflowTask.TaskStatus status;
    private String currentStepId;
    private String currentStepName;
    private int completedSteps;
    private int totalSteps;

    private Map<String, Object> inputs;
    private List<WorkflowExecutionResult.StepResult> stepResults;
    private Map<String, Object> finalOutputs;
    private String error;

    private String startTime;
    private String endTime;
}
