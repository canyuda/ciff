package com.ciff.workflow.service;

import com.ciff.common.dto.PageResult;
import com.ciff.workflow.dto.WorkflowCreateRequest;
import com.ciff.workflow.dto.WorkflowUpdateRequest;
import com.ciff.workflow.dto.WorkflowVO;
import com.ciff.workflow.engine.dto.WorkflowExecutionResult;

import java.util.Map;

public interface WorkflowService {
    WorkflowVO create(WorkflowCreateRequest request, Long userId);
    WorkflowVO update(Long id, WorkflowUpdateRequest request, Long userId);
    WorkflowVO getById(Long id, Long userId);
    void delete(Long id, Long userId);
    PageResult<WorkflowVO> page(Integer page, Integer pageSize, String status, Long userId);
    WorkflowExecutionResult execute(Long id, Map<String, Object> inputs, Long userId);
}
