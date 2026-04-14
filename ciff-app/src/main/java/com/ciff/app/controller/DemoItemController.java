package com.ciff.app.controller;

import com.ciff.app.dto.DemoItemCreateRequest;
import com.ciff.app.dto.DemoItemUpdateRequest;
import com.ciff.app.dto.DemoItemVO;
import com.ciff.app.service.DemoItemService;
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
@RequestMapping("/api/v1/demo-items")
@RequiredArgsConstructor
@Tag(name = "演示项管理", description = "Demo Item CRUD 接口")
public class DemoItemController {

    private final DemoItemService demoItemService;

    @PostMapping
    @Operation(summary = "创建演示项")
    public Result<DemoItemVO> create(@Valid @RequestBody DemoItemCreateRequest request) {
        return Result.ok(demoItemService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新演示项")
    public Result<DemoItemVO> update(
            @Parameter(description = "演示项ID") @PathVariable Long id,
            @Valid @RequestBody DemoItemUpdateRequest request) {
        return Result.ok(demoItemService.update(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询演示项详情")
    public Result<DemoItemVO> getById(
            @Parameter(description = "演示项ID") @PathVariable Long id) {
        return Result.ok(demoItemService.getById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除演示项")
    public Result<Void> delete(
            @Parameter(description = "演示项ID") @PathVariable Long id) {
        demoItemService.delete(id);
        return Result.ok();
    }

    @GetMapping
    @Operation(summary = "分页查询演示项列表")
    public Result<PageResult<DemoItemVO>> page(
            @Parameter(description = "页码，从1开始") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页条数，最大100") @RequestParam(required = false) Integer pageSize) {
        return Result.ok(demoItemService.page(page, pageSize));
    }
}
