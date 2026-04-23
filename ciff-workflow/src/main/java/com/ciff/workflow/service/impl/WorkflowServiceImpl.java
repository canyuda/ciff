package com.ciff.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ciff.common.constant.ErrorCode;
import com.ciff.common.dto.PageResult;
import com.ciff.common.exception.BizException;
import com.ciff.common.util.PageHelper;
import com.ciff.workflow.convertor.WorkflowConvertor;
import com.ciff.workflow.dto.*;
import com.ciff.workflow.entity.WorkflowPO;
import com.ciff.workflow.engine.WorkflowEngine;
import com.ciff.workflow.engine.dto.WorkflowExecutionResult;
import com.ciff.workflow.exception.InvalidWorkflowDefinitionException;
import com.ciff.workflow.mapper.WorkflowMapper;
import com.ciff.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkflowServiceImpl implements WorkflowService {

    private static final Set<String> VALID_STEP_TYPES = Set.of("llm", "tool", "condition", "knowledge_retrieval");

    private final WorkflowMapper workflowMapper;
    private final WorkflowEngine workflowEngine;

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
    public WorkflowExecutionResult execute(Long id, Map<String, Object> inputs, Long userId) {
        WorkflowPO po = requireExists(id, userId);
        return workflowEngine.execute(po.getDefinition(), inputs);
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

        // step id uniqueness
        Set<String> ids = new HashSet<>();
        for (StepDefinition step : steps) {
            if (!ids.add(step.getId())) {
                throw new InvalidWorkflowDefinitionException("Duplicate step id: " + step.getId());
            }
        }

        for (StepDefinition step : steps) {
            // step type validation
            if (!VALID_STEP_TYPES.contains(step.getType())) {
                throw new InvalidWorkflowDefinitionException("Invalid step type: " + step.getType());
            }
            // nextStepId must reference existing step
            if (step.getNextStepId() != null && !ids.contains(step.getNextStepId())) {
                throw new InvalidWorkflowDefinitionException("nextStepId references non-existent step: " + step.getNextStepId());
            }
            // dependsOn must reference existing steps
            if (step.getDependsOn() != null) {
                for (String dep : step.getDependsOn()) {
                    if (!ids.contains(dep)) {
                        throw new InvalidWorkflowDefinitionException("dependsOn references non-existent step: " + dep);
                    }
                }
            }
        }

        // cycle detection via DFS
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
