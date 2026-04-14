package com.ciff.app.controller;

import com.ciff.common.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "健康检查", description = "服务状态检测")
public class HealthController {

    @GetMapping("/api/v1/health")
    @Operation(summary = "健康检查")
    public Result<String> health() {
        return Result.ok("ok");
    }
}
