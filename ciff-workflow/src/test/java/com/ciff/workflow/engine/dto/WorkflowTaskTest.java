package com.ciff.workflow.engine.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowTaskTest {

    @Test
    void builder_shouldCreateTask() {
        LocalDateTime now = LocalDateTime.now();
        WorkflowTask task = WorkflowTask.builder()
                .taskId("abc123")
                .workflowId(1L)
                .userId(100L)
                .status(WorkflowTask.TaskStatus.STARTED)
                .totalSteps(5)
                .completedSteps(0)
                .inputs(Map.of("userInput", "上海"))
                .startTime(now)
                .build();

        assertEquals("abc123", task.getTaskId());
        assertEquals(1L, task.getWorkflowId());
        assertEquals(100L, task.getUserId());
        assertEquals(WorkflowTask.TaskStatus.STARTED, task.getStatus());
        assertEquals(5, task.getTotalSteps());
        assertEquals(0, task.getCompletedSteps());
        assertEquals("上海", task.getInputs().get("userInput"));
        assertEquals(now, task.getStartTime());
    }

    @Test
    void taskStatus_shouldHaveAllValues() {
        WorkflowTask.TaskStatus[] statuses = WorkflowTask.TaskStatus.values();
        assertEquals(5, statuses.length);
        assertArrayEquals(new WorkflowTask.TaskStatus[]{
                WorkflowTask.TaskStatus.STARTED,
                WorkflowTask.TaskStatus.RUNNING,
                WorkflowTask.TaskStatus.SUCCESS,
                WorkflowTask.TaskStatus.FAILED,
                WorkflowTask.TaskStatus.TIMEOUT
        }, statuses);
    }
}
