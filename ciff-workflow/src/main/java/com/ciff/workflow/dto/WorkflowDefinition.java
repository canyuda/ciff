package com.ciff.workflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class WorkflowDefinition {
    @NotEmpty(message = "steps cannot be empty")
    @Valid
    private List<StepDefinition> steps;
    private Map<String, Object> inputs;
}
