package com.ciff.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Update workflow request")
public class WorkflowUpdateRequest {
    @Size(max = 128, message = "name max 128 characters")
    @Schema(description = "Workflow name")
    private String name;

    @Size(max = 512, message = "description max 512 characters")
    @Schema(description = "Description")
    private String description;

    @Schema(description = "Workflow definition JSON")
    private WorkflowDefinition definition;

    @Schema(description = "Status: active / inactive / draft")
    private String status;
}
