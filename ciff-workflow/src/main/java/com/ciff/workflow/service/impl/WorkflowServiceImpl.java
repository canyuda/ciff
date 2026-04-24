package com.ciff.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ciff.common.constant.ErrorCode;
import com.ciff.common.dto.PageResult;
import com.ciff.common.exception.BizException;
import com.ciff.common.util.PageHelper;
import com.ciff.common.util.RedisUtil;
import com.ciff.workflow.convertor.WorkflowConvertor;
import com.ciff.workflow.dto.*;
import com.ciff.workflow.engine.WorkflowEngine;
import com.ciff.workflow.engine.WorkflowRedisKeys;
import com.ciff.workflow.engine.dto.WorkflowExecutionResult;
import com.ciff.workflow.engine.dto.WorkflowTask;
import com.ciff.workflow.engine.dto.WorkflowTaskDetail;
import com.ciff.workflow.engine.dto.WorkflowExecutionResult.StepResult;
import com.ciff.workflow.entity.WorkflowPO;
import com.ciff.workflow.exception.InvalidWorkflowDefinitionException;
import com.ciff.workflow.mapper.WorkflowMapper;
import com.ciff.workflow.service.WorkflowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WorkflowServiceImpl implements WorkflowService {

    private static final Set<String> VALID_STEP_TYPES = Set.of("llm", "tool", "condition", "knowledge_retrieval");
    private static final long TASK_TTL_HOURS = 24;

    private final WorkflowMapper workflowMapper;
    private final WorkflowEngine workflowEngine;
    private final RedisUtil redisUtil;
    private final WorkflowService self;

    public WorkflowServiceImpl(WorkflowMapper workflowMapper,
                               WorkflowEngine workflowEngine,
                               RedisUtil redisUtil,
                               @Lazy WorkflowService self) {
        this.workflowMapper = workflowMapper;
        this.workflowEngine = workflowEngine;
        this.redisUtil = redisUtil;
        this.self = self;
    }

    @Override
    public WorkflowVO create(WorkflowCreateRequest request, Long userId) {
        validateNameUnique(request.getName(), null, userId);
        validateDefinition(request.getDefinition());

        WorkflowPO po = WorkflowConvertor.toPO(request, userId);
        workflowMapper.insert(po);
        return WorkflowConvertor.toVO(po);
    }

    @Override
    public WorkflowVO update(Long id, WorkflowUpdateRequest request, Long userId) {
        WorkflowPO po = requireExists(id, userId);

        if (request.getName() != null) {
            validateNameUnique(request.getName(), id, userId);
        }
        if (request.getDefinition() != null) {
            validateDefinition(request.getDefinition());
        }

        WorkflowConvertor.updatePO(po, request);
        workflowMapper.updateById(po);
        return WorkflowConvertor.toVO(po);
    }

    @Override
    public WorkflowVO getById(Long id, Long userId) {
        return WorkflowConvertor.toVO(requireExists(id, userId));
    }

    @Override
    public void delete(Long id, Long userId) {
        requireExists(id, userId);
        workflowMapper.deleteById(id);
    }

    @Override
    public PageResult<WorkflowVO> page(Integer page, Integer pageSize, String status, Long userId) {
        Page<WorkflowPO> pager = PageHelper.toPage(page, pageSize);

        LambdaQueryWrapper<WorkflowPO> wrapper = new LambdaQueryWrapper<WorkflowPO>()
                .eq(WorkflowPO::getUserId, userId)
                .eq(status != null, WorkflowPO::getStatus, status)
                .orderByDesc(WorkflowPO::getUpdateTime);

        Page<WorkflowPO> result = workflowMapper.selectPage(pager, wrapper);
        List<WorkflowVO> vos = result.getRecords().stream()
                .map(WorkflowConvertor::toVO)
                .collect(Collectors.toList());
        return PageResult.of(vos, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    @Override
    public WorkflowTask submit(Long id, Map<String, Object> inputs, Long userId) {
        WorkflowPO po = requireExists(id, userId);

        String taskId = UUID.randomUUID().toString().replace("-", "");
        int totalSteps = po.getDefinition().getSteps().size();

        WorkflowTask task = WorkflowTask.builder()
                .taskId(taskId)
                .workflowId(id)
                .userId(userId)
                .status(WorkflowTask.TaskStatus.STARTED)
                .totalSteps(totalSteps)
                .completedSteps(0)
                .inputs(inputs != null ? inputs : Map.of())
                .startTime(LocalDateTime.now())
                .build();

        // push task summary to Redis List
        String listKey = WorkflowRedisKeys.taskListKey(userId, id);
        redisUtil.listPushWithTtl(listKey, task, TASK_TTL_HOURS, TimeUnit.HOURS);

        // create task detail as Redis String
        WorkflowTaskDetail detail = WorkflowTaskDetail.builder()
                .taskId(taskId)
                .workflowId(id)
                .status(WorkflowTask.TaskStatus.STARTED)
                .totalSteps(totalSteps)
                .completedSteps(0)
                .inputs(inputs != null ? inputs : Map.of())
                .stepResults(new ArrayList<>())
                .startTime(LocalDateTime.now().toString())
                .build();
        String detailKey = WorkflowRedisKeys.taskDetailKey(userId, id, taskId);
        redisUtil.set(detailKey, detail, TASK_TTL_HOURS, TimeUnit.HOURS);

        // async execution via proxy to ensure @Async works
        self.doExecuteAsync(po.getDefinition(), inputs, userId, id, taskId);

        return task;
    }

    @Override
    public List<WorkflowTask> getTaskList(Long workflowId, Long userId) {
        String listKey = WorkflowRedisKeys.taskListKey(userId, workflowId);
        List<WorkflowTask> tasks = redisUtil.listRange(listKey);
        if (tasks == null) return List.of();
        // newest first
        List<WorkflowTask> result = new ArrayList<>(tasks);
        Collections.reverse(result);
        return result;
    }

    @Override
    public WorkflowTaskDetail getTaskDetail(Long workflowId, String taskId, Long userId) {
        String detailKey = WorkflowRedisKeys.taskDetailKey(userId, workflowId, taskId);
        WorkflowTaskDetail detail = redisUtil.get(detailKey);
        if (detail == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Task not found: " + taskId);
        }
        return detail;
    }

    @Async
    public void doExecuteAsync(WorkflowDefinition definition, Map<String, Object> inputs,
                               Long userId, Long workflowId, String taskId) {
        String listKey = WorkflowRedisKeys.taskListKey(userId, workflowId);
        String detailKey = WorkflowRedisKeys.taskDetailKey(userId, workflowId, taskId);

        try {
            updateStatus(listKey, detailKey, taskId, WorkflowTask.TaskStatus.RUNNING, null);

            WorkflowExecutionResult result = workflowEngine.execute(definition, inputs, (step, stepResult, allResults, totalSteps) -> {
                int completed = allResults.size();

                // update task in list
                WorkflowTask task = findTaskInList(listKey, taskId);
                if (task != null) {
                    task.setStatus(WorkflowTask.TaskStatus.RUNNING);
                    task.setCurrentStepId(step.getId());
                    task.setCurrentStepName(step.getName());
                    task.setCompletedSteps(completed);
                    updateTaskInList(listKey, taskId, task);
                }

                // update task detail
                WorkflowTaskDetail detail = redisUtil.get(detailKey);
                if (detail != null) {
                    detail.setStatus(WorkflowTask.TaskStatus.RUNNING);
                    detail.setCurrentStepId(step.getId());
                    detail.setCurrentStepName(step.getName());
                    detail.setCompletedSteps(completed);
                    detail.setStepResults(new ArrayList<>(allResults));
                    redisUtil.set(detailKey, detail, TASK_TTL_HOURS, TimeUnit.HOURS);
                }
            });

            WorkflowTask.TaskStatus finalStatus = result.isSuccess()
                    ? WorkflowTask.TaskStatus.SUCCESS : WorkflowTask.TaskStatus.FAILED;
            String error = result.isSuccess() ? null : result.getStepResults().stream()
                    .filter(r -> !r.isSuccess())
                    .map(StepResult::getError)
                    .findFirst().orElse(null);

            updateStatus(listKey, detailKey, taskId, finalStatus, error);

            // update detail with final outputs
            WorkflowTaskDetail detail = redisUtil.get(detailKey);
            if (detail != null) {
                detail.setFinalOutputs(result.getFinalOutputs());
                detail.setStepResults(result.getStepResults());
                detail.setEndTime(LocalDateTime.now().toString());
                redisUtil.set(detailKey, detail, TASK_TTL_HOURS, TimeUnit.HOURS);
            }

        } catch (Exception e) {
            log.error("Workflow async execution failed, taskId={}", taskId, e);
            updateStatus(listKey, detailKey, taskId, WorkflowTask.TaskStatus.FAILED, e.getMessage());
        }
    }

    private void updateStatus(String listKey, String detailKey, String taskId,
                              WorkflowTask.TaskStatus status, String error) {
        // update task in list
        WorkflowTask task = findTaskInList(listKey, taskId);
        if (task != null) {
            task.setStatus(status);
            task.setEndTime(isTerminalStatus(status) ? LocalDateTime.now() : null);
            updateTaskInList(listKey, taskId, task);
        }

        // update detail
        WorkflowTaskDetail detail = redisUtil.get(detailKey);
        if (detail != null) {
            detail.setStatus(status);
            if (error != null) detail.setError(error);
            if (isTerminalStatus(status)) detail.setEndTime(LocalDateTime.now().toString());
            redisUtil.set(detailKey, detail, TASK_TTL_HOURS, TimeUnit.HOURS);
        }
    }

    private boolean isTerminalStatus(WorkflowTask.TaskStatus status) {
        return status == WorkflowTask.TaskStatus.SUCCESS
                || status == WorkflowTask.TaskStatus.FAILED
                || status == WorkflowTask.TaskStatus.TIMEOUT;
    }

    private WorkflowTask findTaskInList(String listKey, String taskId) {
        List<WorkflowTask> tasks = redisUtil.listRange(listKey);
        if (tasks == null) return null;
        for (int i = 0; i < tasks.size(); i++) {
            if (taskId.equals(tasks.get(i).getTaskId())) {
                return tasks.get(i);
            }
        }
        return null;
    }

    private void updateTaskInList(String listKey, String taskId, WorkflowTask updated) {
        List<WorkflowTask> tasks = redisUtil.listRange(listKey);
        if (tasks == null) return;
        for (int i = 0; i < tasks.size(); i++) {
            if (taskId.equals(tasks.get(i).getTaskId())) {
                redisUtil.listUpdateEntry(listKey, i, updated);
                return;
            }
        }
    }

    private WorkflowPO requireExists(Long id, Long userId) {
        LambdaQueryWrapper<WorkflowPO> wrapper = new LambdaQueryWrapper<WorkflowPO>()
                .eq(WorkflowPO::getId, id)
                .eq(WorkflowPO::getUserId, userId);
        WorkflowPO po = workflowMapper.selectOne(wrapper);
        if (po == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Workflow not found: " + id);
        }
        return po;
    }

    private void validateNameUnique(String name, Long excludeId, Long userId) {
        LambdaQueryWrapper<WorkflowPO> wrapper = new LambdaQueryWrapper<WorkflowPO>()
                .eq(WorkflowPO::getName, name)
                .eq(WorkflowPO::getUserId, userId);
        if (excludeId != null) {
            wrapper.ne(WorkflowPO::getId, excludeId);
        }
        if (workflowMapper.selectCount(wrapper) > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Workflow name already exists: " + name);
        }
    }

    void validateDefinition(WorkflowDefinition definition) {
        if (definition == null || definition.getSteps() == null || definition.getSteps().isEmpty()) {
            throw new InvalidWorkflowDefinitionException("Definition steps cannot be empty");
        }

        List<StepDefinition> steps = definition.getSteps();

        Set<String> ids = new HashSet<>();
        for (StepDefinition step : steps) {
            if (!ids.add(step.getId())) {
                throw new InvalidWorkflowDefinitionException("Duplicate step id: " + step.getId());
            }
        }

        for (StepDefinition step : steps) {
            if (!VALID_STEP_TYPES.contains(step.getType())) {
                throw new InvalidWorkflowDefinitionException("Invalid step type: " + step.getType());
            }
            if (step.getNextStepId() != null && !ids.contains(step.getNextStepId())) {
                throw new InvalidWorkflowDefinitionException("nextStepId references non-existent step: " + step.getNextStepId());
            }
            if (step.getDependsOn() != null) {
                for (String dep : step.getDependsOn()) {
                    if (!ids.contains(dep)) {
                        throw new InvalidWorkflowDefinitionException("dependsOn references non-existent step: " + dep);
                    }
                }
            }
        }

        detectCycle(steps);
    }

    private void detectCycle(List<StepDefinition> steps) {
        Map<String, List<String>> graph = new HashMap<>();
        for (StepDefinition step : steps) {
            graph.put(step.getId(), new ArrayList<>());
        }
        for (StepDefinition step : steps) {
            if (step.getNextStepId() != null) {
                graph.get(step.getId()).add(step.getNextStepId());
            }
        }

        Set<String> visited = new HashSet<>();
        Set<String> inStack = new HashSet<>();
        for (StepDefinition step : steps) {
            if (dfs(step.getId(), graph, visited, inStack)) {
                throw new InvalidWorkflowDefinitionException("Cycle detected in workflow definition");
            }
        }
    }

    private boolean dfs(String node, Map<String, List<String>> graph, Set<String> visited, Set<String> inStack) {
        if (inStack.contains(node)) return true;
        if (visited.contains(node)) return false;

        visited.add(node);
        inStack.add(node);
        for (String next : graph.getOrDefault(node, List.of())) {
            if (dfs(next, graph, visited, inStack)) return true;
        }
        inStack.remove(node);
        return false;
    }
}
