package com.ciff.workflow.engine;

public final class WorkflowRedisKeys {

    private WorkflowRedisKeys() {
    }

    private static final String PREFIX = "ciff:workflow";

    // task list: ciff:workflow:tasks:{userId}:{workflowId}
    public static String taskListKey(Long userId, Long workflowId) {
        return PREFIX + ":tasks:" + userId + ":" + workflowId;
    }

    // task detail: ciff:workflow:task:{userId}:{workflowId}:{taskId}
    public static String taskDetailKey(Long userId, Long workflowId, String taskId) {
        return PREFIX + ":task:" + userId + ":" + workflowId + ":" + taskId;
    }
}
