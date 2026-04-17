package com.ciff.provider.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "供应商列表项")
public class ProviderListItemVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "供应商名称")
    private String name;
}
