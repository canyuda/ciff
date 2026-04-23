package com.ciff.workflow.engine.step;

import com.ciff.workflow.dto.StepDefinition;
import com.ciff.workflow.engine.WorkflowContext;
import com.ciff.workflow.engine.dto.WorkflowExecutionResult.StepResult;

import java.util.Map;

public interface StepExecutor {
    StepResult execute(StepDefinition step, WorkflowContext context);
}
