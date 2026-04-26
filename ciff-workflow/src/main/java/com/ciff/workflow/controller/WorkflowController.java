package com.ciff.workflow.controller;

import com.ciff.common.context.UserContext;
import com.ciff.common.dto.PageResult;
import com.ciff.common.dto.Result;
import com.ciff.workflow.dto.WorkflowCreateRequest;
import com.ciff.workflow.dto.WorkflowUpdateRequest;
import com.ciff.workflow.dto.WorkflowVO;
import com.ciff.workflow.engine.dto.WorkflowExecutionResult;
import com.ciff.workflow.engine.dto.WorkflowTask;
import com.ciff.workflow.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/workflows")
@RequiredArgsConstructor
@Tag(name = "Workflow CRUD", description = "Workflow definition management")
public class WorkflowController {

    private final WorkflowService workflowService;

    // todo [未被前端使用:待清理]
    @PostMapping
    @Operation(summary = "Create workflow")
    public Result<WorkflowVO> create(@Valid @RequestBody WorkflowCreateRequest request) {
        return Result.ok(workflowService.create(request, UserContext.getUserId()));
    }

    // todo [未被前端使用:待清理]
    @PutMapping("/{id}")
    @Operation(summary = "Update workflow")
    public Result<WorkflowVO> update(
            @Parameter(description = "Workflow ID") @PathVariable Long id,
            @Valid @RequestBody WorkflowUpdateRequest request) {
        return Result.ok(workflowService.update(id, request, UserContext.getUserId()));
    }

    // todo [未被前端使用:待清理]
    @GetMapping("/{id}")
    @Operation(summary = "Get workflow by ID")
    public Result<WorkflowVO> getById(
            @Parameter(description = "Workflow ID") @PathVariable Long id) {
        return Result.ok(workflowService.getById(id, UserContext.getUserId()));
    }

    // todo [未被前端使用:待清理]
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete workflow")
    public Result<Void> delete(
            @Parameter(description = "Workflow ID") @PathVariable Long id) {
        workflowService.delete(id, UserContext.getUserId());
        return Result.ok();
    }

    // todo [未被前端使用:待清理]
    @GetMapping
    @Operation(summary = "Page query workflows")
    public Result<PageResult<WorkflowVO>> page(
            @Parameter(description = "Page number, starts from 1") @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size, max 100") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Status filter") @RequestParam(required = false) String status) {
        return Result.ok(workflowService.page(page, pageSize, status, UserContext.getUserId()));
    }

    // todo [未被前端使用:待清理]
    @PostMapping("/{id}/execute")
    @Operation(summary = "Submit workflow execution (async)")
    public Result<WorkflowTask> execute(
            @Parameter(description = "Workflow ID") @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> inputs) {
        return Result.ok(workflowService.submit(id, inputs, UserContext.getUserId()));
    }
}
