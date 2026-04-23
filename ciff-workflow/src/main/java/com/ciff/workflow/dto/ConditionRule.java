package com.ciff.workflow.dto;

import lombok.Data;

@Data
public class ConditionRule {
    private String operator; // eq / contains / gt / default
    private String field;
    private Object value;
    private String nextStepId;
}
