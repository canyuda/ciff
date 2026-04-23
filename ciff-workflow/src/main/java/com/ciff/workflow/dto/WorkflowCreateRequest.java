package com.ciff.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Create workflow request")
public class WorkflowCreateRequest {
    @NotBlank(message = "name cannot be blank")
    @Size(max = 128, message = "name max 128 characters")
    @Schema(description = "Workflow name")
    private String name;

    @Size(max = 512, message = "description max 512 characters")
    @Schema(description = "Description")
    private String description;

    @NotNull(message = "definition cannot be null")
    @Schema(description = "Workflow definition JSON")
    private WorkflowDefinition definition;

    @Schema(description = "Status: active / inactive / draft", example = "draft")
    private String status = "draft";
}
