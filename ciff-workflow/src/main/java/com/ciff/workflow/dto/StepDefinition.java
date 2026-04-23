package com.ciff.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class StepDefinition {
    @NotBlank(message = "step id cannot be blank")
    private String id;
    @NotBlank(message = "step type cannot be blank")
    private String type; // llm / tool / condition / knowledge_retrieval
    @NotBlank(message = "step name cannot be blank")
    private String name;
    private Map<String, Object> config;
    private List<String> dependsOn;
    private String nextStepId;
    private Map<String, String> outputs;
}
