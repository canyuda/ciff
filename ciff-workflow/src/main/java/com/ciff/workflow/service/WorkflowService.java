package com.ciff.workflow.service;

import com.ciff.common.dto.PageResult;
import com.ciff.workflow.dto.WorkflowCreateRequest;
import com.ciff.workflow.dto.WorkflowDefinition;
import com.ciff.workflow.dto.WorkflowUpdateRequest;
import com.ciff.workflow.dto.WorkflowVO;
import com.ciff.workflow.engine.dto.WorkflowTask;
import com.ciff.workflow.engine.dto.WorkflowTaskDetail;

import java.util.List;
import java.util.Map;

public interface WorkflowService {
    WorkflowVO create(WorkflowCreateRequest request, Long userId);
    WorkflowVO update(Long id, WorkflowUpdateRequest request, Long userId);
    WorkflowVO getById(Long id, Long userId);
    void delete(Long id, Long userId);
    PageResult<WorkflowVO> page(Integer page, Integer pageSize, String status, Long userId);

    WorkflowTask submit(Long id, Map<String, Object> inputs, Long userId);
    List<WorkflowTask> getTaskList(Long workflowId, Long userId);
    WorkflowTaskDetail getTaskDetail(Long workflowId, String taskId, Long userId);
    void doExecuteAsync(WorkflowDefinition definition, Map<String, Object> inputs,
                        Long userId, Long workflowId, String taskId);
}
