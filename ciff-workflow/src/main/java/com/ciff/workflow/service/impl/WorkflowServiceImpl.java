package com.ciff.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ciff.common.constant.ErrorCode;
import com.ciff.common.dto.PageResult;
import com.ciff.common.exception.BizException;
import com.ciff.common.util.PageHelper;
import com.ciff.workflow.convertor.WorkflowConvertor;
import com.ciff.workflow.dto.*;
import com.ciff.workflow.engine.WorkflowEngine;
import com.ciff.workflow.engine.dto.WorkflowExecutionResult;
import com.ciff.workflow.engine.dto.WorkflowExecutionResult.StepResult;
import com.ciff.workflow.engine.dto.WorkflowTask;
import com.ciff.workflow.engine.dto.WorkflowTaskDetail;
import com.ciff.workflow.entity.WorkflowExecutionPO;
import com.ciff.workflow.entity.WorkflowNodeExecutionPO;
import com.ciff.workflow.entity.WorkflowPO;
import com.ciff.workflow.exception.InvalidWorkflowDefinitionException;
import com.ciff.workflow.mapper.WorkflowExecutionMapper;
import com.ciff.workflow.mapper.WorkflowMapper;
import com.ciff.workflow.mapper.WorkflowNodeExecutionMapper;
import com.ciff.workflow.service.WorkflowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WorkflowServiceImpl implements WorkflowService {

    private static final Set<String> VALID_STEP_TYPES = Set.of("llm", "tool", "condition", "knowledge_retrieval");

    private final WorkflowMapper workflowMapper;
    private final WorkflowEngine workflowEngine;
    private final WorkflowExecutionMapper executionMapper;
    private final WorkflowNodeExecutionMapper nodeExecutionMapper;
    private final WorkflowService self;

    public WorkflowServiceImpl(WorkflowMapper workflowMapper,
                               WorkflowEngine workflowEngine,
                               WorkflowExecutionMapper executionMapper,
                               WorkflowNodeExecutionMapper nodeExecutionMapper,
                               @Lazy WorkflowService self) {
        this.workflowMapper = workflowMapper;
        this.workflowEngine = workflowEngine;
        this.executionMapper = executionMapper;
        this.nodeExecutionMapper = nodeExecutionMapper;
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

        WorkflowExecutionPO execution = WorkflowConvertor.toExecutionPO(id, userId, taskId, inputs, totalSteps);
        executionMapper.insert(execution);

        self.doExecuteAsync(po.getDefinition(), inputs, userId, id, taskId, execution.getId());

        return WorkflowConvertor.toTask(execution);
    }

    @Override
    public List<WorkflowTask> getTaskList(Long workflowId, Long userId) {
        LambdaQueryWrapper<WorkflowExecutionPO> wrapper = new LambdaQueryWrapper<WorkflowExecutionPO>()
                .eq(WorkflowExecutionPO::getWorkflowId, workflowId)
                .eq(WorkflowExecutionPO::getUserId, userId)
                .orderByDesc(WorkflowExecutionPO::getStartTime);
        return executionMapper.selectList(wrapper).stream()
                .map(WorkflowConvertor::toTask)
                .collect(Collectors.toList());
    }

    @Override
    public WorkflowTaskDetail getTaskDetail(Long workflowId, String taskId, Long userId) {
        LambdaQueryWrapper<WorkflowExecutionPO> wrapper = new LambdaQueryWrapper<WorkflowExecutionPO>()
                .eq(WorkflowExecutionPO::getWorkflowId, workflowId)
                .eq(WorkflowExecutionPO::getTaskId, taskId)
                .eq(WorkflowExecutionPO::getUserId, userId);
        WorkflowExecutionPO execution = executionMapper.selectOne(wrapper);
        if (execution == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Task not found: " + taskId);
        }

        LambdaQueryWrapper<WorkflowNodeExecutionPO> nodeWrapper = new LambdaQueryWrapper<WorkflowNodeExecutionPO>()
                .eq(WorkflowNodeExecutionPO::getExecutionId, execution.getId())
                .orderByAsc(WorkflowNodeExecutionPO::getId);
        List<WorkflowNodeExecutionPO> nodeExecutions = nodeExecutionMapper.selectList(nodeWrapper);

        return WorkflowConvertor.toTaskDetail(execution, nodeExecutions);
    }

    @Async
    public void doExecuteAsync(WorkflowDefinition definition, Map<String, Object> inputs,
                               Long userId, Long workflowId, String taskId, Long executionId) {
        WorkflowExecutionPO execution = executionMapper.selectById(executionId);

        try {
            execution.setStatus("RUNNING");
            executionMapper.updateById(execution);

            WorkflowExecutionResult result = workflowEngine.execute(definition, inputs,
                    (step, stepResult, allResults, totalSteps) -> {
                execution.setCurrentStepId(step.getId());
                execution.setCurrentStepName(step.getName());
                execution.setCompletedSteps(allResults.size());
                executionMapper.updateById(execution);

                WorkflowNodeExecutionPO nodePO = WorkflowConvertor.toNodeExecutionPO(executionId, stepResult);
                nodeExecutionMapper.insert(nodePO);
            });

            String finalStatus = result.isSuccess() ? "SUCCESS" : "FAILED";
            String error = result.isSuccess() ? null : result.getStepResults().stream()
                    .filter(r -> !r.isSuccess())
                    .map(StepResult::getError)
                    .findFirst().orElse(null);

            execution.setStatus(finalStatus);
            execution.setErrorMessage(error);
            execution.setFinalOutputs(result.getFinalOutputs());
            execution.setEndTime(LocalDateTime.now());
            executionMapper.updateById(execution);

        } catch (Exception e) {
            log.error("Workflow async execution failed, taskId={}", taskId, e);
            execution.setStatus("FAILED");
            execution.setErrorMessage(e.getMessage());
            execution.setEndTime(LocalDateTime.now());
            executionMapper.updateById(execution);
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
