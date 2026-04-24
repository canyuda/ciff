package com.ciff.workflow.engine.dto;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowTaskDetailTest {

    @Test
    void builder_shouldCreateDetail() {
        WorkflowTaskDetail detail = WorkflowTaskDetail.builder()
                .taskId("task123")
                .workflowId(1L)
                .status(WorkflowTask.TaskStatus.RUNNING)
                .completedSteps(2)
                .totalSteps(5)
                .currentStepId("judge_weather")
                .currentStepName("判断天气类型")
                .inputs(Map.of("userInput", "上海"))
                .stepResults(new ArrayList<>())
                .startTime("2026-04-24T10:00:00")
                .build();

        assertEquals("task123", detail.getTaskId());
        assertEquals(WorkflowTask.TaskStatus.RUNNING, detail.getStatus());
        assertEquals(2, detail.getCompletedSteps());
        assertEquals("judge_weather", detail.getCurrentStepId());
        assertEquals("上海", detail.getInputs().get("userInput"));
    }
}
