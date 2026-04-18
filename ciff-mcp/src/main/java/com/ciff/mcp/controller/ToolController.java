package com.ciff.mcp.controller;

import com.ciff.common.dto.PageResult;
import com.ciff.common.dto.Result;
import com.ciff.mcp.dto.ToolCreateRequest;
import com.ciff.mcp.dto.ToolUpdateRequest;
import com.ciff.mcp.dto.ToolVO;
import com.ciff.mcp.service.ToolService;
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
@RequestMapping("/api/v1/tools")
@RequiredArgsConstructor
@Tag(name = "工具管理", description = "Tool CRUD 接口")
public class ToolController {

    private final ToolService toolService;

    @PostMapping
    @Operation(summary = "创建工具")
    public Result<ToolVO> create(@Valid @RequestBody ToolCreateRequest request) {
        return Result.ok(toolService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新工具")
    public Result<ToolVO> update(
            @Parameter(description = "工具ID") @PathVariable Long id,
            @Valid @RequestBody ToolUpdateRequest request) {
        return Result.ok(toolService.update(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询工具详情")
    public Result<ToolVO> getById(
            @Parameter(description = "工具ID") @PathVariable Long id) {
        return Result.ok(toolService.getById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除工具")
    public Result<Void> delete(
            @Parameter(description = "工具ID") @PathVariable Long id) {
        toolService.delete(id);
        return Result.ok();
    }

    @GetMapping
    @Operation(summary = "分页查询工具列表")
    public Result<PageResult<ToolVO>> page(
            @Parameter(description = "页码，从1开始") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页条数，最大100") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "类型筛选: api / mcp") @RequestParam(required = false) String type,
            @Parameter(description = "状态筛选: enabled / disabled") @RequestParam(required = false) String status) {
        return Result.ok(toolService.page(page, pageSize, type, status));
    }
}
