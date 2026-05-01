package com.ciff.workflow.convertor;

import com.ciff.workflow.dto.WorkflowCreateRequest;
import com.ciff.workflow.dto.WorkflowDefinition;
import com.ciff.workflow.dto.WorkflowUpdateRequest;
import com.ciff.workflow.dto.WorkflowVO;
import com.ciff.workflow.engine.dto.WorkflowExecutionResult;
import com.ciff.workflow.engine.dto.WorkflowTask;
import com.ciff.workflow.engine.dto.WorkflowTaskDetail;
import com.ciff.workflow.entity.WorkflowExecutionPO;
import com.ciff.workflow.entity.WorkflowNodeExecutionPO;
import com.ciff.workflow.entity.WorkflowPO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class WorkflowConvertor {
    private WorkflowConvertor() {}

    public static WorkflowPO toPO(WorkflowCreateRequest request, Long userId) {
        WorkflowPO po = new WorkflowPO();
        po.setUserId(userId);
        po.setName(request.getName());
        po.setDescription(request.getDescription() != null ? request.getDescription() : "");
        po.setDefinition(request.getDefinition());
        po.setStatus(request.getStatus());
        return po;
    }

    public static void updatePO(WorkflowPO po, WorkflowUpdateRequest request) {
        if (request.getName() != null) {
            po.setName(request.getName());
        }
        if (request.getDescription() != null) {
            po.setDescription(request.getDescription());
        }
        if (request.getDefinition() != null) {
            po.setDefinition(request.getDefinition());
        }
        if (request.getStatus() != null) {
            po.setStatus(request.getStatus());
        }
    }

    public static WorkflowVO toVO(WorkflowPO po) {
        WorkflowVO vo = new WorkflowVO();
        vo.setId(po.getId());
        vo.setName(po.getName());
        vo.setDescription(po.getDescription());
        vo.setDefinition(po.getDefinition());
        vo.setStatus(po.getStatus());
        vo.setCreateTime(po.getCreateTime());
        vo.setUpdateTime(po.getUpdateTime());
        return vo;
    }

    public static WorkflowExecutionPO toExecutionPO(Long workflowId, Long userId, String taskId,
                                                     Map<String, Object> inputs, int totalSteps) {
        WorkflowExecutionPO po = new WorkflowExecutionPO();
        po.setTaskId(taskId);
        po.setWorkflowId(workflowId);
        po.setUserId(userId);
        po.setStatus("STARTED");
        po.setCompletedSteps(0);
        po.setTotalSteps(totalSteps);
        po.setInputs(inputs != null ? inputs : Map.of());
        po.setStartTime(LocalDateTime.now());
        return po;
    }

    public static WorkflowTask toTask(WorkflowExecutionPO po) {
        return WorkflowTask.builder()
                .taskId(po.getTaskId())
                .workflowId(po.getWorkflowId())
                .userId(po.getUserId())
                .status(WorkflowTask.TaskStatus.valueOf(po.getStatus()))
                .currentStepId(po.getCurrentStepId())
                .currentStepName(po.getCurrentStepName())
                .completedSteps(po.getCompletedSteps())
                .totalSteps(po.getTotalSteps())
                .inputs(po.getInputs())
                .startTime(po.getStartTime())
                .endTime(po.getEndTime())
                .build();
    }

    public static WorkflowTaskDetail toTaskDetail(WorkflowExecutionPO po,
                                                  List<WorkflowNodeExecutionPO> nodeExecutions) {
        List<WorkflowExecutionResult.StepResult> stepResults = nodeExecutions.stream()
                .map(WorkflowConvertor::toStepResult)
                .collect(Collectors.toList());

        return WorkflowTaskDetail.builder()
                .taskId(po.getTaskId())
                .workflowId(po.getWorkflowId())
                .status(WorkflowTask.TaskStatus.valueOf(po.getStatus()))
                .currentStepId(po.getCurrentStepId())
                .currentStepName(po.getCurrentStepName())
                .completedSteps(po.getCompletedSteps())
                .totalSteps(po.getTotalSteps())
                .inputs(po.getInputs())
                .stepResults(stepResults)
                .finalOutputs(po.getFinalOutputs())
                .error(po.getErrorMessage())
                .startTime(po.getStartTime() != null ? po.getStartTime().toString() : null)
                .endTime(po.getEndTime() != null ? po.getEndTime().toString() : null)
                .build();
    }

    public static WorkflowNodeExecutionPO toNodeExecutionPO(Long executionId,
                                                             WorkflowExecutionResult.StepResult stepResult) {
        WorkflowNodeExecutionPO po = new WorkflowNodeExecutionPO();
        po.setExecutionId(executionId);
        po.setStepId(stepResult.getStepId());
        po.setStepName(stepResult.getStepName());
        po.setStepType(stepResult.getType());
        po.setStatus(stepResult.isSuccess() ? "SUCCESS" : "FAILED");
        po.setOutputs(stepResult.getOutputs());
        po.setErrorMessage(stepResult.getError());
        po.setStartTime(LocalDateTime.now());
        po.setEndTime(LocalDateTime.now());
        po.setRetryCount(0);
        return po;
    }

    private static WorkflowExecutionResult.StepResult toStepResult(WorkflowNodeExecutionPO po) {
        return WorkflowExecutionResult.StepResult.builder()
                .stepId(po.getStepId())
                .stepName(po.getStepName())
                .type(po.getStepType())
                .success("SUCCESS".equals(po.getStatus()))
                .error(po.getErrorMessage())
                .outputs(po.getOutputs())
                .build();
    }
}
