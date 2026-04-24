package com.ciff.workflow.scheduler;

import com.ciff.common.util.RedisUtil;
import com.ciff.workflow.engine.WorkflowRedisKeys;
import com.ciff.workflow.engine.dto.WorkflowTask;
import com.ciff.workflow.engine.dto.WorkflowTaskDetail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowTaskTimeoutScheduler {

    private final RedisUtil redisUtil;

    @Value("${ciff.workflow.task-timeout-minutes:1}")
    private int timeoutMinutes;

    @Scheduled(fixedDelayString = "${ciff.workflow.task-scan-interval-ms:30000}")
    public void scanAndMarkTimeout() {
        Iterable<String> listKeys = redisUtil.getKeysByPattern("ciff:workflow:tasks:*");
        int timeoutCount = 0;

        for (String listKey : listKeys) {
            List<WorkflowTask> tasks = redisUtil.listRange(listKey);
            if (tasks == null) continue;

            for (int i = 0; i < tasks.size(); i++) {
                WorkflowTask task = tasks.get(i);
                if (!isRunning(task)) continue;
                if (!isTimedOut(task)) continue;

                markTimeout(task, listKey, i);
                timeoutCount++;
            }
        }

        if (timeoutCount > 0) {
            log.info("Marked {} timed-out workflow tasks", timeoutCount);
        }
    }

    private boolean isRunning(WorkflowTask task) {
        return task.getStatus() == WorkflowTask.TaskStatus.STARTED
                || task.getStatus() == WorkflowTask.TaskStatus.RUNNING;
    }

    private boolean isTimedOut(WorkflowTask task) {
        if (task.getStartTime() == null) return false;
        Duration elapsed = Duration.between(task.getStartTime(), LocalDateTime.now());
        return elapsed.toMinutes() >= timeoutMinutes;
    }

    private void markTimeout(WorkflowTask task, String listKey, int index) {
        log.warn("Workflow task {} timed out after {} minutes", task.getTaskId(), timeoutMinutes);

        task.setStatus(WorkflowTask.TaskStatus.TIMEOUT);
        task.setEndTime(LocalDateTime.now());
        redisUtil.listUpdateEntry(listKey, index, task);

        // update detail
        String detailKey = WorkflowRedisKeys.taskDetailKey(task.getUserId(), task.getWorkflowId(), task.getTaskId());
        WorkflowTaskDetail detail = redisUtil.get(detailKey);
        if (detail != null) {
            detail.setStatus(WorkflowTask.TaskStatus.TIMEOUT);
            detail.setError("Task timed out after " + timeoutMinutes + " minutes");
            detail.setEndTime(LocalDateTime.now().toString());
            redisUtil.set(detailKey, detail, 24, TimeUnit.HOURS);
        }
    }
}
