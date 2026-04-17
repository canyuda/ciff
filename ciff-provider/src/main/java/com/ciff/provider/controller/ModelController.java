package com.ciff.provider.controller;

import com.ciff.common.dto.PageResult;
import com.ciff.common.dto.Result;
import com.ciff.provider.dto.ModelCreateRequest;
import com.ciff.provider.dto.ModelUpdateRequest;
import com.ciff.provider.dto.ModelVO;
import com.ciff.provider.service.ModelService;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/models")
@RequiredArgsConstructor
@Tag(name = "模型管理", description = "Model CRUD 接口")
public class ModelController {

    private final ModelService modelService;

    @PostMapping
    @Operation(summary = "创建模型")
    public Result<ModelVO> create(@Valid @RequestBody ModelCreateRequest request) {
        return Result.ok(modelService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新模型")
    public Result<ModelVO> update(
            @Parameter(description = "模型ID") @PathVariable Long id,
            @Valid @RequestBody ModelUpdateRequest request) {
        return Result.ok(modelService.update(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询模型详情")
    public Result<ModelVO> getById(
            @Parameter(description = "模型ID") @PathVariable Long id) {
        return Result.ok(modelService.getById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除模型")
    public Result<Void> delete(
            @Parameter(description = "模型ID") @PathVariable Long id) {
        modelService.delete(id);
        return Result.ok();
    }

    @GetMapping
    @Operation(summary = "分页查询模型列表")
    public Result<PageResult<ModelVO>> page(
            @Parameter(description = "页码，从1开始") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页条数，最大100") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "供应商ID筛选") @RequestParam(required = false) Long providerId,
            @Parameter(description = "状态筛选") @RequestParam(required = false) String status) {
        return Result.ok(modelService.page(page, pageSize, providerId, status));
    }

    @GetMapping("/providers/{providerId}")
    @Operation(summary = "查询供应商下的模型列表")
    public Result<List<ModelVO>> listByProviderId(
            @Parameter(description = "供应商ID") @PathVariable Long providerId) {
        return Result.ok(modelService.listByProviderId(providerId));
    }
}
