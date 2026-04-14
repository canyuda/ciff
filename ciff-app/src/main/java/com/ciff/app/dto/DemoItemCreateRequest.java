package com.ciff.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "创建演示项请求")
public class DemoItemCreateRequest {

    @NotBlank(message = "名称不能为空")
    @Size(max = 100, message = "名称最长100个字符")
    @Schema(description = "名称", example = "测试项")
    private String name;

    @NotNull(message = "状态不能为空")
    @Schema(description = "状态", example = "0")
    private Integer status;
}
