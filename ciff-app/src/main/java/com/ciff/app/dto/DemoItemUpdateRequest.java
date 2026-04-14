package com.ciff.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "更新演示项请求")
public class DemoItemUpdateRequest {

    @Size(max = 100, message = "名称最长100个字符")
    @Schema(description = "名称", example = "测试项")
    private String name;

    @Schema(description = "状态", example = "1")
    private Integer status;
}
