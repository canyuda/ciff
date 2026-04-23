package com.ciff.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Workflow response")
public class WorkflowVO {
    @Schema(description = "ID")
    private Long id;

    @Schema(description = "Workflow name")
    private String name;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Workflow definition")
    private WorkflowDefinition definition;

    @Schema(description = "Status")
    private String status;

    @Schema(description = "Create time")
    private LocalDateTime createTime;

    @Schema(description = "Update time")
    private LocalDateTime updateTime;
}
