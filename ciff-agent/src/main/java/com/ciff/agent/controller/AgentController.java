package com.ciff.agent.controller;

import com.ciff.agent.dto.AgentCreateRequest;
import com.ciff.agent.dto.AgentUpdateRequest;
import com.ciff.agent.dto.AgentVO;
import com.ciff.agent.service.AgentService;
import com.ciff.common.context.UserContext;
import com.ciff.common.dto.PageResult;
import com.ciff.common.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
@Tag(name = "Agent 管理", description = "Agent CRUD 接口")
public class AgentController {

    private final AgentService agentService;

    @PostMapping
    @Operation(summary = "创建 Agent")
    public Result<AgentVO> create(@Valid @RequestBody AgentCreateRequest request) {
        return Result.ok(agentService.create(request, UserContext.getUserId()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新 Agent")
    public Result<AgentVO> update(
            @Parameter(description = "Agent ID") @PathVariable Long id,
            @Valid @RequestBody AgentUpdateRequest request) {
        return Result.ok(agentService.update(id, request, UserContext.getUserId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询 Agent 详情")
    public Result<AgentVO> getById(
            @Parameter(description = "Agent ID") @PathVariable Long id) {
        return Result.ok(agentService.getById(id, UserContext.getUserId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除 Agent")
    public Result<Void> delete(
            @Parameter(description = "Agent ID") @PathVariable Long id) {
        agentService.delete(id, UserContext.getUserId());
        return Result.ok();
    }

    @GetMapping
    @Operation(summary = "分页查询 Agent 列表")
    public Result<PageResult<AgentVO>> page(
            @Parameter(description = "页码，从1开始") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页条数，最大100") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "类型筛选: chatbot / agent / workflow") @RequestParam(required = false) String type,
            @Parameter(description = "状态筛选: active / inactive / draft") @RequestParam(required = false) String status) {
        return Result.ok(agentService.page(page, pageSize, type, status, UserContext.getUserId()));
    }
}
