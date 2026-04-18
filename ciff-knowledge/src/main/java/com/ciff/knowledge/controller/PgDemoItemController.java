package com.ciff.knowledge.controller;

import com.ciff.common.dto.Result;
import com.ciff.knowledge.entity.PgDemoItem;
import com.ciff.knowledge.service.PgDemoItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pg-demo-items")
@RequiredArgsConstructor
public class PgDemoItemController {

    private final PgDemoItemService pgDemoItemService;

    @PostMapping
    public Result<PgDemoItem> create(@RequestParam String name) {
        return Result.ok(pgDemoItemService.create(name));
    }

    @GetMapping("/{id}")
    public Result<PgDemoItem> getById(@PathVariable Long id) {
        PgDemoItem item = pgDemoItemService.getById(id);
        return item == null ? Result.fail(404, "Item not found") : Result.ok(item);
    }

    @GetMapping
    public Result<List<PgDemoItem>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(pgDemoItemService.list(page, pageSize));
    }

    @PutMapping("/{id}")
    public Result<PgDemoItem> update(@PathVariable Long id, @RequestParam String name) {
        return Result.ok(pgDemoItemService.update(id, name));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        pgDemoItemService.delete(id);
        return Result.ok();
    }
}