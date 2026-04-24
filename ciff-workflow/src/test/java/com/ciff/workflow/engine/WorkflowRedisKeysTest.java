package com.ciff.workflow.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowRedisKeysTest {

    @Test
    void taskListKey_shouldContainCorrectPrefix() {
        String key = WorkflowRedisKeys.taskListKey(100L, 5L);
        assertEquals("ciff:workflow:tasks:100:5", key);
    }

    @Test
    void taskDetailKey_shouldContainTaskId() {
        String key = WorkflowRedisKeys.taskDetailKey(100L, 5L, "abc123");
        assertEquals("ciff:workflow:task:100:5:abc123", key);
    }
}
