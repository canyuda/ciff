package com.ciff.provider.controller;

import com.ciff.common.dto.PageResult;
import com.ciff.common.dto.Result;
import com.ciff.common.enums.ProviderStatus;
import com.ciff.common.enums.ProviderType;
import com.ciff.provider.dto.ProviderCreateRequest;
import com.ciff.provider.dto.ProviderListItemVO;
import com.ciff.provider.dto.ProviderUpdateRequest;
import com.ciff.provider.dto.ProviderVO;
import com.ciff.provider.dto.ProviderHealthVO;
import com.ciff.provider.service.ProviderHealthService;
import com.ciff.provider.service.ProviderService;
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
@RequestMapping("/api/v1/providers")
@RequiredArgsConstructor
@Tag(name = "供应商管理", description = "Provider CRUD 接口")
public class ProviderController {

    private final ProviderService providerService;
    private final ProviderHealthService healthService;

    @PostMapping
    @Operation(summary = "创建供应商")
    public Result<ProviderVO> create(@Valid @RequestBody ProviderCreateRequest request) {
        return Result.ok(providerService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新供应商")
    public Result<ProviderVO> update(
            @Parameter(description = "供应商ID") @PathVariable Long id,
            @Valid @RequestBody ProviderUpdateRequest request) {
        return Result.ok(providerService.update(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询供应商详情")
    public Result<ProviderVO> getById(
            @Parameter(description = "供应商ID") @PathVariable Long id) {
        return Result.ok(providerService.getById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除供应商")
    public Result<Void> delete(
            @Parameter(description = "供应商ID") @PathVariable Long id) {
        providerService.delete(id);
        return Result.ok();
    }

    @GetMapping
    @Operation(summary = "分页查询供应商列表")
    public Result<PageResult<ProviderVO>> page(
            @Parameter(description = "页码，从1开始") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页条数，最大100") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "类型筛选") @RequestParam(required = false) ProviderType type,
            @Parameter(description = "状态筛选") @RequestParam(required = false) String status) {
        ProviderStatus statusEnum = status != null && !status.isEmpty()
                ? ProviderStatus.valueOf(status.toUpperCase())
                : null;
        return Result.ok(providerService.page(page, pageSize, type, statusEnum));
    }

    @GetMapping("/{id}/health")
    @Operation(summary = "查询供应商健康状态")
    public Result<ProviderHealthVO> getHealth(
            @Parameter(description = "供应商ID") @PathVariable Long id) {
        return Result.ok(healthService.getHealth(id));
    }

    @GetMapping("/list")
    @Operation(summary = "查询所有供应商列表")
    public Result<List<ProviderListItemVO>> listAll() {
        return Result.ok(providerService.listAll());
    }
}
