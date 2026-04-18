package com.ciff.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ciff.agent.convertor.AgentConvertor;
import com.ciff.agent.dto.AgentCreateRequest;
import com.ciff.agent.dto.AgentUpdateRequest;
import com.ciff.agent.dto.AgentVO;
import com.ciff.agent.entity.AgentPO;
import com.ciff.agent.mapper.AgentMapper;
import com.ciff.agent.service.AgentService;
import com.ciff.agent.service.AgentToolService;
import com.ciff.common.constant.ErrorCode;
import com.ciff.common.dto.PageResult;
import com.ciff.common.exception.BizException;
import com.ciff.common.util.PageHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private final AgentMapper agentMapper;
    private final AgentToolService agentToolService;
    private final AgentDetailCacheHelper detailCacheHelper;

    private static final List<String> VALID_TYPES = List.of("chatbot", "agent", "workflow");
    private static final List<String> VALID_STATUSES = List.of("active", "inactive", "draft");

    @Override
    @Transactional
    public AgentVO create(AgentCreateRequest request, Long userId) {
        validateType(request.getType());
        validateNameUnique(request.getName(), null, userId);

        AgentPO po = AgentConvertor.toPO(request, userId);
        agentMapper.insert(po);

        if (request.getToolIds() != null && !request.getToolIds().isEmpty()) {
            agentToolService.replaceAll(po.getId(), request.getToolIds());
        }

        AgentVO vo = AgentConvertor.toVO(po);
        vo.setTools(agentToolService.listTools(po.getId()));
        return vo;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "agent-cache", key = "#id", beforeInvocation = true)
    public AgentVO update(Long id, AgentUpdateRequest request, Long userId) {
        AgentPO po = requireExists(id, userId);

        if (request.getName() != null && !request.getName().equals(po.getName())) {
            validateNameUnique(request.getName(), id, userId);
        }
        if (request.getType() != null) {
            validateType(request.getType());
        }

        AgentConvertor.updatePO(po, request);
        agentMapper.updateById(po);

        if (request.getToolIds() != null) {
            agentToolService.replaceAll(po.getId(), request.getToolIds());
        }

        AgentVO vo = AgentConvertor.toVO(po);
        vo.setTools(agentToolService.listTools(po.getId()));
        return vo;
    }

    @Override
    public AgentVO getById(Long id, Long userId) {
        requireExists(id, userId);
        AgentVO vo = detailCacheHelper.getDetail(id);
        if (vo == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Agent 不存在: " + id);
        }
        return vo;
    }

    @Override
    @CacheEvict(cacheNames = "agent-cache", key = "#id")
    public void delete(Long id, Long userId) {
        requireExists(id, userId);
        agentMapper.deleteById(id);
    }

    @Override
    public PageResult<AgentVO> page(Integer page, Integer pageSize, String type, String status, Long userId) {
        Page<AgentPO> pageParam = PageHelper.toPage(page, pageSize);

        LambdaQueryWrapper<AgentPO> wrapper = new LambdaQueryWrapper<AgentPO>()
                .eq(userId != null, AgentPO::getUserId, userId)
                .eq(type != null && !type.isEmpty(), AgentPO::getType, type)
                .eq(status != null && !status.isEmpty(), AgentPO::getStatus, status)
                .orderByDesc(AgentPO::getCreateTime);

        Page<AgentPO> result = agentMapper.selectPage(pageParam, wrapper);
        List<AgentVO> records = result.getRecords().stream().map(AgentConvertor::toVO).toList();
        return PageResult.of(records, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    private void validateType(String type) {
        if (!VALID_TYPES.contains(type)) {
            throw new BizException(ErrorCode.BAD_REQUEST,
                    "不支持的 Agent 类型: " + type + "，仅支持: " + VALID_TYPES);
        }
    }

    private void validateNameUnique(String name, Long excludeId, Long userId) {
        LambdaQueryWrapper<AgentPO> wrapper = new LambdaQueryWrapper<AgentPO>()
                .eq(AgentPO::getName, name)
                .eq(AgentPO::getUserId, userId);
        if (excludeId != null) {
            wrapper.ne(AgentPO::getId, excludeId);
        }
        if (agentMapper.selectCount(wrapper) > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Agent 名称已存在: " + name);
        }
    }

    private AgentPO requireExists(Long id, Long userId) {
        AgentPO po = agentMapper.selectById(id);
        if (po == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Agent 不存在: " + id);
        }
        if (userId != null && !userId.equals(po.getUserId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "Agent 不存在: " + id);
        }
        return po;
    }
}
