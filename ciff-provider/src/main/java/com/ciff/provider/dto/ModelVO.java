package com.ciff.provider.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "模型响应")
public class ModelVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "所属供应商ID")
    private Long providerId;

    @Schema(description = "供应商名称")
    private String providerName;

    @Schema(description = "模型名称")
    private String name;

    @Schema(description = "模型显示名称")
    private String displayName;

    @Schema(description = "最大 token 数")
    private Integer maxTokens;

    @Schema(description = "默认参数")
    private ModelDefaultParam defaultParams;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
