package com.ciff.workflow.convertor;

import com.ciff.workflow.engine.dto.WorkflowExecutionResult;
import com.ciff.workflow.engine.dto.WorkflowTask;
import com.ciff.workflow.engine.dto.WorkflowTaskDetail;
import com.ciff.workflow.entity.WorkflowExecutionPO;
import com.ciff.workflow.entity.WorkflowNodeExecutionPO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowConvertorTest {

    @Test
    void toExecutionPO_withInputs() {
        Map<String, Object> inputs = Map.of("query", "hello");
        WorkflowExecutionPO po = WorkflowConvertor.toExecutionPO(1L, 2L, "task-123", inputs, 3);

        assertEquals("task-123", po.getTaskId());
        assertEquals(1L, po.getWorkflowId());
        assertEquals(2L, po.getUserId());
        assertEquals("STARTED", po.getStatus());
        assertEquals(0, po.getCompletedSteps());
        assertEquals(3, po.getTotalSteps());
        assertEquals("hello", po.getInputs().get("query"));
        assertNotNull(po.getStartTime());
    }

    @Test
    void toExecutionPO_nullInputs() {
        WorkflowExecutionPO po = WorkflowConvertor.toExecutionPO(1L, 2L, "task-123", null, 3);

        assertNotNull(po.getInputs());
        assertTrue(po.getInputs().isEmpty());
    }

    @Test
    void toTask_fullMapping() {
        WorkflowExecutionPO po = new WorkflowExecutionPO();
        po.setTaskId("task-123");
        po.setWorkflowId(1L);
        po.setUserId(2L);
        po.setStatus("RUNNING");
        po.setCurrentStepId("step-1");
        po.setCurrentStepName("LLM");
        po.setCompletedSteps(1);
        po.setTotalSteps(3);
        po.setInputs(Map.of("query", "hello"));
        po.setStartTime(LocalDateTime.of(2026, 4, 30, 10, 0));

        WorkflowTask task = WorkflowConvertor.toTask(po);

        assertEquals("task-123", task.getTaskId());
        assertEquals(1L, task.getWorkflowId());
        assertEquals(2L, task.getUserId());
        assertEquals(WorkflowTask.TaskStatus.RUNNING, task.getStatus());
        assertEquals("step-1", task.getCurrentStepId());
        assertEquals("LLM", task.getCurrentStepName());
        assertEquals(1, task.getCompletedSteps());
        assertEquals(3, task.getTotalSteps());
    }

    @Test
    void toTaskDetail_withNodeExecutions() {
        WorkflowExecutionPO po = new WorkflowExecutionPO();
        po.setTaskId("task-123");
        po.setWorkflowId(1L);
        po.setUserId(2L);
        po.setStatus("SUCCESS");
        po.setCompletedSteps(2);
        po.setTotalSteps(2);
        po.setFinalOutputs(Map.of("answer", "42"));
        po.setStartTime(LocalDateTime.of(2026, 4, 30, 10, 0));
        po.setEndTime(LocalDateTime.of(2026, 4, 30, 10, 1));

        WorkflowNodeExecutionPO node1 = new WorkflowNodeExecutionPO();
        node1.setStepId("s1");
        node1.setStepName("LLM");
        node1.setStepType("llm");
        node1.setStatus("SUCCESS");
        node1.setOutputs(Map.of("result", "hello"));

        WorkflowNodeExecutionPO node2 = new WorkflowNodeExecutionPO();
        node2.setStepId("s2");
        node2.setStepName("Condition");
        node2.setStepType("condition");
        node2.setStatus("SUCCESS");
        node2.setOutputs(Map.of("_nextStepId", "end"));

        WorkflowTaskDetail detail = WorkflowConvertor.toTaskDetail(po, List.of(node1, node2));

        assertEquals("task-123", detail.getTaskId());
        assertEquals(WorkflowTask.TaskStatus.SUCCESS, detail.getStatus());
        assertEquals(2, detail.getStepResults().size());
        assertEquals("s1", detail.getStepResults().get(0).getStepId());
        assertEquals("s2", detail.getStepResults().get(1).getStepId());
        assertEquals("42", detail.getFinalOutputs().get("answer"));
        assertNull(detail.getError());
    }

    @Test
    void toTaskDetail_withError() {
        WorkflowExecutionPO po = new WorkflowExecutionPO();
        po.setTaskId("task-456");
        po.setWorkflowId(1L);
        po.setUserId(2L);
        po.setStatus("FAILED");
        po.setCompletedSteps(1);
        po.setTotalSteps(2);
        po.setErrorMessage("LLM timeout");
        po.setStartTime(LocalDateTime.of(2026, 4, 30, 10, 0));
        po.setEndTime(LocalDateTime.of(2026, 4, 30, 10, 1));

        WorkflowTaskDetail detail = WorkflowConvertor.toTaskDetail(po, List.of());

        assertEquals(WorkflowTask.TaskStatus.FAILED, detail.getStatus());
        assertEquals("LLM timeout", detail.getError());
        assertTrue(detail.getStepResults().isEmpty());
    }

    @Test
    void toNodeExecutionPO_success() {
        WorkflowExecutionResult.StepResult stepResult = WorkflowExecutionResult.StepResult.builder()
                .stepId("s1")
                .stepName("LLM Call")
                .type("llm")
                .success(true)
                .outputs(Map.of("result", "hello"))
                .build();

        WorkflowNodeExecutionPO po = WorkflowConvertor.toNodeExecutionPO(100L, stepResult);

        assertEquals(100L, po.getExecutionId());
        assertEquals("s1", po.getStepId());
        assertEquals("LLM Call", po.getStepName());
        assertEquals("llm", po.getStepType());
        assertEquals("SUCCESS", po.getStatus());
        assertEquals("hello", po.getOutputs().get("result"));
        assertNull(po.getErrorMessage());
        assertEquals(0, po.getRetryCount());
    }

    @Test
    void toNodeExecutionPO_failure() {
        WorkflowExecutionResult.StepResult stepResult = WorkflowExecutionResult.StepResult.builder()
                .stepId("s1")
                .stepName("LLM Call")
                .type("llm")
                .success(false)
                .error("timeout")
                .build();

        WorkflowNodeExecutionPO po = WorkflowConvertor.toNodeExecutionPO(100L, stepResult);

        assertEquals("FAILED", po.getStatus());
        assertEquals("timeout", po.getErrorMessage());
    }
}
