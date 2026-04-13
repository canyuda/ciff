package com.ciff.app.controller;

import com.ciff.common.dto.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/api/v1/health")
    public Result<String> health() {
        return Result.ok("成功");
    }
}
