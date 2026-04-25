package com.ciff.app.controller;

import com.ciff.app.dto.apikey.ApiKeyCreateRequest;
import com.ciff.app.dto.apikey.ApiKeyVO;
import com.ciff.app.service.ApiKeyService;
import com.ciff.common.dto.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @PostMapping
    public Result<ApiKeyVO> create(@Valid @RequestBody ApiKeyCreateRequest request) {
        return Result.ok(apiKeyService.createKey(request));
    }

    @GetMapping
    public Result<List<ApiKeyVO>> list() {
        return Result.ok(apiKeyService.listKeys());
    }

    @DeleteMapping("/{id}")
    public Result<Void> revoke(@PathVariable Long id) {
        apiKeyService.revokeKey(id);
        return Result.ok();
    }
}
