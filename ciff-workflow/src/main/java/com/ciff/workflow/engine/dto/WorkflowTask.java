package com.ciff.workflow.engine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTask {

    private String taskId;
    private Long workflowId;
    private Long userId;
    private TaskStatus status;
    private String currentStepId;
    private String currentStepName;
    private int completedSteps;
    private int totalSteps;
    private Map<String, Object> inputs;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public enum TaskStatus {
        STARTED, RUNNING, SUCCESS, FAILED, TIMEOUT
    }
}
