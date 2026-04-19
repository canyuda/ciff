package com.ciff.app.controller;

import com.ciff.agent.dto.AgentCreateRequest;
import com.ciff.agent.dto.AgentUpdateRequest;
import com.ciff.agent.dto.AgentVO;
import com.ciff.agent.service.AgentKnowledgeService;
import com.ciff.agent.service.AgentService;
import com.ciff.agent.service.AgentToolService;
import com.ciff.common.constant.ErrorCode;
import com.ciff.common.context.UserContext;
import com.ciff.common.dto.PageResult;
import com.ciff.common.dto.Result;
import com.ciff.common.exception.BizException;
import com.ciff.provider.dto.ModelVO;
import com.ciff.provider.facade.ProviderFacade;
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
@RequestMapping("/api/v1/app/agents")
@RequiredArgsConstructor
@Tag(name = "Agent 聚合接口", description = "聚合 Agent + Provider 校验的接口")
public class AppAgentController {

    private final AgentService agentService;
    private final AgentToolService agentToolService;
    private final AgentKnowledgeService agentKnowledgeService;
    private final ProviderFacade providerFacade;

    @PostMapping
    @Operation(summary = "创建 Agent（含模型校验）")
    public Result<AgentVO> create(@Valid @RequestBody AgentCreateRequest request) {
        validateModelExists(request.getModelId());
        if (request.getFallbackModelId() != null) {
            validateModelExists(request.getFallbackModelId());
        }

        AgentVO vo = agentService.create(request, UserContext.getUserId());
        enrichModelNames(vo);
        return Result.ok(vo);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新 Agent（含模型校验）")
    public Result<AgentVO> update(
            @Parameter(description = "Agent ID") @PathVariable Long id,
            @Valid @RequestBody AgentUpdateRequest request) {
        if (request.getModelId() != null) {
            validateModelExists(request.getModelId());
        }
        if (request.getFallbackModelId() != null) {
            validateModelExists(request.getFallbackModelId());
        }

        AgentVO vo = agentService.update(id, request, UserContext.getUserId());
        enrichModelNames(vo);
        return Result.ok(vo);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询 Agent 详情（含模型名称、知识库列表）")
    public Result<AgentVO> getById(
            @Parameter(description = "Agent ID") @PathVariable Long id) {
        AgentVO vo = agentService.getById(id, UserContext.getUserId());
        enrichModelNames(vo);
        enrichKnowledges(vo);
        return Result.ok(vo);
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
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页条数") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "类型筛选") @RequestParam(required = false) String type,
            @Parameter(description = "状态筛选") @RequestParam(required = false) String status) {
        PageResult<AgentVO> result = agentService.page(page, pageSize, type, status, UserContext.getUserId());
        result.getList().forEach(this::enrichModelNames);
        return Result.ok(result);
    }

    @PostMapping("/{id}/tools/{toolId}")
    @Operation(summary = "绑定工具到 Agent")
    public Result<Void> bindTool(
            @Parameter(description = "Agent ID") @PathVariable Long id,
            @Parameter(description = "工具 ID") @PathVariable Long toolId) {
        agentToolService.bind(id, toolId);
        return Result.ok();
    }

    @DeleteMapping("/{id}/tools/{toolId}")
    @Operation(summary = "解绑工具")
    public Result<Void> unbindTool(
            @Parameter(description = "Agent ID") @PathVariable Long id,
            @Parameter(description = "工具 ID") @PathVariable Long toolId) {
        agentToolService.unbind(id, toolId);
        return Result.ok();
    }

    @PutMapping("/{id}/tools")
    @Operation(summary = "全量替换 Agent 绑定的工具")
    public Result<Void> replaceTools(
            @Parameter(description = "Agent ID") @PathVariable Long id,
            @RequestBody java.util.List<Long> toolIds) {
        agentToolService.replaceAll(id, toolIds);
        return Result.ok();
    }

    @PostMapping("/{id}/knowledges/{knowledgeId}")
    @Operation(summary = "绑定知识库到 Agent")
    public Result<Void> bindKnowledge(
            @Parameter(description = "Agent ID") @PathVariable Long id,
            @Parameter(description = "知识库 ID") @PathVariable Long knowledgeId) {
        agentKnowledgeService.bind(id, knowledgeId);
        return Result.ok();
    }

    @DeleteMapping("/{id}/knowledges/{knowledgeId}")
    @Operation(summary = "解绑知识库")
    public Result<Void> unbindKnowledge(
            @Parameter(description = "Agent ID") @PathVariable Long id,
            @Parameter(description = "知识库 ID") @PathVariable Long knowledgeId) {
        agentKnowledgeService.unbind(id, knowledgeId);
        return Result.ok();
    }

    @PutMapping("/{id}/knowledges")
    @Operation(summary = "全量替换 Agent 绑定的知识库")
    public Result<Void> replaceKnowledges(
            @Parameter(description = "Agent ID") @PathVariable Long id,
            @RequestBody java.util.List<Long> knowledgeIds) {
        agentKnowledgeService.replaceAll(id, knowledgeIds);
        return Result.ok();
    }

    private void validateModelExists(Long modelId) {
        ModelVO model = providerFacade.getModelById(modelId);
        if (model == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "模型不存在: " + modelId);
        }
    }

    private void enrichModelNames(AgentVO vo) {
        if (vo == null) {
            return;
        }
        if (vo.getModelId() != null) {
            ModelVO model = providerFacade.getModelById(vo.getModelId());
            vo.setModelName(model != null ? model.getName() : null);
        }
        if (vo.getFallbackModelId() != null) {
            ModelVO model = providerFacade.getModelById(vo.getFallbackModelId());
            vo.setFallbackModelName(model != null ? model.getName() : null);
        }
    }

    private void enrichKnowledges(AgentVO vo) {
        if (vo == null) {
            return;
        }
        vo.setKnowledges(agentKnowledgeService.listKnowledges(vo.getId()));
    }
}
