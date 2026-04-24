package com.ciff.app.controller;

import com.ciff.common.constant.ErrorCode;
import com.ciff.common.context.UserContext;
import com.ciff.common.dto.PageResult;
import com.ciff.common.dto.Result;
import com.ciff.common.exception.BizException;
import com.ciff.mcp.dto.ToolVO;
import com.ciff.mcp.service.ToolService;
import com.ciff.provider.dto.ModelVO;
import com.ciff.provider.facade.ProviderFacade;
import com.ciff.workflow.dto.*;
import com.ciff.workflow.engine.dto.WorkflowTask;
import com.ciff.workflow.engine.dto.WorkflowTaskDetail;
import com.ciff.workflow.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/app/workflows")
@RequiredArgsConstructor
@Tag(name = "Workflow 聚合接口", description = "聚合 Workflow + Provider/MCP 校验的接口")
public class AppWorkflowController {

    private final WorkflowService workflowService;
    private final ProviderFacade providerFacade;
    private final ToolService toolService;

    @PostMapping
    @Operation(summary = "创建工作流（含模型/工具校验）")
    public Result<WorkflowVO> create(@Valid @RequestBody WorkflowCreateRequest request) {
        validateDefinitionRefs(request.getDefinition());
        return Result.ok(workflowService.create(request, UserContext.getUserId()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新工作流（含模型/工具校验）")
    public Result<WorkflowVO> update(
            @Parameter(description = "Workflow ID") @PathVariable Long id,
            @Valid @RequestBody WorkflowUpdateRequest request) {
        if (request.getDefinition() != null) {
            validateDefinitionRefs(request.getDefinition());
        }
        return Result.ok(workflowService.update(id, request, UserContext.getUserId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询工作流详情")
    public Result<WorkflowVO> getById(
            @Parameter(description = "Workflow ID") @PathVariable Long id) {
        return Result.ok(workflowService.getById(id, UserContext.getUserId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除工作流")
    public Result<Void> delete(
            @Parameter(description = "Workflow ID") @PathVariable Long id) {
        workflowService.delete(id, UserContext.getUserId());
        return Result.ok();
    }

    @GetMapping
    @Operation(summary = "分页查询工作流列表")
    public Result<PageResult<WorkflowVO>> page(
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页条数") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "状态筛选") @RequestParam(required = false) String status) {
        return Result.ok(workflowService.page(page, pageSize, status, UserContext.getUserId()));
    }

    @PostMapping("/{id}/execute")
    @Operation(summary = "提交工作流执行（异步）")
    public Result<WorkflowTask> execute(
            @Parameter(description = "Workflow ID") @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> inputs) {
        return Result.ok(workflowService.submit(id, inputs, UserContext.getUserId()));
    }

    @GetMapping("/{id}/tasks")
    @Operation(summary = "查询工作流任务列表")
    public Result<List<WorkflowTask>> getTaskList(
            @Parameter(description = "Workflow ID") @PathVariable Long id) {
        return Result.ok(workflowService.getTaskList(id, UserContext.getUserId()));
    }

    @GetMapping("/{id}/tasks/{taskId}")
    @Operation(summary = "查询任务详情")
    public Result<WorkflowTaskDetail> getTaskDetail(
            @Parameter(description = "Workflow ID") @PathVariable Long id,
            @Parameter(description = "Task ID") @PathVariable String taskId) {
        return Result.ok(workflowService.getTaskDetail(id, taskId, UserContext.getUserId()));
    }

    private void validateDefinitionRefs(WorkflowDefinition definition) {
        if (definition == null || definition.getSteps() == null) return;

        for (StepDefinition step : definition.getSteps()) {
            Map<String, Object> config = step.getConfig();
            if (config == null) continue;

            if ("llm".equals(step.getType()) && config.get("modelId") != null) {
                Long modelId = toLong(config.get("modelId"));
                ModelVO model = providerFacade.getModelById(modelId);
                if (model == null) {
                    throw new BizException(ErrorCode.BAD_REQUEST,
                            "Step '" + step.getId() + "' references non-existent model: " + modelId);
                }
            }

            if ("tool".equals(step.getType()) && config.get("toolId") != null) {
                Long toolId = toLong(config.get("toolId"));
                ToolVO tool = toolService.getById(toolId);
                if (tool == null) {
                    throw new BizException(ErrorCode.BAD_REQUEST,
                            "Step '" + step.getId() + "' references non-existent tool: " + toolId);
                }
            }
        }
    }

    private Long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        if (value instanceof String s) return Long.parseLong(s);
        return null;
    }
}
