package com.ciff.workflow.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ciff.workflow.entity.WorkflowExecutionPO;
import com.ciff.workflow.mapper.WorkflowExecutionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowTaskTimeoutScheduler {

    private final WorkflowExecutionMapper executionMapper;

    @Value("${ciff.workflow.task-timeout-minutes:1}")
    private int timeoutMinutes;

    @Scheduled(fixedDelayString = "${ciff.workflow.task-scan-interval-ms:30000}")
    public void scanAndMarkTimeout() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(timeoutMinutes);

        LambdaQueryWrapper<WorkflowExecutionPO> wrapper = new LambdaQueryWrapper<WorkflowExecutionPO>()
                .in(WorkflowExecutionPO::getStatus, "STARTED", "RUNNING")
                .lt(WorkflowExecutionPO::getStartTime, cutoff);

        List<WorkflowExecutionPO> timedOut = executionMapper.selectList(wrapper);
        for (WorkflowExecutionPO execution : timedOut) {
            log.warn("Workflow task {} timed out after {} minutes", execution.getTaskId(), timeoutMinutes);
            execution.setStatus("TIMEOUT");
            execution.setErrorMessage("Task timed out after " + timeoutMinutes + " minutes");
            execution.setEndTime(LocalDateTime.now());
            executionMapper.updateById(execution);
        }

        if (!timedOut.isEmpty()) {
            log.info("Marked {} timed-out workflow tasks", timedOut.size());
        }
    }
}
